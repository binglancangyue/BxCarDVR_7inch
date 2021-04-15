package com.bx.carDVR.manager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.bx.carDVR.DvrService;
import com.bx.carDVR.bean.BufferBean;
import com.bx.carDVR.util.ClickUtils;
import com.bx.carDVR.util.GpioManager;
import com.bx.carDVR.util.LogUtils;
import com.bx.carDVR.util.StorageUtils;
import com.bx.carDVR.util.ThreadPoolUtil;
import com.zqc.share.ShareBuffer;

public class ShareBufferManager {

    private static final String TAG = "BxShareBufferManager";

    private static final boolean DEBUG = false;
    public static final int PREVIEW_WIDTH = 1280;
    public static final int PREVIEW_HEIGHT = 720;
    public static int PREVIEW_WIDTH1 = 1280;
    public static int PREVIEW_HEIGHT1 = 720;
    public static final int FRONT_CAMERA_ID = 0;
    public static final int BACK_CAMERA_ID = 1;
    private boolean isPreview = true;
    private int zoomLevel = 1;

    private final int BUFFER_SIZE = PREVIEW_WIDTH*PREVIEW_HEIGHT*3/2;// 1280 * 720 * 3 / 2;
    // private final int MEMORY_SIZE = 3133440+1;//1920 * 1088 * 3 / 2 + 1;
    private final int FRAME_HEADER = 4;// 16;
    private final int MEMORY_SIZE = BUFFER_SIZE + FRAME_HEADER;// 3133440 + 1;
    private BufferBean mBufferBean;

    private ShareBuffer shareBuffer;
    private static ShareBufferManager shareBufferManager;
    private Thread mReadBufferThread;
    private boolean isInit = false;
    private boolean isLooper = false;
    private boolean lastLooperStatus = isLooper;
    private boolean isReadBufferThreadRunning = false;
    private String mPictureName;

    private ShareBufferHandler mShareBufferHandler;
    private static final int MSG_INIT_SHAREBUFFER = 0;
    private static final int MSG_TAKE_PICTURE = 1;
    private static final int MSG_NOTIFY_MAPGOO_GSENSOR_WAKEUP = 2;

    private Map<Integer, List<YUVDataCallback>> mShareBufferUseList;
    private Map<Integer, Boolean> mShareBufferSwitchList;

