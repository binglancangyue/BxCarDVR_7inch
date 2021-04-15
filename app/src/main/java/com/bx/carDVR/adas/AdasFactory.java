package com.bx.carDVR.adas;

public abstract class AdasFactory {

    public abstract void build();

    public abstract IAdas getAdas();

    public abstract void setAdasEnable(boolean value);

    public abstract boolean isAdasEnable();

    public abstract boolean isAdasActive();

    public abstract void register();

    public abstract void unRegister();

    public abstract void check();

    public abstract void start();

    public abstract void stop();

    public abstract void handClick();

    public abstract void activeInterface();

    public abstract void loadAudioResource();

    public abstract void release();
}
