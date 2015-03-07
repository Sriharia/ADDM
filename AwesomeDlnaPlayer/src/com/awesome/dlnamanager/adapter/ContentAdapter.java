package com.awesome.dlnamanager.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.awesome.dlnamanager.R;
import com.awesome.dlnamanager.upnp.MediaItem;
import com.awesome.dlnamanager.upnp.UpnpUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import java.util.List;

public class ContentAdapter extends BaseAdapter {

    private List<MediaItem> contentItem;
    private LayoutInflater mInflater;
    private Context mContext;

    private Drawable foldIcon;
    private Drawable musicIcon;
    private Drawable picIcon;
    private Drawable videoIcon;

    private DisplayImageOptions options;

    public ContentAdapter(Context context, List<MediaItem> contentItem) {
        mInflater = LayoutInflater.from(context);
        this.contentItem = contentItem;
        mContext = context;

        Resources res = context.getResources();
        foldIcon = res.getDrawable(R.drawable.ic_folder_black_36dp);
        musicIcon = res.getDrawable(R.drawable.ic_audiotrack_black_36dp);
        picIcon = res.getDrawable(R.drawable.ic_photo_black_36dp);
        videoIcon = res.getDrawable(R.drawable.ic_movie_black_36dp);

        options = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.ic_launcher)
                .showImageOnFail(R.drawable.ic_launcher)
                .resetViewBeforeLoading(true).cacheOnDisk(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .bitmapConfig(Bitmap.Config.RGB_565).considerExifParams(true)
                .displayer(new FadeInBitmapDisplayer(300)).build();
    }

    public void refreshData(List<MediaItem> contentItem) {
        this.contentItem = contentItem;
        notifyDataSetChanged();
    }

    public void clear() {
        if (contentItem != null) {
            contentItem.clear();
            notifyDataSetChanged();
        }
    }

    /**
     * The number of items in the list is determined by the number of speeches
     * in our array.
     *
     * @see android.widget.ListAdapter#getCount()
     */
    public int getCount() {
        return contentItem.size();
    }

    /**
     * Since the data comes from an array, just returning the index is sufficent
     * to get at the data. If we were using a more complex data structure, we
     * would return whatever object represents one row in the list.
     *
     * @see android.widget.ListAdapter#getItem(int)
     */
    public Object getItem(int position) {
        return contentItem.get(position);
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
     * android.view.ViewGroup)
     */

    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.content_list_item, null);
        }

        MediaItem dataItem = (MediaItem) getItem(position);
        // Log.e("ADAPTER", "item detals: " + dataItem.getShowString());
        TextView tvContent = (TextView) convertView
                .findViewById(R.id.tv_content);
        tvContent.setText(dataItem.getTitle());
        if (UpnpUtil.isAudioItem(dataItem)) {
            tvContent.setCompoundDrawablesWithIntrinsicBounds(
                    musicIcon, null, null, null);
            // icon.setBackgroundDrawable(musicIcon);
        } else if (UpnpUtil.isVideoItem(dataItem)) {
            tvContent.setCompoundDrawablesWithIntrinsicBounds(
                    videoIcon, null, null, null);
            // icon.setBackgroundDrawable(videoIcon);
        } else if (UpnpUtil.isPictureItem(dataItem)) {
            tvContent.setCompoundDrawablesWithIntrinsicBounds(picIcon,
                    null, null, null);
            // icon.setBackgroundDrawable(picIcon);
        } else {
            tvContent.setCompoundDrawablesWithIntrinsicBounds(foldIcon,
                    null, null, null);
            // icon.setBackgroundDrawable(foldIcon);
        }

        return convertView;
    }
}
