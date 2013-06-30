/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugnears;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class BugNEARSASSocketClient {

    private Socket cliSkt;
    private ObjectOutputStream sktWriter;
    private ServerSocket serSkt;
    int portNum;

    public BugNEARSASSocketClient() {
        cliSkt = null;
        portNum = ((new Random()).nextInt(100) * 100) + 10000;
    }

    public void connectIRS(String hostAddr, int port) {
        try {
            if (cliSkt != null) {
                if (cliSkt.isConnected()) {
                    cliSkt.close();
                }
            }
            cliSkt = new Socket(hostAddr, port);
        } catch (UnknownHostException ex) {
            Logger.getLogger(BugNEARSASSocketClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BugNEARSASSocketClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void getFileFromIS(String ISID, String filePath) {
        cliSkt = connectToIS(ISID);
        if (cliSkt != null) {
            SocketCMD cmd;
            try {
                listenFileSocket();
                sktWriter = new ObjectOutputStream(cliSkt.getOutputStream());
                cmd = new SocketCMD("getfile", new String[]{filePath, String.valueOf(portNum)});
                sktWriter.writeObject(cmd);
                Socket fileSkt = getFileSocket();
                if (fileSkt != null) {
                    File f = new File(filePath);
                    String fileName = f.getName();
                    filePath = "./temp/ftp/" + fileName + "/";
                    f = new File(filePath);
                    if (!f.isDirectory()) {
                        f.mkdirs();
                    }
                    f = new File(filePath + fileName);
                    InputStream in = fileSkt.getInputStream();
                    OutputStream out = new FileOutputStream(f);
                    IOUtils.copy(in, out);
                    fileSkt.close();
                    in.close();
                    out.close();
                    cliSkt.close();
                    cliSkt = null;
                }
            } catch (IOException ex) {
                Logger.getLogger(BugNEARSASSocketClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private Socket connectToIS(String ISID) {
        Socket skt = null;
        try {
            skt = new Socket("127.0.0.1", 15896);////
        } catch (UnknownHostException ex) {
            Logger.getLogger(BugNEARSASSocketClient.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BugNEARSASSocketClient.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return skt;
    }

    private void listenFileSocket() {
        for (;;) {
            try {
                portNum++;
                serSkt = new ServerSocket(portNum);
            } catch (IOException ex) {
                Logger.getLogger(BugNEARSASSocketClient.class
                        .getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (serSkt != null) {
                    break;
                }
            }
        }
    }

    private Socket getFileSocket() {
        Socket fileSkt = null;
        try {
            serSkt.setSoTimeout(30000);
            fileSkt = serSkt.accept();
            serSkt.close();
        } catch (IOException ex) {
            Logger.getLogger(BugNEARSASSocketClient.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            serSkt = null;
            return fileSkt;
        }
    }
}
