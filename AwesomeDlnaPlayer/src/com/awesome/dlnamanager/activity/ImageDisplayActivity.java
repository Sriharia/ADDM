package com.awesome.dlnamanager.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.awesome.dlnamanager.R;
import com.awesome.dlnamanager.controller.IController;
import com.awesome.dlnamanager.controller.RemoteDlnaController;
import com.awesome.dlnamanager.customviews.JazzyViewPager;
import com.awesome.dlnamanager.customviews.JazzyViewPager.TransitionEffect;
import com.awesome.dlnamanager.customviews.TouchImageView;
import com.awesome.dlnamanager.proxy.DlnaMediaProxy;
import com.awesome.dlnamanager.proxy.MediaManager;
import com.awesome.dlnamanager.upnp.MediaItem;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class ImageDisplayActivity extends Activity {

    private JazzyViewPager mJazzy;
    private boolean m_isLocalFiles;
    int width, height;
    private DlnaMediaProxy mDlnaMediaProxy;
    private IController m_controller;
    private MediaManager mMediaMgr;
    private List<MediaItem> picList = new ArrayList<MediaItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_display_activity);
        m_isLocalFiles = getIntent().getBooleanExtra("isLocal", false);
        mDlnaMediaProxy = DlnaMediaProxy.getInstance(getApplicationContext());
        mMediaMgr = MediaManager.getInstance();
        if (mDlnaMediaProxy.getDMRSelectedDevice() != null)
            m_controller = new RemoteDlnaController();
        setupJazziness(TransitionEffect.Tablet);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Toggle Fade");
        String[] effects = this.getResources().getStringArray(
                R.array.jazzy_effects);
        for (String effect : effects)
            menu.add(effect);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // if (item.getTitle().toString().equals("Toggle Fade")) {
        // mJazzy.setFadeEnabled(!mJazzy.getFadeEnabled());
        // } else {
        TransitionEffect effect = TransitionEffect.valueOf(item.getTitle()
                .toString());
        setupJazziness(effect);
        // }
        return true;
    }

    private void setupJazziness(TransitionEffect effect) {
        mJazzy = (JazzyViewPager) findViewById(R.id.jazzy_pager);
        mJazzy.setTransitionEffect(effect);
        picList.clear();
        picList.addAll(mMediaMgr.getPictureList());
        mJazzy.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(final int arg0) {
                Log.e("IDA", "page selected: " + arg0 + ", is local file? "
                        + m_isLocalFiles);
                feedRemoteClients(picList.get(arg0).getRes());
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
                // TODO Auto-generated method stub

            }
        });
        mJazzy.setAdapter(new FullScreenImageAdapter());
        final int selectedPos = getIntent().getIntExtra(PlayerControlActivity.PLAY_INDEX, 0);
        feedRemoteClients(picList.get(selectedPos).getRes());
        if (selectedPos > 0) {
            Log.e("IDA", "selected pos: " + selectedPos);
            mJazzy.setCurrentItem(selectedPos);
        }
        mJazzy.setPageMargin(30);
    }

    protected void feedRemoteClients(final String uri) {
        if (null != mDlnaMediaProxy.getDMRSelectedDevice()) {
            if (m_isLocalFiles)
                mDlnaMediaProxy.serveLocalFile(new File(uri));
            else {
                new Thread() {
                    public void run() {
                        m_controller.play(
                                mDlnaMediaProxy.getDMRSelectedDevice(), uri);
                    }
                }.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (null != m_controller
                && mDlnaMediaProxy.getDMRSelectedDevice() != null)
            new Thread() {
                @Override
                public void run() {
                    Log.e("IDA", "stopping image play " + m_controller.stop(mDlnaMediaProxy.getDMRSelectedDevice()));
                }
            };
        else
            Log.e("IDA", "no controller and device");
        mMediaMgr.clearPictureList();
        super.onDestroy();
    }

    class FullScreenImageAdapter extends PagerAdapter {

        // constructor
        public FullScreenImageAdapter() {
            if (!m_isLocalFiles)
                mJazzy.setOffscreenPageLimit(Math.min(6, picList.size()));
        }

        @Override
        public int getCount() {
            return picList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((RelativeLayout) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final TouchImageView imgDisplay;
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View viewLayout = inflater.inflate(
                    R.layout.image_viewpager_item_layout, null);

            imgDisplay = (TouchImageView) viewLayout
                    .findViewById(R.id.touchImageView);
            if (m_isLocalFiles) {
                Log.e("IDA", "local file and url is: "
                        + picList.get(position).getRes());
                Bitmap bm = decodeFile(picList.get(position).getRes(), width,
                        height);
                if (null != bm)
                    imgDisplay.setImageBitmap(bm);
                else {
                    imgDisplay.setImageResource(R.drawable.ic_error_white_36dp);
                    imgDisplay.setScaleType(ScaleType.CENTER);
                }
            } else {
                Log.e("IDA", "remote file, url is: "
                        + picList.get(position).getRes());
                final ProgressBar pb = (ProgressBar) viewLayout
                        .findViewById(R.id.image_loading_status);
                pb.setVisibility(View.VISIBLE);
                ImageLoader.getInstance().displayImage(
                        picList.get(position).getRes(), imgDisplay,
                        new ImageLoadingListener() {

                            @Override
                            public void onLoadingStarted(String arg0, View arg1) {
                                // TODO Auto-generated method stub

                            }

                            @Override
                            public void onLoadingFailed(String arg0, View arg1,
                                                        FailReason arg2) {
                                pb.setVisibility(View.GONE);
                                imgDisplay
                                        .setImageResource(R.drawable.ic_error_white_36dp);
                                imgDisplay.setScaleType(ScaleType.CENTER);
                            }

                            @Override
                            public void onLoadingComplete(String arg0,
                                                          View arg1, Bitmap arg2) {
                                if (!isFinishing())
                                    pb.setVisibility(View.GONE);
                            }

                            @Override
                            public void onLoadingCancelled(String arg0,
                                                           View arg1) {
                                // TODO Auto-generated method stub

                            }
                        });
            }
            imgDisplay.setScaleType(ScaleType.FIT_CENTER);
            container.addView(viewLayout);
            mJazzy.setObjectForPosition(viewLayout, position);
            return viewLayout;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(mJazzy.findViewFromObject(position));

        }
    }

    /*
     * Resizing image size
     */
    public static Bitmap decodeFile(String filePath, int WIDTH, int HIGHT) {
        try {

            File f = new File(filePath);

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            // BitmapFactory.Options o2 = new BitmapFactory.Options();
            o.inSampleSize = calculateInSampleSize(o, WIDTH, HIGHT);
            o.inJustDecodeBounds = false;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e("IDA", "file not found***");
        }
        return null;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 4;
            }
        }
        Log.e("IDA", "sample size: " + inSampleSize);
        return Math.max(4, inSampleSize);
    }
}
