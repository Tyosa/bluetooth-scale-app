package com.example.coffeescaleapp;

import android.Manifest;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class TestActivity extends AppCompatActivity {

    private String TAG = "TestActivity";

    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;
    private BluetoothManager mManager;
    private BluetoothAdapter mAdapter;
    private ConnectThread thread;

    /**
     * Thread that will stay connected to the PI
     */
    public class ConnectThread extends Thread {
        private static final String TAG = "ConnectThread";
        private boolean running;

        private ConnectThread(BluetoothDevice device) throws IOException {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;

            // Create the bluetooth socket
            BluetoothSocket tmp = null;
            mDevice = device;
            try {
                tmp = mDevice.createRfcommSocketToServiceRecord(MainActivity.uuid);
            } catch (IOException e) {
                Log.e(TAG, "Error while creating socket");
            }
            mSocket = tmp;
            mAdapter.cancelDiscovery();

            // Initialize the connection
            try {
                mSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "Error while connecting to remote device");
                try {
                    mSocket.close();
                } catch (Exception ignored) {}
            }

            running = true;

            // We continuously ask for data
            while (running) {
                send("data");
            }
        }

        public void send(String topic) throws IOException {
            OutputStream outputStream = mSocket.getOutputStream();
            outputStream.write(topic.getBytes(StandardCharsets.UTF_8));
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

        @Override
        public void interrupt() {
            running = false;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comms);

        mManager = getSystemService(BluetoothManager.class);
        mAdapter = mManager.getAdapter();
        final Intent intent = getIntent();
        final String address = intent.getStringExtra(MainActivity.EXTRA_ADDRESS);
        Log.i(TAG, "Address is :" + address);
        Button startButton = (Button) findViewById(R.id.start_button);
        Button stopButton = (Button) findViewById(R.id.stop_button);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final BluetoothDevice device = mAdapter.getRemoteDevice(address);

                try {
                    thread = new ConnectThread(device);
                    thread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                thread.interrupt();
                thread = null;
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
