package com.hthuz.lockcompanion.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DashboardViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<String> mState;
    private final MutableLiveData<Boolean> mConnected;

    public DashboardViewModel() {
        mText = new MutableLiveData<>();
        mState = new MutableLiveData<>();
        mConnected = new MutableLiveData<>();
        mText.setValue("Lock Companion Unlock");
        mState.setValue("state");
        mConnected.setValue(false);
    }

    public void setState(String state) {
        mState.setValue(state);
    }
    public String getState() {return mState.getValue();}
    public void setConnected(Boolean connected) {
        mConnected.setValue(connected);
    }
    public Boolean isConnected() {return mConnected.getValue();}
    public LiveData<String> getText() {
        return mText;
    }
}