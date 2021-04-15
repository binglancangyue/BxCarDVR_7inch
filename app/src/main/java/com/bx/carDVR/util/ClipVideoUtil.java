package com.bx.carDVR.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.bx.carDVR.DvrService;
import com.bx.carDVR.app.DvrApplication;
import com.bx.carDVR.bean.Configuration;
import com.bx.carDVR.bean.VideoInfoBean;
import com.bx.carDVR.video.Mp4ParseUtil;
import com.bx.carDVR.video.VideoClip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class ClipVideoUtil {

    private static final String TAG = "ClipVideoUtil";
    public static final int CUT_BEFORE_DURATION = 15;
    public static final int CUT_AFTER_DURATION = 15;
    public static final int CUT_TOTAL_DURATION = 30;

    private static final int MSG_TO_MERGE_VIDEO = 1035;

    public static final String RESULT_VIDEO = "VIDEO_CALL_WE_TAKE_VIDEO_RESULT";

    private static SimpleDateFormat formatVideo = new SimpleDateFormat("yyyyMMddHHmmss");
    private List<String> clipVideos = new ArrayList<>();
    private List<String> mergeVideoList = new ArrayList<>();
    private String mergeVideoPath;
    private long mStartTime;
    private Context mContext;
    private String mCameraId;
    private CompositeDisposable compositeDisposable;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_TO_MERGE_VIDEO) {
                mergeVideo();
            }
        }
    };

    public ClipVideoUtil(Context context,String cameraId) {
        this.mContext = context;
        compositeDisposable = new CompositeDisposable();
        this.mCameraId = cameraId;
    }

    public void setClipVideos(String fileName) {
        logD("setClipVideos fileName : "+fileName);
        if (clipVideos.size() == 2) {
            clipVideos.remove(0);
            clipVideos.add(fileName);
        } else {
            clipVideos.add(fileName);
        }
        if (!isContinuousFile(clipVideos)) {
            clipVideos.remove(0);
        }
        logD("setClipVideos clipVideos size : "+clipVideos.size());
    }

    private boolean isContinuousFile(final List<String> list) {
        if (list.size() == 2) {
            String fileOne = list.get(0);
            String fileTwo = list.get(1);
            String timeOne = getTimeByFileName(fileOne);
            String timeTwo = getTimeByFileName(fileTwo);
            long num0 = 0;
            long num1 = 0;
            if (!TextUtils.isEmpty(timeOne)) {
                num0 = Long.parseLong(timeOne);
            }
            if (!TextUtils.isEmpty(timeTwo)) {
                num1 = Long.parseLong(timeTwo);
            }
            long reduce = calculateTime(num1, num0) - getLocalVideoDuration(list.get(0));
            if (reduce >= -3 && reduce <= 3) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取加锁触发时间和当前视频开始时间的值
     *
     * @param time 加锁触发时间
     * @param num  视频录制时间
     * @return
     */
    private long calculateTime(long time, long num) {
        // TODO Auto-generated method stub
        if (time == -1 || num == -1) {
            return -1;
        }
        Date before = null;
        Date now = null;
        try {
            before = formatVideo.parse(String.valueOf(num));
            now = formatVideo.parse(String.valueOf(time));
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        long l = now.getTime() - before.getTime();
        long day = l / (24 * 60 * 60 * 1000);
        long hour = (l / (60 * 60 * 1000) - day * 24);
        long min = ((l / (60 * 1000)) - day * 24 * 60 - hour * 60);
        long s = (l / 1000 - day * 24 * 60 * 60 - hour * 60 * 60);
        logD( "min:" + min);
        return s;
    }

    private int getLocalVideoDuration(String videoPath) {
        int duration;
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(videoPath);
            duration = Integer.parseInt(mmr.extractMetadata
                    (MediaMetadataRetriever.METADATA_KEY_DURATION));
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
        logD( "getLocalVideoDuration:duration " + duration);
        return duration / 1000;
    }

    public void toFileClip(long time) {
        mStartTime = time;
        compositeDisposable.add(Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> emitter) throws Exception {
                fileClip(time);
                emitter.onNext(1);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {

                    }
                }));
    }

    private void fileClip(long startTime) {
        logD( "fileClip fileList size : "+ clipVideos.size() +" , start time = "+startTime);
        if (clipVideos.size() == 1) {
            String fileName = clipVideos.get(0);
            String strTime = getTimeByFileName(fileName);
            long fileTime = Long.parseLong(strTime);
            String clipVideoName = getMergeVideoName(startTime);
            startTime = Long.parseLong(formatVideo.format(startTime));
            final int subTime = (int) calculateTime(startTime, fileTime);
            final int videoDuration = getLocalVideoDuration(fileName);
            String outPath = StorageUtils.DIRECTORY_VIDEO;
            logD("clipVideos.size() == 1 , subTime = "+subTime + " , videoDuration = "+videoDuration );
            if (subTime <= CUT_BEFORE_DURATION) {
                if (videoDuration > CUT_TOTAL_DURATION) {
                    cutVideo(getVideoClip(0, CUT_TOTAL_DURATION, fileName, outPath, clipVideoName,
                            true, false));
                    logD( "cutVideo size1 : 0 - " + CUT_TOTAL_DURATION + " , fileName = " +fileName);
                } else {
                    cutVideo(getVideoClip(0, videoDuration, fileName, outPath, clipVideoName,
                            true, false));
                    logD( "cutVideo size1 : 0 - " + videoDuration + " , fileName = " +fileName);
                }
            } else if (subTime > CUT_BEFORE_DURATION) {
                int start = subTime - CUT_BEFORE_DURATION;
                int end = subTime + CUT_AFTER_DURATION;
                if (subTime <= (videoDuration - CUT_AFTER_DURATION)) {
                    cutVideo(getVideoClip(start, end, fileName,
                            outPath, clipVideoName, true, false));
                } else {
                    end = videoDuration;
                    cutVideo(getVideoClip(start, videoDuration, fileName,
                            outPath, clipVideoName, true, false));
                }
                logD( "cutVideo size1 : start - "+ start+" , end "+end+" , fileName = " +fileName);
            } else {
                DvrService.getInstance().saveClipVideoStatus(false);
                logD( "MSG_VID_SEND_ERROR size1 subTime>videoDuration-" + CUT_AFTER_DURATION+ " , fileName = " +fileName);
            }
        } else if (clipVideos.size() == 2) {
            String substring = null;
            for (int i = 0; i < clipVideos.size(); i++ ){
                substring = getTimeByFileName(clipVideos.get(i));
                long num = Long.parseLong(substring);
                long lockTime = Long.parseLong(formatVideo.format(startTime));
                final int subTime = (int) calculateTime(lockTime, num);
                String outPath = StorageUtils.DIRECTORY_VIDEO;
                logD("clipVideos.size() == 2 , subTime = "+subTime);
                if (i == 1) {
                    int videoDuration = getLocalVideoDuration(clipVideos.get(1));
                    logD( "fileClip:videoDuration "+videoDuration);
                    if (subTime >= CUT_BEFORE_DURATION ) {
                        String filename = getMergeVideoName(startTime);
                        logD( "cutVideo size2:subtime " + subTime + " filename " + filename);
                        int start = subTime - CUT_BEFORE_DURATION;
                        int end = subTime + CUT_AFTER_DURATION;
                        if (subTime <= (videoDuration - CUT_AFTER_DURATION)) {
                            cutVideo(getVideoClip(start, end, clipVideos.get(i), outPath, filename, true, false));
                        } else {
                            end = videoDuration;
                            cutVideo(getVideoClip(subTime - CUT_BEFORE_DURATION, videoDuration,
                                    clipVideos.get(i), outPath, filename, true, false));
                        }
                        logD( "cutVideo size2 : start - "+ start+" , end "+end+" , fileName = " +clipVideos.get(i));
                    } else if (subTime < CUT_BEFORE_DURATION && subTime >= 0) {
                        mergeVideoList.clear();
                        String filename = getMergeVideoName(clipVideos.get(0));
                        mergeVideoList.add(outPath + "/" + filename);
                        int videoTime = getLocalVideoDuration(clipVideos.get(0));
                        logD( "cutVideo run: clip first video " + filename + " videoTime " + videoTime);
                        cutVideo(getVideoClip(videoTime - CUT_BEFORE_DURATION + subTime, videoTime,
                                clipVideos.get(0), outPath, filename, false, false));

                        long finalTime = startTime;
                        String fileName = getMergeVideoName(clipVideos.get(1));
                        mergeVideoPath = outPath + "/" + getMergeVideoName(finalTime);
                        mergeVideoList.add(outPath + "/" + fileName);
                        logD( "run: mergeVideoPath " + mergeVideoPath);
                        logD( "cutVideo size2 run filename: " + fileName);
                        int video2Time = getLocalVideoDuration(clipVideos.get(1));
                        int end = subTime + CUT_AFTER_DURATION;
                        if (video2Time >= end) {
                            cutVideo(getVideoClip(0, subTime + CUT_AFTER_DURATION, clipVideos.get(1), outPath,
                                    fileName, false, true));
                        } else {
                            cutVideo(getVideoClip(0, video2Time, clipVideos.get(1), outPath,
                                    fileName, false, true));
                        }
                    }else {
                        DvrService.getInstance().saveClipVideoStatus(false);
                        LogUtils.getInstance().e(TAG, "MSG_VID_SEND_ERROR:cutVideo size2" + subTime);
                    }
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    public void cutVideo(VideoClip videoClip) {
        compositeDisposable.add(Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                cutMp4(videoClip, emitter);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String fileName) throws Exception {
                        Log.d(TAG, "cutVideo accept: " + fileName);
                        //ToastTool.hideLongToast();
                        if (videoClip.isSendBroadcast() && !fileName.equals("null")) {
                            DvrService.getInstance().saveClipVideoStatus(false);
                            sendBroadcast("1", RESULT_VIDEO, fileName, true);
                        }
                        if (videoClip.isTheLastVideo()) {
                            mHandler.sendEmptyMessage(MSG_TO_MERGE_VIDEO);
                        }
                    }
                }));
    }

    private synchronized void cutMp4(VideoClip info, ObservableEmitter<String> emitter) {
        VideoClip videoClip = info;
        videoClip.clip(emitter);
    }

    /**
     * 视频剪切
     *
     * @param startTime      视频剪切的开始时间
     * @param endTime        视频剪切的结束时间
     * @param FilePath       被剪切视频的路径
     * @param WorkingPath    剪切成功保存的视频路径
     * @param fileName       剪切成功保存的文件名
     * @param isTrue         是否发送上传广播
     * @param isTheLastVideo 是否是合并视频的最后一个时候
     */
    private VideoClip getVideoClip(final long startTime, final long endTime, final String FilePath,
                                   final String WorkingPath, final String fileName, final Boolean isTrue
            , final Boolean isTheLastVideo) {
        //视频剪切
        VideoClip videoClip = new VideoClip();//实例化VideoClip类
        videoClip.setFilePath(FilePath);//设置被编辑视频的文件路径  FileUtil.getMediaDir()
        // +"/test/laoma3.mp4"
        videoClip.setWorkingPath(WorkingPath);//设置被编辑的视频输出路径  FileUtil.getMediaDir()
        videoClip.setStartTime(startTime * 1000);//设置剪辑开始的时间
        videoClip.setEndTime(endTime * 1000);//设置剪辑结束的时间
        videoClip.setOutName(fileName);//设置输出的文件名称
        videoClip.setSendBroadcast(isTrue);//是否发送上传广播
        videoClip.setTheLastVideo(isTheLastVideo);//是否是合并视频的最后一个时候
        return videoClip;
    }

    private void mergeVideo() {
        compositeDisposable.add(Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                Mp4ParseUtil.appendMp4List(mergeVideoList, mergeVideoPath, emitter);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String fileName) throws Exception {
                        logD( "mergeVideo accept: " + fileName);
                        //ToastTool.hideLongToast();
                        if (!fileName.equals("null")) {
                            DvrService.getInstance().saveClipVideoStatus(false);
                            sendBroadcast("2", RESULT_VIDEO, fileName, true);
                        }
                    }
                }));
    }

    private String getTimeByFileName(String fileName) {
        String str = null;
        if (DvrService.CAMERA_FRONT_ID.equals(mCameraId)) {
            if (fileName.contains("VID")) {
                str = fileName.substring(fileName.length() - 16,fileName.length() - 4);
            } else {
                str = fileName.substring(fileName.length() - 24,fileName.length() - 10);
            }
        } else if (DvrService.CAMERA_BACK_ID.equals(mCameraId)) {
            if (fileName.contains("VID")) {
                str = fileName.substring(fileName.length() - 16,fileName.length() - 4);
            } else {
                str = fileName.substring(fileName.length() - 23,fileName.length() - 9);
            }
        }
        logD("getTimeByFileName = "+str);
        return str;
    }


    private String getMergeVideoName(long time) {
        String filename = formatVideo.format(time);
        String cameraType = "_default_";
        if (DvrService.CAMERA_FRONT_ID.equals(mCameraId)) {
            cameraType = "_front_";
        } else if (DvrService.CAMERA_BACK_ID.equals(mCameraId)) {
            cameraType = "_back_";
        }
        filename = "VID" + cameraType +  filename + ".mp4";
        logD( "getMergeVideoName: " + filename);
        return filename;
    }

    private String getMergeVideoName(String file) {
        String filename = getTimeByFileName(file);
        String cameraType = "_default_";
        if (DvrService.CAMERA_FRONT_ID.equals(mCameraId)) {
            cameraType = "_front_";
        } else if (DvrService.CAMERA_BACK_ID.equals(mCameraId)) {
            cameraType = "_back_";
        }
        filename = "VID" + cameraType +  filename + ".mp4";
        logD( "getMergeVideoName: " + filename);
        return filename;
    }

    @SuppressLint("WrongConstant")
    public void sendBroadcast(String msgId, String resultType, String path, boolean successful) {
        if (!Configuration.IS_3IN) {
            return;
        }

        compositeDisposable.add(Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                try {
                    VideoInfoBean videoInfoBean = new VideoInfoBean();
                    videoInfoBean.setVideoName(path);
                    videoInfoBean.setTracks(DvrService.getInstance().getTracks());
                    File file = new File(path);
                    if (file.exists()) {
                        FileInputStream fileInputStream = new FileInputStream(file);
                        long size = fileInputStream.available();
                        videoInfoBean.setVideoSize(size);
                    }
                    long duration = getLocalVideoDuration(path) * 1000;
                    videoInfoBean.setVideoCreateTime(mStartTime);
                    videoInfoBean.setVideoTimeLength(duration);
                    String uploadInfo = JSONObject.toJSONString(videoInfoBean);
                    LogUtils.getInstance().d(TAG,"uploadInfo : "+uploadInfo);
                    emitter.onNext(uploadInfo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String str) throws Exception {
                        Intent intent = new Intent();
                        intent.setAction("com.android.zqc.send");
                        intent.addFlags(0x01000000);
                        intent.putExtra("ecarSendKey", resultType);
                        intent.putExtra("result", successful);
                        intent.putExtra("msgid", msgId);
                        intent.putExtra("filePath", path);
                        intent.putExtra("cameraId",Integer.parseInt(mCameraId));
                        intent.putExtra("uploadInfo", str);
                        DvrApplication.getDvrApplication().sendBroadcast(intent);
                        logD( "sendBroadcastToEcar");
                    }
                })
        );
    }

    private void logD(String str){
        LogUtils.getInstance().d(TAG, str + "cameraId = "+mCameraId);
    }

    private void logE(String str){
        LogUtils.getInstance().e(TAG, str + "cameraId = "+mCameraId);
    }

}
