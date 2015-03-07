package com.awesome.dlnamanager.controller;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Service;

public class RemoteDlnaController implements IController {
    private static final String AVTransport1 = "urn:schemas-upnp-org:service:AVTransport:1";
    private static final String SetAVTransportURI = "SetAVTransportURI";
    private static final String RenderingControl = "urn:schemas-upnp-org:service:RenderingControl:1";
    private static final String Play = "Play";
    private static final String TAG = "RemoteDlnaController";

    public RemoteDlnaController() {

    }

    public static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";

    @Override
    public boolean play(Device device, String path) {

        Service service = device.getService(AVTransport1);
        if (service == null) {
            Log.e(TAG, "no AVTransport1 actions");
            return false;
        }
        // ActionList actionList = service.getActionList();
        // Log.e(TAG, "actions size: " + actionList.size());
        // int nActions = actionList.size();
        // for (int n = 0; n < nActions; n++) {
        // Action action = actionList.getAction(n);
        // Log.e(TAG, "action name: " + action.getName());
        // }
        final Action action = service.getAction(SetAVTransportURI);
        if (action == null) {
            Log.e(TAG, "no SetAVTransportURI service");
            return false;
        }

        final Action playAction = service.getAction(Play);
        if (playAction == null) {
            Log.e(TAG, "no Play service");
            return false;
        }

        if (TextUtils.isEmpty(path)) {
            Log.e(TAG, "path is empty");
            return false;
        }
        Log.e(TAG, "path is: " + Uri.encode(path, ALLOWED_URI_CHARS));
        action.setArgumentValue("InstanceID", 0);
        action.setArgumentValue("CurrentURI", Uri.encode(path, ALLOWED_URI_CHARS));
        action.setArgumentValue("CurrentURIMetaData", 0);
        if (!action.postControlAction()) {
            Log.e(TAG, "action isnt post control");
            return false;
        }

        playAction.setArgumentValue("InstanceID", 0);
        playAction.setArgumentValue("Speed", "1");
        return playAction.postControlAction();
    }

    @Override
    public boolean goon(Device device, String pausePosition) {

        Service localService = device.getService(AVTransport1);
        if (localService == null)
            return false;

        final Action localAction = localService.getAction("Seek");
        if (localAction == null)
            return false;
        localAction.setArgumentValue("InstanceID", "0");
        localAction.setArgumentValue("Unit", "ABS_TIME");
        localAction.setArgumentValue("Target", pausePosition);
        localAction.postControlAction();

        Action playAction = localService.getAction("Play");
        if (playAction == null) {
            return false;
        }

        playAction.setArgumentValue("InstanceID", 0);
        playAction.setArgumentValue("Speed", "1");
        return playAction.postControlAction();
    }

    @Override
    public String getTransportState(Device device) {
        Service localService = device.getService(AVTransport1);
        if (localService == null) {
            return null;
        }

        final Action localAction = localService.getAction("GetTransportInfo");
        if (localAction == null) {
            return null;
        }

        localAction.setArgumentValue("InstanceID", "0");

        if (localAction.postControlAction()) {
            return localAction.getArgumentValue("CurrentTransportState");
        } else {
            return null;
        }
    }

    public String getVolumeDbRange(Device device, String argument) {
        Service localService = device.getService(RenderingControl);
        if (localService == null) {
            return null;
        }
        Action localAction = localService.getAction("GetVolumeDBRange");
        if (localAction == null) {
            return null;
        }
        localAction.setArgumentValue("InstanceID", "0");
        localAction.setArgumentValue("Channel", "Master");
        if (!localAction.postControlAction()) {
            return null;
        } else {
            return localAction.getArgumentValue(argument);
        }
    }

    @Override
    public int getMinVolumeValue(Device device) {
        String minValue = getVolumeDbRange(device, "MinValue");
        if (TextUtils.isEmpty(minValue)) {
            return 0;
        }
        return Integer.parseInt(minValue);
    }

    @Override
    public int getMaxVolumeValue(Device device) {
        String maxValue = getVolumeDbRange(device, "MaxValue");
        if (TextUtils.isEmpty(maxValue)) {
            return 100;
        }
        return Integer.parseInt(maxValue);
    }

