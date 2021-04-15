package com.autonavi.amapauto.gdarcameraservice.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 打开摄像头后的返回的结果参数,客户端会以服务端返回的该类中的参数来最终矫正缓存的大小
 */
public class ArCameraOpenResultParam implements Parcelable {

    /**
     * 摄像头服务返回的真正支持的图片数据格式
     */
    public int imageFormat;

    /**
     * 摄像头服务返回的真正支持的数据类型
     */
    public int dataType;

    /**
     * 摄像头服务返回的真正支持的摄像头支持类型
     */
    public int cameraUseType;

    /**
     * 摄像头服务返回的真正支持的图片数据宽度
     */
    public int imageWidth;

    /**
     * 摄像头服务返回的真正支持的图片数据高度
     */
    public int imageHeight;

    /**
     * 摄像头服务返回的真正支持的摄像头ID
     */
    public String cameraId;

    /**
     * 摄像头服务返回的真正支持的共享内存中的图片数据内容的大小
     */
    public int imageSize;

    public ArCameraOpenResultParam() {
    }

    public ArCameraOpenResultParam(int imageWidth, int imageHeight, int imageSize) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.imageSize = imageSize;
    }


    public ArCameraOpenResultParam(int imageFormat, int imageWidth, int imageHeight, int imageSize) {
        this.imageFormat = imageFormat;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.imageSize = imageSize;
    }

    public ArCameraOpenResultParam(int imageFormat, int imageWidth, int imageHeight, int imageSize, String cameraId) {
        this.imageFormat = imageFormat;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.imageSize = imageSize;
        this.cameraId = cameraId;
    }

    public ArCameraOpenResultParam(int imageFormat, int dataType, int cameraUseType, int imageWidth, int imageHeight, int imageSize, String cameraId) {
        this.imageFormat = imageFormat;
        this.dataType = dataType;
        this.cameraUseType = cameraUseType;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.imageSize = imageSize;
        this.cameraId = cameraId;
    }

    public int getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(int imageFormat) {
        this.imageFormat = imageFormat;
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public int getCameraUseType() {
        return cameraUseType;
    }

    public void setCameraUseType(int cameraUseType) {
        this.cameraUseType = cameraUseType;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public int getImageSize() {
        return imageSize;
    }

    public void setImageSize(int imageSize) {
        this.imageSize = imageSize;
    }

    protected ArCameraOpenResultParam(Parcel in) {
        imageFormat = in.readInt();
        dataType = in.readInt();
        cameraUseType = in.readInt();
        imageWidth = in.readInt();
        imageHeight = in.readInt();
        cameraId = in.readString();
        imageSize = in.readInt();
    }

    public static final Creator<ArCameraOpenResultParam> CREATOR = new Creator<ArCameraOpenResultParam>() {
        @Override
        public ArCameraOpenResultParam createFromParcel(Parcel in) {
            return new ArCameraOpenResultParam(in);
        }

        @Override
        public ArCameraOpenResultParam[] newArray(int size) {
            return new ArCameraOpenResultParam[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(imageFormat);
        dest.writeInt(dataType);
        dest.writeInt(cameraUseType);
        dest.writeInt(imageWidth);
        dest.writeInt(imageHeight);
        dest.writeString(cameraId);
        dest.writeInt(imageSize);
    }

    @Override
    public String toString() {
        return "ArCameraParam{" +
                "imageFormat=" + imageFormat +
                ", dataType=" + dataType +
                ", cameraUseType=" + cameraUseType +
                ", imageWidth=" + imageWidth +
                ", imageHeight=" + imageHeight +
                ", imageSize=" + imageSize +
                ", cameraId='" + cameraId + '\'' +
                '}';
    }
}
