package com.bx.carDVR.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.provider.Settings;

import com.bx.carDVR.DvrService;
import com.bx.carDVR.app.DvrApplication;
import com.bx.carDVR.util.LogUtils;

import java.util.ArrayList;
import java.util.List;

public class SettingsManager {
	private static final String TAG = "SettingsManager";
	private static SettingsManager settingsManager;
	private static final String SETTING_SHARE_NAME = "setting_share";
	private static final String KEY_REAR_FLIP = "rear_flip";
	private static final String KEY_RECORD_TIME = "record_time";
	private static final String KEY_RECORD_QUALITY = "record_quality";
	private static final String KEY_PICTURE_QUALITY = "picture_quality";
	private static final String KEY_COLLISION_LOCK = "collision_lock";
	private static final String KEY_ADAS_SENSITY = "adas_sensity";
	private static final String KEY_MUTE_STATE = "key_mute";
	private static final String KEY_ADAS_ENABLE_STATE = "key_adas_enable";
	private static final String KEY_BSD_ENABLE_STATE = "key_bsd_enable";
	private static final String KEY_GSENSOR_ENABLE_STATE = "key_gsensor_enable";
	private static final int DEFAULT_VALUE_RECORD_TIME = 0;
	private static final int DEFAULT_VALUE_RECORD_QUALITY = 1;
	private static final int DEFAULT_VALUE_PICTURE_QUALITY = 2;
	//private static final int DEFAULT_VALUE_COLLISION_LOCK =  SystemProperties.getInt("persist.lockvideo.sensitivity.default",1);
	private static final int DEFAULT_VALUE_ADAS_SENSITY = 1;
	public static final String RECORD_TIME_VALUE = "record_time_value";
	public static final String ADAS_LEVEL = "adas_level";
	public static final String COLLISION_LOCK_VALUE = "collision_lock_value";
	public static final String IS_MUTE = "is_mute";
	public static final int[] RECORD_TIME_SECOND = new int[] { 60 , 3 * 60 , 5 * 60 };
	public static final int ONE_MINUTE = 60;
	public static final int THREE_MINUTE = 180;
	public static final int FIVE_MINUTE = 300;
	private static SharedPreferences sharedPreferences;

	private static DvrApplication dvrApplication;

	private SettingsManager() {
		dvrApplication = DvrApplication.getDvrApplication();
		sharedPreferences = dvrApplication.getSharedPreferences(SETTING_SHARE_NAME, Context.MODE_PRIVATE);
	}

	public static SettingsManager getInstance() {
		if (null == settingsManager) {
			settingsManager = new SettingsManager();
		}
		return settingsManager;
	}

	public void setMute(boolean isMute) {
		if (null != sharedPreferences) {
			sharedPreferences.edit().putBoolean(KEY_MUTE_STATE, isMute)
					.commit();
			//Settings.Global.putInt(dvrApplication.getContentResolver(), IS_MUTE, isMute?0:1);
			if(onMuteStateChangeListener.size() > 0){
				for (OnMuteStateChangeListener listener : onMuteStateChangeListener) {
					listener.onMuteStateChange(isMute);
				}
			}
		}
	}

	public boolean isMute() {
		if (null != sharedPreferences) {
			return sharedPreferences.getBoolean(KEY_MUTE_STATE, false);
		}
		return false;
	}
	
	public boolean isRearFlip(){
		if (null != sharedPreferences) {
			return sharedPreferences.getBoolean(KEY_REAR_FLIP, false);
		}
		return false;
	}
	
	public void setRearFlip(boolean isFlip){
		if (null != sharedPreferences) {
			sharedPreferences.edit()
					.putBoolean(KEY_REAR_FLIP, isFlip).commit();
		}
	}

	public void setAdasEnable(boolean isEnable) {
		if (null != sharedPreferences) {
			sharedPreferences.edit()
					.putBoolean(KEY_ADAS_ENABLE_STATE, isEnable).commit();
		}
	}

