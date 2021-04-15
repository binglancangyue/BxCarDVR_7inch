package com.bx.carDVR.util;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemProperties;


import android.app.Application;
import android.nfc.FormatException;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;

import com.bx.carDVR.DvrService;
import com.bx.carDVR.app.DvrApplication;

public class StorageUtils {

    private static final String TAG = "BxStorageUtils";
    private static final boolean DEBUG = true;
    private static StorageUtils storageUtils;
    private static Application application;
    public static final String BASE_PATH = "/storage/sdcard0";
    public static final String VIDEO_DIR = "/DVR-BX/Video";
    public static final String VIDEO_DIR_ECAR = "/storage/sdcard0/ECAR/Video";
    public static final String LOCK_VIDEO_DIR = "/DVR-BX/LockVideo";
    public static final String PICTURE_DIR = "/DVR-BX/Picture";
    public static final String PICTURE_DIR_ECAR = "/storage/sdcard0/ECAR/Picture";
    public static final String THUMBNAIL_PICTURE_DIR = "/DVR-BX/.thumbnail";
    public static final String VIDEO_INFO_FILE_NAME = BASE_PATH +"/DVR-BX/.videoinfo";
    public static final String DIRECTORY_VIDEO = BASE_PATH + VIDEO_DIR;
    public static final String DIRECTORY_LOCK_VIDEO = BASE_PATH + LOCK_VIDEO_DIR;
    public static final String DIRECTORY_PICTURE = BASE_PATH + PICTURE_DIR;
    public static final String DIRECTORY_THUMBNAIL_PICTURE = BASE_PATH + THUMBNAIL_PICTURE_DIR;
    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;
    public static final long LOW_STORAGE_THRESHOLD_BYTES = 50000000;
    private static final long SPACE_OF_1G = 1024 * 1024 * 1024;
    public static final long RECORD_VIDEO_NEED_SPACE = (long) (1.6 * SPACE_OF_1G);
    public static final long DELETE_FILE_NEED_SPACE = (2 * SPACE_OF_1G);
    private static final long RECORD_VIDEO_RECORD_SPACE = (long) (1.2 * SPACE_OF_1G);
    public static final String VIDEO_FORMAT_MP4 = ".mp4";
    public static final String VIDEO_FORMAT_TS = ".ts";
    public static final boolean IS_MP4_FORMAT = true;
    public static final String VIDEO_FORMAT = IS_MP4_FORMAT ? VIDEO_FORMAT_MP4 : VIDEO_FORMAT_TS;
    private static SimpleDateFormat formatVideo = new SimpleDateFormat("yyyyMMddHHmmss");
    private static SimpleDateFormat formatPicture = new SimpleDateFormat("yyyyMMddHHmmss");
    private static SimpleDateFormat formatPictureEcar = new SimpleDateFormat("yyyyMMddHHmmssSSSS");
    protected StorageManager mStorage;
    protected VolumeInfo mVolume;
    protected DiskInfo mDisk;
    private PartitionTask mTask;
    private UnmountTask mUnmountTask;
    private boolean isRegistStorageListener = false;
    private boolean isFormating = false;
    private static final int MSG_CHECK_UNSUPPORT_DISK = 1;
    private static final int MSG_SHOW_FORMAT_PROGRESS_DIALOG = 2;
    private static final int MSG_HIDE_FORMAT_PROGRESS_DIALOG = 3;
    private static final int MSG_SHOW_FORMAT_DIALOG = 4;
    private static final int MSG_SHOW_UNMOUNT_DIALOG = 5;
    private static final int MSG_HIDE_UNMOUNT_DIALOG = 6;
    private StorageHandler mHandler;
    public class StorageHandler extends Handler{
        public StorageHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_CHECK_UNSUPPORT_DISK:
                    initUnsupportDisk();
                    break;
                case MSG_SHOW_FORMAT_PROGRESS_DIALOG:
                   // FormatProgressDialog.getInstance().showFormatSdcardDialog(application);
                    break;
                case MSG_HIDE_FORMAT_PROGRESS_DIALOG:
                    //FormatProgressDialog.getInstance().hideFormatSdcardDialog();
                    break;
                case MSG_SHOW_FORMAT_DIALOG:
//                    mDisk = unsupportDisks.get(0);
//                    if(null == mFormatConfirmDialog){
//                        mFormatConfirmDialog = FormatConfirmDialog.getInstance();
//                    }
//                    mFormatConfirmDialog.setFormatConfirmListener(StorageUtils.this);
//                    mFormatConfirmDialog.showConfirmDialog(application);
                    break;
                case MSG_SHOW_UNMOUNT_DIALOG:
//                    if(null == mUnmountConfirmDialog){
//                        mUnmountConfirmDialog = UnmountConfirmDialog.getInstance();
//                    }
//                    mUnmountConfirmDialog.setUnmountConfirmListener(StorageUtils.this);
//                    mUnmountConfirmDialog.showConfirmDialog(application);
                    break;
                case MSG_HIDE_UNMOUNT_DIALOG:
//                    if(null != mUnmountConfirmDialog){
//                        mUnmountConfirmDialog.setUnmountConfirmListener(null);
//                        mUnmountConfirmDialog = null;
//                    }
                    break;
                default:
                    break;
            }
        }
    }

    private StorageUtils() {
        application = DvrApplication.getDvrApplication();
        mStorage = application.getSystemService(StorageManager.class);

        HandlerThread mHandlerThread = new HandlerThread("StorageUtils");
        mHandlerThread.start();
        mHandler = new StorageHandler(mHandlerThread.getLooper());
    }

    public static StorageUtils getInstance() {
        if (null == storageUtils) {
            storageUtils = new StorageUtils();
        }
        return storageUtils;
    }

    public void formatSdCard(){
        if(isFormating){
            return;
        }
        isFormating = true;
        final List<VolumeInfo> volumes = mStorage.getVolumes();
        String volumeId;
        for (VolumeInfo vol : volumes) {
            LogUtils.getInstance().d(TAG, vol.toString());
            if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                LogUtils.getInstance().d(TAG, "formatSdCard() TYPE_PRIVATE ");
            }else if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                mVolume = vol;
                if (mVolume.isMountedReadable()) {
                    long totalBytes = 0;
                    final File path = mVolume.getPath();
                    totalBytes = path.getTotalSpace();
                    final long freeBytes = path.getFreeSpace();
                    final long usedBytes = totalBytes - freeBytes;
                    final String used = Formatter.formatFileSize(application, usedBytes);
                    final String total = Formatter.formatFileSize(application, totalBytes);
                    LogUtils.getInstance().d(TAG, "totalBytes = "+totalBytes+",usedBytes = "+usedBytes);
                    LogUtils.getInstance().d(TAG, "total = "+total+",used = "+used);
                }
                break;
            }
        }
        isFormating = false;
        if (mVolume != null) {
            mDisk = mVolume.getDisk();
            if (mDisk != null) {
                mStorage.registerListener(mStorageListener);
                isRegistStorageListener = true;
                isFormating = true;
    			if(null != onSdcardFormatStateChangeListener){
    				onSdcardFormatStateChangeListener.formateStarted();
    			}
                mHandler.sendEmptyMessage(MSG_SHOW_FORMAT_PROGRESS_DIALOG);
                mHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (mTask == null) {
                            mTask = new PartitionTask();
                            mTask.execute();
                        }
                    }
                }, 3 * 1000);
            }
        }
    }

    public class PartitionTask extends AsyncTask<Void, Integer, Exception> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            LogUtils.getInstance().d(TAG, "onPreExecute()");
            mHandler.sendEmptyMessage(MSG_SHOW_FORMAT_PROGRESS_DIALOG);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            LogUtils.getInstance().d(TAG, "format sdcard doInBackground...");
            try {
                mStorage.partitionPublic(mDisk.getId());
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception result) {
            isFormating = false;
            if (result != null) {
                LogUtils.getInstance().d(TAG, "Failed to partition "+result.getMessage());
                //FloatToast.makeText(result.getMessage(), FloatToast.LENGTH_LONG).show();
                if(null != onSdcardFormatStateChangeListener){
					onSdcardFormatStateChangeListener.formateFinished(false);
				}
            }else{
                LogUtils.getInstance().d(TAG, "success to partition!");
                //FloatToast.makeText(R.string.format_sdcard_success, FloatToast.LENGTH_LONG).show();
				if(null != onSdcardFormatStateChangeListener){
					onSdcardFormatStateChangeListener.formateFinished(true);
				}
            }
            if(null != mTask){
                mTask = null;
            }
            mHandler.sendEmptyMessage(MSG_HIDE_FORMAT_PROGRESS_DIALOG);
        }
    }

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onDiskDestroyed(DiskInfo disk) {
            // We know mDisk != null.
            if (mDisk.id.equals(disk.id)) {
                LogUtils.getInstance().d(TAG, "onDiskDestroyed()");
                if(isRegistStorageListener){
                    isFormating = false;
                    mStorage.unregisterListener(mStorageListener);
                }
            }
        }
    };

    public class UnmountTask extends AsyncTask<Void, Void, Exception> {

        public UnmountTask() {

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            LogUtils.getInstance().d(TAG, "UnmountTask onPreExecute()");
            //FloatToast.makeText(R.string.storage_unmount_start, FloatToast.LENGTH_SHORT).show();
        }

        @Override
        protected Exception doInBackground(Void... params) {
            LogUtils.getInstance().d(TAG, "UnmountTask doInBackground()");
            try {
                final List<VolumeInfo> volumes = mStorage.getVolumes();
                VolumeInfo mVolume = null;
                String volumeId;
                for (VolumeInfo vol : volumes) {
                    LogUtils.getInstance().d(TAG, vol.toString());
                    if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                        mVolume = vol;
                        break;
                    }
                }
                if(null != mVolume){
                    volumeId = mVolume.getId();
                    LogUtils.getInstance().d(TAG, "volumeId="+volumeId);
                    mStorage.unmount(volumeId);
                }
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            LogUtils.getInstance().d(TAG, "UnmountTask onPostExecute()");
            if (e == null) {
                //FloatToast.makeText(R.string.storage_unmount_success, FloatToast.LENGTH_SHORT).show();
            } else {
                LogUtils.getInstance().d(TAG, "Failed to unmount "+ e.getMessage());
               // FloatToast.makeText(R.string.storage_unmount_failure, FloatToast.LENGTH_SHORT).show();
            }
            if(null != mUnmountTask){
                mUnmountTask = null;
            }
        }
    }


    public boolean isSDCardMounted() {
        String state = Environment.getStorageState(new File(BASE_PATH));
        File dir = new File(BASE_PATH);
        if (!dir.exists()) {
            return false;
        }
        if (Environment.MEDIA_MOUNTED.equals(state) && dir.isDirectory() && dir.canWrite()) {
            LogUtils.getInstance().d(TAG, "External storage state=" + state);
            return true;
        }
        return false;
    }

    public static boolean hasEnoughSpaceAfterClear(){
        return getAvailableSpace() > RECORD_VIDEO_RECORD_SPACE;
    }

    public static long getAvailableSpace() {
        String state = Environment.getStorageState(new File(BASE_PATH));
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return UNAVAILABLE;
        }
        File dir = new File(BASE_PATH);
        if (!dir.exists()) {
            return UNAVAILABLE;
        }
        if (!dir.isDirectory() || !dir.canWrite()) {
            return UNAVAILABLE;
        }
        try {
            StatFs stat = new StatFs(BASE_PATH);
            return stat.getAvailableBlocksLong() * (long) stat.getBlockSizeLong();
        } catch (Exception e) {
            LogUtils.getInstance().d(TAG, "Fail to access external storage "+e.getMessage());
        }
        return UNKNOWN_SIZE;
    }

    public static long getTotalSpace() {
        String state = Environment.getStorageState(new File(BASE_PATH));
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return UNAVAILABLE;
        }
        File dir = new File(BASE_PATH);
        if (!dir.exists()) {
            return UNAVAILABLE;
        }
        if (!dir.isDirectory() || !dir.canWrite()) {
            return UNAVAILABLE;
        }
        try {
            StatFs stat = new StatFs(BASE_PATH);
            return stat.getBlockCountLong() * (long) stat.getBlockSizeLong();
        } catch (Exception e) {
            LogUtils.getInstance().d(TAG, "Fail to access external storage "+e.getMessage());
        }
        return UNKNOWN_SIZE;
    }

    public static String generateVideoFileName(String mCameraId) {
        File videoFile = new File(DIRECTORY_VIDEO);
        if (!videoFile.exists()) {
            if(videoFile.mkdirs()){
                if(DEBUG) LogUtils.getInstance().d(TAG, "create video folder success! path = " + DIRECTORY_VIDEO);
            }else{
                LogUtils.getInstance().d(TAG, "create video folder failed! path = " + DIRECTORY_VIDEO);
            }
        }
        String cameraType = "_default";
        if (DvrService.CAMERA_FRONT_ID.equals(mCameraId)) {
            cameraType = "_front";
        } else if (DvrService.CAMERA_BACK_ID.equals(mCameraId)) {
            cameraType = "_back";
        }
        return DIRECTORY_VIDEO + "/"+ generateVideoDate() + cameraType  + VIDEO_FORMAT;
    }

    public static String generatePictureFileName(String mCameraId) {
        File pictureFile = new File(DIRECTORY_PICTURE);
        if (!pictureFile.exists()) {
            if(pictureFile.mkdirs()){
                if(DEBUG) LogUtils.getInstance().d(TAG, "create picture folder success! path = " + DIRECTORY_PICTURE);
            }else{
                LogUtils.getInstance().d(TAG, "create picture folder failed! path = " + DIRECTORY_PICTURE);
            }
        }
        String cameraType = "_default";
        if (DvrService.CAMERA_FRONT_ID.equals(mCameraId)) {
            cameraType = "_front";
        } else if (DvrService.CAMERA_BACK_ID.equals(mCameraId)) {
            cameraType = "_back";
        }
        return DIRECTORY_PICTURE + "/"+ generatePictureDate() + cameraType  + ".jpg";
    }

    public static String generateECarVideoFileName(String mCameraId) {
        File videoFile = new File(VIDEO_DIR_ECAR);
        if (!videoFile.exists()) {
            if(videoFile.mkdirs()){
                if(DEBUG) LogUtils.getInstance().d(TAG, "create video folder success! path = " + DIRECTORY_VIDEO);
            }else{
                LogUtils.getInstance().d(TAG, "create video folder failed! path = " + DIRECTORY_VIDEO);
            }
        }
        String cameraType = "_default";
        if (DvrService.CAMERA_FRONT_ID.equals(mCameraId)) {
            cameraType = "_front";
        } else if (DvrService.CAMERA_BACK_ID.equals(mCameraId)) {
            cameraType = "_back";
        }
        return VIDEO_DIR_ECAR + "/"+ generateVideoDate() + cameraType  + VIDEO_FORMAT;
    }

    public static String generateECarPictureFileName(String mCameraId) {
        File pictureFile = new File(PICTURE_DIR_ECAR);
        if (!pictureFile.exists()) {
            if(pictureFile.mkdirs()){
                if(DEBUG) LogUtils.getInstance().d(TAG, "create picture folder success! path = " + DIRECTORY_PICTURE);
            }else{
                LogUtils.getInstance().d(TAG, "create picture folder failed! path = " + DIRECTORY_PICTURE);
            }
        }
        String cameraType = "_default";
        if (DvrService.CAMERA_FRONT_ID.equals(mCameraId)) {
            cameraType = "_front";
        } else if (DvrService.CAMERA_BACK_ID.equals(mCameraId)) {
            cameraType = "_back";
        }
        return PICTURE_DIR_ECAR + "/"+ generatePictureDate() + cameraType  + ".jpg";
    }

    public static String generateYUVFileName(String mCameraId) {
        File pictureFile = new File(DIRECTORY_PICTURE);
        if (!pictureFile.exists()) {
            if(pictureFile.mkdirs()){
                if(DEBUG) LogUtils.getInstance().d(TAG, "create picture folder success! path = " + DIRECTORY_PICTURE);
            }else{
                LogUtils.getInstance().d(TAG, "create picture folder failed! path = " + DIRECTORY_PICTURE);
            }
        }
        String cameraType = "_default";
        if (DvrService.CAMERA_FRONT_ID.equals(mCameraId)) {
            cameraType = "_front";
        } else if (DvrService.CAMERA_BACK_ID.equals(mCameraId)) {
            cameraType = "_back";
        }
        return DIRECTORY_PICTURE + "/"+ generatePictureDate() + cameraType  + ".yuv";
    }

    public static String generateThumbnailFileName(String fileName){
        File thumbnailFile = new File(DIRECTORY_THUMBNAIL_PICTURE);
        if(!thumbnailFile.exists()){
            if(thumbnailFile.mkdirs()){
                if(DEBUG) LogUtils.getInstance().d(TAG, "create thumbnail folder success! path = " + DIRECTORY_THUMBNAIL_PICTURE);
            }
        }
        return DIRECTORY_THUMBNAIL_PICTURE + "/" + fileName + "_T.jpg";
    }

    private static String generateVideoDate() {
        return formatVideo.format(new Date()).toString();
    }

    public static String generatePictureDate() {
        return formatPicture.format(new Date()).toString();
    }

    public static String generatePictureDateEcar() {
        return formatPictureEcar.format(new Date()).toString();
    }

    private OnSdcardFormatStateChangeListener onSdcardFormatStateChangeListener;

    public void setOnSdcardFormatStateChangeListener(OnSdcardFormatStateChangeListener onSdcardFormatStateChangeListener){
        this.onSdcardFormatStateChangeListener = onSdcardFormatStateChangeListener;
    }

    public interface OnSdcardFormatStateChangeListener{
        void formateStarted();
        void formateFinished(boolean success);
    }

    public void checkUnSupportSdCard(){
        mHandler.sendEmptyMessage(MSG_CHECK_UNSUPPORT_DISK);
    }

    // Show unsupported disks to give a chance to init
    List<DiskInfo> unsupportDisks = new ArrayList<DiskInfo>();
    public void initUnsupportDisk(){
        unsupportDisks.clear();
        final List<VolumeInfo> volumes = mStorage.getVolumes();
        Collections.sort(volumes, VolumeInfo.getDescriptionComparator());
        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                DiskInfo disk = mStorage.findDiskById(vol.getDiskId());
                if(null != disk){
                    LogUtils.getInstance().d(TAG, "unsupportDisks " + disk.toString());
                    unsupportDisks.add(disk);
                }
            }
        }
        LogUtils.getInstance().d(TAG, "unsupportDisks size = " + unsupportDisks.size());
        if(unsupportDisks.size() == 0){
            final List<DiskInfo> disks = mStorage.getDisks();
            for (DiskInfo disk : disks) {
                LogUtils.getInstance().d(TAG, "---" + disk.toString());
                if (disk.volumeCount == 0 && disk.size > 0) {
                    unsupportDisks.add(disk);
                }
            }
        }
        mHandler.removeMessages(MSG_SHOW_FORMAT_DIALOG);
        if(unsupportDisks.size() > 0){
            mHandler.sendEmptyMessageDelayed(MSG_SHOW_FORMAT_DIALOG, 500);
        }else{
            //FloatToast.makeText(R.string.please_insert_sdcard, FloatToast.LENGTH_SHORT).show();
        }
    }

    public void showUnmountTips(){
        LogUtils.getInstance().d(TAG, "showUnmountTips()");
        mHandler.sendEmptyMessage(MSG_SHOW_UNMOUNT_DIALOG);
    }

    public void hideUnmountTips(){
        LogUtils.getInstance().d(TAG, "hideUnmountTips()");
        mHandler.sendEmptyMessage(MSG_HIDE_UNMOUNT_DIALOG);
    }

//    @Override
//    public void onFormatConfirmed() {
//        if (mTask == null) {
//            mTask = new PartitionTask();
//            mTask.execute();
//        }
//    }

//    @Override
//    public void onFormatCanceled() {
//        if(null != mFormatConfirmDialog){
//            mFormatConfirmDialog.setFormatConfirmListener(null);
//            mFormatConfirmDialog = null;
//        }
//    }
//
//    @Override
//    public void onUnmountConfirmed() {
//        if(mUnmountTask == null){
//            mUnmountTask = new UnmountTask();
//            mUnmountTask.execute();
//        }
//    }
//
//    @Override
//    public void onUnmountCanceled() {
//        if(null != mUnmountConfirmDialog){
//            mUnmountConfirmDialog.setUnmountConfirmListener(null);
//            mUnmountConfirmDialog = null;
//        }
//    }
}
