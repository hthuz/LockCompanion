package com.hthuz.lockcompanion.ui.dashboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.hthuz.lockcompanion.Values;
import com.hthuz.lockcompanion.databinding.FragmentDashboardBinding;
import com.hthuz.lockcompanion.BluetoothLeService;

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


        if (getViewModel().getReadRssiThread().getState() == Thread.State.NEW) {
            getViewModel().getReadRssiThread().start();
        }

        getViewModel().getReadRssiThread().startThread();
        // bluetooth related
        Log.i(TAG, "connected is " + getViewModel().isConnected());

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
                if (readValue.equals("Door Opening...")) {
                    getViewModel().setEtime(System.currentTimeMillis());
                    double time_dif = (getViewModel().getEtime() - getViewModel().getStime()) / 1000.0;
                    Log.i(TAG, String.valueOf(time_dif));
                    Log.i(TAG, "Unlocked!");
                    showMsg("Unlocked!");
                    showSnack(String.valueOf(time_dif) + "s");
                }
            } else if (BluetoothLeService.ACTION_READ_REMOTE_RSSI.equals(action)) {
                int rssi = intent.getIntExtra("rssi", 0);
                binding.rssiValue.setText("rssi: " + rssi);
                if (getViewModel().isConnected() && rssi < 0 && rssi > -45) {
                    getViewModel().setStime(System.currentTimeMillis());
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
            getViewModel().setStime(System.currentTimeMillis());
            writeESP32("5");
            showMsg("Unlocking...");
        });
        binding.btnEnroll.setOnClickListener(v -> {
            if (!getViewModel().isConnected()) {
                showMsg("Please connect doorlock first");
                return;
            }
            writeESP32("1");
            showMsg("Enrolling...");
        });
        binding.btnEmpty.setOnClickListener(v -> {
            if (!getViewModel().isConnected()) {
                showMsg("Please connect doorlock first");
                return;
            }
            writeESP32("2");
            showMsg("Emptying...");
        });
        binding.btnGetnum.setOnClickListener(v -> {
            if (!getViewModel().isConnected()) {
                showMsg("Please connect doorlock first");
                return;
            }
            writeESP32("3");
            showMsg("Getting...");


        });
        binding.btnReadvalue.setOnClickListener((v -> {
            if (!getViewModel().isConnected()) {
                showMsg("Please connect doorlock first");
                return;
            }
            readESP32();
        }));
        binding.btnDisconnect.setOnClickListener((v -> {
            if (!getViewModel().isConnected()) {
                showMsg("Device is already disconnected");
                return;
            }
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
        getActivity().unregisterReceiver(gattUpdateReceiver);
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
}