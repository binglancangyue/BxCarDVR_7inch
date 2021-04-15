package com.bx.carDVR.util;

import android.util.Log;

public class LogUtils {

    private static LogUtils logUtils;

    private LogUtils(){

    }

    public static LogUtils getInstance(){
        if (logUtils == null) {
            logUtils = new LogUtils();
        }
        return logUtils;
    }

    public void d(String tag,String text) {
        Log.d(tag,text);
    }

    public void e(String tag,String text) {
        Log.e(tag,text);
    }

}
