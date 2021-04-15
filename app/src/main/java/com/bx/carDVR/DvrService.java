package com.bx.carDVR;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.amap.api.location.AMapLocation;
import com.bx.carDVR.app.DvrApplication;
import com.bx.carDVR.bean.Configuration;
import com.bx.carDVR.bean.TrackBean;
import com.bx.carDVR.bean.UploadBean;
import com.bx.carDVR.manager.ARNaviManager;
import com.bx.carDVR.manager.BxSoundManager;
import com.bx.carDVR.manager.BxStorageManager;
import com.bx.carDVR.manager.BxWakeManager;
import com.bx.carDVR.manager.ECarOverseaManager;
import com.bx.carDVR.manager.MapgooManager;
import com.bx.carDVR.manager.NotifyMessageManager;
import com.bx.carDVR.manager.ShareBufferManager;
import com.bx.carDVR.prefs.SettingsManager;
import com.bx.carDVR.ui.FloatPreviewWindow;
import com.bx.carDVR.ui.FloatToast;
import com.bx.carDVR.ui.FormatConfirmDialog;
import com.bx.carDVR.util.ClipVideoUtil;
import com.bx.carDVR.util.GpioManager;
import com.bx.carDVR.util.LocationManagerTool;
import com.bx.carDVR.util.LogUtils;
import com.bx.carDVR.util.StorageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;

public class DvrService extends Service implements CameraServiceCallback, FormatConfirmDialog.IFormatConfirmCallBack, GpioManager.GpioStateChangeListener {

    private static final String TAG = "BxDvrService";
    private static final String CAMERA_RECORD_STATUS = "camera_record_status";
    private static final int MSG_START_RECORD_TIME = 1;
    private static final int MSG_UPDATE_RECORD_TIME = 2;
    private static final int MSG_STOP_RECORD_TIME = 3;
    private static final int MSG_SWITCH_NEXT_FILE = 4;
    public static final int MSG_GSENSOR_LOCKED = 5;
    private static final int MSG_AUTO_LAUNCH_PREVIEW = 6;
    private static final int MSG_LOCAL_CONFIGURE_CHANGED = 7;
    public static final int MSG_STOP_RECORD_DURE_TO_SPACE = 8;
    private static final int MSG_CPU_TEMPERATURE_CHANGE = 9;
    private static final int MSG_STOP_RECORD = 10;
    private static final int MSG_CLOSE_CAMERA = 11;
    private static final int MSG_SYSTEM_SLEEP = 12;
    private static final int MSG_SYSTEM_WAKEUP = 13;
    private static final int MSG_REQUEST_RECODING_TIME = 14;
    private static final int MSG_OPEN_CAMERA = 15;
    private static final int MSG_NET_WAKEUP = 16;
    private static final int MSG_TIME_TO_SLEEP = 17;
    private static final int MSG_MODIFY_RECORD_TIME = 18;
    private static final int MSG_MODIFY_COLLISION_LOCK_VALUE = 19;
    public static final int MSG_NOTIFY_SD_CARD_ERROR = 20;
    private static final int MSG_READ_SIM_STATE = 21;
    private static final int MSG_MODIFY_RECORD_AND_PREVIEW_FRAME = 22;

    private static final int MSG_FORMAT_TF_CARD = 23;

    private static final int MSG_TAKE_PIC_FROM_SYSTEMUI = 24;
    private static final int MSG_OPEN_BACK_CAMERA_FROM_SYSTEMUI = 25;
    public static final int MSG_UPLOAD_COLLISION_VIDEO_INFO = 26;
    private static final int MSG_RECORD_STATUS_FROM_SYSTEMUI = 27;

    private static final int MSG_FIRST_LAUNCH_PREVIEW = 28;
    private static final int MSG_START_RECORD_BY_POWER = 29;
    //public static final int DELAY_TIME_CHECK_STORAGE_SPACE = 2 * 60 * 1000;// 5min
    public static final int DELAY_TIME_CHECK_SD_CARD_STATUS = 5 * 1000;// 5s
    public static final int CHECK_SD_CARD_STATUS_COUNT = 10;
    //public static final int MSG_CHECK_STORAGE_SPACE = 1;
    public static final int MSG_DELETE_FILE_WHEN_UNMOUNT = 2;
    public static final int MSG_CHECK_SD_CARD_STATUS = 3;
    public static final String PATH_CHECK_SD_CARD_STATUS = "/sys/devices/platform/bx_gpio/mmc_error";

    public static final boolean SUPPORT_MAILIANBAO = true;

    public static final String CAMERA_FRONT_ID = "0";
    public static final String CAMERA_BACK_ID = "1";
    private static DvrService sDvrService;
    private CameraManager mCameraManager;
    private HandlerThread handlerThread;
    private BackgroundHandler backgroundHandler;
    private List<CameraInstance> cameraList = new ArrayList<>();
    private FloatPreviewWindow floatPreviewWindow;
    private ContentResolver contentResolver;

    private BxStorageManager mStorageManager;
    private BxSoundManager mSoundManager;
    private BxWakeManager mWakeManager;
    private SensorManager mSensorManager;
    private SensorEvent mSensorEvent;
    private FormatConfirmDialog mFormatConfirmDialog;
    private StorageManager storageManager;

    private LocationManagerTool locationManagerTool;

    private int mActivityLife = MainActivity.ACTIVITY_STATE_ONDESTORY;

    private DvrBinder dvrBinder = new DvrBinder();
    private GpioManager gpioManager;
    private MapgooManager mapgooManager;

    private List<TrackBean> tracks;
    private boolean isReadyUpload = false;

    private Timer mRecordingTimer;

    public static boolean isSystemSleep = false;

    public class DvrBinder extends Binder {
        public DvrService getService() {
            return DvrService.this;
        }
    }

    public static DvrService getInstance(){
        return sDvrService;
    }

