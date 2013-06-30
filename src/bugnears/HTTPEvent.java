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
public class HTTPEvent implements Serializable {

    long time;
    String cliIP;
    String hostIP;
    String HostDN;
    String reqPath;
    String referer;
    String contentType;
    int contentLength;
    boolean isChunked;
    boolean isGZip;
    String filePathPrefix;

    public HTTPEvent() {
        time = 0;
        cliIP = null;
        hostIP = null;
        HostDN = null;
        reqPath = null;
        referer = null;
        contentType = null;
        contentLength = 0;
        isChunked = false;
        isGZip = false;
        filePathPrefix = null;
    }
}
