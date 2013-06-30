/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugnears;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
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
public class ProtocolParserFTPCommandParser extends ProtocolParser {

    String streamKey;
    ProtocolParserFTP classfier;
    TCPPacket processingPkt;
    InetAddress dataIP;
    int dataPort;
    boolean pasvMode;
    InetAddress pasvFinIP;
    boolean lastACK;
    long lastActive;
    String user;
    String password;
    InetAddress cliIP, hostIP;

    public ProtocolParserFTPCommandParser(PacketPool pktPool, Integer TIDKey, String streamKey, ProtocolParserFTP classfier, InetAddress cliIP, InetAddress hostIP) {
        super(pktPool, TIDKey);
        this.streamKey = streamKey;
        this.classfier = classfier;
        this.pasvFinIP = null;
        this.lastACK = false;
        lastActive = System.currentTimeMillis();
        this.cliIP = cliIP;
        this.hostIP = hostIP;
    }

    @Override
    protected boolean isRelative(Packet p) {
        if (p instanceof TCPPacket) {
            processingPkt = (TCPPacket) p;
            String key;
            InetAddress tcliIP;
            int cliPort;
            InetAddress thostIP;
            if (processingPkt.src_port == 21) {
                tcliIP = processingPkt.dst_ip;
                cliPort = processingPkt.dst_port;
                thostIP = processingPkt.src_ip;
            } else if (processingPkt.dst_port == 21) {
                tcliIP = processingPkt.src_ip;
                cliPort = processingPkt.src_port;
                thostIP = processingPkt.dst_ip;
            } else {
                return false;
            }
            key = tcliIP.getHostAddress() + cliPort + thostIP.getHostAddress() + "21";
            if (key.equals(streamKey)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void processPacket() {
        lastActive = System.currentTimeMillis();
        if (processingPkt.data.length > 0) {
            String cmdStr = new String(processingPkt.data);
            if (cmdStr.contains("PORT")) {
                activeMode(cmdStr);
            } else if (cmdStr.contains("227")) {
                pasiveMode(cmdStr);
            } else if (cmdStr.contains("STOR")) {
                upload(cmdStr);
            } else if (cmdStr.contains("RETR")) {
                download(cmdStr);
            } else if (cmdStr.contains("USER")) {
                user = getUserPass(cmdStr);
            } else if (cmdStr.contains("PASS")) {
                password = getUserPass(cmdStr);
            } else if (cmdStr.contains("230")) {
                loginEvent();
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
                classfier.removeHandled(streamKey);
//                GUI.addText(streamKey + " close - lastACK");
                return false;
            }
        } else {
            long time = System.currentTimeMillis();
            if (time - lastActive > 300000L) {
                classfier.removeHandled(streamKey);
//                GUI.addText(streamKey + " close - timeout");
                return false;
            }
        }
        return true;
    }

    private String getDataConnectionIPStr(String cmdStr) {
        int f = cmdStr.indexOf('(');
        int r = cmdStr.indexOf(')');
        if (f < 0) {
            f = cmdStr.indexOf(' ');
            r = cmdStr.indexOf('\r');
        }
        String tStr = cmdStr.substring(f + 1, r);
        String[] splitStrs = tStr.split(",");
        return splitStrs[0] + "." + splitStrs[1] + "." + splitStrs[2] + "." + splitStrs[3];
    }

    private int getDataConnectionPort(String cmdStr) {
        int f = cmdStr.indexOf('(');
        int r = cmdStr.indexOf(')');
        if (f < 0) {
            f = cmdStr.indexOf(' ');
            r = cmdStr.indexOf('\r');
        }
        String tStr = cmdStr.substring(f + 1, r);
        String[] splitStrs = tStr.split(",");
        return Integer.valueOf(splitStrs[4]) * 256 + Integer.valueOf(splitStrs[5]);
    }

    private String getFileName(String cmdStr) {
        int f = cmdStr.indexOf(" ") + 1;
        int r = cmdStr.indexOf("\r\n", f);
        if (r == -1) {
            r = cmdStr.indexOf("\n", f);
        }
        return cmdStr.substring(f, r).replace("\\", "").replace("/", "");
    }

    private void activeMode(String cmdStr) {
        try {
            dataIP = InetAddress.getByName(getDataConnectionIPStr(cmdStr));
            dataPort = getDataConnectionPort(cmdStr);
            pasvMode = false;
        } catch (UnknownHostException ex) {
            Logger.getLogger(ProtocolParserFTPCommandParser.class.getName()).log(Level.SEVERE, null, ex);
        }
//        GUI.addText("PORT " + dataIP.getHostAddress() + " " + dataPort);////
    }

    private void pasiveMode(String cmdStr) {
        try {
            dataIP = InetAddress.getByName(getDataConnectionIPStr(cmdStr));
            dataPort = getDataConnectionPort(cmdStr);
            pasvMode = true;
        } catch (UnknownHostException ex) {
            Logger.getLogger(ProtocolParserFTPCommandParser.class.getName()).log(Level.SEVERE, null, ex);
        }
//        GUI.addText("PASV " + dataIP.getHostAddress() + " " + dataPort);////

    }

    private void upload(String cmdStr) {
        String fName = getFileName(cmdStr);
//        GUI.addText(fName);
        ProtocolParserFTPRecorder rec = new ProtocolParserFTPRecorder(pktPool, pktPool.getNewKey(TIDKey), cliIP, hostIP, dataPort, pasvMode, "./record/ftp/", fName, System.currentTimeMillis(), true);
        Thread t = new Thread(rec);
        t.start();
    }

    private void download(String cmdStr) {
        String fName = getFileName(cmdStr);
//        GUI.addText(fName);
        ProtocolParserFTPRecorder rec = new ProtocolParserFTPRecorder(pktPool, pktPool.getNewKey(TIDKey), cliIP, hostIP, dataPort, pasvMode, "./record/ftp/", fName, System.currentTimeMillis(), false);
        Thread t = new Thread(rec);
        t.start();
    }

    private String getUserPass(String cmdStr) {
        int r = cmdStr.indexOf("\r\n", 5);
        if (r == -1) {
            r = cmdStr.indexOf("\n", 5);
        }
        return cmdStr.substring(5, r);
    }

    private void loginEvent() {
        if (user == null || password == null) {
            return;
        }
        List<String> colL = new ArrayList<String>();
        colL.add("ifID");
        colL.add("time");
        colL.add("clientIP");
        colL.add("hostIP");
        colL.add("info");
        List<String> valL = new ArrayList<String>();
        valL.add("\"testIf\"");
        valL.add(String.valueOf(System.currentTimeMillis()));
        String tcliIP, thostIP;
        if (processingPkt.dst_port == 21) {
            tcliIP = processingPkt.src_ip.getHostAddress();
            thostIP = processingPkt.dst_ip.getHostAddress();
        } else {
            tcliIP = processingPkt.dst_ip.getHostAddress();
            thostIP = processingPkt.src_ip.getHostAddress();
        }
        valL.add("\"" + tcliIP + "\"");
        valL.add("\"" + thostIP + "\"");
        String infoStr = "\"login," + user.length() + "," + user + "," + password.length() + "," + password + "\"";
        valL.add(infoStr);
        SQLConnector sc = SQLConnector.getInstance();
        sc.incert("ftp", colL, valL);
    }

    @Override
    protected void endProcess(boolean haveRelPkt) {
    }
}
