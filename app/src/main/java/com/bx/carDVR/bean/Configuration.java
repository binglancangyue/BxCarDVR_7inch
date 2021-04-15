package com.bx.carDVR.bean;

public class Configuration {

    public static final boolean CAMERA2 = true;

    public static final boolean DEBUG = true;

    public static final int VIDEO_WIDTH_720P = 1280;
    public static final int VIDEO_HEIGHT_720P = 720;
    public static final int VIDEO_WIDTH_1080P = 1920;
    public static final int VIDEO_HEIGHT_1080P = 1080;

    public static final int PREVIEW_WIDTH_720P = 1280;
    public static final int PREVIEW_HEIGHT_720P = 720;
    public static final int PREVIEW_WIDTH_1080P = 1920;
    public static final int PREVIEW_HEIGHT_1080P = 1080;

    public static final int CAMERA_NUM = 2;
    //        public static final int[] CAMERA_IDS = {1, 0, 6, 7};
    public static final int[] CAMERA_IDS = {0, 1, 6, 7};

    public static final boolean[] IS_USB_CAMERA = {false, false, false, false}; //是否是USB摄像头

    public static final boolean ENABLE_CAR_REVERSE = false;

    public static final boolean ONLY_TAKE_PHOTOS_WHILE_RECORDING = true;

    public static final String ACTION_CLOSE_DVR = "com.bx.carDVR.action_close";
    public static final String ACTION_SET_DVR_RECORD_TIME = "com.android.systemui.SET_DVR_RECORD_TIME";
    public static final String ACTION_SHOW_SETTING_WINDOW = "com.android.systemui.show_setting_window";
    public static final String ACTION_SET_G_SENSOR_LEVEL = "com.android.systemui.SET_G_SENSOR_LEVEL";
    public static final String ACTION_SET_ADAS_LEVEL = "com.android.systemui.SET_ADAS_LEVEL";
    public static final String ACTION_UPLOAD_VIDEO = "com.bx.carDVR.action.UPLOAD_VIDEO";
    public static final String ACTION_FORMAT_SD_CARD = "com.android.systemui.FORMAT_SD_CARD";
    public static final String ACTION_UPLOAD = "bixin.action.shangchuan";
    public static final String ACTION_OPEN_DVR_CAMERA = "com.android.systemui.OPEN_CAMERA";
    public static final String DVR_COLLISION = "Dvr_collision";
    public static final String DVR_ADAS = "Dvr_ADAS";
    public static final String ACTION_STOP_RECORD = "com.bixin.bxvideolist.action.stop_recording";
    public static final String ACTION_SPEECH_TOOL_CMD = "com.bixin.speechrecognitiontool.action_cmd";

    public static final String ACTION_DISMISS_SPLIT_WINDOW = "com.android.deskclock.action.dismiss_settings_dialog";
    public static final String ACTION_REVERSE = "com.bx.action.reverse";

    public static final String ACTION_SETTINGS_FUNCTION = "com.android.systemui.action.send_to_dvr";
    public static final String KEY_TAKE_PICTURE = "KEY_TAKE_PICTURE";
    public static final String KEY_BACK_CAMERA = "KEY_BACK_CAMERA";
    public static final String KEY_UPLOAD = "KEY_UPLOAD";
    public static final String TYPE_G_SENSOR = "TYPE_G_SENSOR";
    public static final String KEY_RECORD_TIME = "KEY_RECORD_TIME";
    public static final String KEY_RECORD = "KEY_RECORD";
    public static final String ACTION_GAODE_SEND = "AUTONAVI_STANDARD_BROADCAST_SEND";
    public static final int CMD_GAODE_AR = 10086;


    public static final int CMD_ECAR_START_LIVE = 10087;
    public static final int CMD_ECAR_STOP_LIVE = 10089;

    public static final String ACTION_BX_SEND = "com.android.bx.send";
    public static final String ACTION_BX_RECEIVE = "com.android.bx.recv";

    public static final String ACTION_SYSTEM_SLEEP = "com.bx.action.system_sleep";
    public static final String ACTION_SYSTEM_WAKE_UP = "com.bx.action.system_wake_up";

    public static final String ACTION_POWER_CONNECTED = "com.status.power.connected";
    public static final String ACTION_POWER_DISCONNECTED = "com.status.power.disconnected";

    public static final String ACTION_TXZ_SHOW = "com.txznet.txz.record.show";
    public static final String ACTION_TXZ_DISMISS = "com.txznet.txz.record.dismiss";
    public static final String ACTION_SETTINGS_WINDOW = "com.android.systemui.settings_window_state";

    public static final String CLIP_VIDEO_STATUS = "clip_video_status";

    public static final boolean IS_3IN = false;
    public static final boolean IS_7IN = true;
    public static final boolean IS_966 = false;
    public static final boolean IS_439 = false;

    public static final String KD003 = "KD003";
    public static final String KD002 = "KD002";
    public static final String T10 = "T10";
    public static final String PROJECT_NAME = "";

    public static final boolean IS_SUPPORT_REVERSE = true;

    public static final boolean IS_SUPPORT_ADAS = true;

    public static final boolean IS_SUPPORT_SPLIT = false;

    public static final boolean IS_SUPPORT_REBOOT_HINT = true;

    public static final boolean ONLY_BACK_CAMERA = true;

    /*
    public static final int CAMERA_NUM = 4; //sofar 4 avin camera
    public static final int[] CAMERA_IDS = {4, 5, 6, 7};

    public static final boolean[] IS_USB_CAMERA = {false, false, false, false}; //是否是USB摄像头

    public static final boolean ENABLE_CAR_REVERSE = true;

     */

    /*
    public static final int CAMERA_NUM = 1; //tianyu 1 usb camera
    public static final int[] CAMERA_IDS = {0, 5, 6, 7};

    public static final boolean[] IS_USB_CAMERA = {true, false, false, false}; //是否是USB摄像头

    public static final boolean ENABLE_CAR_REVERSE = false;
    */
    private Configuration() {
    }
}
