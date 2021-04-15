package com.bx.carDVR.adas;

import android.content.Context;
import android.view.View;

import com.bx.carDVR.ui.FloatPreviewWindowCallback;

public interface IAdas {
    void init(FloatPreviewWindowCallback callback, Context mContext, View view,
              ConcreteAdasFactory.AdasActivatedCallback adasActivatedCallback);
    void reInit(ConcreteAdasFactory.AdasActivatedCallback adasActivatedCallback);
    void setEnable(boolean value);
    boolean isEnable();
    boolean isActive();
    void register();
    void unRegister();
    void check();
    void start();
    void stop();
    void handClick();
    void activeInterface();
    void loadAudioResource();
    void release();
}
