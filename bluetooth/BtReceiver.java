package edu.sysnet.skimmer.bluetoothscanner.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import edu.sysnet.skimmer.bluetoothscanner.data.DataRecorder;
import edu.sysnet.skimmer.bluetoothscanner.layout.DeviceScannerFragment;
import edu.sysnet.skimmer.bluetoothscanner.location.GPSTracker;

import static android.app.PendingIntent.getActivity;

/**
 * Created by Maxwell on 1/31/18.
 */

/**
 * Internal broadcast receiver for bluetooth device discovery within this device scanner
 * fragment each device item that is discovered is logged to a csv file, with the scan
 * number as one column.
 */
public class BtReceiver extends BroadcastReceiver {
    private final BtAdapterUtil btAdaptUtil;
    private boolean discovering;
    private boolean intervalScan;
    private boolean connectedToPower;
    public int scanNumber = 0;
    private DeviceScannerFragment view;
    private GPSTracker gpsTracker;
    private Integer lastInterval;
    public Context context;
    Timer timer;

    public BtReceiver(GPSTracker gpsTracker, Context context, DeviceScannerFragment view) {
        this.gpsTracker = gpsTracker;
        btAdaptUtil = BtAdapterUtil.getInstance();
        this.lastInterval = 5000;
        this.context = context;
        this.intervalScan = true;
        this.view = view;
        discovering = false;
        timer = new Timer();
    }

    public void disableInterval() {
        intervalScan = false;
    }

    public void disableIntervalTimer() {
        timer.cancel();
    }

    public void setInterval(Integer interval) {
        lastInterval = interval;
    }

    public void enableInterval() {
        intervalScan = true;
    }

    public void startInterval() {
        timer.cancel();
        final Handler handler = new Handler();
        timer = new Timer();
        TimerTask doIntervalScanReset = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @SuppressWarnings("unchecked")
                    public void run() {
                        btAdaptUtil.getBTAdapter().cancelDiscovery();
                        startDiscovery();
                    }
                });
            }
        };
        timer.schedule(doIntervalScanReset, lastInterval.longValue(), lastInterval.longValue());
    }

    public void setDiscovering(boolean setTo) {
        this.discovering = setTo;
    }

    public void startDiscovery() {
        if (discovering) {
            btAdaptUtil.forceEnableBluetooth();
            if (!btAdaptUtil.getBTAdapter().startDiscovery()) {
                Timer timer = new Timer();
                final Handler handler = new Handler();
                TimerTask tryAgain = new TimerTask() {
                    @Override
                    public void run() {
                        handler.post(new Runnable() {
                            @SuppressWarnings("unchecked")
                            public void run() {
                                startDiscovery();
                            }
                        });
                    }
                };
                timer.schedule(tryAgain, 500);
                return;
            }
            scanNumber++;
            if (intervalScan) {
                startInterval();
            }
        }
    }

    public void cancelDiscovery() {
        btAdaptUtil.getBTAdapter().cancelDiscovery();
        if (intervalScan) {
            timer.cancel();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("Scanner", "Received Intent " + action);
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            if (!discovering) {
                return;
            }

            if (view.getUploadProgress() == 100) {
                view.setUploadProgress(0);
            }

            // Get discovered device
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            // Get data needed for logging
            int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

            // Initialize datapoint
            final BtDeviceDatapoint dataPoint = new BtDeviceDatapoint(device,
                    "Classic", rssi, scanNumber, gpsTracker, context);
            Log.d("Scanner", "Device Discovered " + dataPoint.toCsv());

            view.updateDatapoint(dataPoint);
            // Record datapoint to csv
            view.getDataRecorder().recordDevice(dataPoint);
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            view.notifyScanFinished();
            if (discovering && !intervalScan) {
                Log.d("Scanner", "Starting scan #:" + scanNumber);
                startDiscovery();
            }
        }
    }

    /**
     * Gets whether the bt receiver is in the process of scanning or not
     *
     * @return whether the bluetooth receiver is currently discovering
     */
    public boolean getDiscovering() {
        return discovering;
    }
}
