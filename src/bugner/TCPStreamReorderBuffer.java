/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import jpcap.packet.TCPPacket;

/**
 *
 * @author c9cf4fd2NTUT
 */
public class TCPStreamReorderBuffer {

    private Map<Long, TCPPacket> oooBuffer;
    private int oooBufferLen;
    private List<TCPPacket> ioBuffer;
    private long sSeq;
    private long nSeq;
    private long rxAckSeq;
    private List<Long> lostSegs;

    public TCPStreamReorderBuffer() {
        oooBuffer = new HashMap<Long, TCPPacket>();
        oooBufferLen = 0;
        ioBuffer = new LinkedList<TCPPacket>();
        sSeq = -1;
        nSeq = -1;
        rxAckSeq = -1;
        lostSegs = new ArrayList<Long>();
    }

    public void put(TCPPacket tp) {
        TCPPacket otp = oooBuffer.put(tp.sequence, tp);
        if (otp != null) {
            if (otp.data.length > tp.data.length) {
                System.out.println("otp.data.length > tp.data.length");
                oooBuffer.put(otp.sequence, otp);
                return;
            }
        }
        oooBufferLen += tp.data.length;
    }

    public long getStartSeq() {
        return sSeq;
    }

    public void setAckedSeq(long ackNum) {
        if (rxAckSeq < ackNum) {
            rxAckSeq = ackNum;
        }
    }

    public long getAckedSeq() {
        return rxAckSeq;
    }

    public int getOOOBufLen() {
        return oooBufferLen;
    }

    public TCPPacket[] getIOPackets() {
        if (oooBufferLen == 0) {
            return null;
        } else if (oooBufferLen < 0) {
            System.out.println("oooBufferLen < 0");
        }
        Set<Long> oookeySet = oooBuffer.keySet();
        TreeSet<Long> oooKTS = new TreeSet<Long>(oookeySet);
        Iterator<Long> oooKTSIt = oooKTS.iterator();
        long pSeq;
        TCPPacket tp;
        if (nSeq < 0) {
            nSeq = oooKTS.first();
            sSeq = nSeq;
        }
        while (oooKTSIt.hasNext()) {
            pSeq = oooKTSIt.next();
            if (pSeq < nSeq) {
                tp = oooBuffer.remove(pSeq);
                oooBufferLen -= tp.data.length;
            } else if (pSeq == nSeq) {
                tp = oooBuffer.remove(pSeq);
                oooBufferLen -= tp.data.length;
                ioBuffer.add(tp);
                nSeq = tp.sequence + tp.data.length;
            } else if (pSeq > nSeq) {
                tp = oooBuffer.get(pSeq);
                if (pSeq + tp.data.length >= rxAckSeq) {
                    break;
                } else {
                    if (nSeq > 0) {
                        System.out.println("lost seq " + (nSeq - sSeq) + " - " + (tp.sequence - sSeq));
                        lostSegs.add(nSeq - sSeq);
                        lostSegs.add(tp.sequence - sSeq);
                    }
                    oooBuffer.remove(pSeq);
                    oooBufferLen -= tp.data.length;
                    ioBuffer.add(tp);
                    nSeq = tp.sequence + tp.data.length;
                }
            }
        }
        TCPPacket[] retA = new TCPPacket[ioBuffer.size()];
        for (int i = 0; i < retA.length; i++) {
            retA[i] = ioBuffer.remove(0);
        }
        return retA;
    }

    public List<Long> getLostSegInfo() {
        return lostSegs;
    }

    public void clearLostSegInfo() {
        lostSegs.clear();
    }

    public void clear() {
        oooBuffer.clear();
        ioBuffer.clear();
        lostSegs.clear();
    }
}
