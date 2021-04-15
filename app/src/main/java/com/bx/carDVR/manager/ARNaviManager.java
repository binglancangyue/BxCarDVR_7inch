package com.bx.carDVR.manager;

import com.autonavi.amapauto.gdarcameraservice.model.ArCameraOpenResultParam;
import com.bx.carDVR.bean.Configuration;

public class ARNaviManager implements ShareBufferManager.YUVDataCallback {

    private boolean isStartARNavi = false;
    private ArCameraOpenResultParam arCameraOpenResultParam;

    private static ARNaviManager sARNaviManager;

    private ARNaviManager() {
        initArCameraOpenResultParam();
    }

    public static ARNaviManager getInstance() {
        if (sARNaviManager == null) {
            synchronized (ARNaviManager.class) {
                if (sARNaviManager == null) {
                    sARNaviManager = new ARNaviManager();
                }
            }
        }
        return sARNaviManager;
    }

    public void startARNavigation() {
        isStartARNavi = true;
        ShareBufferManager.getInstance().addShareBufferRequest(0,this,"ar");
    }

    public void stopARNavigation() {
        isStartARNavi = false;
        ShareBufferManager.getInstance().removeShareBufferRequest(0,this);
    }

    private void  initArCameraOpenResultParam(){
        arCameraOpenResultParam = new ArCameraOpenResultParam();
        arCameraOpenResultParam.cameraId = String.valueOf(Configuration.CAMERA_IDS[0]);
        arCameraOpenResultParam.imageWidth = Configuration.VIDEO_WIDTH_720P;
        arCameraOpenResultParam.imageHeight = Configuration.VIDEO_HEIGHT_720P;
        arCameraOpenResultParam.imageFormat = 0;
        arCameraOpenResultParam.imageSize = 1280*720*3/2;
    }

    @Override
    public void processData(byte[] data, int cameraId) {
        if (isStartARNavi) {
            NotifyMessageManager.getInstance().sendToGaoDeOpened(data,arCameraOpenResultParam,"ARData");
        }
    }
}
