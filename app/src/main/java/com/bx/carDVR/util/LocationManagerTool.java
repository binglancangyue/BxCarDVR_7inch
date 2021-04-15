package com.bx.carDVR.util;

import android.annotation.SuppressLint;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.bx.carDVR.app.DvrApplication;
import com.bx.carDVR.bean.Configuration;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static android.content.Context.LOCATION_SERVICE;


/**
 * @author Altair
 * @date :2020.03.31 下午 06:01
 * @description:
 */
public class LocationManagerTool {
    private LocationManager mLocationManager;
    private Location mLocation;
    private static final String TAG = "LocationManagerTool";
    private AMapLocation mGaoDeMapLocation;

    private AMapLocationClient mLocationClient = null;
    private AMapLocationClientOption mLocationOption = null;

    private static LocationManagerTool sLocationManagerTool;

    private LocationManagerTool(){
    }

    public static LocationManagerTool getInstance(){
        if (sLocationManagerTool == null) {
            sLocationManagerTool = new LocationManagerTool();
        }
        return sLocationManagerTool;
    }

    @SuppressLint("MissingPermission")
    public Location getLocation() {
        mLocationManager =
                (LocationManager) DvrApplication.getDvrApplication().getSystemService(LOCATION_SERVICE);
        if (mLocationManager == null) {
            return null;
        }
        mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (mLocation == null) {
            mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0,
                mLocationListener);
        mLocationManager.addGpsStatusListener(statusListener);
        Log.d(TAG, "getLocation: " + mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
        return mLocation;
    }


    public void startGaoDe() {
        if (mLocationManager == null) {
            //初始化定位
            mLocationClient = new AMapLocationClient(DvrApplication.getDvrApplication());
            //初始化AMapLocationClientOption对象
            mLocationOption = new AMapLocationClientOption();
            //设置定位回调监听
            mLocationClient.setLocationListener(aMapLocationListener);
            mLocationOption.setInterval(1000);
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationClient.setLocationOption(mLocationOption);
            mLocationClient.stopLocation();
            mLocationClient.startLocation();
            Log.d(TAG, "gaoDe: ");
        } else {
            mLocationClient.startLocation();
        }
    }

    private AMapLocationListener aMapLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {
            mGaoDeMapLocation = aMapLocation;
            int speed = (int) (aMapLocation.getSpeed() * 3.6);
            if (Configuration.DEBUG) {
                LogUtils.getInstance().d(TAG,"onLocationChanged Latitude : "+aMapLocation.getLatitude()+" , Longitude :"+aMapLocation.getLongitude());
            }
            if(mGpsChangeListener != null){
                mGpsChangeListener.onSpeedChange(speed);
                mGpsChangeListener.onLocationChange(aMapLocation.getLatitude(),aMapLocation.getLongitude());
            }
        }
    };

    public Location getLastKnownLocation() {
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            @SuppressLint("MissingPermission") Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged: ");
            if (location == null) return;
            String strResult = "\r\n"
                    + "准确度:" + location.getAccuracy() + "\r\n"
                    + "海拔:" + location.getAltitude() + "\r\n"
                    + "方位:" + location.getBearing() + "\r\n"
                    + "getElapsedRealtimeNanos:" + String.valueOf(location.getElapsedRealtimeNanos()) + "\r\n"
                    + "纬度:" + location.getLatitude() + "\r\n"
                    + "经度:" + location.getLongitude() + "\r\n"
                    + "供应商:" + location.getProvider() + "\r\n"
                    + "速度:" + location.getSpeed() + "\r\n"
                    + "时间:" + getTimeByGPS(location.getTime()) + "\r\n";
            Log.d("LocationManagerTool", strResult);
//            getDetailedAddress(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged provider: " + provider + " status: " + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "onProviderDisabled: " + provider);
        }
    };

    @SuppressLint("MissingPermission")
    private GpsStatus.Listener statusListener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
           GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
            if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {//周期的报告卫星状态
                //得到所有收到的卫星的信息，包括 卫星的高度角、方位角、信噪比、和伪随机号（及卫星编号）
                Iterable<GpsSatellite> allSatellites;
                allSatellites = gpsStatus.getSatellites();
                Iterator<GpsSatellite> iterator = allSatellites.iterator();
                int numOfSatellites = 0;
                int maxSatellites = gpsStatus.getMaxSatellites();
                while (iterator.hasNext() && numOfSatellites < maxSatellites) {
                    GpsSatellite s = iterator.next();
                    if (s.getSnr() != 0) {
                        numOfSatellites++;
                    }
                }
                Log.v(TAG, "GPS satellite : " + numOfSatellites);
                if (numOfSatellites <= 3) {
                }
            }
        }
    };


    public AMapLocation getAMapLocation() {
        return mGaoDeMapLocation;
    }

    //停止定位
    public void stopLocation() {
        if (mLocationManager != null && mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
        stopGaoDeLocation();
    }


    @SuppressLint("SimpleDateFormat")
    private static String getTimeByGPS(long gpsTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(gpsTime);
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return df.format(calendar.getTime());
    }

    /**
     * 获取 Location Provider
     *
     * @return
     */
    private String getProviderName() {
        // 构建位置查询条件
        Criteria criteria = new Criteria();
        // 查询精度：高
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // 是否查询海拨：否
        criteria.setAltitudeRequired(true);
        // 是否查询方位角 : 否
        criteria.setBearingRequired(true);
        // 是否允许付费：是
        criteria.setCostAllowed(true);
        // 电量要求：低
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        // 返回最合适的符合条件的 provider ，第 2 个参数为 true 说明 , 如果只有一个 provider 是有效的 , 则返回当前
        // provider
        return mLocationManager.getBestProvider(criteria, true);
    }


    private String parseAddress(Address address) {
        return address.getAddressLine(0) + address.getAddressLine(1)
                + address.getAddressLine(2) + address.getFeatureName();
    }

    // 获取地址信息
    private List<Address> getAddressByGeoPoint(Location location) {
        List<Address> result = null;
        // 先将 Location 转换为 GeoPoint
        // GeoPoint gp =getGeoByLocation(location);
        try {
            if (location != null) {
                // 获取 Geocoder ，通过 Geocoder 就可以拿到地址信息
                Geocoder gc = new Geocoder(DvrApplication.getDvrApplication(), Locale.getDefault());
                result = gc.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private void getDetailedAddress(Location location) {
        List<Address> addressList = getAddressByGeoPoint(location);
        String address = "当前详细地址：";
        if (addressList != null && !addressList.isEmpty()) {
            address += parseAddress(addressList.get(0));
            Log.d("LocationManagerTool", "当前详细地址: " + address);
        }
    }

    public void stopGaoDeLocation() {
        if (mLocationClient != null) {
            mLocationClient.setLocationListener(null);
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
    }

    private OnGpsChangeListener mGpsChangeListener;
    public void setOnGpsChangeListener(OnGpsChangeListener gpsChangeListener) {
        this.mGpsChangeListener = gpsChangeListener;
    }

    public interface OnGpsChangeListener {
        void onSpeedChange(int speed);
        void onLocationChange(double lat,double lng);
    }


}
