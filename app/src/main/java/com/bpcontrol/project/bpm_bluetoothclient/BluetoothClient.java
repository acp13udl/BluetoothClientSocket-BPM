package com.bpcontrol.project.bpm_bluetoothclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

    private EditText text1,text2,text3;
    private TextView textView;
    private Handler handler;
    private String writeXML;
    private ProgressDialog dialog;


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

        text1 = (EditText) findViewById(R.id.text1);
        text2 = (EditText) findViewById(R.id.text2);
        text3 = (EditText) findViewById(R.id.text3);

        textView = (TextView) findViewById(R.id.showxmlresult);

        dialog = new ProgressDialog(this);
        dialog.setMessage(getResources().getString(R.string.process));

        send.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (text1.getText().toString().equals("") || text2.getText().toString().equals("")
                        || text3.getText().toString().equals("")) {

                    Toast.makeText(BluetoothClient.this,getResources().getString(R.string.mandatoryfuields),Toast.LENGTH_LONG).show();
                }else {

                    badapter = (BluetoothAdapter) BluetoothAdapter.getDefaultAdapter();
                    XMLMeasurementCreator creator = new XMLMeasurementCreator(text1.getText().toString(),
                            text2.getText().toString(),text3.getText().toString());
                    writeXML = creator.createXML();
                    if (!badapter.isEnabled()) {
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableIntent, REQUESTCODE_BLUETOOTH_ENABLE);
                    } else {
                        searchDevices();
                    }

                }
            }
        });

        handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {

                String xml = (String) msg.obj;
                textView.setText(xml);


            }
        };

        IntentFilter filtersearch = new IntentFilter();

        filtersearch.addAction(BluetoothDevice.ACTION_FOUND);
        filtersearch.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filtersearch.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(searchDevicesReceiver, filtersearch);


    }

    @Override
    public void onStart(){
        super.onStart();

    }

    @Override
    public void onDestroy(){
         super.onDestroy();
        try {
            connectedThread.cancel();
        }catch (Exception ex){
            ex.printStackTrace();
        }
        try {
            connectThread.cancel();
        }catch (Exception ex){
            ex.printStackTrace();
        }
        try {
            unregisterReceiver(searchDevicesReceiver);
        }catch (Exception ex){
            ex.printStackTrace();
        }

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
            if (dialog!=null) dialog.dismiss();
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

            Message msg = new Message();
            try {
                write(writeXML.getBytes("UTF-8"));
                msg.obj =writeXML;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                msg.obj="Error";

            }

            handler.sendMessage(msg);
            dialog.dismiss();

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
            if (dialog!=null) dialog.dismiss();
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

                BluetoothClient.this.dialog.show();

                BluetoothDevice device = devicesfound.get(item);

                connect(device);


            }
        });
        AlertDialog alert = builderdevicesdialog.create();
        alert.show();

    }
}
