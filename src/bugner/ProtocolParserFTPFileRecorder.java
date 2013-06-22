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
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class ProtocolParserFTPFileRecorder extends ProtocolParser {

    BugNERISGUI GUI;
    InetAddress dataIP;
    int dataPort;
    boolean toDataIPTarget;
    boolean toDataIP;
    String filePath;
    String fileName;
    Date startTime;
    TCPPacket processingPkt;
    long nextSeq;
    List<TCPPacket> reorderBuffer;
    InetAddress pasvFinIP;
    boolean lastACK;
    long lastActive;
    int testCT = 0;
    TCPStreamRecordBuffer buffer;
    InetAddress cliIP, hostIP;
    boolean upload;

    public ProtocolParserFTPFileRecorder(PacketPool pktPool, Integer TIDKey, InetAddress dataIP, int dataPort, boolean toDataIPTarget, String filePath, String fileName, Date startTime, InetAddress cliIP, InetAddress hostIP, boolean upload, BugNERISGUI GUI) {
        super(pktPool, TIDKey);
        this.dataIP = dataIP;
        this.dataPort = dataPort;
        this.toDataIPTarget = toDataIPTarget;
        this.filePath = filePath;
        this.fileName = fileName;
        this.startTime = startTime;
        nextSeq = 0;
        reorderBuffer = new ArrayList<TCPPacket>();
        lastACK = false;
        lastActive = System.currentTimeMillis();
        buffer = new TCPStreamRecordBuffer();
        this.cliIP = cliIP;
        this.hostIP = hostIP;
        this.upload = upload;
    }

    @Override
    protected boolean isRelative(Packet p) {
        if (p instanceof TCPPacket) {
            processingPkt = (TCPPacket) p;
            if (processingPkt.src_ip.equals(dataIP) && processingPkt.src_port == dataPort) {
                toDataIP = false;
                return true;
            } else if (processingPkt.dst_ip.equals(dataIP) && processingPkt.dst_port == dataPort) {
                toDataIP = true;
                return true;
            }
        }
        return false;
    }

    @Override
    protected void processPacket() {
        lastActive = System.currentTimeMillis();
        if (toDataIP == toDataIPTarget) {
            if (processingPkt.data.length > 0) {
                buffer.put(processingPkt);
            }
        } else {
            if (processingPkt.ack) {
                buffer.setAckedSeq(processingPkt.ack_num);
                if (buffer.getOOOBufLen() > 100) {
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
                if (buffer.getOOOBufLen() > 0) {
                    recordFile();
                }
                fileEvent();
                return false;
            }
        } else {
            long time = System.currentTimeMillis();
            if (time - lastActive > 300000L) {
                if (buffer.getOOOBufLen() > 0) {
                    recordFile();
                }
                fileEvent();
                return false;
            }
        }
        return true;
    }

    @Override
    protected void endProcess(boolean haveRelPkt) {
    }

    private void recordFile() {
        DateFormat dateFormat = new SimpleDateFormat("[yyyy-MM-dd-HH-mm-ss]");
        String timeId = dateFormat.format(startTime);
        File recordFile = new File(filePath);
        if (!recordFile.isDirectory()) {
            recordFile.mkdirs();
        }
        recordFile = new File(filePath + timeId + fileName);
        if (!recordFile.exists()) {
            try {
                recordFile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(ProtocolParserFTPFileRecorder.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            FileOutputStream FOS = new FileOutputStream(recordFile, true);
            byte[] dataBA = buffer.getIOData();
            FOS.write(dataBA);
            FOS.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ProtocolParserFTPFileRecorder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ProtocolParserFTPFileRecorder.class.getName()).log(Level.SEVERE, null, ex);
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
        valL.add("\"testIf\"");
        valL.add(String.valueOf(System.currentTimeMillis()));
        valL.add("\"" + cliIP.getHostAddress() + "\"");
        valL.add("\"" + hostIP.getHostAddress() + "\"");
        String infoStr = "\"";
        if (upload) {
            infoStr += "upload,";
        } else {
            infoStr += "download,";
        }
        infoStr += filePath + fileName;
        List<Long> lostSegInfo = buffer.getLostSegInfo();
        for (int i = 0; i < lostSegInfo.size(); i += 2) {
            infoStr += "," + lostSegInfo.get(i) + "," + lostSegInfo.get(i + 1);
        }
        infoStr += "\"";
        valL.add(infoStr);
        /*
        try {
            SQLConnector sc = SQLConnector.getInstance();
            sc.incert("ftp", colL, valL);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ProtocolParserFTPCommandParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(ProtocolParserFTPCommandParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        */
    }
}