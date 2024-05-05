package com.hthuz.lockcompanion;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hthuz.lockcompanion.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;



    private ActivityResultLauncher<Intent> enableBluetooth;         // open bluetooth intent
    private ActivityResultLauncher<String> requestBluetoothConnect;
    private ActivityResultLauncher<String> requestBluetoothScan;
    private ActivityResultLauncher<String> requestLocation;
    private ActivityResultLauncher<Intent> connectBluetooth;

    private final String TAG = MainActivity.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothLeScanner scanner;

    boolean isScanning = false;


    private final List<MyDevice> deviceList = new ArrayList<>();

    private MyDeviceAdapter myDeviceAdapter;

    private void registerIntent() {
        enableBluetooth = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (isOpenBluetooth()){
                    BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
                    mBluetoothAdapter = manager.getAdapter();
                    scanner = mBluetoothAdapter.getBluetoothLeScanner();
                    showMsg("Bluetooth is open");
                } else {
                    showMsg("Bluetooth is not open");
                }
            }
        });
        requestBluetoothConnect = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) {
                enableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            } else {
                showMsg("Android12 no permission");
            }
        });
        requestBluetoothScan = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) {
                startScan();
            } else {
                showMsg("Android12 no permission");
            }
        });
        requestLocation = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) {
                startScan();
            } else {
                showMsg("require location access");
            }
        });


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        registerIntent();
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addDeviceList(new MyDevice(result.getDevice(),result.getRssi()));
        }
    };

    private void initView() {

        // Navigation
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        if (isOpenBluetooth()) {
            BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            mBluetoothAdapter = manager.getAdapter();
            scanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        // open bluetooth button event
        Button btnOpenBluetooth = findViewById(R.id.btn_open_bluetooth);
        btnOpenBluetooth.setOnClickListener(v -> {
            showMsg("Button pressed");
            if (isOpenBluetooth()) {
                showMsg("Bluetooth open");
                return;
            }
            if (isAndroid12()) {
                if (hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                    // open bluetooth
                    enableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                } else {
                    // ask for request
                    requestBluetoothConnect.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
                }
                return;
            }
            // not android, open bluetooth directly
            enableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        });
        // scan bluetooth button event
        Button btnScanBluetooth = findViewById(R.id.btn_scan_bluetooth);
        btnScanBluetooth.setOnClickListener(v -> {
            if (isAndroid12()) {
                if (!hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                    requestBluetoothConnect.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
                    return;
                }
                if (hasPermission(android.Manifest.permission.BLUETOOTH_SCAN)) {
                    if (isScanning) stopScan();
                    else startScan();
                } else {
                    requestBluetoothScan.launch(android.Manifest.permission.BLUETOOTH_SCAN);
                }
            } else {
                if (hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (isScanning) stopScan();
                    else startScan();
                } else {
                    requestLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
        });

        // connect button to start activity
//        binding.btnConnectBluetooth.setOnClickListener(v -> {
//            Intent intent_control = new Intent(this, DeviceControlActivity.class);
//            startActivity(intent_control);
//        });
        // initialize lists
        myDeviceAdapter = new MyDeviceAdapter(deviceList);
        RecyclerView rvDevice = findViewById(R.id.rv_device);
        rvDevice.setLayoutManager(new LinearLayoutManager(this));

        rvDevice.setAdapter(myDeviceAdapter);
    }

    private void startScan() {
        if (!isScanning) {
            scanner.startScan(scanCallback);
            isScanning = true;
            Button btnScanBluetooth = findViewById(R.id.btn_scan_bluetooth);
            btnScanBluetooth.setText("Stop scanning");
        }
    }

    private void stopScan() {
        if (isScanning) {
            scanner.stopScan(scanCallback);
            isScanning = false;
            Button btnScanBluetooth = findViewById(R.id.btn_scan_bluetooth);
            btnScanBluetooth.setText("Start Scanning");
        }
    }

    private int findDeviceIndex(MyDevice scanDevice, List<MyDevice> deviceList) {
        int index = 0;
        for (MyDevice myDevice : deviceList) {
            if (myDevice.getDevice().getAddress().equals(scanDevice.getDevice().getAddress())) return index;
            index += 1;
        }
        return -1;
    }

    private void addDeviceList(MyDevice device) {
        int index = findDeviceIndex(device, deviceList);
        if (index == -1) {
            deviceList.add(device);
            myDeviceAdapter.notifyDataSetChanged();
        } else {
            deviceList.get(index).setRssi(device.getRssi());
            myDeviceAdapter.notifyItemChanged(index);
        }
    }

    private boolean isOpenBluetooth() {
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null) {
            return false;
        }
        return adapter.isEnabled();
    }

    private boolean isAndroid12() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    private boolean hasPermission(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public void showMsg(CharSequence msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


}