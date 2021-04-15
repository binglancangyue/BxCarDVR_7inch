package com.bx.carDVR.bean;

public class UploadBean {
    private double event_lon;
    private double event_lat;
    private float car_speed;
    private float g_x_axis;
    private float g_y_axis;
    private float g_z_axis;
    private long event_time;
    private boolean auto_upload;

    public UploadBean() {

    }

    public double getEvent_lon() {
        return event_lon;
    }

    public void setEvent_lon(double event_lon) {
        this.event_lon = event_lon;
    }

    public double getEvent_lat() {
        return event_lat;
    }

    public void setEvent_lat(double event_lat) {
        this.event_lat = event_lat;
    }

    public float getCar_speed() {
        return car_speed;
    }

    public void setCar_speed(float car_speed) {
        this.car_speed = car_speed;
    }

    public float getG_x_axis() {
        return g_x_axis;
    }

    public void setG_x_axis(float g_x_axis) {
        this.g_x_axis = g_x_axis;
    }

    public float getG_y_axis() {
        return g_y_axis;
    }

    public void setG_y_axis(float g_y_axis) {
        this.g_y_axis = g_y_axis;
    }

    public float getG_z_axis() {
        return g_z_axis;
    }

    public void setG_z_axis(float g_z_axis) {
        this.g_z_axis = g_z_axis;
    }

    public long getEvent_time() {
        return event_time;
    }

    public void setEvent_time(long event_time) {
        this.event_time = event_time;
    }

    public boolean isAuto_upload() {
        return auto_upload;
    }

    public void setAuto_upload(boolean auto_upload) {
        this.auto_upload = auto_upload;
    }

}
