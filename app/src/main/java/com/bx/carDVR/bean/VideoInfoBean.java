package com.bx.carDVR.bean;

import java.util.List;

public class VideoInfoBean {

    private String videoName;
    private String previewName;
    private long videoCreateTime;
    private long videoTimeLength;
    private long videoSize;
    private List<TrackBean> tracks;

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public String getPreviewName() {
        return previewName;
    }

    public void setPreviewName(String previewName) {
        this.previewName = previewName;
    }

    public long getVideoCreateTime() {
        return videoCreateTime;
    }

    public void setVideoCreateTime(long videoCreateTime) {
        this.videoCreateTime = videoCreateTime;
    }

    public long getVideoTimeLength() {
        return videoTimeLength;
    }

    public void setVideoTimeLength(long videoTimeLength) {
        this.videoTimeLength = videoTimeLength;
    }

    public long getVideoSize() {
        return videoSize;
    }

    public void setVideoSize(long videoSize) {
        this.videoSize = videoSize;
    }

    public List<TrackBean> getTracks() {
        return tracks;
    }

    public void setTracks(List<TrackBean> tracks) {
        this.tracks = tracks;
    }
}
