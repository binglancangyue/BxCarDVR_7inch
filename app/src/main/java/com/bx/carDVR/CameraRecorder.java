package com.bx.carDVR;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.bx.carDVR.bean.CameraProfile;
import com.bx.carDVR.bean.Configuration;
import com.bx.carDVR.prefs.SettingsManager;
import com.bx.carDVR.util.LogUtils;
import com.bx.carDVR.util.StorageUtils;
import com.bx.carDVR.util.ThreadPoolUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class CameraRecorder implements MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    private static final String TAG = "BxCameraRecorder";

    public static final int RESON_CONFIGURE_FAILED = 1;
    public static final int RESON_CAMERA_ACCESS_EXCEPTION = 2;
    public static final int RESON_ILLEGAL_STATE_EXCEPTION = 3;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraInstance mCameraInstance;
    private CameraProfile mProfile;
    private Context mContext;
    private String mCameraId;
    private MediaRecorder mMediaRecorder;
    private CaptureRequest.Builder mPreviewBuilder;
    private Handler mBackgroundHandler;
    private String mVideoFilename = "";
    private String mNewFileName = "";
    private CameraCaptureSession mSession;
    private boolean mRecordStarted;
    private Surface persistentSurface = null;
    private boolean mCreatingNextFile = false;

    public CameraRecorder(Context mContext, CameraManager mCameraManager, CameraInstance mCameraInstance,
                          CameraDevice mCameraDevice, Handler mBackgroundHandler, String mCameraId) {
        this.mContext = mContext;
        this.mCameraManager = mCameraManager;
        this.mCameraInstance = mCameraInstance;
        this.mCameraDevice = mCameraDevice;
        this.mBackgroundHandler = mBackgroundHandler;
        this.mCameraId = mCameraId;
        //settingsManager = SettingsManager.getInstance();
        //settingsManager.addOnMuteStateChangeListener(this);
        persistentSurface = MediaCodec.createPersistentInputSurface();
    }

    private boolean initVideoRecording() {
        logI("initVideoRecording");
        try {
            if (null == mCameraManager) {
                logE("mCameraManager is null");
                return false;
            }
            if (null == mMediaRecorder) {
                mMediaRecorder = new MediaRecorder();
                if (null == mMediaRecorder) {
                    logE("new MediaRecorder failed");
                    return false;
                }
            }
            setVideoRecordingParaments();
            setVideoRecordingOutputFile();
            mMediaRecorder.setOnErrorListener(this);
            mMediaRecorder.setOnInfoListener(this);
            return true;
        }catch (Exception e){
            logI("initVideoRecording fail :"+e.getMessage());
            return  false;
        }
    }

    private void setVideoRecordingParaments() {
        logI("setVideoRecordingParaments()");
        initProfile();
        //logI("isMute = " +SettingsManager.getInstance().isMute());
        if(DvrService.CAMERA_FRONT_ID.equals(mCameraId) && !SettingsManager.getInstance().isMute()){
            logI("video will record voice!");
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(StorageUtils.IS_MP4_FORMAT
                    ? MediaRecorder.OutputFormat.MPEG_4 : MediaRecorder.OutputFormat.MPEG_2_TS);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        } else{
            logI("video will not record voice!");
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(StorageUtils.IS_MP4_FORMAT
                    ? MediaRecorder.OutputFormat.MPEG_4 : MediaRecorder.OutputFormat.MPEG_2_TS);
        }

        mMediaRecorder.setVideoEncoder(mProfile.videoCodec);
        mMediaRecorder.setVideoSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
        mMediaRecorder.setVideoEncodingBitRate(mProfile.videoBitRate);
        mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
    }

    private void initProfile(){
        logI("initProfile()");
        if (null == mProfile) {
            mProfile = new CameraProfile();
        }
        if (null != mProfile) {
            mProfile.videoCodec = MediaRecorder.VideoEncoder.H264;
            if (DvrService.CAMERA_FRONT_ID.equals(mCameraId)) {
                mProfile.videoBitRate = 12000000;
                mProfile.videoFrameWidth = Configuration.VIDEO_WIDTH_1080P;
                mProfile.videoFrameHeight = Configuration.VIDEO_HEIGHT_1080P;
            } else if (DvrService.CAMERA_BACK_ID.equals(mCameraId)) {
                mProfile.videoBitRate = 10000000;
                mProfile.videoFrameWidth = Configuration.VIDEO_WIDTH_720P;
                mProfile.videoFrameHeight = Configuration.VIDEO_HEIGHT_720P;
            }
            mProfile.videoFrameRate = 25;
//            if (DvrService.CAMERA_FRONT_ID.equals(mCameraId)) {
//                mProfile.videoBitRate = 12000000;
//                mProfile.videoFrameWidth = 1920;
//                mProfile.videoFrameHeight = 1080;
//                mProfile.videoFrameRate = 25;
//            } else if (DvrService.CAMERA_BACK_ID.equals(mCameraId)) {
//                mProfile.videoBitRate = 8000000;
//                mProfile.videoFrameWidth = 1280;
//                mProfile.videoFrameHeight = 720;
//                mProfile.videoFrameRate = 25;
//            }
        }
    }

    private void setVideoRecordingOutputFile() {
        mVideoFilename = StorageUtils.generateVideoFileName(mCameraId);
        mNewFileName = mVideoFilename;
        logI("setVideoRecordingOutputFile() name :"+mVideoFilename);
        mMediaRecorder.setOutputFile(mVideoFilename);
    }

    public boolean startVideoRecording() {
        logI("startVideoRecording");
        if(null == mCameraDevice){
            logI("startVideoRecording() null == mCameraDevice!");
            return false;
        }
        boolean initRecord = initVideoRecording();
        if (!initRecord) {
            logE("initRecord failed!");
            return false;
        }
        mMediaRecorder.setInputSurface(persistentSurface);
        try {
            logI("startVideoRecording() persistentSurface = " + (persistentSurface == null));
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            logE("startVideoRecording() IllegalStateException e = "+e.getMessage());
            return false;
        } catch (IOException e) {
            logE("startVideoRecording() IOException e = "+e.getMessage());
            return false;
        }
        try {
//            mPreviewBuilder = mCameraInstance.getBuilder();
//            logI("startVideoRecording() mPreviewBuilder = " + (mPreviewBuilder == null));
//            if (null == mPreviewBuilder) {
//                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
//                mCameraInstance.updateBuilder(mPreviewBuilder);
//            }
//            List<Surface> surfaces = mCameraInstance.getSurfaceList();
//            logI("startVideoRecording() start surfaces size = " + surfaces.size());
//            Surface mPreviewSurface = mCameraInstance.getPreviewSurface();
//            logI("startVideoRecording() mPreviewSurface= " + (mPreviewSurface == null));
//            if (null != mPreviewSurface) {
//                if(!surfaces.contains(mPreviewSurface)){
//                    surfaces.add(mPreviewSurface);
//                    mPreviewBuilder.addTarget(mPreviewSurface);
//                }
//            }
//            Surface recorderSurface = (persistentSurface != null) ? persistentSurface : mMediaRecorder.getSurface();
//            surfaces.add(recorderSurface);
//            logI("startVideoRecording() end surfaces size = " + surfaces.size());
//            mPreviewBuilder.addTarget(recorderSurface);
            List<Surface> surfaces = new ArrayList<>();
            final Surface mPreviewSurface = mCameraInstance.getPreviewSurface();
            surfaces.add(mPreviewSurface);
            final Surface recorderSurface = (persistentSurface != null) ? persistentSurface : mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
//            Surface takePicSurface = mCameraInstance.getTakePicSurface();
//            surfaces.add(takePicSurface);
            final Surface liveSurface = mCameraInstance.getLiveSurface();
            if (mCameraInstance.getCmdList().size() > 0) {
                surfaces.add(liveSurface);
            }
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCameraInstance.updatePreview(session);
                    //updatePreview(session);
                    try {
                        CaptureRequest.Builder builder =
                                mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                        builder.addTarget(mPreviewSurface);
                        builder.addTarget(recorderSurface);
                        if (mCameraInstance.getCmdList().size() > 0) {
                            builder.addTarget(liveSurface);
                        }
                        //builder.addTarget(liveSurface);
                        CaptureRequest request = builder.build();
                        session.setRepeatingRequest(request, null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    startRecording();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    startRecordingFailed(RESON_CONFIGURE_FAILED);
                    logE("startVideoRecording onConfigureFailed!");
                }
            },mBackgroundHandler);
        } catch (CameraAccessException e) {
            logE("startVideoRecording() CameraAccessException");
            e.printStackTrace();
            startRecordingFailed(RESON_CAMERA_ACCESS_EXCEPTION);
            return false;
        } catch (IllegalStateException e) {
            logE("startVideoRecording() IllegalStateException");
            e.printStackTrace();
            startRecordingFailed(RESON_ILLEGAL_STATE_EXCEPTION);
            return false;
        }
        return true;
    }

    public void createCaptureSessionForLive() {
        if (mCameraDevice != null) {
            try {
                List<Surface> surfaces = new ArrayList<>();
                final Surface mPreviewSurface = mCameraInstance.getPreviewSurface();
                surfaces.add(mPreviewSurface);
                final Surface recorderSurface = (persistentSurface != null) ? persistentSurface : mMediaRecorder.getSurface();
                surfaces.add(recorderSurface);
                Surface takePicSurface = mCameraInstance.getTakePicSurface();
                surfaces.add(takePicSurface);
                final Surface liveSurface = mCameraInstance.getLiveSurface();
                if (mCameraInstance.getCmdList().size() > 0) {
                    surfaces.add(liveSurface);
                }
                mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        mCameraInstance.updatePreview(session);
                        try {
                            CaptureRequest.Builder builder =
                                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                            builder.addTarget(mPreviewSurface);
                            builder.addTarget(recorderSurface);
                            if (mCameraInstance.getCmdList().size() > 0) {
                                builder.addTarget(liveSurface);
                            }
                            CaptureRequest request = builder.build();
                            session.setRepeatingRequest(request, null, mBackgroundHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                        //startRecording();
                        mCameraInstance.setRestartSessionStatus(CameraInstance.OPENED);
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        startRecordingFailed(RESON_CONFIGURE_FAILED);
                        mCameraInstance.setRestartSessionStatus(CameraInstance.CLOSED);
                        logE("startVideoRecording onConfigureFailed!");
                    }
                },mBackgroundHandler);
            } catch (CameraAccessException e) {
                logE("startVideoRecording() CameraAccessException");
                e.printStackTrace();
                mCameraInstance.setRestartSessionStatus(CameraInstance.CLOSED);
                startRecordingFailed(RESON_CAMERA_ACCESS_EXCEPTION);
            } catch (IllegalStateException e) {
                logE("startVideoRecording() IllegalStateException");
                e.printStackTrace();
                mCameraInstance.setRestartSessionStatus(CameraInstance.CLOSED);
                startRecordingFailed(RESON_ILLEGAL_STATE_EXCEPTION);
            }
        }
    }

    private void startRecording() {
        logI("startRecording()");
        try {
            mMediaRecorder.start();
            mRecordStarted = true;
            mCameraInstance.sendMainMessage(CameraInstance.MSG_RECORD_STARTED_CB);
        } catch (IllegalStateException e) {
            logE("startRecording() IllegalStateException e = "+e.getMessage());
            e.printStackTrace();
        }
    }

    private void updatePreview(CameraCaptureSession previewSession) {
        if (null == mCameraDevice) {
            return;
        }
        try {
            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
            mSession = previewSession;
            mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            logE("updatePreview fail! CameraAccessException :");
            e.printStackTrace();
        }
    }

    private void startRecordingFailed(int reson){
        logI("startRecordingFailed() reson = "+reson);
        if(null != mCameraInstance){
            Message msg = Message.obtain();
            msg.what = CameraInstance.MSG_RECORD_START_FAILED_CB;
            msg.arg1 = reson;
            mCameraInstance.sendMainMessage(msg);
        }
    }

    public boolean switchVideoFile() {
        boolean isSuccess=true;
        logI("switchVideoFile");
        if(mCreatingNextFile || null == mMediaRecorder){
            logI("switchVideoFile disabled...");
            isSuccess=false;
            return isSuccess;
        }
        mCreatingNextFile = true;
        logE("create switchFile start" );
        try {
            String switchFileName = StorageUtils.generateVideoFileName(mCameraId);
            RandomAccessFile mNextVideofile = new RandomAccessFile(switchFileName, "rws");
            logE("create switchFile end" );
            if(null != mNextVideofile){
                mMediaRecorder.switchRecording(mNextVideofile.getFD());
                String newSwitchFileName = StorageUtils.generateVideoFileName(mCameraId);
                ThreadPoolUtil.post(new RenameThread(mVideoFilename, mNewFileName, CameraInstance.MSG_SWITCH_NEXT_FILE_CB));
                mVideoFilename = switchFileName;
                mNewFileName = newSwitchFileName;
                logE("next mVideoFilename = " + mVideoFilename);
                logE("next mNewFileName = " + mNewFileName);
            }
        } catch (FileNotFoundException e) {
            logE("switchVideoFile fail! FileNotFoundException :" + e.getMessage());
            isSuccess=false;
        } catch(Exception e){
            logE("switchVideoFile fail! Exception :" + e.getMessage());
            isSuccess=false;
        }finally {
            mCreatingNextFile = false;//zqc zhangyafei add for front camera not recording
        }
        mCreatingNextFile = false;

        return isSuccess;
    }

    class RenameThread implements Runnable {
        int msgId;
        String curName;
        String newName;
        public RenameThread(String s1, String s2, int id) {
            curName = s1;
            newName = s2;
            msgId = id;
            logI("curName = " + curName + "; newName = " + newName + "; msgId = " + msgId);
        }

        @Override
        public void run() {
            renameFileAndSendMsg(curName, newName, msgId);
        }
    }

    private void renameFileAndSendMsg(String curName, String newName, int msgId) {
        Message msg  = Message.obtain();
        msg.what = msgId;
        boolean result = renameFile();
        if (result) {
            msg.obj = newName;
        } else {
            msg.obj = curName;
        }
        logE("rename video file = " + msg.obj);
        mCameraInstance.sendMainMessage(msg);
    }

    private boolean renameFile() {
        try {
            if (mVideoFilename.equals(mNewFileName)) {
                return true;
            } else {
                File file = new File(mVideoFilename);
                if (file.exists() && file.isFile()) {
                    return file.renameTo(new File(mNewFileName));
                }
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isSwitchingFile(){
        return mCreatingNextFile;
    }

    public boolean stopVideoRecording() {
        logI("stopVideoRecording()");
        if (!mRecordStarted) {
            return false;
        }
        try {
            logI("stopVideoRecording() start. ");
            if(null != mMediaRecorder){
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                mMediaRecorder.setOnErrorListener(null);
                mMediaRecorder.setOnInfoListener(null);
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
            logI("stopVideoRecording() end. ");
        } catch (Exception e) {
            logE("mMediaRecorder stop error! "+e.getMessage());
        }
        mRecordStarted = false;
        //List<Surface> surfaces = mCameraInstance.getSurfaceList();
        //logI("stopVideoRecording() start surfaces size = " + surfaces.size());
        CaptureRequest.Builder builder = null;
        try {
            builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Surface recorderSurface = (persistentSurface != null) ? persistentSurface : mMediaRecorder.getSurface();
        //surfaces.remove(recorderSurface);
        builder.removeTarget(recorderSurface);
        builder.removeTarget(mCameraInstance.getTakePicSurface());
        //logI("stopVideoRecording() end surfaces size = " + surfaces.size());
        try {
            if(mSession != null) {
                mSession.setRepeatingRequest(builder.build(), null, mBackgroundHandler);
            }
        }catch (IllegalStateException e){
            logE("stopVideoRecording fail! IllegalStateException :");
            e.printStackTrace();
        }catch (CameraAccessException e) {
            logE("stopVideoRecording fail! CameraAccessException :");
            e.printStackTrace();
        }
        //mCameraInstance.updateBuilder(mPreviewBuilder);
        ThreadPoolUtil.post(new RenameThread(mVideoFilename, mNewFileName, CameraInstance.MSG_RECORD_STOPPED_CB));
        return true;
    }

    public boolean getRecordStarted() {
        return mRecordStarted;
    }

    public void releaseRecorder() {
        logI("releaseRecorder()");
        mRecordStarted = false;
//        if (settingsManager != null) {
//            settingsManager.removeOnMuteStateChangeListener(this);
//            settingsManager = null;
//        }
        if(null != mProfile){
            mProfile = null;
        }
//        if(null != persistentSurface){
//        	persistentSurface.release();
//        	persistentSurface = null;
//        }
    }

    public void releasePersistentSurface() {
        logI("releasePersistentSurface() persistentSurface =" + persistentSurface);
        if(null != persistentSurface){
            persistentSurface.release();
            persistentSurface = null;
        }
    }

    private void logI(String str){
        LogUtils.getInstance().d(TAG, str + ";mCameraId = " + mCameraId);
    }

    private void logE(String str){
        LogUtils.getInstance().e(TAG, str + ";mCameraId = " + mCameraId);
    }

    @Override
    public void onError(MediaRecorder mediaRecorder, int what, int extra) {
        logE("onError() what = " + what + ";extra = "+extra);
        switch (what) {
            case MediaRecorder.MEDIA_ERROR_SERVER_DIED:
                logE("media recorder died!");
                break;
            case MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN:
                logE("media recorder unknow error!");
                break;
            default:
                break;
        }
    }

    @Override
    public void onInfo(MediaRecorder mediaRecorder, int i, int i1) {

    }

}
