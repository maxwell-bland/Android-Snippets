package edu.sysnet.skimmer.bluetoothscanner.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.provider.ContactsContract;
import android.support.annotation.RequiresApi;

import java.util.List;
import java.util.Timer;

import edu.sysnet.skimmer.bluetoothscanner.data.DataRecorder;
import edu.sysnet.skimmer.bluetoothscanner.layout.DeviceScannerFragment;
import edu.sysnet.skimmer.bluetoothscanner.data.DeviceDatapoint;
import edu.sysnet.skimmer.bluetoothscanner.layout.devicelist.DeviceListFragment;
import edu.sysnet.skimmer.bluetoothscanner.network.NetworkStateReceiver;

/**
 * Handles recieving wireless device broadcasts and registering them in the UI/CSV File
 */
public class WifiReceiver extends BroadcastReceiver {
    // This is actually a singleton but from the outside it doesn't appear to be
    private boolean wifiInitialEnabled = false;
    private WifiManager myWifiManager;
    private Context context;
    private boolean scanning;
    private DeviceScannerFragment view;
    private CountDownTimer unjamCountdown;

    /**
     * Instantiates the receiver
     */
    public WifiReceiver(Context context, DeviceScannerFragment view) {
        this.context = context;
        myWifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        this.view = view;
    }

    /**
     * Starts scanning for wireless access points, registers a receiver that will wait for new
     * results
     */
    public void startScanning() {
        scanning = true;
        wifiInitialEnabled = myWifiManager.isWifiEnabled();
        // Set wifi enabled and wait for it
        myWifiManager.setWifiEnabled(true);
        //noinspection StatementWithEmptyBody
        while (!myWifiManager.isWifiEnabled()) {
        }
        myWifiManager.startScan();
        register();

        // Try to unjam wifi after five seconds of no records
        unjamCountdown = new CountDownTimer(90000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public void onFinish() {
                ConnectivityManager connManager = (ConnectivityManager)
                        context.getSystemService(Context.CONNECTIVITY_SERVICE);
                assert connManager != null;
                NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);


                try {
                    if (mWifi.getState() != NetworkInfo.State.CONNECTING &&
                            !view.getDataRecorder().isUploading()) {
                        myWifiManager.setWifiEnabled(false);
                        myWifiManager.setWifiEnabled(true);
                    }
                } catch (Exception ignored) {
                }
                unjamCountdown.start();
            }
        }.start();
    }

    /**
     * Registers this reciever, checks if one already exists and if so, removes it
     */
    private void register() {
        IntentFilter i = new IntentFilter();
        i.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(this, i);
    }

    /**
     * Stops the scanning for wireless access points, unregisters the receiver.
     */
    public void stopScanning() {
        scanning = false;
        try {
            context.unregisterReceiver(this);
        } catch (Exception ignored) {
        }
        if (unjamCountdown != null) {
            unjamCountdown.cancel();
        }
    }

    /**
     * Recieves the next intent with the wireless device which has been discovered
     *
     * @param context the application context in which the intent was sent
     * @param intent  the intent which contains data on the wireless device
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onReceive(Context context, Intent intent) {
        // List available APs
        List<ScanResult> scans = myWifiManager.getScanResults();
        if (unjamCountdown != null) {
            unjamCountdown.cancel();
            unjamCountdown.start();
        }
        if (scans != null && !scans.isEmpty()) {
            for (ScanResult scan : scans) {
                DeviceDatapoint dp =
                        new WifiDeviceDatapoint(scan,
                                view.getScanManager().getBtScanCount(),
                                view.getScanManager().getGpsTracker(),
                                view.getContext());
                view.updateDatapoint(dp);
                // Record datapoint to csv
                view.getDataRecorder().recordDevice(dp);
            }
        }
        view.notifyScanFinished();
    }


    /**
     * Whether the receiver is actively scanning or not
     *
     * @return true or false
     */
    public boolean getScanning() {
        return scanning;
    }
}