	public boolean isAdasEnable() {
		if (null != sharedPreferences) {
			return sharedPreferences.getBoolean(KEY_ADAS_ENABLE_STATE, true);
		}
		return true;
	}
	
//    public void setBsdEnable(boolean isEnable) {
//    	if (null != sharedPreferences) {
//			sharedPreferences.edit()
//					.putBoolean(KEY_BSD_ENABLE_STATE, isEnable).commit();
//		}
//	}
    
    public boolean isBsdEnable() {
    	if (null != sharedPreferences) {
			return sharedPreferences.getBoolean(KEY_BSD_ENABLE_STATE, false);
		}
		return true;
	}

	public int getRecordTime() {
		return sharedPreferences.getInt(KEY_RECORD_TIME,
				ONE_MINUTE);
	}

	public int getRecordQuality() {
		return sharedPreferences.getInt(KEY_RECORD_QUALITY,
				DEFAULT_VALUE_RECORD_QUALITY);
	}

	public int getPictureQuality() {
		return sharedPreferences.getInt(KEY_PICTURE_QUALITY,
				DEFAULT_VALUE_PICTURE_QUALITY);
	}

	public int getCollisionLock() {
		return sharedPreferences.getInt(KEY_COLLISION_LOCK, 2);
	}
	
	public int getAdasSensity() {
		return sharedPreferences.getInt(KEY_ADAS_SENSITY,
				DEFAULT_VALUE_ADAS_SENSITY);
	}

	private static void saveRecordTime(int value) {
		sharedPreferences.edit().putInt(KEY_RECORD_TIME, value).commit();
	}

	private static void saveRecordQuality(int value) {
		sharedPreferences.edit().putInt(KEY_RECORD_QUALITY, value).commit();
	}

	private static void savePictureQuality(int value) {
		sharedPreferences.edit().putInt(KEY_PICTURE_QUALITY, value).commit();
	}

	private static void saveCollisionLock(int value) {
		sharedPreferences.edit().putInt(KEY_COLLISION_LOCK, value).commit();
	}
	
	private static void saveAdasSensity(int value) {
		sharedPreferences.edit().putInt(KEY_ADAS_SENSITY, value).commit();
	}
	
	private static void saveGlobalAdasLevel(int value) {
		Settings.Global.putInt(dvrApplication.getContentResolver(), ADAS_LEVEL, value);
	}
	private static void saveGlobalRecordTime(int value) {
		Settings.Global.putInt(dvrApplication.getContentResolver(), RECORD_TIME_VALUE, RECORD_TIME_SECOND[value]);
	}
	private static void saveGlobalCollisionLock(int value) {
		Settings.Global.putInt(dvrApplication.getContentResolver(), COLLISION_LOCK_VALUE, value);
	}
	
	public static class OnAdasSensityChangeListener implements
		OnSettingDataChangeListener {
	
		@Override
		public void onChoiceChanged(int checkedPosition) {
			//LogUtils.getInstance().d(TAG, "OnAdasSensityChangeListener checkedPosition = " + checkedPosition);
			saveAdasSensity(checkedPosition);
			saveGlobalAdasLevel(checkedPosition);
			if (null != onSettingProfileChangeListener) {
				onSettingProfileChangeListener
						.onAdasSensityChanged(checkedPosition);
			}
		}
	
	}

	public static class OnCollisionLockChangeListener implements
			OnSettingDataChangeListener {

		@Override
		public void onChoiceChanged(int checkedPosition) {
			LogUtils.getInstance().d(TAG,"OnCollisionLockChangeListener checkedPosition = "
					+ checkedPosition);
			saveCollisionLock(checkedPosition);
			saveGlobalCollisionLock(checkedPosition);
			if (null != onSettingProfileChangeListener) {
				onSettingProfileChangeListener
						.onCollisionLockChanged(checkedPosition);
			}
		}

	}

