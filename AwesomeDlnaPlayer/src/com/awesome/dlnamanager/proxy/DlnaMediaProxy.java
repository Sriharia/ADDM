package com.awesome.dlnamanager.proxy;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Formatter;
import android.util.Log;

import com.awesome.dlnamanager.controller.RemoteDlnaController;
import com.awesome.dlnamanager.workers.DlnaService;
import com.awesome.dlnamanager.controller.IController;
import com.awesome.dlnamanager.server.SimpleWebServer;
import com.awesome.dlnamanager.upnp.AbstractMediaMng;
import com.awesome.dlnamanager.upnp.MediaServerMng;
import com.awesome.dlnamanager.upnp.UpnpUtil;

import org.cybergarage.upnp.Device;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class DlnaMediaProxy implements IDeviceOperator,
        IDeviceOperator.IDMSDeviceOperator {

    // private static final TAG

    private static DlnaMediaProxy instance;
    private Context mContext;

    private AbstractMediaMng dmsMediaMng;
    private IController mController;
    private SimpleWebServer m_server;

    private DlnaMediaProxy(Context context) {
        mContext = context;
        dmsMediaMng = new MediaServerMng(context);
        mController = new RemoteDlnaController();
    }

    public static synchronized DlnaMediaProxy getInstance(Context context) {
        if (instance == null) {
            instance = new DlnaMediaProxy(context);
        }
        return instance;
    }

    public void startSearch() {
        mContext.startService(new Intent(DlnaService.SEARCH_DEVICES));
    }

    public void resetSearch() {
        mContext.startService(new Intent(DlnaService.RESET_SEARCH_DEVICES));
        clearDevice();
    }

    public void exitSearch() {
        mContext.stopService(new Intent(mContext, DlnaService.class));
        clearDevice();
    }

    public void startLocalServer() {
        if (null == m_server) {
            Log.e("ASP", "server is null, create one and start..");
            m_server = new SimpleWebServer(getDeviceIp(), 4231, new File("/"),
                    false);
            try {
                m_server.start();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("ASP", "exception in sarting server**");
            }
        } else if (!m_server.isAlive()) {
            Log.e("ASP", "server uisnt alove, starting again..");
            try {
                m_server.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else
            Log.e("ASP", "server is alive already, skipping start");
    }

    public void serveLocalFile(final File f) {
        startLocalServer();
        new Thread() {
            @Override
            public void run() {
                String url = String.format("http://" + getDeviceIp() + ":%d%s", 4231,
                        f.getAbsolutePath());
                mController.play(getDMRSelectedDevice(), url);
            }
        }.start();
    }

    public void stopLocalFileServer() {
        if (null != m_server)
            m_server.stop();
        m_server = null;
    }

    private String getDeviceIp() {
        WifiManager wm = (WifiManager) mContext
                .getSystemService(ActionBarActivity.WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    @Override
    public void addDevice(Device d) {
        if (UpnpUtil.isMediaServerDevice(d)) {
            dmsMediaMng.addServerDevice(d);
        } else if (UpnpUtil.isMediaRenderDevice(d)) {
            dmsMediaMng.addRendererDevice(d);
        }
    }

    @Override
    public void removeDevice(Device d) {
        if (UpnpUtil.isMediaServerDevice(d)) {
            dmsMediaMng.removeServerDevice(d);
        } else if (UpnpUtil.isMediaRenderDevice(d))
            dmsMediaMng.removeRendererDevice(d);
    }

    @Override
    public void clearDevice() {
        dmsMediaMng.clear();
    }

    @Override
    public List<Device> getDMSDeviceList() {
        return dmsMediaMng.getServerDeviceList();
    }

    @Override
    public void setDMSSelectedDevice(Device selectedDevice) {
        dmsMediaMng.setSelectedServerDevice(selectedDevice);
    }

    @Override
    public Device getDMSSelectedDevice() {
        return dmsMediaMng.getSelectedServerDevice();
    }

    @Override
    public List<Device> getDMRDeviceList() {
        return dmsMediaMng.getRendererDeviceList();
    }

    @Override
    public Device getDMRSelectedDevice() {
        return dmsMediaMng.getSelectedRenderingDevice();
    }

    @Override
    public void setDMRSelectedDevice(Device selectedDevice) {
        dmsMediaMng.setSelectedRenderingDevice(selectedDevice);
    }

}
