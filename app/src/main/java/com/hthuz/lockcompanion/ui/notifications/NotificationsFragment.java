package com.hthuz.lockcompanion.ui.notifications;

import android.app.DownloadManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.hthuz.lockcompanion.databinding.FragmentNotificationsBinding;

import org.json.JSONObject;

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
//                Looper.prepare();
//                showMsg("Connect failed");
//                Looper.loop();
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i(TAG, "RESP");
                try (ResponseBody responseBody = response.body()) {
                    String msg = responseBody.string();
                    Log.i(TAG, msg);
//                    binding.serverResp.setText(responseBody.string());
                    if (msg.isEmpty())
                        return;
                    try {
                        JSONObject jsonObject = new JSONObject(msg);
                        if (!jsonObject.has("content"))
                            return;
                        Log.i(TAG, jsonObject.getString("content"));
//                        binding.serverResp.setText(jsonObject.getString("content"));
                        showMsg(jsonObject.getString("content"));
                    } catch (Exception e) {e.printStackTrace();}

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

        binding.btnAssign.setOnClickListener(v -> {
            String url = "http://82.157.179.228:8080/assignment";
            try {
                run(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        binding.registerButton.setOnClickListener(v -> {
            Response response = null;
            String username = binding.regUsername.getText().toString();
            if (username.isEmpty()) {
                showMsg("Please enter username");
                return;
            }
            String token = binding.regToken.getText().toString();
            if (token.isEmpty()) {
                showMsg("Please enter password");
                return;
            }
            String lockid = binding.regLockid.getText().toString();
            if (lockid.isEmpty()) {
                showMsg("Please enter lock id");
                return;
            }
            Log.i(TAG, "register");
            String url = String.format("http://82.157.179.228:8080/register?token=%s&name=%s&ID=%s", token, username, lockid);
            try {
                run(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        binding.settingButton.setOnClickListener(v -> {
            Response response = null;
            String username = binding.setUsername.getText().toString();
            if (username.isEmpty()) {
                showMsg("Please enter username");
                return;
            }
            String token = binding.setToken.getText().toString();
            if (token.isEmpty()) {
                showMsg("Please enter password");
                return;
            }
            String password = binding.setPassword.getText().toString();
            if (password.isEmpty()) {
                showMsg("Please enter lock id");
                return;
            }
            Log.i(TAG, "setting password");
            String url = String.format("http://82.157.179.228:8080/setting_password?token=%s&name=%s&password=%s", token, username, password);
            try {
                run(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        binding.requestButton.setOnClickListener(v -> {
            Response response = null;
            String username = binding.reqUsername.getText().toString();
            if (username.isEmpty()) {
                showMsg("Please enter username");
                return;
            }
            String password = binding.reqPassword.getText().toString();
            if (password.isEmpty()) {
                showMsg("Please enter password");
                return;
            }
            String lockid = binding.reqLockid.getText().toString();
            if (lockid.isEmpty()) {
                showMsg("Please enter lock id");
                return;
            }
            Log.i(TAG, "request");
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

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT);
                toast.show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        toast.cancel();
                    }
                }, 500);
            }
        });

    }
}