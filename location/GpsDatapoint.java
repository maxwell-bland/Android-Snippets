package edu.sysnet.skimmer.bluetoothscanner.location;

import android.content.Context;

import edu.sysnet.skimmer.bluetoothscanner.data.DeviceDatapoint;

/**
 * Datapoint for recording a csv record for GPS position
 */
public class GpsDatapoint extends DeviceDatapoint {
    private static final String SCAN_MODE = "gps";

    /**
     * Creates a new GPS datapoint
     * @param tracker the gps tracker to grab data from
     * @param context the application context for getting android id
     */
    public GpsDatapoint(GPSTracker tracker, Context context) {
        super(context, tracker, 0, "", 6, "SKATELAB GEOLOC DP");
    }


    @Override
    public String toCsv() {
        String csvString = "";
        csvString += scanNumber + ",";
        csvString += deviceAddress + ",";
        csvString += "-" + ",";
        csvString += "-" + ",";
        csvString += deviceType + ",";
        csvString += deviceName + ",";
        csvString += SCAN_MODE + ",";
        csvString += "-" + ",";
        csvString += geoProvider + ",";
        csvString += geoAccuracy + ",";
        csvString += longitude + ",";
        csvString += latitude + ",";
        csvString += altitude + ",";
        csvString += foundTime.getTime() + ",";
        csvString += foundTimeGMT + ",";
        csvString += androidDeviceId + ",";
        csvString += androidMacAddress + "\n";
        return csvString;
    }
}
