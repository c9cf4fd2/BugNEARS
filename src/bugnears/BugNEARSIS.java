/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugnears;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class BugNEARSIS {

    BugNEARSISGUI GUI;
    BugNERISNetworkFoo networkF;
    private PacketCapture pktCpt;
    private PacketPool pktPool;

    public BugNEARSIS() {
        pktPool = new PacketPool();
        pktCpt = new PacketCapture(2, "arp or ip", pktPool);
        GUI = new BugNEARSISGUI();
        networkF = new BugNERISNetworkFoo(15896, GUI);////
    }

    public void start() {
        Thread tpc = new Thread(pktCpt, "pktCpt");
        tpc.setPriority(Thread.MAX_PRIORITY);
        tpc.start();
        Integer newKey;
        newKey = pktPool.getNewKey();
        ProtocolParserFTP ppf = new ProtocolParserFTP(pktPool, newKey, GUI);
        Thread tppf = new Thread(ppf, "ppf");
        tppf.start();
        newKey = pktPool.getNewKey();
        ProtocolParserHTTP pph = new ProtocolParserHTTP(pktPool, newKey, GUI);
        Thread tpph = new Thread(pph, "pph");
        tpph.start();
        GUI.setVisible(true);
        Thread NFT = new Thread(networkF, "networkF");
        NFT.setDaemon(true);
        NFT.start();
    }
}
