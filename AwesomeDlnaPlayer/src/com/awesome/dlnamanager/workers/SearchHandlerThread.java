package com.awesome.dlnamanager.workers;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import org.cybergarage.upnp.ControlPoint;

public class SearchHandlerThread extends HandlerThread {

    private Handler mWorkerHandler;

    private static final String TAG = "ControlCenterWorkThread";

    public static final int START_CP = 10001;
    public static final int INITIATE_SEACRH = 10002;
    private static final int REFRESH_DEVICES_INTERVAL = 30 * 1000;

    public static interface ISearchDeviceListener {
        public void onSearchStart();

        public void onSearchComplete(boolean searchSuccess);

        public void onStartInit();

        public void onStartComplete(boolean startSuccess);
    }

    private ControlPoint mCP = null;
    private ISearchDeviceListener mSearchDeviceListener;

    public Handler getWorkerHandler() {
        return mWorkerHandler;
    }

    public SearchHandlerThread(ControlPoint controlPoint) {
        super("SearchHandlerThread");
        mCP = controlPoint;
    }

    public synchronized void waitUntilReady() {
        mWorkerHandler = new Handler(getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case START_CP:
                        mSearchDeviceListener.onStartInit();
                        boolean startRet = mCP.start();
                        Log.e(TAG, "mCP.start() ret = " + startRet + ", schedule seach in 30secs");
                        mSearchDeviceListener.onStartComplete(startRet);
//                        mStartComplete = startRet;
                        mWorkerHandler.sendEmptyMessageDelayed(INITIATE_SEACRH, REFRESH_DEVICES_INTERVAL);
                        break;
                    case INITIATE_SEACRH:
                        mSearchDeviceListener.onSearchStart();
                        boolean searchRet = mCP.search();
                        Log.e(TAG, "mCP.search() ret = " + searchRet);
                        if (mSearchDeviceListener != null)
                            mSearchDeviceListener.onSearchComplete(searchRet);
                        mWorkerHandler.sendEmptyMessageDelayed(INITIATE_SEACRH, REFRESH_DEVICES_INTERVAL);
                        break;
                }
                return true;
            }
        });
    }

    public void setSearchListener(ISearchDeviceListener listener) {
        mSearchDeviceListener = listener;
    }


    public void reset() {
        if (null == mWorkerHandler)
            waitUntilReady();
        else
            mWorkerHandler.removeCallbacksAndMessages(null);
        mWorkerHandler.sendEmptyMessage(START_CP);
    }

    public void exit() {
//        mIsExit = true;
        mCP.stop();
        mWorkerHandler.removeCallbacksAndMessages(null);
        quit();
    }


}
