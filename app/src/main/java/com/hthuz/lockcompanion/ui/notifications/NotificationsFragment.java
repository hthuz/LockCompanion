package com.hthuz.lockcompanion.ui.notifications;

import android.app.DownloadManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
    final static String TAG = "HTTP_DEBUG";

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

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textNotifications;
        notificationsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        initView();

        return root;
    }

    private void initView() {
        binding.btnRequest.setOnClickListener(v -> {
            Response response = null;
            try {
                run("http://82.157.179.228:8080/user?name=Bob&ID=1&password=123456");
            } catch (IOException e) {
                e.printStackTrace();
            }

        });



    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}