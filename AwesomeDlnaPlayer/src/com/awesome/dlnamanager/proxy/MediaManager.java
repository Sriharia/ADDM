package com.awesome.dlnamanager.proxy;

import com.awesome.dlnamanager.upnp.MediaItem;

import java.util.ArrayList;
import java.util.List;

public class MediaManager {

    private static MediaManager mInstance;

    private List<MediaItem> mMusicList;
    private List<MediaItem> mVideoList;
    private List<MediaItem> mPictureist;

    public synchronized static MediaManager getInstance() {
        if (mInstance == null) {
            mInstance = new MediaManager();
        }
        return mInstance;
    }

    private MediaManager() {
        mMusicList = new ArrayList<MediaItem>();
        mVideoList = new ArrayList<MediaItem>();
        mPictureist = new ArrayList<MediaItem>();
    }

    public void setMusicList(List<MediaItem> list) {
        if (list != null) {
            mMusicList = list;
        }

    }

    public List<MediaItem> getMusicList() {
        return mMusicList;
    }

    public void clearMusicList() {
        mMusicList.clear();
    }

    public void setVideoList(List<MediaItem> list) {
        if (list != null) {
            mVideoList = list;
        }

    }

    public List<MediaItem> getVideoList() {
        return mVideoList;
    }

    public void clearVideoList() {
        mVideoList.clear();
    }

    public void setPictureList(List<MediaItem> list) {
        if (list != null) {
            mPictureist = list;
        } else {
            mPictureist.clear();
            mPictureist = null;
        }

    }

    public List<MediaItem> getPictureList() {
        return mPictureist;
    }

    public void clearPictureList() {
        mPictureist.clear();
    }

}
