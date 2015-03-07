package com.awesome.dlnamanager.upnp;

import android.content.Context;

import org.cybergarage.upnp.Device;
import org.cybergarage.util.CommonLog;
import org.cybergarage.util.LogFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMediaMng {

    public static final CommonLog log = LogFactory.createLog();
    protected Context mContext;

    protected List<Device> mServerDeviceList = new ArrayList<Device>();
    protected List<Device> mRendererDeviceList = new ArrayList<Device>();
    protected Device mSelectedRenderingDevice, mSelectedServerDevice;

    public abstract void sendAddBrocast(Context context);

    public abstract void sendRemoveBrocast(Context context, boolean isSelected);

    public abstract void sendClearBrocast(Context context);

    public AbstractMediaMng(Context context) {
        mContext = context;
    }

    public synchronized List<Device> getServerDeviceList() {
        return mServerDeviceList;
    }

    public synchronized List<Device> getRendererDeviceList() {
        return mRendererDeviceList;
    }

    /**
     * @return the selectedDevice
     */
    public Device getSelectedRenderingDevice() {
        return mSelectedRenderingDevice;
    }

    public Device getSelectedServerDevice() {
        return mSelectedServerDevice;
    }

    public void setSelectedServerDevice(Device mSelectedServerDevice) {
        this.mSelectedServerDevice = mSelectedServerDevice;
    }

    /**
     * @param selectedDevice the selectedDevice to set
     */
    public void setSelectedRenderingDevice(Device selectedDevice) {
        mSelectedRenderingDevice = selectedDevice;
    }

    public synchronized void addServerDevice(Device d) {
        mServerDeviceList.add(d);
        sendAddBrocast(mContext);
    }

    public synchronized void addRendererDevice(Device d) {
        mRendererDeviceList.add(d);
        sendAddBrocast(mContext);
    }

    public synchronized void removeServerDevice(Device d) {
        int size = mServerDeviceList.size();
        for (int i = 0; i < size; i++) {
            String udnString = mServerDeviceList.get(i).getUDN();
            if (d.getUDN().equalsIgnoreCase(udnString)) {
                Device device = mServerDeviceList.remove(i);
//                Log.e("AMM", "mSelectedServerDevice: " + mSelectedServerDevice + ", device to be removed: " + d.getUDN());
                if (null != mSelectedServerDevice && mSelectedServerDevice.getUDN().equalsIgnoreCase(
                        device.getUDN()))
                    setSelectedServerDevice(null);
                sendRemoveBrocast(mContext, mSelectedServerDevice == null ? false : mSelectedServerDevice.getUDN()
                        .equalsIgnoreCase(device.getUDN()));
                return;
            }
        }
    }

    public synchronized void removeRendererDevice(Device d) {
        int size = mRendererDeviceList.size();
        for (int i = 0; i < size; i++) {
            String udnString = mRendererDeviceList.get(i).getUDN();
            if (d.getUDN().equalsIgnoreCase(udnString)) {
                Device device = mRendererDeviceList.remove(i);

                boolean ret = false;
                if (mSelectedRenderingDevice != null) {
                    ret = mSelectedRenderingDevice.getUDN().equalsIgnoreCase(
                            device.getUDN());
                }
                if (ret) {
                    setSelectedRenderingDevice(null);
                }
                sendRemoveBrocast(mContext, ret);
                return;
            }
        }
    }

    public synchronized void clear() {
        if (null != mServerDeviceList)
            mServerDeviceList.clear();
        if (null != mRendererDeviceList)
            mRendererDeviceList.clear();
        mSelectedServerDevice = null;
        mSelectedRenderingDevice = null;
        sendClearBrocast(mContext);
    }

}
