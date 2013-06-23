/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class ProtocolParserHTTPRecorder extends ProtocolParser {

    InetAddress cliIP;
    int cliPort;
    InetAddress hostIP;
    int hostPort;
    String recFilePath;
    String recFileNamePrefix;
    ProtocolParserHTTP PPHTTP;
    //
    TCPPacket processingPkt;
    boolean toHost;
    //
    HTTPStream reqStream;
    HTTPStream resStream;
    //
    InetAddress pasvFinIP;
    boolean lastACK;
    long lastActive;
    String streamKey;
    //test
    HTTPEvent event;
    boolean first;
    long sTime;
    int reqHeaderStartIdx;
    boolean reqHeaderRxing;
    int resHeaderStartIdx;
    boolean resHeaderRxing;
    int reqIdx;
    int resIdx;

    public ProtocolParserHTTPRecorder(PacketPool pktPool, Integer TIDKey, InetAddress cliIP, int cliPort, InetAddress hostIP, int hostPort, String recFilePath, ProtocolParserHTTP PPHTTP, TCPPacket firstPkt) {
        super(pktPool, TIDKey);
        this.cliIP = cliIP;
        this.cliPort = cliPort;
        this.hostIP = hostIP;
        this.hostPort = hostPort;
        this.recFilePath = recFilePath;
        this.PPHTTP = PPHTTP;
        streamKey = cliIP.getHostAddress() + cliPort + hostIP.getHostAddress() + "80";
        reqStream = new HTTPStream(true);
        resStream = new HTTPStream(false);
        sTime = System.currentTimeMillis();
        lastActive = sTime;
        event = new HTTPEvent();
        event.cliIP = cliIP.getHostAddress();
        event.hostIP = hostIP.getHostAddress();
        event.time = sTime;
        event.filePathPrefix = sTime + "-" + streamKey + "-";
        //
        reqHeaderStartIdx = 0;
        reqHeaderRxing = false;
        resHeaderStartIdx = 0;
        resHeaderRxing = false;
        reqIdx = 0;
        resIdx = 0;
    }

    @Override
    protected boolean isRelative(Packet p) {
        if (p instanceof TCPPacket) {
            processingPkt = (TCPPacket) p;
            if (processingPkt.src_ip.equals(cliIP) && processingPkt.src_port == cliPort) {
                toHost = true;
                return true;
            } else if (processingPkt.dst_ip.equals(cliIP) && processingPkt.dst_port == cliPort) {
                toHost = false;
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
            if (time - lastActive > 20000L) { // 20sec
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

    private void record(HTTPStream ioStream) {
        List<TCPPacket> ioStreamPL = ioStream.getIOStream();
        String fileName = event.filePathPrefix + ioStream.getStreamName() + ".httplog";
        writeFile(fileName, ioStreamPL);
        ioStreamPL.clear();
    }

    private void writeFile(String fileName, List<TCPPacket> ioStreamPL) {
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
            for (int i = 0; i < ioStreamPL.size(); i++) {
                FOS.write(ioStreamPL.get(i).data);
            }
            FOS.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ProtocolParserHTTPRecorder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ProtocolParserHTTPRecorder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
