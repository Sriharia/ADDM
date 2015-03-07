/*
 * Copyright 2012 Aeesh Joseph
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.awesome.dlnamanager.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.awesome.dlnamanager.R;
import com.awesome.dlnamanager.proxy.DlnaMediaProxy;
import com.awesome.dlnamanager.proxy.MediaManager;
import com.awesome.dlnamanager.upnp.MediaItem;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FilePickerActivity extends BaseActivity implements
        OnItemClickListener {

    /**
     * The file path
     */
    public final static String EXTRA_FILE_PATH = "file_path";

    /**
     * Sets whether hidden files should be visible in the list or not
     */
    public final static String EXTRA_SHOW_HIDDEN_FILES = "show_hidden_files";
    /**
     * Sets whether only files are to be picked.
     */
    public final static String EXTRA_SELECT_FILES_ONLY = "only_files";
    /**
     * Sets whether multiple items can be selected
     */
    public final static String EXTRA_SELECT_MULTIPLE = "select_multiple";
    /**
     * Sets whether only directories are to be picked
     */
    public final static String EXTRA_SELECT_DIRECTORIES_ONLY = "only_directories";

    /**
     * The allowed file extensions in an ArrayList of Strings
     */
    public final static String EXTRA_ACCEPTED_FILE_EXTENSIONS = "accepted_file_extensions";

    /**
     * The initial directory which will be used if no directory has been sent
     * with the intent
     */
    private final static String DEFAULT_INITIAL_DIRECTORY = "/";

    protected File mDirectory;
    protected ArrayList<File> mFiles;
    protected boolean singleMode = true;
    /**
     * pickType=0 - Picks files and directories =1 - Picks only files =2 - Pick
     * only directories
     */
    protected int pickType = 0;
    protected FilePickerListAdapter mAdapter;
    protected boolean mShowHiddenFiles = false;
    private ListView m_content_list;

    // protected String[] acceptedFileExtensions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_holder);
        m_content_list = (ListView) findViewById(R.id.content_list);
        // Set the view to be shown if the list is empty
        m_content_list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View emptyView = inflator
                .inflate(R.layout.file_picker_empty_view, null);
        ((ViewGroup) m_content_list.getParent()).addView(emptyView);
        m_content_list.setEmptyView(emptyView);
        try {
            getSupportActionBar().setTitle(
                    Build.MANUFACTURER + " " + Build.MODEL);
        } catch (Exception e) {

        }
        // Set initial directory
        mDirectory = new File(DEFAULT_INITIAL_DIRECTORY);

        // Initialize the ArrayList
        mFiles = new ArrayList<File>();

        // Set the ListAdapter
        mAdapter = new FilePickerListAdapter(this, mFiles);
        m_content_list.setAdapter(mAdapter);

        m_content_list.setOnItemClickListener(this);

        // Initialize the extensions array to allow any file extensions
        // acceptedFileExtensions = new String[] {};

        // Get intent extras
        if (getIntent().hasExtra(EXTRA_FILE_PATH)) {
            mDirectory = new File(getIntent().getStringExtra(EXTRA_FILE_PATH));
        }
        mShowHiddenFiles = getIntent().getBooleanExtra(EXTRA_SHOW_HIDDEN_FILES,
                false);

        // if (getIntent().hasExtra(EXTRA_ACCEPTED_FILE_EXTENSIONS)) {
        // ArrayList<String> collection = getIntent().getStringArrayListExtra(
        // EXTRA_ACCEPTED_FILE_EXTENSIONS);
        // acceptedFileExtensions = (String[]) collection
        // .toArray(new String[collection.size()]);
        // }

        singleMode = !getIntent().getBooleanExtra(EXTRA_SELECT_MULTIPLE, false);

        if (getIntent().getBooleanExtra(EXTRA_SELECT_FILES_ONLY, false))
            pickType = 1;

        if (getIntent().getBooleanExtra(EXTRA_SELECT_DIRECTORIES_ONLY, false))
            pickType = 2;

        Button ok = (Button) findViewById(R.id.ok);
        ok.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // returnResults();
            }
        });
        if (singleMode)
            ok.setVisibility(View.GONE);

        m_content_list.setLongClickable(true);
    }

    private void returnResults(File f) {
        Intent extra = null;
        String typeRegex = null;
        DlnaMediaProxy.getInstance(getApplicationContext()).startLocalServer();
        if (f.getName().matches(MimeTypes._RegexFileTypeImages)) {
            typeRegex = MimeTypes._RegexFileTypeImages;
            extra = new Intent(getApplicationContext(),
                    ImageDisplayActivity.class);
        } else if (f.getName().matches(MimeTypes._RegexFileTypeVideos)) {
            typeRegex = MimeTypes._RegexFileTypeVideos;
            extra = new Intent(getApplicationContext(), PlayerControlActivity.class);
        } else if (f.getName().matches(MimeTypes._RegexFileTypeAudios)) {
            typeRegex = MimeTypes._RegexFileTypeAudios;
            extra = new Intent(getApplicationContext(), PlayerControlActivity.class);
            extra.putExtra("isAudioPlayback", true);
        }

        if ((null == DlnaMediaProxy.getInstance(getApplicationContext()).getDMRSelectedDevice() && (typeRegex == MimeTypes._RegexFileTypeAudios || typeRegex == MimeTypes._RegexFileTypeVideos))) {
            firePlayVideoIntent(f.getAbsolutePath(), false);
            return;
        }
        List<MediaItem> mCurItems = new ArrayList<MediaItem>();
        for (File p : mFiles) {
            MediaItem item = new MediaItem();
            if (p.getName().matches(typeRegex)) {
                item.title = p.getName();
                item.resInfo = new MediaItem.ResInfo();
                item.resInfo.res = p.getAbsolutePath();
                mCurItems.add(item);
            }
            if (null != item.title && item.title.equals(f.getName())) {
                Log.e("FPA", "selected item pos is: " + mCurItems.indexOf(item));
                extra.putExtra(PlayerControlActivity.PLAY_INDEX, mCurItems.indexOf(item));
            }
        }
        if (mCurItems.size() > 0) {
            if (typeRegex.equals(MimeTypes._RegexFileTypeImages))
                MediaManager.getInstance().setPictureList(mCurItems);
            else if (typeRegex.equals(MimeTypes._RegexFileTypeVideos))
                MediaManager.getInstance().setVideoList(mCurItems);
            else if (typeRegex.equals(MimeTypes._RegexFileTypeAudios))
                MediaManager.getInstance().setMusicList(mCurItems);
            extra.putExtra("isLocal", true);
            startActivity(extra);
        }

        // } else if (null != f
        // && f.getName().matches(MimeTypes._RegexFileTypeVideos))
        // firePlayVideoIntent(f.getAbsolutePath(), true);
        // if (null != extra)
        // startActivity(extra);
        // setResult(RESULT_OK, extra);
        // finish();
    }

    @Override
    protected void onResume() {
        refreshFilesList(true);
        super.onResume();
    }

    /**
     * Updates the list view to the current directory
     */
    protected void refreshFilesList(boolean setToTop) {
        // Clear the files ArrayList
        mFiles.clear();
        // clear the checked item list
        mAdapter.clearBoxes();
        // Set the extension file filter
        File[] files;
        // if (acceptedFileExtensions != null && acceptedFileExtensions.length >
        // 0) {
        ExtensionFilenameFilter filter = new ExtensionFilenameFilter();
        files = mDirectory.listFiles(filter);
        // } else {
        // files = mDirectory.listFiles();
        // }
        // Get the files in the directory

        if (files != null && files.length > 0) {
            for (File f : files) {
                if ((f.isHidden() && !mShowHiddenFiles)
                        || (f.isFile() && pickType == 2)) {
                    // Don't add the file
                    continue;
                }
                // Add the file the ArrayAdapter
                mFiles.add(f);
            }

            Collections.sort(mFiles, new FileComparator());
        }
        mAdapter.notifyDataSetChanged();
        if (setToTop)
            m_content_list.setSelection(0);
    }

    @Override
    public void onBackPressed() {
        if (mDirectory.getParentFile() != null) {
            // Go to parent directory
            mDirectory = mDirectory.getParentFile();
            refreshFilesList(false);
            return;
        }

        finish();
    }

    private class FilePickerListAdapter extends ArrayAdapter<File> {

        private List<File> mObjects;
        private ArrayList<File> checkedObjects = new ArrayList<File>();

        public FilePickerListAdapter(Context context, List<File> objects) {
            super(context, R.layout.file_picker_list_item, android.R.id.text1,
                    objects);
            mObjects = objects;
        }

        public void clearBoxes() {
            checkedObjects = new ArrayList<File>();
        }

        public ArrayList<File> getFiles() {
            return checkedObjects;
        }

        public void toggleCheckBox(File file) {
            if (checkedObjects.contains(file))
                checkedObjects.remove(file);
            else
                checkedObjects.add(file);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View row = null;
            CheckBox checkBox;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(singleMode ? R.layout.content_list_item
                        : R.layout.file_picker_list_item, parent, false);
            } else {
                row = convertView;
            }

            final File object = mObjects.get(position);
            TextView textView;
            if (singleMode) {
                textView = (TextView) row.findViewById(R.id.tv_content);
                Drawable d = getResources().getDrawable(
                        object.isFile() ? getFileIcon(object.getName())
                                : R.drawable.ic_folder_black_36dp);
                textView.setCompoundDrawablesWithIntrinsicBounds(d, null, null,
                        null);
            } else {
                ImageView imageView = (ImageView) row
                        .findViewById(R.id.file_picker_image);
                textView = (TextView) row.findViewById(R.id.file_picker_text);
                checkBox = (CheckBox) row
                        .findViewById(R.id.file_picker_checkbox);
                checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        if (isChecked) {
                            if (!checkedObjects.contains(object))
                                checkedObjects.add(object);
                        } else {
                            checkedObjects.remove(object);
                        }

                    }
                });
                if ((object.isFile() && pickType == 2)
                        || (object.isDirectory() && pickType == 1))
                    checkBox.setVisibility(View.GONE);
                else
                    checkBox.setVisibility(View.VISIBLE);
                // Set single line
                // textView.setSingleLine(true);
                if (checkedObjects.contains(object))
                    checkBox.setChecked(true);
                else
                    checkBox.setChecked(false);
                if (object.isFile())
                    imageView.setImageResource(getFileIcon(object.getName()));
                else
                    imageView.setImageResource(R.drawable.ic_folder_black_36dp);
            }

            textView.setText(object.getName());

            return row;
        }

        private int getFileIcon(String filename) {
            if (filename.matches(MimeTypes._RegexFileTypeAudios))
                return R.drawable.ic_audiotrack_black_36dp;
            if (filename.matches(MimeTypes._RegexFileTypeVideos))
                return R.drawable.ic_movie_black_36dp;
            if (filename.matches(MimeTypes._RegexFileTypeImages))
                return R.drawable.ic_photo_black_36dp;
            if (filename.matches(MimeTypes._RegexFileTypeCompressed))
                return R.drawable.file_compressed;
            if (filename.matches(MimeTypes._RegexFileTypePlainTexts))
                return R.drawable.file_plain_text;
            return R.drawable.file;
        }

    }

    private class FileComparator implements Comparator<File> {
        public int compare(File f1, File f2) {
            if (f1 == f2) {
                return 0;
            }
            if (f1.isDirectory() && f2.isFile()) {
                // Show directories above files
                return -1;
            }
            if (f1.isFile() && f2.isDirectory()) {
                // Show files below directories
                return 1;
            }
            // Sort the directories alphabetically
            return f1.getName().compareToIgnoreCase(f2.getName());
        }
    }

    private class ExtensionFilenameFilter implements FilenameFilter {
        // private String[] mExtensions;

        public ExtensionFilenameFilter() {
            super();
            // mExtensions = extensions;
        }

        public boolean accept(File dir, String filename) {
            if (new File(dir, filename).isDirectory()) {
                // Accept all directory names
                return true;
            }

            return (filename.matches(MimeTypes._RegexFileTypeImages)
                    || filename.matches(MimeTypes._RegexFileTypeAudios) || filename
                    .matches(MimeTypes._RegexFileTypeVideos));

            // if(mExtensions != null && mExtensions.length > 0) {
            // for(int i = 0; i < mExtensions.length; i++) {
            // if(filename.toLowerCase().endsWith(mExtensions[i].toLowerCase()))
            // {
            // // The filename ends with the extension
            // return true;
            // }
            // }
            // The filename did not match any of the extensions
            // return false;
            // }
            // No extensions has been set. Accept all file extensions.
            // return true;
        }
    }

    class MimeTypes {

        public static final String _RegexFileTypePlainTexts = "(?si).+\\.(txt|html?|json|csv|java|pas|php.+|c|cpp|"
                + "bas|python|js|javascript|scala|xml|kml|css|ps|xslt?|tpl|tsv|bash|cmd|pl|pm|ps1|ps1xml|psc1|psd1|psm1|"
                + "py|pyc|pyo|r|rb|sdl|sh|tcl|vbs|xpl|ada|adb|ads|clj|cls|cob|cbl|cxx|cs|csproj|d|e|el|go|h|hpp|hxx|l|"
                + "m)";

        /**
         * @see http://en.wikipedia.org/wiki/Image_file_formats
         */
        public static final String _RegexFileTypeImages = "(?si).+\\.(gif|jpe?g|png|tiff?|wmf|emf|jfif|exif|"
                + "raw|bmp|ppm|pgm|pbm|pnm|webp|riff|tga|ilbm|img|pcx|ecw|sid|cd5|fits|pgf|xcf|svg|pns|jps|icon?|"
                + "jp2|mng|xpm|djvu)";

        /**
         * @see http://en.wikipedia.org/wiki/Audio_file_format
         * @see http://en.wikipedia.org/wiki/List_of_file_formats
         */
        public static final String _RegexFileTypeAudios = "(?si).+\\.(mp[2-3]+|wav|aiff|au|m4a|ogg|raw|flac|"
                + "mid|amr|aac|alac|atrac|awb|m4p|mmf|mpc|ra|rm|tta|vox|wma)";

        /**
         * @see http://en.wikipedia.org/wiki/Video_file_formats
         */
        public static final String _RegexFileTypeVideos = "(?si).+\\.(mp[4]+|flv|wmv|webm|m4v|3gp|mkv|mov|mpe?g|rmv?|ogv)";

        /**
         * @see http://en.wikipedia.org/wiki/List_of_file_formats
         */
        public static final String _RegexFileTypeCompressed = "(?si).+\\.(zip|7z|lz?|[jrt]ar|gz|gzip|bzip|xz|cab|sfx|"
                + "z|iso|bz?|rz|s7z|apk|dmg)";
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        File newFile = (File) parent.getItemAtPosition(position);

        if (newFile.isFile()) {

            if ((pickType == 0 || pickType == 1)) {
                mAdapter.toggleCheckBox(newFile);
                if (singleMode)
                    returnResults(newFile);
            }
        } else {
            mDirectory = newFile;
            // Update the files list
            refreshFilesList(true);
        }
    }

}
