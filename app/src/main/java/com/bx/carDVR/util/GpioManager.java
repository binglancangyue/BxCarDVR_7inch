package com.bx.carDVR.util;

import android.os.Looper;
import android.util.Log;

import com.bx.carDVR.ui.FloatToast;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GpioManager {

    private static final String TAG = "BxGpioManager";
    private static final String GPIO_PATH = "/dev/bx_gpio";

    private static GpioManager gpioManager;
    private char ahdChar = '0';
    private boolean isAhdStatusChanged = false;
    private char reverseChar = '0';
    private boolean isReverseStatusChanged = false;

    private static final int STATE_AHD_PLUGIN_IN = 1;
    private static final int STATE_AHD_PLUGIN_OUT = 2;
    private static final int STATE_REVERSE_ON = 3;
    private static final int STATE_REVERSE_OFF = 4;

    private int mAHDState = -1;
    private int mReverseState = -1;

    private static final int MSG_AHD_PLUGIN_IN = 1;
    private static final int MSG_AHD_PLUGIN_OUT = 2;
    private static final int MSG_REVERSE_TURN_ON = 3;
    private static final int MSG_REVERSE_TURN_OFF = 4;

    private GpioHandler mHandler;
    private Thread mReaderThread;
    private boolean isNeedRunning = false;
    private List<GpioStateChangeListener> mGpioStateChangeListenerList;
    private class GpioHandler extends android.os.Handler {
        public GpioHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_AHD_PLUGIN_IN:
                    LogUtils.getInstance().d(TAG, "ahd plugin in!");
                    for (GpioStateChangeListener listener : mGpioStateChangeListenerList) {
                        listener.onAhdStateChanged(true);
                    }
                    break;
                case MSG_AHD_PLUGIN_OUT:
                    LogUtils.getInstance().d(TAG, "ahd plugin out!");
                    for (GpioStateChangeListener listener : mGpioStateChangeListenerList) {
                        listener.onAhdStateChanged(false);
                    }
                    break;
                case MSG_REVERSE_TURN_ON:
                    //FloatToast.makeText("正在倒车",FloatToast.LENGTH_LONG).show();
                    LogUtils.getInstance().d(TAG, "reverse turn on!");
                    if(isAhdPluginIn()){
                        for (GpioStateChangeListener listener : mGpioStateChangeListenerList) {
                            listener.onReverseStateChanged(true);
                        }
                    }
                    break;
                case MSG_REVERSE_TURN_OFF:
                    //FloatToast.makeText("倒车结束",FloatToast.LENGTH_LONG).show();
                    LogUtils.getInstance().d(TAG, "reverse turn off!");
                    for (GpioStateChangeListener listener : mGpioStateChangeListenerList) {
                        listener.onReverseStateChanged(false);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private GpioManager() {
        mGpioStateChangeListenerList = new ArrayList<>();
        //处理UI更新线程
        mHandler = new GpioHandler(Looper.getMainLooper());
        //读取gpio状态线程
        mReaderThread = new Thread(runnable);
    }

    public static GpioManager getInstance() {
        if (null == gpioManager) {
            gpioManager = new GpioManager();
        }
        return gpioManager;
    }

    public void startCheck() {
        isNeedRunning = true;
        mReaderThread.start();
    }

    public void stopCheck() {
        isNeedRunning = false;
    }

    private int ahdCount = 0;
    private int reverseCount = 0;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            LogUtils.getInstance().d(TAG, "start check...");
            isAhdStatusChanged = true;
            while (isNeedRunning){
                try {
                    char[] buf = new char[2];
                    readDevice(buf);
                    char a = buf[0];
                    LogUtils.getInstance().d(TAG, "ahdChar = " + ahdChar +" , buf[0] = "+a);
                    if (buf[0] != ahdChar) {
                        ahdCount++;
                        if (ahdCount >= 5) {
                            isAhdStatusChanged = true;
                            ahdChar = buf[0];
                        }
                    } else {
                        ahdCount = 0;
                    }

                    if (buf[1] != reverseChar) {
                        reverseCount++;
                        if (reverseCount >= 1) {
                            isReverseStatusChanged = true;
                            reverseChar = buf[1];
                        }
                    } else {
                        reverseCount = 0;
                    }

                    if (isAhdStatusChanged) {
                        LogUtils.getInstance().d(TAG, "ahdChar = " + ahdChar);
                        mHandler.removeMessages(MSG_AHD_PLUGIN_IN);
                        mHandler.removeMessages(MSG_AHD_PLUGIN_OUT);
                        if (ahdChar == '1') {
                            mAHDState = STATE_AHD_PLUGIN_IN;
                            mHandler.sendEmptyMessageDelayed(MSG_AHD_PLUGIN_IN,500);
                        } else {
                            mAHDState = STATE_AHD_PLUGIN_OUT;
                            mHandler.sendEmptyMessage(MSG_AHD_PLUGIN_OUT);
                        }
                    }

                    if (isReverseStatusChanged) {
                        Log.w(TAG, "reverseChar = " + reverseChar);
                        mHandler.removeMessages(MSG_REVERSE_TURN_ON);
                        mHandler.removeMessages(MSG_REVERSE_TURN_OFF);
                        if (reverseChar == '1') {
                            mReverseState = STATE_REVERSE_ON;
                            mHandler.sendEmptyMessage(MSG_REVERSE_TURN_ON);
                        } else {
                            mReverseState = STATE_REVERSE_OFF;
                            mHandler.sendEmptyMessage(MSG_REVERSE_TURN_OFF);
                        }
                    }
                    isReverseStatusChanged = false;
                    isAhdStatusChanged = false;
                    Thread.sleep(400);
                }catch (Exception e){
                    LogUtils.getInstance().d(TAG, "read gpio device status error!");
                    e.printStackTrace();
                }
            }
        }
    };

    private int readDevice(char[] buffer) {
        int result = -1;
        try {
            FileReader reader = new FileReader(GPIO_PATH);
            result = reader.read(buffer);
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean isAhdPluginIn(){
        return mAHDState == STATE_AHD_PLUGIN_IN;
    }

    public boolean isReversing(){
        return mReverseState == STATE_REVERSE_ON;
    }

    public void addGpioStateChangeListener(GpioStateChangeListener listener){
        if (!mGpioStateChangeListenerList.contains(listener)) {
            mGpioStateChangeListenerList.add(listener);
        }
    }

    public void removeGpioStateChangeListener(GpioStateChangeListener listener){
        mGpioStateChangeListenerList.remove(listener);
    }

    public interface GpioStateChangeListener{
        void onAhdStateChanged(boolean isPlugin);
        void onReverseStateChanged(boolean isTurnOn);
    }

}
