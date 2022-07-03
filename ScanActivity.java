package edu.sysnet.skimmer.bluetoothscanner;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.android.gms.dynamic.SupportFragmentWrapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.sysnet.skimmer.bluetoothscanner.layout.DeviceScannerFragment;
import edu.sysnet.skimmer.bluetoothscanner.location.GPSLocationListener;
import edu.sysnet.skimmer.bluetoothscanner.location.GPSTracker;
import edu.sysnet.skimmer.bluetoothscanner.scan.ScanManager;
import edu.sysnet.skimmer.bluetoothscanner.system.KioskHandler;
import edu.sysnet.skimmer.bluetoothscanner.system.SystemUtil;
import edu.sysnet.skimmer.bluetoothscanner.update.ApkUpdateAsyncTask;
import edu.sysnet.skimmer.bluetoothscanner.system.ErrorLogHandler;

import static io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider.REQUEST_CHECK_SETTINGS;
import static java.lang.Thread.sleep;

/**
 * List activity that acts as the main entry point into the app; interfaces with the
 * device scanner fragment, which provides feedback on device scan statistics.
 */
public class ScanActivity extends FragmentActivity {
    public static ScanActivity activity;
    public DeviceScannerFragment deviceScannerFragment; // View Fragment of device scan
    private ServiceConnection gpsServiceConnection;
    public static Typeface defaultCondensedTf;
    private GPSTracker gps;
    private static ScanManager scanManager;
    private AsyncTask<Object, Void, Void> updateAsyncTask;

    /**
     * Function that is fired when the app starts up or is restarted, will cause this activity to
     * begin, which will fire up the bluetooth, and create the device scanner fragment layout
     * element
     *
     * @param savedInstanceState the previous state of the layout
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // If it is the first time we are creating the activity
        // Try to switch the app to kiosk mode
        tryStartKiosk();

        defaultCondensedTf = Typeface.createFromAsset(getAssets(),
                "fonts/RobotoCondensed-Regular.ttf");

        // Set up the accessor for the activity
        ScanActivity.activity = this;

        // Check for APK updates
        updateAsyncTask = new ApkUpdateAsyncTask().executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR, this, false
        );

        // Modify default exception handler so that we check for updates before exiting
        overrideExceptionHandler(updateAsyncTask);

        // Initialize the rest of the fragment and the gps
        this.initializeGpsService();

    }


    /**
     * Fired when the activity shuts down; removes bluetooth reciever so that
     * there isn't background activity firing.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onDestroy() {
        try {
            gps.pause();
            unbindService(gpsServiceConnection);
        } catch (Exception ignored) {
        }

        try {
            scanManager.destroy();
        } catch (Exception ignored) {
        }

        try {
            deviceScannerFragment.deallocate();
        } catch (Exception ignored) {
        }

        super.onDestroy();
    }

    /**
     * In case of error checking, wait for the async task to complete then call the
     * default exception handler
     *
     * @param updateAsyncTask the async task to wait on completion for
     */
    private void overrideExceptionHandler(final AsyncTask updateAsyncTask) {
        final ScanActivity thisActivity = this;
        final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(
                    final Thread paramThread, final Throwable paramThrowable
            ) {
                paramThrowable.printStackTrace();
                StringWriter sw = new StringWriter();
                paramThrowable.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = sw.toString();

                try {
                    new ErrorLogHandler().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, exceptionAsString).get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                updateAsyncTask.cancel(true);
                new Thread() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        Toast.makeText(getApplicationContext(), "An error occured! Bluetana " +
                                "will now check for updates and exit!", Toast.LENGTH_LONG).show();
                        Looper.loop();
                    }
                }.start();

