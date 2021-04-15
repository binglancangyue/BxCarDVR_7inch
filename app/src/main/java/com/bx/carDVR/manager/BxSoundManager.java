package com.bx.carDVR.manager;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.bx.carDVR.R;
import com.bx.carDVR.util.LogUtils;

public class BxSoundManager {

    private static final String TAG = "BxSoundManager";

    private SoundPool mSoundPool;

    public static final int TYPE_START_RECORD = 1;
    public static final int TYPE_STOP_RECORD = 2;
    public static final int TYPE_TAKE_PICTURE = 3;

    private int photoId;
    public BxSoundManager(Context context) {
        mSoundPool = new SoundPool(15, AudioManager.STREAM_SYSTEM, 5);
        loadAudioResource(context);
    }

    private void loadAudioResource(Context context){
        photoId = mSoundPool.load(context,R.raw.photo,1);
    }

    public void playSound(int type) {
        switch (type) {
            case TYPE_START_RECORD:

                break;
            case TYPE_STOP_RECORD:

                break;
            case TYPE_TAKE_PICTURE:
                LogUtils.getInstance().d(TAG,"playSound TYPE_TAKE_PICTURE photoId = "+photoId);
                mSoundPool.play(photoId,1,1,2,0,1);
                break;
            default:
                break;
        }
    }
}
