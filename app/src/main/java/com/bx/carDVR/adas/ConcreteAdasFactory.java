package com.bx.carDVR.adas;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import com.bx.carDVR.ui.FloatPreviewWindowCallback;
import com.bx.carDVR.util.LogUtils;

public class ConcreteAdasFactory extends AdasFactory{
    private static final String TAG = "ConcreteAdasFactory";

    private IAdas mIAdas;
    private static ConcreteAdasFactory sConcreteAdasFactory;

    private View mRootView;
    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private FloatPreviewWindowCallback mFloatPreviewWindowCallback;

    private boolean isInited = false;

    private ConcreteAdasFactory() {
    }

    public static ConcreteAdasFactory getInstance() {
        if (sConcreteAdasFactory == null) {
            synchronized (ConcreteAdasFactory.class) {
                if (sConcreteAdasFactory == null) {
                    sConcreteAdasFactory = new ConcreteAdasFactory();
                }
            }
        }
        return sConcreteAdasFactory;
    }

    public void onCreate(Context context, FloatPreviewWindowCallback callback, View view) {
        mContext = context;
        mFloatPreviewWindowCallback = callback;
        mRootView = view;
        //mSharedPreferences = sharedPreferences;
    }

    @Override
    public void build() {
        mIAdas = TTAdasManager.getInstance();
        mIAdas.init(mFloatPreviewWindowCallback, mContext, mRootView, new AdasActivatedCallback() {
            @Override
            public void callback(boolean isActivated) {
                LogUtils.getInstance().d(TAG,"AdasActivatedCallback isActivated " + isActivated);
                if (isActivated) {
                    //mSharedPreferences.edit().putString("key_adas_type", "tiantongweishi").commit();
                }
            }
        });

//        if (isBSD) {
//            mIAdas = TTBsdManager.getInstance();
//            mIAdas.init(mFloatPreviewWindowCallback, mContext, mRootView,null);
//        } else {
//            mIAdas = KYAdasManager.getInstance();
//            mIAdas.init(mFloatPreviewWindowCallback, mContext, mRootView, new AdasActivatedCallback() {
//                @Override
//                public void callback(boolean isActivated) {
//                    LogUtils.getInstance().I(TAG, "KY AdasActivatedCallback isActivated " + isActivated);
//                    if (isActivated) {
//                        mSharedPreferences.edit().putString(CameraService.KEY_ADAS_TYPE, CameraService.KAI_YI_ADAS).commit();
//                    } else {
//                        mIAdas = KYAdasManager.getInstance();
//                        mIAdas.init(mFloatPreviewWindowCallback, mContext, mRootView, new AdasActivatedCallback() {
//                            @Override
//                            public void callback(boolean isActivated) {
//                                LogUtils.getInstance().I(TAG, "KY2 AdasActivatedCallback isActivated " + isActivated);
//                                if (isActivated) {
//                                    mSharedPreferences.edit().putString(CameraService.KEY_ADAS_TYPE, CameraService.KAI_YI_ADAS).commit();
//                                } else {
//                                    mIAdas = NewAdasManager.getInstance();
//                                    mIAdas.init(mFloatPreviewWindowCallback, mContext, mRootView, new AdasActivatedCallback() {
//                                        @Override
//                                        public void callback(boolean isActivated) {
//                                            LogUtils.getInstance().I(TAG, "TianTong AdasActivatedCallback isActivated " + isActivated);
//                                            if (isActivated) {
//                                                mSharedPreferences.edit().putString(CameraService.KEY_ADAS_TYPE, CameraService.TIAN_TONG_WEI_SHI_ADAS).commit();
//                                            } else {
//                                                mIAdas = KYAdasManager.getInstance();
//                                                mIAdas.init(mFloatPreviewWindowCallback, mContext, mRootView, new AdasActivatedCallback() {
//                                                    @Override
//                                                    public void callback(boolean isActivated) {
//                                                        LogUtils.getInstance().I(TAG, "KY3 AdasActivatedCallback isActivated " + isActivated);
//                                                        if (isActivated) {
//                                                            mSharedPreferences.edit().putString(CameraService.KEY_ADAS_TYPE, CameraService.KAI_YI_ADAS).commit();
//                                                        }
//                                                    }
//                                                });
//                                            }
//                                        }
//                                    });
//                                }
//                            }
//                        });
//                    }
//                }
//            });
//        }

    }

    public void setInited(boolean value) {
        isInited = value;
    }

    public boolean isInited() {
        return isInited;
    }

    @Override
    public IAdas getAdas() {
        return mIAdas;
    }

    @Override
    public void setAdasEnable(boolean value) {
        if (mIAdas != null) {
            mIAdas.setEnable(value);
        }
    }

    @Override
    public boolean isAdasEnable() {
        if (mIAdas != null) {
            return mIAdas.isEnable();
        }
        return false;
    }

    @Override
    public boolean isAdasActive() {
        if (mIAdas != null) {
            return mIAdas.isActive();
        }
        return false;
    }

    @Override
    public void register() {
        if (mIAdas != null) {
            mIAdas.register();
        }
    }

    @Override
    public void unRegister() {
        if (mIAdas != null) {
            mIAdas.unRegister();
        }
    }

    @Override
    public void check() {
        if (mIAdas != null) {
            mIAdas.check();
        }
    }

    @Override
    public void start() {
        if (mIAdas != null) {
            mIAdas.start();
        }
    }

    @Override
    public void stop() {
        if (mIAdas != null) {
            mIAdas.stop();
        }
    }

    @Override
    public void handClick() {
        if (mIAdas != null) {
            mIAdas.handClick();
        }
    }

    @Override
    public void activeInterface() {
        if (mIAdas != null) {
            mIAdas.activeInterface();
        }
    }

    @Override
    public void loadAudioResource() {
        if (mIAdas != null) {
            mIAdas.loadAudioResource();
        }
    }

    @Override
    public void release() {
        if (mIAdas != null) {
            mIAdas.release();
        }
    }

    public interface AdasActivatedCallback {
        void callback(boolean isActivated);
    }
}
