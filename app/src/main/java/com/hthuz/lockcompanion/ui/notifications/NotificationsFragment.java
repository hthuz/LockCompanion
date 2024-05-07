package com.hthuz.lockcompanion.ui.notifications;

import android.app.DownloadManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.hthuz.lockcompanion.databinding.FragmentNotificationsBinding;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private OkHttpClient client = new OkHttpClient();
    final static String TAG = "MY_DEBUG";

    public void run(String url) throws IOException {

        Request request = new Request.Builder()
            .url(url)
            .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    Log.i(TAG, responseBody.string());
                }
            }
        });


    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        Log.i(TAG, "Notification fragment onCreateView");
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textNotifications;
        notificationsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        initView();

        return root;
    }

    private void initView() {

        binding.loginButton.setOnClickListener(v -> {
            Response response = null;
            String username = binding.username.getText().toString();
            if (username.isEmpty()) {
                showMsg("Please enter username");
                return;
            }
            String password = binding.password.getText().toString();
            if (password.isEmpty()) {
                showMsg("Please enter password");
                return;
            }
            String lockid = binding.lockid.getText().toString();
            if (lockid.isEmpty()) {
                showMsg("Please enter lock id");
                return;
            }
            String url = String.format("http://82.157.179.228:8080/user?name=%s&ID=%s&password=%s", username, lockid, password);
            try {
                run(url);
            } catch (IOException e) {
                e.printStackTrace();
            }

        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        Log.i(TAG, "Notification fragment onDestroyView");
    }
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Notification fragment onDestroy");
    }
    public void onResume() {
        super.onResume();
        Log.i(TAG, "Notification fragment onResume");
    }

    public void showMsg(CharSequence msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }
}