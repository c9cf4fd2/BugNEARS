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

    public BugNEARSASSocketClient() {
        cliSkt = null;
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
            ServerSocket fileSerSkt = null;
            int port = 1000;
            for (;;) {
                try {
                    port++;
                    fileSerSkt = new ServerSocket(port);
                } catch (IOException ex) {
                    Logger.getLogger(BugNEARSASSocketClient.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (fileSerSkt != null) {
                    break;
                }
            }
            try {
                sktWriter = new ObjectOutputStream(cliSkt.getOutputStream());
                SocketCMD cmd = new SocketCMD("getfile", new String[]{filePath, String.valueOf(port)});
                sktWriter.writeObject(cmd);
            } catch (IOException ex) {
                Logger.getLogger(BugNEARSASSocketClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                File f = new File(filePath);
                String name = f.getName();
                f = new File("./temp/ftp/");
                if (!f.isDirectory()) {
                    f.mkdirs();
                }
                f = new File("./temp/ftp/" + name);
                Socket fileSkt = fileSerSkt.accept();
                fileSerSkt.close();
                fileSerSkt = null;
                InputStream in = fileSkt.getInputStream();
                OutputStream out = new FileOutputStream(f);
                IOUtils.copy(in, out);
                fileSkt.close();
                in.close();
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(BugNEARSASSocketClient.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

    private Socket connectToIS(String ISID) {
        Socket skt = null;
        try {
            skt = new Socket("127.0.0.1", 15896);
        } catch (UnknownHostException ex) {
            Logger.getLogger(BugNEARSASSocketClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BugNEARSASSocketClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return skt;
    }
}
