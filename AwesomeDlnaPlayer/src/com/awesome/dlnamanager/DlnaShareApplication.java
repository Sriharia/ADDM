package com.awesome.dlnamanager;

import android.app.Application;

import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.util.CommonLog;
import org.cybergarage.util.LogFactory;

public class DlnaShareApplication extends Application {

    private static final CommonLog log = LogFactory.createLog();

    private ControlPoint mControlPoint;

    private static DlnaShareApplication mDlnaShareApplication;

    public static DlnaShareApplication getInstance() {
        return mDlnaShareApplication;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();

        mDlnaShareApplication = this;

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
                getApplicationContext())
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .memoryCacheSize(4 * 1024 * 1024)
                .denyCacheImageMultipleSizesInMemory()
                .memoryCache(new WeakMemoryCache())
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                        // .enableLogging() // Not necessary in common
                .build();
        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config);

    }

    public void setControlPoint(ControlPoint controlPoint) {
        mControlPoint = controlPoint;
    }

    public ControlPoint getControlPoint() {
        return mControlPoint;
    }

}
