package com.learning.sprihabiswas.bluetooth_trial2;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MeshService extends Service {
    private List<Device> devices;
    private List<Packet> broadcasts;
    private int continueFrom=0;
    private Device connectedDevice;

    private BluetoothChatHelper bluetoothHelper;

    private BluetoothAdapter mBluetoothAdapter = null;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatHelper.STATE_CONNECTED:
                            //setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            //msgList.removeAllViews();


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
                            device.sendPacket(pckt);
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

    private final Runnable queuePoller = new Runnable() {
        @Override
        public void run() {
            if(bluetoothHelper.getState() == BluetoothChatHelper.STATE_LISTEN) {
                int i;
                for (i=continueFrom; i<devices.size(); i++) {
                    if (devices.get(i).queueSize() > 0) {
                        bluetoothHelper.connect(devices.get(i), true);
                        break;
                    }
                }

                continueFrom = (i<devices.size()? i+1 : i) % devices.size();
            }
        }
    };

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        MeshService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MeshService.this;
        }
    }

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

        executor.scheduleAtFixedRate(queuePoller, 1000, 1500, TimeUnit.MILLISECONDS);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void pairWith(String deviceAddress){
        Device device = new Device(mBluetoothAdapter.getRemoteDevice(deviceAddress));

        bluetoothHelper.connect(device,true);
    }

    public void broadcastMessage(Packet p){
        for(Device d : devices){
            d.sendPacket(p);
        }
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
        return mBinder;
    }

    public Device getDevice(BluetoothDevice device) {
        for(Device d : devices){
            if(d.btDevice==device){
                return d;
            }
        }

        Device d = new Device(device);
        devices.add(d);
        return d;
    }
}
