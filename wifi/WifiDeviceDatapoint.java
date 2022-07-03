package edu.sysnet.skimmer.bluetoothscanner.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;

import edu.sysnet.skimmer.bluetoothscanner.location.GPSTracker;
import edu.sysnet.skimmer.bluetoothscanner.data.DeviceDatapoint;

/**
 * Represents a single wifi device datapoint, handles packaging the device information and
 * getting strings for it
 */
public class WifiDeviceDatapoint extends DeviceDatapoint {
    private static final String SCAN_MODE = "wifi";
    public String capabilities;
    public int frequency;
    public int level;

    /**
     * Creates a new datapoint
     * @param device the device
     * @param scanNumber the scan num
     * @param tracker the gps coord
     * @param context the context
     */
    WifiDeviceDatapoint(ScanResult device, int scanNumber, GPSTracker tracker,
                        Context context) {
        super(context, tracker, scanNumber, device.BSSID, 5, device.SSID);
        capabilities = device.capabilities;
        frequency = device.frequency;
        level = device.level;
    }


    @Override
    public String toCsv() {
        String csvString = "";
        csvString += scanNumber + ",";
        csvString += deviceAddress + ",";
        csvString += capabilities + ",";
        csvString += frequency + ",";
        csvString += deviceType + ",";
        csvString += deviceName + ",";
        csvString += SCAN_MODE + ",";
        csvString += level + ",";
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
