package com.bx.carDVR.app;

import android.app.Application;

public class DvrApplication extends Application {

    private static DvrApplication mApplication;
    private String uploadInfo;
    @Override
    public void onCreate() {
        super.onCreate();
        mApplication = this;
    }

    public static DvrApplication getDvrApplication() {
        return mApplication;
    }
    public void setUploadInfo(String info) {
        this.uploadInfo = info;
    }
    public String getUploadInfo() {
        return uploadInfo;
    }
}
