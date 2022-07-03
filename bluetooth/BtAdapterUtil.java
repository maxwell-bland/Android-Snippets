package edu.sysnet.skimmer.bluetoothscanner.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.os.Build;
import android.util.Log;

import edu.sysnet.skimmer.bluetoothscanner.ScanActivity;


public class BtAdapterUtil {
    private static BtAdapterUtil instance;
    private BluetoothAdapter BTAdapter;

    private BtAdapterUtil() {

    }

    public static BtAdapterUtil getInstance() {
        if (instance == null) {
            Log.d("BtAdapterUtil", "Initializing");
            instance = new BtAdapterUtil();
            instance.initializeAdapter();
            instance.forceEnableBluetooth();
            Log.d("BtAdapterUtil", "Finished Initializing");
        }
        return instance;
    }

    public BluetoothAdapter getBTAdapter() {
        if (BTAdapter == null) {
            initializeAdapter();
        }
        return BTAdapter;
    }

    /**
     * Initializes the bluetooth adapter for the application; if the phone allows bluetooth,
     * this will return successfully, otherwise it will render a dialog informing the user
     * that bluetooth is not supported on their phone
     *
     * @return a boolean indicating whether there is a BTAdapter or not
     */
    private void initializeAdapter() {
        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (ScanActivity.activity != null) {
                BluetoothManager bm = (BluetoothManager) ScanActivity.activity.getSystemService(
                        android.content.Context.BLUETOOTH_SERVICE);
                if (bm != null) {
                    BTAdapter = bm.getAdapter();
                }
            }
        }
        // Phone does not support Bluetooth so let the user know and exit.
        if (BTAdapter == null) {
            ScanActivity.createAlert(
                    "Not compatible", "Your phone does not support Bluetooth");
        }
    }

    /**
     * Force enables bluetooth using the BTAdapter and Bluetooth administration activities
     * this is so that the app can be run independently on startup and without user interaction
     *
     * @return a boolean indicating whether the action of enabling bluetooth was successful
     */
    public void forceEnableBluetooth() {
        if (!BTAdapter.isEnabled()) {
            BTAdapter.enable();
        }
    }
}
