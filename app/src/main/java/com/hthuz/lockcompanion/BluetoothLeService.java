package com.hthuz.lockcompanion;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.hthuz.lockcompanion.ui.dashboard.DashboardFragment;

import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
    private Binder binder = new LocalBinder();
//    public BluetoothLeService() {
//    }
    public static final String TAG = "GATT_DEBUG";
    private BluetoothAdapter adapter;
    private BluetoothGatt bluetoothGatt; // used to close connection when service no longer needed
    public final static String ACTION_GATT_CONNECTED = "com.hthuz.lockcompanion.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.hthuz.lockcompanion.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.hthuz.lockcompanion.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_CHARACTERISTIC_READ = "com.hthuz.lockcompanion.ACTION_DATA_AVAILABLE_CHARACTERISTIC";
    public final static String ACTION_CHARACTERISTIC_WRITE = "com.hthuz.lockcompanion.ACTION_CHARACTERISTIC_WRITE";
    public final static String ACTION_CHARACTERISTIC_CHANGED = "com.hthuz.lockcompanion.ACTION_CHARACTERISTIC_CHANGED";
    public final static String ACTION_READ_REMOTE_RSSI = "com.hthuz.lockcompanion.ACTION_READ_REMOTE_RSSI";
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTED = 2;
    private int connectState;

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.i(TAG,"onConnectionStateChange");
            //successfully connected to gatt server
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "New state is connected");
                connectState = STATE_CONNECTED;
                broadcastUpdate(ACTION_GATT_CONNECTED);
                // Discover services after connection
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "New state is disconnected");
                connectState = STATE_DISCONNECTED;
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            super.onServicesDiscovered(gatt, status);
            Log.i(TAG, "onServiceDiscovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "broadcast service discovered");
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServiceDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS){
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    String rdata = new String(data);
                    Log.e(TAG, rdata);
                    // broadcast data
                    Intent intent = new Intent(ACTION_CHARACTERISTIC_READ);
                    intent.putExtra("readValue", rdata);
                    sendBroadcast(intent);
                }
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicWrite");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_CHARACTERISTIC_WRITE);
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i(TAG, "onCharacteristicChanged");

            byte[] data = characteristic.getValue();
            if (data != null & data.length > 0) {
                String rdata = new String(data);
                Log.i(TAG, "tx value:" + rdata);

                // broadcast data
                Intent intent = new Intent(ACTION_CHARACTERISTIC_CHANGED);
                intent.putExtra("readValue", rdata);
                sendBroadcast(intent);
            }
        }
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Intent intent = new Intent(ACTION_READ_REMOTE_RSSI);
                intent.putExtra("rssi", rssi);
                sendBroadcast(intent);
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    public boolean initialize() {
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Log.e(TAG, "Unable to obtain bluetooth adapter");
            return false;
        }
        Log.e(TAG, "Service initialized!");
        return true;
    }
    // called by activity to initiate connection
    public boolean connect(final String address) {
        if (adapter == null || address == null) {
            Log.w(TAG,"Bluetoothadapter not initalized or unspecified address");
            return false;
        }
        try {
            final BluetoothDevice device = adapter.getRemoteDevice(address);
            // connect to Gatt server on device
            Log.e(TAG, "device name:" + device.getName());
            Log.e(TAG, "device string" + device.toString());
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
            Log.e(TAG, "connect over");
            return true;
        } catch (IllegalArgumentException e) {
            Log.w(TAG,"Device not found with provided address");
            return false;
        }
        // connect to GATT server on device.
    }
    // when connect/disconnect to GATT, service notify activity
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        return super.onUnbind(intent);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;
        Log.i(TAG, "number of services: " + bluetoothGatt.getServices().size());
        return bluetoothGatt.getServices();
    }
    public BluetoothGattService getGattServiceByUUID(UUID uuid) {
        if (bluetoothGatt == null) return null;
        return bluetoothGatt.getService(uuid);
    }
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothGatt == null) {
            Log.e(TAG, "BluetoothGatt not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothGatt == null) {
            Log.e(TAG, "BluetoothGatt not initialized");
            return;
        }
        Log.i(TAG,"call writeCharacteristic");
        bluetoothGatt.writeCharacteristic(characteristic);
    }

    public void writeDescriptor(BluetoothGattDescriptor descriptor) {
        if (bluetoothGatt == null) {
            Log.e(TAG, "BluetoothGatt not initialized");
            return;
        }
        bluetoothGatt.writeDescriptor(descriptor);
    }

    public boolean readRemoteRssi() {
        return bluetoothGatt.readRemoteRssi();
    }
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        Log.i(TAG, "call setCharacteristicNotification");
        if (bluetoothGatt == null) {
            Log.e(TAG, "BluetoothGatt not initialized");
            return;
        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }
    public BluetoothGattCharacteristic getTXCharacteristc() {
        BluetoothGattService esp32Service = getGattServiceByUUID(DashboardFragment.ESP32_USER_SERVICE_UUID);
        if (esp32Service == null)
            return null;
//        BluetoothGattCharacteristic rxCharacteristic = esp32Service.getCharacteristic(DashboardFragment.ESP32_RX_CHARACTERISTIC_UUID);
        BluetoothGattCharacteristic txCharacteristic = esp32Service.getCharacteristic(DashboardFragment.ESP32_TX_CHARACTERISTIC_UUID);
        return txCharacteristic;
    }
}