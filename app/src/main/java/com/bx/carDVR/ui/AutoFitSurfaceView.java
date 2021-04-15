package com.bx.carDVR.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.bx.carDVR.R;
import com.bx.carDVR.bean.Configuration;

public class AutoFitSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "BxAutoFitSurfaceView";
    private boolean isSurfaceReady = false;
    public static final float STANDARD_RATIO = 16f / 9;
    private static final int PREVIEW_HIDE_LEFT_WIDTH = 20;//px
    private String mCameraId;
    private int mRequestWidth = 1;
    private int mRequestHeight = 1;

    public AutoFitSurfaceView(Context context) {
        super(context);
        initData();
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initData();
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData();
    }

    private void initData() {
        getHolder().addCallback(this);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        if (Configuration.IS_3IN) {
            getHolder().setFixedSize(Configuration.PREVIEW_WIDTH_720P,Configuration.PREVIEW_HEIGHT_720P);
        } else {
//            if (getId() == R.id.back_preview) {
//                getHolder().setFixedSize(Configuration.VIDEO_WIDTH_720P,Configuration.VIDEO_HEIGHT_720P);
//            } else if (getId() == R.id.front_preview) {
//                getHolder().setFixedSize(Configuration.VIDEO_WIDTH_1080P,Configuration.VIDEO_HEIGHT_1080P);
//            }
            getHolder().setFixedSize(Configuration.PREVIEW_WIDTH_720P,Configuration.PREVIEW_HEIGHT_720P);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        isSurfaceReady = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        isSurfaceReady = false;
    }

    public boolean isSurfaceReady() {
        return isSurfaceReady;
    }

    public void bindCameraId(String mCameraId) {
        this.mCameraId = mCameraId;
    }

    public Surface getSurface() {
        return getHolder().getSurface();
    }

    public void requestSize(int width, int height){
        mRequestWidth = width;
        mRequestHeight = height;
    }
}