                try {
                    sleep(4000); // Let the Toast display before app will get shutdown
                } catch (InterruptedException e) {
                    // Ignored.
                }
                try {
                    new ApkUpdateAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, thisActivity, true).get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
        });
    }

    /**
     * Initializes the scanner layout fragment which handles scanning for bluetooth devices
     * and recording the data to a csv, as well as rendering feedback to the app's screen.
     * <p>
     * Side Effects: Callback which finishes the initialization of the main application view
     * fragment
     */
    public void initializeGpsService() {
        // Return the FragmentManager for interacting with fragments associated with
        // this activity.
        final ScanActivity sAct = this;
        gpsServiceConnection = new ServiceConnection() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                gps = ((GPSTracker.Binder) service).getService();
                // Do not change this line! the gps service needs a deviceScannerFragment to
                // associate with the location listener
                initializeMainFragment(gps);
                gps.startservice(
                        sAct.getBaseContext(), new GPSLocationListener(deviceScannerFragment)
                );
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        bindService(new Intent(this, GPSTracker.class),
                gpsServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Initializes the main scanner fragment; called once the gps service is connected to ensure
     * proper initialization of resources.
     *
     * @param gps the gps tracker for accessing location data
     */
    public void initializeMainFragment(GPSTracker gps) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Create the device scanner, initialize GPS.
        deviceScannerFragment = DeviceScannerFragment.newInstance();
        gps.setView(deviceScannerFragment);

        if (scanManager != null) {
            Log.d("ACTIVITY", "SCAN MANAGER EXISTS!");
            scanManager.destroy();
        }
        scanManager = new ScanManager(gps, this, deviceScannerFragment);
        deviceScannerFragment.setScanManager(scanManager);

        android.support.v4.app.FragmentTransaction ft =
                fragmentManager.beginTransaction();
        ft.add(android.R.id.content, deviceScannerFragment).commitNow();
    }

    /**
     * Trys to start kiosk mode if this is a kiosk mode device
     */
    private void tryStartKiosk() {
        if (KioskHandler.isKioskDevice(getApplicationContext())) {
            PackageManager pm = getPackageManager();
            String packageName = getPackageName();
            ComponentName kioskName =
                    new ComponentName(this, packageName + ".ScanActivityKiosk");
            ComponentName nonKioskName =
                    new ComponentName(this, packageName + ".ScanActivityDefault");
            if (pm.getComponentEnabledSetting(kioskName) !=
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                pm.setComponentEnabledSetting(
                        kioskName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        0
                );
                pm.setComponentEnabledSetting(
                        nonKioskName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        0
                );
                System.exit(0);
            }
        }
    }


    /**
     * Creates an alert dialog which exits the application with the given string and
     * message
     *
     * @param title   the title of the dialog
     * @param message the message of the dialog
     */
    public static void createAlert(String title, String message) {
        new AlertDialog.Builder(ScanActivity.activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        System.exit(0);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Handle render of the options menu into the application so that we can select options
     *
     * @param menu the menu to render
     * @return success
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return true;
    }

    /**
     * Handles changing the device scanner fragment in response to different menu item selections
     * on the primary screen
     *
     * @param item the menuitem selected
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                deviceScannerFragment.showSettings();
                // Tell device scanner fragment to render settings screen
                return true;

            case R.id.action_scan:
                deviceScannerFragment.showScan();
                // Tell device scanner fragment to render device list screen
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Handles switching the view back to the main page when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        View view = deviceScannerFragment.getView();
        ViewSwitcher viewSwitcher = (ViewSwitcher) view.findViewById(R.id.viewSwitch);
        LinearLayout settingsPage = (LinearLayout) view.findViewById(R.id.settingsPage);
        if (viewSwitcher.getCurrentView() == settingsPage) {
            viewSwitcher.showPrevious();
        } else {
            if (!KioskHandler.isKioskDevice(getApplicationContext())) {
                moveTaskToBack(true);
                this.onPause();
            }
        }
    }

    /**
     * Handles receiving the result of the location settings dialog; handled by this class and
     * not the GPS tracker due to API restrictions
     *
     * @param requestCode the request code
     * @param resultCode  the result code
     * @param data        the data of the message
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                ScanManager sm = deviceScannerFragment.getScanManager();
                if (resultCode == Activity.RESULT_OK) {
                    sm.reset();
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    sm.notifyInaccurateLocation();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

}
