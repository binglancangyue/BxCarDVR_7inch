package com.bx.carDVR.manager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Gravity;

import com.bx.carDVR.DvrService;
import com.bx.carDVR.app.DvrApplication;
import com.bx.carDVR.bean.Configuration;
import com.bx.carDVR.util.GpioManager;
import com.bx.carDVR.util.H264Encoder;
import com.bx.carDVR.util.LogUtils;
import com.bx.carDVR.util.StorageUtils;
import com.bx.carDVR.util.ThreadPoolUtil;
import com.ecar.ecarjttsdk.ECarJttListener;
import com.ecar.ecarjttsdk.ECarJttManage;
import com.riemannlee.liveproject.StreamProcessManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class ECarOverseaManager implements ECarJttListener {

    private static final String TAG = "ECarOverseaManager";

    private static ECarOverseaManager eCarOverseaManager;

    private ECarJttManage mECarJttManage;

    private static final long duration = 10*1000*1000;

    private H264Encoder mH264EncoderOne;
    private H264Encoder mH264EncoderTwo;

    private LinkedBlockingQueue<byte[]> yuvQueue;

    private ECarOverseaManager() {
        ECarJttManage.init(DvrApplication.getDvrApplication());
        mECarJttManage = ECarJttManage.getInstance();
        StreamProcessManager.init(1280,720,640,480);
        mH264EncoderOne = new H264Encoder(640,480);
        mH264EncoderTwo = new H264Encoder(640,480);
    }

    public static ECarOverseaManager getInstance(){
        if (eCarOverseaManager == null) {
            synchronized (ECarOverseaManager.class){
                if (eCarOverseaManager == null) {
                    eCarOverseaManager = new ECarOverseaManager();
                }
            }
        }
        return eCarOverseaManager;
    }

    public void startECarListener() {
        Log.d(TAG,"mECarJttManage == "+mECarJttManage);
        //mECarJttManage.registerECarJtt808("123412345678901");
        mECarJttManage.registerECarJtt808(DvrApplication.getDvrApplication());
        mECarJttManage.setJttListener(this);
    }

    @Override
    public void startLive(int channelNum) {
         LogUtils.getInstance().d(TAG,"startLive channelNum == "+channelNum);
        if (channelNum < 1 || channelNum > 2) {
            mECarJttManage.snapResult0805(2,0);
            return;
        }
         int cameraId = channelNum -1;
         if (cameraId == 1 && !GpioManager.getInstance().isAhdPluginIn()) {
             return;
         }
         if (cameraId == 0) {
             ShareBufferManager.getInstance().addShareBufferRequest(cameraId,liveChannelOne,"ECarOversea");
         } else if (cameraId == 1) {
             ShareBufferManager.getInstance().addShareBufferRequest(cameraId,liveChannelTwo,"ECarOversea");
         }

    }

    @Override
    public void stopLive(int channelNum) {
        LogUtils.getInstance().d(TAG,"stopLive channelNum == "+channelNum);
        if (channelNum < 1 || channelNum > 2) {
            mECarJttManage.snapResult0805(2,0);
            return;
        }
        int cameraId = channelNum -1;
        if (cameraId == 0) {
            ShareBufferManager.getInstance().removeShareBufferRequest(cameraId,liveChannelOne);
        } else if (cameraId == 1) {
            ShareBufferManager.getInstance().removeShareBufferRequest(cameraId,liveChannelTwo);
        }
    }

    @Override
    public void takePhotoCMD(int channelNum, int amount, int snapSpace, int saveType, int resolution, int quality, int brightness, int contrastRatio, int saturation, int chroma) {
        LogUtils.getInstance().d(TAG,"takePhotoCMD channelNum = "+channelNum);
        if (channelNum < 1 || channelNum > 2) {
            mECarJttManage.snapResult0805(2,0);
            return;
        }
        int cameraId = channelNum -1;
        if (cameraId == 1 && !GpioManager.getInstance().isAhdPluginIn()) {
            mECarJttManage.snapResult0805(1,0);
            return;
        }
        if (StorageUtils.getInstance().isSDCardMounted()) {
            ShareBufferManager.getInstance().addShareBufferRequest(cameraId,pictureChannel,"ECarOversea");
        } else {
            mECarJttManage.snapResult0805(1,0);
        }
    }

    @Override
    public void takeVideoCMD(int channelNum, int duration, int saveType, int resolution, int quality, int brightness, int contrastRatio, int saturation, int chroma) {
        LogUtils.getInstance().d(TAG,"takeVideoCMD channelNum = "+channelNum);
        if (channelNum < 1 || channelNum > 2) {
            mECarJttManage.snapResult0805(2,0);
            return;
        }
        int cameraId = channelNum -1;
        if (cameraId == 1 && !GpioManager.getInstance().isAhdPluginIn()) {
            mECarJttManage.snapResult0805(1,0);
            return;
        }
        if (StorageUtils.getInstance().isSDCardMounted()) {
            if (yuvQueue == null) {
                yuvQueue = new LinkedBlockingQueue<>();
            } else {
                yuvQueue.clear();
            }
            ShareBufferManager.getInstance().addShareBufferRequest(cameraId,videoChannel,"ECarOversea");
            startRecordCaptureVideo(cameraId);
        } else {
            mECarJttManage.snapResult0805(1,0);
        }
    }

    @Override
    public void stopTakePhotoVideoCMD() {
        LogUtils.getInstance().d(TAG,"stopTakePhotoVideo ");
        //ShareBufferManager.getInstance().removeShareBufferRequest(cameraId,this);
    }

    @Override
    public void uploadSnapFilm() {
        LogUtils.getInstance().d(TAG,"uploadSnapFilm ");
    }

    ShareBufferManager.YUVDataCallback pictureChannel = new ShareBufferManager.YUVDataCallback() {
        @Override
        public void processData(byte[] data, int cameraId) {
            LogUtils.getInstance().d(TAG,"CMD_SNAP_PICTURE cameraId = "+cameraId);
            if (StorageUtils.getInstance().isSDCardMounted()) {
                final byte[] picture = data.clone();
                ThreadPoolUtil.post(new Runnable() {
                    @Override
                    public void run() {
                        saveYUV2Bitmap(picture,cameraId+1);
                    }
                });
            }
            ShareBufferManager.getInstance().removeShareBufferRequest(cameraId,pictureChannel);
        }
    };

    ShareBufferManager.YUVDataCallback videoChannel = new ShareBufferManager.YUVDataCallback() {
        @Override
        public void processData(byte[] data, int cameraId) {
            LogUtils.getInstance().d(TAG,"CMD_CAPTURE_VIDEO cameraId = "+cameraId);
            byte[] yuv420p = new byte[640*480*3/2];
            StreamProcessManager.compressYUV(data, 1280, 720, yuv420p, 640, 480, 0, 0, false);
            byte[] nv21Data = new byte[640*480*3/2];
            StreamProcessManager.yuvI420ToNV21(yuv420p, nv21Data, 640, 480);
            yuvQueue.add(nv21Data);
        }
    };

    ShareBufferManager.YUVDataCallback liveChannelOne = new ShareBufferManager.YUVDataCallback(){
        @Override
        public void processData(byte[] data, int cameraId) {
            byte[] yuv420p = new byte[640*480*3/2];
            StreamProcessManager.compressYUV(data, 1280, 720, yuv420p, 640, 480, 0, 0, false);
            byte[] nv21Data = new byte[640*480*3/2];
            StreamProcessManager.yuvI420ToNV21(yuv420p, nv21Data, 640, 480);
            mH264EncoderOne.offerEncoder(nv21Data,cameraId+1,mECarJttManage);
            LogUtils.getInstance().d(TAG,"sendVideoData cameraId = "+cameraId);
        }
    };

    ShareBufferManager.YUVDataCallback liveChannelTwo = new ShareBufferManager.YUVDataCallback(){
        @Override
        public void processData(byte[] data, int cameraId) {
            byte[] yuv420p = new byte[640*480*3/2];
            StreamProcessManager.compressYUV(data, 1280, 720, yuv420p, 640, 480, 0, 0, false);
            byte[] nv21Data = new byte[640*480*3/2];
            StreamProcessManager.yuvI420ToNV21(yuv420p, nv21Data, 640, 480);
            mH264EncoderTwo.offerEncoder(nv21Data,cameraId+1,mECarJttManage);
            LogUtils.getInstance().d(TAG,"sendVideoData cameraId = "+cameraId);
        }
    };

    private void saveYUV2Bitmap(byte[] data,final int channelNum) {
        ByteArrayOutputStream stream = null;
        try {
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, Configuration.PREVIEW_WIDTH_720P, Configuration.PREVIEW_HEIGHT_720P,null);
            LogUtils.getInstance().d(TAG, "readShareBufferMsg yuvImage:" + yuvImage);
            if (yuvImage != null) {
                stream = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, Configuration.PREVIEW_WIDTH_720P, Configuration.PREVIEW_HEIGHT_720P), 50, stream);
                Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                String mPictureName;
                if (channelNum == 1) {
                    mPictureName = StorageUtils.generateECarPictureFileName(DvrService.CAMERA_FRONT_ID);
                } else {
                    mPictureName = StorageUtils.generateECarPictureFileName(DvrService.CAMERA_BACK_ID);
                }
                File saveFile = new File(mPictureName);
                LogUtils.getInstance().d(TAG, "saveFile:" + saveFile);
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(saveFile);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 50, fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    ECarJttManage.getInstance().uploadSnap0801(channelNum, saveFile,0,0);
                    ECarJttManage.getInstance().snapResult0805(0,1);
                } catch (Exception e) {
                    LogUtils.getInstance().d(TAG, "error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private long mStartWhen = 0;
    private MediaCodec mEncoder;
    private MediaMuxer mMuxer;
    private MediaCodec.BufferInfo mBufferInfo;
    private int mTrackIndex;
    private boolean isEcarRecord = false;
    private void startRecordCaptureVideo(int cameraId) {
        ThreadPoolUtil.post(new Runnable() {
            @Override
            public void run() {
                isEcarRecord = true;
                String path = StorageUtils.generateECarVideoFileName(String.valueOf(cameraId));
                byte[] input = null;
                long generateIndex = 0;
                mStartWhen = System.nanoTime();
                LogUtils.getInstance().d(TAG, "EncoderThread  start run mStartWhen ="+mStartWhen);
                // mediaCodeC采用的是H.264编码
                MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", 640, 480);
                // 数据来源自surface
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
                // 视频码率
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 300000);
                // 帧率
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
                // 设置关键帧的时间间隔,录成mp4格式,视频第一帧必须是关键帧
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
                try {
                    mEncoder = MediaCodec.createEncoderByType("video/avc");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // 设置为编码
                mEncoder.configure(mediaFormat, null, null,MediaCodec.CONFIGURE_FLAG_ENCODE);
                mEncoder.start();
                try {
                    mMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    mBufferInfo = new MediaCodec.BufferInfo();
                    mTrackIndex = -1;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                while (isEcarRecord) {
                    long presentationTimeUs = (System.nanoTime() - mStartWhen) / 1000;
                    if(presentationTimeUs > duration){
                        isEcarRecord = false;
                    }
                    if (yuvQueue.size() > 0) {
                        input = yuvQueue.poll();
                    }
                    if (input != null) {
                        try {
                            ByteBuffer[] inputBuffers = mEncoder.getInputBuffers();
                            ByteBuffer[] outputBuffers = mEncoder.getOutputBuffers();
                            // 编码
                            int inputBufferIndex = mEncoder.dequeueInputBuffer(-1);
                            if (inputBufferIndex >= 0) {
                                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                                inputBuffer.clear();
                                inputBuffer.put(input);
                                presentationTimeUs = (System.nanoTime() - mStartWhen) / 1000;
                                mEncoder.queueInputBuffer(inputBufferIndex, 0,input.length, presentationTimeUs, 0);
                                generateIndex += 1;
                            }
                            drainEncoder(false);
                        } catch (Throwable t) {
                            LogUtils.getInstance().e(TAG,t.getMessage());
                            t.printStackTrace();
                        }
                    }
                }

                if (yuvQueue.size() > 0) {
                    input = yuvQueue.poll();
                }
                if(input!=null) {
                    try {
                        ByteBuffer[] inputBuffers = mEncoder.getInputBuffers();
                        int inputBufferIndex = mEncoder.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            long presentationTimeUs = (System.nanoTime() - mStartWhen) / 1000;
                            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                            inputBuffer.clear();
                            inputBuffer.put(input);
                            mEncoder.queueInputBuffer(inputBufferIndex, 0, input.length, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            generateIndex += 1;
                        }
                        drainEncoder(false);
                    } catch (Throwable t) {
                        LogUtils.getInstance().e(TAG,t.getMessage());
                        t.printStackTrace();
                    }
                }
                mMuxerStarted = false;
                releaseEncoder();
                ShareBufferManager.getInstance().removeShareBufferRequest(cameraId,videoChannel);
                ECarJttManage.getInstance().uploadSnap0801(1, new File(path), 2, 4);
                ECarJttManage.getInstance().snapResult0805(0,1);
            }
        });
    }

    private final int TIMEOUT_USEC = 10000;
    private boolean mMuxerStarted;
    private void drainEncoder(boolean endOfStream) {
        if (endOfStream) {
            mEncoder.signalEndOfInputStream();
        }
        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            //拿到输出缓冲区的索引
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);

            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) {
                    break;
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (mMuxerStarted) {
                    //throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mEncoder.getOutputFormat();
                mTrackIndex = mMuxer.addTrack(newFormat);
//                if(mIsHaveSound){
//                    audioTrackIndex = mMuxer.addTrack(aEncoder.getOutputFormat());
//                }
                mMuxer.start();
                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
            } else {
                //获取解码后的数据
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    //throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    if(mTrackIndex == -1){
                        continue;
                    }
                    mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                }
                mEncoder.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        LogUtils.getInstance().d(TAG, "reached end of stream unexpectedly");
                    }
                    break;
                }
            }
        }
    }

    private void releaseEncoder() {
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mBufferInfo != null) {
            mBufferInfo = null;
        }
        if (mMuxer != null) {
            LogUtils.getInstance().d(TAG, "releaseEncoderBack mMuxer");
            try {
                mMuxer.stop();
                mMuxer.release();
                mMuxer = null;
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.getInstance().d(TAG, "releaseEncoder exception");
            }
        }
    }

    public void showECarQR() {
        if (mECarJttManage != null) {
            mECarJttManage.showECarQRActivity(DvrApplication.getDvrApplication(),0,0,460,300, Gravity.CENTER);
        }
    }

}
