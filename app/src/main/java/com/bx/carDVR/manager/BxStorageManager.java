package com.bx.carDVR.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.SystemProperties;
import android.text.TextUtils;

import com.bx.carDVR.CameraServiceCallback;
import com.bx.carDVR.DvrService;
import com.bx.carDVR.util.LogUtils;
import com.bx.carDVR.util.StorageUtils;
import com.bx.carDVR.util.ThreadPoolUtil;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Comparator;

public class BxStorageManager {

    private static final String TAG = "QcStorageManager";

    private static final int STATE_SDCARD_MOUNTED = 1;
    private static final int STATE_SDCARD_EJECT = 2;

    private CameraServiceCallback mCameraServiceCallback;
    private boolean isFormatFlag = false;
    private int checkCount = 0;
    private boolean isChecking;

    private BroadcastReceiver sdcardReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtils.getInstance().d(TAG, "sdcardReceiver onReceive() action = " + action);
            if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                onSdcardStateChanged(STATE_SDCARD_MOUNTED, intent);
            } else if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
                onSdcardStateChanged(STATE_SDCARD_EJECT, intent);
            }
        }
    };

    private Thread mCheckingSpaceThread = new Thread() {
        @Override
        public void run() {
            while (isChecking) {
                try {
                    checkStorageSpace();
                    Thread.sleep(2 * 60 * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public BxStorageManager(CameraServiceCallback cb) {
        LogUtils.getInstance().d(TAG, "BxStorageManager init");
        mCameraServiceCallback = cb;
        startCheckingSpaceThread();
    }

    private void startCheckingSpaceThread() {
        isChecking = true;
        mCheckingSpaceThread.setName("BxThreadCheckingSpace");
        ThreadPoolUtil.post(mCheckingSpaceThread);
    }

    public void register(Context context) {
        IntentFilter sdcardFilter = new IntentFilter();
        sdcardFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        sdcardFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        sdcardFilter.addDataScheme("file");
        context.registerReceiver(sdcardReceiver, sdcardFilter);
    }

    public void unregister(Context context) {
        context.unregisterReceiver(sdcardReceiver);
    }

    private void onSdcardStateChanged(int state, Intent intent) {
        final Uri uri = intent.getData();
        String path = uri.getPath();
        LogUtils.getInstance().d(TAG, "onSdcardStateChanged() path" + path + ", state = " + state);
        checkCount = 0;
        if (state == STATE_SDCARD_MOUNTED) {
//        if(CameraService.isSystemSleep){
//            LogUtils.getInstance().d(TAG,"onSdcardStateChanged() system sleeped! ignore.");
//            return;
//        }
            mCameraServiceCallback.backHandlerSendMsg(DvrService.MSG_CHECK_SD_CARD_STATUS);
            if (!isFormatFlag) {
                boolean isSdCardMount = StorageUtils.getInstance().isSDCardMounted();
                LogUtils.getInstance().d(TAG, "onSdcardStateChanged() isSdCardMount=" + isSdCardMount);
                if (isSdCardMount) {
                    mCameraServiceCallback.startRecord();
                }
            }
        } else if (state == STATE_SDCARD_EJECT) {
            StorageUtils.getInstance().hideUnmountTips();
            mCameraServiceCallback.backHandlerRemoveMsg(DvrService.MSG_CHECK_SD_CARD_STATUS);
            mCameraServiceCallback.backHandlerRemoveMsg(DvrService.MSG_DELETE_FILE_WHEN_UNMOUNT);
            mCameraServiceCallback.stopRecord();
        }
    }

    public void checkSdCardStatus() {
        LogUtils.getInstance().d(TAG, "checkSdCardStatus() checkCount=" + checkCount);
        if (checkCount < DvrService.CHECK_SD_CARD_STATUS_COUNT) {
            File file = null;
            FileReader reader = null;
            try {
                file = new File(DvrService.PATH_CHECK_SD_CARD_STATUS);
                if (file.exists()) {
                    reader = new FileReader(file);
                    char[] chars = new char[10];
                    if (reader.read(chars) > 0) {
                        String errors = new String(chars).trim();
                        LogUtils.getInstance().d(TAG, "checkSdCardStatus() errors=" + errors);
                        if (!TextUtils.isEmpty(errors) && "mmcblk1".equals(errors)) {
                            mCameraServiceCallback.mainHandlerSendMsg(DvrService.MSG_NOTIFY_SD_CARD_ERROR);
                        } else {
                            mCameraServiceCallback.backHandlerSendMsgDelay(DvrService.MSG_CHECK_SD_CARD_STATUS, DvrService.DELAY_TIME_CHECK_SD_CARD_STATUS);
                            checkCount++;
                        }
                    }
                }
            } catch (Exception e) {
                LogUtils.getInstance().d(TAG, "checkSdCardStatus() error=" + e.getMessage());
            } finally {
                try {
                    if (null != reader) {
                        reader.close();
                        reader = null;
                    }
                } catch (Exception e2) {
                }
            }
        } else {
            LogUtils.getInstance().d(TAG, "checkSdCardStatus() no errors,check end.");
        }
    }

    private void checkStorageSpace() {
        boolean sdMounted = StorageUtils.getInstance().isSDCardMounted();
        LogUtils.getInstance().d(TAG, "checkStorageSpace() sdMounted = " + sdMounted);
        if (sdMounted) {
            long totalSpace = StorageUtils.getTotalSpace();
            long availableSpace = StorageUtils.getAvailableSpace();
            LogUtils.getInstance().d(TAG, "total = " + (totalSpace / 1024 / 1024) + "M ; available = " + (availableSpace / 1024 / 1024) + "M");
            if (availableSpace < StorageUtils.RECORD_VIDEO_NEED_SPACE) {
                deleteFileWhenNoSpace(availableSpace, StorageUtils.DIRECTORY_VIDEO);
                availableSpace = StorageUtils.getAvailableSpace();
                LogUtils.getInstance().d(TAG, "total = " + (totalSpace / 1024 / 1024) + "M ; available = " + (availableSpace / 1024 / 1024) + "M");
                if (availableSpace < StorageUtils.RECORD_VIDEO_NEED_SPACE) {
//                if(!SystemProperties.getBoolean("persist.device.is.no.screen", false)){
//                    deleteFileWhenNoSpace(availableSpace, StorageUtils.DIRECTORY_LOCK_VIDEO);
//                }
                    deleteFileWhenNoSpace(availableSpace, StorageUtils.DIRECTORY_LOCK_VIDEO);
                    availableSpace = StorageUtils.getAvailableSpace();
                    LogUtils.getInstance().d(TAG, "total = " + (totalSpace / 1024 / 1024) + "M ; available = " + (availableSpace / 1024 / 1024) + "M");
                    if (!StorageUtils.hasEnoughSpaceAfterClear()) {
                        LogUtils.getInstance().d(TAG, "no more space to record: check again!");
                        mCameraServiceCallback.mainHandlerSendMsgDelay(DvrService.MSG_STOP_RECORD_DURE_TO_SPACE, 3 * 1000);
                    }
                }
            }
        }
    }

    private void deleteFileWhenNoSpace(long availableSpace, String path) {
        File dirFile = new File(path);
        File[] subFiles = dirFile.listFiles();
        if (subFiles != null) {
            Arrays.sort(subFiles, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if (o1 == null || o2 == null || o2.getName() == null || o1.getName() == null) {
                        return -1;
                    }
                    return o1.getName().compareTo(o2.getName());
                }
            });
        }
        long recycledSize = 0;
        if (subFiles != null && subFiles.length > 0) {
            //LogUtils.getInstance().I(TAG,"subFiles size= " + subFiles.length);
            for (File f : subFiles) {
                if (f.getName() == null || f.getPath() == null) {
                    continue;
                }
                if (f.getName().endsWith(StorageUtils.VIDEO_FORMAT_MP4)
                        || f.getName().endsWith(StorageUtils.VIDEO_FORMAT_TS)) {
                    if (f.exists()) {
                        recycledSize += f.length();
                        f.delete();
                    }
                }
                LogUtils.getInstance().d(TAG, path + ":" + f.getName() + " recycledSize = " + (recycledSize / 1024 / 1024) + " M");
                if ((recycledSize + availableSpace) >= StorageUtils.DELETE_FILE_NEED_SPACE) {
                    //LogUtils.getInstance().I(TAG,"we have enough space.");
                    break;
                }
            }
        }
    }

    public boolean isFormatFlag() {
        return isFormatFlag;
    }

    public void setFormatFlag(boolean formatFlag) {
        isFormatFlag = formatFlag;
    }

    public void start() {
        LogUtils.getInstance().d(TAG, "start");
        startCheckingSpaceThread();
    }

    public void stop() {
        LogUtils.getInstance().d(TAG, "stop");
        isChecking = false;
    }

}
