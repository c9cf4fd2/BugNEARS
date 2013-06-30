/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugnears;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpcap.JpcapCaptor;
import jpcap.PacketReceiver;
import jpcap.packet.Packet;

/**
 *
 * @author c9cf4fd2AtNTUT
 */
public class PacketCapture implements Runnable {

    private int ifNum;
    private JpcapCaptor captor;
    private String filterStr;
    private boolean goon;
    final PacketPool pktPool;
    int ct = 0;
    PacketRxer pktRxer = new PacketRxer();

    private class PacketRxer implements PacketReceiver {

        @Override
        public void receivePacket(Packet packet) {
            pktPool.addPacket(packet);
        }
    }

    public PacketCapture(int ifNum, String filterStr, PacketPool pktPool) {
        this.ifNum = ifNum;
        this.filterStr = filterStr;
        this.pktPool = pktPool;
        this.goon = true;
    }

    @Override
    public void run() {
        try {
            captor = JpcapCaptor.openDevice(JpcapCaptor.getDeviceList()[ifNum], 65535, true, 5000);
            captor.setFilter(filterStr, true);
        } catch (IOException ex) {
            Logger.getLogger(PacketCapture.class.getName()).log(Level.SEVERE, null, ex);
        }
        while(goon) {
        captor.loopPacket(100000, pktRxer);
        }
    }

    public int getInterfaceNumber() {
        return ifNum;
    }

    public String getFielterString() {
        return filterStr;
    }

    public void CaptureEnd() {
        goon = false;
    }
}