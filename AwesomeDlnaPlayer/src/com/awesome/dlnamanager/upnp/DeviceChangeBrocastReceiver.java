package com.awesome.dlnamanager.upnp;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.awesome.dlnamanager.proxy.IDeviceChangeListener;

import org.cybergarage.util.CommonLog;
import org.cybergarage.util.LogFactory;

public class DeviceChangeBrocastReceiver extends BroadcastReceiver{

    public static final CommonLog log = LogFactory.createLog();
    protected IDeviceChangeListener mListener;

    public void setListener(IDeviceChangeListener listener) {
        mListener = listener;
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();


        if (DMSDeviceBrocastFactory.ADD_DEVICES.equalsIgnoreCase(action) ||
                DMSDeviceBrocastFactory.REMOVE_DEVICES.equalsIgnoreCase(action) ||
                DMSDeviceBrocastFactory.CLEAR_DEVICES.equalsIgnoreCase(action)) {
            boolean isSelDeviceChange = intent.getBooleanExtra(DMSDeviceBrocastFactory.REMOVE_EXTRA_FLAG, false);
            if (mListener != null) {
                mListener.onDeviceChange(isSelDeviceChange);
            }
        }

    }
}
