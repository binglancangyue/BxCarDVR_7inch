package com.bx.carDVR;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;

import com.autonavi.amapauto.gdarcameraservice.model.ArCameraOpenResultParam;
import com.bx.carDVR.bean.CmdPara;
import com.bx.carDVR.bean.Configuration;
import com.bx.carDVR.manager.NotifyMessageManager;
import com.bx.carDVR.manager.ShareBufferManager;
import com.bx.carDVR.ui.FloatPreviewWindow;
import com.bx.carDVR.util.AvcEncoder;
import com.bx.carDVR.util.ClipVideoUtil;
import com.bx.carDVR.util.H264Encoder;
import com.bx.carDVR.util.ImageUtil;
import com.bx.carDVR.util.LogUtils;
import com.bx.carDVR.util.StorageUtils;
import com.bx.carDVR.util.ThreadPoolUtil;
import com.mapgoo.mapgooipc.MapgooIPC;
import com.riemannlee.liveproject.StreamProcessManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class CameraInstance {

    private static final String TAG = "BxCameraInstance";
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private DvrService dvrService;
    private CameraManager cameraManager;
    private CameraRecorder cameraRecorder;
    private String cameraId;
    private Handler cameraHandler;
    private HandlerThread mThreadHandler;
    private HandlerThread liveThreadHandler;
    private HandlerThread cameraHandlerThread;
    private Handler operateHandler;
    private Handler liveHandler;
    private Handler mainHandler;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CaptureRequest mPreviewRequest;
    private Surface mPreviewSurface = null;
    private List<Surface> mSurfaceList = new ArrayList<>();
    private CameraCaptureSession mSession;
    private int mPreviewWidth = 1280;
    private int mPreviewHeight = 720;
    private boolean isPlugin = true;
    private boolean lockVideoFlag = false;
    private boolean lockClipVideoFlag = false;
    private long mLockTime;
    private static final int OPENING = 0;
    public static final int OPENED = 1;
    private static final int CLOSING = 2;
    public static final int CLOSED = 3;
    private static final int ERROR = 4;
    private static final int REOPENING = 5;
    private int mCameraStatus = CLOSED;
    private int mPreviewStatus = CLOSED;
    private int mRecordStatus = CLOSED;
    private int mRestartSession = CLOSED;

    private ImageReader mImageReader;
    private ImageReader maiguImageReader;

    private CaptureRequest mTakingPicRequest;

    private ClipVideoUtil mClipVideoUtil;
    private boolean isStartAR = false;

    private H264Encoder mH264Encoder;

    public static final int MSG_OPEN_CAMERA = 1;
    public static final int MSG_CLOSE_CAMERA = 2;
    public static final int MSG_START_PREVIEW = 3;
    public static final int MSG_STOP_PREVIEW = 4;
    public static final int MSG_START_RECORD = 5;
    public static final int MSG_STOP_RECORD = 6;
    public static final int MSG_TAKE_PICTURE = 7;
    public static final int MSG_SWITCH_NEXT_FILE = 8;

    public static final int MSG_OPEN_CAMERA_CB = 100;
    public static final int MSG_RECORD_STARTED_CB = 101;
    public static final int MSG_RECORD_START_FAILED_CB = 102;
    public static final int MSG_RECORD_STOPPED_CB = 103;
    public static final int MSG_SWITCH_NEXT_FILE_CB = 104;
    public static final int MSG_TAKE_PICTURE_CB = 105;
    public static final int MSG_PREVIEW_STARTED_CB = 106;
    public static final int MSG_CLOSE_CAMERA_CB = 107;
    private static final int MSG_REBOOT = 108;

    private static final int MSG_TAKE_PICTURE_IN_THREAD = 1001;
    private static final int MSG_MOVE_VIDEO_FILE_IN_THREAD = 1002;
    private static final int MSG_SWITCH_NEXT_FILE_IN_THREAD = 1003;
    private static final int MSG_STOP_PREVIEW_IN_THREAD = 1004;
    private static final int MSG_CLOSE_CAMERA_IN_THREAD = 1005;
    private static final int MSG_SAVE_VIDEO_INFO = 1006;
    private static final int MSG_REOPEN_CAMERA_IN_THREAD = 1007;
    private static final int MSG_CREATE_SESSION_FOR_LIVE_STATUS = 1008;
    private static final int MSG_CLIP_VIDEO_IN_THREAD = 1009;

    private static final int MSG_SEND_AR_NAVI_DATA = 201;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private ArCameraOpenResultParam arCameraOpenResultParam;

    public CameraInstance(DvrService dvrService,CameraManager cameraManager,String cameraId) {
        this.dvrService = dvrService;
        this.cameraManager = cameraManager;
        this.cameraId = cameraId;
        initData();
        mClipVideoUtil = new ClipVideoUtil(dvrService,cameraId);
    }

    private void initData() {
        logD("initData");
        cameraHandlerThread = new HandlerThread(String.format("CameraThread_%s", cameraId));
        cameraHandlerThread.start();
        cameraHandler = new Handler(cameraHandlerThread.getLooper());
        mThreadHandler = new HandlerThread(String.format("OperateThread_%s", cameraId));
        mThreadHandler.start();
        operateHandler = new CameraOperateHandler(mThreadHandler.getLooper());
        liveThreadHandler = new HandlerThread(String.format("LiveThread_%s", cameraId));
        liveThreadHandler.start();
        liveHandler = new LiveHandler(liveThreadHandler.getLooper());
        mainHandler = new MainHandler();
        //setUpCameraOutputs();
    }

    private boolean isFirst = true;
    public void setCameraPluginStatus(boolean isPlugin) {
        logD("setCameraPluginStatus isPlugin : "+isPlugin);
        this.isPlugin = isPlugin;
        if (isPlugin) {
            if (isFirst) {
                isFirst = false;
                if (mCameraStatus == OPENED) {
                    startPreview();
                } else {
                    openCamera();
                }
            } else {
                dvrService.closeCameraFromLive();
                dvrService.sendBroadcast(new Intent("com.bx.action.fastreboot"));
                if (Configuration.IS_SUPPORT_REBOOT_HINT) {
                    showRebootDialog();
                }
            }
        } else {
            if (mRecordStatus == OPENED) {
                stopRecordInner();
            }
//            if (mCameraStatus == OPENED && mPreviewStatus == OPENED) {
//                stopPreview();
//            }
            //closeCamera();
        }
    }

    public String getCameraId(){
        return cameraId;
    }

    public void openCamera() {
        logD("openCamera()");
        //!isPlugin ||
        if (mCameraStatus == OPENED
                || mCameraStatus == OPENING || mCameraStatus == REOPENING) {
            return;
        }
        mCameraStatus = OPENING;
        operateHandler.removeMessages(MSG_OPEN_CAMERA);
        operateHandler.sendEmptyMessageDelayed(MSG_OPEN_CAMERA, 500);
    }

    @SuppressLint("MissingPermission")
    private void openCameraInner() {
        logD("openCameraInner()");
        if (null == cameraDevice) {
            try {
                cameraManager.openCamera(cameraId, mCameraDeviceStateCallback, cameraHandler);
            } catch (Exception e) {
                mCameraStatus = ERROR;
                logE("openCameraInner Exception");
                e.printStackTrace();
            }
        } else {
            mCameraStatus = ERROR;
        }
    }

    public void reopenCamera() {
        logD("reopenCamera()");
        if (mCameraStatus == REOPENING) {
            return;
        }
        mCameraStatus = REOPENING;
        operateHandler.sendEmptyMessageDelayed(MSG_REOPEN_CAMERA_IN_THREAD, 10000);
    }

    private void reopenCameraInner() {
        try {
            if (null != cameraRecorder) {
                cameraRecorder.releaseRecorder();
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            mSurfaceList.clear();
            if (null != cameraRecorder) {
                cameraRecorder.releasePersistentSurface();
                cameraRecorder = null;
            }
            if (null != mSession) {
                mSession.close();
                mSession = null;
            }
        } catch (Exception e) {
            logD("reopenCameraInner() Exception");
            e.printStackTrace();
        }
        mPreviewStatus = CLOSED;
        mRecordStatus = CLOSED;
        openCameraInner();
    }

    public void closeCamera() {
        logD("closeCamera()");
        if (mCameraStatus == CLOSED || mCameraStatus == CLOSING || mCameraStatus == REOPENING) {
            return;
        }
        mCameraStatus = CLOSING;
        operateHandler.sendEmptyMessage(MSG_CLOSE_CAMERA);
    }

    private void closeCameraInner() {
        logD("closeCameraInner()");
        closePreviewInner();
        operateHandler.sendEmptyMessageDelayed(MSG_CLOSE_CAMERA_IN_THREAD, 500);
    }

    private void closeCameraInThread() {
        if (null != cameraRecorder) {
            cameraRecorder.releaseRecorder();
        }
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        mSurfaceList.clear();
        if (null != cameraRecorder) {
            cameraRecorder.releasePersistentSurface();
            cameraRecorder = null;
        }
        mRecordStatus = CLOSED;
    }

    public void startPreview() {
        logD("startPreview()");
        startPreviewInner();
    }

    private void startPreviewInner() {
        logD("startPreviewInner()");
        if (mPreviewStatus == OPENING || !isPlugin) {
            return;
        }
        mPreviewStatus = OPENING;
        if (null != cameraDevice) {
            try {
                if (null == mPreviewSurface) {
                    mPreviewSurface = getPreviewSurface();
                }
                List<Surface> surfaces = new ArrayList<>();
                surfaces.add(mPreviewSurface);
                final Surface liveSurface = maiguImageReader.getSurface();
                if (mCmdList.size() > 0) {
                    surfaces.add(liveSurface);
                }
                logD("start size = " + surfaces.size());
                cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        logD("onConfigured()");
                        if (null != session) {
                            try {
                                mSession = session;
                                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                builder.addTarget(mPreviewSurface);
                                if (mCmdList.size() > 0) {
                                    builder.addTarget(liveSurface);
                                }
                                CaptureRequest request = builder.build();
                                session.setRepeatingRequest(request, null, cameraHandler);
                                mPreviewStatus = OPENED;
                                mainHandler.removeMessages(MSG_PREVIEW_STARTED_CB);
                                mainHandler.sendEmptyMessage(MSG_PREVIEW_STARTED_CB);
//                                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                                mPreviewRequest = mPreviewBuilder.build();
//                                session.setRepeatingRequest(mPreviewRequest, null,
//                                        cameraHandler);
//                                mSession = session;
//                                mainHandler.removeMessages(MSG_PREVIEW_STARTED_CB);
//                                mainHandler.sendEmptyMessage(MSG_PREVIEW_STARTED_CB);
//                                mPreviewStatus = OPENED;
//                                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
//                                builder.set(CaptureRequest.CONTROL_ENABLE_ZSL, true);
//                                builder.addTarget(takePicSurface);
//                                mTakingPicRequest = builder.build();
                            } catch (CameraAccessException e) {
                                mPreviewStatus = CLOSED;
                                logE("onConfigured() Exception " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        logE("onConfigureFailed()");
                        mPreviewStatus = CLOSED;
                    }
                },cameraHandler);
            }catch (Exception e){
                mPreviewStatus = CLOSED;
                logE("startPreview() Exception  " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            mPreviewStatus = CLOSED;
            logE("startPreview() textureView is null !");
        }
    }

    private void createCaptureSessionForLive() {
        if (null != cameraDevice) {
            try {
                if (null == mPreviewSurface) {
                    mPreviewSurface = getPreviewSurface();
                }
                List<Surface> surfaces = new ArrayList<>();
                surfaces.add(mPreviewSurface);
                final Surface live = maiguImageReader.getSurface();
                if (mCmdList.size() > 0) {
                    surfaces.add(live);
                }
                logD("start size = " + surfaces.size());
                cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        logD("onConfigured()");
                        if (null != session) {
                            try {
                                mSession = session;
                                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                builder.addTarget(mPreviewSurface);
                                if (mCmdList.size() > 0) {
                                    builder.addTarget(live);
                                }
                                CaptureRequest request = builder.build();
                                session.setRepeatingRequest(request, null, cameraHandler);
                                mPreviewStatus = OPENED;
                            } catch (CameraAccessException e) {
                                mPreviewStatus = CLOSED;
                                logE("onConfigured() Exception " + e.getMessage());
                                e.printStackTrace();
                            }
                            mRestartSession = OPENED;
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        logE("onConfigureFailed()");
                        mPreviewStatus = CLOSED;
                        mRestartSession = CLOSED;
                    }
                },cameraHandler);
            }catch (Exception e){
                mPreviewStatus = CLOSED;
                mRestartSession = CLOSED;
                logE("startPreview() Exception  " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            mPreviewStatus = CLOSED;
            mRestartSession = CLOSED;
            logE("startPreview() textureView is null !");
        }
    }

    public void stopPreview() {
        logD("stopPreview()");
        closePreviewInner();
    }

    private void closePreviewInner() {
        logD("closePreviewInner()");
        mPreviewStatus = CLOSED;
        if (null != mSession) {
            mSession.close();
            mSession = null;
        }
    }

    public void lockVideo(boolean lockVideo){
        logD("lockVideo() lockVideo = "+lockVideo);
        lockVideoFlag = lockVideo;
    }

    public void lockVideo(boolean lock,long lockTime) {
        lockClipVideoFlag = lock;
        this.mLockTime = lockTime;
    }

    public void startRecord() {
        logD("startRecord()");
        if (mRecordStatus == OPENED  || mRecordStatus == OPENING
                ||!isPlugin || !StorageUtils.getInstance().isSDCardMounted()) {
            return;
        }
        mRecordStatus = OPENING;
        operateHandler.removeMessages(MSG_START_RECORD);
        operateHandler.sendEmptyMessageDelayed(MSG_START_RECORD, 500);
    }

    private void startRecordInner() {
        if (null == cameraDevice) {
            logD("startRecording() cameraDevice=null");
            return;
        }

        if (null == cameraRecorder) {
            cameraRecorder = new CameraRecorder(dvrService, cameraManager, this, cameraDevice, cameraHandler,
                    cameraId);
        }
        if(!cameraRecorder.getRecordStarted()){
            cameraRecorder.startVideoRecording();
        }
    }

    public void stopRecord(long delayTime) {
        logD("stopRecord() delayTime = " + delayTime);
        if (mRecordStatus == CLOSING  || mRecordStatus == CLOSED) {
            return;
        }
        mRecordStatus = CLOSING;
        operateHandler.removeMessages(MSG_STOP_RECORD);
        operateHandler.sendEmptyMessageDelayed(MSG_STOP_RECORD, (delayTime > 0? delayTime : 0));
    }

    private void stopRecordInner() {
        logD("stopRecordInner()");
        if (null != cameraRecorder) {
            boolean stopVideoSuccess = cameraRecorder.stopVideoRecording();
            logD("stopRecordInner() stopVideoSuccess ="+stopVideoSuccess);
            if (!stopVideoSuccess) {
                if (null != dvrService) {
                    dvrService.onRecordStopped(cameraId);
                }
            }
        }
        mRecordStatus = CLOSED;
    }

    public void switchNextFile() {
        logD("switchNextFile()");
        operateHandler.sendEmptyMessage(MSG_SWITCH_NEXT_FILE);
    }

    private void switchNextFileInner(){
        logD("switchNextFileInner()");
        operateHandler.removeMessages(MSG_SWITCH_NEXT_FILE_IN_THREAD);
        operateHandler.sendEmptyMessage(MSG_SWITCH_NEXT_FILE_IN_THREAD);
    }

    public void takePictures() {
        logD("takePictures()");
        if (mCameraStatus == OPENED && mPreviewStatus == OPENED) {
            operateHandler.removeMessages(MSG_TAKE_PICTURE_IN_THREAD);
            operateHandler.sendEmptyMessage(MSG_TAKE_PICTURE_IN_THREAD);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void takePicturesInner() {
        try {
            if (mSession != null) {
                if (mTakingPicRequest == null) {
                    final Surface takePicSurface = mImageReader.getSurface();
                    CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
                    builder.set(CaptureRequest.CONTROL_ENABLE_ZSL, true);
                    builder.addTarget(takePicSurface);
                    mTakingPicRequest = builder.build();
                }
                logD("takePicturesInner : "+mTakingPicRequest);
                mSession.capture(mTakingPicRequest,mCaptureCallback,cameraHandler);
            }
        } catch (CameraAccessException e) {
            logE("takePicturesInner error: "+e.getMessage());
            e.printStackTrace();
        }
    }

    public Surface getTakePicSurface() {
        return mImageReader.getSurface();
    }

    public Surface getLiveSurface() {
        return maiguImageReader.getSurface();
    }


    private CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session,
                                             CaptureRequest request, long timestamp,
                                             long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }
            };

    public boolean isCameraBusy(){
        boolean isBusy = mCameraStatus == OPENING || mCameraStatus == CLOSING || mCameraStatus == REOPENING ;
        logD("isCameraBusy() .... : "+isBusy);
        return  isBusy;
    }

    public boolean isRecordOpened() {
        return mRecordStatus == OPENED;
    }

    private void setUpCameraOutputs() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && cameraId.equals(String.valueOf(facing))) {
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isRecordStarted() {
        return mRecordStatus == OPENING || mRecordStatus == OPENED;
    }

    public boolean getCameraPluginStatus() {
        return isPlugin;
    }

    public boolean isRecordClosed() {
        return mRecordStatus == CLOSED;
    }

    public boolean isCameraLocked(){
        return lockVideoFlag;
    }

    public List<CmdPara> getCmdList() {
        return mCmdList;
    }

    @SuppressLint("HandlerLeak")
    private class MainHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            logD("handleMessage msg : "+msg.what);
            switch (msg.what){
                case MSG_OPEN_CAMERA_CB:

                    ShareBufferManager.getInstance().openShareBuffer(Integer.parseInt(cameraId));

                    if (null != cameraStateChangeListener) {
                        cameraStateChangeListener.onCameraOpened(cameraId);
                    }
                    break;
                case MSG_PREVIEW_STARTED_CB:
                    if (null != cameraStateChangeListener) {
                        cameraStateChangeListener.onPreviewStarted(cameraId);
                    }
                    break;
                case MSG_RECORD_STARTED_CB:
                    mRecordStatus = OPENED;
                    logD("MSG_RECORD_STARTED_CB");
                    if (null != dvrService) {
                        dvrService.onRecordStarted(cameraId);
                    }
                    if (null != cameraStateChangeListener) {
                        cameraStateChangeListener.onRecordStarted(cameraId);
                    }
                    break;
                case MSG_RECORD_STOPPED_CB:
                    mRecordStatus = CLOSED;
                    logD("MSG_RECORD_STOPPED_CB lockVideoFlag = "+lockVideoFlag);
//                    Message messageSave = Message.obtain(cameraHandler);
//                    messageSave.what = MSG_SAVE_VIDEO_INFO;
//                    messageSave.obj = msg.obj;
//                    cameraHandler.sendMessage(messageSave);
                    if (lockVideoFlag) {
                        lockVideoFlag = false;
                        Message message = operateHandler.obtainMessage();
                        message.what = MSG_MOVE_VIDEO_FILE_IN_THREAD;
                        message.obj = msg.obj;
                        operateHandler.sendMessage(message);
                    }
                    if (null != dvrService) {
                        dvrService.onRecordStopped(cameraId);
                    }
                    if (null != cameraStateChangeListener) {
                        cameraStateChangeListener.onRecordStoped(cameraId);
                    }
//                    Message message = Message.obtain(operateHandler);
//                    message.what = MSG_CLIP_VIDEO_IN_THREAD;
//                    message.obj = msg.obj;
//                    operateHandler.sendMessage(message);
                    break;
                case MSG_TAKE_PICTURE_CB:
                    String path = (String) msg.obj;
                    if (null != cameraStateChangeListener) {
                        cameraStateChangeListener.onTakePictureCallback(cameraId,path);
                    }
                    break;
                case MSG_SWITCH_NEXT_FILE_CB:
                    logD("MSG_SWITCH_NEXT_FILE_CB");
//                    Message messageSwitchSave = Message.obtain(cameraHandler);
//                    messageSwitchSave.what = MSG_SAVE_VIDEO_INFO;
//                    messageSwitchSave.obj = msg.obj;
//                    cameraHandler.sendMessage(messageSwitchSave);
//                    Message message1 = Message.obtain(operateHandler);
//                    message1.what = MSG_CLIP_VIDEO_IN_THREAD;
//                    message1.obj = msg.obj;
//                    operateHandler.sendMessage(message1);
                    if (lockVideoFlag) {
                        lockVideoFlag = false;
                        Message message = operateHandler.obtainMessage();
                        message.what = MSG_MOVE_VIDEO_FILE_IN_THREAD;
                        message.obj = msg.obj;
                        operateHandler.sendMessage(message);
                        if(null != cameraStateChangeListener){
                            cameraStateChangeListener.onSwitchNextFile(cameraId);
                        }
                    }
                    break;
                case MSG_RECORD_START_FAILED_CB:
                    mRecordStatus = CLOSED;
                    logD("MSG_RECORD_START_FAILED_CB");
                    if (null != cameraStateChangeListener) {
                        cameraStateChangeListener.onRecordStartFailed(cameraId, msg.arg1);
                    }
                    break;
                case MSG_CLOSE_CAMERA_CB:
                    logD("MSG_CLOSE_CAMERA_CB");
                    ShareBufferManager.getInstance().closeShareBuffer(Integer.parseInt(cameraId));
                    dvrService.onCameraClosed(cameraId);
//                    if (DvrService.CAMERA_FRONT_ID.equals(cameraId)) {
//                        ShareBufferManager.getInstance().closeShareBuffer();
//                    }
                    break;
                case MSG_REBOOT:
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private class CameraOperateHandler extends Handler {
        public CameraOperateHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_OPEN_CAMERA:
                    logD("MSG_OPEN_CAMERA: ");
                    openCameraInner();
                    break;
                case MSG_REOPEN_CAMERA_IN_THREAD:
                    reopenCameraInner();
                    break;
                case MSG_START_RECORD:
                    logD("MSG_START_RECORD");
                    if (isPlugin) {
                        if (mCameraStatus == OPENED) {
                            startRecordInner();
                        } else {
                            if (mCameraStatus == CLOSED) {
                                openCamera();
                            } else if (mCameraStatus == ERROR) {
                                reopenCamera();
                            }
                            operateHandler.removeMessages(MSG_START_RECORD);
                            operateHandler.sendEmptyMessageDelayed(MSG_START_RECORD, 500);
                        }
                    }
                    break;
                case MSG_STOP_RECORD:
                    logD("MSG_STOP_RECORD isSwitchingFile = " + (cameraRecorder == null? "null" : cameraRecorder.isSwitchingFile()));
                    if(null != cameraRecorder && cameraRecorder.isSwitchingFile()){
                        operateHandler.sendEmptyMessageDelayed(MSG_STOP_RECORD, 1000);
                    }else{
                        stopRecordInner();
                    }
                    break;
                case MSG_SWITCH_NEXT_FILE:
                    logD("MSG_SWITCH_NEXT_FILE");
                    if(isCameraBusy()){
                        logD("MSG_SWITCH_NEXT_FILE : isCameraBusy!");
                        operateHandler.removeMessages(MSG_SWITCH_NEXT_FILE);
                        operateHandler.sendEmptyMessageDelayed(MSG_SWITCH_NEXT_FILE, 500);
                    }else {
                        switchNextFileInner();
                    }
                    break;
                case MSG_SWITCH_NEXT_FILE_IN_THREAD:
                    logD("MSG_SWITCH_NEXT_FILE_IN_THREAD");
                    if(isRecordOpened()){
                        if (cameraRecorder != null) {
                            boolean b = cameraRecorder.switchVideoFile();
                            if(!b){
                                //cameraHandler.removeMessages(MSG_SWITCH_NEXT_FILE_IN_THREAD);
                               // cameraHandler.sendEmptyMessageDelayed(MSG_SWITCH_NEXT_FILE_IN_THREAD, 500);
                            }
                        }
                    } else {
                        //startRecord();
                        //cameraHandler.removeMessages(MSG_SWITCH_NEXT_FILE_IN_THREAD);
                       // cameraHandler.sendEmptyMessageDelayed(MSG_SWITCH_NEXT_FILE_IN_THREAD, 500);
                    }
                    break;
                case MSG_TAKE_PICTURE_IN_THREAD:
                    logD("MSG_TAKE_PICTURE_IN_THREAD");
                    takePicturesInner();
                    break;
                case MSG_TAKE_PICTURE:
                    Image image = (Image) msg.obj;
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    onJpegTaken(bytes);
                    image.close();
                    break;
                case MSG_MOVE_VIDEO_FILE_IN_THREAD:
                    moveLockVideoFile((String) msg.obj);
                    break;
                case MSG_CREATE_SESSION_FOR_LIVE_STATUS:
                    if (isRecordStarted()) {
                        cameraRecorder.createCaptureSessionForLive();
                    } else if (isCameraPreview()){
                        createCaptureSessionForLive();
                    }
                    break;
                case MSG_CLIP_VIDEO_IN_THREAD:
                    if (Configuration.PROJECT_NAME.equals(Configuration.KD003)) {
                        String fileName = (String) msg.obj;
                        mClipVideoUtil.setClipVideos(fileName);
                        if (lockClipVideoFlag) {
                            lockClipVideoFlag = false;
                            mClipVideoUtil.toFileClip(mLockTime);
                        }
                    }
                    break;
                case MSG_CLOSE_CAMERA:
                    logD("MSG_CLOSE_CAMERA");
                    closeCameraInner();
                    break;
                case MSG_CLOSE_CAMERA_IN_THREAD:
                    closeCameraInThread();
                    break;
                default:
                    break;
            }
        }
    }

    private class LiveHandler extends Handler {
        public LiveHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_SEND_AR_NAVI_DATA:
                    byte[] data = (byte[]) msg.obj;
                    NotifyMessageManager.getInstance().sendToGaoDeOpened(data, arCameraOpenResultParam, "dvrPicture");
                    break;
                default:
                    break;
            }
        }
    }

    private void moveLockVideoFile(String fileName){
        logD("moveLockVideoFile() fileName = " + fileName);
        File unlockFile = new File(fileName);
        if(unlockFile.exists()){
            File tmpFile = new File(StorageUtils.DIRECTORY_LOCK_VIDEO);
            if(!tmpFile.exists()){
                tmpFile.mkdirs();
            }
            if (unlockFile.renameTo(new File(StorageUtils.DIRECTORY_LOCK_VIDEO
                    + "/lock_"+unlockFile.getName()))) {
                logD("moveLockVideoFile() move success !");
            }
        }else{
            logD("moveLockVideoFile() file not exists !");
        }
    }

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraStatus = OPENED;
            cameraDevice = camera;
            initImageReader();
            initMapgooImageReader();
            mainHandler.sendEmptyMessage(MSG_OPEN_CAMERA_CB);
            if (DvrService.CAMERA_FRONT_ID.equals(cameraId)) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        initArCameraOpenResultParam();
                        NotifyMessageManager.getInstance().sendToGaoDeConnected();
                    }
                });
            }
            logD("onOpened()");
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            closeCamera();
            if (DvrService.CAMERA_FRONT_ID.equals(cameraId)) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        NotifyMessageManager.getInstance().sendToGaoDeDisconnected();
                    }
                });
            }
            mCmdList.clear();
            logE("onDisconnected()");
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
             if (isPlugin) {
                 mCameraStatus = ERROR;
                 reopenCamera();
             }
            if (DvrService.CAMERA_FRONT_ID.equals(cameraId)) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //NotifyMessageManager.getInstance().sendToGaoDeError(0,"");
                    }
                });
            }
            logE("onError ; error = " + error);
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            sendCloseCameraBroadcast();
            if (mCameraStatus != REOPENING) {
                logD("onClosed()");
                mCameraStatus = CLOSED;
                mainHandler.sendEmptyMessage(MSG_CLOSE_CAMERA_CB);
            }
            if (DvrService.CAMERA_FRONT_ID.equals(cameraId)) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        NotifyMessageManager.getInstance().sendToGaoDeClosed(0,"");
                    }
                });
            }
            mCmdList.clear();
            super.onClosed(camera);
        }
    };

    private void logD(String str){
        LogUtils.getInstance().d(TAG, getStatusPrint(str));
    }

    private void logE(String str){
        LogUtils.getInstance().e(TAG, getStatusPrint(str));
    }

    private String getStatusPrint(String msg) {
        StringBuffer sb = new StringBuffer(msg);
        sb.append(";mCameraId = ");
        sb.append(cameraId);
        sb.append(";mCameraStatus=");
        sb.append(mCameraStatus);
        sb.append(";mPreviewStatus=");
        sb.append(mPreviewStatus);
        sb.append(";mRecordStatus=");
        sb.append(mRecordStatus);
        sb.append(";isPlugin=");
        sb.append(isPlugin);
        return sb.toString();
    }

    public Surface getPreviewSurface(){
        if(null == mPreviewSurface){
            if(DvrService.CAMERA_FRONT_ID.equals(cameraId)){
                return FloatPreviewWindow.getFrontSurface();
            }else if(DvrService.CAMERA_BACK_ID.equals(cameraId)){
                return FloatPreviewWindow.getBackSurface();
            }
        }
        return mPreviewSurface;
    }

    public CaptureRequest.Builder getBuilder(){
        return mPreviewBuilder;
    }

    public void updateBuilder(CaptureRequest.Builder mBuilder){
        this.mPreviewBuilder = mBuilder;
    }

    public List<Surface> getSurfaceList(){
        return mSurfaceList;
    }

    public void updatePreview(CameraCaptureSession mPreviewSession){
        if(null != mPreviewSession){
            mSession = mPreviewSession;
        }
    }

    public void sendMainMessage(int what) {
        mainHandler.sendEmptyMessage(what);
    }

    public void sendMainMessage(Message msg){
        mainHandler.sendMessage(msg);
    }

    public boolean isCameraOpened() {
        return mCameraStatus == OPENED;
    }

    public boolean isCameraClosed() {
        return mCameraStatus == CLOSED;
    }

    public boolean isCameraPreview() {
        return mPreviewStatus == OPENED;
    }

    private CameraStateChangeListener cameraStateChangeListener = null;

    public void setCameraStateChangeListener(CameraStateChangeListener cameraStateChangeListener) {
        this.cameraStateChangeListener = cameraStateChangeListener;
    }

    public interface CameraStateChangeListener {
        void onCameraOpened(String mCameraId);

        void onCameraClosed(String mCameraId);

        void onRecordStarted(String mCameraId);

        void onPreviewStarted(String mCameraId);

        void onSwitchNextFile(String mCameraId);

        void onRecordStartFailed(String mCameraId, int reson);

        void onRecordStoped(String mCameraId);

        void onTakePictureCallback(String mCameraId, String pictureName);
    }

    private void initImageReader() {
//        if (DvrService.CAMERA_FRONT_ID.equals(cameraId)) {
//            mImageReader = ImageReader.newInstance(Configuration.VIDEO_WIDTH_1080P,Configuration.VIDEO_HEIGHT_1080P, ImageFormat.JPEG,2);
//        } else if (DvrService.CAMERA_BACK_ID.equals(cameraId)) {
//            mImageReader = ImageReader.newInstance(Configuration.VIDEO_WIDTH_720P,Configuration.VIDEO_HEIGHT_720P, ImageFormat.JPEG,2);
//        }
        mImageReader = ImageReader.newInstance(1280,720, ImageFormat.JPEG,2);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                LogUtils.getInstance().d(TAG,"Image available, camera id "+cameraId);
                Image image = reader.acquireNextImage();
                //ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                //byte[] bytes = new byte[buffer.remaining()];
                //buffer.get(bytes);
                //onJpegTaken(bytes, mOnTakePictureFinishListener);
                //image.close();
                Message message = operateHandler.obtainMessage();
                message.what = MSG_TAKE_PICTURE;
                message.obj = image;
                operateHandler.sendMessage(message);
            }
        },liveHandler);
    }

    private boolean isTakingPic = false;
    private void onJpegTaken(byte[] data) {
        if (data != null) {
            logD("onJpegTaken data length :"+data.length);
        }
        String photoPath = null;
        do {
            if (mCameraStatus != OPENED) {
                break;
            }
            String photoName = StorageUtils.getInstance().generatePictureFileName(cameraId);
            File saveFile = new File(photoName);
            logD("saveFile :"+saveFile);
            if (saveFile != null) {
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(saveFile);
                    outputStream.write(data);
                    photoPath = saveFile.getAbsolutePath();
                } catch (IOException e) {
                    e.printStackTrace();
                    logD("saveFile fail :"+e.getMessage());
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            logD("saveFile fail :"+e.getMessage());
                        }
                    }
                }
            } else {
                Log.w(TAG, "Create a picture file fail !");
            }
        } while (false);
        Message message = mainHandler.obtainMessage();
        message.what = MSG_TAKE_PICTURE_CB;
        message.obj = photoPath;
        mainHandler.sendMessage(message);
    }

    public int dataIndex = -1;
    private List<CmdPara> mCmdList=new ArrayList<>();
    public void initMapgooStatus() {
        String uniqueId = "bixin_"+cameraId;
        ThreadPoolUtil.post(new Runnable() {
            @Override
            public void run() {
                dataIndex = -1;
                while (dataIndex < 0) {
                    dataIndex = MapgooIPC.getDataIndexByUniqueID(uniqueId);
                }
            }
        });
    }

    public void addMapgooCmd(int cmd,long time) {
        logD("addMapgooCmd cmd = "+cmd+" , time = "+time);
        if(cmd == MapgooIPC.MAPGOO_IPC_CMD_CAPTURE_LIVE_START || cmd == MapgooIPC.MAPGOO_IPC_CMD_CAPTURE_LIVE_STOP
                || cmd == MapgooIPC.MAPGOO_IPC_CMD_CAPTURE_PHOTO || cmd ==MapgooIPC.MAPGOO_IPC_CMD_CAPTURE_VIDEO ||
                   cmd == Configuration.CMD_GAODE_AR ) {
            if (mCmdList.size() == 0) {
                CmdPara para = new CmdPara(cmd, time);
                mCmdList.add(para);
                restartSession();
            } else {
                CmdPara para = new CmdPara(cmd, time);
                mCmdList.add(para);
            }
        }
    }

    private int channelNum;
    private String channel;
    public void addEcarCMD(int cmd,int channelNum,String channel,long time) {
       if (cmd == Configuration.CMD_ECAR_START_LIVE) {
           this.channelNum = channelNum;
           this.channel = channel;
           if (mH264Encoder == null) {
               mH264Encoder = new H264Encoder(640,480);
           }
           if (mCmdList.size() == 0) {
               StreamProcessManager.init(1280,720,640,480);
               CmdPara para = new CmdPara(cmd, time);
               mCmdList.add(para);
               restartSession();
           } else {
               CmdPara para = new CmdPara(cmd, time);
               mCmdList.add(para);
           }
       } else if (cmd == Configuration.CMD_ECAR_STOP_LIVE) {
           mCmdList.clear();
           restartSession();
           if (mH264Encoder != null) {
               mH264Encoder.stopEncoder();
               mH264Encoder = null;
               LogUtils.getInstance().d(TAG,"release mH264Encoder");
               StreamProcessManager.release();
           }
       }
    }

    private static final int FORMAT=1;
    private void initMapgooImageReader() {
//        if (DvrService.CAMERA_FRONT_ID.equals(cameraId)) {
//            maiguImageReader = ImageReader.newInstance(Configuration.VIDEO_WIDTH_1080P, Configuration.VIDEO_HEIGHT_1080P, ImageFormat.YUV_420_888, 2);
//        } else if (DvrService.CAMERA_BACK_ID.equals(cameraId)) {
//            maiguImageReader = ImageReader.newInstance(Configuration.VIDEO_WIDTH_720P, Configuration.VIDEO_HEIGHT_720P, ImageFormat.YUV_420_888, 2);
//        }
        maiguImageReader = ImageReader.newInstance(Configuration.VIDEO_WIDTH_720P, Configuration.VIDEO_HEIGHT_720P, ImageFormat.YUV_420_888, 2);
        maiguImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            private byte[] mReadBuffer;
            @Override
            public void onImageAvailable(ImageReader reader) {
                if (mCmdList.size() > 0) {//dataIndex >= 0 &&
                    Image image = reader.acquireNextImage();
                    if (image != null) {
                        int width=image.getWidth();
                        int height=image.getHeight();
                        int n_image_size = width * height * 3 / 2;
                        mReadBuffer = new byte[n_image_size];
                        System.arraycopy(ImageUtil.getBytesFromImageAsType2(image, ImageUtil.NV21), 0, mReadBuffer, 0, n_image_size);
                        image.close();
                        int cmd = -100;
                        CmdPara cmdPara = null;
                        if (mCmdList.size() > 0) {
                            cmdPara = mCmdList.get(0);
                            cmd = cmdPara.cmd;
                        }
                        switch (cmd){
                            case MapgooIPC.MAPGOO_IPC_CMD_CAPTURE_LIVE_START:
                                if(!isHasStopLiveCmd()) {
                                    LogUtils.getInstance().d(TAG,"MAPGOO_IPC_CMD_CAPTURE_LIVE_START"+cameraId);
                                    MapgooIPC.PutYUVFrame(dataIndex, FORMAT, width, height, mReadBuffer, n_image_size);
                                }else{
                                    mCmdList.remove(cmdPara);
                                    if(mCmdList.size() == 0){
                                        restartSession();
                                    }
                                }
                                break;
                            case MapgooIPC.MAPGOO_IPC_CMD_CAPTURE_LIVE_STOP:
                                LogUtils.getInstance().d(TAG,"MAPGOO_IPC_CMD_CAPTURE_LIVE_STOP"+cameraId);
                                mCmdList.remove(cmdPara);
                                if(mCmdList.size() == 0){
                                    restartSession();
                                }
                                break;
                            case MapgooIPC.MAPGOO_IPC_CMD_CAPTURE_PHOTO:
                                LogUtils.getInstance().d(TAG,"MAPGOO_IPC_CMD_CAPTURE_PHOTO"+cameraId);
                                MapgooIPC.PutYUVFrame(dataIndex, FORMAT, width, height, mReadBuffer, n_image_size);
                                mCmdList.remove(cmdPara);
                                if(mCmdList.size() == 0){
                                    restartSession();
                                }
                                break;
                            case MapgooIPC.MAPGOO_IPC_CMD_CAPTURE_VIDEO:
                                LogUtils.getInstance().d(TAG,"MAPGOO_IPC_CMD_CAPTURE_VIDEO"+cameraId);
                                MapgooIPC.PutYUVFrame(dataIndex, FORMAT, width, height, mReadBuffer, n_image_size);
                                if(mIsSnapTimeOver){
                                    mIsSnapTimeOver = false;
                                    mIsSnapStart =false;
                                    mCmdList.remove(cmdPara);
                                    if(mCmdList.size() == 0){
                                        restartSession();
                                    }
                                }else{
                                    if(!mIsSnapStart){
                                        mIsSnapStart = true;
                                        mTask = new SnapTask();
                                        mTask.cmdPara = cmdPara;
                                        mTask.isRun = true;
                                        mTask.start();
                                    }
                                }
                                break;
                            case Configuration.CMD_GAODE_AR:
                                if (!isStartAR){
                                    mCmdList.remove(cmdPara);
                                    if(mCmdList.size() == 0){
                                        restartSession();
                                    }
                                    break;
                                }
                                if (DvrService.CAMERA_FRONT_ID.equals(cameraId)) {
                                    arCameraOpenResultParam.imageSize = n_image_size;
                                    //LogUtils.getInstance().d(TAG,"CMD_GAODE_AR");
                                    Message message = liveHandler.obtainMessage();
                                    message.what = MSG_SEND_AR_NAVI_DATA;
                                    message.obj = mReadBuffer;
                                    liveHandler.sendMessage(message);
                                }
                                break;
                            case Configuration.CMD_ECAR_START_LIVE:
                                if (mH264Encoder != null) {
                                    byte[] yuv420p = new byte[640*480*3/2];
                                    StreamProcessManager.compressYUV(mReadBuffer, 1280, 720, yuv420p, 640, 480, 0, 0, false);
                                    byte[] nv21Data = new byte[640*480*3/2];
                                    StreamProcessManager.yuvI420ToNV21(yuv420p, nv21Data, 640, 480);
                                    //byte[] h264 = new byte[200000];
                                    //int ret = mH264Encoder.offerEncoder(nv21Data,channelNum, dvrService.getLiveClient(channel));
                                    logD("N21encoderH264");
//                                if (ret > 0) {
//                                    byte[] h264Data = new byte[ret];
//                                    System.arraycopy(h264, 0, h264Data, 0, ret);
//                                    JTT808Manager.getInstance().videoLive(h264Data, channelNum, dvrService.getLiveClient(channel));
//                                }
                                }
                                break;
                            case Configuration.CMD_ECAR_STOP_LIVE:
                                LogUtils.getInstance().d(TAG,"CMD_ECAR_STOP_LIVE"+cameraId);
                                mCmdList.remove(cmdPara);
                                if(mCmdList.size() == 0){
                                    restartSession();
                                }
                                break;
                            default:
                                break;
                        }
                    }
                } else {
                    Image image = reader.acquireNextImage();
                    if (image != null) {
                        image.close();
                    }
                }
            }
        }, liveHandler);
    }

    private void restartSession(){
        if (mRestartSession == REOPENING) {
            return;
        }
        mRestartSession = REOPENING;
        LogUtils.getInstance().d(TAG,"restartSession");
        operateHandler.removeMessages(MSG_CREATE_SESSION_FOR_LIVE_STATUS);
        operateHandler.sendEmptyMessage(MSG_CREATE_SESSION_FOR_LIVE_STATUS);
    }

    public void setRestartSessionStatus(int status) {
        mRestartSession = status;
    }

    private boolean isHasStopLiveCmd(){
        int index = -1;
        for(int i=0;i < mCmdList.size();i++){
            if(mCmdList.get(i).cmd == MapgooIPC.MAPGOO_IPC_CMD_CAPTURE_LIVE_STOP){
                index = i;
                break;
            }
        }
        if(index > 0) {
            mCmdList.remove(index);
        }
        return index != -1;
    }

    private boolean mIsSnapStart = false;
    private boolean mIsSnapTimeOver = false;

    private SnapTask mTask = new SnapTask();
    class  SnapTask extends Thread{
        public CmdPara cmdPara;
        public boolean isRun=true;
        @Override
        public void run() {
            while (isRun){
                cmdPara.time--;
                logD("time:"+cmdPara.time);
                if(cmdPara.time == 0){
                    mIsSnapTimeOver = true;
                    isRun = false;
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setAROpen(boolean isOpen) {
        this.isStartAR = isOpen;
    }

    private void  initArCameraOpenResultParam(){
        arCameraOpenResultParam = new ArCameraOpenResultParam();
        arCameraOpenResultParam.cameraId = String.valueOf(Configuration.CAMERA_IDS[0]);
        arCameraOpenResultParam.imageWidth = Configuration.VIDEO_WIDTH_720P;
        arCameraOpenResultParam.imageHeight = Configuration.VIDEO_HEIGHT_720P;
        arCameraOpenResultParam.imageFormat = 0;
    }

    private void sendCloseCameraBroadcast() {
        if (Configuration.PROJECT_NAME.equals(Configuration.KD003)) {
            Intent intent = new Intent();
            intent.setAction(Configuration.ACTION_BX_SEND);
            intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.putExtra("release_camera",true);
            intent.putExtra("camera_id",cameraId);
            dvrService.sendBroadcast(intent);
            LogUtils.getInstance().d(TAG, "sendCloseCameraBroadcase: ");
        }

    }

    AlertDialog dialog = null;
    private void showRebootDialog() {
        if (dialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(dvrService);
            builder.setTitle(R.string.dialog_title);
            builder.setMessage(R.string.ahd_reboot);
            builder.setPositiveButton(R.string.btn_sure, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mainHandler.removeMessages(MSG_REBOOT);
                }
            });
            builder.setCancelable(false);
            dialog = builder.create();
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_DISPLAY_OVERLAY);
        }
        dialog.show();
        mainHandler.removeMessages(MSG_REBOOT);
        mainHandler.sendEmptyMessageDelayed(MSG_REBOOT,5000);
    }

}
