package com.bx.carDVR.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.bx.carDVR.R;
import com.bx.carDVR.app.DvrApplication;
import com.bx.carDVR.bean.Configuration;
import com.bx.carDVR.util.LogUtils;

public class FloatToast {

	private static final String TAG = "FloatToast";
	private static boolean localLOGV = true;
	private static FloatToast result;
	private static Context context;
	private int mDuration;
	private static View mView;
	private static TextView tv;
	private static CharSequence content;
	private boolean isViewAdded = false;
	private WindowManager mWM;
	public static final int LENGTH_SHORT = 0;
	public static final int LENGTH_LONG = 1;
	private static final int LENGTH_SHORT_TIME = 2 * 1000;
	private static final int LENGTH_LONG_TIME = 4 * 1000;
	private static LayoutInflater inflate;
	private WindowManager.LayoutParams params = new WindowManager.LayoutParams();
	private static final int MSG_SHOW_TOAST = 1;
	private static final int MSG_HIDE_TOAST = 2;
	private MainHandler mHandler;
	private class MainHandler extends Handler {
		
		public MainHandler(Looper mLooper){
			super(mLooper);
		}
		
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SHOW_TOAST:
				handleShow();
				break;
			case MSG_HIDE_TOAST:
				handleHide();
				break;

			default:
				break;
			}
		}
	}

	private FloatToast() {
		initParams();
	}

	private void initParams() {
		context = DvrApplication.getDvrApplication();
		mWM = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		inflate = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		params.height = WindowManager.LayoutParams.WRAP_CONTENT;
		params.width = WindowManager.LayoutParams.WRAP_CONTENT;
		if (Configuration.PROJECT_NAME.equals(Configuration.KD003)) {
			params.y = 70;
		}
		params.format = PixelFormat.TRANSLUCENT;
		params.windowAnimations = com.android.internal.R.style.Animation_Toast;
		params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
		params.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
		mHandler = new MainHandler(context.getMainLooper());
	}

	public static FloatToast makeText(CharSequence text,
                                      int duration) {
		if (null == result) {
			result = new FloatToast();
		}
		content = text;
		result.mDuration = duration;
		return result;

	}

	public static FloatToast makeText(int resId, int duration) {
		if (null == result) {
			result = new FloatToast();
		}
		return makeText(context.getText(resId), duration);
	}

	public void show() {
		mHandler.sendEmptyMessage(MSG_SHOW_TOAST);
		mHandler.sendEmptyMessageDelayed(MSG_HIDE_TOAST,
				mDuration == LENGTH_LONG ? LENGTH_LONG_TIME : (mDuration == 800 ? 800 : LENGTH_SHORT_TIME));
	}

	public void hide() {
		mHandler.sendEmptyMessage(MSG_HIDE_TOAST);
	}

	private void handleShow() {
		if(isViewAdded && null != mView){
			mWM.removeView(mView);
		}
		if (null == mView) {
			if (Configuration.PROJECT_NAME.equals(Configuration.KD003)) {
				mView = inflate.inflate(
						R.layout.transient_notification_kd003, null);
			} else {
				mView = inflate.inflate(
						R.layout.transient_notification, null);
			}

		}
		if (null == tv) {
			tv = (TextView) mView
					.findViewById(R.id.message);
		}
		if(null != tv){
			tv.setText(content);
		}
		if(null != mView){
			LogUtils.getInstance().d(TAG,"handleShow addView");
			mWM.addView(mView, params);
			isViewAdded = true;
		}
	}

	private void handleHide() {
		if(isViewAdded && null != mView){
			LogUtils.getInstance().d(TAG,"handleHide removeView");
			mWM.removeView(mView);
		}
		isViewAdded = false;
	}

}
