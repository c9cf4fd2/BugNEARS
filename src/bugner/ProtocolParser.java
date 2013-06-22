/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugner;

import java.util.logging.Level;
import java.util.logging.Logger;
import jpcap.packet.Packet;

/**
 *
 * @author bugVM
 */
public abstract class ProtocolParser implements Runnable {

    PacketPool pktPool;
    Integer TIDKey;
    boolean goon;

    public ProtocolParser(PacketPool pktPool, Integer TIDKey) {
        this.pktPool = pktPool;
        this.TIDKey = TIDKey;
        this.goon = true;
    }

    @Override
    public void run() {
        Packet p;
        while (goon) {
            p = pktPool.getPacket(TIDKey);
            if (p != null) {
                if (isRelative(p)) {
                    processPacket();
                    goon = isContinue(true);
                    if (!goon) {
                        endProcess(true);
                    }
                }
            } else {
                goon = isContinue(false);
                if (!goon) {
                    endProcess(false);
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ProtocolParser.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        pktPool.removeKey(TIDKey);
    }

    protected abstract boolean isRelative(Packet p);

    protected abstract void processPacket();

    protected abstract boolean isContinue(boolean haveRelPkt);

    protected abstract void endProcess(boolean haveRelPkt);
}
