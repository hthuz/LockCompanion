package com.hthuz.lockcompanion.ui.dashboard;

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
import android.os.Bundle;
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

import com.hthuz.lockcompanion.databinding.FragmentDashboardBinding;
import com.hthuz.lockcompanion.BluetoothLeService;

import java.util.List;
import java.util.UUID;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

    private ActivityResultLauncher<String> requestBluetoothConnect;

//    public void OnCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Log.e(TAG, "fragment on create");
//        // start service
//        Log.e(TAG, "Before start service");
//        Log.e(TAG, "After start service");
//    }
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textDashboard;
        dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        requestBluetoothConnect = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
            getActivity().bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        });

        // bluetooth related
        initView();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public String testMAC = "E8:6B:EA:D4:FC:D6"; // another ESP32
    //        public String testMAC = "14:94:6C:C0:53:83"; // My iPhone
    public UUID ESP32_USER_SERVICE_UUID = UUID.fromString("12a59900-17cc-11ec-9621-0242ac130002");
    public UUID ESP32_RX_CHARACTERISTIC_UUID = UUID.fromString("12a59e0a-17cc-11ec-9621-0242ac130002");
    public UUID ESP32_TX_CHARACTERISTIC_UUID = UUID.fromString("12a5a148-17cc-11ec-9621-0242ac130002");

    public static final String TAG = "GATT_DEBUG";
    private BluetoothLeService bluetoothService;
    private boolean connected;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "onServiceConnected");
            bluetoothService = ((BluetoothLeService.LocalBinder) iBinder).getService();
            if ( bluetoothService != null) {
                if (!bluetoothService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    getActivity().finish();
                }
                // perform connection
                getActivity().registerReceiver(gattUpdateReceiver,makeGattUpdateIntentFilter());
                final boolean result = bluetoothService.connect(testMAC);
                Log.d(TAG, "Connect request result=" + result);
            }

        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothService = null;
        }
    };
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.e(TAG, "Recevie broadcasted action: " + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                // show connected message
                binding.connectState.setText("CONNECTED!");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                // show disconnected message
                binding.connectState.setText("DISCONNECTED!");

                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    Log.e(TAG, "Error");
                }
                final boolean result = bluetoothService.connect(testMAC);
                Log.d(TAG, "Connect request result=" + result);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // display supported services
                displayGattServices(bluetoothService.getSupportedGattServices());
            }
        }
    };

    private void initView() {

        binding.btnConnect.setOnClickListener(v -> {
            if (connected) {
                showMsg("DoorLock is Already Connected");
                return;
            }
            if (requestBluetoothConnect == null) {
                showMsg("request is null");
            }
            requestBluetoothConnect.launch(android.Manifest.permission.BLUETOOTH_CONNECT);

        });
        binding.btnUnlock.setOnClickListener(v -> {
            if (!connected) {
                showMsg("Please connect doorlock first");
                return;
            }
            BluetoothGattService esp32Service = bluetoothService.getGattServiceByUUID(ESP32_USER_SERVICE_UUID);
            if (esp32Service == null)
                return;
            BluetoothGattCharacteristic rxCharacteristic = esp32Service.getCharacteristic(ESP32_RX_CHARACTERISTIC_UUID);
            BluetoothGattCharacteristic txCharacteristic = esp32Service.getCharacteristic(ESP32_TX_CHARACTERISTIC_UUID);
            rxCharacteristic.setValue("3");
            bluetoothService.writeCharacteristic(rxCharacteristic);
            Log.i(TAG, "rx value: " + new String(rxCharacteristic.getValue()));
        });
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
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE_CHARACTERISTC);
        return intentFilter;
    }



    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if(gattServices == null) return;
        String msg = "Service number: " + gattServices.size();
        binding.testMsg.setText(msg);
        String service_info_msg = "";

        BluetoothGattCharacteristic txCharacteristic = null;
        for (BluetoothGattService gattService : gattServices) {
            service_info_msg += "Service UUID: " + gattService.getUuid() + "\n";
            if (!gattService.getUuid().equals(ESP32_USER_SERVICE_UUID))
                continue;

            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            int i = 0;
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                service_info_msg += "\n" + "Character #" + i + "\n";
                service_info_msg += "Characteristic UUID: " + gattCharacteristic.getUuid() + "\n";
                if (gattCharacteristic.getUuid().equals(ESP32_TX_CHARACTERISTIC_UUID)) {
                    txCharacteristic = gattCharacteristic;
                }
                service_info_msg += "Char value: " + gattCharacteristic.getValue() + "\n";
                i++;

                List<BluetoothGattDescriptor> gattDescriptors = gattCharacteristic.getDescriptors();
                int j = 0;
                for (BluetoothGattDescriptor gattDescriptor : gattDescriptors) {
                    service_info_msg += "Descriptor #" + j + "\n";
                    service_info_msg += "Descriptor UUID: " + gattDescriptor.getUuid() + "\n";
                    service_info_msg += "Descriptor value: " + gattDescriptor.getValue() + "\n";
                    j++;
                }
            }
        }
        // enable read characteristic
        if (txCharacteristic != null) {
            bluetoothService.setCharacteristicNotification(txCharacteristic, true);
        }

        binding.serviceTextInfo.setText(service_info_msg);

    }

    public void showMsg(CharSequence msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }
}