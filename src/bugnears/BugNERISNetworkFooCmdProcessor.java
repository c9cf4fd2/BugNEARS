/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugnears;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class BugNERISNetworkFooCmdProcessor implements Runnable {

    Socket skt;
    ObjectInputStream sktReader;
    SocketCMD cmd;

    BugNERISNetworkFooCmdProcessor(Socket skt) {
        this.skt = skt;
        cmd = null;
        try {
            sktReader = new ObjectInputStream(skt.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(BugNERISNetworkFooCmdProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void run() {
        System.out.println("connection accepted");
        do {
            try {
                Object o = sktReader.readObject();
                if (o != null) {
                    if (o instanceof SocketCMD) {
                        cmd = (SocketCMD) o;
                        if (cmd.cmd.compareTo("getfile") == 0) {
                            System.out.println(cmd.cmd);
                            System.out.println(cmd.parameters[0]);
                            System.out.println(cmd.parameters[1]);
                            Socket fileSkt = new Socket(skt.getInetAddress(), Integer.parseInt(cmd.parameters[1]));
                            File f = new File(cmd.parameters[0]);
                            OutputStream out = fileSkt.getOutputStream();
                            InputStream in = new FileInputStream(f);
                            IOUtils.copy(in, out);
                            fileSkt.close();
                            in.close();
                            out.close();
                            skt.close();
                            skt = null;
                            break;
                        } else if (cmd.cmd.compareTo("bye") == 0) {
                            skt.close();
                            skt = null;
                            break;
                        }
                    } else {
                        Thread.sleep(500);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(BugNERISNetworkFooCmdProcessor.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(BugNERISNetworkFooCmdProcessor.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(BugNERISNetworkFooCmdProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
        } while (skt != null);
    }
}
