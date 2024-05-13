package com.hthuz.lockcompanion.ui.home;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hthuz.lockcompanion.MainActivity;
import com.hthuz.lockcompanion.MyDevice;
import com.hthuz.lockcompanion.MyDeviceAdapter;
import com.hthuz.lockcompanion.R;
import com.hthuz.lockcompanion.databinding.ActivityMainBinding;
import com.hthuz.lockcompanion.databinding.FragmentHomeBinding;
import com.hthuz.lockcompanion.ui.dashboard.DashboardFragment;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        registerIntent();
        Log.i(TAG, "Home fragment onCreateView");
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        getActivity().registerReceiver(selectDeviceReceiver, makeIntentFilter());
        // Init bluetooth related button
        initView();
        return root;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "Home fragment onDestroyView");
        getActivity().unregisterReceiver(selectDeviceReceiver);
        binding = null;
    }

    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Home fragment onDestroy");
    }
    public void onResume() {
        super.onResume();
        Log.i(TAG, "Home fragment onResume");
    }

    private ActivityResultLauncher<Intent> enableBluetooth;         // open bluetooth intent
    private ActivityResultLauncher<String> requestBluetoothConnect;
    private ActivityResultLauncher<String> requestBluetoothScan;
    private ActivityResultLauncher<String> requestLocation;
    private ActivityResultLauncher<Intent> connectBluetooth;

//    private final String TAG = MainActivity.class.getSimpleName();
    private final String TAG = "MY_DEBUG";
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

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addDeviceList(new MyDevice(result.getDevice(),result.getRssi()));
        }
    };

    private void initView() {

//        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("lock_companion", Context.MODE_PRIVATE);
//        boolean autoConnect = sharedPreferences.getBoolean("autoConnect", false);
//        Log.i(TAG, "autoconnect:" + String.valueOf(autoConnect));

        if (isOpenBluetooth()) {
            BluetoothManager manager = (BluetoothManager) getActivity().getSystemService(BluetoothManager.class);
            mBluetoothAdapter = manager.getAdapter();
            scanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
//        if (isOpenBluetooth() && autoConnect) {
//            scanBluetooth();
//        }

        // open bluetooth button event
        binding.btnOpenBluetooth.setOnClickListener(v -> {

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
        binding.btnScanBluetooth.setOnClickListener(v -> {
            scanBluetooth();
        });


//         connect button to start activity
//        binding.btnConnectBluetooth.setOnClickListener(v -> {
//            Intent intent_control = new Intent(this, DeviceControlActivity.class);
//            startActivity(intent_control);
//        });
        // initialize lists
        myDeviceAdapter = new MyDeviceAdapter(deviceList, getActivity());
        binding.rvDevice.setLayoutManager(new LinearLayoutManager(getActivity()));

        binding.rvDevice.setAdapter(myDeviceAdapter);
    }

    private void scanBluetooth() {
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
    }
    private void startScan() {
        if (!isScanning) {
            if (scanner == null) {
                Log.e(TAG, "scanner is null");
            }
            scanner.startScan(scanCallback);
            isScanning = true;
            binding.btnScanBluetooth.setText("Stop scanning");
        }
    }

    private void stopScan() {
        if (isScanning) {
            scanner.stopScan(scanCallback);
            isScanning = false;
            binding.btnScanBluetooth.setText("Start Scanning");
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

        // If auto connect
//        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("lock_companion", Context.MODE_PRIVATE);
//        boolean autoConnect = sharedPreferences.getBoolean("autoConnect", false);
//        if (autoConnect) {
//            if (device.getDevice().getName() != null && device.getDevice().getName().equals("ESP32_doorlock")) {
//
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.putString("macAddress", device.getDevice().getAddress());
//                editor.putString("deviceName", device.getDevice().getName());
//                editor.commit();
//                Log.i(TAG, "NAV!");
//
////                NavController navController = NavHostFragment.findNavController(this);
//                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
//                navController.navigate(R.id.navigation_dashboard);
//
//
//            }
//        }
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

    private BroadcastReceiver selectDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (MyDeviceAdapter.ACTION_DEVICE_SELECTED.equals(action)) {
                TextView curDevice = (TextView) getActivity().findViewById(R.id.cur_device);
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("lock_companion", Context.MODE_PRIVATE);
                String macAddress = sharedPreferences.getString("macAddress", "E8:6B:EA:D4:FC:D6");
                String deviceName = sharedPreferences.getString("deviceName", "ESP32_doorlock");
                curDevice.setText(deviceName + " " + macAddress);
            }
        }
    };

    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MyDeviceAdapter.ACTION_DEVICE_SELECTED);
        return intentFilter;
    }
}