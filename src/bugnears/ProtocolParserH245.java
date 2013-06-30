/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugnears;

import jpcap.packet.Packet;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class ProtocolParserH245 extends ProtocolParser {

    public ProtocolParserH245(PacketPool pktPool, Integer TIDKey) {
        super(pktPool, TIDKey);
    }

    @Override
    protected boolean isRelative(Packet p) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void processPacket() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean isContinue(boolean haveRelPkt) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void endProcess(boolean haveRelPkt) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
