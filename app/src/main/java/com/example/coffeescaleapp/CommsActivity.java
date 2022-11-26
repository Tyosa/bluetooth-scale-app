package com.example.coffeescaleapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class CommsActivity extends AppCompatActivity {

    private String G_TAG = "CommsActivity";

    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    public class ConnectThread extends Thread {
        private static final String TAG = "CommThread";

        private ConnectThread(BluetoothDevice device) throws IOException {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;

            BluetoothSocket tmp = null;
            mDevice = device;
            try {
                tmp = mDevice.createRfcommSocketToServiceRecord(MainActivity.uuid);
            } catch (IOException e) {
                Log.e(TAG, "Error creating socket");
            }
            mSocket = tmp;
            bluetoothAdapter.cancelDiscovery();

            try {
                mSocket.connect();
            } catch (IOException connectException) {
                Log.e(TAG, "Connection exception !");
                try {
                    mSocket.close();
                } catch (IOException ignored) {

                }
            }
            send();
        }

        public void send() throws IOException {
            String msg = "data";
            OutputStream outputStream = mSocket.getOutputStream();
            outputStream.write(msg.getBytes(StandardCharsets.UTF_8));
            receive();
        }

        public void receive() throws IOException {
            InputStream inputStream = mSocket.getInputStream();
            byte[] buffer = new byte[256];
            int bytes;

            try {
                bytes = inputStream.read(buffer);
                String readMessage = new String(buffer, 0, bytes);
                Log.d(TAG, "Received : " + readMessage);
                TextView weightTextView = (TextView) findViewById(R.id.weight_text);
                weightTextView.setText(readMessage);
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Problem occured");
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comms);

        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        final Intent intent = getIntent();
        final String address = intent.getStringExtra(MainActivity.EXTRA_ADDRESS);
        Log.i(G_TAG, "Address is :" + address);
        Button weightButton = (Button) findViewById(R.id.start_button);

        weightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

                try {
                    new ConnectThread(device).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
