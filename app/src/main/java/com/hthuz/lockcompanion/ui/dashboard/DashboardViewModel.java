package com.hthuz.lockcompanion.ui.dashboard;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.AndroidViewModel;

import com.hthuz.lockcompanion.BluetoothLeService;
import com.hthuz.lockcompanion.Values;

import java.util.List;
import java.util.UUID;

public class DashboardViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<String> mState;
    private final MutableLiveData<Boolean> mConnected;

    private static final String TAG = "MY_DEBUG";

    private BluetoothLeService bluetoothService;

    public class ReadRssiThread extends Thread {
        private volatile boolean running = true;
        @Override
        public void run() {
            while (true) {
                if (running && isConnected()) {
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        // Handle interruption
                    }
                    if (isConnected()) {
                        Log.i(TAG, "LOOP");
                        bluetoothService.readRemoteRssi();
//                        BluetoothGattService esp32Service = getBluetoothService().getGattServiceByUUID(DashboardFragment.ESP32_USER_SERVICE_UUID);
//                        if (esp32Service != null) {
////                            BluetoothGattCharacteristic rxCharacteristic = esp32Service.getCharacteristic(DashboardFragment.ESP32_RX_CHARACTERISTIC_UUID);
//                            BluetoothGattCharacteristic txCharacteristic = esp32Service.getCharacteristic(DashboardFragment.ESP32_TX_CHARACTERISTIC_UUID);
//                            getBluetoothService().readCharacteristic(txCharacteristic);
//                        }

                    }
                }
            }
        }
        // Method to stop the thread
        public void stopThread() {
            running = false;
        }
        public void startThread() {
            running = true;
        }
    }
    ReadRssiThread readRssiThread;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "onServiceConnected");
            bluetoothService = ((BluetoothLeService.LocalBinder) iBinder).getService();
            if ( bluetoothService != null) {
                if (!bluetoothService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                }

                // perform connection
                final boolean result = bluetoothService.connect(macAddress);
                Log.d(TAG, "Connect request result: " + result);
            }

        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothService = null;
        }
    };


    public void displayGattServices(List<BluetoothGattService> gattServices) {
        if(gattServices == null) return;
//        String msg = "Service number: " + gattServices.size();
//        binding.testMsg.setText(msg);
        String service_info_msg = "";

        BluetoothGattCharacteristic txCharacteristic = null;
        for (BluetoothGattService gattService : gattServices) {
            service_info_msg += "Service UUID: " + gattService.getUuid() + "\n";
            if (!gattService.getUuid().equals(DashboardFragment.ESP32_USER_SERVICE_UUID))
                continue;

            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            int i = 0;
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                service_info_msg += "\n" + "Character #" + i + "\n";
                service_info_msg += "Characteristic UUID: " + gattCharacteristic.getUuid() + "\n";
                if (gattCharacteristic.getUuid().equals(DashboardFragment.ESP32_TX_CHARACTERISTIC_UUID)) {
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
            List<BluetoothGattDescriptor> descriptors = txCharacteristic.getDescriptors();
            for (BluetoothGattDescriptor descriptor : descriptors) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothService.writeDescriptor(descriptor);
            }
        }
//        binding.serviceTextInfo.setText(service_info_msg);

    }
    private String macAddress = "E8:6B:EA:D4:FC:D6";

    public DashboardViewModel() {
        mText = new MutableLiveData<>();
        mState = new MutableLiveData<>();
        mConnected = new MutableLiveData<>();
        mText.setValue("Lock Companion Unlock");
        mState.setValue("DISCONNECTED");
        mConnected.setValue(false);
        Values.connected = false;
        readRssiThread = new ReadRssiThread();
    }

    public void setMacAddress(String address) {
        macAddress = address;
    }
    public BluetoothLeService getBluetoothService() {
        return bluetoothService;
    }
    public ServiceConnection getServiceConnection() { return serviceConnection; }

    public ReadRssiThread getReadRssiThread() {
        return readRssiThread;
    }
    public void setState(String state) {
        mState.setValue(state);
    }
    public String getState() {return mState.getValue();}
    public void setConnected(Boolean connected) {
        mConnected.setValue(connected);
        Values.connected = connected;
    }
    public Boolean isConnected() {return mConnected.getValue();}
    public LiveData<String> getText() {
        return mText;
    }
}