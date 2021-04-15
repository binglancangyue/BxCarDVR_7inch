
package com.bx.carDVR.adas;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

import com.bx.carDVR.R;
import com.calmcar.adas.apiserver.AdasConf;



/**
 * 播报功能
 */


public class WarnSpeakManager {
    private Context mContext;
    private long start_time, timeCrash, timeLaunch,timeLast;
    private SoundPool soundPool;
    private int streamId1, streamId2, streamId3, streamId4, streamId5;
    private int soundId1, soundId2, soundId3, soundId4, soundId5,soundId6;

    public WarnSpeakManager(final Context mContext) {
        this.mContext = mContext;
        soundPoolInit(mContext);
    }

    public void carOutLine() {
        if (System.currentTimeMillis() - start_time >= AdasConf.LaneConf.OUT_FREQVAL * 1000) {
            start_time = System.currentTimeMillis();
            streamId1 = soundPool.play(soundId1, 1, 1, 1, 0, 1);
        }
    }

    public void frontCarCrash() {
        if (System.currentTimeMillis() - timeCrash >= AdasConf.CarConf.CRASH_FREQVAL * 1000) {
            timeCrash = System.currentTimeMillis();
            streamId3 = soundPool.play(soundId3, 1, 1, 1, 0, 1);
        }
    }

    public void frontCarSafeDistance() {
        if (System.currentTimeMillis() - timeCrash >= AdasConf.CarConf.DIS_FREQVAL * 1000) {
            timeCrash = System.currentTimeMillis();
            streamId2 = soundPool.play(soundId2, 1, 1, 1, 0, 1);
        }
    }

    public void frontCarLaunchWarn() {
        if (System.currentTimeMillis() - timeLaunch >= AdasConf.CarConf.CRASH_FREQVAL * 1000) {
            timeLaunch = System.currentTimeMillis();
            streamId4 = soundPool.play(soundId4, 1, 1, 1, 0, 1);
        }

    }

    public void adasStart() {
        if (System.currentTimeMillis() - timeCrash >= 5000) {
            timeCrash = System.currentTimeMillis();
            streamId5 = soundPool.play(5, 1, 1, 1, 0, 1);//第一个参数是12345，其中一个，，，5是adas已启动
        }
    }


    public void stop() {
        soundPool.release();
    }

    public void soundPoolInit(Context mContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes abs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(100)   //设置允许同时播放的流的最大值
                    .setAudioAttributes(abs)  //完全可以设置为null
                    .build();
        } else {
            soundPool = new SoundPool(100, AudioManager.STREAM_MUSIC, 1);//构建对象
        }
    }


    public void playSoud(int soundId) {
        if (System.currentTimeMillis() - timeLast >= 5000) {
            timeLast = System.currentTimeMillis();
            int curid = soundPool.play(soundId, 1, 1, 1, 0, 1);//第一个参数是12345，其中一个，，，5是adas已启动
        }

    }
    public void playSoudNEW(int soundId) {
        if (System.currentTimeMillis() - timeLast >= 10000) {
            timeLast = System.currentTimeMillis();
            int curid = soundPool.play(soundId, 1, 1, 1, 0, 1);//第一个参数是12345，其中一个，，，5是adas已启动
        }
    }

    public void initVideoPlayers(int laneWarnResId, int levelOneWarnResId, int levelTwoWarnResId, int frontCarLaunchResId) {
        soundId1 = soundPool.load(mContext, laneWarnResId, 1);//加载资源，得到soundId
        soundId2 = soundPool.load(mContext, levelOneWarnResId, 1);//加载资源，得到soundId
        soundId3 = soundPool.load(mContext, levelTwoWarnResId, 1);//加载资源，得到soundId
        soundId4 = soundPool.load(mContext, frontCarLaunchResId, 1);//加载资源，得到soundId
        soundId6 = soundPool.load(mContext, R.raw.voice_di_2, 1);//加载资源，得到soundId
    }

    public void initVideoPlayers(int laneWarnResId, int levelOneWarnResId, int levelTwoWarnResId, int frontCarLaunchResId, int adasStartResId) {
        initVideoPlayers(laneWarnResId, levelOneWarnResId, levelTwoWarnResId, frontCarLaunchResId);
        if (adasStartResId > 0) {
            soundId5 = soundPool.load(mContext, adasStartResId, 1);//加载资源，得到soundId
        }
    }


    public int[] loadSoundNew(int[] resIds) {
        int[] result = new int[resIds.length];
        for (int m = 0; m < resIds.length; m++) {
            result[m] = soundPool.load(mContext, resIds[m], 1);
        }
        return result;
    }

    public void startSuccess() {
        int streamId1 = soundPool.play(soundId6, 1, 1, 1, 0, 1);
    }
}

