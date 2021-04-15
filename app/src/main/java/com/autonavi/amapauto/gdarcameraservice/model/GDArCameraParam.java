package com.autonavi.amapauto.gdarcameraservice.model;

import android.os.Parcel;
import android.os.Parcelable;

public class GDArCameraParam implements Parcelable {

    public int imageFormat;

    public int dataType;

    public int cameraUseType;

    public int imageWidth;

    public int imageHeight;

    public String cameraId;

    public GDArCameraParam() {
    }

    protected GDArCameraParam(Parcel in) {
        imageFormat = in.readInt();
        dataType = in.readInt();
        cameraUseType = in.readInt();
        imageWidth = in.readInt();
        imageHeight = in.readInt();
        cameraId = in.readString();
    }

    public static final Creator<GDArCameraParam> CREATOR = new Creator<GDArCameraParam>() {
        @Override
        public GDArCameraParam createFromParcel(Parcel in) {
            return new GDArCameraParam(in);
        }

        @Override
        public GDArCameraParam[] newArray(int size) {
            return new GDArCameraParam[size];
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
    }

    @Override
    public String toString() {
        return "ArCameraParam{" +
                "imageFormat=" + imageFormat +
                ", dataType=" + dataType +
                ", cameraUseType=" + cameraUseType +
                ", imageWidth=" + imageWidth +
                ", imageHeight=" + imageHeight +
                ", cameraId='" + cameraId + '\'' +
                '}';
    }
}
