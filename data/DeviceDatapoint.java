package edu.sysnet.skimmer.bluetoothscanner.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import edu.sysnet.skimmer.bluetoothscanner.bluetooth.BtAdapterUtil;
import edu.sysnet.skimmer.bluetoothscanner.location.GPSTracker;

public abstract class DeviceDatapoint {
    protected String androidMacAddress;
    protected String androidDeviceId;
    protected String geoProvider;
    public final String deviceAddress;
    public String deviceName;
    public int deviceType;
    protected float geoAccuracy;
    protected double longitude;
    protected double latitude;
    protected double altitude;
    protected String foundTimeGMT;
    protected Date foundTime;
    protected int scanNumber;

    @SuppressLint("HardwareIds")
    public DeviceDatapoint(Context context, GPSTracker tracker, int scanNumber, String macAddr,
                           int type, String name) {
        androidDeviceId = Settings.Secure.getString(context.getContentResolver(),
                        Settings.Secure.ANDROID_ID);
        androidMacAddress = BtAdapterUtil.getInstance().getBTAdapter().getAddress();
        geoProvider = tracker.getGeoProvider();
        geoAccuracy = tracker.locList.accuracy;
        longitude = tracker.locList.longitude;
        latitude = tracker.locList.latitude;
        altitude = tracker.locList.altitude;
        deviceType = type;
        deviceName = name;
        deviceAddress = macAddr.toUpperCase();
        this.scanNumber = scanNumber;

        foundTime = Calendar.getInstance().getTime();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormatGmt =
                new SimpleDateFormat("yyyyMMdd kk:mm:ss");
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        foundTimeGMT = (dateFormatGmt.format(new Date())+"");
    }

    /**
     * Returns csv header list for this datapoint as a string.
     * @return the list of comma-seperated header names
     */
    public String getHeaders() {
        String csvString = "";
        /* Note: removed for privacy */
        return csvString;
    }

    public abstract String toCsv();
}
