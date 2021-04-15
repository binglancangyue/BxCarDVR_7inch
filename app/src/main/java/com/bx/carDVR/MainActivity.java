package com.bx.carDVR;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bx.carDVR.bean.Configuration;
import com.bx.carDVR.ui.FloatPreviewWindow;
import com.bx.carDVR.util.DisplayUtils;
import com.bx.carDVR.util.LogUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BxMainActivity";

    private DvrService mDvrService;
    public static final String KEY_SHOW_CAMERA_ID = "cameraid";
    public static final String KEY_REQUEST_MODE = "requestMode";
    private static int requestMode ;
    public static final String ACTION_CLOSE_ACTIVITY_INNER = "com.bx.action.close_activity";
    public static final int ACTIVITY_STATE_FULLSCREEN = 1;
    public static final int ACTIVITY_STATE_NORMAL = 2;
    public static final int ACTIVITY_STATE_CLOSED = 3;

    public static final int ACTIVITY_STATE_ONCREATE = 10;
    public static final int ACTIVITY_STATE_ONRESUME = 11;
    public static final int ACTIVITY_STATE_ONPAUSE = 12;
    public static final int ACTIVITY_STATE_ONDESTORY = 13;

    private int mLife;
    private String mCameraId = DvrService.CAMERA_FRONT_ID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtils.getInstance().d(TAG,"onCreate");
        Intent intent = getIntent();
        requestMode = intent.getIntExtra(KEY_REQUEST_MODE, FloatPreviewWindow.REQUEST_WINDOW_NORMAL);
        mCameraId = intent.getStringExtra(KEY_SHOW_CAMERA_ID);
        if (Configuration.ONLY_BACK_CAMERA) {
            mCameraId = DvrService.CAMERA_BACK_ID;
        }
        setContentView(R.layout.activity_main);
        hideNavigationBar();
        Intent startIntent = new Intent(MainActivity.this, DvrService.class);
        startService(startIntent);
        Intent bindIntent = new Intent(MainActivity.this, DvrService.class);
        bindService(bindIntent, conn, Context.BIND_AUTO_CREATE);
        updateLife(ACTIVITY_STATE_ONCREATE);
        registerBroadcast();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtils.getInstance().d(TAG,"onResume");
        updateLife(ACTIVITY_STATE_ONRESUME);
    }

    private ServiceConnection conn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected()");
            if (null == mDvrService) {
                mDvrService = ((DvrService.DvrBinder) service).getService();
            }
            if(null != mDvrService){
                mDvrService.requestFloatWindowState(requestMode, mCameraId);
            }
            updateLife(mLife);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected()");
            mDvrService = null;
        }

    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        requestMode = intent.getIntExtra(KEY_REQUEST_MODE, FloatPreviewWindow.REQUEST_WINDOW_NORMAL);
        mCameraId = intent.getStringExtra(KEY_SHOW_CAMERA_ID);
        LogUtils.getInstance().d(TAG, "onNewIntent() requestMode=" + requestMode+";mCameraId="+mCameraId);
        if(null != mDvrService){
            mDvrService.requestFloatWindowState(requestMode, mCameraId);
        }
        updateLife(ACTIVITY_STATE_ONCREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        updateLife(ACTIVITY_STATE_ONPAUSE);
        String currentRunningActivity = DisplayUtils.getInstance().getRunningActivityName(this);
        LogUtils.getInstance().d(TAG, "onPause() runningactivity = " + currentRunningActivity);
        if ("com.bixin.speechrecognitiontool.MainActivity".equals(currentRunningActivity)
            ||"com.bx.carDVR.MainActivity".equals(currentRunningActivity)) {
            LogUtils.getInstance().d(TAG, "onPause() ignore this onPause.");
            return;
        }
        if(null != mDvrService){
            if (Configuration.IS_SUPPORT_SPLIT) {
                mDvrService.requestFloatWindowState(FloatPreviewWindow.REQUEST_WINDOW_SPLIT, "0");
            } else {
                mDvrService.requestFloatWindowState(FloatPreviewWindow.REQUEST_WINDOW_MINI, "0");
            }
        }
        showNavigationBar();

        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        updateLife(ACTIVITY_STATE_ONDESTORY);
        if (null != mDvrService) {
            unbindService(conn);
        }
        unregisterReceiver(localReceiver);
    }

    private void showNavigationBar() {
        if (!Configuration.IS_3IN ) {
            return;
        }
        if (Configuration.KEY_RECORD.equals(Configuration.KD003)) {
            return;
        }
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void hideNavigationBar() {
        if (!Configuration.IS_3IN) {
            return;
        }
        if (Configuration.KEY_RECORD.equals(Configuration.KD003)) {
            return;
        }
        int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        uiFlags |= 0x00001000;
        getWindow().getDecorView().setSystemUiVisibility(uiFlags);
    }

    private void updateLife(int life){
        mLife = life;
        if(null != mDvrService){
            mDvrService.updateActivityLife(life);
        }
    }

    private void registerBroadcast(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CLOSE_ACTIVITY_INNER);
        registerReceiver(localReceiver, filter);
    }

    private BroadcastReceiver localReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtils.getInstance().d(TAG, "localReceiver onReceive() action = "+action);
            if(ACTION_CLOSE_ACTIVITY_INNER.equals(action)){
                MainActivity.this.finish();
            }
        }
    };

}