    public class ShareBufferHandler extends Handler {
        public ShareBufferHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT_SHAREBUFFER:
                    initSharebufferInternal();
                    break;
                case MSG_TAKE_PICTURE:
                    saveBitmap();
                    break;
                default:
                    break;
            }
        }
    }

    private ShareBufferManager() {
        LogUtils.getInstance().d(TAG, "ShareBufferManager()");

        mShareBufferUseList = new HashMap<>();
        mShareBufferUseList.put(FRONT_CAMERA_ID, new ArrayList<YUVDataCallback>());
        mShareBufferUseList.put(BACK_CAMERA_ID, new ArrayList<YUVDataCallback>());

        mBufferBean = new BufferBean(PREVIEW_WIDTH*PREVIEW_HEIGHT*3/2,PREVIEW_WIDTH1*PREVIEW_HEIGHT1*3/2);
        shareBuffer = new ShareBuffer();

        mShareBufferSwitchList = new HashMap<>();
        mShareBufferSwitchList.put(FRONT_CAMERA_ID, false);
        mShareBufferSwitchList.put(BACK_CAMERA_ID, false);

        HandlerThread mHandlerThread = new HandlerThread("share_buffer");
        mHandlerThread.start();
        mShareBufferHandler = new ShareBufferHandler(mHandlerThread.getLooper());
        startReadBufferThread();
    }

    private synchronized void startReadBufferThread() {
        isReadBufferThreadRunning = true;
        if (mReadBufferThread == null) {
            mReadBufferThread = new Thread() {
                @Override
                public void run() {
                    while(isReadBufferThreadRunning) {
                        try {
                            if (isLooper) {
                                readShareBufferInternal();
                                if (mShareBufferUseList.get(FRONT_CAMERA_ID).size() == 0 && mShareBufferUseList.get(BACK_CAMERA_ID).size() == 0) {
                                    Thread.sleep(1000);
                                } else {
                                    Thread.sleep(20);
                                }
                            } else {
                                Thread.sleep(1000);
                            }

                            if (isLooper != lastLooperStatus) {
                                Log.w(TAG,"mReadBufferThread isLooper = " + isLooper +
                                        "; isAhdPluginIn = " + GpioManager.getInstance().isAhdPluginIn());
                            }

                            lastLooperStatus = isLooper;
                        } catch (Throwable t) {
                            t.printStackTrace();
                            Log.w(TAG,"mReadBufferThread Throwable = " + t.getMessage());
                        }
                    }
                }
            };
            mReadBufferThread.setName("QCThreadReadBuffer");
            ThreadPoolUtil.post(mReadBufferThread);
        }
    }

    public static ShareBufferManager getInstance() {
        if (shareBufferManager == null) {
            synchronized (ShareBufferManager.class) {
                if (shareBufferManager == null) {
                    shareBufferManager = new ShareBufferManager();
                }
            }
        }
        return shareBufferManager;
    }

    public void initShareBuffer() {
        LogUtils.getInstance().d(TAG, "initShareBuffer()");
        mShareBufferHandler.removeMessages(MSG_INIT_SHAREBUFFER);
        mShareBufferHandler.sendEmptyMessageDelayed(MSG_INIT_SHAREBUFFER,5000);
    }


    private void enableShareBuffer(int cameraId) {
        LogUtils.getInstance().d(TAG, "enableShareBuffer() cameraId = " + cameraId);
        shareBuffer.setShareBufferEnable(cameraId, true, isPreview);
        mShareBufferSwitchList.put(cameraId, true);
    }

    private void disableShareBuffer(int cameraId) {
        LogUtils.getInstance().d(TAG, "disableShareBuffer() cameraId = " + cameraId);
        shareBuffer.setShareBufferEnable(cameraId, false, isPreview);
        mShareBufferSwitchList.put(cameraId, false);
    }

    /**
     * 关闭所有子码流
     */
    public void closeShareBuffer() {
        LogUtils.getInstance().d(TAG, "closeShareBuffer()");
        disableShareBuffer(FRONT_CAMERA_ID);
        disableShareBuffer(BACK_CAMERA_ID);
        isLooper = false;
        isInit = false;
    }

    /**
     * 根据摄像头状态关闭子码流
     * @param cameraId
     */
    public void closeShareBuffer(int cameraId) {
        LogUtils.getInstance().d(TAG, "closeShareBuffer() cameraId = " + cameraId);
        disableShareBuffer(cameraId);
        if (isAllShareBufferClosed()) {
            isInit = false;
            isLooper = false;
        }
    }

    /**
     * 根据摄像头状态打开子码流
     * @param cameraId
     */
    public void openShareBuffer(final int cameraId) {
        LogUtils.getInstance().d(TAG, "openShareBuffer() cameraId = " + cameraId);
        ThreadPoolUtil.post(new Runnable() {
            @Override
            public void run() {
                initSharebufferInternal();
                if (mShareBufferUseList.get(FRONT_CAMERA_ID).size() > 0) {
                    enableShareBuffer(FRONT_CAMERA_ID);
                }
                if (mShareBufferUseList.get(BACK_CAMERA_ID).size() > 0) {
                    enableShareBuffer(BACK_CAMERA_ID);
                }
                if (!isAllShareBufferClosed()) {
                    isLooper = true;
                }
            }
        });
    }

    /**
     * 需要使用子码流的申请
     * @param cameraId
     * @param tag
     */
    public void addShareBufferRequest(final int cameraId, YUVDataCallback callback, String tag) {
        LogUtils.getInstance().d(TAG, "addShareBufferRequest() cameraId = " + cameraId + "; tag = " + tag);
        if (mShareBufferUseList.get(cameraId) != null && callback !=null) {
            mShareBufferUseList.get(cameraId).add(callback);
        }
        ThreadPoolUtil.post(new Runnable() {
            @Override
            public void run() {
                //initSharebufferInternal();
                if (!isLooper) {
                    isLooper = true;
                }
                enableShareBuffer(cameraId);
            }
        });
    }

    /**
     * 移除不需要使用子码流的申请，当所有子码流申请都被移除，则关闭子码流，关闭循环
     * @param cameraId
     * @param callback
     */
    public void removeShareBufferRequest(int cameraId, YUVDataCallback callback) {
        Log.e(TAG, "removeShareBufferRequest() start " );
        mShareBufferUseList.get(cameraId).remove(callback);

        int frontCount = mShareBufferUseList.get(FRONT_CAMERA_ID).size();
        if (frontCount == 0) {
            disableShareBuffer(FRONT_CAMERA_ID);
        }

        int backCount = mShareBufferUseList.get(BACK_CAMERA_ID).size();
        if (backCount == 0) {
            disableShareBuffer(BACK_CAMERA_ID);
        }

        if (frontCount == 0 && backCount == 0) {
            isLooper = false;
        }
        Log.e(TAG, "removeShareBufferRequest() frontCount = " + frontCount + "; backCount = " + backCount);
    }

    private boolean isAllShareBufferClosed() {
        for(Boolean isOpened : mShareBufferSwitchList.values()){
            if (isOpened) {
                return false;
            }
        }
        return true;
    }

    private void readShareBufferInternal() {
        try {
            if (shareBuffer != null) {
                shareBuffer.readBytes(mBufferBean.isCanRead, 0, 0, 1, FRONT_CAMERA_ID,isPreview);
                shareBuffer.readBytes(mBufferBean.isCanRead1, 0, 0, 1, BACK_CAMERA_ID,isPreview);
                LogUtils.getInstance().d(TAG, "readShareBufferInternal isCanRead1:" + mBufferBean.isCanRead1[0]);
                LogUtils.getInstance().d(TAG, "readShareBufferInternal isCanRead0:" + mBufferBean.isCanRead[0]);
                if (mBufferBean.isCanRead1[0] != 0) {
                    //LogUtils.getInstance().I(TAG, "readShareBufferInternal isCanRead1:" + mBufferBean.isCanRead1[0]);
                    shareBuffer.readBytes(mBufferBean.mBuffer1, FRAME_HEADER, 0, mBufferBean.mBuffer1.length, BACK_CAMERA_ID,isPreview);
                    if (mBufferBean.mBuffer1 != null) {
						/*LogUtils.getInstance().I(TAG, "readShareBufferInternal mBufferBean.mBuffer1:" + mBufferBean.mBuffer1
								+ ";backDataIndex=" + backDataIndex
								+ ";CameraService.SUPPORT_MAILIANBAO=" + CameraService.SUPPORT_MAILIANBAO);*/
                        List<YUVDataCallback> list = mShareBufferUseList.get(BACK_CAMERA_ID);
                        for (int i = list.size() - 1; i >= 0; i--) {
                            YUVDataCallback callback = list.get(i);
                            callback.processData(mBufferBean.mBuffer1, BACK_CAMERA_ID);
                        }
                    } else {
                        LogUtils.getInstance().d(TAG, "back camera null preview");
                    }
                }

                if (mBufferBean.isCanRead[0] != 0) {
                    shareBuffer.readBytes(mBufferBean.mBuffer0, FRAME_HEADER, 0, mBufferBean.mBuffer0.length, FRONT_CAMERA_ID,isPreview);
                    if (mBufferBean.mBuffer0 != null) {
						/*LogUtils.getInstance().I(TAG, "readShareBufferInternal mBufferBean.mBuffer0:" + mBufferBean.mBuffer0
								+ ";frontDataIndex=" + frontDataIndex
								+ ";CameraService.SUPPORT_MAILIANBAO=" + CameraService.SUPPORT_MAILIANBAO);*/
                        List<YUVDataCallback> list = mShareBufferUseList.get(FRONT_CAMERA_ID);
                        for (int i = list.size() - 1; i >= 0; i--) {
                            YUVDataCallback callback = list.get(i);
                            callback.processData(mBufferBean.mBuffer0, FRONT_CAMERA_ID);
                        }
                    } else {
                        LogUtils.getInstance().d(TAG, "front camera null preview");
                    }
                }
                mBufferBean.isCanRead[0] = 0;
                mBufferBean.isCanRead1[0] = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.getInstance().d(TAG, "err exception e = " + e.getMessage());
        }
    }

    private synchronized void initSharebufferInternal() {
//        if(!isInit){
//            try{
//                LogUtils.getInstance().d(TAG, "initSharebufferInternal() isInit = " + isInit);
//                shareBuffer.native_init(FRONT_CAMERA_ID, PREVIEW_WIDTH, PREVIEW_HEIGHT, zoomLevel, isPreview);
//                shareBuffer.native_init(BACK_CAMERA_ID, PREVIEW_WIDTH1, PREVIEW_HEIGHT1, zoomLevel, isPreview);
//                //shareBuffer.setShareBufferEnable(FRONT_CAMERA_ID, true, isPreview);
//                //shareBuffer.setShareBufferEnable(BACK_CAMERA_ID, true, isPreview);
//                isInit = true;
//            }catch(Exception e){
//                isInit =  false;
//                initShareBuffer();
//                LogUtils.getInstance().d(TAG, "native_init() error = "+e.getMessage());
//            }
//        }
            try{
                LogUtils.getInstance().d(TAG, "initSharebufferInternal() isInit = " + isInit);
                shareBuffer.native_init(FRONT_CAMERA_ID, PREVIEW_WIDTH, PREVIEW_HEIGHT, zoomLevel, isPreview);
                shareBuffer.native_init(BACK_CAMERA_ID, PREVIEW_WIDTH1, PREVIEW_HEIGHT1, zoomLevel, isPreview);
                //shareBuffer.setShareBufferEnable(FRONT_CAMERA_ID, true, isPreview);
                //shareBuffer.setShareBufferEnable(BACK_CAMERA_ID, true, isPreview);
                isInit = true;
            }catch(Exception e){
                isInit =  false;
                initShareBuffer();
                LogUtils.getInstance().d(TAG, "native_init() error = "+e.getMessage());
            }
    }
    public synchronized void takePicture() {
        LogUtils.getInstance().d(TAG, "takePicture()");
        if(ClickUtils.isFastDoubleClick()){
            return;
        }
        mShareBufferHandler.sendEmptyMessage(MSG_TAKE_PICTURE);
    }

    YUVDataCallback pictureCallback = new YUVDataCallback() {
        @Override
        public void processData(byte[] data, int cameraId) {
            final byte[] picture = data.clone();
            if (cameraId == FRONT_CAMERA_ID) {
                ThreadPoolUtil.post(new Runnable() {
                    @Override
                    public void run() {
                        saveYUV2Bitmap(picture);
                    }
                });
            } else if (cameraId == BACK_CAMERA_ID) {
                ThreadPoolUtil.post(new Runnable() {
                    @Override
                    public void run() {
                        saveYUV2Bitmap1(picture);
                    }
                });
            }
            removeShareBufferRequest(cameraId, pictureCallback);
        }
    };

    private void saveBitmap() {
        addShareBufferRequest(FRONT_CAMERA_ID, pictureCallback, "saveBitmap");
        if (GpioManager.getInstance().isAhdPluginIn()) {
            addShareBufferRequest(BACK_CAMERA_ID, pictureCallback, "saveBitmap");
        }
    }

    private void saveYUV2Bitmap(byte[] data) {
        LogUtils.getInstance().d(TAG, "saveYUV2Bitmap");
        if (!isLooper) {
            return;
        }
        ByteArrayOutputStream stream = null;
        try {
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, PREVIEW_WIDTH, PREVIEW_HEIGHT,null);
            LogUtils.getInstance().d(TAG, "readShareBufferMsg yuvImage:" + yuvImage);
            if (yuvImage != null) {
                stream = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT), 100, stream);
                Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                mPictureName = StorageUtils.generatePictureFileName(DvrService.CAMERA_FRONT_ID);
                File saveFile = new File(mPictureName);
                LogUtils.getInstance().d(TAG, "saveFile:" + saveFile);

                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(saveFile);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    if(null != mTakePictureCallback){
                        mTakePictureCallback.tackPicture(0,mPictureName);
                    }
                } catch (Exception e) {
                    LogUtils.getInstance().d("storageTakePictureBitmap", "error: " + e.getMessage());
                    e.printStackTrace();
                    if(null != mTakePictureCallback){
                        mTakePictureCallback.tackPicture(0,"");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(null != mTakePictureCallback){
                mTakePictureCallback.tackPicture(0,"");
            }
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

    private void saveYUV2Bitmap1(byte[] picture) {
        LogUtils.getInstance().d(TAG, "saveYUV2Bitmap");
        if (!isLooper) {
            return;
        }
        ByteArrayOutputStream stream = null;
        try {
            YuvImage yuvImage = new YuvImage(picture, ImageFormat.NV21, PREVIEW_WIDTH1, PREVIEW_HEIGHT1,null);
            LogUtils.getInstance().d(TAG, "readShareBufferMsg yuvImage:" + yuvImage);
            if (yuvImage != null) {
                stream = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, PREVIEW_WIDTH1, PREVIEW_HEIGHT1), 100, stream);
                Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.toByteArray().length);
                mPictureName = StorageUtils.generatePictureFileName(DvrService.CAMERA_BACK_ID);
                File saveFile = new File(mPictureName);
                LogUtils.getInstance().d(TAG, "saveFile:" + saveFile);

                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(saveFile);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream);
                    fileOutputStream.write(stream.toByteArray());
                    //fileOutputStream.flush();
                    fileOutputStream.close();
                    if(null != mTakePictureCallback){
                        mTakePictureCallback.tackPicture(1,mPictureName);
                    }
                } catch (Exception e) {
                    LogUtils.getInstance().d("storageTakePictureBitmap", "error: " + e.getMessage());
                    e.printStackTrace();
                    if(null != mTakePictureCallback){
                        mTakePictureCallback.tackPicture(1,"");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(null != mTakePictureCallback){
                mTakePictureCallback.tackPicture(1,"");
            }
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //String yuvName = StorageUtils.generateYUVFileName(DvrService.CAMERA_BACK_ID);
        //Log.e(TAG,"yuvname = "+yuvName);
        //writeFile(yuvName,picture);
    }

    public void writeFile(String path, byte[] data) {
        FileOutputStream out = null;
        try {
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists())
                parent.mkdirs();
            if (!file.exists()){
                boolean isok  =  file.createNewFile();
                out = new FileOutputStream(path);
                out.write(data);
                FileDescriptor fd = out.getFD();
                fd.sync();
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (Exception e) {
            }
        }
    }

    public interface YUVDataCallback {
        void processData(byte[] data, int cameraId);
    }

    private TakePictureCallback mTakePictureCallback;
    public void setTakePictureCallbackCallbackListener(TakePictureCallback mTakePictureCallback) {
        this.mTakePictureCallback = mTakePictureCallback;
    }
    public interface TakePictureCallback {
        void tackPicture(int cameraId, String pictureName);
    }
}

