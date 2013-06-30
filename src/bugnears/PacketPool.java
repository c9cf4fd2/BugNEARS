/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bugnears;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import jpcap.packet.Packet;

/**
 *
 * @author bugVM
 */
public class PacketPool {

    private final List<Packet> packets;
    private final Map<Integer, Integer> its;
    private final Map<Integer, Integer> steps;
    private int step;
    private int bufferByteAdded;
    private int shiftAmount;
    private Random rnd;
    private final TreeSet<Integer> TS;

    public PacketPool() {
        its = new HashMap<Integer, Integer>();
        packets = new ArrayList<Packet>();
        steps = new HashMap<Integer, Integer>();
        step = 0;
        bufferByteAdded = 0;
        rnd = new Random();
        TS = new TreeSet<Integer>();
    }

    public synchronized void addPacket(Packet p) {
        packets.add(p);
        bufferByteAdded += p.len;
        if (bufferByteAdded > 4096) {
            cleanUnuse();
        }
    }

    public synchronized Packet getPacket(Integer key) {
        int it = getSynIt(key);
        if (it < packets.size()) {
            its.put(key, it + 1);
            return packets.get(it);
        } else {
            return null;
        }
    }

    public synchronized Integer getNewKey() {
        Integer key = rnd.nextInt();
        for (;;) {
            if (!its.containsKey(key)) {
                its.put(key, 0);
                steps.put(key, step);
                return key;
            }
        }
    }

    public synchronized Integer getNewKey(Integer oldKey) {
        Integer key = rnd.nextInt();
        for (;;) {
            if (!its.containsKey(key)) {
                its.put(key, its.get(oldKey));
                steps.put(key, steps.get(oldKey));
                return key;
            }
        }
    }

    public synchronized void removeKey(Integer TIDKey) {
        its.remove(TIDKey);
        steps.remove(TIDKey);
    }

    private void cleanUnuse() {
        if (isSyned()) {
            TS.addAll(its.values());
            shiftAmount = TS.first();
            TS.clear();
            packets.subList(0, shiftAmount).clear();
            step++;
            bufferByteAdded =0;
        }
    }

    private int getSynIt(Integer key) {
        int oit = its.get(key);
        if (steps.get(key) == step) {
            return oit;
        } else {
            int nit = oit - shiftAmount;
            int nstep = steps.get(key) + 1;
            its.put(key, nit);
            steps.put(key, nstep);
            return nit;
        }
    }

    private boolean isSyned() {
        Collection<Integer> values = steps.values();
        if (values.contains(step - 1)) {
            return false;
        } else {
            return true;
        }
    }
}
