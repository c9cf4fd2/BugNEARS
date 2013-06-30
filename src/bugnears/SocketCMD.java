/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugnears;

import java.io.Serializable;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class SocketCMD implements Serializable {

    String cmd;
    String[] parameters;

    public SocketCMD(String cmd, String[] parameters) {
        this.cmd = cmd;
        this.parameters = parameters;
    }
}
