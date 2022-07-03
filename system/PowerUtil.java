package edu.sysnet.skimmer.bluetoothscanner.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import edu.sysnet.skimmer.bluetoothscanner.layout.DeviceScannerFragment;

/**
 * Handles recieving the battery status updates and notifying various views so that power-only
 * scan works
 */
public class PowerUtil extends BroadcastReceiver {
    private DeviceScannerFragment scannerFragment;
    private boolean isCharging;

    /**
     * Initializes the notifier with the btReciever so that it may notify it.
     * @param scanner the fragment listening for notifications of charging
     */
    public PowerUtil(DeviceScannerFragment scanner) {
        scannerFragment = scanner;
    }

    /**
     * Handles dealing with handling battery charge status events.
     * @param context the context of this charge reciever.
     * @param intent the data values stored in the event
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        if (isCharging) {
            scannerFragment.notifyCharging(true);
        } else {
            scannerFragment.notifyCharging(false);
        }

    }

    /**
     * Tells whether or not there is power connected to the device
     * @param context the context under which to run the check
     * @return whether or not the phone is plugged in
     */
    public boolean powerConnected() {
        return isCharging;
    }
}
