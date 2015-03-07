package com.awesome.dlnamanager.upnp;

import android.content.Context;

import com.awesome.dlnamanager.proxy.IDeviceChangeListener;

import org.cybergarage.util.CommonLog;
import org.cybergarage.util.LogFactory;

public abstract class AbstractDeviceBrocastFactory {

	protected static final CommonLog log = LogFactory.createLog();
	
	protected Context mContext;
	protected DeviceChangeBrocastReceiver mReceiver;
	
	public AbstractDeviceBrocastFactory(Context context){
		mContext = context;
	}
	
	public abstract void registerListener(IDeviceChangeListener listener);
	public abstract void unRegisterListener();
	
}
