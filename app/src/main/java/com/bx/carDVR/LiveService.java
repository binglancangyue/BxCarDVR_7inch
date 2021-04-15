package com.bx.carDVR;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;

import com.bx.carDVR.util.LogUtils;

public class LiveService extends Service {

    private static final String TAG = "LiveService";

    public LiveService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return iBinder;
    }

    private IBinder iBinder = new IMyAidlInterface.Stub() {
        @Override
        public void startLive() throws RemoteException {
            saveLiveStatus(true);
        }

        @Override
        public void stopLive() throws RemoteException {
            saveLiveStatus(false);
        }
    };

    public void saveLiveStatus(boolean isLive){
        if (isLive) {
            DvrService.getInstance().closeCameraFromLive();
        } else {
            DvrService.getInstance().openCameraFromLive();
        }
    }
}
