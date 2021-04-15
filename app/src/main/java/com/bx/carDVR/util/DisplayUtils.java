package com.bx.carDVR.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.SystemProperties;
import android.util.DisplayMetrics;

public class DisplayUtils {
	private static final String TAG = "BxDisplayUtils";
	private static DisplayUtils displayUtils;

	private DisplayUtils() {

	}

	public static DisplayUtils getInstance() {
		if (null == displayUtils) {
			displayUtils = new DisplayUtils();
		}
		return displayUtils;
	}

	public Point getDisplaySize(Context context) {
		Resources resources = context.getResources();
		DisplayMetrics dm = resources.getDisplayMetrics();
		int width = dm.widthPixels;
		int height = dm.heightPixels;
		LogUtils.getInstance().d(TAG, "getDisplaySize() width = " + width + ", height = " + height);
		return new Point(width, height);
	}

	public int getOrientation(Context context) {
		return context.getResources().getConfiguration().orientation;
	}
	
	public String getRunningActivityName(Context context){
		try {
			ActivityManager activityManager=(ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			String runningActivity=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
			return runningActivity;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}
	
	
	public static boolean isSimplifiedChinse(){
		if("zh-CN".equals(SystemProperties.get("persist.sys.locale"))){
			return true;
		}else{
			return false;
		}
	}

}
