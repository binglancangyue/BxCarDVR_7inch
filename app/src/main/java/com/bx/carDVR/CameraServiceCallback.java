package com.bx.carDVR;

public interface CameraServiceCallback {
    void mainHandlerSendMsg(int what);
    void mainHandlerSendMsgDelay(int what, int delay);
    void mainHandlerRemoveMsg(int what);
    void backHandlerSendMsg(int what);
    void backHandlerSendMsgDelay(int what, int delay);
    void backHandlerRemoveMsg(int what);
    boolean isCameraOpened(String cameraId);
    boolean isRecordStarted(String cameraId);
    void startRecord();
    void stopRecord();
}
