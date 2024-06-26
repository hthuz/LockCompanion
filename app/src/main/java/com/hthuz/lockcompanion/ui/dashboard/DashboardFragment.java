package com.hthuz.lockcompanion.ui.dashboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.hthuz.lockcompanion.MyDevice;
import com.hthuz.lockcompanion.MyDeviceAdapter;
import com.hthuz.lockcompanion.R;
import com.hthuz.lockcompanion.Values;
import com.hthuz.lockcompanion.databinding.FragmentDashboardBinding;
import com.hthuz.lockcompanion.BluetoothLeService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

    private ActivityResultLauncher<String> requestBluetoothConnect;
    private static final String TAG = "MY_DEBUG";

//    public void OnCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Log.e(TAG, "fragment on create");
//        // start service
//        Log.e(TAG, "Before start service");
//        Log.e(TAG, "After start service");
//    }

    public void OnCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Dashboard fragment onCreate");
    }


    private DashboardViewModel getViewModel() {
        return new ViewModelProvider(this).get(DashboardViewModel.class);
    }
    private int test_num;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        DashboardViewModel dashboardViewModel = getViewModel();
        Log.i(TAG, "Dashboard fragment onCreateView " + test_num);
        test_num++;
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textDashboard;
        dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        requestBluetoothConnect = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
            getActivity().bindService(gattServiceIntent, getViewModel().getServiceConnection(), Context.BIND_AUTO_CREATE);
        });

        getActivity().registerReceiver(gattUpdateReceiver,makeGattUpdateIntentFilter());
        getViewModel().setBinding(binding);


        if (getViewModel().getReadRssiThread().getState() == Thread.State.NEW) {
            getViewModel().getReadRssiThread().start();
        }



        getViewModel().getReadRssiThread().startThread();
        // bluetooth related
        Log.i(TAG, "connected is " + getViewModel().isConnected());

        initViewScan();

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("lock_companion", Context.MODE_PRIVATE);
        boolean autoConnect = sharedPreferences.getBoolean("autoConnect", false);
        Log.i(TAG, "autoconnected is " + autoConnect);
        initView();
        return root;
    }

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
//            Log.e(TAG, "Recevie broadcasted action: " + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                getViewModel().setConnected(true);
                // show connected message
                getViewModel().setState("CONNECTED");

                binding.connectState.setText(getViewModel().getState());
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                getViewModel().setConnected(false);
                // show disconnected message
                getViewModel().setState("DISCONNECTED");
                binding.connectState.setText(getViewModel().getState());

                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    Log.e(TAG, "Error");
                }
                final boolean result = getViewModel().getBluetoothService().connect(getMacAddress());
                Log.d(TAG, "Connect request result=" + result);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // display supported services
                getViewModel().displayGattServices(getViewModel().getBluetoothService().getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_CHARACTERISTIC_READ.equals(action)
                    || BluetoothLeService.ACTION_CHARACTERISTIC_CHANGED.equals(action)) {
                String readValue = intent.getStringExtra("readValue");
                binding.readConsole.setText(readValue);
                Log.i(TAG, "Read Data: " + readValue);

                if (getViewModel().isProcessing() && readValue.equals("Door Opening...")) {
                    getViewModel().setEtime(System.currentTimeMillis());
                    double time_dif = (getViewModel().getEtime() - getViewModel().getStime()) / 1000.0;
                    Log.i(TAG, String.valueOf(time_dif));
                    Log.i(TAG, "Unlocked!");
                    showMsg("Unlocked!");
                    showSnack(String.valueOf(time_dif) + "s");
                }
                if (getViewModel().isProcessing() && readValue.equals("Door Open Done!`")) {
                    getViewModel().setProcessing(false);
                }
                if (getViewModel().isProcessing()) {
                    if (readValue.equals("Enroll_Fail...") ||
                    readValue.equals("Enroll_Done!") ||
                    readValue.equals("Clear_Done!") ||
                    readValue.equals("Clear_Fail...") ||
                    readValue.startsWith("Finger_num:") ) {
                        getViewModel().setProcessing(false);
                    }
                }

            } else if (BluetoothLeService.ACTION_READ_REMOTE_RSSI.equals(action)) {
                int rssi = intent.getIntExtra("rssi", 0);
                binding.rssiValue.setText("rssi: " + rssi);
                if (!getViewModel().isProcessing() && getViewModel().isConnected() && rssi < 0 && rssi > -45) {
                    getViewModel().setStime(System.currentTimeMillis());
                    getViewModel().setProcessing(true);
                    writeESP32("5");
                    showMsg("Auto Unlocking...");
                }
            }
        }
    };
    @Override
    public void onDestroyView() {
        Log.i(TAG, "Dashboard fragment onDestroyView");
        super.onDestroyView();
        getViewModel().getReadRssiThread().stopThread();
        binding = null;
        stopScan();
        getActivity().unregisterReceiver(gattUpdateReceiver);

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Dashboard fragment onDestroy");
    }
    public void onResume() {
        super.onResume();
        Log.i(TAG, "Dashboard fragment onResume");
    }



