package com.awesome.dlnamanager.adapter;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.awesome.dlnamanager.upnp.DummyDevice;
import com.awesome.dlnamanager.R;

import org.cybergarage.upnp.Device;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends BaseAdapter {

	private List<Device> devices = new ArrayList<Device>();
	private LayoutInflater mInflater;
	private Context mContext;
	private boolean isRendererList;

	public DeviceAdapter(Context context, List<Device> devices,
			boolean isRendererList) {
		mInflater = LayoutInflater.from(context);
		devices.add(getLocalDevice());
		this.devices.addAll(devices);
		mContext = context;
		this.isRendererList = isRendererList;
	}

	private Device getLocalDevice() {
		DummyDevice d = new DummyDevice();
		// d.setFriendlyName(getLocalDeviceName());
		// d.setDeviceType("Awesome" + (isRendererList ? "Renderer" :
		// "Sender"));
		return d;
	}

	private String getLocalDeviceName() {
		String name;
		try {
			if (Build.MODEL.contains(Build.MANUFACTURER))
				name = Build.MODEL;
			else
				name = Build.MANUFACTURER + " " + Build.MODEL;
			name += " (Local)";
		} catch (Exception e) {
			return "Local File Structure";
		}
		return name;
	}

	public void refreshData(List<Device> devices) {
		this.devices.clear();
		this.devices.add(getLocalDevice());
		this.devices.addAll(devices);
		notifyDataSetChanged();
	}

	/**
	 * The number of items in the list is determined by the number of speeches
	 * in our array.
	 * 
	 * @see android.widget.ListAdapter#getCount()
	 */
	public int getCount() {
		return devices.size();
	}

	/**
	 * Since the data comes from an array, just returning the index is sufficent
	 * to get at the data. If we were using a more complex data structure, we
	 * would return whatever object represents one row in the list.
	 * 
	 * @see android.widget.ListAdapter#getItem(int)
	 */
	public Object getItem(int position) {
		return devices.get(position);
	}

	/**
	 * Use the array index as a unique id.
	 * 
	 * @see android.widget.ListAdapter#getItemId(int)
	 */
	public long getItemId(int position) {
		return position;
	}

	/**
	 * Make a view to hold each row.
	 * 
	 * @see android.widget.ListAdapter#getView(int, android.view.View,
	 *      android.view.ViewGroup)
	 */

	public View getView(int position, View convertView, ViewGroup parent) {

		if (convertView == null) {
			convertView = mInflater
					.inflate(
							isRendererList ? R.layout.renderer_listitem_layout
									: android.R.layout.simple_list_item_1, null);
		}

		Device dataItem = (Device) getItem(position);
		TextView geckoView = (TextView) convertView
				.findViewById(android.R.id.text1);
		geckoView
				.setText(dataItem instanceof DummyDevice ? getLocalDeviceName()
						: dataItem.getFriendlyName());
		// TextView locationView = (TextView) convertView
		// .findViewById(R.id.ctrl_list_item_location);
		// locationView.setText(dataItem.getLocation());
		// Log.e("DeviceAdapter", "root xml: " + dataItem.getLocation());
		// TextView uuidView = (TextView) convertView
		// .findViewById(R.id.ctrl_list_item_uuid);
		// uuidView.setText(dataItem.getUDN());
		// TextView typeView = (TextView) convertView
		// .findViewById(R.id.ctrl_list_item_type);
		// typeView.setText(dataItem.getDeviceType());

		return convertView;
	}
}
