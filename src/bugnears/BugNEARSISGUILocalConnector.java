/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugnears;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class BugNEARSISGUILocalConnector {

    ServerSocket serSkt;

    public BugNEARSISGUILocalConnector(int listenPort) {
        try {
            serSkt = new ServerSocket(listenPort);
        } catch (IOException ex) {
            Logger.getLogger(BugNEARSISGUILocalConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
