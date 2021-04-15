
package com.bx.carDVR.adas;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bx.carDVR.R;
import com.bx.carDVR.manager.ShareBufferManager;
import com.bx.carDVR.prefs.SettingsManager;
import com.bx.carDVR.ui.AutoFitSurfaceView;
import com.bx.carDVR.ui.FloatPreviewWindow;
import com.bx.carDVR.ui.FloatPreviewWindowCallback;
import com.bx.carDVR.ui.FloatToast;
import com.bx.carDVR.util.LogUtils;
import com.calmcar.adas.apiserver.AdasConf;
import com.calmcar.adas.apiserver.AdasServer;
import com.calmcar.adas.apiserver.model.CdwDetectInfo;
import com.calmcar.adas.apiserver.model.FrontCarInfo;
import com.calmcar.adas.apiserver.model.JavaCameraFrame;
import com.calmcar.adas.apiserver.model.LdwDetectInfo;
import com.calmcar.adas.apiserver.out.ActiveSuccessListener;
import com.calmcar.adas.apiserver.out.DetectInitSuccessListener;
import com.calmcar.adas.apiserver.view.AdasDrawViewPro;
import com.calmcar.adas.gps.LocationTickListener;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class TTAdasManager extends BaseAdas implements
        ShareBufferManager.YUVDataCallback, AdasServer.CameraDataProcessCallBack,IAdas, View.OnClickListener {

    private static final String TAG = "TTAdasManager";
    private static final boolean DEBUG = false;
    private static TTAdasManager adasManager;
    private Context mContext;
    private View mRootView;
    private WarnSpeakManager warnSpeakerManager;
    private AdasServer adasServer;
    private AdasDrawViewPro adasDrawView;
    private AutoFitSurfaceView frontPreview;
    private AutoFitSurfaceView backPreview;
    // 校准
    private float mScaleWidth, mScaleHeight;
    private LinearLayout ln_up_down, ln_left_right;
    private View v_up_down, v_left_right;
    private Button btn_set_line_ok;
    private int lastX, lastY;
    private int desLastX, desLastY;
    private int deadStartX;
    private int deadEndX;
    private RelativeLayout center_conf_rela;
    private int deadStartY;
    private int deadEndY;
    private int mPreViewWidth = 902;
    private int mPewViewHeight = 320;
    private Mat mFrameChain;
    protected JavaCameraFrame mCameraFrame;
    public static final int PREVIEW_WIDTH = 1280;
    public static final int PREVIEW_HEIGHT = 720;

    private boolean isRegisterAdas = false;
    private ConnectivityManager mConnectivityManager;

    private static final int MSG_NEW_ADAS_INITED = 1;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_NEW_ADAS_INITED:
                    adasInit();
                    break;
                default:
                    break;
            }
        }
    };

    private TTAdasManager() {
    }

    public static TTAdasManager getInstance() {
        if (adasManager == null) {
            synchronized (TTAdasManager.class) {
                if (adasManager == null) {
                    adasManager = new TTAdasManager();
                }
            }
        }
        return adasManager;
    }

    @Override
    public void init(FloatPreviewWindowCallback callback, Context mContext, View view,
                     ConcreteAdasFactory.AdasActivatedCallback adasActivatedCallback) {
        mAdasActivatedCallback = adasActivatedCallback;
        mFloatPreviewWindowCallback = callback;
        this.mContext = mContext;
        this.mRootView = view;
        mConnectivityManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        initView(view);
        initData();
    }

    private void adasInit() {
        LogUtils.getInstance().d(TAG, "adasInit() getNewAdasEnable() = "+getNewAdasEnable()+"  ,getNewAdasActiveState() = "+getAdasActiveState());
        if(getNewAdasEnable() && getAdasActiveState() ) {
            mFloatPreviewWindowCallback.setAdasOnImage();
            ShareBufferManager.getInstance().addShareBufferRequest(ShareBufferManager.FRONT_CAMERA_ID, this, TAG);
        }else {
            mFloatPreviewWindowCallback.setAdasOffImage();
            ShareBufferManager.getInstance().removeShareBufferRequest(ShareBufferManager.FRONT_CAMERA_ID, this);
        }
    }

    private void handAdas() {
        LogUtils.getInstance().d(TAG, "handNewAdas() adasEnable = "+getNewAdasEnable()+"  ,activeState = "+getAdasActiveState());
        if(getNewAdasEnable()) {
            if(getAdasActiveState()) {
                if (adasDrawView != null) {
                    adasDrawView.setVisibility(View.GONE);
                }
                if(center_conf_rela != null)center_conf_rela.setVisibility(View.GONE);
                FloatPreviewWindow.getInstance().setAdasOffImage();
                SettingsManager.getInstance().setAdasEnable(false);
                ShareBufferManager.getInstance().removeShareBufferRequest(ShareBufferManager.FRONT_CAMERA_ID, this);
            }else {
                if (isNetworkConnected()) {
                    FloatToast.makeText(R.string.adas_not_inited, FloatToast.LENGTH_SHORT).show();
                } else {
                    FloatToast.makeText(R.string.please_activate_online, FloatToast.LENGTH_SHORT).show();
                }
            }
        }else {
            if (getAdasActiveState()) {
                warnSpeakerManager.startSuccess();
                if (adasDrawView != null) {
                    adasDrawView.setVisibility(View.VISIBLE);
                }
                FloatPreviewWindow.getInstance().setAdasOnImage();
                SettingsManager.getInstance().setAdasEnable(true);
                ShareBufferManager.getInstance().addShareBufferRequest(ShareBufferManager.FRONT_CAMERA_ID, this, TAG);
            } else {
                if (isNetworkConnected()) {
                    FloatToast.makeText(R.string.adas_not_inited, FloatToast.LENGTH_SHORT).show();
                } else {
                    FloatToast.makeText(R.string.please_activate_online, FloatToast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void startAdas() {
        LogUtils.getInstance().d(TAG, "startAdas()");
        if(getNewAdasEnable() && getAdasActiveState()) {
            if(adasDrawView != null){
                adasDrawView.setVisibility(View.VISIBLE);
            }
            ShareBufferManager.getInstance().addShareBufferRequest(ShareBufferManager.FRONT_CAMERA_ID, this, TAG);
        }
    }

    private void stopAdas() {
        LogUtils.getInstance().d(TAG, "stopAdas()");
        SettingsManager.getInstance().setAdasEnable(false);
        FloatPreviewWindow.getInstance().setAdasOffImage();
        if(adasDrawView != null) {
            adasDrawView.setVisibility(View.GONE);
        }
        if(center_conf_rela != null) {
            center_conf_rela.setVisibility(View.GONE);
        }
        ShareBufferManager.getInstance().removeShareBufferRequest(ShareBufferManager.FRONT_CAMERA_ID, this);
    }

    public void registerAdas() {
        LogUtils.getInstance().d(TAG, "registerAdas() isRegisterAdas = "+isRegisterAdas+"  ,adasEnable = "+SettingsManager.getInstance().isAdasEnable());
        if (!isRegisterAdas && SettingsManager.getInstance().isAdasEnable()) {
            if (isActive()) {
                if(adasDrawView != null)adasDrawView.setVisibility(View.VISIBLE);
                FloatPreviewWindow.getInstance().setAdasOnImage();
            }
            isRegisterAdas = true;
        }
    }

    private void unregisterAdas() {
        LogUtils.getInstance().d(TAG, "unregisterAdas() isRegisterAdas = "+ isRegisterAdas);
        if (isRegisterAdas) {
            FloatPreviewWindow.getInstance().setAdasOffImage();
            if(adasDrawView != null)adasDrawView.setVisibility(View.GONE);
            if(center_conf_rela != null)center_conf_rela.setVisibility(View.GONE);
            isRegisterAdas = false;
        }
    }

    private void initView(View view) {
        if (view == null)return;
        LogUtils.getInstance().d(TAG, "initView");
        adasDrawView = view.findViewById(R.id.adasDrawView);
        frontPreview = (AutoFitSurfaceView) view.findViewById(R.id.front_preview);
        center_conf_rela = (RelativeLayout) view.findViewById(R.id.center_conf_rela);
        btn_set_line_ok = (Button) view.findViewById(R.id.id_btn_set_line_ok);
        ln_up_down = (LinearLayout) view.findViewById(R.id.id_lin_up_down);
        ln_left_right = (LinearLayout) view.findViewById(R.id.id_lin_left_right);
        v_up_down = view.findViewById(R.id.id_view_up_down);
        v_left_right = view.findViewById(R.id.id_view_left_right);
    }

    private void initData() {
        LogUtils.getInstance().d(TAG, "initData");
        AdasConf.IN_FRAME_WIDTH = 1280;
        AdasConf.IN_FRAME_HEIGHT = 720;
        adasServer = new AdasServer(mContext);
        warnSpeakerManager = new WarnSpeakManager(mContext);
        adasServer.setDetectInitSuccessListener(new DetectInitSuccessListener() {
            @Override
            public void onInitSuccess() {
                LogUtils.getInstance().d(TAG, "onInitSuccess()");
            }
        });
        adasServer.setActiveSuccessListener(new ActiveSuccessListener() {
            @Override
            public void onActiveCallBack(String type) {
                String type0 = type + "";
                // type :1 首次激活成功 10 已经激活 2 未经授权，激活失败 3 网络错误，激活失败
                LogUtils.getInstance().d(TAG, "onActiveCallBack() type0 = " + type0);
                if ("1".equals(type) || "10".equals(type)) {
                    mRootView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mAdasActivatedCallback != null) {
                                mAdasActivatedCallback.callback(true);
                            }
                            playSound();
                            registerAdas();
                        }
                    });
                    ShareBufferManager.getInstance().addShareBufferRequest(ShareBufferManager.FRONT_CAMERA_ID, TTAdasManager.this, TAG);
                } else {
                    mRootView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mAdasActivatedCallback != null) {
                                mAdasActivatedCallback.callback(false);
                            }
                        }
                    });
                }
                mHandler.sendEmptyMessage(MSG_NEW_ADAS_INITED);
            }
        });
        adasServer.setCameraDataProcessCallBack(this);
        adasServer.initConf(1280, 720);
        // 设置室内测试模式0,室外1
        adasServer.setAdasServerRunMode(1);
        adasServer.setLaneWarnSensitivity(0.74f);

        warnSpeakerManager.initVideoPlayers(R.raw.car_out_line, R.raw.front_car_warn_level_one
                , R.raw.front_car_warn_level_two, R.raw.front_car_launch, R.raw.adas_start_warn);

        adasServer.startServer(locationTickListener);
        boolean isActive = adasServer.getActiveState();// true 已激活

        // 设置预警速度 默认速度为30
        adasServer.setLaneDetectRate(30);
        adasServer.setAdasServerModuleState(true, 0);
        adasServer.setFrontCarWarnSencitivity(3);

        mFrameChain = new Mat(PREVIEW_HEIGHT + (PREVIEW_HEIGHT/2), PREVIEW_WIDTH, CvType.CV_8UC1);
        mCameraFrame= new JavaCameraFrame(mFrameChain, PREVIEW_WIDTH, PREVIEW_HEIGHT);

    }

    @Override
    public void reInit(ConcreteAdasFactory.AdasActivatedCallback adasActivatedCallback) {
        mAdasActivatedCallback = adasActivatedCallback;
    }

    public boolean getAdasActiveState() {
        if(adasServer != null) {
            return adasServer.getActiveState();
        }
        return false;
    }

    private boolean getNewAdasEnable() {
        return SettingsManager.getInstance().isAdasEnable();
    }

    private LocationTickListener locationTickListener = new LocationTickListener() {
        @Override
        public void onTickArrive(final double longitude, final double latitude, final double rate) {
            //if(DEBUG)LogUtils.getInstance().I(TAG, "onT ickArrive longitude = " + longitude + " , latitude = " +latitude + " ,rate = " + rate);
        }
    };

    @Override
    public void onProcessBack(LdwDetectInfo ldwDetectInfo, CdwDetectInfo cdwDetectInfo) {
        // TODO Auto-generated method stub
        try {
            checkAdasWarnInfo(ldwDetectInfo, cdwDetectInfo);
            if (adasDrawView != null) {
                adasDrawView.drawBitmap(ldwDetectInfo, cdwDetectInfo);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void checkAdasWarnInfo(LdwDetectInfo ldwDetectInfo, CdwDetectInfo cdwDetectInfo) {
        if (ldwDetectInfo != null) {
            if(DEBUG)LogUtils.getInstance().d(TAG,"ldwDetectInfo.getDetectState() = "+ldwDetectInfo.getDetectState());
            if (ldwDetectInfo.getDetectState() == 2 || ldwDetectInfo.getDetectState() == 3) {
                warnSpeakerManager.carOutLine();
            }
        }
        if (cdwDetectInfo != null) {
            FrontCarInfo frontCarInfo = cdwDetectInfo.getFrontCarInfo();
            if (frontCarInfo != null) {
                if(DEBUG)LogUtils.getInstance().d(TAG, "checkAdasWarnInfo FrontCarStateType = " + frontCarInfo.getFrontCarStateType());
                switch (frontCarInfo.getFrontCarStateType()) {
                    case 1:
                        warnSpeakerManager.frontCarSafeDistance();
                        break;
                    case 2:
                        warnSpeakerManager.frontCarCrash();
                        break;
                    case 3:
                        warnSpeakerManager.frontCarLaunchWarn();
                        break;
                }
            }
        }
    }

    // 手动校准
    private void adjustAdas() {
        if(!getAdasActiveState())return;
        if(frontPreview != null && frontPreview.getMeasuredWidth() != 0) {
            mPreViewWidth = frontPreview.getMeasuredWidth();
            mPewViewHeight = frontPreview.getMeasuredHeight();
        }
        int width = mPreViewWidth;
        int height = mPewViewHeight;
        LogUtils.getInstance().d(TAG, "adjustAdas() width = "+width+" ,height = "+height);
        mScaleWidth = ((float) width) / AdasConf.IN_FRAME_WIDTH;
        mScaleHeight = ((float) height) / AdasConf.IN_FRAME_HEIGHT;

        float desWidth = AdasConf.IN_FRAME_WIDTH * mScaleWidth;
        float desHeight = AdasConf.IN_FRAME_HEIGHT * mScaleHeight;

        deadStartX = 150;
        deadEndX = (int) (desWidth - 150);

        deadStartY = 100;
        deadEndY = (int) (desHeight - 100);

        RelativeLayout.LayoutParams relaParams = (RelativeLayout.LayoutParams) center_conf_rela.getLayoutParams(); // 取控件textView当前的布局参数
        // linearParams.height
        relaParams.width = (int) desWidth;// 控件的宽强制设成30
        relaParams.height = (int) desHeight;// 控件的宽强制设成30

        center_conf_rela.setLayoutParams(relaParams); // 使设置好的布局参数应用到控件
        center_conf_rela.setVisibility(View.VISIBLE);
        int[] oldVPPara = adasServer.getVPPara();
        if (oldVPPara[0] > 0) {
            desLastX = (int) (oldVPPara[0] * mScaleWidth);
            desLastY = (int) (oldVPPara[1] * mScaleHeight);
            ViewGroup.MarginLayoutParams margin = new ViewGroup.MarginLayoutParams(ln_left_right.getLayoutParams());
            margin.setMargins(desLastX - margin.width / 2, 0, (int) (desWidth - desLastX) - margin.width / 2, 0);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(margin);
            ln_left_right.setLayoutParams(layoutParams);

            ViewGroup.MarginLayoutParams margin1 = new ViewGroup.MarginLayoutParams(ln_up_down.getLayoutParams());
            margin1.setMargins(0, desLastY - margin1.height / 2, 0, (int) (desHeight - desLastY) - margin1.height / 2);
            RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(margin1);
            ln_up_down.setLayoutParams(layoutParams1);

        } else {
            desLastX = (int) desWidth / 2;
            desLastY = (int) desHeight / 2;

            ViewGroup.MarginLayoutParams margin = new ViewGroup.MarginLayoutParams(ln_left_right.getLayoutParams());
            margin.setMargins((int) (desWidth / 2) - margin.width / 2, 0, ((int) (desWidth / 2) - margin.width / 2), 0);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(margin);
            ln_left_right.setLayoutParams(layoutParams);
            ViewGroup.MarginLayoutParams margin1 = new ViewGroup.MarginLayoutParams(ln_up_down.getLayoutParams());
            margin1.setMargins(0, (int) (desHeight / 2) - margin1.height / 2, 0,
                    (int) (desHeight / 2) - margin1.height / 2);
            RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(margin1);
            ln_up_down.setLayoutParams(layoutParams1);
        }

        setGestureListener();// 手势监听
        btn_set_line_ok.setOnClickListener(this);
        // 计算平移距离
    }

    private void setGestureListener() {
        ln_left_right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v_left_right.setBackgroundColor(Color.parseColor("#0000FF"));
                        // 按下屏幕的操作
                        lastX = (int) event.getRawX();// 获取触摸事件触摸位置的原始X坐标
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // 在屏幕上移动的操作
                        int dx = (int) event.getRawX() - lastX;
                        int l = v.getLeft() + dx;
                        int b = v.getBottom();
                        int r = v.getRight() + dx;
                        int t = v.getTop();

                        if (l < deadStartX) {
                            l = deadStartX;
                            r = deadStartX + v.getWidth();
                        }

                        if (r > deadEndX) {
                            r = deadEndX;
                            l = deadEndX - v.getWidth();
                        }

                        v.layout(l, t, r, b);
                        lastX = (int) event.getRawX();// 获取触摸事件触摸位置的原始X坐标
                        desLastX = v.getLeft() + 20;
                        v.postInvalidate();
                        // btn_set_line_ok.setVisibility(View.VISIBLE);
                        break;
                    case MotionEvent.ACTION_UP:
                        v_left_right.setBackgroundColor(Color.parseColor("#00FF00"));
                        lastX = (int) event.getRawX();// 获取触摸事件触摸位置的原始X坐标
                        desLastX = v.getLeft() + 20;
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        // 手势撤消的操作
                        // 一般认为不能由用户主动触发。
                        // 系统在运行到一定程度下无法继续响应你的后续动作时会产生此事件
                        break;
                    default:
                        break;
                }
                // 这个返回值如果是false的话，那么它只会接受到第一个ACTION_DOWN的效果，
                // 后面的它认为没有触发，所以要想继续监听后续事件，需要返回值为true
                return true;
            }
        });

        ln_up_down.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v_up_down.setBackgroundColor(Color.parseColor("#0000FF"));
                        // 按下屏幕的操作
                        lastY = (int) event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // 在屏幕上移动的操作
                        int dy = (int) event.getRawY() - lastY;
                        int l = v.getLeft();
                        int b = v.getBottom() + dy;
                        int r = v.getRight();
                        int t = v.getTop() + dy;

                        if (t < deadStartY) {
                            t = deadStartY;
                            b = deadStartY + v.getHeight();
                        }
                        if (b > deadEndY) {
                            b = deadEndY;
                            t = deadEndY - v.getHeight();
                        }
                        v.layout(l, t, r, b);
                        lastY = (int) event.getRawY();
                        desLastY = v.getTop() + 20;
                        v.postInvalidate();
                        break;
                    case MotionEvent.ACTION_UP:
                        v_up_down.setBackgroundColor(Color.parseColor("#00FF00"));
                        lastY = (int) event.getRawY();
                        desLastY = v.getTop() + 20;
                        // 离开屏幕的操作
                        break;
                    case (MotionEvent.ACTION_CANCEL):
                        // 手势撤消的操作
                        // 一般认为不能由用户主动触发。
                        // 系统在运行到一定程度下无法继续响应你的后续动作时会产生此事件
                        break;
                    default:
                        break;
                }
                // 这个返回值如果是false的话，那么它只会接受到第一个ACTION_DOWN的效果，
                // 后面的它认为没有触发，所以要想继续监听后续事件，需要返回值为true
                return true;
            }
        });
    }
    // 手动校准-----------------------------------------------end

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.id_btn_set_line_ok:
                float fcX = (desLastX) / mScaleWidth;
                float fcY = (desLastY) / mScaleHeight;
                if (getNewAdasEnable() && getAdasActiveState()) {
                    adasServer.setVPPara((int) fcX, (int) fcY);
                }
                center_conf_rela.setVisibility(View.GONE);
                break;

            default:
                break;
        }
    }

    private void releaseTianTongAdas() {
        try {
            if (adasServer != null)
                adasServer.serverStop();
            if (warnSpeakerManager != null)
                warnSpeakerManager.stop();
            ShareBufferManager.getInstance().removeShareBufferRequest(ShareBufferManager.FRONT_CAMERA_ID, this);
            if(adasDrawView != null)adasDrawView.setVisibility(View.GONE);
            if(center_conf_rela != null)center_conf_rela.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processData(byte[] data, int cameraId) {
        // TODO Auto-generated method stub
        try {
            if (data == null || mFrameChain == null || mCameraFrame == null) {
                Log.w(TAG, "processData data = " + data + "; mFrameChain = " + mFrameChain + ";mCameraFrame = " + mCameraFrame);
                return;
            }
            if(DEBUG)Log.w(TAG,"processData data.length : " + data.length);
            if(data != null && data.length == 1382400) {
                mFrameChain.put(0, 0, data);
                adasServer.processDataAsyn(mCameraFrame);
            }
//            if (CameraService.SUPPORT_FRONT_CAMERA_1080P) {
//                if (data != null) {
//                    byte[] yuv420p = new byte[1280*720*3/2];
//                    StreamProcessManager.compressYUV(data, 1920, 1080, yuv420p, 1280, 720, 0, 0, false);
//                    byte[] nv21Data = new byte[1280*720*3/2];
//                    StreamProcessManager.yuvI420ToNV21(yuv420p, nv21Data, 1280, 720);
//                    mFrameChain.put(0, 0, nv21Data);
//                    adasServer.processDataAsyn(mCameraFrame);
//                }
//            } else {
//                if(data != null && data.length == 1382400) {
//                    mFrameChain.put(0, 0, data);
//                    adasServer.processDataAsyn(mCameraFrame);
//                }
//            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    private boolean isNetworkConnected() {
        NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (mNetworkInfo != null) {
            return mNetworkInfo.isAvailable();
        }
        return false;
    }

    //--IAdas start--

    @Override
    public void setEnable(boolean value) {
        SettingsManager.getInstance().setAdasEnable(value);
    }

    @Override
    public boolean isEnable() {
        return getNewAdasEnable();
    }

    @Override
    public boolean isActive() {
        return getAdasActiveState();
    }

    @Override
    public void register() {
        registerAdas();
    }

    @Override
    public void unRegister() {
        unregisterAdas();
    }

    @Override
    public void check() {
        adjustAdas();
    }

    @Override
    public void start() {
        startAdas();
    }

    @Override
    public void stop() {
        stopAdas();
    }

    @Override
    public void handClick() {
        handAdas();
    }

    @Override
    public void activeInterface() {

    }

    @Override
    public void loadAudioResource() {

    }

    @Override
    public void release() {
        releaseTianTongAdas();
        mContext = null;
        mRootView = null;
    }

    private void playSound() {
        warnSpeakerManager.startSuccess();
    }

}

