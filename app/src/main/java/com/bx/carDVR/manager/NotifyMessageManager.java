package com.bx.carDVR.manager;

import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.autonavi.amapauto.gdarcameraservice.IGDCameraStateCallBack;
import com.autonavi.amapauto.gdarcameraservice.model.ArCameraOpenResultParam;
import com.bx.carDVR.util.LogUtils;
import com.bx.carDVR.util.SharedMemUtils;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NotifyMessageManager {

    private static final String TAG = "NotifyMessageManager";

    private IGDCameraStateCallBack iGDCameraStateCallBack;

    private MemoryFile memoryFile = null;
    private boolean isFirstData = true;
    private boolean isStartAr = false;

    /**
     * 共享文件头的长度
     */
    public static final int HEADER_SIZE = 20;
    /**
     * 共享内存的HEADER
     */
    private byte[] header = new byte[HEADER_SIZE];

    public static NotifyMessageManager getInstance() {
        return SingletonHolder.sInstance;
    }

    private static class SingletonHolder {
        private static final NotifyMessageManager sInstance = new NotifyMessageManager();
    }


    public void setGDCameraStateCallBack(IGDCameraStateCallBack gdCameraStateCallBack) {
        this.iGDCameraStateCallBack = gdCameraStateCallBack;
    }

    public IGDCameraStateCallBack getGDCameraStateCallBack(){
        return iGDCameraStateCallBack;
    }

    public void sendToGaoDeClosed(int code, String message) {
        if (iGDCameraStateCallBack != null) {
            try {
                iGDCameraStateCallBack.onClosed(code, message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendToGaoDeConnected() {
        if (iGDCameraStateCallBack != null) {
            try {
                iGDCameraStateCallBack.onConnected();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendToGaoDeDisconnected() {
        if (iGDCameraStateCallBack != null) {
            try {
                iGDCameraStateCallBack.onDisconnected();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendToGaoDeError(int code, String message) {
        if (iGDCameraStateCallBack != null) {
            try {
                iGDCameraStateCallBack.onError(code, message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void closeMemoryFile() {
        isStartAr = false;
        LogUtils.getInstance().d(TAG,"closeMemoryFile memoryFile = " + memoryFile);
        if (memoryFile != null) {
            memoryFile.close();
            memoryFile = null;
        }
    }

    public void sendToGaoDeOpened(byte[] bytes, ArCameraOpenResultParam arCameraOpenResultParam,
                                  String memoryfileName) {
        if (iGDCameraStateCallBack == null) {
            LogUtils.getInstance().e(TAG, "sendToGaoDeOpened: mGdCameraStateCallBack == null");
            return;
        }
        isStartAr = true;
        int size = bytes.length;
        FileDescriptor fd = null;
        SharedMemUtils.initHeader(header);
        try {
            if (memoryFile == null) {
                isFirstData = true;
                memoryFile = new MemoryFile(memoryfileName, size + HEADER_SIZE);
            }
            if (!SharedMemUtils.canWrite(header)) {
                LogUtils.getInstance().e(TAG, "sendToGaoDeOpened: cant not write");
                return;
            }
            if (isFirstData) {
                memoryFile.writeBytes(bytes, 0, HEADER_SIZE, size);
                SharedMemUtils.setCanRead(header);
                memoryFile.writeBytes(header, 0, 0, HEADER_SIZE);
                Method method = MemoryFile.class.getDeclaredMethod("getFileDescriptor");
                fd = (FileDescriptor) method.invoke(memoryFile);
                if (fd == null) {
                    Log.e(TAG, "sendToGaoDeOpened: mGdCameraStateCallBack == null");
                    return;
                }
                iGDCameraStateCallBack.onOpened(new ParcelFileDescriptor(fd), arCameraOpenResultParam,
                        memoryfileName);
                LogUtils.getInstance().d(TAG, "iGDCameraStateCallBack.onOpened");
                isFirstData = false;
            } else {
                memoryFile.writeBytes(bytes, 0, HEADER_SIZE, size);
                SharedMemUtils.setCanRead(header);
                memoryFile.writeBytes(header, 0, 0, HEADER_SIZE);
                LogUtils.getInstance().d(TAG, "memoryFile writeBytes");
            }
        } catch (RemoteException
                | InvocationTargetException
                | NoSuchMethodException
                | IllegalAccessException
                | IOException e) {
            e.printStackTrace();
            Log.e(TAG, "sendToGaoDeOpened: error : "+e.getMessage());
        }
    }

}
