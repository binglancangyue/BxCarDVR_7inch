// IGDCameraStateCallBack.aidl
package com.autonavi.amapauto.gdarcameraservice;

import android.os.ParcelFileDescriptor;
import com.autonavi.amapauto.gdarcameraservice.model.ArCameraOpenResultParam;

interface IGDCameraStateCallBack{

    void onConnected();


    void onDisconnected();


    void onOpened(in ParcelFileDescriptor parcelFileDescriptor, in ArCameraOpenResultParam arCameraOpenResultParam, String memoryfileName);


    void onClosed(int code, String message);


    void onError(int code, String message);

}