package com.hthuz.lockcompanion;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.hthuz.lockcompanion.databinding.ItemDeviceBinding;
import com.hthuz.lockcompanion.ui.dashboard.DashboardFragment;
import com.hthuz.lockcompanion.ui.home.HomeFragment;

import java.util.List;

public class MyDeviceAdapter extends RecyclerView.Adapter<MyDeviceAdapter.ViewHolder> {

    private final List<MyDevice> lists;
    private final static String TAG = "DEBUG";

    public MyDeviceAdapter(List<MyDevice> lists) {
        this.lists = lists;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDeviceBinding binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.getContext()),parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemDeviceBinding binding = DataBindingUtil.getBinding(holder.binding.getRoot());
        if (binding != null) {
            binding.setDevice(lists.get(position));
            binding.executePendingBindings();

        }
    }

    @Override
    public int getItemCount() {
        return lists.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ItemDeviceBinding binding;

        public ViewHolder(@NonNull ItemDeviceBinding itemTextDataRvBinding) {
            super(itemTextDataRvBinding.getRoot());
            binding = itemTextDataRvBinding;
            binding.deviceItem.setOnClickListener(v -> {
                MyDevice device = binding.getDevice();
                String name = device.getDevice().getName() != null ? device.getDevice().getName().toString() : "Unknown";
                String address = device.getDevice().getAddress().toString();
                if (!name.contains("ESP32")) {
                    showMsg("Only door lock can be connected");
                    return;
                }
                Log.i(TAG, name);
                Log.i(TAG, device.getDevice().getAddress());
                Values.macAddress = address;
                showMsg(name + " selected");
            });
        }

        public void showMsg(CharSequence msg) {
            Toast toast = Toast.makeText(this.itemView.getContext(), msg, Toast.LENGTH_SHORT);
            toast.show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    toast.cancel();
                }
            }, 500);
        }

    }
}
