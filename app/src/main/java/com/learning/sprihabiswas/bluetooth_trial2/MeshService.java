package com.learning.sprihabiswas.bluetooth_trial2;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MeshService extends Service {
    private List<Device> devices;
    private List<Packet> broadcasts;
    private int pending=0;
    private int connectedTo=0;
    private Device connectedDevice;

    private BluetoothChatHelper bluetoothHelper;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatHelper.STATE_CONNECTED:
                            //setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            //msgList.removeAllViews();

                            while (connectedDevice.queueSize()>0) {
                                bluetoothHelper.write(connectedDevice.pop());
                                pending--;
                            }

                            bluetoothHelper.start();

                            break;
                        case BluetoothChatHelper.STATE_CONNECTING:
                            //setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatHelper.STATE_LISTEN:
                        case BluetoothChatHelper.STATE_NONE:
                            //setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //addToConversation("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    Packet pckt = new Packet(readBuf);

                    // TODO display msg
                    Log.d("Broadcast", pckt.content);

                    // Check then broadcast
                    for (Packet broadcasted : broadcasts ){
                        if(broadcasted.id.equals(pckt.id)) {
                            Log.d("Broadcast", "Looped back : "+pckt.id);
                            break;
                        }
                    }


                    for(Device device : devices){
                        if(!device.getId().equals(connectedDevice.getId()))
                            pending += (device.sendPacket(pckt)? 1 :0 );
                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    String addr = msg.getData().getString(Constants.DEVICE_NAME);

                    for(Device d : devices){
                        if(d.btDevice.getAddress().equals(addr)) {
                            connectedDevice = d;
                            break;
                        }
                    }

                    break;
            }
        }
    };


    public MeshService() {
        devices = new ArrayList<>();
        broadcasts = new ArrayList<>();
        bluetoothHelper = new BluetoothChatHelper(this, mHandler);
    }

    @Override
    public void onCreate(){
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

    }


    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    devices.add(new Device(device));
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d("MeshService", "Finished discovery");
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
