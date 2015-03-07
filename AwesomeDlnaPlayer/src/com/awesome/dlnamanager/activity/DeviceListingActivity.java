package com.awesome.dlnamanager.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.awesome.dlnamanager.R;
import com.awesome.dlnamanager.adapter.DeviceAdapter;
import com.awesome.dlnamanager.proxy.DlnaMediaProxy;
import com.awesome.dlnamanager.proxy.IDeviceChangeListener;
import com.awesome.dlnamanager.upnp.DMSDeviceBrocastFactory;
import com.awesome.dlnamanager.upnp.DummyDevice;
import com.awesome.dlnamanager.upnp.MediaItemFactory;
import com.awesome.dlnamanager.workers.DlnaService;

import org.cybergarage.upnp.Device;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DeviceListingActivity extends BaseActivity implements IDeviceChangeListener {

    private static final int REQUEST_PICK_FILE = 10000;

    // private TextView m_local_server;
    private ListView mServerDevLv, mRendererDevLv;

    private DeviceAdapter mSDevAdapter, mRDevAdapter;
    private DlnaMediaProxy mDlnaMediaProxy;
    private BroadcastReceiver mSearchStatus;
    private DMSDeviceBrocastFactory mBrocastFactory;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dms_layout);

        initView();
        initData();
        mDlnaMediaProxy.startSearch();
    }


    private void initView() {
        mServerDevLv = (ListView) findViewById(R.id.device_list_servers);
        mRendererDevLv = (ListView) findViewById(R.id.device_list_renderers);
        mServerDevLv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Device device = (Device) parent.getItemAtPosition(position);
                if (device instanceof DummyDevice)
                    startFilePickerActivity();
                else {
                    mDlnaMediaProxy.setDMSSelectedDevice(device);
                    goContentActivity();
                }
            }
        });
        mRendererDevLv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Device device = (Device) parent.getItemAtPosition(position);
                mDlnaMediaProxy
                        .setDMRSelectedDevice(device instanceof DummyDevice ? null
                                : device);
                if (!(device instanceof DummyDevice))
                    mDlnaMediaProxy.stopLocalFileServer();
            }
        });
    }

    private void initData() {
        mDlnaMediaProxy = DlnaMediaProxy.getInstance(this);
        mSDevAdapter = new DeviceAdapter(this, new ArrayList<Device>(), false);
        View v1 = getLayoutInflater().inflate(
                R.layout.content_device_listing_header, null);
        ((TextView) v1.findViewById(R.id.device_list_header))
                .setText("Play FROM:");
        mServerDevLv.addHeaderView(v1);
        View v2 = getLayoutInflater().inflate(
                R.layout.content_device_listing_header, null);
        ((TextView) v2.findViewById(R.id.device_list_header))
                .setText("Play TO");
        mRendererDevLv.addHeaderView(v2);
        mServerDevLv.setAdapter(mSDevAdapter);
        mRDevAdapter = new DeviceAdapter(this, new ArrayList<Device>(), true);
        mRendererDevLv.setAdapter(mRDevAdapter);
        mRendererDevLv.setSelection(0);
        mBrocastFactory = new DMSDeviceBrocastFactory(this);
        mBrocastFactory.registerListener(this);
    }

    protected void startFilePickerActivity() {
        Intent intent = new Intent(this, FilePickerActivity.class);
        startActivityForResult(intent, REQUEST_PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_FILE:
                    if (data.hasExtra(FilePickerActivity.EXTRA_FILE_PATH)) {
                        // Get the file path
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) data
                                .getSerializableExtra(FilePickerActivity.EXTRA_FILE_PATH);
                        // Print the File/Folder names
                        if (files.size() == 1
                                && null != mDlnaMediaProxy.getDMRSelectedDevice())
                            mDlnaMediaProxy.serveLocalFile(files.get(0));
                        if (data.hasExtra("gotoImages")) {
                            Log.e("DMS", "found image loc list, starting activty");
                            Intent intent = new Intent(getApplicationContext(),
                                    ImageDisplayActivity.class);
                            intent.putExtra("isLocal", true);
                            MediaItemFactory.putItemToIntent(
                                    MediaItemFactory.getItemFromIntent(data),
                                    intent);
                            // intent.putExtra("selectedImagePosition",
                            // data.getIntExtra("selectedImagePosition", 0));
                            startActivity(intent);
                        }
                    }
            }
        }
    }

    private void updateDeviceList() {
        mSDevAdapter.refreshData(mDlnaMediaProxy.getDMSDeviceList());
        mRDevAdapter.refreshData(mDlnaMediaProxy.getDMRDeviceList());
    }

    private Menu optionsMenu;

    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.ic_menu_refresh, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void setRefreshActionButtonState(final boolean refreshing) {
        if (optionsMenu != null) {
            final MenuItem refreshItem = optionsMenu
                    .findItem(R.id.airport_menuRefresh);
            if (refreshItem != null) {
                if (refreshing) {
                    refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
                } else {
                    refreshItem.setActionView(null);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDeviceList();
        if (null == mSearchStatus)
            mSearchStatus = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String status = intent.getStringExtra("status");
                    if (status == null)
                        return;
                    setRefreshActionButtonState(status.equals("start"));
                }
            };
        registerReceiver(mSearchStatus, new IntentFilter(DlnaService.SEARCH_DEVICES_STATUS));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.airport_menuRefresh) {
            mDlnaMediaProxy.resetSearch();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mSearchStatus);
    }

    @Override
    protected void onDestroy() {
        // m_local_DLNA_server.stop();
        mDlnaMediaProxy.stopLocalFileServer();
        mBrocastFactory.unRegisterListener();
        mDlnaMediaProxy.exitSearch();
        super.onDestroy();
    }

    @Override
    public void onDeviceChange(boolean isSelDeviceChange) {
        updateDeviceList();
    }

    private void goContentActivity() {
        Intent intent = new Intent(this, ContentListingActivity.class);
        startActivity(intent);
    }

}