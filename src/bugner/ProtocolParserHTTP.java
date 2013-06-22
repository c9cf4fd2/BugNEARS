/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugner;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class ProtocolParserHTTP extends ProtocolParser {
    BugNERISGUI GUI;
    TCPPacket processingPkt;
    String processingKey;
    Map<String, String> handled;

    public ProtocolParserHTTP(PacketPool pktPool, Integer TIDKey, BugNERISGUI GUI) {
        super(pktPool, TIDKey);
        this.GUI = GUI;
        handled = new HashMap<String, String>();
    }

    @Override
    protected boolean isRelative(Packet p) {
        if (p instanceof TCPPacket) {
            processingPkt = (TCPPacket) p;
            if (processingPkt.dst_port == 80) {
                processingKey = processingPkt.src_ip.getHostAddress() + processingPkt.src_port + processingPkt.dst_ip.getHostAddress() + "80";
                return true;
            }
        }
        return false;
    }

    @Override
    protected void processPacket() {
        try {
            String dataStr = new String(processingPkt.data,"UTF-8");
            String firstLine = "";
            int r = dataStr.indexOf("\r\n");
            if (r > 0) {
                firstLine = dataStr.substring(0, r);
            }
            if (!handled.containsKey(processingKey)) {
                if (firstLine.contains("HTTP/")) {
                    handled.put(processingKey, "");
                    ProtocolParserHTTPRecorder rec = new ProtocolParserHTTPRecorder(pktPool, pktPool.getNewKey(TIDKey), processingPkt.src_ip, processingPkt.src_port, processingPkt.dst_ip, processingPkt.dst_port, "./temp/http/", this, processingPkt);
                    Thread t = new Thread(rec);
                    GUI.addText("---HTTP " + processingKey);
                    t.start();
                }
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ProtocolParserHTTP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected boolean isContinue(boolean haveRelPkt) {
        return true;
    }

    void removeHandled(String streamKey) {
        handled.remove(streamKey);
    }

    @Override
    protected void endProcess(boolean haveRelPkt) {
    }
}