    private boolean isPowerConnected = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.getInstance().d(TAG,"onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind:");
        return dvrBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.getInstance().d(TAG,"onCreate");
        sDvrService = this;
        contentResolver = getContentResolver();
        storageManager = getSystemService(StorageManager.class);
        gpioManager = GpioManager.getInstance();
        gpioManager.addGpioStateChangeListener(this);
        gpioManager.startCheck();
        saveCameraRecordStatus(false);
        saveClipVideoStatus(false);
        if (SUPPORT_MAILIANBAO) {
            mapgooManager = new MapgooManager();
        }
        initData();
        registerReceiver();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorEventListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
        if (Configuration.PROJECT_NAME.equals(Configuration.KD003)) {
            tracks = new ArrayList<>();
        }
    }

    public void updateActivityLife(int state){
        LogUtils.getInstance().d(TAG,"updateActivityLife() state = "+state);
        mActivityLife = state;
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Configuration.ACTION_CLOSE_DVR);
        filter.addAction(Configuration.ACTION_SET_DVR_RECORD_TIME);
        filter.addAction(Configuration.ACTION_SET_ADAS_LEVEL);
        filter.addAction(Configuration.ACTION_SET_G_SENSOR_LEVEL);
        filter.addAction(Configuration.ACTION_FORMAT_SD_CARD);
        filter.addAction(Configuration.ACTION_STOP_RECORD);
        //filter.addAction(Configuration.ACTION_UPLOAD);
        filter.addAction(Configuration.ACTION_SETTINGS_FUNCTION);
        filter.addAction(Configuration.ACTION_GAODE_SEND);
        filter.addAction(Configuration.ACTION_BX_RECEIVE);
        filter.addAction(Configuration.ACTION_SYSTEM_SLEEP);
        filter.addAction(Configuration.ACTION_SYSTEM_WAKE_UP);
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction(Configuration.ACTION_SPEECH_TOOL_CMD);
        filter.addAction(Configuration.ACTION_POWER_CONNECTED);
        filter.addAction(Configuration.ACTION_POWER_DISCONNECTED);
        registerReceiver(CameraConnectionReceiver, filter);
        if (mStorageManager != null) {
            mStorageManager.register(this);
        }
    }

    private void initData() {
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mStorageManager = new BxStorageManager(this);
        mSoundManager = new BxSoundManager(this);
        mWakeManager = new BxWakeManager(this);
        handlerThread = new HandlerThread("DvrService");
        handlerThread.start();
        backgroundHandler = new BackgroundHandler(handlerThread.getLooper());

        if (null == cameraList) {
            cameraList = new ArrayList<>();
        }

        recordTimeDuration = SettingsManager.getInstance().getRecordTime()*1000;
        nextRecordTime = SettingsManager.getInstance().getRecordTime()*1000;
        if (!Configuration.ONLY_BACK_CAMERA) {
            cameraList.add(new CameraInstance(this, mCameraManager, CAMERA_FRONT_ID));
        }
        cameraList.add(new CameraInstance(this, mCameraManager, CAMERA_BACK_ID));

        floatPreviewWindow = FloatPreviewWindow.getInstance();
        floatPreviewWindow.onCreate();
        if (Configuration.ONLY_BACK_CAMERA){
            requestFloatWindowState(FloatPreviewWindow.REQUEST_WINDOW_FIRST_LAUNCH, CAMERA_BACK_ID);
        }else{
            requestFloatWindowState(FloatPreviewWindow.REQUEST_WINDOW_FIRST_LAUNCH, CAMERA_FRONT_ID);
        }
        openCameraAtFirst();
        mMainHandler.sendEmptyMessage(MSG_FIRST_LAUNCH_PREVIEW);
        locationManagerTool = LocationManagerTool.getInstance();
        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                locationManagerTool.startGaoDe();
            }
        },1000);
        //ECarOverseaManager.getInstance().startECarListener();
    }


    private long mStartTime;
    private long cur;
    private long dura;
    private boolean mSwitchFileFlag = false;
    public static final long[] RECORD_TIME_DURATIONS = new long[] { 60 * 1000, 3 * 60 * 1000, 5 * 60 * 1000 };
    private static long recordTimeDuration = RECORD_TIME_DURATIONS[0];
    private long nextRecordTime = RECORD_TIME_DURATIONS[0];
    private static final long DELAY_TIME_SWITCH_NEXT_FILE = 3 * 1000;
    private static final long DELAY_TIME_UPDATE_RECORD_TIME = 1000;
    @SuppressLint("HandlerLeak")
    private Handler mMainHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            LogUtils.getInstance().d(TAG,"handleMessage : "+msg.what);
            switch (msg.what) {
                case MSG_START_RECORD_TIME:
                    dura = 0;
                    mSwitchFileFlag = false;
                    mStartTime = System.currentTimeMillis();
                    LogUtils.getInstance().d(TAG, "msg == MSG_START_RECORD_TIME");
                    removeMessages(MSG_UPDATE_RECORD_TIME);
                    sendEmptyMessage(MSG_UPDATE_RECORD_TIME);
                    break;
                case MSG_UPDATE_RECORD_TIME:
                    cur = System.currentTimeMillis();
                    dura = Math.abs(cur - mStartTime);
                    //LogUtils.getInstance().d(TAG,"mStartTime = "+mStartTime+" , cur = "+cur+" , dura = "+dura);
                    if (dura > recordTimeDuration - DELAY_TIME_SWITCH_NEXT_FILE && !mSwitchFileFlag) {
                        sendEmptyMessageDelayed(MSG_SWITCH_NEXT_FILE, DELAY_TIME_SWITCH_NEXT_FILE);
                        mSwitchFileFlag = true;
                    }
                    if (null != mRecordStateChangeListener) {
                        mRecordStateChangeListener.onRecordTimeUpdate(dura);
                    }
                    addTrack(0);
                    removeMessages(MSG_UPDATE_RECORD_TIME);
                    sendEmptyMessageDelayed(MSG_UPDATE_RECORD_TIME, DELAY_TIME_UPDATE_RECORD_TIME);
                    break;
                case MSG_SWITCH_NEXT_FILE:
                    recordTimeDuration = nextRecordTime;
                    switchNextFile();
                    sendEmptyMessage(MSG_START_RECORD_TIME);
                    break;
                case MSG_FIRST_LAUNCH_PREVIEW:
                    if (Configuration.ONLY_BACK_CAMERA) {
                        requestFloatWindowState(FloatPreviewWindow.REQUEST_WINDOW_NORMAL, CAMERA_BACK_ID);
                    } else {
                        requestFloatWindowState(FloatPreviewWindow.REQUEST_WINDOW_NORMAL, CAMERA_FRONT_ID);
                    }
                    break;
                case MSG_AUTO_LAUNCH_PREVIEW:
                case MSG_OPEN_BACK_CAMERA_FROM_SYSTEMUI:
                    if (mActivityLife != MainActivity.ACTIVITY_STATE_ONRESUME) {
                        startCameraActivity(FloatPreviewWindow.REQUEST_WINDOW_NORMAL,CAMERA_FRONT_ID);
                    } else {
                        requestFloatWindowState(FloatPreviewWindow.REQUEST_WINDOW_NORMAL,CAMERA_FRONT_ID);
                    }
                    //requestFloatWindowState(FloatPreviewWindow.REQUEST_WINDOW_NORMAL, CAMERA_FRONT_ID);
                    break;
                case MSG_NOTIFY_SD_CARD_ERROR:
                    LogUtils.getInstance().d(TAG,"MSG_NOTIFY_SD_CARD_ERROR");
                    StorageUtils.getInstance().showUnmountTips();
                    break;
                case MSG_STOP_RECORD_TIME:
                    dura = 0;
                    mSwitchFileFlag = false;
                    removeMessages(MSG_UPDATE_RECORD_TIME);
                    if (null != mRecordStateChangeListener) {
                        mRecordStateChangeListener.onRecordTimeUpdate(0);
                    }
                    break;
                case MSG_MODIFY_RECORD_TIME:
                    int time = (int) msg.obj;
                    int alreadyTime = SettingsManager.getInstance().getRecordTime();
                    if (time < SettingsManager.ONE_MINUTE || time > SettingsManager.FIVE_MINUTE) {
                        break;
                    }
                    if (time != alreadyTime) {
                        if (!cameraList.get(0).isRecordOpened()) {
                             recordTimeDuration = time * 1000;
                        }
                        nextRecordTime = time * 1000;
                        SettingsManager.getInstance().saveRecordTimeInterface(time);
                    }
                    break;
                case MSG_GSENSOR_LOCKED:
                    LogUtils.getInstance().d(TAG,"MSG_GSENSOR_LOCKED");
                    if (null != mRecordStateChangeListener) {
                        if(!isLocked(CAMERA_FRONT_ID)){
                            mRecordStateChangeListener.onGsonsorLocked();
                        }
                    }
                    break;
                case MSG_FORMAT_TF_CARD:
                    if(null == mFormatConfirmDialog){
                        mFormatConfirmDialog = FormatConfirmDialog.getInstance();
                    }
                    mFormatConfirmDialog.setFormatConfirmListener(DvrService.this);
                    mFormatConfirmDialog.showConfirmDialog(DvrService.this);
                    break;
                case MSG_STOP_RECORD_DURE_TO_SPACE:
                    break;
                case MSG_TAKE_PIC_FROM_SYSTEMUI:
                    if (isSdcardEnable()) {
                        if (isAllRecordOpened()) {
                            LogUtils.getInstance().d(TAG,"MSG_TAKE_PIC_FROM_SYSTEMUI");
                            takePictures();
                        } else {
                            FloatToast.makeText(R.string.take_pic_before_record,FloatToast.LENGTH_SHORT).show();
                        }
                    } else {
                        FloatToast.makeText(R.string.card_not_exist,FloatToast.LENGTH_SHORT).show();
                    }
                    break;
                case MSG_UPLOAD_COLLISION_VIDEO_INFO:
                    boolean isAutoUpload = (boolean) msg.obj;
                    if (isCameraRecording()) {
                        LogUtils.getInstance().d(TAG,"MSG_UPLOAD_COLLISION_VIDEO_INFO dura = "+dura);
                        long diffTime = ClipVideoUtil.CUT_AFTER_DURATION*1000 - recordTimeDuration + dura;
                        long laveTime = recordTimeDuration - dura;
                        LogUtils.getInstance().d(TAG,"MSG_UPLOAD_COLLISION_VIDEO_INFO diffTime = "+diffTime+",laveTime=" +laveTime);
                        if (laveTime < ClipVideoUtil.CUT_AFTER_DURATION*1000) {
                            if (dura > recordTimeDuration - DELAY_TIME_SWITCH_NEXT_FILE) {
                                removeMessages(MSG_SWITCH_NEXT_FILE);
                                mSwitchFileFlag = false;
                            }
                            recordTimeDuration += diffTime+1000;
                        }
                        LogUtils.getInstance().d(TAG,"MSG_UPLOAD_COLLISION_VIDEO_INFO recordTimeDuration = "+recordTimeDuration);
                        long lockStartTime = System.currentTimeMillis();
                        saveClipVideoStatus(true);
                        isReadyUpload = true;
                        trackSize = tracks.size();
                        if (isAutoUpload) {
                            addTrack(1);
                        } else {
                            addTrack(0);
                        }
                        //setUploadInfo(isAutoUpload,lockStartTime);
                        lockVideo(lockStartTime);
                    } else {
                        if (isSdcardEnable()) {
                            startRecord();
                            long lockStartTime = System.currentTimeMillis();
                            saveClipVideoStatus(true);
                            //setUploadInfo(isAutoUpload,lockStartTime);
                            isReadyUpload = true;
                            trackSize = tracks.size();
                            if (isAutoUpload) {
                                addTrack(1);
                            } else {
                                addTrack(0);
                            }
                            lockVideo(lockStartTime);
                        } else {
                            FloatToast.makeText(R.string.please_open_record,FloatToast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case MSG_RECORD_STATUS_FROM_SYSTEMUI:
                    if (isSdcardEnable()) {
                        if (isCameraRecording()) {
                            stopRecord();
                        } else {
                            startRecord();
                        }
                    } else {
                        FloatToast.makeText(R.string.card_not_exist,FloatToast.LENGTH_SHORT).show();
                    }
                    break;
                case MSG_CLOSE_CAMERA:
                    Log.w(TAG, "MSG_CLOSE_CAMERA isAllRecordClosed=" + isAllRecordClosed()+" , isAllCameraClosed "+isAllCameraClosed());
                    if (isAllRecordClosed()) {
                        if (!isAllCameraClosed()) {
                            closeCamera();
                            sendEmptyMessageDelayed(MSG_CLOSE_CAMERA, 1000);
                        }
                    } else {
                        stopRecord();
                        sendEmptyMessageDelayed(MSG_CLOSE_CAMERA, 1000);
                    }
                    break;
                case MSG_OPEN_CAMERA:
                    openCameraAtFirst();
                    mMainHandler.sendEmptyMessageDelayed(MSG_AUTO_LAUNCH_PREVIEW, 3000);
                    break;
                case MSG_SYSTEM_SLEEP:
                    handleSystemSleep();
                    break;
                case MSG_SYSTEM_WAKEUP:
                    String reason = (String) msg.obj;
                    handleSystemWakeup(reason);
                    break;
                case MSG_START_RECORD_BY_POWER:
                    if (isSdcardEnable()) {
                        if (isPowerConnected) {
                            if (!isCameraRecording()) {
                                startRecord();
                            }
                        } else {
                            stopRecord();
                        }
                    } else {
                        FloatToast.makeText(R.string.card_not_exist, FloatToast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private class BackgroundHandler extends Handler {

        public BackgroundHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_DELETE_FILE_WHEN_UNMOUNT:
                    deleteFileWhenUnmount();
                    break;
                case MSG_CHECK_SD_CARD_STATUS:
                    if (mStorageManager !=null) {
                        mStorageManager.checkSdCardStatus();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void requestFloatWindowState(int requestWindowState, String mCameraId){
        if(null != floatPreviewWindow){
            floatPreviewWindow.requestWindowState(requestWindowState, mCameraId);
        }
    }

    private void openCameraAtFirst() {
//        for (CameraInstance cameraInstance : cameraList) {
//            cameraInstance.openCamera();
//        }
        if (!Configuration.ONLY_BACK_CAMERA) {
            cameraList.get(0).openCamera();
            if (cameraList.size() > 1) {
                if (!cameraList.get(1).isCameraOpened() && gpioManager.isAhdPluginIn()) {
                    cameraList.get(1).openCamera();
                }
            }
        } else {
            if (!cameraList.get(0).isCameraOpened() && gpioManager.isAhdPluginIn()) {
                cameraList.get(0).openCamera();
            } else {

            }
        }

//        mMainHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        },1000);
    }

    public void openCamera() {
        for (CameraInstance cameraInstance : cameraList) {
            if (cameraInstance.isCameraOpened()) {
                if (!cameraInstance.isCameraPreview()) {
                    cameraInstance.startPreview();
                }
            } else {
                cameraInstance.openCamera();
            }
        }
    }

    public void startPreview(String cameraId) {
        for (CameraInstance cameraInstance : cameraList) {
            if (cameraInstance.getCameraId().equals(cameraId)) {
                if(!cameraInstance.isCameraPreview()){
                    cameraInstance.startPreview();
                }
            }
        }
    }

    public synchronized void onRecordStarted(String mCameraId) {
        LogUtils.getInstance().d(TAG,"onRecordStarted() CB mCameraId = " + mCameraId + ";isCameraRecording = " + isCameraRecording());
        if (!isCameraRecording()) {
            saveCameraRecordStatus(true);
            recordTimeDuration = nextRecordTime;
            mMainHandler.sendEmptyMessage(MSG_START_RECORD_TIME);
        }
    }

    public void closeCamera(String cameraId) {
        for (CameraInstance cameraInstance : cameraList) {
            if (cameraInstance.getCameraId().equals(cameraId)) {
                cameraInstance.closeCamera();
            }
        }
    }

    public void closeCamera() {
        for (CameraInstance cameraInstance : cameraList) {
            cameraInstance.closeCamera();
        }
    }

    public void reopenCamera(String cameraId) {
        for (CameraInstance cameraInstance : cameraList) {
            if (cameraInstance.getCameraId().equals(cameraId)) {
                cameraInstance.reopenCamera();
            }
        }
    }

    public void reopenCamera() {
        for (CameraInstance cameraInstance : cameraList) {
                cameraInstance.reopenCamera();
        }
    }

    public synchronized void takePictures() {
        for (CameraInstance cameraInstance : cameraList) {
            if(cameraInstance.isRecordOpened()){
                cameraInstance.takePictures();
            }
        }
    }

    /**
     * clip video before after 15s
     * @param time
     */
    public synchronized void lockVideo(long time) {
        for (CameraInstance cameraInstance : cameraList) {
            if(cameraInstance.isRecordOpened()){
                cameraInstance.lockVideo(true,time);
            }
        }
    }

    public void switchNextFile(){
        for (CameraInstance cameraInstance : cameraList) {
            if (CAMERA_BACK_ID.equals(cameraInstance.getCameraId())
                    && !gpioManager.isAhdPluginIn()){
                LogUtils.getInstance().d(TAG,"switchNextFile() ahd is not plugin in!");
                continue;
            }
            cameraInstance.switchNextFile();
        }
    }

    public void setCameraStateChangeListener(String cameraId, CameraInstance.CameraStateChangeListener listener) {
        for (CameraInstance cameraInstance : cameraList) {
            if (cameraInstance.getCameraId().equals(cameraId)) {
                cameraInstance.setCameraStateChangeListener(listener);
            }
        }
    }

    public boolean isCameraRecording() {
        if(null != contentResolver){
            try {
                int status = Settings.Global.getInt(contentResolver, CAMERA_RECORD_STATUS);
                LogUtils.getInstance().d(TAG,"CAMERA_RECORD_STATUS = "+status);
                return status == 1;
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean isClipVideo() {
        if(null != contentResolver){
            try {
                int status = Settings.Global.getInt(contentResolver, Configuration.CLIP_VIDEO_STATUS);
                LogUtils.getInstance().d(TAG,"CLIP_VIDEO_STATUS = "+status);
                return status == 1;
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void saveCameraRecordStatus(boolean isRecording){
        if(null != contentResolver){
            LogUtils.getInstance().d(TAG,"saveCameraRecordStatus() isRecording = " + isRecording);
            Settings.Global.putInt(contentResolver, CAMERA_RECORD_STATUS, isRecording ? 1 : 0);
        }
    }

    public void saveClipVideoStatus(boolean isClipVideo) {
        if(null != contentResolver){
            LogUtils.getInstance().d(TAG,"saveClipVideoStatus() isClipVideo = " + isClipVideo);
            Settings.Global.putInt(contentResolver, Configuration.CLIP_VIDEO_STATUS, isClipVideo ? 1 : 0);
        }
    }

    public boolean isAllRecordOpened() {
        for (CameraInstance cameraInstance : cameraList) {
            if (cameraInstance.getCameraPluginStatus() && !cameraInstance.isRecordOpened()) {
                return false;
            }
        }
        return true;
    }

    private void deleteFileWhenUnmount(){
        LogUtils.getInstance().d(TAG, "deleteFileWhenUnmount()");
        File file = new File("/storage/emulated/0/adas.log");
        if(file.exists()){
            LogUtils.getInstance().d(TAG, "deleteFileWhenUnmount() exists adas.log");
            file.delete();
        }
    }

    public synchronized void onRecordStopped(String mCameraId) {
        LogUtils.getInstance().d(TAG, "onRecordStopped() CB mCameraId = "+mCameraId);
        if (isCameraRecording() && isAllRecordClosed()) {
            saveCameraRecordStatus(false);
            mMainHandler.sendEmptyMessage(MSG_STOP_RECORD_TIME);
//            if(null != floatPreviewWindow){
//                floatPreviewWindow.updateRecordStatusWhenFullScreen(false);
//            }
        }
    }

    public boolean isAllRecordClosed() {
        for (CameraInstance cameraInstance : cameraList) {
            if (cameraInstance.getCameraPluginStatus() && !cameraInstance.isRecordClosed()) {
                return false;
            }
        }
        return true;
    }

    public boolean isAllCameraClosed() {
        for (CameraInstance cameraInstance : cameraList) {
            if (cameraInstance.getCameraPluginStatus() && !cameraInstance.isCameraClosed()) {
                return false;
            }
        }
        return true;
    }

    public boolean isLocked(String cameraId){
        for (CameraInstance cameraInstance : cameraList) {
            if (cameraInstance.getCameraId().equals(cameraId)) {
                return cameraInstance.isCameraLocked();
            }
        }
        return false;
    }

    /**
     * move to lock video folder
     * @param lockVideo
     */
    public void lockVideo(boolean lockVideo){
        for (CameraInstance cameraInstance : cameraList) {
            cameraInstance.lockVideo(lockVideo);
        }
    }

    private boolean isSdcardEnable() {
        if (StorageUtils.getInstance().isSDCardMounted()) {
            return true;
        } else {
            return false;
        }
    }

    //---CameraServiceCallback start----
    @Override
    public void mainHandlerSendMsg(int what) {
        if (mMainHandler != null) {
            mMainHandler.sendEmptyMessage(what);
        }
    }

    @Override
    public void mainHandlerSendMsgDelay(int what, int delay) {
        if (mMainHandler != null) {
            mMainHandler.sendEmptyMessageDelayed(what, delay);
        }
    }

    @Override
    public void mainHandlerRemoveMsg(int what) {
        if (mMainHandler != null) {
            mMainHandler.removeMessages(what);
        }
    }

    @Override
    public void backHandlerSendMsg(int what) {
        if (backgroundHandler != null) {
            backgroundHandler.sendEmptyMessage(what);
        }
    }

    @Override
    public void backHandlerSendMsgDelay(int what, int delay) {
        if (backgroundHandler != null) {
            backgroundHandler.sendEmptyMessageDelayed(what, delay);
        }
    }

    @Override
    public void backHandlerRemoveMsg(int what) {
        if (backgroundHandler != null) {
            backgroundHandler.removeMessages(what);
        }
    }

    @Override
    public boolean isCameraOpened(String cameraId) {
        for (CameraInstance cameraInstance : cameraList) {
            if (cameraInstance.getCameraId().equals(cameraId)) {
                return cameraInstance.isCameraOpened();
            }
        }
        return false;
    }

    @Override
    public boolean isRecordStarted(String cameraId) {
        for (CameraInstance cameraInstance : cameraList) {
            if (cameraInstance.getCameraId().equals(cameraId)) {
                return cameraInstance.isRecordStarted();
            }
        }
        return false;
    }

    @Override
    public void startRecord() {
        LogUtils.getInstance().d(TAG,"All startRecord()");
        for (CameraInstance cameraInstance : cameraList) {
            cameraInstance.startRecord();
        }
    }

    public void startRecord(String cameraId) {
        LogUtils.getInstance().d(TAG,"startRecord() cameraId : "+cameraId);
        for (CameraInstance cameraInstance : cameraList) {
            if (cameraInstance.getCameraId().equals(cameraId)) {
                cameraInstance.startRecord();
            }
        }
    }

    @Override
    public void stopRecord() {
        LogUtils.getInstance().d(TAG,"All stopRecord()");
        mMainHandler.removeMessages(MSG_SWITCH_NEXT_FILE);
        long delayTime = DELAY_TIME_SWITCH_NEXT_FILE - dura;
        for (CameraInstance cameraInstance : cameraList) {
            cameraInstance.stopRecord(delayTime);
        }
    }
    //---CameraServiceCallback end----


    private void startRecordingTime() {
        if (mRecordingTimer == null) {
            mRecordingTimer = new Timer();
            mRecordingTimer.schedule(new TimerTask() {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {

                    }
                };
                @Override
                public void run() {
                    mMainHandler.removeCallbacks(runnable);
                    mMainHandler.post(runnable);
                }
            },0,1000);
        }
    }

    public void playSound(int type) {
        if (mSoundManager != null) {
            mSoundManager.playSound(type);
        }
    }

    private RecordStateChangeListener mRecordStateChangeListener;

    public void setRecordStateChangeListener(RecordStateChangeListener listener) {
        this.mRecordStateChangeListener = listener;
    }

    public interface RecordStateChangeListener {
        void onRecordTimeUpdate(long time);

        void onGsonsorLocked();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(CameraConnectionReceiver);
        if (mStorageManager != null) {
            mStorageManager.unregister(this);
        }
        locationManagerTool.stopLocation();
        if (floatPreviewWindow != null) {
            floatPreviewWindow.onDestroy();
        }
        if (mSensorManager != null && mSensorEventListener != null) {
            mSensorManager.unregisterListener(mSensorEventListener);
            mSensorManager = null;
            mSensorEventListener = null;
        }
    }

    private void startCameraActivity(int requestState, String mCameraId){
        LogUtils.getInstance().d(TAG,"startCameraActivity() requestState = " + requestState+" ;mCameraId = " + mCameraId);
        Intent intent = new Intent(this,MainActivity.class);
        intent.putExtra(MainActivity.KEY_REQUEST_MODE, requestState);
        intent.putExtra(MainActivity.KEY_SHOW_CAMERA_ID, mCameraId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private BroadcastReceiver CameraConnectionReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtils.getInstance().d(TAG,"CameraConnectionReceiver action: "+action);
            switch (action){
                case Intent.ACTION_SHUTDOWN:
                    mMainHandler.sendEmptyMessage(MSG_CLOSE_CAMERA);
                    break;
                case Configuration.ACTION_CLOSE_DVR:
                    break;
                case Configuration.ACTION_SET_DVR_RECORD_TIME:
                    int time = intent.getIntExtra("record_time", 60);
                    Message message = mMainHandler.obtainMessage();
                    message.what = MSG_MODIFY_RECORD_TIME;
                    message.obj = time;
                    mMainHandler.sendMessage(message);
                    break;
                case Configuration.ACTION_SET_G_SENSOR_LEVEL:
                    int sensorLevel = intent.getIntExtra("g_sensor", 2);
                    LogUtils.getInstance().d(TAG,"sensorLevel = "+sensorLevel);
                    SettingsManager.getInstance().saveCollisionLockInterface(sensorLevel);
                    break;
                case Configuration.ACTION_FORMAT_SD_CARD:
                    mMainHandler.removeMessages(MSG_FORMAT_TF_CARD);
                    mMainHandler.sendEmptyMessage(MSG_FORMAT_TF_CARD);
                    break;
                case Configuration.ACTION_SETTINGS_FUNCTION:
                    String keyType = intent.getStringExtra("key_type");
                    if (Configuration.KEY_TAKE_PICTURE.equals(keyType)) {
                        mMainHandler.removeMessages(MSG_TAKE_PIC_FROM_SYSTEMUI);
                        mMainHandler.sendEmptyMessage(MSG_TAKE_PIC_FROM_SYSTEMUI);
                    } else if (Configuration.KEY_BACK_CAMERA.equals(keyType)) {
                        mMainHandler.removeMessages(MSG_OPEN_BACK_CAMERA_FROM_SYSTEMUI);
                        mMainHandler.sendEmptyMessage(MSG_OPEN_BACK_CAMERA_FROM_SYSTEMUI);
                    } else if (Configuration.KEY_UPLOAD.equals(keyType)) {
                        sendUploadVideo(false);
                    } else if (Configuration.KEY_RECORD_TIME.equals(keyType)) {
                        int key_time = intent.getIntExtra("key_time", 60);
                        Message msg = mMainHandler.obtainMessage();
                        msg.what = MSG_MODIFY_RECORD_TIME;
                        msg.obj = key_time;
                        mMainHandler.sendMessage(msg);
                    } else if (Configuration.TYPE_G_SENSOR.equals(keyType)) {
                        boolean status = SettingsManager.getInstance().isGSensorOpened();
                        SettingsManager.getInstance().saveGSensorStatus(!status);
                    } else if (Configuration.KEY_RECORD.equals(keyType)) {
                        mMainHandler.removeMessages(MSG_RECORD_STATUS_FROM_SYSTEMUI);
                        mMainHandler.sendEmptyMessage(MSG_RECORD_STATUS_FROM_SYSTEMUI);
                    }
                    break;
                case Configuration.ACTION_GAODE_SEND:
                    int keyCode = intent.getIntExtra("KEY_TYPE", 0);
                    if (keyCode == 12116) {
                        sendToGaoDe();
                    }
                    if (keyCode == 10019) {
                        int statusCode = intent.getIntExtra("EXTRA_STATE", -1);
                        switch (statusCode) {
                            case 1://
                            case 0:
                                sendToGaoDe();
                                LogUtils.getInstance().d(TAG, "onReceive:初始化、开始运⾏ "+statusCode);
                                break;
                            case 2://退出地图
                                LogUtils.getInstance().d(TAG, "onReceive:退出地图 ");
                                break;
                            case 8://开始导航
                                LogUtils.getInstance().d(TAG, "onReceive:开始导航 ");
                                break;
                            case 9://结束导航
                                LogUtils.getInstance().d(TAG, "onReceive:结束导航 ");
                                break;
                            case 501://开始AR导航
                                ARNaviManager.getInstance().startARNavigation();
                                //ShareBufferManager.getInstance().setARNaviStatus(true);
                                //cameraList.get(0).setAROpen(true);
                                //cameraList.get(0).addMapgooCmd(Configuration.CMD_GAODE_AR, System.currentTimeMillis());
                                LogUtils.getInstance().d(TAG, "onReceive:开始AR导航 ");
                                break;
                            case 502://退出AR导航
                                //cameraList.get(0).setAROpen(false);
                                //ShareBufferManager.getInstance().setARNaviStatus(false);
                                ARNaviManager.getInstance().stopARNavigation();
                                NotifyMessageManager.getInstance().closeMemoryFile();
                                LogUtils.getInstance().d(TAG, "onReceive:退出AR导航 ");
                                break;
                        }
                    }
                    break;
                case Configuration.ACTION_BX_RECEIVE:
                    boolean releaseStatus = intent.getBooleanExtra("release_camera",false);
                    int cameraId = intent.getIntExtra("camera_id",0);
                    if (releaseStatus) {
                        mMainHandler.removeMessages(MSG_OPEN_CAMERA);
                        mMainHandler.sendEmptyMessage(MSG_OPEN_CAMERA);
                    }
                    break;
                case Configuration.ACTION_SYSTEM_SLEEP :
                    mMainHandler.sendEmptyMessage(MSG_SYSTEM_SLEEP);
                    break;
                case Configuration.ACTION_SYSTEM_WAKE_UP:
                    Message msg = Message.obtain();
                    msg.obj = intent.getStringExtra("wakeup_reason");
                    msg.what = MSG_SYSTEM_WAKEUP;
                    mMainHandler.sendMessage(msg);
                    break;
                case Configuration.ACTION_SPEECH_TOOL_CMD:
                    String type = intent.getStringExtra("type");
                    if ("look_front".equals(type)) {
                        if (mActivityLife != MainActivity.ACTIVITY_STATE_ONRESUME) {
                            startCameraActivity(FloatPreviewWindow.REQUEST_WINDOW_NORMAL,CAMERA_FRONT_ID);
                        } else {
                            if(null != floatPreviewWindow){
                                floatPreviewWindow.handBackPreviewClick();
                            }
                        }
                    } else if ("look_back".equals(type)) {
                        if (gpioManager.isAhdPluginIn()) {
                            if (mActivityLife != MainActivity.ACTIVITY_STATE_ONRESUME) {
                                startCameraActivity(FloatPreviewWindow.REQUEST_WINDOW_NORMAL,CAMERA_BACK_ID);
                            } else {
                                if(null != floatPreviewWindow){
                                    floatPreviewWindow.handFrontPreviewClick();
                                }
                            }
                        } else {
                            FloatToast.makeText(R.string.ahd_hint,FloatToast.LENGTH_SHORT).show();
                        }
                    } else if ("dvr_close".equals(type)) {
                        sendBroadcast(new Intent(MainActivity.ACTION_CLOSE_ACTIVITY_INNER));
                        requestFloatWindowState(FloatPreviewWindow.REQUEST_WINDOW_MINI,"0");
                    }
                    break;
                case Configuration.ACTION_POWER_CONNECTED:
                    isPowerConnected = true;
                    mMainHandler.sendEmptyMessage(MSG_START_RECORD_BY_POWER);
                    break;
                case Configuration.ACTION_POWER_DISCONNECTED:
                    isPowerConnected = false;
                    mMainHandler.sendEmptyMessage(MSG_START_RECORD_BY_POWER);
                    break;
                default:
                    break;
            }
        }
    };

    public static final int SENSOR_LEVEL_HIGH = 28;// 20;
    public static final int SENSOR_LEVEL_MIDDLE = 38;// 30;
    public static final int SENSOR_LEVEL_LOW = 55;// 47;
    public static final int SENSOR_LEVEL_CLOSE = 100;// 47;
    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mSensorEvent = sensorEvent;
                float threshold = SENSOR_LEVEL_MIDDLE;
                float xlateral = sensorEvent.values[0];
                float ylongitudinal = sensorEvent.values[1];
                float zvertical = sensorEvent.values[2];
                int g_sensorLockLevel = SettingsManager.getInstance().getCollisionLock();
                if (g_sensorLockLevel == 0) {
                    threshold = SENSOR_LEVEL_CLOSE;
                } else if (g_sensorLockLevel == 1) {
                    threshold = SENSOR_LEVEL_LOW;
                } else if (g_sensorLockLevel == 2) {
                    threshold = SENSOR_LEVEL_MIDDLE;
                } else {
                    threshold = SENSOR_LEVEL_HIGH;
                }
                if ((xlateral > threshold) || (ylongitudinal > threshold) || (zvertical > threshold)) {
                    mSensorEvent = sensorEvent;
                    LogUtils.getInstance().d(TAG, "heading=" + xlateral + ", pitch=" + ylongitudinal + "," +
                            " roll=" + zvertical + ", " + "threshold=" + threshold);
                    mMainHandler.removeMessages(MSG_GSENSOR_LOCKED);
                    mMainHandler.sendEmptyMessage(MSG_GSENSOR_LOCKED);
//                    if (Configuration.PROJECT_NAME.equals(Configuration.KD003)) {
//                        if (SettingsManager.getInstance().isGSensorOpened()) {
//                            sendUploadVideo(true);
//                        }
//                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    public void onFormatConfirmed() {
        stopRecord();
        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                formatCard();
            }
        },2000);
    }

    @Override
    public void onFormatCanceled() {
        if(null != mFormatConfirmDialog){
            mFormatConfirmDialog.setFormatConfirmListener(null);
            mFormatConfirmDialog = null;
        }
    }

    @Override
    public void onAhdStateChanged(boolean isPlugin) {
        for (CameraInstance cameraInstance: cameraList) {
           if (CAMERA_BACK_ID.equals(cameraInstance.getCameraId())) {
               cameraInstance.setCameraPluginStatus(isPlugin);
           }
        }
        if (SUPPORT_MAILIANBAO) {
            mapgooManager.setMagooRequestCameraIndex(ShareBufferManager.BACK_CAMERA_ID,isPlugin);
        }
    }

    @Override
    public void onReverseStateChanged(boolean isTurnOn) {

    }

    private void handleSystemSleep() {
        LogUtils.getInstance().d(TAG,"handleSystemSleep");
        if (!isSystemSleep) {
            isSystemSleep = true;
            if (mWakeManager != null) {
                mWakeManager.releasePowerLock();
            }
            mMainHandler.removeMessages(MSG_OPEN_CAMERA);
            mMainHandler.removeMessages(MSG_AUTO_LAUNCH_PREVIEW);
            if (mSensorManager != null && mSensorEventListener != null) {
                mSensorManager.unregisterListener(mSensorEventListener);
            }
            gpioManager.removeGpioStateChangeListener(this);
            mStorageManager.stop();

            mMainHandler.removeMessages(MSG_SWITCH_NEXT_FILE);
            mMainHandler.removeMessages(MSG_UPDATE_RECORD_TIME);

            //mMainHandler.sendEmptyMessage(MSG_STOP_RECORD);
            mMainHandler.sendEmptyMessage(MSG_CLOSE_CAMERA);
            sendBroadcast(new Intent(MainActivity.ACTION_CLOSE_ACTIVITY_INNER));
        } else {
            LogUtils.getInstance().d(TAG,"handleSystemSleep system is already sleep");
        }
    }

    private void handleSystemWakeup(String wakeupReason) {
        LogUtils.getInstance().d(TAG,"handleSystemWakeup wakeupReason : "+wakeupReason+" , isSystemSleep : "+isSystemSleep);
        if (isSystemSleep) {
            isSystemSleep = false;
            SystemProperties.set("camera.sleep_status","false");
            mMainHandler.sendEmptyMessageDelayed(MSG_OPEN_CAMERA,1000);
            mMainHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    if (mSensorManager != null && mSensorEventListener != null) {
                        mSensorManager.registerListener(mSensorEventListener,
                                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                                SensorManager.SENSOR_DELAY_FASTEST);
                    }
                }
            }, 5000);
            gpioManager.addGpioStateChangeListener(this);
            mStorageManager.start();
        }

    }

    public void onCameraClosed(String cameraId) {
        if (isSystemSleep) {
            LogUtils.getInstance().d(TAG,"onCameraClosed isSystemSleep");
            SystemProperties.set("camera.sleep_status","true");
        }
    }

    public void closeCameraFromLive() {
        mMainHandler.removeMessages(MSG_CLOSE_CAMERA);
        mMainHandler.sendEmptyMessage(MSG_CLOSE_CAMERA);
    }
	
	public void openCameraFromLive() {
        mMainHandler.removeMessages(MSG_OPEN_CAMERA);
        mMainHandler.sendEmptyMessage(MSG_OPEN_CAMERA);
    }

    public static final int FORMATTING_RST_SUCCESSFUL = 1;
    public static final int FORMATTING_RST_FAIL = -1;
    public static final int FORMATTING_RST_FAIL_CARD_INVALID = -2;
    public static final int FORMATTING_RST_FAIL_CURRENTLY_RECORDING = -3;
    public static final String EXTRA_FORMAT_PRIVATE = "format_private";
    public static final String EXTRA_FORGET_UUID = "forget_uuid";
    private void formatCard() {
        int strId = -1;
        int result = startFormatting();
        if (result == FORMATTING_RST_FAIL_CURRENTLY_RECORDING) {
            strId = R.string.cannot_formatting;
        } else if (result == FORMATTING_RST_FAIL_CARD_INVALID) {
            strId = R.string.card_not_exist;
        } else if (result == FORMATTING_RST_FAIL) {
            strId = R.string.formatting_fail;
        }
        if (strId != -1) {
            FloatToast.makeText(strId,FloatToast.LENGTH_LONG).show();
        }
    }

    private int startFormatting() {
//        for (CameraInstance cameraInstance : cameraList) {
//            if (cameraInstance.isRecordOpened()) {
//                return FORMATTING_RST_FAIL_CURRENTLY_RECORDING;
//            }
//        }
        DiskInfo mDisk = storageManager.findDiskById(getDiskId());
        if (mDisk == null) {
            return DvrService.FORMATTING_RST_FAIL_CARD_INVALID;
        }
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardFormatProgress");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
        intent.putExtra(EXTRA_FORMAT_PRIVATE, false);
        intent.putExtra(EXTRA_FORGET_UUID, "");
        startActivity(intent);
        return FORMATTING_RST_SUCCESSFUL;
    }

    private String getDiskId() {
        String id = "";
        for (VolumeInfo volumeInfo : storageManager.getVolumes()) {
            if (VolumeInfo.TYPE_PUBLIC == volumeInfo.getType()) {
                id = volumeInfo.getDiskId();
            }
//            DiskInfo aa = volumeInfo.getDisk();
//            Log.d(TAG, "aa: VolumeInfo " + volumeInfo.getType() + " " + volumeInfo.getDiskId());
        }
        return id;
    }

    private void setUploadInfo(boolean isAutoUpload,long lockStartTime) {
        AMapLocation location = locationManagerTool.getAMapLocation();
        UploadBean uploadBean = new UploadBean();
        uploadBean.setEvent_time(lockStartTime);
        uploadBean.setAuto_upload(isAutoUpload);
        if (location != null) {
            LogUtils.getInstance().d(TAG, "setUploadInfo: location != null");
            uploadBean.setEvent_lat(location.getLatitude());
            uploadBean.setEvent_lon(location.getLongitude());
            uploadBean.setCar_speed((float) (location.getSpeed()*3.6));
        } else {
            LogUtils.getInstance().d(TAG, "setUploadInfo: location == null");
        }
        if (mSensorEvent != null) {
            float xLateral = mSensorEvent.values[0];
            float yLongitudinal = mSensorEvent.values[1];
            float zVertical = mSensorEvent.values[2];
            uploadBean.setG_x_axis(xLateral);
            uploadBean.setG_y_axis(yLongitudinal);
            uploadBean.setG_z_axis(zVertical);
        }
        String uploadInfo = JSONObject.toJSONString(uploadBean);
        DvrApplication.getDvrApplication().setUploadInfo(uploadInfo);
        LogUtils.getInstance().d(TAG, "setUploadInfo:uploadInfo " + uploadInfo);
    }

    public void sendUploadVideo(boolean isAuto) {
        mMainHandler.removeMessages(MSG_UPLOAD_COLLISION_VIDEO_INFO);
        Message msg = mMainHandler.obtainMessage();
        msg.what = MSG_UPLOAD_COLLISION_VIDEO_INFO;
        msg.obj = isAuto;
        mMainHandler.sendMessage(msg);
    }

    private int trackSize;
    private void addTrack(int signal) {
        if (Configuration.PROJECT_NAME.equals(Configuration.KD003)) {
            long time = System.currentTimeMillis();
            LogUtils.getInstance().d(TAG,"addTrack time = "+time);
            if (isReadyUpload) {
               if (tracks != null && tracks.size() < 30) {
                   if (trackSize < 15 ) {
                       if (tracks.size() < (trackSize+15)) {
                           AMapLocation location = locationManagerTool.getAMapLocation();
                           TrackBean trackBean = new TrackBean();
                           if (location != null) {
                               trackBean.setLat(location.getLatitude());
                               trackBean.setLng(location.getLongitude());
                           }
                           trackBean.setTime(time);
                           trackBean.setSignal(signal);
                           tracks.add(trackBean);
                       }
                   } else {
                       AMapLocation location = locationManagerTool.getAMapLocation();
                       TrackBean trackBean = new TrackBean();
                       if (location != null) {
                           trackBean.setLat(location.getLatitude());
                           trackBean.setLng(location.getLongitude());
                       }
                       trackBean.setTime(time);
                       trackBean.setSignal(signal);
                       tracks.add(trackBean);
                   }
               }
            } else {
                if (tracks != null) {
                    if (tracks.size() >= 15 ) {
                        tracks.remove(0);
                    }
                    AMapLocation location = locationManagerTool.getAMapLocation();
                    TrackBean trackBean = new TrackBean();
                    if (location != null) {
                        trackBean.setLat(location.getLatitude());
                        trackBean.setLng(location.getLongitude());
                    }
                    trackBean.setTime(time);
                    trackBean.setSignal(signal);
                    tracks.add(trackBean);
                }
            }
        }
    }

    public List<TrackBean> getTracks() {
        return tracks;
    }


    private void sendToGaoDe() {
        Intent intent = new Intent();
        intent.setAction("AUTONAVI_STANDARD_BROADCAST_RECV");
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setPackage("com.autonavi.amapautolite");
        intent.putExtra("KEY_TYPE", 12116);
        intent.putExtra("cameraDisplay", "1280x720");
        intent.putExtra("productModel", "SC08");
        intent.putExtra("cameraName", "C2395");
        intent.putExtra("productName", "SC08");
        intent.putExtra("cameraConnect", "mipi");
        intent.putExtra("apkName", "com.bx.carDVR");
        intent.putExtra("serviceAction", "com.bx.carDVR.action.gdarcameraservice");
        sendBroadcast(intent);
        LogUtils.getInstance().d(TAG, "sendToGaoDe: ");
    }

    public void acquireScreenWakeLock(){
        if (mWakeManager !=null) {
            mWakeManager.acquireScreenWakeLock();
        }
    }

    public void releaseScreenWakeLock(){
        if (mWakeManager !=null) {
            mWakeManager.releaseScreenWakeLock();
        }
    }

    private void startActivityForPackage(String packName) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packName);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
