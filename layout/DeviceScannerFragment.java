package edu.sysnet.skimmer.bluetoothscanner.layout;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import edu.sysnet.skimmer.bluetoothscanner.ScanActivity;
import edu.sysnet.skimmer.bluetoothscanner.layout.devicelist.BtDeviceListAdapter;
import edu.sysnet.skimmer.bluetoothscanner.layout.devicelist.DeviceListFragment;
import edu.sysnet.skimmer.bluetoothscanner.layout.devicelist.WifiDeviceListAdapter;
import edu.sysnet.skimmer.bluetoothscanner.location.GpsDatapoint;
import edu.sysnet.skimmer.bluetoothscanner.network.NetworkStateReceiver;
import edu.sysnet.skimmer.bluetoothscanner.scan.ScanManager;
import edu.sysnet.skimmer.bluetoothscanner.system.PowerUtil;
import edu.sysnet.skimmer.bluetoothscanner.R;
import edu.sysnet.skimmer.bluetoothscanner.data.DataRecorder;
import edu.sysnet.skimmer.bluetoothscanner.data.DeviceDatapoint;
import edu.sysnet.skimmer.bluetoothscanner.wifi.WifiDeviceDatapoint;
import edu.sysnet.skimmer.toggle.LabeledSwitch;
import edu.sysnet.skimmer.toggle.interfaces.OnToggledListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import static edu.sysnet.skimmer.bluetoothscanner.BuildConfig.VERSION_NAME;
import static java.lang.Math.max;

/**
 * A fragment representing a list of Items.Contains the main view logic of the app, which isn't
 * much. Mostly handles notifications about scanning and updating the text to say what's up.
 */
public class DeviceScannerFragment extends Fragment {

    private BtDeviceListAdapter btDeviceListAdapter;
    private WifiDeviceListAdapter wifiDeviceListAdapter;
    // interfacing with the scan device
    private PowerUtil powerUtil;
    private View view;
    private Semaphore viewLock;
    private NetworkStateReceiver networkStateReceiver;
    private DataRecorder myDataRecorder;
    private boolean powerOnlyScan;
    private int uploadProgress = 0;
    private boolean scanOnce;
    private ScanManager scanManager;
    private LabeledSwitch scanToggle;
    private Context context;
    private DeviceListFragment wifiListFrag;
    private DeviceListFragment btListFrag;
    private boolean scanningWhenPaused = true;


    /**
     * Creates a new instance of this view with the given bluetooth adapter, sets up the adapter
     * and gives the fragment of view back.
     *
     * @return the new list of devices
     */
    public static DeviceScannerFragment newInstance() {
        DeviceScannerFragment fragment = new DeviceScannerFragment();
        return fragment;
    }

    /**
     * Useless constructor, doesn't really handle rendering the fragment that controls the list
     */
    public DeviceScannerFragment() {
        super();
        viewLock = new Semaphore(0);
    }

    /**
     * Fired when this fragment is generated. This function handles beginning the
     * scanning and delegating to the logger the information that is received.`
     *
     * @param savedInstanceState the previous state of this fragment's instance when
     *                           the fragment is reloaded
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        powerOnlyScan = sharedPref.getBoolean("powerOnlyScan", false);
        myDataRecorder = new DataRecorder(this);

        btDeviceListAdapter = new BtDeviceListAdapter(getActivity().getBaseContext());
        wifiDeviceListAdapter =
                new WifiDeviceListAdapter(getActivity().getBaseContext());

        if (networkStateReceiver != null) {
            context.unregisterReceiver(networkStateReceiver);
        }
        // Start network listening
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(myDataRecorder);
        context.registerReceiver(networkStateReceiver,
                new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

        if (powerUtil != null) {
            context.unregisterReceiver(powerUtil);
        }
        powerUtil = new PowerUtil(this);
        context.registerReceiver(
                powerUtil, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        );

        setRetainInstance(true);
    }

    @Override
    public void onPause() {
        scanningWhenPaused = scanManager.isScanning();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Called when the fragment actually starts doing it's business. Handles wiring together all
     * the UI components
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(
                R.layout.fragment_device_scanner, container, false
        );
        initializeUI(view);
        viewLock.release();
        return view;
    }

    /**
     * Initialize all the view logic for the fragment
     * TODO: Put the settings into their own fragment
     *
     * @param view the view
     */
    private void initializeUI(final View view) {
        initializeScanToggle(view);

        initSettingsButton(view);
        initTearCsvButton(view);
        initializeRefreshHitlistButton(view);
        instantiateDeviceLists(view);
        initPowerSettings(view);

        setTextElementText(view, R.id.versionText, VERSION_NAME);

        String lastUpdateTime = "";
        try {
            lastUpdateTime = new Date(context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0
            ).lastUpdateTime).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } finally {
            setTextElementText(view, R.id.lastUpdateText, lastUpdateTime);
        }