    @Override
    public boolean seek(Device device, String targetPosition) {
        Service localService = device.getService(AVTransport1);
        if (localService == null)
            return false;

        Action localAction = localService.getAction("Seek");
        if (localAction == null) {
            return false;
        }
        localAction.setArgumentValue("InstanceID", "0");
        // if (mUseRelTime) {
        // localAction.setArgumentValue("Unit", "REL_TIME");
        // } else {
        localAction.setArgumentValue("Unit", "ABS_TIME");
        // }
        localAction.setArgumentValue("Target", targetPosition);
        boolean postControlAction = localAction.postControlAction();
        if (!postControlAction) {
            localAction.setArgumentValue("Unit", "REL_TIME");
            localAction.setArgumentValue("Target", targetPosition);
            return localAction.postControlAction();
        } else {
            return postControlAction;
        }

    }

    @Override
    public String getPositionInfo(Device device) {
        Service localService = device.getService(AVTransport1);

        if (localService == null)
            return null;

        final Action localAction = localService.getAction("GetPositionInfo");
        if (localAction == null) {
            return null;
        }

        localAction.setArgumentValue("InstanceID", "0");
        boolean isSuccess = localAction.postControlAction();
        if (isSuccess) {
            return localAction.getArgumentValue("AbsTime");
        } else {
            return null;
        }
    }

    @Override
    public String getMediaDuration(Device device) {
        Service localService = device.getService(AVTransport1);
        if (localService == null) {
            return null;
        }

        final Action localAction = localService.getAction("GetMediaInfo");
        if (localAction == null) {
            return null;
        }

        localAction.setArgumentValue("InstanceID", "0");
        if (localAction.postControlAction()) {
            return localAction.getArgumentValue("MediaDuration");
        } else {
            return null;
        }

    }

    @Override
    public boolean setMute(Device mediaRenderDevice, String targetValue) {
        Service service = mediaRenderDevice.getService(RenderingControl);
        if (service == null) {
            return false;
        }
        final Action action = service.getAction("SetMute");
        if (action == null) {
            return false;
        }

        action.setArgumentValue("InstanceID", "0");
        action.setArgumentValue("Channel", "Master");
        action.setArgumentValue("DesiredMute", targetValue);
        return action.postControlAction();
    }

    @Override
    public String getMute(Device device) {
        Service service = device.getService(RenderingControl);
        if (service == null) {
            return null;
        }

        final Action getAction = service.getAction("GetMute");
        if (getAction == null) {
            return null;
        }
        getAction.setArgumentValue("InstanceID", "0");
        getAction.setArgumentValue("Channel", "Master");
        getAction.postControlAction();
        return getAction.getArgumentValue("CurrentMute");
    }

    @Override
    public boolean setVoice(Device device, int value) {
        Service service = device.getService(RenderingControl);
        if (service == null) {
            return false;
        }

        final Action action = service.getAction("SetVolume");
        if (action == null) {
            return false;
        }

        action.setArgumentValue("InstanceID", "0");
        action.setArgumentValue("Channel", "Master");
        action.setArgumentValue("DesiredVolume", value);
        return action.postControlAction();

    }

    @Override
    public int getVoice(Device device) {
        Service service = device.getService(RenderingControl);
        if (service == null) {
            return -1;
        }

        final Action getAction = service.getAction("GetVolume");
        if (getAction == null) {
            return -1;
        }
        getAction.setArgumentValue("InstanceID", "0");
        getAction.setArgumentValue("Channel", "Master");
        if (getAction.postControlAction()) {
            return getAction.getArgumentIntegerValue("CurrentVolume");
        } else {
            return -1;
        }

    }

    @Override
    public boolean stop(Device device) {
        Service service = device.getService(AVTransport1);

        if (service == null) {
            return false;
        }
        final Action stopAction = service.getAction("Stop");
        if (stopAction == null) {
            return false;
        }

        stopAction.setArgumentValue("InstanceID", 0);
        return stopAction.postControlAction();

    }

    @Override
    public boolean pause(Device mediaRenderDevice) {

        Service service = mediaRenderDevice.getService(AVTransport1);
        if (service == null) {
            return false;
        }
        final Action pauseAction = service.getAction("Pause");
        if (pauseAction == null) {
            return false;
        }
        pauseAction.setArgumentValue("InstanceID", 0);
        return pauseAction.postControlAction();
    }

}