//    public String testMAC = "E8:6B:EA:D4:FC:D6"; // another ESP32
    //        public String testMAC = "14:94:6C:C0:53:83"; // My iP
//
//    public String testMac = Values.macAddress;
    public static UUID ESP32_USER_SERVICE_UUID = UUID.fromString("12a59900-17cc-11ec-9621-0242ac130002");
    public static UUID ESP32_RX_CHARACTERISTIC_UUID = UUID.fromString("12a59e0a-17cc-11ec-9621-0242ac130002");
    public static UUID ESP32_TX_CHARACTERISTIC_UUID = UUID.fromString("12a5a148-17cc-11ec-9621-0242ac130002");



    public void startConnect() {
        requestBluetoothConnect.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
    }

    private void initView() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("lock_companion", Context.MODE_PRIVATE);
        boolean autoConnect = sharedPreferences.getBoolean("autoConnect", false);
        binding.autoconnectState.setText("auto_connect: " + String.valueOf(autoConnect));
        binding.connectState.setText(getViewModel().getState());
        binding.btnConnect.setOnClickListener(v -> {
            if (getViewModel().isConnected()) {
                showMsg("DoorLock is Already Connected");
                return;
            }
            if (requestBluetoothConnect == null) {
                showMsg("request is null");
                return;
            }
            getViewModel().setMacAddress(getMacAddress());
            Log.i(TAG, "MAC is " + getMacAddress());
            startConnect();
            showMsg("Connecting...");

        });
        binding.btnUnlock.setOnClickListener(v -> {
            if (!getViewModel().isConnected()) {
                showMsg("Please connect doorlock first");
                return;
            }
            if (getViewModel().isProcessing()) {
                showMsg("Processing previous requests");
                return;
            }
            getViewModel().setProcessing(true);
            getViewModel().setStime(System.currentTimeMillis());
            writeESP32("5");
            showMsg("Unlocking...");
        });
        binding.btnEnroll.setOnClickListener(v -> {
            if (!getViewModel().isConnected()) {
                showMsg("Please connect doorlock first");
                return;
            }
            if (getViewModel().isProcessing()) {
                showMsg("Processing previous requests");
                return;
            }
            getViewModel().setProcessing(true);
            writeESP32("1");
            showMsg("Enrolling...");
        });
        binding.btnEmpty.setOnClickListener(v -> {
            if (!getViewModel().isConnected()) {
                showMsg("Please connect doorlock first");
                return;
            }
            if (getViewModel().isProcessing()) {
                showMsg("Processing previous requests");
                return;
            }
            getViewModel().setProcessing(true);
            writeESP32("2");
            showMsg("Emptying...");
        });
        binding.btnGetnum.setOnClickListener(v -> {
            if (!getViewModel().isConnected()) {
                showMsg("Please connect doorlock first");
                return;
            }
            if (getViewModel().isProcessing()) {
                showMsg("Processing previous requests");
                return;
            }
            getViewModel().setProcessing(true);
            writeESP32("3");
            showMsg("Getting...");


        });
