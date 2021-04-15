package com.bx.carDVR.manager;

import android.content.Context;
import android.os.PowerManager;

import com.bx.carDVR.util.LogUtils;

public class BxWakeManager {

    private static final String TAG = "QcWakeManager";
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock = null;
    private PowerManager.WakeLock wakeLock = null;

    public BxWakeManager(Context context) {
        LogUtils.getInstance().d(TAG, "QcWakeManager init");
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    public void acquireScreenWakeLock(){
        if(null == mWakeLock){
            mWakeLock = mPowerManager.newWakeLock
                    (PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "BxDVR");
            mWakeLock.acquire();
        }
    }

    public void releaseScreenWakeLock(){
        if(null != mWakeLock){
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    public void requestPowerLock(){
        LogUtils.getInstance().d(TAG, "requestPowerLock()");
        if(null == wakeLock){
            wakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "net_wakelock");
            if (null != wakeLock){
                wakeLock.acquire();
            }
        }
    }

    public void releasePowerLock(){
        LogUtils.getInstance().d(TAG, "releasePowerLock()");
        if (null != wakeLock){
            wakeLock.release();
            wakeLock = null;
        }
    }

    public boolean isScreenOn() {
        if (mPowerManager != null) {
            return mPowerManager.isScreenOn();
        }
        return false;
    }
}
