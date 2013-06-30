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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class ProtocolParserFTPRecorder extends ProtocolParser {

    InetAddress cliIP, hostIP, dataIP;
    int dataPort;
    boolean fileStreamToDataIP;
    boolean pktToDataIP;
    boolean pasvMode;
    String filePath;
    String fileName;
    long startTime;
    boolean upload;
    InetAddress fileStreamDstIP;
    int fileStreamDstPort;
    //
    TCPPacket processingPkt;
    InetAddress pasvFinIP;
    boolean lastACK;
    long lastActive;
    int testCT = 0;
    TCPStreamReorderBuffer buffer;
    String streamKey;

    public ProtocolParserFTPRecorder(PacketPool pktPool, Integer TIDKey, InetAddress cliIP, InetAddress hostIP, int dataPort, boolean pasvMode, String filePath, String fileName, long startTime, boolean upload) {
        super(pktPool, TIDKey);
        this.cliIP = cliIP;
        this.hostIP = hostIP;
        this.dataPort = dataPort;
        this.upload = upload;
        this.filePath = filePath;
        this.fileName = fileName;
        this.startTime = startTime;
        streamKey = cliIP.getHostAddress() + hostIP.getHostAddress() + dataPort;
        if (pasvMode) {
            dataIP = hostIP;
            if (upload) {
                fileStreamToDataIP = true;
            } else {
                fileStreamToDataIP = false;
            }
        } else {
            dataIP = cliIP;
            if (upload) {
                fileStreamToDataIP = false;
            } else {
                fileStreamToDataIP = true;
            }
        }
        buffer = new TCPStreamReorderBuffer();
        lastACK = false;
        lastActive = System.currentTimeMillis();
    }

    @Override
    protected boolean isRelative(Packet p) {
        if (p instanceof TCPPacket) {
            processingPkt = (TCPPacket) p;
            if (processingPkt.src_ip.equals(dataIP) && processingPkt.src_port == dataPort) {
                pktToDataIP = false;
                return true;
            } else if (processingPkt.dst_ip.equals(dataIP) && processingPkt.dst_port == dataPort) {
                pktToDataIP = true;
                return true;
            }
        }
        return false;
    }

    @Override
    protected void processPacket() {
        lastActive = System.currentTimeMillis();
        if (pktToDataIP == fileStreamToDataIP) {
            if (processingPkt.data.length > 0) {
                buffer.put(processingPkt);
            }
        } else {
            if (processingPkt.ack) {
                buffer.setAckedSeq(processingPkt.ack_num);
                if (buffer.getOOOBufLen() > 4096) {
                    recordFile();
                }
            }
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
                return false;
            }
        } else {
            long time = System.currentTimeMillis();
            if (time - lastActive > 300000L) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void endProcess(boolean haveRelPkt) {
        if (buffer.getOOOBufLen() > 0) {
            recordFile();
        }
        fileEvent();
    }

    private void recordFile() {
        File recordFile = new File(filePath);
        if (!recordFile.isDirectory()) {
            recordFile.mkdirs();
        }
        recordFile = new File(filePath + startTime + "-" + streamKey + "-" + fileName);
        if (!recordFile.exists()) {
            try {
                recordFile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(ProtocolParserFTPRecorder.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            FileOutputStream FOS = new FileOutputStream(recordFile, true);
            TCPPacket[] packetBA = buffer.getIOPackets();
            for (int i = 0; i < packetBA.length; i++) {
                FOS.write(packetBA[i].data);
            }
            FOS.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ProtocolParserFTPRecorder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ProtocolParserFTPRecorder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void fileEvent() {
        List<String> colL = new ArrayList<String>();
        colL.add("ifID");
        colL.add("time");
        colL.add("clientIP");
        colL.add("hostIP");
        colL.add("info");
        List<String> valL = new ArrayList<String>();
        valL.add("\'testIf\'");
        valL.add(String.valueOf(startTime));
        valL.add("\'" + cliIP.getHostAddress() + "\'");
        valL.add("\'" + hostIP.getHostAddress() + "\'");
        String infoStr = "\'";
//        if (upload) {
//            infoStr += "upload,";
//        } else {
//            infoStr += "download,";
//        }
        infoStr += filePath + startTime + "-" + streamKey + "-" + fileName;
//        List<Long> lostSegInfo = buffer.getLostSegInfo();
//        for (int i = 0; i < lostSegInfo.size(); i += 2) {
//            infoStr += "," + lostSegInfo.get(i) + "," + lostSegInfo.get(i + 1);
//        }
        infoStr += "\'";
        valL.add(infoStr);
        SQLConnector sc = SQLConnector.getInstance();
        sc.incert("ftp", colL, valL);
    }
}