	public static class OnPictureQualityChangeListener implements
			OnSettingDataChangeListener {

		@Override
		public void onChoiceChanged(int checkedPosition) {
			LogUtils.getInstance().d(TAG, "OnPictureQualityChangeListener checkedPosition = "
					+ checkedPosition);
			savePictureQuality(checkedPosition);
			if (null != onSettingProfileChangeListener) {
				onSettingProfileChangeListener
						.onPictureQualityChanged(checkedPosition);
			}
		}

	}

	public static class OnRecordQualityChangeListener implements
			OnSettingDataChangeListener {

		@Override
		public void onChoiceChanged(int checkedPosition) {
			LogUtils.getInstance().d(TAG, "OnRecordQualityChangeListener checkedPosition = "
					+ checkedPosition);
			saveRecordQuality(checkedPosition);
			if (null != onSettingProfileChangeListener) {
				onSettingProfileChangeListener
						.onRecordQualityChanged(checkedPosition);
			}
		}

	}

	public static class OnRecordTimeChangeListener implements
			OnSettingDataChangeListener {

		@Override
		public void onChoiceChanged(int checkedPosition) {
			LogUtils.getInstance().d(TAG, "OnRecordTimeChangeListener checkedPosition = "
					+ checkedPosition);
			saveRecordTime(checkedPosition);
			saveGlobalRecordTime(checkedPosition);
			if (null != onSettingProfileChangeListener) {
				onSettingProfileChangeListener
						.onRecordTimeChanged(checkedPosition);
			}
		}

	}

	public void setOnSettingProfileChangeListener(
			OnSettingProfileChangeListener Listener) {
		onSettingProfileChangeListener = Listener;
	}

	private static OnSettingProfileChangeListener onSettingProfileChangeListener;

	public static interface OnSettingProfileChangeListener {
		void onRecordTimeChanged(int value);

		void onRecordQualityChanged(int value);

		void onPictureQualityChanged(int value);

		void onCollisionLockChanged(int value);
		
		void onAdasSensityChanged(int value);
	}

	public interface OnSettingDataChangeListener {
		void onChoiceChanged(int checkedPosition);
	}
	
	
	private List<OnMuteStateChangeListener> onMuteStateChangeListener = new ArrayList<OnMuteStateChangeListener>();
	public void addOnMuteStateChangeListener(OnMuteStateChangeListener listener){
		if(!onMuteStateChangeListener.contains(listener)){
			onMuteStateChangeListener.add(listener);
		}
	}
	public void removeOnMuteStateChangeListener(OnMuteStateChangeListener listener){
		if(!onMuteStateChangeListener.contains(listener)){
			onMuteStateChangeListener.remove(listener);
		}
	}
	public static interface OnMuteStateChangeListener{
		void onMuteStateChange(boolean isMute);
	}

	public void saveCollisionLockInterface(int value){
		//if(value < 0 || value > 3)value = 1;
		saveCollisionLock(value);
		//saveGlobalCollisionLock(value);
	}
	
	public void saveRecordTimeInterface(int value){
		//if(value < 0 || value > 2) return;
		saveRecordTime(value);
		//saveGlobalRecordTime(value);
		LogUtils.getInstance().d(TAG, "saveRecordTimeInterface value = " + value);
	}
	public void saveAdasLevelInterface(int value){
		if(value < 0 || value > 2) return;
		saveAdasSensity(value);
		saveGlobalAdasLevel(value);
	}

	public boolean isGSensorOpened() {
		if (null != sharedPreferences) {
			return sharedPreferences.getBoolean(KEY_MUTE_STATE, true);
		}
		return false;
	}

	public void saveGSensorStatus(boolean isOpen){
		if (sharedPreferences != null) {
			sharedPreferences.edit().putBoolean(KEY_GSENSOR_ENABLE_STATE, isOpen).commit();
		}
	}
}
