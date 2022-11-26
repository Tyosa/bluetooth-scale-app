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
import android.widget.Toast;

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
import java.util.concurrent.atomic.AtomicBoolean;

public class CommsActivity extends AppCompatActivity {

    private String TAG = "CommsActivity";

    private BluetoothSocket mSocket;
    private BluetoothAdapter bluetoothAdapter;
    private DataThread thread;

    public class DataThread implements Runnable {

        private Thread worker;
        private final int interval;
        private int consecutiveErrors = 0;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final AtomicBoolean stopped = new AtomicBoolean(true);

        public DataThread(int intervalMillis) {
            this.interval = intervalMillis;
        }

        public void start() {
            worker = new Thread(this);
            worker.start();
        }

        public void stop() {
            running.set(false);
        }

        public void interrupt() {
            running.set(false);
            worker.interrupt();
        }

        boolean isRunning() {
            return running.get();
        }

        boolean isStopped() {
            return stopped.get();
        }

        public void run() {
            running.set(true);
            stopped.set(false);
            while(running.get()) {
                try {
                    send("data");
                    consecutiveErrors = 0;
                    Thread.sleep(interval);
                } catch (InterruptedException | IOException e) {
                    Log.e(TAG, "Error while acquiring data");
                    consecutiveErrors++;
                    if (consecutiveErrors >= 9) Thread.currentThread().interrupt();
                }
            }
            stopped.set(true);
        }

        private void send(String topic) throws IOException {
            OutputStream outputStream = mSocket.getOutputStream();
            outputStream.write(topic.getBytes(StandardCharsets.UTF_8));
            receive();
        }

        private void receive() throws IOException {
            InputStream inputStream = mSocket.getInputStream();
            byte[] buffer = new byte[256];
            int bytes = inputStream.read(buffer);

            String readMessage = new String(buffer, 0, bytes);
            Log.i(TAG, "Received data" + readMessage);

            runOnUiThread(() -> {
                TextView weightTextView = (TextView) findViewById(R.id.weight_text);
                weightTextView.setText(readMessage);
            });

        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comms);

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        final Intent intent = getIntent();
        final String address = intent.getStringExtra(MainActivity.EXTRA_ADDRESS);
        Button connectButton = (Button) findViewById(R.id.connect_button);
        Button startButton = (Button) findViewById(R.id.data_button);
        Button resetButton = (Button) findViewById(R.id.reset_button);

        connectButton.setOnClickListener(view -> connect(address));
        startButton.setOnClickListener(view -> acquire());
    //TODO resetButton + tare (reset when we have a graph, tare to set 0)
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

    /**
     * Initialize connection to the device OR disconnect
     * @param address Device address
     */
    private void connect(String address) {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;

        if (mSocket != null && mSocket.isConnected()) {
            try {
                mSocket.close();
                Toast.makeText(getApplicationContext(), "Disconnected !", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

            // Create the socket
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MainActivity.uuid);
            } catch (IOException e) {
                Log.e(TAG, "Error creating socket");
            }
            mSocket = tmp;
            bluetoothAdapter.cancelDiscovery();

            // Connect to the socket
            try {
                mSocket.connect();
                Toast.makeText(getApplicationContext(), "Connected to the device !", Toast.LENGTH_SHORT).show();
            } catch (IOException connectException) {
                Log.e(TAG, "Connection exception !");
                try {
                    mSocket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Start or stop acquiring data
     */
    private void acquire() {
        if (mSocket == null || !mSocket.isConnected()) {
            Toast.makeText(getApplicationContext(), "Please connect before trying to get data", Toast.LENGTH_SHORT).show();
            return;
        }
        if (thread == null || !thread.isRunning()) {
           start();
        } else {
            stop();
        }
    }

    /**
     * Start acquiring data
     */
    private void start() {
        thread = new DataThread(100);
        thread.start();
    }

    /**
     * Stop acquiring data
     */
    private void stop() {
        if (thread != null && thread.isRunning()) thread.interrupt();
    }
}
