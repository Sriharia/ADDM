package com.awesome.dlnamanager.activity;

import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.awesome.dlnamanager.R;
import com.awesome.dlnamanager.controller.IController;
import com.awesome.dlnamanager.controller.RemoteDlnaController;
import com.awesome.dlnamanager.proxy.DlnaMediaProxy;
import com.awesome.dlnamanager.proxy.MediaManager;
import com.awesome.dlnamanager.upnp.MediaItem;

import org.cybergarage.upnp.Device;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlayerControlActivity extends BaseActivity implements OnClickListener {
    private IController mController;
    private TextView tv_title;
    private SeekBar sb_progress;
    private TextView tv_current;
    private TextView tv_total;
    private ImageView iv_pre;
    private ImageView iv_next;
    private ImageView iv_play;
    private SeekBar sb_voice;
    private ImageView iv_volume;

    public static final String PLAY_INDEX = "play_index";

    private Device mDevice;

    private List<MediaItem> mMediaItems = new ArrayList<MediaItem>();
    private int mCurrentFileIndex;
    private static final String ZEROTIME = "00:00:00";
    private static final String MUTE = "1";
    private static final String UNMUTE = "0";
    private int mMediaDuration;
    private static final int RETRY_TIME = 1000;
    private static final String NOT_IMPLEMENTED = "NOT_IMPLEMENTED";
    private boolean mPaused;
    private boolean mPlaying;
    private static final int AUTO_INCREASING = 8001;
    private static final int AUTO_PLAYING = 8002;
    private static final int START_AUTOPLAY = 8003;
    private static final int PLAYBACK_FAILED = 8004;
    private static final int MUTE_PLAYER = 8005;
    private boolean mStartAutoPlayed;
    private DlnaMediaProxy mDlnaMediaProxy;

    private static final String TAG = "PlayerControlActivity";
    private boolean isAudioPlayback = false;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AUTO_INCREASING:
                    stopAutoIncreasing();
                    getPositionInfo();
                    startAutoIncreasing();
                    break;
                case AUTO_PLAYING:
                    playNext();
                    break;
                case MUTE_PLAYER:
                    initMuteImg(MUTE);
                    break;
                case START_AUTOPLAY:
                    Log.d(TAG,
                            "play success and start to get media duration");
                    mPlaying = true;
                    iv_play.setImageResource(R.drawable.ic_pause_circle_fill_black_36dp);
                    dismissLoadingDialog();
                    startAutoIncreasing();
                    showPlay(false);
                    // Get the media duration and set it to the total
                    // time.
                    getMediaDuration();
                    break;
                case PLAYBACK_FAILED:
                    showMessageDialogWithListner(getString(R.string.Unplayable_Message), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            play(getCurrentPlayPath());
                        }
                    }, true, true);
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        isAudioPlayback = getIntent().getBooleanExtra("isAudioPlayback", false);
        mDlnaMediaProxy = DlnaMediaProxy.getInstance(getApplicationContext());
        MediaManager mgr = MediaManager.getInstance();
        if (isAudioPlayback) {
            mMediaItems.addAll(mgr.getMusicList());
            mgr.clearMusicList();
        } else {
            mMediaItems.addAll(mgr.getVideoList());
            mgr.clearVideoList();
        }
        mCurrentFileIndex = getIntent().getIntExtra(PLAY_INDEX, 0);
        showLoadingDialog();
        findView();
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateVoice();
        getMute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        stop();
    }

    private void findView() {
        tv_title = (TextView) findViewById(R.id.tv_title);
        sb_progress = (SeekBar) findViewById(R.id.sb_progress);
        tv_current = (TextView) findViewById(R.id.tv_current);
        tv_total = (TextView) findViewById(R.id.tv_total);
        iv_pre = (ImageView) findViewById(R.id.iv_pre);
        iv_next = (ImageView) findViewById(R.id.iv_next);
        iv_play = (ImageView) findViewById(R.id.iv_play);
        sb_voice = (SeekBar) findViewById(R.id.sb_voice);
        iv_volume = (ImageView) findViewById(R.id.iv_volume);
        iv_volume.setTag("");
    }

    private void initView() {
        setController(new RemoteDlnaController());
        mDevice = DlnaMediaProxy.getInstance(getApplicationContext())
                .getDMRSelectedDevice();

        if (mController == null || mDevice == null) {
            // usually can't reach here.
            Toast.makeText(getApplicationContext(), "Invalidate operation",
                    Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Controller or Device is null, finish this activity");
            finish();
        }

        // init the state
        getMaxVolumn();

        sb_progress.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                startAutoIncreasing();
                int progress = seekBar.getProgress();
                seek(secToTime(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopAutoIncreasing();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                tv_current.setText(secToTime(progress));
                if (fromUser) {
                    stopAutoIncreasing();
                }
            }
        });

        sb_voice.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setVoice(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if (fromUser)
                    initMuteImg(progress == 0 ? MUTE : UNMUTE);
            }
        });

        iv_pre.setOnClickListener(this);
        iv_next.setOnClickListener(this);
