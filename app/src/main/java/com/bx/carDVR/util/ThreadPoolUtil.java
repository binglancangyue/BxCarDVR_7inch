package com.bx.carDVR.util;

import android.os.Handler;
import android.os.Message;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class ThreadPoolUtil {
    private static final String TAG = "ThreadPoolUtil";
    private static volatile ExecutorService sThreadPool;

    public static Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.obj instanceof Runnable) {
                ThreadPoolUtil.post((Runnable) msg.obj);
            }
        }
    };

    private static ExecutorService getThreadPool() {
        if (sThreadPool == null) {
            synchronized (ThreadPoolUtil.class) {
                if (sThreadPool == null) {
                    sThreadPool = Executors.newCachedThreadPool();
                }
            }
        }
        return sThreadPool;
    }

    public static void post(Runnable task) {
        try {
            getThreadPool().execute(task);
        } catch (RejectedExecutionException e) {
           e.printStackTrace();
        }
    }

    public static void postDelayed(final Runnable task, long delayMillis) {
        Message msg = Message.obtain();
        msg.obj = task;
        mMainHandler.sendMessageDelayed(msg, delayMillis);
    }

    public static void removeCallbacks(Runnable task) {
        mMainHandler.removeCallbacksAndMessages(task);
    }

}
