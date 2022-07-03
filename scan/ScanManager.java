package edu.sysnet.skimmer.bluetoothscanner.scan;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import edu.sysnet.skimmer.bluetoothscanner.ScanActivity;
import edu.sysnet.skimmer.bluetoothscanner.bluetooth.BtReceiver;
import edu.sysnet.skimmer.bluetoothscanner.layout.DeviceScannerFragment;
import edu.sysnet.skimmer.bluetoothscanner.location.GPSTracker;
import edu.sysnet.skimmer.bluetoothscanner.wifi.WifiReceiver;

/**
 * Manages bluetooth, wifi, and gps scanning, including interactions with the view.
 */
public class ScanManager {
    private static GPSTracker gpsTracker;
    private static WifiReceiver wifiReceiver;
    private static BtReceiver btReceiver;
    private Context activity;

    /**
     * Whether we are scanning at all
     *
     * @return whether either of the scan modes is enabled
     */
    public boolean isScanning() {
        return isBluetoothScanning() || isWifiScanning();
    }

    /**
     * Whether wifi scanning is enabled in the user settings
     *
     * @return whether wifi scanning is enabled
     */
    public boolean wifiScanEnabled() {
        final SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        return sharedPref.getBoolean("enableWifiScan", false);
    }

    /**
     * Whether bt scanning is enabled in the user settings
     *
     * @return whether bt scanning is enabled
     */
    public boolean btScanEnabled() {
        final SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        return sharedPref.getBoolean("enableBtScan", true);
    }

    /**
     * Resets the scanning sequence for bluetooth and wifi depending on the settings
     */
    public void reset() {
        toggleWifiScanning(wifiScanEnabled());
        toggleBtScanning(btScanEnabled());
    }

    /**
     * Stops the scanning sequence of both bluetooth and wifi.
     */
    public void stop() {
        toggleWifiScanning(false);
        toggleBtScanning(false);
    }

    /**
     * Destroys this scan manager, deallocating it's receivers
     */
    public void destroy() {
        stop();
        try {
            activity.unregisterReceiver(btReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            activity.unregisterReceiver(wifiReceiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    /**
     * Gets the number of bt scans which have occurred
     *
     * @return the number of scans
     */
    public int getBtScanCount() {
        return btReceiver.scanNumber;
    }

    public GPSTracker getGpsTracker() {
        return gpsTracker;
    }

    public enum ScanType {
        bluetooth,
        wifi
    }

    /**
     * Creates a new scan manager which can help with toggling certain scan modes
     *
     * @param gpsTracker the gps tracker
     */
    public ScanManager(GPSTracker gps, Activity activity, DeviceScannerFragment view) {
        if (gpsTracker != null) {
            gpsTracker.pause();
        }
        gpsTracker = gps;
        this.activity = activity;

        if (btReceiver != null) {
            try {
                btReceiver.context.unregisterReceiver(btReceiver);
            } catch (Exception ignored) {
            }
        }
        if (wifiReceiver != null) {
            wifiReceiver.stopScanning();
        }

        // Register Bluetooth Receiver
        btReceiver = new BtReceiver(gpsTracker, activity, view);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.registerReceiver(btReceiver, filter);

        // Register Wifi Receiver
        wifiReceiver = new WifiReceiver(activity, view);
    }

    /**
     * Toggles gps scanning on or off
     *
     * @param on whether to turn scanning on or off
     */
    private void toggleGpsScanning(final boolean on) {
        if (on) {
            try {
                if (gpsTracker != null) {
                    gpsTracker.checkLocationSettingsAndStart();
                }
            } catch (Exception ignored) {
            }
        } else {
            try {
                if (gpsTracker != null) {
                    gpsTracker.pause();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Toggles wifi scanning on or off
     *
     * @param on whether to turn scanning on or off
     */
    private void toggleWifiScanning(final boolean on) {
        if (on) {
            toggleGpsScanning(true);
            try {
                wifiReceiver.startScanning();
            } catch (Exception ignored) {
            }
        } else {
            if (!isBluetoothScanning()) {
                toggleGpsScanning(false);
            }
            try {
                wifiReceiver.stopScanning();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Toggles bluetooth scanning on or off
     *
     * @param on whether to turn scanning on or off
     */
    private void toggleBtScanning(final boolean on) {
        if (on) {
            toggleGpsScanning(true);
            try {
                btReceiver.setDiscovering(true);
                btReceiver.startDiscovery();
            } catch (Exception ignored) {
            }
        } else {
            if (!isWifiScanning()) {
                toggleGpsScanning(false);
            }
            try {
                btReceiver.setDiscovering(false);
                btReceiver.cancelDiscovery();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Notifies the manager that the location was inaccurate. Turns off scanning if this is not
     * overridden
     */
    public void notifyInaccurateLocation() {
        final SharedPreferences contextPreferences =
                PreferenceManager.getDefaultSharedPreferences(
                        activity.getApplicationContext()
                );
        boolean inaccurateLocation =
                contextPreferences.getBoolean("inaccurateLocation", false);
        if (isScanning() && !inaccurateLocation) {
            stop();
        }
    }

    /**
     * Whether wifi scanning is enabled
     *
     * @return true or false
     */
    public boolean isWifiScanning() {
        return wifiReceiver.getScanning();
    }

    /**
     * Whether bluetooth scanning is enabled
     *
     * @return true or false
     */
    public boolean isBluetoothScanning() {
        return btReceiver.getDiscovering();
    }

    /**
     * Changes the bluetooth scan interval to whatever the new interval needs to be by using a
     * substring
     *
     * @param defaultScan whether to do a default scan interval (OS determined) or not. 0 for OS
     *                    determined
     * @param refireScan  whether to start up scanning again or not
     * @param newInterval the new scan interval to use for scanning. Used if defaultScan is
     *                    non-zero
     */
    public void changeBtScanInterval(int defaultScan, boolean refireScan, String newInterval) {
        if (defaultScan != 0) {
            btReceiver.enableInterval();
            newInterval = newInterval.substring(0, newInterval.length() - 4);
            try {
                Integer interval = (int) (Float.valueOf(newInterval) * 1000);
                btReceiver.setInterval(interval);
            } catch (NumberFormatException e) {
                // Ignore
            }
        } else {
            btReceiver.disableInterval();
            btReceiver.disableIntervalTimer();
        }

        if (isBluetoothScanning() && refireScan) {
            btReceiver.cancelDiscovery();
            toggleBtScanning(true);
        }
    }

}
