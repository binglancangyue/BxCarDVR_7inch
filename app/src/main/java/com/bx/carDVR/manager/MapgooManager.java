package com.bx.carDVR.manager;

import com.bx.carDVR.DvrService;
import com.bx.carDVR.bean.Configuration;
import com.bx.carDVR.util.GpioManager;
import com.bx.carDVR.util.LogUtils;
import com.bx.carDVR.util.ThreadPoolUtil;
import com.mapgoo.mapgooipc.MapgooIPC;


public class MapgooManager implements ShareBufferManager.YUVDataCallback, MapgooIPC.ConnStatusNotifCallBack, MapgooIPC.CmdDealCallBack {

    private static final String TAG = "BxMapgooManager";

    private static int frontDataIndex = -1;
    private static int backDataIndex = -1;
    private static final String uniqueFrontID = "bixin_0";
    private static final String uniqueBackID = "bixin_1";
    private boolean magooRequestFrontCamera = false;
    private boolean magooRequestBackCamera = false;
    private static final int FRONT_CAMERA_ID = 0;
    private static final int BACK_CAMERA_ID = 1;

    private int currentCmd = -1;

    public MapgooManager(){
        MapgooIPC.setConnStatusNotifCallBack(this);
        MapgooIPC.setCmdDealCallBack(this);
    }

    public void initCameraDataIndexThread(){
        ThreadPoolUtil.post(new Runnable() {
            public void run() {
                frontDataIndex = -1;
                backDataIndex = -1;
                while (frontDataIndex < 0) {
                    frontDataIndex = MapgooIPC.getDataIndexByUniqueID(uniqueFrontID);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                while (backDataIndex < 0) {
                    backDataIndex = MapgooIPC.getDataIndexByUniqueID(uniqueBackID);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                sendMapgooCameraPlugUnplugstatus();
                LogUtils.getInstance().d(TAG, "frontDataIndex = " + frontDataIndex+" ; backDataIndex = " + backDataIndex);
            }
        });
    }

    public void sendMapgooCameraPlugUnplugstatus(){
        LogUtils.getInstance().d(TAG, "sendMapgooCameraPlugUnplugstatus() result = " + GpioManager.getInstance().isAhdPluginIn());
        if(!DvrService.SUPPORT_MAILIANBAO){
            return;
        }
        if(GpioManager.getInstance().isAhdPluginIn()){
            boolean result = MapgooIPC.sendMsg(MapgooIPC.MAPGOO_IPC_MSG_CAMERA_STATUS,
                    MapgooIPC.MAPGOO_IPC_SUBMSG_CAMERA_IN, backDataIndex, 50, "");
            LogUtils.getInstance().d(TAG, "getbackDataIndexThread() isPlugin = "+result);
        }else{
            boolean result = MapgooIPC.sendMsg(MapgooIPC.MAPGOO_IPC_MSG_CAMERA_STATUS,
                    MapgooIPC.MAPGOO_IPC_SUBMSG_CAMERA_OUT, backDataIndex, 50, "");
            LogUtils.getInstance().d(TAG, "getbackDataIndexThread() isPlugout = "+result);
        }
    }

    public void setMagooRequestCameraIndex(int cameraId, boolean value) {
        LogUtils.getInstance().d(TAG, "setMagooRequestCameraIndex cameraId = " + cameraId + "; value = " + value);
        LogUtils.getInstance().d(TAG, "setMagooRequestCameraIndex frontDataIndex = " + frontDataIndex + "; backDataIndex = " + backDataIndex);
        if (frontDataIndex == -1 || backDataIndex == -1) {
            initCameraDataIndexThread();
        }
        if (cameraId == FRONT_CAMERA_ID) {
            magooRequestFrontCamera = value;
        } else if (cameraId == BACK_CAMERA_ID) {
            magooRequestBackCamera = value;
        }
    }


    @Override
    public void processData(byte[] data, int cameraId) {
        if (cameraId == FRONT_CAMERA_ID ) {
            if (magooRequestFrontCamera) {
                LogUtils.getInstance().d(TAG,"PutYUVFrame start cameraId : "+cameraId);
                int result = MapgooIPC.PutYUVFrame(frontDataIndex, 1, Configuration.PREVIEW_WIDTH_720P, Configuration.PREVIEW_HEIGHT_720P, data,
                        data.length);
                LogUtils.getInstance().d(TAG,"PutYUVFrame end cameraId : "+cameraId+" , result : "+result);
            }
        } else if (cameraId == BACK_CAMERA_ID) {
            if (magooRequestBackCamera) {
                LogUtils.getInstance().d(TAG,"PutYUVFrame start cameraId : "+cameraId);
                int result = MapgooIPC.PutYUVFrame(backDataIndex, 1, Configuration.PREVIEW_WIDTH_720P, Configuration.PREVIEW_HEIGHT_720P, data,
                        data.length);
                LogUtils.getInstance().d(TAG,"PutYUVFrame end cameraId : "+cameraId+" , result : "+result);
            }
        }
//        if (currentCmd == MapgooIPC.MAPGOO_IPC_CMD_CAPTURE_PHOTO) {
//            setMagooRequestCameraIndex(cameraId, false);
//            ShareBufferManager.getInstance().removeShareBufferRequest(cameraId, this);
//        } else if (currentCmd == MapgooIPC.MAPGOO_IPC_CMD_CAPTURE_VIDEO){
//
//        }

    }

    @Override
    public void connStatusNotif(int status) {
        LogUtils.getInstance().d(TAG,"connStatusNotif status = "+status);
        if (status == MapgooIPC.MAPGOO_IPC_CONNECTED) {
            initCameraDataIndexThread();
        }
    }

    @Override
    public void getCmd(int cmd, int DataIndex, long time) {
        LogUtils.getInstance().d(TAG, "getCmd() cmd:" + cmd+";DataIndex = "+DataIndex+";time = "+time);
        if (cmd == MapgooIPC.MAPGOO_IPC_CMD_START_PUT_VIDEO_FRAME) {//推流打开请求
            //currentCmd = cmd;
            if (DataIndex == 1) {
                if (GpioManager.getInstance().isAhdPluginIn()) {
                    setMagooRequestCameraIndex(DataIndex, true);
                    ShareBufferManager.getInstance().addShareBufferRequest(DataIndex, this, "MAPGOO");
                }
            } else {
                setMagooRequestCameraIndex(DataIndex, true);
                ShareBufferManager.getInstance().addShareBufferRequest(DataIndex, this, "MAPGOO");
            }

        } else if (cmd == MapgooIPC.MAPGOO_IPC_CMD_STOP_PUT_VIDEO_FRAME) {//推流关闭请求
            //currentCmd = cmd;
            setMagooRequestCameraIndex(DataIndex, false);
            ShareBufferManager.getInstance().removeShareBufferRequest(DataIndex, this);
        }
    }
}
