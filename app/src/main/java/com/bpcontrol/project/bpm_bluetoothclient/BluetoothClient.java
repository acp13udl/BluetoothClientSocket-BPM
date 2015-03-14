package com.bpcontrol.project.bpm_bluetoothclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.UUID;


public class BluetoothClient extends Activity {


    private BluetoothAdapter badapter;

    private final UUID UUID_CONNECT = UUID.fromString("b7746a40-c758-4868-aa19-7ac6b3475dfc");
    private final String TAG= "BLUETOOTH";
    private final int REQUESTCODE_BLUETOOTH_ENABLE = 1;
    private final int MAX_BYTES_SEND = 1024;
    private final int MAX_TIME_TO_DISCOVERY = 300;

    private ArrayAdapter<String> adapterdialog;
    private ArrayList<BluetoothDevice> devicesfound;

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;


    private final BroadcastReceiver searchDevicesReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(TAG,"STARTING SEARCH");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG,"SEARCH FINISHED");
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d(TAG,"DEVICE FOUND");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                adapterdialog.add(device.getName());
                adapterdialog.notifyDataSetChanged();
                devicesfound.add(device);
            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bluetooth_client);
        Button send = (Button) findViewById(R.id.button);
        devicesfound = new ArrayList<BluetoothDevice>();
        send.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                badapter = (BluetoothAdapter)BluetoothAdapter.getDefaultAdapter();
                if (!badapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent,REQUESTCODE_BLUETOOTH_ENABLE);
                } else {
                    searchDevices();
                }
            }
        });


    }

    @Override
    public void onStart(){
        super.onStart();

        IntentFilter filtersearch = new IntentFilter();

        filtersearch.addAction(BluetoothDevice.ACTION_FOUND);
        filtersearch.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filtersearch.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(searchDevicesReceiver, filtersearch);

    }

    @Override
    public void onStop(){
         super.onStop();

         connectedThread.cancel();
         connectThread.cancel();
         if (searchDevicesReceiver!=null) unregisterReceiver(searchDevicesReceiver);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUESTCODE_BLUETOOTH_ENABLE:
                if (resultCode == Activity.RESULT_OK) {
                    searchDevices();
                }
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket clientsocket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            this.device = device;
            clientsocket = null;
            try {
                clientsocket = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            badapter.cancelDiscovery();

            try {
                clientsocket.connect();
            } catch (IOException e) {

                try {
                    clientsocket.close();
                } catch (IOException e2) {
                    e.printStackTrace();
                }

            }

            connected(clientsocket, device);
        }

        public void cancel() {
            try {
                clientsocket.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
        }
    }

    private void connected(BluetoothSocket mmSocket, BluetoothDevice connect){

        connectedThread= new ConnectedThread(mmSocket);
        connectedThread.start();


    }

    private void connect(BluetoothDevice device){

        try {
            connectThread = new ConnectThread(device, UUID_CONNECT);
            connectThread.start();
        }catch (Exception ex){
            ex.printStackTrace();
        }

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket clientSocket;
        private OutputStream outputStream ;

        public ConnectedThread(BluetoothSocket clientSocket) {
            this.clientSocket = clientSocket;

            try {
                outputStream = clientSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            try {
                write("PRUEBA DE PASAR BYTES".getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void searchDevices(){

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, MAX_TIME_TO_DISCOVERY);
        startActivity(discoverableIntent);

        AlertDialog.Builder builderdevicesdialog = new AlertDialog.Builder(this);
        builderdevicesdialog.setTitle(getResources().getString(R.string.devicechoose));
        adapterdialog =new ArrayAdapter<String>(
                this, android.R.layout.simple_expandable_list_item_1);


        if (badapter.isDiscovering()) {
            badapter.cancelDiscovery();
        }

        badapter.startDiscovery();

        builderdevicesdialog.setAdapter(adapterdialog, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {

                BluetoothDevice device = devicesfound.get(item);

                connect(device);


            }
        });
        AlertDialog alert = builderdevicesdialog.create();
        alert.show();

    }
}
