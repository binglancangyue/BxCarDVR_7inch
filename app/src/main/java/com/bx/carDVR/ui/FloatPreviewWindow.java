package com.bx.carDVR.ui;

import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bx.carDVR.CameraInstance;
import com.bx.carDVR.DvrService;
import com.bx.carDVR.MainActivity;
import com.bx.carDVR.R;
import com.bx.carDVR.adas.ConcreteAdasFactory;
import com.bx.carDVR.app.DvrApplication;
import com.bx.carDVR.bean.Configuration;
import com.bx.carDVR.manager.BxSoundManager;
import com.bx.carDVR.manager.ECarOverseaManager;
import com.bx.carDVR.manager.ShareBufferManager;
import com.bx.carDVR.prefs.SettingsManager;
import com.bx.carDVR.util.GpioManager;
import com.bx.carDVR.util.LocationManagerTool;
import com.bx.carDVR.util.LogUtils;
import com.bx.carDVR.util.StorageUtils;

import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import androidx.annotation.NonNull;

public class FloatPreviewWindow implements CameraInstance.CameraStateChangeListener, View.OnClickListener,
        DvrService.RecordStateChangeListener, LocationManagerTool.OnGpsChangeListener, GpioManager.GpioStateChangeListener,
        ShareBufferManager.TakePictureCallback ,FloatPreviewWindowCallback{

    private static final String TAG = "BxFloatPreviewWindow";

    private static FloatPreviewWindow floatPreviewWindow;
    private DvrService mDvrService;
    private Application application;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mParams;
    private LayoutInflater inflater;
    private View rootView;

    private ContentResolver contentResolver;

    private static final int MINI_WIDTH = 1;
    private static final int MINI_HEIGHT = 1;
    private static final int WINDOW_STATE_NONE = 0;
    private static final int WINDOW_STATE_FIRST_LAUNCH = 1;
    private static final int WINDOW_STATE_MINI = 2;
    public static final int WINDOW_STATE_NORMAL = 3;
    private static final int WINDOW_STATE_FULL_FRONT = 4;
    private static final int WINDOW_STATE_FULL_BACK = 5;
    private static final int WINDOW_STATE_FULL_REVERSE = 6;
    public static final int WINDOW_STATE_EXCEPT_STATUS_NAVIGATION_BAR = 7;
    public static final int WINDOW_STATE_FULL_BACK_PHOTO = 8;
    public static final int WINDOW_STATE_FULL_FRONT_BACK = 9;
    public static final int WINDOW_STATE_SPLIT = 10;

    public static final int REQUEST_WINDOW_NONE = 100;
    public static final int REQUEST_WINDOW_FIRST_LAUNCH = 101;
    public static final int REQUEST_WINDOW_MINI = 102;
    public static final int REQUEST_WINDOW_NORMAL = 103;
    public static final int REQUEST_WINDOW_FULL_FRONT = 104;
    public static final int REQUEST_WINDOW_FULL_BACK = 105;
    public static final int REQUEST_WINDOW_FULL_REVERSE = 106;
    public static final int REQUEST_WINDOW_EXCEPT_STATUS_NAVIGATION_BAR = 107;
    public static final int REQUEST_WINDOW_FULL_BACK_PHOTO = 108;
    public static final int REQUEST_WINDOW_FULL_FRONT_BACK = 109;
    public static final int REQUEST_WINDOW_SPLIT = 111;

    public static final String ACTION_TXZ_SHOW = "com.txznet.txz.record.show";
    public static final String ACTION_TXZ_DISMISS = "com.txznet.txz.record.dismiss";
    public static final String ACTION_SETTINGS_WINDOW = "com.android.systemui.settings_window_state";

    private static final int MSG_SHOW_FULL_REVERSE = 10;

    private static final int MSG_START_RECORD_BY_MUTE = 1000;
    private static final int MSG_MUTE_STATUS_FROM_EXTERNAL = 1001;

    private static final int MSG_SHOW_SOFT_KEYBOARD = 1002;
    private static final int MSG_HIDE_SOFT_KEYBOARD = 1003;
    private static final int MSG_ADJUST_SPLIT_STATUS = 1004;

    private int mWindowState;
    private int mWindowStateWhenReverse = -1;
    private int beforeSoftState = -1;
    private int mPreviousWindowState;
    private static int mScreenWidth = 1024;
    private static int mScreenHeight = 600;
    private static int mAppWidth = 1024;
    private static int mAppHeight = 530;
    private static int mNavigationBarWidth;
    private static int mNavigationBarHeight = 50;
    private static int mStatusBarHeight = 20;

    private LinearLayout ll_camera_operation;
    private FrameLayout layout_preview;
    private FrameLayout fl_float_preview;
    private static AutoFitSurfaceView frontPreview, backPreview;
    private LinearLayout recording_disp;
    private TextView recording_time;
    private ImageView btn_lock,btn_take_picture,btn_record,btn_adas,btn_microphone,btn_settings,btn_upload,btn_back_preview,btn_back,btn_test_qrcode;
    private ImageView iv_reverse;
    private TextView tv_speed,tv_location;

    private boolean isFrontPreviewInFront = true;

    private String speedStr= "";
    private String locationStr = "";

    DecimalFormat locationDF = new DecimalFormat("0.0000");

    private SimpleDateFormat recordFormatter = new SimpleDateFormat("mm:ss");

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_START_RECORD_BY_MUTE:
                    if (null != mDvrService) {
                        mDvrService.startRecord();
                    }
                    break;
                case MSG_MUTE_STATUS_FROM_EXTERNAL:
                    handMuteBySystemUI();
                    break;
                case MSG_SHOW_FULL_REVERSE:
                    handleShowReverseFullPreview();
                    break;
                case MSG_SHOW_SOFT_KEYBOARD:
                    beforeSoftState = mWindowState;
                    requestWindowState(REQUEST_WINDOW_MINI, DvrService.CAMERA_FRONT_ID);
                    break;
                case MSG_HIDE_SOFT_KEYBOARD:
                    if (beforeSoftState == WINDOW_STATE_SPLIT) {
                        requestWindowState(REQUEST_WINDOW_SPLIT, DvrService.CAMERA_FRONT_ID);
                    }
                    break;
                case MSG_ADJUST_SPLIT_STATUS:
                    if (mWindowState == WINDOW_STATE_SPLIT) {
                        requestWindowState(REQUEST_WINDOW_MINI,DvrService.CAMERA_FRONT_ID);
                    } else if (mWindowState == WINDOW_STATE_NORMAL) {
                        mDvrService.sendBroadcast(new Intent(MainActivity.ACTION_CLOSE_ACTIVITY_INNER));
                        requestWindowState(REQUEST_WINDOW_SPLIT,DvrService.CAMERA_FRONT_ID);
                    } else {
                        requestWindowState(REQUEST_WINDOW_SPLIT,DvrService.CAMERA_FRONT_ID);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private FloatPreviewWindow() {
        application = DvrApplication.getDvrApplication();
        mDvrService = DvrService.getInstance();
        mWindowManager = (WindowManager) application.getSystemService(Context.WINDOW_SERVICE);
        contentResolver = application.getContentResolver();
        mParams = new WindowManager.LayoutParams();
        inflater = LayoutInflater.from(application);
        initSizeInfo();
    }

    public static FloatPreviewWindow getInstance(){
        if (null == floatPreviewWindow) {
            floatPreviewWindow = new FloatPreviewWindow();
        }
        return floatPreviewWindow;
    }

    private void initSizeInfo() {
        Point displaySize = new Point();
        application.getDisplay().getRealSize(displaySize);
        mScreenWidth = displaySize.x;
        mScreenHeight = displaySize.y;
        mNavigationBarWidth = application.getResources().getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_width);
        mNavigationBarHeight = application.getResources().getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_height);
        LogUtils.getInstance().d(TAG,"initSizeInfo mScreenWidth = "+mScreenWidth+" , mScreenHeight = "+mScreenHeight+" , mNavigationBarWidth = "
                +mNavigationBarWidth+" , mNavigationBarHeight = "+mNavigationBarHeight);
        if (Configuration.IS_3IN ) {
            mAppHeight = 360;
            mAppWidth = mScreenWidth;
        } else if (Configuration.IS_966) {
            mAppHeight = mScreenHeight;
            mAppWidth = mScreenWidth - 2*mNavigationBarWidth;
        } else if (Configuration.IS_7IN) {
            mAppHeight = mScreenHeight - mNavigationBarHeight;
            mAppWidth = mScreenWidth;
        } else if (Configuration.IS_439) {
            mAppHeight = mScreenHeight;
            mAppWidth = mScreenWidth;
        }

    }

    public void onCreate() {
        LogUtils.getInstance().d(TAG,"onCreate()");
        if (Configuration.PROJECT_NAME.equals(Configuration.KD003)) {
            rootView = inflater.inflate(R.layout.layout_float_preview_kd003,null);
        } else if (Configuration.PROJECT_NAME.equals(Configuration.T10)) {
            rootView = inflater.inflate(R.layout.layout_float_preview_t10,null);
        } else {
            rootView = inflater.inflate(R.layout.layout_float_preview,null);
        }
        initView();
        addView();
        GpioManager.getInstance().addGpioStateChangeListener(this);
        ShareBufferManager.getInstance().setTakePictureCallbackCallbackListener(this);
        registerWindowReceiver();
        registerClipVideoStatus();

        rootView.post(new Runnable() {
            @Override
            public void run() {
                startInitAdas();
            }
        });
    }

    public void onDestroy() {
        unregisterWindowReceiver();
        unregisterClipVideoStatus();
    }

    private void addView() {
        mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        // mParams.format = PixelFormat.RGBA_8888;
        mParams.format = PixelFormat.TRANSLUCENT;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        mParams.gravity = Gravity.TOP | Gravity.RIGHT;
        mParams.x = 0;
        mParams.y = 0;
        mParams.width = MINI_WIDTH;
        mParams.height = MINI_HEIGHT;
        mParams.windowAnimations = 0;//com.android.internal.R.style.Animation_Toast;//R.style.AnimationPreviewWindow;
        mWindowManager.addView(rootView,mParams);
        mWindowState = WINDOW_STATE_NONE;
    }

    private void initView() {
        rootView.post(new Runnable() {
            @Override
            public void run() {
                initSizeInfo();
            }
        });
        ll_camera_operation = rootView.findViewById(R.id.ll_camera_operation);
        layout_preview = rootView.findViewById(R.id.layout_preview);
        fl_float_preview = rootView.findViewById(R.id.fl_float_preview);
        frontPreview = rootView.findViewById(R.id.front_preview);
        backPreview = rootView.findViewById(R.id.back_preview);
        recording_disp = rootView.findViewById(R.id.recording_disp);
        recording_time = rootView.findViewById(R.id.recording_time);
        btn_take_picture = rootView.findViewById(R.id.btn_take_picture);
        btn_record = rootView.findViewById(R.id.btn_record);
        btn_adas = rootView.findViewById(R.id.btn_adas);
        btn_microphone = rootView.findViewById(R.id.btn_microphone);
        btn_settings = rootView.findViewById(R.id.btn_settings);
        btn_lock = rootView.findViewById(R.id.btn_lock);
        iv_reverse = rootView.findViewById(R.id.iv_reverse);
        //btn_test_qrcode = rootView.findViewById(R.id.btn_test_qrcode);
        frontPreview.bindCameraId(DvrService.CAMERA_FRONT_ID);
        backPreview.bindCameraId(DvrService.CAMERA_BACK_ID);
        frontPreview.setScaleX(-1);
        tv_speed = rootView.findViewById(R.id.tv_speed);
        tv_location = rootView.findViewById(R.id.tv_location);
        if (TextUtils.isEmpty(speedStr)) {
            tv_speed.setText(R.string.default_speed_string);
        } else {
            tv_speed.setText(speedStr);
        }
        if (TextUtils.isEmpty(locationStr)) {
            tv_location.setText(R.string.default_location_string);
        } else {
            tv_location.setText(locationStr);
        }
        if (Configuration.ONLY_BACK_CAMERA) {
            btn_adas.setVisibility(View.GONE);
        }
        LocationManagerTool.getInstance().setOnGpsChangeListener(this);

//        if (Configuration.PROJECT_NAME.equals(Configuration.KD003)) {
//            btn_upload = rootView.findViewById(R.id.btn_upload_video);
//            btn_back_preview = rootView.findViewById(R.id.btn_back_preview);
//            btn_back = rootView.findViewById(R.id.btn_finish);
//            tv_speed = rootView.findViewById(R.id.tv_speed);
//            tv_location = rootView.findViewById(R.id.tv_location);
//            btn_upload.setOnClickListener(this);
//            btn_back_preview.setOnClickListener(this);
//            btn_back.setOnClickListener(this);
//            LocationManagerTool.getInstance().setOnGpsChangeListener(this);
//            if (TextUtils.isEmpty(speedStr)) {
//                tv_speed.setText(R.string.default_speed_string);
//            } else {
//                tv_speed.setText(speedStr);
//            }
//            if (TextUtils.isEmpty(locationStr)) {
//                tv_location.setText(R.string.default_location_string);
//            } else {
//                tv_location.setText(locationStr);
//            }
//        }
//        if (Configuration.PROJECT_NAME.equals(Configuration.T10)) {
//            btn_back = rootView.findViewById(R.id.btn_finish);
//            btn_back.setOnClickListener(this);
//        }
        if (!Configuration.PROJECT_NAME.equals(Configuration.KD003) && !Configuration.ONLY_BACK_CAMERA) {
            frontPreview.setOnClickListener(this);
            backPreview.setOnClickListener(this);
        }
        btn_record.setOnClickListener(this);
        btn_take_picture.setOnClickListener(this);
        btn_settings.setOnClickListener(this);
        btn_lock.setOnClickListener(this);
        btn_adas.setOnClickListener(this);
        //btn_shrink.setOnClickListener(this);
        btn_adas.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ConcreteAdasFactory.getInstance().check();
                return true;
            }
        });
        //btn_test_qrcode.setOnClickListener(this);
        if (Configuration.PROJECT_NAME.equals(Configuration.KD003)) {
            if (SettingsManager.getInstance().isMute()) {
                btn_microphone.setVisibility(View.VISIBLE);
            } else {
                btn_microphone.setVisibility(View.GONE);
            }
        } else {
            btn_microphone.setSelected(!SettingsManager.getInstance().isMute());
            btn_microphone.setOnClickListener(this);
        }
    }

    public void requestWindowState(int requestWindowState, String mCameraId) {
        LogUtils.getInstance().d(TAG, "requestWindowState() requestWindowState = " + requestWindowState + ";mCameraId = " + mCameraId
                + ";mWindowState = " + mWindowState);
        if (requestWindowState == REQUEST_WINDOW_MINI && mWindowState != WINDOW_STATE_MINI) {
            unregisterAdas();
            setExpandWidth(0);
            mWindowState = WINDOW_STATE_MINI;
            ll_camera_operation.setVisibility(View.GONE);
            mParams.x = -1;
            mParams.y = -1;
            mParams.width = MINI_WIDTH;
            mParams.height = MINI_HEIGHT;
            mParams.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            mWindowManager.updateViewLayout(rootView, mParams);
        } else if (requestWindowState == REQUEST_WINDOW_FIRST_LAUNCH && mWindowState != WINDOW_STATE_FIRST_LAUNCH) {
            mWindowState = WINDOW_STATE_FIRST_LAUNCH;
            setExpandWidth(0);
            registerCameraStateListener();
            //openCameraIfNeed();
            mParams.width = MINI_WIDTH;
            mParams.height = MINI_HEIGHT;
            mParams.x = -1;
            mParams.y = -1;
            mParams.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            mWindowManager.updateViewLayout(rootView, mParams);
        } else if (requestWindowState == REQUEST_WINDOW_NORMAL && mWindowState != WINDOW_STATE_NORMAL) {
            mWindowState = WINDOW_STATE_NORMAL;
            btn_settings.setVisibility(View.VISIBLE);
            backPreview.setClickable(true);
            //btn_shrink.setVisibility(View.GONE);
            if (Configuration.IS_SUPPORT_ADAS) {
                requestImageViewLayout(btn_lock, 70, 70);
                requestImageViewLayout(btn_take_picture, 70, 70);
                requestImageViewLayout(btn_record, 70, 70);
                requestImageViewLayout(btn_adas, 70, 70);
                requestImageViewLayout(btn_microphone, 70, 70);
                btn_settings.setVisibility(View.VISIBLE);
                //btn_shrink.setVisibility(View.GONE);
            }
            setExpandWidth(0);
            ll_camera_operation.setVisibility(View.VISIBLE);
            mParams.x = 0;
            mParams.y = 0;
            mParams.width = mAppWidth;
            mParams.height = mAppHeight;
            mParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            if (Configuration.IS_966) {
                mParams.x = mNavigationBarWidth;
            }
            mWindowManager.updateViewLayout(rootView, mParams);
//            mCameraId = DvrService.CAMERA_BACK_ID;
            if (DvrService.CAMERA_BACK_ID.equals(mCameraId)) {
                if (Configuration.PROJECT_NAME.equals(Configuration.KD003)) {
                    setKD003ViewStatus(false);
                }
                requestFrontPreviewLayout(MINI_WIDTH, MINI_HEIGHT);
                requestBackPreviewLayout(mAppWidth, mAppHeight);
                layout_preview.bringChildToFront(backPreview);
                isFrontPreviewInFront = false;
            } else {
                requestFrontPreviewLayout(mAppWidth, mAppHeight);
                requestBackPreviewLayout(MINI_WIDTH, MINI_HEIGHT);
                layout_preview.bringChildToFront(frontPreview);
                isFrontPreviewInFront = true;
                if (Configuration.IS_SUPPORT_ADAS) {
                    LogUtils.getInstance().d(TAG,"registAdasType");
                    btn_adas.setVisibility(View.VISIBLE);
                    registerAdas();
                    if (ConcreteAdasFactory.getInstance().isAdasEnable()
                            && ConcreteAdasFactory.getInstance().isAdasActive()) {
                        setAdasOnImage();
                    } else {
                        setAdasOffImage();
                    }
                } else {
                    btn_adas.setVisibility(View.GONE);
                }
            }
        } else if (requestWindowState == REQUEST_WINDOW_FULL_REVERSE && mWindowState != WINDOW_STATE_FULL_REVERSE) {
            mWindowState = WINDOW_STATE_FULL_REVERSE;
            setExpandWidth(0);
            awakenDreams();
            if (GpioManager.getInstance().isReversing()) {
                mHandler.sendEmptyMessage(MSG_SHOW_FULL_REVERSE);
            }
        } else if (requestWindowState == REQUEST_WINDOW_SPLIT && mWindowState != WINDOW_STATE_SPLIT) {
            mWindowState = WINDOW_STATE_SPLIT;
            setExpandWidth(450);
            backPreview.setClickable(true);
            ll_camera_operation.setVisibility(View.VISIBLE);
            btn_settings.setVisibility(View.GONE);
            //btn_shrink.setVisibility(View.VISIBLE);
            requestImageViewLayout(btn_lock, 60, 60);
            requestImageViewLayout(btn_take_picture, 60, 60);
            requestImageViewLayout(btn_record, 60, 60);
            requestImageViewLayout(btn_adas, 60, 60);
            requestImageViewLayout(btn_microphone, 60, 60);
            mParams.x = 0;
            mParams.y = 25;
            mParams.width = 450;
            mParams.height = mAppHeight-25;
            mParams.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            mWindowManager.updateViewLayout(rootView, mParams);
            if (DvrService.CAMERA_BACK_ID.equals(mCameraId)) {
                requestFrontPreviewLayout(MINI_WIDTH, MINI_HEIGHT);
                requestBackPreviewLayout(450, mAppHeight-25);
                layout_preview.bringChildToFront(backPreview);
                isFrontPreviewInFront = false;
            } else {
                if (isFrontPreviewInFront) {
                    requestFrontPreviewLayout(450, mAppHeight-25);
                    requestBackPreviewLayout(MINI_WIDTH, MINI_HEIGHT);
                    layout_preview.bringChildToFront(frontPreview);
                    isFrontPreviewInFront = true;
                } else {
                    requestFrontPreviewLayout(MINI_WIDTH, MINI_HEIGHT);
                    requestBackPreviewLayout(450, mAppHeight-25);
                    layout_preview.bringChildToFront(backPreview);
                }
            }
        }
    }

    private void openCameraIfNeed(){
        LogUtils.getInstance().d(TAG, "openCameraIfNeed()");
        if (mDvrService != null) {
            mDvrService.openCamera();
        }
    }

    private void registerCameraStateListener() {
        if (null != mDvrService) {
            mDvrService.setCameraStateChangeListener(DvrService.CAMERA_FRONT_ID,this);
            mDvrService.setCameraStateChangeListener(DvrService.CAMERA_BACK_ID,this);
            mDvrService.setRecordStateChangeListener(this);
        }
    }

    public static Surface getFrontSurface() {
        return frontPreview.getSurface();
    }

    public static Surface getBackSurface() {
        return backPreview.getSurface();
    }

    @Override
    public void onCameraOpened(String mCameraId) {
        LogUtils.getInstance().d(TAG, "onCameraOpened mCameraId = " + mCameraId);
        if (DvrService.CAMERA_FRONT_ID.equals(mCameraId)) {
            if (null != mDvrService) {
                mDvrService.startPreview(DvrService.CAMERA_FRONT_ID);
            }
        } else if (DvrService.CAMERA_BACK_ID.equals(mCameraId)) {
            if (null != mDvrService && GpioManager.getInstance().isAhdPluginIn()) {
                mDvrService.startPreview(DvrService.CAMERA_BACK_ID);
            }
        }
    }

    @Override
    public void onCameraClosed(String mCameraId) {

    }

    @Override
    public void onRecordStarted(String mCameraId) {
        LogUtils.getInstance().d(TAG,"onRecordStarted mCameraId = " + mCameraId);
        if (mDvrService.isAllRecordOpened()) {
            if (null != recording_disp) {
                recording_disp.setVisibility(View.VISIBLE);
            }
            if (null != btn_record) {
                btn_record.setClickable(true);
                btn_record.setSelected(true);
            }
            if(mDvrService.isLocked(DvrService.CAMERA_FRONT_ID)){
                btn_lock.setImageResource(R.drawable.lock_on);
            }else{
                btn_lock.setImageResource(R.drawable.lock_off);
            }
        }
    }

    @Override
    public void onPreviewStarted(String mCameraId) {
        LogUtils.getInstance().d(TAG,"onPreviewStarted mCameraId = "+mCameraId);
		if (DvrService.CAMERA_FRONT_ID.equals(mCameraId)) {
			if (null != mDvrService) {
				if (isSdcardEnable()) {
                    mDvrService.startRecord(mDvrService.CAMERA_FRONT_ID);
				}
			}
		} else if (mDvrService.CAMERA_BACK_ID.equals(mCameraId)) {
			if (null != mDvrService && GpioManager.getInstance().isAhdPluginIn()) {
				if (isSdcardEnable()) {
                    //mDvrService.startRecord(mDvrService.CAMERA_BACK_ID);
                    mDvrService.startRecord();
				}
			}
		}
    }

    @Override
    public void onSwitchNextFile(String mCameraId) {
        LogUtils.getInstance().d(TAG, "onSwitchNextFile mCameraId = " + mCameraId);
        if (mCameraId.equals(DvrService.CAMERA_FRONT_ID)) {
            if (null != btn_lock) {
                btn_lock.setImageResource(R.drawable.lock_off);
            }
        }
    }

    @Override
    public void onRecordStartFailed(String mCameraId, int reson) {
        mDvrService.saveCameraRecordStatus(false);
        if (null != recording_disp) {
            recording_disp.setVisibility(View.GONE);
        }
        if (null != btn_record) {
            btn_record.setClickable(true);
            btn_record.setSelected(false);
        }
    }

    @Override
    public void onRecordStoped(String mCameraId) {
        LogUtils.getInstance().d(TAG, "onRecordStopped mCameraId = " + mCameraId);
        if (mDvrService.isAllRecordClosed()) {
            if (null != recording_disp) {
                recording_disp.setVisibility(View.GONE);
            }
            if (null != btn_record) {
                btn_record.setClickable(true);
                btn_record.setSelected(false);
            }
            if (null != btn_lock) {
                btn_lock.setImageResource(R.drawable.lock_off);
            }
        }
    }

    @Override
    public void onTakePictureCallback(String mCameraId, String pictureName) {
         LogUtils.getInstance().d(TAG,"pictureName = "+pictureName);
         btn_take_picture.setClickable(true);
         if (DvrService.CAMERA_FRONT_ID.equals(mCameraId) && !TextUtils.isEmpty(pictureName)) {
             FloatToast.makeText(R.string.photo_saved,FloatToast.LENGTH_SHORT).show();
         }
         if (DvrService.CAMERA_FRONT_ID.equals(mCameraId) && TextUtils.isEmpty(pictureName)) {
             FloatToast.makeText(R.string.take_pic_fail,FloatToast.LENGTH_SHORT).show();
         }
    }

    @Override
    public void onClick(View view) {
        if(ActivityManager.isUserAMonkey()){
            return;
        }
        LogUtils.getInstance().d(TAG,"onClick id = " + view.getId());
        switch (view.getId()){
            case R.id.front_preview:
                handFrontPreviewClick();
                break;
            case R.id.back_preview:
                handBackPreviewClick();
                break;
            case R.id.btn_record:
                handRecord();
                break;
            case R.id.btn_take_picture:
                handTakePictures();
                break;
            case R.id.btn_settings:
                sendBroadcastForHideNavigationBar();
                //ECarOverseaManager.getInstance().showECarQR();
                break;
            case R.id.btn_microphone:
                handMute();
                break;
            case R.id.btn_upload_video:
                if (mDvrService != null){
                    mDvrService.sendUploadVideo(false);
                }
                break;
            case R.id.btn_back_preview:
                if (isFrontPreviewInFront) {
                    handFrontPreviewClick();
                    setKD003ViewStatus(false);
                }
                break;
            case R.id.btn_finish:
                if (Configuration.PROJECT_NAME.equals(Configuration.KD003)) {
                    if (isFrontPreviewInFront) {
                        requestWindowState(REQUEST_WINDOW_MINI, "0");
                        mDvrService.sendBroadcast(new Intent(MainActivity.ACTION_CLOSE_ACTIVITY_INNER));
                    } else {
                        setKD003ViewStatus(true);
                        handBackPreviewClick();
                    }
                } else if (Configuration.PROJECT_NAME.equals(Configuration.T10)) {
                    requestWindowState(REQUEST_WINDOW_MINI, "0");
                    mDvrService.sendBroadcast(new Intent(MainActivity.ACTION_CLOSE_ACTIVITY_INNER));
                }
                break;
            case R.id.btn_lock:
                handLock();
                break;
//            case R.id.btn_test_qrcode:
//                ECarOverseaManager.getInstance().showECarQR();
//                break;
            case R.id.btn_adas:
                ConcreteAdasFactory.getInstance().handClick();
                break;

            default:
                break;
        }
    }

    private void handRecord() {
        if (isSdcardEnable()) {
            if (null != mDvrService) {
                mHandler.removeMessages(MSG_START_RECORD_BY_MUTE);
                boolean isRecording = mDvrService.isCameraRecording();
                LogUtils.getInstance().d(TAG, "handRecord() isRecording = " + isRecording);
                btn_record.setClickable(false);
                if (isRecording) {
                    if (Configuration.PROJECT_NAME.equals(Configuration.KD003) && mDvrService.isClipVideo()) {
                        FloatToast.makeText(R.string.dvr_video_locking,FloatToast.LENGTH_SHORT).show();
                        return;
                    }
                    mDvrService.stopRecord();
                } else {
                    mDvrService.startRecord();
                }
            }
        }else{
            StorageUtils.getInstance().checkUnSupportSdCard();
            FloatToast.makeText(R.string.card_not_exist,FloatToast.LENGTH_SHORT).show();
        }
    }

    private void handTakePictures() {
        if (isSdcardEnable()) {
//            if (null != mDvrService) {
//                if (mDvrService.isAllRecordOpened()) {
//                    LogUtils.getInstance().d(TAG,"handTakePictures()");
//                    //btn_take_picture.setClickable(false);
//                    mDvrService.takePictures();
//                    mDvrService.playSound(BxSoundManager.TYPE_TAKE_PICTURE);
//                } else {
//                    FloatToast.makeText(R.string.take_pic_before_record,FloatToast.LENGTH_SHORT).show();
//                }
//            }
            btn_take_picture.setClickable(false);
            LogUtils.getInstance().d(TAG,"handTakePictures()");
            ShareBufferManager.getInstance().takePicture();
            mDvrService.playSound(BxSoundManager.TYPE_TAKE_PICTURE);
        } else {
            FloatToast.makeText(R.string.card_not_exist,FloatToast.LENGTH_SHORT).show();
        }
    }

    public void handFrontPreviewClick() {
        LogUtils.getInstance().d(TAG, "handFrontPreviewClick()");
        //TODO judge back camera
        if (GpioManager.getInstance().isAhdPluginIn()) {
            if (mWindowState == WINDOW_STATE_NORMAL) {
                if (Configuration.IS_SUPPORT_ADAS) {
                    unregisterAdas();
                    btn_adas.setVisibility(View.GONE);
                }
                requestFrontPreviewLayout(MINI_WIDTH, MINI_HEIGHT);
                requestBackPreviewLayout(mAppWidth, mAppHeight);
                layout_preview.bringChildToFront(backPreview);
                isFrontPreviewInFront = false;
            } else if (mWindowState == WINDOW_STATE_SPLIT) {
                if (Configuration.IS_SUPPORT_ADAS) {
                    unregisterAdas();
                    btn_adas.setVisibility(View.GONE);
                }
                requestFrontPreviewLayout(MINI_WIDTH, MINI_HEIGHT);
                requestBackPreviewLayout(450, mAppHeight-25);
                layout_preview.bringChildToFront(backPreview);
                isFrontPreviewInFront = false;
            }
        } else {
            FloatToast.makeText(R.string.ahd_hint,FloatToast.LENGTH_SHORT).show();
        }
    }

    public void handBackPreviewClick() {
        LogUtils.getInstance().d(TAG, "handBackPreviewClick()");
        if (mWindowState == WINDOW_STATE_NORMAL) {
            if(Configuration.IS_SUPPORT_ADAS) {
                btn_adas.setVisibility(View.VISIBLE);
                registerAdas();
            }
            requestFrontPreviewLayout(mAppWidth, mAppHeight);
            requestBackPreviewLayout(MINI_WIDTH, MINI_HEIGHT);
            layout_preview.bringChildToFront(frontPreview);
            isFrontPreviewInFront = true;
        } else if (mWindowState == WINDOW_STATE_SPLIT) {
            if(Configuration.IS_SUPPORT_ADAS) {
                btn_adas.setVisibility(View.VISIBLE);
                registerAdas();
            }
            requestFrontPreviewLayout(450, mAppHeight-25);
            requestBackPreviewLayout(MINI_WIDTH, MINI_HEIGHT);
            layout_preview.bringChildToFront(frontPreview);
            isFrontPreviewInFront = true;
        }

    }

    private void handleShowReverseFullPreview() {
        ll_camera_operation.setVisibility(View.GONE);
        iv_reverse.setVisibility(View.VISIBLE);
        unregisterAdas();
        backPreview.setClickable(false);
        mParams.width = mScreenWidth;
        mParams.height = mScreenHeight;
        mParams.x = 0;
        mParams.y = 0;
        mParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        mWindowManager.updateViewLayout(rootView, mParams);
        requestFrontPreviewLayout(MINI_WIDTH, MINI_HEIGHT);
        requestBackPreviewLayout(mScreenWidth, mScreenHeight);
        layout_preview.bringChildToFront(backPreview);
        isFrontPreviewInFront = false;
    }

    private void handBackOnReverse() {
        if (null != mDvrService) {
            iv_reverse.setVisibility(View.GONE);
            if (mWindowStateWhenReverse == WINDOW_STATE_MINI) {
                requestWindowState(REQUEST_WINDOW_MINI,DvrService.CAMERA_FRONT_ID);
            } else if (mWindowStateWhenReverse == WINDOW_STATE_NORMAL) {
                requestWindowState(REQUEST_WINDOW_NORMAL,DvrService.CAMERA_FRONT_ID);
            } else if (mWindowStateWhenReverse == WINDOW_STATE_SPLIT) {
                requestWindowState(REQUEST_WINDOW_SPLIT,DvrService.CAMERA_FRONT_ID);
            }
        }
    }

    private void handLock() {
        if (isSdcardEnable()) {
            if (null != mDvrService) {
                mHandler.removeMessages(MSG_START_RECORD_BY_MUTE);
                mDvrService.lockVideo(true);
                boolean isRecording = mDvrService.isCameraRecording();
                if (!isRecording) {
                    btn_record.setClickable(false);
                    mDvrService.startRecord();
                } else {
                    btn_lock.setImageResource(R.drawable.lock_on);
                }
            }
        }else{
            FloatToast.makeText(R.string.card_not_exist,FloatToast.LENGTH_SHORT).show();
            //StorageUtils.getInstance().checkUnSupportSdCard();
        }
    }

    private void handMute() {
        if (mDvrService == null) {
            return;
        }
        LogUtils.getInstance().d(TAG, "handMute() isAllRecordOpened = " + mDvrService.isAllRecordOpened());
        boolean isMute = !SettingsManager.getInstance().isMute();
        btn_microphone.setSelected(!isMute);
        SettingsManager.getInstance().setMute(isMute);

        if (mDvrService.isAllRecordOpened()) {
            if (mDvrService.isCameraRecording()) {
                mDvrService.stopRecord();
                mHandler.sendEmptyMessageDelayed(MSG_START_RECORD_BY_MUTE, 3000);
            }
        }
    }

    private void handMuteBySystemUI() {
        if (mDvrService == null) {
            return;
        }
        if (mDvrService.isClipVideo()) {
            return;
        }
        LogUtils.getInstance().d(TAG, "handMuteBySystemUI() isAllRecordOpened = " + mDvrService.isAllRecordOpened());
        boolean isMute = SettingsManager.getInstance().isMute();
        btn_microphone.setVisibility(isMute?View.GONE:View.VISIBLE);
        SettingsManager.getInstance().setMute(!isMute);

        if (mDvrService.isAllRecordOpened()) {
            if (mDvrService.isCameraRecording()) {
                mDvrService.stopRecord();
                mHandler.sendEmptyMessageDelayed(MSG_START_RECORD_BY_MUTE, 3000);
            }
        }
    }

    private void requestFrontPreviewLayout(int requestWidth, int requestHeight) {
        LogUtils.getInstance().d(TAG,"requestFrontPreviewLayout requestWidth = "+requestWidth+",requestHeight = "+requestHeight);
        frontPreview.requestSize(requestWidth, requestHeight);
        FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) frontPreview
                .getLayoutParams();
        params.width = requestWidth;
        params.height = requestHeight;
        frontPreview.setLayoutParams(params);
        frontPreview.requestLayout();
    }

    private void requestBackPreviewLayout(int requestWidth, int requestHeight) {
        LogUtils.getInstance().d(TAG,"requestBackPreviewLayout requestWidth = "+requestWidth+",requestHeight = "+requestHeight);
        backPreview.requestSize(requestWidth, requestHeight);
        FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) backPreview.getLayoutParams();
        params.width = requestWidth;
        params.height = requestHeight;
        params.leftMargin = 0;
        backPreview.setLayoutParams(params);
        backPreview.requestLayout();
    }

    private boolean isSdcardEnable() {
        if (StorageUtils.getInstance().isSDCardMounted()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onRecordTimeUpdate(long time) {
        String displayTime = recordFormatter.format(time);
        //LogUtils.getInstance().d(TAG, "displayTime == "+displayTime);
        if (null != recording_time) {
            recording_time.setText(displayTime);
        }
    }

    @Override
    public void onGsonsorLocked() {
        LogUtils.getInstance().d(TAG, "onGsonsorLocked()");
        //FloatToast.makeText(R.string.dvr_video_locking, FloatToast.LENGTH_SHORT).show();
        handLock();
    }

    private void sendBroadcastForHideNavigationBar() {
        Intent intent = new Intent(Configuration.ACTION_SHOW_SETTING_WINDOW);
        intent.putExtra("isHideNavigationBar", true);
        if (mDvrService != null) {
            mDvrService.sendBroadcast(intent);
        }
    }

    private void registerWindowReceiver() {
        IntentFilter filterWindow = new IntentFilter();
        filterWindow.addAction(ACTION_TXZ_SHOW);
        filterWindow.addAction(ACTION_TXZ_DISMISS);
        if (Configuration.PROJECT_NAME.equals(Configuration.KD003)) {
            filterWindow.addAction(Configuration.ACTION_UPLOAD);
        }
        //filterWindow.addAction(Configuration.ACTION_SETTINGS_WINDOW);
        filterWindow.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filterWindow.addAction(Configuration.ACTION_DISMISS_SPLIT_WINDOW);
        if (Configuration.IS_SUPPORT_SPLIT) {
            filterWindow.addAction("bx.action.hide.soft_keyboard");
            filterWindow.addAction("bx.action.show.soft_keyboard");
            filterWindow.addAction(Configuration.ACTION_OPEN_DVR_CAMERA);
        }
        if (mDvrService != null) {
            mDvrService.registerReceiver(WindowStateReceiver,filterWindow);
        }

    }

    private void unregisterWindowReceiver() {
        if (mDvrService != null) {
            mDvrService.unregisterReceiver(WindowStateReceiver);
        }
    }

    private BroadcastReceiver WindowStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,"WindowStateReceiver action = "+action);
            if (ACTION_TXZ_SHOW.equals(action)) {
                if (mWindowState == WINDOW_STATE_NORMAL) {
                    mPreviousWindowState = WINDOW_STATE_NORMAL;
                    requestWindowState(REQUEST_WINDOW_MINI,"0");
                }
            } else if (ACTION_TXZ_DISMISS.equals(action)) {
                if (mPreviousWindowState == WINDOW_STATE_NORMAL&&mWindowState == WINDOW_STATE_MINI) {
                    mPreviousWindowState = WINDOW_STATE_MINI;
                    requestWindowState(REQUEST_WINDOW_NORMAL,"0");
                }
            } else if (ACTION_SETTINGS_WINDOW.equals(action)) {
                boolean isShow = intent.getBooleanExtra("window_state",false);
                Log.d(TAG,"isShowWindowSettings = "+isShow);
                if (isShow && mWindowState == WINDOW_STATE_NORMAL) {
                    mPreviousWindowState = WINDOW_STATE_NORMAL;
                    requestWindowState(REQUEST_WINDOW_MINI,"0");
                }else if (mPreviousWindowState == WINDOW_STATE_NORMAL&&mWindowState == WINDOW_STATE_MINI) {
                    mPreviousWindowState = WINDOW_STATE_MINI;
                    requestWindowState(REQUEST_WINDOW_NORMAL,"0");
                }
            } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                if (TextUtils.equals(intent.getStringExtra("reason"), "homekey")) {
                    if (Configuration.IS_SUPPORT_SPLIT) {
                        requestWindowState(REQUEST_WINDOW_SPLIT, "0");
                    } else if (mWindowState == WINDOW_STATE_NORMAL) {
                        mDvrService.sendBroadcast(new Intent(MainActivity.ACTION_CLOSE_ACTIVITY_INNER));
                        requestWindowState(REQUEST_WINDOW_MINI, "0");
                    }
                } else {
                    if (mWindowState == WINDOW_STATE_NORMAL) {
                        mDvrService.sendBroadcast(new Intent(MainActivity.ACTION_CLOSE_ACTIVITY_INNER));
                        requestWindowState(REQUEST_WINDOW_MINI, "0");
                    }
                }
            } else if (Configuration.ACTION_UPLOAD.equals(action)) {
                mHandler.removeMessages(MSG_MUTE_STATUS_FROM_EXTERNAL);
                mHandler.sendEmptyMessage(MSG_MUTE_STATUS_FROM_EXTERNAL);
            } else if (Configuration.ACTION_DISMISS_SPLIT_WINDOW.equals(action)) {
                requestWindowState(REQUEST_WINDOW_MINI, "0");
            } else if ("bx.action.show.soft_keyboard".equals(action)) {
                mHandler.sendEmptyMessage(MSG_SHOW_SOFT_KEYBOARD);
            } else if ("bx.action.hide.soft_keyboard".equals(action)) {
                mHandler.sendEmptyMessage(MSG_HIDE_SOFT_KEYBOARD);
            } else if (Configuration.ACTION_OPEN_DVR_CAMERA.equals(action)) {
                mHandler.sendEmptyMessage(MSG_ADJUST_SPLIT_STATUS);
            }
        }
    };

    private void setKD003ViewStatus(boolean visible) {
        if (visible) {
            btn_upload.setVisibility(View.VISIBLE);
            btn_take_picture.setVisibility(View.VISIBLE);
            btn_record.setVisibility(View.VISIBLE);
            btn_back_preview.setVisibility(View.VISIBLE);
        } else {
            btn_upload.setVisibility(View.INVISIBLE);
            btn_take_picture.setVisibility(View.INVISIBLE);
            btn_record.setVisibility(View.INVISIBLE);
            btn_back_preview.setVisibility(View.INVISIBLE);
        }
    }

    private void registerClipVideoStatus() {
        contentResolver.registerContentObserver(Settings.Global.getUriFor(Configuration.CLIP_VIDEO_STATUS), false, contentObserver);
    }

    private void unregisterClipVideoStatus() {
        contentResolver.unregisterContentObserver(contentObserver);
    }

    private boolean isClipVideo = false;

    private ContentObserver contentObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (mDvrService != null) {
                if (mDvrService.isClipVideo()) {
                    isClipVideo = true;
                    btn_upload.setClickable(false);
                    btn_upload.setSelected(true);
                } else {
                    if (isClipVideo) {
                        isClipVideo = false;
                        FloatToast.makeText(R.string.file_saved,FloatToast.LENGTH_SHORT).show();
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mDvrService.getTracks().clear();
                            }
                        },5000);
                    }
                    btn_upload.setClickable(true);
                    btn_upload.setSelected(false);
                }
            }
        }
    };


    @Override
    public void onSpeedChange(int speed) {
        if (Configuration.DEBUG) {
            LogUtils.getInstance().d(TAG,"onSpeedChange speed = "+speed);
        }
        if (speed >= 0) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    speedStr = String.format("%d km/h", speed);
                    SystemProperties.set("persist.sys.globalspeed",speedStr);
                    if (tv_speed != null) {
                        tv_speed.setText(speedStr);
                    }
                }
            });
        }
    }

    @Override
    public void onLocationChange(double lat, double lng) {
        if (Configuration.DEBUG) {
            LogUtils.getInstance().d(TAG,"onLocationChange : "+lat+" , "+lng);
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                locationStr = String.format("E:%s N:%s", locationDF.format(lng), locationDF.format(lat));
                SystemProperties.set("persist.sys.globalgps",locationStr);
                if (tv_location != null) {
                    tv_location.setText(locationStr);
                }
            }
        });
    }

    @Override
    public void onAhdStateChanged(boolean isPlugin) {
        if (!isPlugin) {
            handBackPreviewClick();
        }
    }

    @Override
    public void onReverseStateChanged(boolean isTurnOn) {
        LogUtils.getInstance().d(TAG, "onReverseStateChanged() isTurnOn = " + isTurnOn);
        if (Configuration.IS_SUPPORT_REVERSE) {
            if (null != mDvrService) {
                if (isTurnOn) {
                    if (GpioManager.getInstance().isAhdPluginIn()) {
                        Intent intent = new Intent(Configuration.ACTION_REVERSE);
                        intent.putExtra("status",true);
                        mDvrService.sendBroadcast(intent);
                        mDvrService.acquireScreenWakeLock();
                        markWindowState();
                        requestWindowState(REQUEST_WINDOW_FULL_REVERSE,DvrService.CAMERA_BACK_ID);
                    } else {
                        FloatToast.makeText(R.string.ahd_hint,FloatToast.LENGTH_SHORT).show();
                    }
                } else {
                    Intent intent = new Intent(Configuration.ACTION_REVERSE);
                    intent.putExtra("status",false);
                    mDvrService.sendBroadcast(intent);
                    mDvrService.releaseScreenWakeLock();
                    if (mWindowState == WINDOW_STATE_FULL_REVERSE) {
                        handBackOnReverse();
                    }
                }
            }
        }
    }

    @Override
    public void tackPicture(int cameraId,String pictureName) {
        btn_take_picture.setClickable(true);
        if (cameraId == 0 && !TextUtils.isEmpty(pictureName)) {
            FloatToast.makeText(R.string.photo_saved,FloatToast.LENGTH_SHORT).show();
        }
    }

    private void markWindowState(){
        mWindowStateWhenReverse = mWindowState;
    }

    private IDreamManager dreamManager;

    private void awakenDreams() {
        if (dreamManager == null) {
            dreamManager = IDreamManager.Stub.asInterface(ServiceManager.checkService(DreamService.DREAM_SERVICE));
        }
        if (dreamManager != null) {
            try {
                if (dreamManager.isDreaming()) {
                    dreamManager.awaken();
                }
            } catch (RemoteException e) {
                // fine, stay asleep then
            }
        }
    }

    public synchronized void startInitAdas(){
        LogUtils.getInstance().d(TAG, "onCreate() adas IS_SUPPORT_ADAS = " + Configuration.IS_SUPPORT_ADAS );
        if(Configuration.IS_SUPPORT_ADAS) {
            if (ConcreteAdasFactory.getInstance().isInited()) {
                ConcreteAdasFactory.getInstance().build();
            } else {
                ConcreteAdasFactory.getInstance().setInited(true);
                ConcreteAdasFactory.getInstance().onCreate(application, this, rootView);
                ConcreteAdasFactory.getInstance().build();
            }
        }
    }

    @Override
    public boolean isWindowsStatusReady() {
        Log.w(TAG, "mWindowState = " + mWindowState + ";isFrontPreviewInFront=" + isFrontPreviewInFront);
        return mWindowState == WINDOW_STATE_NORMAL && isFrontPreviewInFront;
    }

    @Override
    public boolean isCpuHighLevel() {
        return false;
    }

    @Override
    public void setAdasOffImage() {
        if (btn_adas != null) {
            btn_adas.setImageResource(R.drawable.icon_dvr_adas_close);
        }
    }

    @Override
    public void setAdasOnImage() {
        if (btn_adas != null) {
            btn_adas.setImageResource(R.drawable.icon_dvr_adas_open);
        }
    }

    private void unregisterAdas() {
        ConcreteAdasFactory.getInstance().unRegister();
    }

    private void registerAdas() {
        ConcreteAdasFactory.getInstance().register();
    }

    private void requestImageViewLayout(ImageView iv,int requestWidth, int requestHeight) {
        LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams ) iv
                .getLayoutParams();
        params.width = requestWidth;
        params.height = requestHeight;
        iv.setLayoutParams(params);
        iv.requestLayout();
    }

    private void setExpandWidth(int width) {
        if (width == 0) {
            if (SystemProperties.getInt("persist.t7t9.expand_width",0) > 0) {
                SystemProperties.set("persist.t7t9.expand_width",String.valueOf(width));
                Intent intent = new Intent("com.bx.action.preview.mini");
                intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                mDvrService.sendBroadcast(intent);
                LogUtils.getInstance().d(TAG,"com.bx.action.preview.mini");
            }
        } else {
            if (SystemProperties.getInt("persist.t7t9.expand_width",0) == 0) {
                SystemProperties.set("persist.t7t9.expand_width",String.valueOf(width));
                Intent intent = new Intent("com.bx.action.preview.split");
                intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                mDvrService.sendBroadcast(intent);
                LogUtils.getInstance().d(TAG,"com.bx.action.preview.split");
            }
        }
    }

}
