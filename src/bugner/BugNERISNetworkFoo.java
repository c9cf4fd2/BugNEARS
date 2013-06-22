/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugner;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class BugNERISNetworkFoo implements Runnable {

    BugNERISGUI GUI;
    ServerSocket sskt;
    int listenPort;

    public BugNERISNetworkFoo(int listenPort, BugNERISGUI GUI) {
        this.GUI = GUI;
        this.listenPort = listenPort;
        try {
            sskt = new ServerSocket(listenPort);
        } catch (IOException ex) {
            Logger.getLogger(BugNERISNetworkFoo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        GUI.addText("listen cmd on "+listenPort);
        for (;;) {
            Socket clientSocket;
            try {
                clientSocket = sskt.accept();
                BugNERISNetworkFooCmdProcessor cmdProc = new BugNERISNetworkFooCmdProcessor(clientSocket, GUI);
                Thread t = new Thread(cmdProc);
                t.start();
            } catch (IOException ex) {
                Logger.getLogger(BugNERISNetworkFoo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
