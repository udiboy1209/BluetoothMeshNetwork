package com.learning.sprihabiswas.bluetooth_trial2;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;

public class Device {
    BluetoothDevice btDevice;
    private List<Packet> pcktQueue;

    public Device(BluetoothDevice bt){
        btDevice = bt;
        pcktQueue = new ArrayList<>();


    }

    /**
     * @return boolean true if packet added to queue, else false
     */
    public boolean sendPacket(Packet newp){
        for(Packet p : pcktQueue){
            if(p.id.equals(newp.id))
                return false;
        }
        pcktQueue.add(newp);

        return true;
    }

    public int queueSize(){
        return pcktQueue.size();
    }

    /**
     * @return byte data of packet popped from start
     */
    public byte[] pop(){
        return pcktQueue.remove(0).getBytes();
    }

    public String getId(){
        return btDevice.getAddress();
    }
}
