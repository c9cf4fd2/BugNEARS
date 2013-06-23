/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugner;

import java.io.Serializable;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class HTTPEvent implements Serializable {
    long time;
    String cliIP;
    String hostIP;
    String HostDN;
    String reqPath;
    String referer;
    String contentType;
    String filePathPrefix;
}
