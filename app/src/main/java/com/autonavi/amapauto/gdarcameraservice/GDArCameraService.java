package com.autonavi.amapauto.gdarcameraservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Surface;

import com.autonavi.amapauto.gdarcameraservice.model.GDArCameraParam;
import com.bx.carDVR.DvrService;
import com.bx.carDVR.manager.NotifyMessageManager;
import com.bx.carDVR.util.LogUtils;

public class GDArCameraService extends Service {

    private static final String TAG = "GDArCameraService";

    private boolean isCameraOpened;

    public GDArCameraService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return iBinder;
    }


    private IBinder iBinder = new IGDCameraService.Stub() {
        @Override
        public boolean registerCameraStateCallback(String clientId, IGDCameraStateCallBack gdCameraStateCallBack) throws RemoteException {
            LogUtils.getInstance().d(TAG,"registerCameraStateCallback");
            NotifyMessageManager.getInstance().setGDCameraStateCallBack(gdCameraStateCallBack);
            return true;
        }

        @Override
        public boolean unregisterCameraStateCallback(String clientId, IGDCameraStateCallBack gdCameraStateCallBack) throws RemoteException {
            LogUtils.getInstance().d(TAG,"registerCameraStateCallback");
            NotifyMessageManager.getInstance().setGDCameraStateCallBack(gdCameraStateCallBack);
            return true;
        }

        @Override
        public boolean isSupportArNavi(String clientId) throws RemoteException {
            LogUtils.getInstance().d(TAG,"isSupportArNavi");
            return true;
        }

        @Override
        public IGDSize getRecommendSize(String clientId) throws RemoteException {
            return null;
        }

        @Override
        public boolean isCameraConnected(String clientId) throws RemoteException {
            boolean isConnected = DvrService.getInstance().isCameraOpened(DvrService.CAMERA_FRONT_ID);
            LogUtils.getInstance().d(TAG,"isCameraConnected : "+isConnected);
            return isConnected;
        }

        @Override
        public boolean isCameraOpened(String clientId) throws RemoteException {
            LogUtils.getInstance().d(TAG,"isCameraOpened : "+isCameraOpened);
            return isCameraOpened;
        }

        @Override
        public boolean initCamera(String clientId, GDArCameraParam gdArCameraParam, Surface surface) throws RemoteException {
            LogUtils.getInstance().d(TAG,"initCamera");
            return true;
        }

        @Override
        public boolean openCamera(String clientId) throws RemoteException {
            LogUtils.getInstance().d(TAG,"openCamera");
            isCameraOpened = true;
            return true;
        }

        @Override
        public boolean closeCamera(String clientId) throws RemoteException {
            LogUtils.getInstance().d(TAG,"closeCamera");
            NotifyMessageManager.getInstance().closeMemoryFile();
            isCameraOpened = false;
            return false;
        }

        @Override
        public boolean unInitCamera(String clientId) throws RemoteException {
            LogUtils.getInstance().d(TAG,"unInitCamera");
            return false;
        }
    };

}
