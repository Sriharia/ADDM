package com.awesome.dlnamanager.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;

import com.awesome.dlnamanager.R;

public class BaseActivity extends ActionBarActivity {

    protected ProgressDialog progressDialog;

    private static final String TAG = "BaseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (null != getSupportActionBar())
            getSupportActionBar().setDisplayHomeAsUpEnabled(
                    !(this instanceof DeviceListingActivity));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // AllShareApplication.onPause(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // AllShareApplication.onResume(this);
    }

    public void showLoadingDialog() {
        if (isFinishing())
            return;
        if (progressDialog == null
                || (progressDialog != null && !progressDialog.isShowing()))
            try {
                progressDialog = ProgressDialog.show(this, "",
                        getString(R.string.buffering), true);
            } catch (Exception e) {
                Log.e(TAG, "exception while showing window");
            }
    }

    public void dismissLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing())
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "exception while dismissing window");
            }
    }

    public void showMessageDialogWithListner(String message,
                                             DialogInterface.OnClickListener listner, boolean showNegavtive,
                                             final boolean finishCurrentActivity) {
        if (BaseActivity.this.isFinishing())
            return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.Dialog_Button_OK, listner);
        if (showNegavtive)
            builder.setNegativeButton(getString(R.string.Dialog_Button_Cancel),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (finishCurrentActivity)
                                finish();
                        }
                    });
        builder.show();
    }

    protected void firePlayVideoIntent(String uri, boolean isLocalFile) {
        try {
            if (isLocalFile)
                uri = "file://" + uri;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setDataAndType(Uri.parse(uri), "video/*");
            startActivity(intent);
        } catch (ActivityNotFoundException anfe) {
            Log.e("BA", "ANF exception");
        } catch (Exception e) {
            Log.e("BA", "unkown exception: " + e.getMessage());
        }
    }
}
