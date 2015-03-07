package com.awesome.dlnamanager.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.awesome.dlnamanager.R;
import com.awesome.dlnamanager.adapter.ContentAdapter;
import com.awesome.dlnamanager.controller.IController;
import com.awesome.dlnamanager.controller.RemoteDlnaController;
import com.awesome.dlnamanager.proxy.BrowseDMSManager;
import com.awesome.dlnamanager.proxy.DlnaMediaProxy;
import com.awesome.dlnamanager.proxy.BrowseDMSManager.BrowseRequestCallback;
import com.awesome.dlnamanager.proxy.IDeviceChangeListener;
import com.awesome.dlnamanager.proxy.MediaManager;
import com.awesome.dlnamanager.upnp.DMSDeviceBrocastFactory;
import com.awesome.dlnamanager.upnp.MediaItem;
import com.awesome.dlnamanager.upnp.UpnpUtil;
import com.awesome.dlnamanager.util.CommonUtil;

import org.cybergarage.upnp.Device;

import java.util.ArrayList;
import java.util.List;

public class ContentListingActivity extends BaseActivity implements
        OnItemClickListener, IDeviceChangeListener, BrowseRequestCallback {

    protected static final String TAG = "ContentActivity";
    private IController mController;
    // private TextView mTVSelDeV;
    private ListView mContentListView;
    // private Button mBtnBack;

    private ContentAdapter mContentAdapter;
    private DlnaMediaProxy mDlnaMediaProxy;
    private ContentManager mContentManager;

    private List<MediaItem> mCurItems;
    private DMSDeviceBrocastFactory mBrocastFactory;

    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_layout);

        initView();
        initData();
    }

    @Override
    protected void onDestroy() {
        mContentManager.clear();
        mBrocastFactory.unRegisterListener();
        super.onDestroy();
    }

    private void initView() {
        mContentListView = (ListView) findViewById(R.id.content_list);
        mContentListView.setOnItemClickListener(this);
        mController = new RemoteDlnaController();
    }

    private void initData() {
        mDlnaMediaProxy = DlnaMediaProxy.getInstance(this);
        mContentManager = ContentManager.getInstance();

        mCurItems = new ArrayList<MediaItem>();
        mContentAdapter = new ContentAdapter(this, mCurItems);
        mContentListView.setAdapter(mContentAdapter);

        mBrocastFactory = new DMSDeviceBrocastFactory(this);

        updateSelDev();

        mHandler = new Handler();
        mHandler.postDelayed(new RequestDirectoryRunnable(), 100);

        mBrocastFactory.registerListener(this);
    }

    private void requestDirectory() {
        Device selDevice = mDlnaMediaProxy.getDMSSelectedDevice();
        if (selDevice == null) {
            CommonUtil.showToask(this, "can't select any devices...");
            finish();
            return;
        }
        BrowseDMSManager.syncGetDirectory(this, this);
        showLoadingDialog();
    }

    class RequestDirectoryRunnable implements Runnable {

        @Override
        public void run() {
            requestDirectory();
        }

    }

    private void setContentlist(List<MediaItem> list, boolean setToTop) {
        mCurItems = list;
        if (list == null) {
            mContentAdapter.clear();
        } else {
            mContentAdapter.refreshData(list);
            if (setToTop)
                mContentListView.setSelection(0);
        }
    }

    private int mselectedPosition;


    private void goMusicPlayerActivity(int index, MediaItem item) {

        MediaManager.getInstance().setMusicList(
                filterCurrentListItems(CommonUtil.ITEM_TYPE_AUDIO, item));

        Intent intent = new Intent();
        intent.setClass(this, PlayerControlActivity.class);
        intent.putExtra(PlayerControlActivity.PLAY_INDEX, mselectedPosition);
        intent.putExtra("isAudioPlayback", true);
//        MediaItemFactory.putItemToIntent(item, intent);
        ContentListingActivity.this.startActivity(intent);
    }

    private void goVideoPlayerActivity(MediaItem item) {

        MediaManager.getInstance().setVideoList(
                filterCurrentListItems(CommonUtil.ITEM_TYPE_VIDEO, item));

        Intent intent = new Intent();
        intent.setClass(this, PlayerControlActivity.class);
        intent.putExtra(PlayerControlActivity.PLAY_INDEX, mselectedPosition);
        // MediaItemFactory.putItemToIntent(item, intent);
        startActivity(intent);
    }

    private void goPicturePlayerActivity(MediaItem item) {

        MediaManager.getInstance().setPictureList(
                filterCurrentListItems(CommonUtil.ITEM_TYPE_IMAGE, item));

        Intent intent = new Intent();
        intent.setClass(this, ImageDisplayActivity.class);
        intent.putExtra("selectedPosition", mselectedPosition);
        // MediaItemFactory.putItemToIntent(item, intent);
        ContentListingActivity.this.startActivity(intent);
    }

    private List<MediaItem> filterCurrentListItems(int type,
                                                   MediaItem selectedItem) {
        List<MediaItem> tempList = new ArrayList<MediaItem>();
        for (MediaItem item : mCurItems) {
            switch (type) {
                case 0:
                    if (UpnpUtil.isPictureItem(item))
                        tempList.add(item);
                    break;
                case 1:
                    if (UpnpUtil.isAudioItem(item))
                        tempList.add(item);
                    break;
                case 2:
                    if (UpnpUtil.isVideoItem(item))
                        tempList.add(item);
                    break;
            }
            if (item.getRes().equals(selectedItem.getRes()))
                mselectedPosition = tempList.size() - 1;
        }
        return tempList;
    }

    private void back() {
        mContentManager.popListItem();
        List<MediaItem> list = mContentManager.peekListItem();
        if (list == null) {
            super.onBackPressed();
        } else {
            setContentlist(list, false);
        }

    }

    @Override
    public void onBackPressed() {
        back();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {

        final MediaItem item = (MediaItem) parent.getItemAtPosition(position);
        Log.e("CA",
                "item = \n" + item.getShowString() + item.resInfo != null ? ", url is: "
                        + item.resInfo.res
                        : "");
        Device renderer = mDlnaMediaProxy.getDMRSelectedDevice();

        if (UpnpUtil.isPictureItem(item))
            goPicturePlayerActivity(item);
        else if (UpnpUtil.isAudioItem(item)) {
            goMusicPlayerActivity(position, item);
        } else if (UpnpUtil.isVideoItem(item)) {
            if (null == renderer)
                firePlayVideoIntent(item.getRes(), false);
            else
                goVideoPlayerActivity(item);
        } else {
            BrowseDMSManager.syncGetItems(ContentListingActivity.this,
                    item.getStringid(), ContentListingActivity.this);
            showLoadingDialog();
        }

    }

    @Override
    public void onDeviceChange(boolean isSelDeviceChange) {
        // TODO Auto-generated method stub
        if (isSelDeviceChange) {
            CommonUtil.showToask(this, "current device has been dropped...");
            finish();
        }
    }

    @Override
    public void onGetItems(final List<MediaItem> list) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                dismissLoadingDialog();
                if (list == null) {
                    CommonUtil.showToask(ContentListingActivity.this,
                            "can't get folder...");
                    return;
                }
                mContentManager.pushListItem(list);
                setContentlist(list, true);

            }
        });
    }

    private void updateSelDev() {
        setSelDevUI(mDlnaMediaProxy.getDMSSelectedDevice());
    }

    private void setSelDevUI(Device device) {
        if (device == null) {
            // mTVSelDeV.setText("no select device");
        } else {
            getSupportActionBar().setTitle(device.getFriendlyName());
            // mTVSelDeV.setText(device.getFriendlyName());
        }

    }

}
