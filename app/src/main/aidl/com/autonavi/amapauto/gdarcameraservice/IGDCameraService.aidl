// IGDCameraService.aidl
package com.autonavi.amapauto.gdarcameraservice;

import com.autonavi.amapauto.gdarcameraservice.IGDCameraStateCallBack;
import android.view.Surface;
import com.autonavi.amapauto.gdarcameraservice.IGDSize;
import com.autonavi.amapauto.gdarcameraservice.model.GDArCameraParam;

interface IGDCameraService {
    boolean registerCameraStateCallback(String clientId,IGDCameraStateCallBack gdCameraStateCallBack);

    boolean unregisterCameraStateCallback(String clientId,IGDCameraStateCallBack gdCameraStateCallBack);

    boolean isSupportArNavi(String clientId);

    IGDSize getRecommendSize(String clientId);

    boolean isCameraConnected(String clientId);

    boolean isCameraOpened(String clientId);

    boolean initCamera(String clientId,in GDArCameraParam gdArCameraParam, in Surface surface);

    boolean openCamera(String clientId);

    boolean closeCamera(String clientId);

    boolean unInitCamera(String clientId);
}
