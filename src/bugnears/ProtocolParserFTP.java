/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugnears;

import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;

/**
 *
 * @author bugVM
 */
public class ProtocolParserFTP extends ProtocolParser {

    BugNEARSISGUI GUI;
    Packet processingPkt;
    Map<String, Date> handledTransport = new HashMap<String, Date>();

    public ProtocolParserFTP(PacketPool pktPool, Integer TIDKey, BugNEARSISGUI GUI) {
        super(pktPool, TIDKey);
        this.GUI = GUI;
    }

    @Override
    protected boolean isRelative(Packet p) {
        processingPkt = p;
        return true;
    }

    @Override
    protected synchronized void processPacket() {
        if (processingPkt instanceof TCPPacket) {
            TCPPacket tPkt = (TCPPacket) processingPkt;
            String key = null;
            InetAddress cliIP = null;
            InetAddress hostIP = null;
            if (tPkt.src_port == 21) {
                key = tPkt.dst_ip.getHostAddress() + tPkt.dst_port + tPkt.src_ip.getHostAddress() + "21";
                cliIP = tPkt.dst_ip;
                hostIP = tPkt.src_ip;
            } else if (tPkt.dst_port == 21) {
                key = tPkt.src_ip.getHostAddress() + tPkt.src_port + tPkt.dst_ip.getHostAddress() + "21";
                cliIP = tPkt.src_ip;
                hostIP = tPkt.dst_ip;
            }
            if (key != null) {
                if (handledTransport.get(key) == null) {
                    handledTransport.put(key, new Date());
                    if (GUI != null) {
                        GUI.addText("---FTP " + key);
                    }
                    ProtocolParserFTPCommandParser ppFTPCP = new ProtocolParserFTPCommandParser(pktPool, pktPool.getNewKey(TIDKey), key, this, cliIP, hostIP);
                    Thread t = new Thread(ppFTPCP);
                    t.start();
                    //return;
                }
            }
        }
    }

    @Override
    protected boolean isContinue(boolean haveRelPkt) {
        return true;
    }

    public synchronized void removeHandled(String key) {
        handledTransport.remove(key);
    }

    @Override
    protected void endProcess(boolean haveRelPkt) {
    }
}
