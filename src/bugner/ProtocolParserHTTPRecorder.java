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
    //StreamRecorder
    InetAddress pasvFinIP;
    boolean lastACK;
    long lastActive;
    String streamKey;
    //test
    boolean first;
    long sTime;

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
        lastActive = System.currentTimeMillis();
        sTime = lastActive;
        //
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
        if (reqStream.getIOStreamLen()> 0) {
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
                return false;
            }
        } else {
            long time = System.currentTimeMillis();
            if (time - lastActive > 20000L) { // 20sec
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
        List<Byte> ioStreamBL = ioStream.getIOStream();
        String fileName = "[" + sTime + "]" + streamKey + ioStream.getStreamName() + ".httplog";
        writeFile(fileName, ioStreamBL);
        ioStreamBL.clear();
    }

    private void writeFile(String fileName, List<Byte> ioStreamBL) {
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
            byte[] WTFBA = new byte[ioStreamBL.size()];
            for (int i = 0; i < WTFBA.length; i++) {
                WTFBA[i]=ioStreamBL.get(i);
            }
            FileOutputStream FOS = new FileOutputStream(f, true);
            FOS.write(WTFBA);
            FOS.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ProtocolParserHTTPRecorder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ProtocolParserHTTPRecorder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
