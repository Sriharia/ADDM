package com.awesome.dlnamanager.workers;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.awesome.dlnamanager.DlnaShareApplication;
import com.awesome.dlnamanager.proxy.DlnaMediaProxy;
import com.awesome.dlnamanager.util.CommonUtil;

import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.device.DeviceChangeListener;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;

public class DlnaService extends Service implements IBaseEngine,
        DeviceChangeListener, SearchHandlerThread.ISearchDeviceListener {

    private static final String TAG = "DlnaService";

    public static final String SEARCH_DEVICES = "com.awesomeDlna.share.search_device";
    public static final String RESET_SEARCH_DEVICES = "com.awesomeDlna.share.reset_search_device";
    public static final String STOP_SERVICE = "com.awesomeDlna.share.stop_self";
    public static final String SEARCH_DEVICES_FAIL = "com.awesomeDlna.share.search_devices_fail";
    public static final String SEARCH_DEVICES_STATUS = "com.awesomeDlna.share.search_devices_status";
    public static final String SEARCH_DEVICES_END = "com.awesomeDlna.share.search_devices_end";


    private static final int NETWORK_CHANGE = 0x0001;
    private boolean firstReceiveNetworkChangeBR = true;
    private NetworkStatusChangeBR mNetworkStatusChangeBR;

    private ControlPoint mControlPoint;

    private SearchHandlerThread mCenterWorkThread;
    private DlnaMediaProxy mDlnaMediaProxy;
    private Handler mHandler, mWorkerHandler;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "DlnaService onCreate");
        init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (DlnaService.SEARCH_DEVICES.equals(action)) {
                startEngine();
            } else if (DlnaService.RESET_SEARCH_DEVICES.equals(action)) {
                restartEngine();
            } else if (STOP_SERVICE.equals(action))
                stopSelf();
        } else {
            Log.e(TAG, "intent = " + intent);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "DlnaService onDestroy");
        unInit();
        super.onDestroy();
    }

    private void init() {
        mDlnaMediaProxy = DlnaMediaProxy.getInstance(this);

        mControlPoint = new ControlPoint();
        DlnaShareApplication.getInstance().setControlPoint(mControlPoint);
        mControlPoint.addDeviceChangeListener(this);
        mControlPoint.addSearchResponseListener(new SearchResponseListener() {
            public void deviceSearchResponseReceived(SSDPPacket ssdpPacket) {
            }
        });

        mCenterWorkThread = new SearchHandlerThread(mControlPoint);
        mCenterWorkThread.setSearchListener(this);
        mHandler = new Handler() {

            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case NETWORK_CHANGE:
                        mDlnaMediaProxy.resetSearch();
                        break;
                }
            }

        };
        registerNetworkStatusBR();

        boolean ret = CommonUtil.aquireMultiLock(this);
        Log.e(TAG, "aquireMultiLock, multicastLock = " + ret);
    }

    private void unInit() {
        unRegisterNetworkStatusBR();
        DlnaShareApplication.getInstance().setControlPoint(null);
        if (null != mHandler)
            mHandler.removeCallbacksAndMessages(null);
        if (null != mCenterWorkThread && mCenterWorkThread.isAlive()) {
            mCenterWorkThread.setSearchListener(null);
            mCenterWorkThread.exit();
        }
    }

    @Override
    public boolean startEngine() {
        Log.e(TAG, "starting engine...");
        awakeWorkThread();
        return true;
    }

    @Override
    public boolean stopEngine() {
        exitWorkThread();
        return true;
    }

    @Override
    public boolean restartEngine() {
        mCenterWorkThread.reset();
        return true;
    }

    @Override
    public void deviceAdded(Device dev) {
        Log.e(TAG, "device added: " + dev.getDeviceType());
        mDlnaMediaProxy.addDevice(dev);
    }

    @Override
    public void deviceRemoved(Device dev) {
        Log.e(TAG, "device removed**");
        mDlnaMediaProxy.removeDevice(dev);
    }

    private void awakeWorkThread() {
        if (mCenterWorkThread.isAlive()) {
            Log.e(TAG, "existing thread instance found, reset..");
            mCenterWorkThread.reset();
        } else {
            Log.e(TAG, "starting new worker thread instance..");
            mCenterWorkThread.start();
            mCenterWorkThread.waitUntilReady();
            mWorkerHandler = mCenterWorkThread.getWorkerHandler();
            mWorkerHandler.sendEmptyMessage(SearchHandlerThread.START_CP);
        }
    }

    private void exitWorkThread() {
        if (mCenterWorkThread != null && mCenterWorkThread.isAlive()) {
            mCenterWorkThread.exit();
            long time1 = System.currentTimeMillis();
            while (mCenterWorkThread.isAlive()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            long time2 = System.currentTimeMillis();
            Log.e(TAG, "exitCenterWorkThread cost time:" + (time2 - time1));
            mCenterWorkThread = null;
        }
    }

    @Override
    public void onSearchStart() {
        sendSearchStatusBroadcast("start");
    }

    private void sendSearchStatusBroadcast(String status) {
        Intent intent = new Intent(SEARCH_DEVICES_STATUS);
        intent.putExtra("status", status);
        sendBroadcast(intent);
    }

    @Override
    public void onStartInit() {
        sendSearchStatusBroadcast("start");
    }

    @Override
    public void onSearchComplete(boolean searchSuccess) {
        sendSearchStatusBroadcast("end");
        if (!searchSuccess) {
            sendSearchDeviceFailBrocast(this);
        }
    }

    @Override
    public void onStartComplete(boolean startSuccess) {
        sendSearchStatusBroadcast("end");
    }

    public static void sendSearchDeviceFailBrocast(Context context) {
        Log.e(TAG, "sendSearchDeviceFailBrocast");
        Intent intent = new Intent(SEARCH_DEVICES_FAIL);
        context.sendBroadcast(intent);
    }

    private class NetworkStatusChangeBR extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent != null) {
                String action = intent.getAction();
                if (action != null) {
                    if (action
                            .equalsIgnoreCase(ConnectivityManager.CONNECTIVITY_ACTION)) {
                        sendNetworkChangeMessage();
                    }
                }
            }

        }

    }

    private void registerNetworkStatusBR() {
        if (mNetworkStatusChangeBR == null) {
            mNetworkStatusChangeBR = new NetworkStatusChangeBR();
            registerReceiver(mNetworkStatusChangeBR, new IntentFilter(
                    ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    private void unRegisterNetworkStatusBR() {
        if (mNetworkStatusChangeBR != null) {
            unregisterReceiver(mNetworkStatusChangeBR);
        }
    }

    private void sendNetworkChangeMessage() {
        if (firstReceiveNetworkChangeBR) {
            Log.e(TAG, "first receive the NetworkChangeMessage, so drop it...");
            firstReceiveNetworkChangeBR = false;
            return;
        }

        mHandler.removeMessages(NETWORK_CHANGE);
        mHandler.sendEmptyMessageDelayed(NETWORK_CHANGE, 500);
    }

}
