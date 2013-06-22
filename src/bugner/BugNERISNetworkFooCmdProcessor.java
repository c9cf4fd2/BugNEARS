/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class BugNERISNetworkFooCmdProcessor implements Runnable {

    Socket skt;
    BufferedReader sktReader;
    BugNERISGUI GUI;
    String cmd;

    BugNERISNetworkFooCmdProcessor(Socket skt, BugNERISGUI GUI) {
        this.skt = skt;
        this.GUI = GUI;
        cmd = null;
        try {
            sktReader = new BufferedReader(new InputStreamReader(skt.getInputStream()));
        } catch (IOException ex) {
            Logger.getLogger(BugNERISNetworkFooCmdProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void run() {
        for (;;) {
            try {
                cmd = sktReader.readLine();
                if (cmd != null) {
                    GUI.addText(cmd);
                    if (cmd.compareTo("bye") == 0) {
                        break;
                    }
                } else {
                    Thread.sleep(500);
                }
            } catch (IOException ex) {
                Logger.getLogger(BugNERISNetworkFooCmdProcessor.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(BugNERISNetworkFooCmdProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
