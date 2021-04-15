package com.bx.carDVR.util;

import android.os.SystemClock;
import android.util.Log;

public class ClickUtils {

    private static final String TAG = "ClickUtils";
    private static long lastClickTime = 0L;
    private static final boolean isDebug = false;
    private static final String BLANK_LOG = "\t";


    /**
     *
     * @return true:
     */
    public static boolean isFastDoubleClick() {
        long nowTime = SystemClock.elapsedRealtime();

        if (isDebug){
            Log.d(TAG,"nowTime:" + nowTime);
            Log.d(TAG,"lastClickTime:" + lastClickTime);
            Log.d(TAG,"time:"+(nowTime - lastClickTime));
        }
        if ((nowTime - lastClickTime) < 1200) {

            if (isDebug){
                Log.d(TAG,"quick click");
                Log.d(TAG, BLANK_LOG);
            }
            return true;
        } else {
            lastClickTime = nowTime;

            if (isDebug){
                Log.d(TAG,"lastClickTime:" + lastClickTime);
                Log.d(TAG,"not quick click");
                Log.d(TAG,BLANK_LOG);
            }
            return false;
        }
    }

}