        setTextElementText(view, R.id.hitlistInfo, String.valueOf("0"));

        final SharedPreferences contextPreferences = initRunOnStartupSettings(view);

        initInaccurateLocSettings(view, contextPreferences);

        final TextView geoLocText = (TextView) view.findViewById(R.id.geoLocDisplay);
        geoLocText.setTypeface(ScanActivity.defaultCondensedTf);
        /*
         If you tap the geolocation on the screen it appends a dummy bt
         device datapoint to the csv which  records the current geolocation.
         */
        final DeviceScannerFragment thisFrag = this;
        geoLocText.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                GpsDatapoint geoDp = new GpsDatapoint(scanManager.getGpsTracker(), getContext());
                getDataRecorder().recordDevice(geoDp);
                Handler boldHandler = new Handler();
                final TextView geoLocText = (TextView) view.findViewById(R.id.geoLocDisplay);
                geoLocText.setTypeface(null, Typeface.BOLD);
                boldHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        geoLocText.setTypeface(null, Typeface.NORMAL);
                    }
                }, 250);
            }
        });

    }

    private void initScanIntervalSettings(View view, final SharedPreferences contextPreferences) {
        final Spinner intervalSelectSpinner = (Spinner) view.findViewById(R.id.intervalSelectSpinner);
        // Spinner Drop down elements
        List<String> intervals = new ArrayList<>();
        intervals.add("Android Default");
        intervals.add("1 sec");
        intervals.add("2.5 sec");
        intervals.add("5 sec");
        intervals.add("7.5 sec");
        intervals.add("10 sec");

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter =
                new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, intervals);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        intervalSelectSpinner.setAdapter(dataAdapter);

        int scanIntervalSetting = contextPreferences.getInt("scanIntervalSetting", 3);

        intervalSelectSpinner.setSelection(scanIntervalSetting);
        intervalSelectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                changeScanInterval(position, intervalSelectSpinner, true);
                SharedPreferences.Editor editor = contextPreferences.edit();
                editor.putInt("scanIntervalSetting", position);
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        changeScanInterval(scanIntervalSetting, intervalSelectSpinner, false);
    }

    private void initTearCsvButton(View view) {
        final Button tearCsvButton = (Button) view.findViewById(R.id.tearFileButton);
        tearCsvButton.setTypeface(ScanActivity.defaultCondensedTf);
        tearCsvButton.setOnClickListener(
                new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onClick(View v) {
                        myDataRecorder.tearFile();
                    }
                }
        );
    }

    private void initInaccurateLocSettings(View view, final SharedPreferences contextPreferences) {
        final CheckBox setInaccurateLocation =
                (CheckBox) view.findViewById(R.id.setInaccurateLocation);
        boolean inaccurateLocation =
                contextPreferences.getBoolean("inaccurateLocation", false);
        setInaccurateLocation.setChecked(inaccurateLocation);
        setInaccurateLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = contextPreferences.edit();
                editor.putBoolean("inaccurateLocation", isChecked);
                editor.commit();
            }
        });
    }

    private void initScanSwitch(View view, final SharedPreferences contextPreferences,
                                int enblWifiScanSwitch, boolean b, final String enableWifiScan) {
        final CheckBox enableWifiScanning = (CheckBox) view.findViewById(enblWifiScanSwitch);
        enableWifiScanning.setChecked(b);
        enableWifiScanning.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = contextPreferences.edit();
                editor.putBoolean(enableWifiScan, isChecked);
                editor.commit();
                if (scanToggle.isOn()) {
                    scanManager.reset();
                }
            }
        });
    }

    @NonNull
    private SharedPreferences initRunOnStartupSettings(View view) {
        final CheckBox setRunOnStartup = (CheckBox) view.findViewById(R.id.setRunOnStartup);
        final SharedPreferences contextPreferences =
                PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        boolean runOnStartup = contextPreferences.getBoolean("runOnStartup", false);
        setRunOnStartup.setChecked(runOnStartup);
        setRunOnStartup.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = contextPreferences.edit();
                editor.putBoolean("runOnStartup", isChecked);
                editor.commit();
            }
        });
        return contextPreferences;
    }

    private void initPowerSettings(View view) {
        final CheckBox setPowerOnlyScan = (CheckBox) view.findViewById(R.id.setPowerOnlyScan);
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        powerOnlyScan = sharedPref.getBoolean("powerOnlyScan", false);
        setPowerOnlyScan.setChecked(powerOnlyScan);
        setPowerOnlyScan.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                handlePowerOnlyCheck(isChecked);
            }
        });
        handlePowerOnlyCheck(powerOnlyScan);
    }

    private void initSettingsButton(View view) {
        final ImageButton bSettings = (ImageButton) view.findViewById(R.id.bSettings);
        bSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        });
    }

    private void setTextElementText(View view, int versionText2, String format) {
        final TextView versionText = (TextView) view.findViewById(versionText2);
        versionText.setText(format);
    }

    private void initializeRefreshHitlistButton(View view) {
        // Refresh Hitlist
        final Button refreshHitlist = (Button) view.findViewById(R.id.refreshHitlistButt);
        final DeviceScannerFragment thisFrag = this;
        refreshHitlist.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
            }
        });
    }

    private void initializeScanToggle(View view) {
        scanToggle = (LabeledSwitch) view.findViewById(R.id.scanToggle);
        scanToggle.setTypeface(ScanActivity.defaultCondensedTf);

        scanToggle.disabledText = "POWER DETACHED";
        scanToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (powerOnlyScan && !powerUtil.powerConnected() && !scanManager.isScanning()) {
                    showPowerDetachedPrompt();
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        try {
            scanManager.stop();
            final Handler handler = new Handler();
            final DeviceScannerFragment thisFrag = this;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    final SharedPreferences contextPreferences =
                            PreferenceManager.getDefaultSharedPreferences(
                                    getActivity().getApplicationContext()
                            );
                    initScanSwitch(view, contextPreferences, R.id.enblBtScanSwitch,
                            scanManager.btScanEnabled(), "enableBtScan");
                    initScanSwitch(view, contextPreferences, R.id.enblWifiScanSwitch,
                            scanManager.wifiScanEnabled(), "enableWifiScan");
                    initScanIntervalSettings(view, contextPreferences);

                    // See if we need to resume scanning in onCreate
                    if (thisFrag.scanningWhenPaused) {
                        scanManager.reset();
                    }

                    if (!powerOnlyScan) {
                        if (scanManager.isScanning()) {
                            scanToggle.setOn(true);
                        }
                    }
                }
            }, 1000);
        } catch (Exception ignored) {
        }
    }

    private void instantiateDeviceLists(View view) {

        final Button clearListButton = (Button) view.findViewById(R.id.clearAdapter);
        clearListButton.setTypeface(ScanActivity.defaultCondensedTf);

        scanToggle.setOnToggledListener(new OnToggledListener() {
            @Override
            public void onSwitched(LabeledSwitch labeledSwitch, boolean isOn) {
                if (labeledSwitch.isEnabled()) {
                    if (isOn) {
                        scanManager.reset();
                    } else {
                        scanManager.stop();
                    }
                }
            }
        });

        btListFrag = DeviceListFragment.newInstance(
                getImageAsset("bt.png"), btDeviceListAdapter
        );
        wifiListFrag = DeviceListFragment.newInstance(
                getImageAsset("wifi.png"), wifiDeviceListAdapter
        );

        // Instantiate a ViewPager and a PagerAdapter.
        ViewPager mPager = (ViewPager) view.findViewById(R.id.devListPager);
        ScanListSlidePagerAdapter mPagerAdapter = new ScanListSlidePagerAdapter(
                getChildFragmentManager(), this, scanToggle, scanManager,
                btListFrag, wifiListFrag, clearListButton);
        mPager.setAdapter(mPagerAdapter);
    }

    /**
     * Shows the dialog prompt for when the app's power is detached but the scan button is pressed.
     * Asks the user if they would like to scan one time only or disable the power only mode
     */
    private void showPowerDetachedPrompt() {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();

        alertDialog.setTitle("Toggle Scanning");

        alertDialog.setMessage("Power only mode is enabled but power is detached." +
                " Would you like to disable this scan mode?");

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                "Disable", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final CheckBox setPowerOnlyScan = (CheckBox)
                                view.findViewById(R.id.setPowerOnlyScan);
                        setPowerOnlyScan.setChecked(false);
                        handlePowerOnlyCheck(false);
                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,
                "Scan Once", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        scanOnce = true;
                        scanToggle.setOn(true);
                        scanManager.reset();
                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                "Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Ignored
                    }
                });

        alertDialog.show();
    }

    /**
     * Changes the bluetooth scan interval
     *
     * @param position              the position in the menu for which the spinner is selected
     * @param intervalSelectSpinner the spinner which is used to select the interval
     * @param refireScan            whether or not to refire the scan
     */
    private void changeScanInterval(
            int position, Spinner intervalSelectSpinner, boolean refireScan
    ) {
        String newInterval = (String) intervalSelectSpinner.getAdapter().getItem(position);
        scanManager.changeBtScanInterval(position, refireScan, newInterval);
    }

    /**
     * Modifies the state of the system when power only scan is checked on and off
     *
     * @param isChecked whether it is checked on or not
     */
    private void handlePowerOnlyCheck(boolean isChecked) {
        try {
            if (isChecked) {
                powerOnlyScan = true;
                if (powerUtil.powerConnected()) {
                    scanManager.reset();
                    scanToggle.setOn(true);
                } else {
                    scanManager.stop();
                    scanToggle.setOn(false);
                }
                scanToggle.setEnabled(false);
                SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("powerOnlyScan", true);
                editor.commit();
            } else {
                powerOnlyScan = false;
                scanToggle.setEnabled(true);
                if (scanManager.isScanning()) {
                    scanManager.reset();
                    scanToggle.setOn(true);
                } else {
                    scanManager.stop();
                    scanToggle.setOn(false);
                }
                SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("powerOnlyScan", false);
                editor.commit();
            }
        } catch (Exception ignored) {
            // Sometimes the intent is fired before the view is ready
        }
    }

    /**
     * Updates the geolocation on the main page of the application
     *
     * @param lat the latitude
     * @param lon the longitude
     */
    public void updateGeoLoc(double lat, double lon) {
        try {
            viewLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            final TextView geoLocText = (TextView) view.findViewById(R.id.geoLocDisplay);
            geoLocText.setText(String.format(Locale.ENGLISH, "%.4f   %.4f", lat, lon));
            viewLock.release();
        }
    }

    @Override
    public void onDestroy() {
        deallocate();
        super.onDestroy();
    }

    /**
     * Deallocates this fragment's receivers.
     */
    public void deallocate() {
        try {
            context.unregisterReceiver(powerUtil);
        } catch (Exception ignored) {
        }

        try {
            networkStateReceiver.removeListener(myDataRecorder);
        } catch (Exception ignored) {
        }

        try {
            context.unregisterReceiver(networkStateReceiver);
        } catch (Exception ignored) {
        }
    }

    /**
     * Shows the settings menu in the fragment.
     */
    public void showScan() {
        ViewSwitcher viewSwitcher = (ViewSwitcher) view.findViewById(R.id.viewSwitch);
        LinearLayout scanPage = (LinearLayout) view.findViewById(R.id.scanPage);
        if (viewSwitcher.getCurrentView() != scanPage) {
            viewSwitcher.showPrevious();
        }
    }

    /**
     * Shows the settings menu in the fragment.
     */
    public void showSettings() {
        ViewSwitcher viewSwitcher = (ViewSwitcher) view.findViewById(R.id.viewSwitch);
        LinearLayout settingsPage = (LinearLayout) view.findViewById(R.id.settingsPage);
        if (viewSwitcher.getCurrentView() != settingsPage) {
            viewSwitcher.showNext();
        }
    }

    /**
     * Handles display of upload progress. Zero for no progress, 100 for complete, anything else
     * to show a progress bar. Starts the changes on the UI thread.
     *
     * @param uploadProgress the upload progress
     */
    public void setUploadProgress(final int uploadProgress) {
        this.uploadProgress = uploadProgress;
        Activity act = getActivity();
        if (act != null) {
            act.runOnUiThread(new Runnable() {
                @SuppressLint({"DefaultLocale", "SetTextI18n"})
                @Override
                public void run() {
                    try {
                        viewLock.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        final Button uploadButton = (Button) view.findViewById(R.id.tearFileButton);
                        if (uploadProgress == 0) {
                            uploadButton.setEnabled(true);
                            uploadButton.setTextColor(Color.WHITE);
                            uploadButton.setText("UPLOAD");
                        } else if (uploadProgress == 100) {
                            uploadButton.setEnabled(false);
                            uploadButton.setTextColor(Color.GRAY);
                            uploadButton.setText("UPLOADED");
                        } else if (uploadProgress == -1) {
                            uploadButton.setEnabled(true);
                            uploadButton.setTextColor(Color.WHITE);
                            uploadButton.setText("FAILED");
                        } else {
                            uploadButton.setEnabled(false);
                            uploadButton.setTextColor(Color.GRAY);
                            uploadButton.setText("UPLOADING");
                        }
                        viewLock.release();
                    }
                }
            });
        }
    }

    /**
     * Gets the current upload progress. If > 0, then the upload is in progress or finished. If
     * == 0, then the upload is not occurring. If 100, complete. -1 for error.
     *
     * @return the local uploadProgress on the view
     */
    public int getUploadProgress() {
        return uploadProgress;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    }

    /**
     * Sets the last uploaded text on the settings page by running an update on the UI thread
     *
     * @param lastUploaded the last uploaded date
     */
    public void setLastUploaded(final Date lastUploaded) {
        // Potentially not thread safe
        Activity act = getActivity();
        if (act != null) {
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        viewLock.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        setTextElementText(view, R.id.lastUploadText, lastUploaded.toString());
                        viewLock.release();
                    }
                }
            });
        }
    }

    /**
     * Sets the last hitlist time of the bluetooth hitlist
     *
     * @param date        the date of the last update
     * @param hitlistUtil the hitlist utility which updated the hitlist
     */
    public void setLastHitlistTime(final Date date, final HitlistUtil hitlistUtil) {
        Activity act = getActivity();
        if (act != null) {
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        viewLock.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        if (hitlistUtil == btHitlist) {
                        }
                        viewLock.release();
                    }
                }
            });
        }
    }

    /**
     * Method called when power is plugged into the device. Handles logic for power only scan.
     *
     * @param powerOn whether the power is on
     */
    public void notifyCharging(boolean powerOn) {
        if (powerOnlyScan) {
            if (powerOn) {
                scanManager.reset();
                scanToggle.setOn(true);
                scanOnce = false;
            } else {
                if (!scanOnce) {
                    scanToggle.setOn(false);
                    if (scanManager.isScanning()) {
                        scanManager.stop();
                    }
                }
            }
        }
    }

    /**
     * Method called when a device scan is finished. If powerOnly and in scan once mode and
     * scanning, turn off scanning after one run.
     */
    public void notifyScanFinished() {
        if (powerOnlyScan) {
            if (!powerUtil.powerConnected() && scanOnce) {
                scanOnce = false;
                scanToggle.setOn(false);
                scanManager.stop();
            }
        }
    }

    /**
     * Updates a given device datapoint in the proper device list adapter
     *
     * @param dataPoint the datapoint to update in the adapter
     */
    public void updateDatapoint(final DeviceDatapoint dataPoint) {
        // Make method thread safe
        Activity act = getActivity();
        if (act != null) {
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dataPoint instanceof WifiDeviceDatapoint) {
                        wifiDeviceListAdapter.updateDatapoint(dataPoint);
                        wifiListFrag.setBgAlpha();
                    } else {
                        btDeviceListAdapter.updateDatapoint(dataPoint);
                        btListFrag.setBgAlpha();
                    }
                }
            });
        }

        if (uploadProgress == 100) {
            setUploadProgress(0);
        }
    }

    /**
     * Gets the scan manager of the fragment, to toggle scanning options
     *
     * @return the scanning manager
     */
    public ScanManager getScanManager() {
        return scanManager;
    }

    /**
     * Sets the scan manager for the fragment, so that scanning options may be toggled
     *
     * @param scanManager the scan manager to set for the fragment
     */
    public void setScanManager(ScanManager scanManager) {
        this.scanManager = scanManager;
    }

    /**
     * Gets an image asset from the assets folder by filename
     *
     * @param fileName the filename of the bitmap
     * @return the bitmap of the image
     */
    public Bitmap getImageAsset(String fileName) {

        AssetManager assetmanager = context.getAssets();
        InputStream is = null;
        try {

            is = assetmanager.open(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return BitmapFactory.decodeStream(is);
    }

    /**
     * Returns the data recorder of this fragment
     *
     * @return the data recorder
     */
    public DataRecorder getDataRecorder() {
        return myDataRecorder;
    }
}