//        iv_go_fast.setOnClickListener(this);
//        iv_back_fast.setOnClickListener(this);
        iv_play.setOnClickListener(this);
        iv_volume.setOnClickListener(this);

        play(getCurrentPlayPath());

    }

    private void setController(IController controller) {
        this.mController = controller;
    }

    /**
     * Get the max volume value and set it the sb_voice.
     */
    private synchronized void getMaxVolumn() {
        new Thread() {
            public void run() {
                final int maxVolumnValue = mController
                        .getMaxVolumeValue(mDevice);

                if (maxVolumnValue <= 0) {
                    Log.d(TAG, "get max volumn value failed..");
                    sb_voice.setMax(100);
                } else {
                    Log.d(TAG, "get max volumn value success, the value is "
                            + maxVolumnValue);
                    sb_voice.setMax(maxVolumnValue);
                }
            }

            ;
        }.start();
    }

    /**
     * Get current voice value and set it the sb_voice.
     */
    private void updateVoice() {
        new Thread() {
            @Override
            public void run() {
                int currentVoice = mController.getVoice(mDevice);
                if (currentVoice == -1) {
                    currentVoice = 0;
                    Log.d(TAG, "get current voice failed");
                } else {
                    Log.d(TAG, "get current voice success");
                    sb_voice.setProgress(currentVoice);
                }
            }
        }.start();
    }

    /**
     * Get if is muted and set the mute image.
     */
    private void getMute() {
        new Thread() {
            @Override
            public void run() {
                final String mute = mController.getMute(mDevice);
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (mute == null) {
                            Log.d(TAG, "get mute failed...");
                            if (sb_voice.getProgress() == 0) {
                                initMuteImg(MUTE);
                            }
                        } else {
                            Log.d(TAG, "get mute success");
                            initMuteImg(mute);
                        }

                    }
                });
            }
        }.start();
    }

    /**
     * Start to play the video.
     *
     * @param path The video path.
     */
    private synchronized void play(final String path) {
        // Initial the state.
        mPaused = false;
        showPlay(true);
        setCurrentTime(ZEROTIME);
        setTotalTime(ZEROTIME);
        setTitle(path);
        stopAutoIncreasing();
        stopAutoPlaying();
        if (getIntent().getBooleanExtra("isLocal", false))
            sendLocalFilePlayAction(getDeviceIp(), new File(path));
        else
            new Thread() {
                public void run() {
                    mHandler.sendEmptyMessage(mController.play(mDevice, path) ? START_AUTOPLAY : PLAYBACK_FAILED);
                }
            }.start();
    }

    private String getDeviceIp() {
        WifiManager wm = (WifiManager) getSystemService(ActionBarActivity.WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    protected void sendLocalFilePlayAction(final String ip, final File f) {
        mDlnaMediaProxy.startLocalServer();
        new Thread() {
            @Override
            public void run() {
                String url = String.format("http://" + ip + ":%d%s", 4231,
                        f.getAbsolutePath());
                mHandler.sendEmptyMessage(mController.play(mDlnaMediaProxy
                        .getDMRSelectedDevice(), url) ? START_AUTOPLAY : PLAYBACK_FAILED);
            }
        }.start();
    }

    private synchronized void pause() {
        stopAutoIncreasing();
        stopAutoPlaying();
        showPlay(true);

        new Thread() {
            public void run() {
                final boolean isSuccess = mController.pause(mDevice);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showPlay(isSuccess);
                        if (isSuccess) {
                            mPaused = true;
                            iv_play.setImageResource(R.drawable.ic_play_circle_fill_black_36dp);
                            mPlaying = false;
                            mHandler.removeMessages(AUTO_PLAYING);
                        } else {
                            startAutoIncreasing();
                        }
                    }
                });
            }
        }.start();
    }

    private synchronized void playNext() {
        mCurrentFileIndex++;
        if (mCurrentFileIndex > mMediaItems.size() - 1) {
            return;
        }
        play(getCurrentPlayPath());
    }

    private synchronized void playPre() {
        mCurrentFileIndex--;
        if (mCurrentFileIndex < 0) {
            return;
        }
        play(getCurrentPlayPath());
    }

    private synchronized void goon(final String pausePosition) {
        new Thread() {
            @Override
            public void run() {
                final boolean isSuccess = mController.goon(mDevice,
                        pausePosition);
                if (isSuccess) {
                    mPlaying = true;
                    Log.d(TAG, "Go on to play success");
                } else {
                    mPlaying = false;
                    Log.d(TAG, "Go on to play failed.");
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        showPlay(!isSuccess);
                        if (isSuccess) {
                            startAutoIncreasing();
                        }
                    }
                });
            }
        }.start();
    }

    /**
     * Seek playing position to the target position.
     *
     * @param targetPosition target position like "00:00:00"
     */
    private synchronized void seek(final String targetPosition) {
        new Thread() {
            @Override
            public void run() {
                boolean isSuccess = mController.seek(mDevice, targetPosition);
                if (isSuccess) {
                    Log.d(TAG, "seek success");
                    sb_progress.setProgress(getIntLength(targetPosition));
                } else {
                    Log.d(TAG, "seek failed..");
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        if (mPlaying) {
                            startAutoIncreasing();
                        } else {
                            stopAutoIncreasing();
                        }
                    }
                });
            }
        }.start();
    }

    /**
     * Get the current playing position.
     */
    private synchronized void getPositionInfo() {
        new Thread() {
            @Override
            public void run() {
                String position = mController.getPositionInfo(mDevice);
                Log.e(TAG, "Get position info and the value is " + position);
                if (TextUtils.isEmpty(position)
                        || NOT_IMPLEMENTED.equals(position)) {
                    return;
                }
                final int currentPosition = getIntLength(position);
                if (currentPosition <= 0 || currentPosition > mMediaDuration) {
                    return;
                }
                sb_progress.setProgress(getIntLength(position));

                runOnUiThread(new Runnable() {
                    public void run() {
                        if (currentPosition >= mMediaDuration - 3
                                && mMediaDuration > 0) {
                            if (mStartAutoPlayed) {
                                return;
                            } else {
                                mStartAutoPlayed = true;
                                Log.d(TAG, "start auto play next video");
                                stopAutoPlaying();
                                startAutoPlaying((mMediaDuration - currentPosition) * 1000);
                            }
                        }
                    }
                });
            }
        }.start();
    }

    private synchronized void getMediaDuration() {
        new Thread() {
            @Override
            public void run() {
                final String mediaDuration = mController
                        .getMediaDuration(mDevice);
                mMediaDuration = getIntLength(mediaDuration);

                Log.d(TAG, "Get media duration and the value is "
                        + mMediaDuration);
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (TextUtils.isEmpty(mediaDuration)
                                || NOT_IMPLEMENTED.equals(mediaDuration)
                                || mMediaDuration <= 0) {
                            mHandler.postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    Log.e(TAG,
                                            "Get media duration failed, retry later."
                                                    + "Duration:"
                                                    + mediaDuration
                                                    + "intLength:"
                                                    + mMediaDuration);
                                    getMediaDuration();
                                }
                            }, RETRY_TIME);
                            return;
                        }
                        tv_total.setText(mediaDuration);
                        sb_progress.setMax(mMediaDuration);
                    }
                });
            }
        }.start();
    }

    private synchronized void setMute(final String targetValue) {
        new Thread() {
            @Override
            public void run() {
                boolean isSuccess = mController.setMute(mDevice, targetValue);
                if (isSuccess) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Log.e(TAG, "set mute success, target value: " + targetValue);
                            initMuteImg(targetValue);
                            getVoice();
                        }
                    });
                }
            }
        }.start();
    }

    private synchronized void setVoice(final int voice) {
        new Thread() {
            @Override
            public void run() {
                boolean isSuccess = mController.setVoice(mDevice, voice);
                if (isSuccess) {
                    sb_voice.setProgress(voice);
                    if (voice == 0) {
                        mHandler.sendEmptyMessage(MUTE_PLAYER);
                    }
                }
            }
        }.start();
    }

    private synchronized void getVoice() {
        new Thread() {
            @Override
            public void run() {
                int voice = mController.getVoice(mDevice);
                sb_voice.setProgress(voice);
                if (voice == 0) {
                    mHandler.sendEmptyMessage(MUTE_PLAYER);
                }
            }
        }.start();
    }

    private synchronized void stop() {
        stopAutoPlaying();
        stopAutoIncreasing();
        new Thread() {
            @Override
            public void run() {
                final boolean isSuccess = mController.stop(mDevice);
                runOnUiThread(new Runnable() {
                    public void run() {
                        showPlay(isSuccess);
                    }
                });
            }
        }.start();
    }

    @SuppressWarnings("unused")
    private synchronized void getTransportState() {
        new Thread() {
            @Override
            public void run() {
                String transportState = mController.getTransportState(mDevice);
                // Do your things here;
                Log.d(TAG, "Get transportState :" + transportState);
            }
        }.start();
    }

    private synchronized void fastGoOrBack(boolean isGo) {
        stopAutoIncreasing();
        String position = tv_current.getText().toString();
        int targetLength;
        if (isGo) {
            targetLength = getIntLength(position) + 10;
            if (targetLength > mMediaDuration) {
                targetLength = mMediaDuration;
            }
        } else {
            targetLength = getIntLength(position) - 10;
            if (targetLength < 0) {
                targetLength = 0;
            }
        }
        sb_progress.setProgress(targetLength);
        seek(secToTime(targetLength));
    }

    private void setCurrentTime(String time) {
        tv_current.setText(time);
    }

    private void setTotalTime(String time) {
        tv_total.setText(time);
    }

    private void setTitle(String title) {
        tv_title.setText(title.substring(title.lastIndexOf('/') + 1));
    }

    private void startAutoIncreasing() {
        mHandler.sendEmptyMessageDelayed(AUTO_INCREASING, 1000);
    }

    private void stopAutoIncreasing() {
        mHandler.removeMessages(AUTO_INCREASING);
    }

    private void startAutoPlaying(long interTimes) {
        mHandler.sendEmptyMessageAtTime(AUTO_PLAYING, interTimes);
    }

    private void stopAutoPlaying() {
        mHandler.removeMessages(AUTO_PLAYING);
    }

    /**
     * Show the play or pause image.
     *
     * @param showPlay True to show the play image otherwise false.
     */
    private void showPlay(boolean showPlay) {
        iv_play.setImageResource(showPlay ? R.drawable.ic_play_circle_fill_black_36dp : R.drawable.ic_pause_circle_fill_black_36dp);
    }

    /**
     * Show the mute or volume image.
     *
     * @param mute 1 is mute, otherwise is 0.
     */
    private void initMuteImg(String mute) {
        iv_volume.setImageResource(MUTE.equals(mute) ? R.drawable.ic_volume_off_black_36dp : R.drawable.ic_volume_up_black_36dp);
        iv_volume.setTag(MUTE.equals(mute) ? "mute" : "unmute");
        if (MUTE.equals(mute))
            sb_voice.setProgress(0);
    }

    /**
     * Get the current play path of the video.
     *
     * @return The video path to play.
     */
    private String getCurrentPlayPath() {
        return mMediaItems.get(mCurrentFileIndex).getRes();
    }

    /**
     * Convert the time in seconds to "00:00:00" style.
     *
     * @param time The time in seconds.
     * @return The formated style like "00:00:00".
     */
    public static String secToTime(int time) {
        String timeStr = null;
        int hour = 0;
        int minute = 0;
        int second = 0;
        if (time <= 0)
            return "00:00:00";
        else {
            minute = time / 60;
            if (minute < 60) {
                second = time % 60;
                timeStr = "00:" + unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99)
                    return "99:59:59";
                minute = minute % 60;
                second = time - hour * 3600 - minute * 60;
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":"
                        + unitFormat(second);
            }
        }
        return timeStr;
    }

    /**
     * Make sure if the parameter is less than 10 to add "0" before it.
     *
     * @param i The number to be formatted.
     * @return The formatted number like "00" or "12";
     */
    public static String unitFormat(int i) {
        String retStr = null;
        if (i >= 0 && i < 10)
            retStr = "0" + Integer.toString(i);
        else if (i >= 10 && i <= 60) {
            retStr = "" + i;
        } else {
            retStr = "00";
        }
        return retStr;
    }

    /**
     * Convert time from "00:00:00" to seconds.
     *
     * @param length 00:00:00或者00:00
     * @return The length in seconds.
     */
    private int getIntLength(String length) {
        if (TextUtils.isEmpty(length)) {
            return 0;
        }
        String[] split = length.split(":");
        int count = 0;
        try {
            if (split.length == 3) {
                count += (Integer.parseInt(split[0])) * 60 * 60;
                count += Integer.parseInt(split[1]) * 60;
                count += Integer.parseInt(split[2]);
            } else if (split.length == 2) {
                count += Integer.parseInt(split[0]) * 60;
                count += Integer.parseInt(split[1]);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return count;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.iv_play:
                if (mPlaying) {
                    pause();
                    return;
                }

                if (mPaused) {
                    String pausePosition = tv_current.getText().toString().trim();
                    goon(pausePosition);
                } else {
                    play(getCurrentPlayPath());
                }
                break;
            case R.id.iv_volume:
                String targetValue = MUTE;
                if (iv_volume.getTag().equals("mute"))
                    targetValue = UNMUTE;
                setMute(targetValue);
                break;
            case R.id.iv_pre:
                playPre();
                break;
            case R.id.iv_next:
                playNext();
                break;
//            case R.id.iv_go_fast:
//                fastGoOrBack(true);
//                break;
//            case R.id.iv_back_fast:
//                fastGoOrBack(false);
//                break;

            default:
                break;
        }
    }
}