//        binding.btnReadvalue.setOnClickListener((v -> {
//            if (!getViewModel().isConnected()) {
//                showMsg("Please connect doorlock first");
//                return;
//            }
//            getViewModel().setProcessing(false);
//            readESP32();
//        }));


        binding.btnEnableAutoConnect.setOnClickListener(v -> {
//            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("lock_companion", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("autoConnect", true);
            binding.autoconnectState.setText("auto_connect: " + String.valueOf(true));
            editor.commit();
        });

        binding.btnDisableAutoConnect.setOnClickListener(v -> {
//            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("lock_companion", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("autoConnect", false);
            binding.autoconnectState.setText("auto_connect: " + String.valueOf(false));
            editor.commit();
        });

        binding.btnDisconnect.setOnClickListener((v -> {
            if (!getViewModel().isConnected()) {
                showMsg("Device is already disconnected");
                return;
            }
            getViewModel().setProcessing(false);
            getViewModel().setConnected(false);
            getViewModel().setState("DISCONNECTED");
            binding.connectState.setText(getViewModel().getState());
            binding.rssiValue.setText("rssi: none");
            binding.readConsole.setText("");
            showMsg("diconnecting...");
            getActivity().unbindService(getViewModel().getServiceConnection());
//            getViewModel().getBluetoothService().onUnbind();

        }));
    }

    public void readESP32() {
        BluetoothGattService esp32Service = getViewModel().getBluetoothService().getGattServiceByUUID(ESP32_USER_SERVICE_UUID);
        if (esp32Service == null)
            return;
        BluetoothGattCharacteristic rxCharacteristic = esp32Service.getCharacteristic(ESP32_RX_CHARACTERISTIC_UUID);
        BluetoothGattCharacteristic txCharacteristic = esp32Service.getCharacteristic(ESP32_TX_CHARACTERISTIC_UUID);
        getViewModel().getBluetoothService().readCharacteristic(txCharacteristic);
    }

    public void writeESP32(String msg) {
        BluetoothGattService esp32Service = getViewModel().getBluetoothService().getGattServiceByUUID(ESP32_USER_SERVICE_UUID);
        if (esp32Service == null)
            return;
        BluetoothGattCharacteristic rxCharacteristic = esp32Service.getCharacteristic(ESP32_RX_CHARACTERISTIC_UUID);
        BluetoothGattCharacteristic txCharacteristic = esp32Service.getCharacteristic(ESP32_TX_CHARACTERISTIC_UUID);
        rxCharacteristic.setValue(msg);
        getViewModel().getBluetoothService().writeCharacteristic(rxCharacteristic);
        Log.i(TAG, "rx value: " + new String(rxCharacteristic.getValue()));
    }

    public void onPause() {
        super.onPause();


    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        // When the following actions are started as intent by services, gattUpdateReceiver's onReceive will be called
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_CHARACTERISTIC_WRITE);
        intentFilter.addAction(BluetoothLeService.ACTION_CHARACTERISTIC_READ);
        intentFilter.addAction(BluetoothLeService.ACTION_CHARACTERISTIC_CHANGED);
        intentFilter.addAction(BluetoothLeService.ACTION_READ_REMOTE_RSSI);
        return intentFilter;
    }





    public String getMacAddress() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("lock_companion", Context.MODE_PRIVATE);
        return sharedPreferences.getString("macAddress", "E8:6B:EA:D4:FC:D6");
    }


    public void showMsg(CharSequence msg) {
        Toast toast = Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT);
        toast.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                toast.cancel();
            }
        }, 500);
    }
    public void showSnack(CharSequence msg) {

        Snackbar snackbar = Snackbar.make(binding.testMsg, msg, Snackbar.LENGTH_SHORT)
                .setAction("x", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });
        snackbar.show();
    }


    //
    // Scanning related
    //

    private ActivityResultLauncher<Intent> enableBluetooth;         // open bluetooth intent
    private ActivityResultLauncher<String> scan_requestBluetoothConnect;
    private ActivityResultLauncher<String> requestBluetoothScan;
    private ActivityResultLauncher<String> requestLocation;

    //    private final String TAG = MainActivity.class.getSimpleName();
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothLeScanner scanner;

    boolean isScanning = false;


    private final List<MyDevice> deviceList = new ArrayList<>();

    private MyDeviceAdapter myDeviceAdapter;

    private void registerIntent() {
        enableBluetooth = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (isOpenBluetooth()){
                    BluetoothManager manager = (BluetoothManager) getActivity().getSystemService(BluetoothManager.class);
                    mBluetoothAdapter = manager.getAdapter();
                    scanner = mBluetoothAdapter.getBluetoothLeScanner();
                    showMsg("Bluetooth is open");
                } else {
                    showMsg("Bluetooth is not open");
                }
            }
        });
        scan_requestBluetoothConnect = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
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

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addDeviceList(getActivity(), new MyDevice(result.getDevice(),result.getRssi()));
        }
    };

    private void initViewScan() {

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("lock_companion", Context.MODE_PRIVATE);
        boolean autoConnect = sharedPreferences.getBoolean("autoConnect", false);
        Log.i(TAG, "autoconnect:" + String.valueOf(autoConnect));

        if (isOpenBluetooth()) {
            BluetoothManager manager = (BluetoothManager) getActivity().getSystemService(BluetoothManager.class);
            mBluetoothAdapter = manager.getAdapter();
            scanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        if (isOpenBluetooth() && autoConnect) {
            scanBluetooth();
        }

    }

    private void scanBluetooth() {
        if (isAndroid12()) {
            if (!hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                scan_requestBluetoothConnect.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
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
    }
    private void startScan() {
        if (!isScanning) {
            if (scanner == null) {
                Log.e(TAG, "scanner is null");
            }
            scanner.startScan(scanCallback);
            isScanning = true;
        }
    }

    private void stopScan() {
        if (isScanning) {
            scanner.stopScan(scanCallback);
            isScanning = false;
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

    public void addDeviceList(FragmentActivity fragmentActivity, MyDevice device) {
        int index = findDeviceIndex(device, deviceList);
        if (index == -1) {
            deviceList.add(device);
//            myDeviceAdapter.notifyDataSetChanged();
        } else {
            deviceList.get(index).setRssi(device.getRssi());
//            myDeviceAdapter.notifyItemChanged(index);
        }



        // If auto connect
        SharedPreferences sharedPreferences = fragmentActivity.getSharedPreferences("lock_companion", Context.MODE_PRIVATE);
        boolean autoConnect = sharedPreferences.getBoolean("autoConnect", false);
        if (autoConnect) {
            if (device.getDevice().getName() != null && device.getDevice().getName().equals("ESP32_doorlock")) {
                Log.i(TAG, "START AUTO CONNECT");

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("macAddress", device.getDevice().getAddress());
                editor.putString("deviceName", device.getDevice().getName());
                editor.commit();

                TextView curDevice = (TextView) getActivity().findViewById(R.id.cur_device);
//                String macAddress = sharedPreferences.getString("macAddress", "E8:6B:EA:D4:FC:D6");
//                String deviceName = sharedPreferences.getString("deviceName", "ESP32_doorlock");
                curDevice.setText(device.getDevice().getAddress() + " " + device.getDevice().getName());

                if (!getViewModel().isConnected() && requestBluetoothConnect != null) {
                    getViewModel().setMacAddress(getMacAddress());
                    Log.i(TAG, "MAC is " + getMacAddress());
                    startConnect();
                    showMsg("Connecting...");
                }
            }
        }
    }

    private boolean isOpenBluetooth() {
        BluetoothManager manager = (BluetoothManager) getActivity().getSystemService(BluetoothManager.class);
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
        return getActivity().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
}