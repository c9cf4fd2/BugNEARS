/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugnears;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class ProtocolParserHTTPRecorder extends ProtocolParser {

    private TCPPacket processingPkt;
    private InetAddress hostIP;
    private InetAddress cliIP;
    private int cliPort;
    private long sTime;
    //
    private String streamKey;
    private String recFilePath;
    private String recFileNamePrefix;
    //
    private InetAddress pasvFinIP;
    private boolean lastACK;
    private long lastActive;
    //
    //
    private HTTPStream reqStream;
    private int reqCount;
    private boolean reqHeaderRxing;
    private String reqHeaderStr;
    private long reqContentLength;
    private boolean reqIsChunked;
    private boolean reqIsGzip;
    private List<Byte> reqWriteFileBuffer;
    //
    private HTTPStream resStream;
    private int resCount;
    private boolean resHeaderRxing;
    private String resHeaderStr;
    private long resContentLength;
    private boolean resIsChunked;
    private boolean resIsGzip;
    private List<Byte> resWriteFileBuffer;
    //
    private Map<String, HTTPEvent> events;
    private ProtocolParserHTTP PPHTTP;

    public ProtocolParserHTTPRecorder(PacketPool pktPool, Integer TIDKey, InetAddress cliIP, int cliPort, InetAddress hostIP, String recFilePath, ProtocolParserHTTP PPHTTP, TCPPacket firstPkt) {
        super(pktPool, TIDKey);
        this.hostIP = hostIP;
        this.cliIP = cliIP;
        this.cliPort = cliPort;
        this.recFilePath = recFilePath;
        this.PPHTTP = PPHTTP;
        streamKey = cliIP.getHostAddress() + cliPort + hostIP.getHostAddress();
        reqStream = new HTTPStream(true);
        resStream = new HTTPStream(false);
        reqStream.put(firstPkt);
        sTime = System.currentTimeMillis();
        recFileNamePrefix = sTime + "-" + streamKey + "-";
        lastActive = sTime;
        //
        reqHeaderRxing = true;
        resHeaderRxing = true;
        reqHeaderStr = "";
        resHeaderStr = "";
        reqCount = 0;
        resCount = 0;
        resContentLength = -1;
        resIsChunked = false;
        reqContentLength = -1;
        reqIsChunked = false;
        resWriteFileBuffer = new LinkedList<Byte>();
        reqWriteFileBuffer = new LinkedList<Byte>();
        events = new HashMap<String, HTTPEvent>();
    }

    @Override
    protected boolean isRelative(Packet p) {
        if (p instanceof TCPPacket) {
            processingPkt = (TCPPacket) p;
            if (processingPkt.src_ip.equals(cliIP) && processingPkt.src_port == cliPort) {
                return true;
            } else if (processingPkt.dst_ip.equals(cliIP) && processingPkt.dst_port == cliPort) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void processPacket() {
        reqStream.put(processingPkt);
        resStream.put(processingPkt);
        if (reqStream.getIOStreamLen() > 0) {
            record(reqStream);
        }
        if (resStream.getIOStreamLen() > 0) {
            record(resStream);
        }
    }

    @Override
    protected boolean isContinue(boolean haveRelPkt) {
        if (haveRelPkt) {
            if (processingPkt.fin) {
                if (pasvFinIP == null) {
                    pasvFinIP = processingPkt.dst_ip;
                } else if (processingPkt.src_ip.equals(pasvFinIP)) {
                    lastACK = true;
                }
            } else if (processingPkt.ack && lastACK && (processingPkt.dst_ip.equals(pasvFinIP))) {
                PPHTTP.removeHandled(streamKey);
                System.out.println("--HTTP FIN " + streamKey);
                return false;
            }
        } else {
            long time = System.currentTimeMillis();
            if (time - lastActive > 60000L) { // 60sec
                System.out.println("--HTTP Time out " + streamKey);
                PPHTTP.removeHandled(streamKey);
                return false;
            }
        }
        return true;
    }

    @Override
    protected void endProcess(boolean haveRelPkt) {
        reqStream.flushBuffer();
        resStream.flushBuffer();
        if (reqStream.getIOStreamLen() > 0) {
            record(reqStream);
        }
        if (resStream.getIOStreamLen() > 0) {
            record(resStream);
        }
        Collection<HTTPEvent> values = events.values();
        ArrayList<HTTPEvent> AL = new ArrayList<HTTPEvent>(values);
        for (int i = 0; i < AL.size(); i++) {
            httpEvent(AL.get(i));
        }
        reqWriteFileBuffer.clear();
        resWriteFileBuffer.clear();
    }

    private String getHostDN(String headerStr) {
        int f = headerStr.indexOf("Host:");
        if (f < 0) {
            return null;
        }
        f += 6;
        int r = headerStr.indexOf("\r\n", f);
        return headerStr.substring(f, r);
    }

    private String getReqPath(String headerStr) {
        int f = headerStr.indexOf("/");
        if (f < 0) {
            return null;
        }
        int r = headerStr.indexOf(" ", f);
        return headerStr.substring(f, r);
    }

    private String getRferer(String headerStr) {
        int f = headerStr.indexOf("Referer:");
        if (f < 0) {
            return null;
        }
        f += 9;
        int r = headerStr.indexOf("\r\n", f);
        return headerStr.substring(f, r);
    }

    private String getContentType(String headerStr) {
        int f = headerStr.indexOf("t-Type:");
        if (f < 0) {
            return null;
        }
        f += 8;
        int r = headerStr.indexOf("\r\n", f);
        return headerStr.substring(f, r);
    }

    private int getContentLength(String headerStr) {
        int f = headerStr.indexOf("t-Length:");
        if (f < 0) {
            return -1;
        }
        f += 10;
        int r = headerStr.indexOf("\r\n", f);
        return Integer.valueOf(headerStr.substring(f, r));
    }

    private boolean isChunked(String headerStr) {
        if (headerStr.contains("Transfer-Encoding: chunked")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isGzip(String headerStr) {
        if (headerStr.contains("Content-Encoding: gzip")) {
            return true;
        } else {
            return false;
        }
    }

    private void record(HTTPStream ioStream) {
        List<TCPPacket> ioStreamPL = ioStream.getIOStream();
        TCPPacket tp;
        String fileName;
        String str;
        int r;
        while (ioStreamPL.size() > 0) {
            tp = ioStreamPL.remove(0);
            if (ioStream.isHostStream()) {
                for (int i = 0; i < tp.data.length; i++) {
                    reqWriteFileBuffer.add(tp.data[i]);
                }
            } else {
                for (int i = 0; i < tp.data.length; i++) {
                    resWriteFileBuffer.add(tp.data[i]);
                }
            }
        }
        if (reqWriteFileBuffer.size() > 0) {
            if (reqHeaderRxing) {
                byte[] BA = new byte[reqWriteFileBuffer.size()];
                Iterator<Byte> it = reqWriteFileBuffer.iterator();
                for (int i = 0; i < BA.length; i++) {
                    BA[i] = it.next();
                }
                str = new String(BA);
                r = str.indexOf("\r\n\r\n");
                if (r > 0) {
                    reqCount++;
                    fileName = recFileNamePrefix + "REQ" + reqCount + "-#Header" + ".httplog";
                    reqHeaderStr = str.substring(0, r + 4);
                    reqContentLength = getContentLength(reqHeaderStr);
                    reqIsChunked = isChunked(reqHeaderStr);
                    String eventKey = recFileNamePrefix + reqCount;
                    if (!events.containsKey(eventKey)) {
                        events.put(eventKey, new HTTPEvent());
                    }
                    HTTPEvent HE = events.get(eventKey);
                    HE.filePathPrefix = recFileNamePrefix + reqCount;
                    HE.cliIP = cliIP.getHostAddress();
                    HE.hostIP = hostIP.getHostAddress();
                    HE.HostDN = getHostDN(reqHeaderStr);
                    HE.reqPath = getReqPath(reqHeaderStr);
                    HE.referer = getRferer(reqHeaderStr);
                    HE.time = sTime;
                    events.put(eventKey, HE);
                    writeFile(fileName, reqWriteFileBuffer.subList(0, r + 4));
                    reqWriteFileBuffer.subList(0, r + 4).clear();
                    if (reqContentLength > 0 || reqIsChunked) {
                        reqHeaderRxing = false;
                    }
                }
            } else {
                fileName = recFileNamePrefix + "REQ" + reqCount + "-Content" + ".httplog";
                if (reqContentLength > 0) {
                    if (reqWriteFileBuffer.size() < reqContentLength) {
                        reqContentLength -= reqWriteFileBuffer.size();
                        writeFile(fileName, reqWriteFileBuffer);
                        reqWriteFileBuffer.clear();
                    } else {
                        writeFile(fileName, reqWriteFileBuffer.subList(0, (int) reqContentLength));
                        reqWriteFileBuffer.subList(0, (int) reqContentLength).clear();
                        reqHeaderRxing = true;
                    }
                } else if (reqIsChunked) {
                    byte[] BA = new byte[reqWriteFileBuffer.size()];
                    for (int i = 0; i < BA.length; i++) {
                        BA[i] = reqWriteFileBuffer.get(i);
                    }
                    byte[] chunkEnd = {'\r', '\n', '0', '\r', '\n', '\r', '\n'};
                    int idx = indexOf(BA, chunkEnd);
                    if (idx > 0) {
                        writeFile(fileName, reqWriteFileBuffer.subList(0, idx + 7));
                        reqWriteFileBuffer.subList(0, idx + 7).clear();
                        reqHeaderRxing = true;
                    } else {
                        writeFile(fileName, reqWriteFileBuffer);
                        reqWriteFileBuffer.clear();
                    }
                }
            }
        }
        if (resWriteFileBuffer.size() > 0) {
            if (resHeaderRxing) {
                byte[] BA = new byte[resWriteFileBuffer.size()];
                Iterator<Byte> it = resWriteFileBuffer.iterator();
                for (int i = 0; i < BA.length; i++) {
                    BA[i] = it.next();
                }
                str = new String(BA);
                r = str.indexOf("\r\n\r\n");
                if (r > 0) {
                    resCount++;
                    fileName = recFileNamePrefix + "RES" + resCount + "-#Header" + ".httplog";
                    resHeaderStr = str.substring(0, r + 4);
                    resContentLength = getContentLength(resHeaderStr);
                    resIsChunked = isChunked(resHeaderStr);
                    resIsGzip = isGzip(resHeaderStr);
                    String eventKey = recFileNamePrefix + resCount;
                    if (!events.containsKey(eventKey)) {
                        events.put(eventKey, new HTTPEvent());
                    }
                    HTTPEvent HE = events.get(eventKey);
                    HE.contentType = getContentType(resHeaderStr);
                    HE.isChunked = resIsChunked;
                    HE.isGZip = resIsGzip;
                    writeFile(fileName, resWriteFileBuffer.subList(0, r + 4));
                    resWriteFileBuffer.subList(0, r + 4).clear();
                    if (resContentLength > 0 || resIsChunked) {
                        resHeaderRxing = false;
                    }
                }
            } else {
                fileName = recFileNamePrefix + "RES" + resCount + "-Content" + ".httplog";
                if (resContentLength > 0) {
                    if (resWriteFileBuffer.size() < resContentLength) {
                        resContentLength -= resWriteFileBuffer.size();
                        writeFile(fileName, resWriteFileBuffer);
                        resWriteFileBuffer.clear();
                    } else {
                        writeFile(fileName, resWriteFileBuffer.subList(0, (int) resContentLength));
                        resWriteFileBuffer.subList(0, (int) resContentLength).clear();
                        resHeaderRxing = true;
                    }
                } else if (resIsChunked) {
                    byte[] BA = new byte[resWriteFileBuffer.size()];
                    for (int i = 0; i < BA.length; i++) {
                        BA[i] = resWriteFileBuffer.get(i);
                    }
                    byte[] chunkEnd = {'\r', '\n', '0', '\r', '\n', '\r', '\n'};
                    int idx = indexOf(BA, chunkEnd);
                    if (idx > 0) {
                        writeFile(fileName, resWriteFileBuffer.subList(0, idx + 7));
                        resWriteFileBuffer.subList(0, idx + 7).clear();
                        resHeaderRxing = true;
                    } else {
                        writeFile(fileName, resWriteFileBuffer);
                        resWriteFileBuffer.clear();
                    }
                }
            }
        }
    }

    private void writeFile(String fileName, List<Byte> writeBL) {
        File f = new File(recFilePath);
        if (!f.exists() || !f.isDirectory()) {
            f.mkdirs();
        }
        f = new File(recFilePath + fileName);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(ProtocolParserHTTPRecorder.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            FileOutputStream FOS = new FileOutputStream(f, true);
            Iterator<Byte> it = writeBL.iterator();
            byte[] BA = new byte[writeBL.size()];
            for (int i = 0; i < BA.length; i++) {
                BA[i] = it.next();
            }
            FOS.write(BA);
            FOS.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ProtocolParserHTTPRecorder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ProtocolParserHTTPRecorder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void httpEvent(HTTPEvent HE) {
        List<String> colL = new ArrayList<String>();
        colL.add("ifID");
        colL.add("time");
        colL.add("eventID");
        colL.add("clientIP");
        colL.add("hostIP");
        colL.add("hostDN");
        colL.add("reqPath");
        colL.add("referer");
        colL.add("contentType");
        colL.add("isChunked");
        colL.add("isGzip");
        List<String> valL = new ArrayList<String>();
        valL.add("\'testIf\'");
        valL.add(String.valueOf(HE.time));
        valL.add("\'" + HE.filePathPrefix + "\'");
        valL.add("\'" + HE.cliIP + "\'");
        valL.add("\'" + HE.hostIP + "\'");
        valL.add("\'" + HE.HostDN + "\'");
        valL.add("\'" + HE.reqPath + "\'");
        valL.add("\'" + HE.referer + "\'");
        valL.add("\'" + HE.contentType + "\'");
        if (HE.isChunked) {
            valL.add("1");
        } else {
            valL.add("0");
        }
        if (HE.isGZip) {
            valL.add("1");
        } else {
            valL.add("0");
        }
        SQLConnector sc = SQLConnector.getInstance();
        sc.incert("http", colL, valL);
    }

    private int indexOf(byte[] data, byte[] pattern) {
        int[] failure = computeFailure(pattern);

        int j = 0;
        if (data.length == 0) {
            return -1;
        }

        for (int i = 0; i < data.length; i++) {
            while (j > 0 && pattern[j] != data[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == data[i]) {
                j++;
            }
            if (j == pattern.length) {
                return i - pattern.length + 1;
            }
        }
        return -1;
    }

    /**
     * Computes the failure function using a boot-strapping process, where the
     * pattern is matched against itself.
     */
    private int[] computeFailure(byte[] pattern) {
        int[] failure = new int[pattern.length];

        int j = 0;
        for (int i = 1; i < pattern.length; i++) {
            while (j > 0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }

        return failure;
    }
}
