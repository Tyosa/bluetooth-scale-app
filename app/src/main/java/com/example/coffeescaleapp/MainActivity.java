package com.example.coffeescaleapp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button enableBt, disableBt, scanBt;
    public final static UUID uuid = UUID.fromString("47b02853-3bcf-4f1c-b682-ccb98cf85f79");
    private BluetoothManager manager;
    private BluetoothAdapter adapter;
    private ListView devices;
    public final static String EXTRA_ADDRESS = null;
    private Set<BluetoothDevice> pairedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN);
        }

        enableBt = (Button) findViewById(R.id.enable_bt);
        disableBt = (Button) findViewById(R.id.disable_bt);
        scanBt = (Button) findViewById(R.id.scan_bt);

        manager = getSystemService(BluetoothManager.class);
        adapter = manager.getAdapter();
        if (adapter == null) {
            // Devices does not support bluetooth
            Toast.makeText(getApplicationContext(), "Device does not support bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (adapter.isEnabled()) scanBt.setVisibility(View.VISIBLE);
            enableBt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    enableBluetooth(view);
                }
            });

            disableBt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    disableBluetooth(view);
                }
            });

            scanBt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    scanDevices(view);
                }
            });
            devices = (ListView) findViewById(R.id.devices_list);
        }
    }

    private void enableBluetooth(View v) {
        if (!adapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            launchIntentForResult.launch(intent);
            Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_SHORT).show();
        }
        scanBt.setVisibility(View.VISIBLE);
        devices.setVisibility(View.VISIBLE);
    }

    private void disableBluetooth(View v) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
        adapter.disable();
        Toast.makeText(getApplicationContext(), "Bluetooth turned off", Toast.LENGTH_SHORT).show();
        scanBt.setVisibility(View.INVISIBLE);
        devices.setVisibility(View.GONE);
    }

    private void scanDevices(View v) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;

        ArrayList<String> deviceList = new ArrayList<>();

        pairedDevices = adapter.getBondedDevices();

        if (pairedDevices.size() < 1) {
            Toast.makeText(getApplicationContext(), "No paired devices found", Toast.LENGTH_SHORT).show();
        } else {
            for (BluetoothDevice device: pairedDevices) {
                deviceList.add(device.getName() + " " + device.getAddress());
            }
            Toast.makeText(getApplicationContext(), "Showing paired devices", Toast.LENGTH_SHORT).show();
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceList);
            devices.setAdapter(arrayAdapter);
            devices.setOnItemClickListener(itemClick);
        }
    }

    private final AdapterView.OnItemClickListener itemClick = (adapterView, view, i, l) -> {
        String info = ((TextView) view).getText().toString();
        String address = info.substring(info.length() - 17);
        Toast.makeText(getApplicationContext(), info, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, TestActivity.class);
        intent.putExtra(EXTRA_ADDRESS, address);
        startActivity(intent);
    };

    private final ActivityResultLauncher<Intent> launchIntentForResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.i("Activity result", String.valueOf(result.getResultCode()));
                if (result.getResultCode() == Activity.RESULT_OK) {

                    Intent intent = result.getData();

                } else {
                    //todo
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    //todo
                } else {
                    // todo
                }
            }
            );
}