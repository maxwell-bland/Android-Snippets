package edu.sysnet.skimmer.bluetoothscanner.location;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import edu.sysnet.skimmer.bluetoothscanner.R;
import edu.sysnet.skimmer.bluetoothscanner.layout.DeviceScannerFragment;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;

import static android.location.GpsStatus.GPS_EVENT_STARTED;
import static android.location.GpsStatus.GPS_EVENT_STOPPED;
import static io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider.REQUEST_CHECK_SETTINGS;

public class GPSTracker extends Service {

    boolean listening = false;
    private Context context;
    public GPSLocationListener locList;
    private LocationAccuracy trackingAccuracy = LocationAccuracy.HIGH;

    private DeviceScannerFragment view;
    private LocationRequest mLocationRequest;
    private Notification notif;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void startservice(final Context context, GPSLocationListener locList) {
        Log.d("GPSPOS", "STARTER");
        this.context = context;
        this.locList = locList;
        LocationManager mLocationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        notif = new Notification.Builder(context)
                .setContentTitle("Location Updates Service")
                .setContentText("Getting Location Updates")
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                .setTicker("Location Being Monitored")
                .build();
        mLocationManager.addGpsStatusListener(new android.location.GpsStatus.Listener() {
            public void onGpsStatusChanged(int event) {
                switch (event) {
                    case GPS_EVENT_STARTED:
                        break;
                    case GPS_EVENT_STOPPED:
                        if (view != null) {
                            if (view.getActivity() != null) {
                                view.getScanManager().notifyInaccurateLocation();
                            }
                        }
                        break;
                }
            }
        });
    }

    private Binder binder;

    @Override
    public void onCreate() {
        super.onCreate();
        binder = new Binder();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class Binder extends android.os.Binder {
        public GPSTracker getService() {
            return GPSTracker.this;
        }
    }

    public void start() {
        if (!listening) {
            listening = true;
            long mLocTrackingInterval = 0; // 5 sec
            float trackingDistance = 0;
            startForeground(69, notif);
            LocationParams.Builder builder = new LocationParams.Builder()
                    .setAccuracy(trackingAccuracy)
                    .setDistance(trackingDistance)
                    .setInterval(mLocTrackingInterval);
            SmartLocation.with(context)
                    .location()
                    .continuous()
                    .config(builder.build())
                    .start(locList);
        }
    }

    public void pause() {
        if (listening) {
            listening = false;
            try {
                SmartLocation.with(context).location().stop();
                stopForeground(true);
            } catch (Exception e) {
                // If there is an error with pausing, then the location is already stopped,
                // ignore
            }
        }
    }

    public void checkLocationSettingsAndStart() {
        final SharedPreferences contextPreferences =
                PreferenceManager.getDefaultSharedPreferences(
                        view.getActivity().getApplicationContext()
                );
        boolean inaccurateLocation =
                contextPreferences.getBoolean("inaccurateLocation", false);
        if (!inaccurateLocation) {
            createLocationRequest();
            buildLocationSettingsRequest();
        } else {
            start();
        }
    }

    @SuppressLint("RestrictedApi")
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(0);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(0);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public String getGeoProvider() {
        if (locList.location != null) {
            return locList.location.getProvider();
        }
        return null;
    }

    public void setView(DeviceScannerFragment view) {
        this.view = view;
    }

    /**
     * Builds the dialog to ask for better location settings to be enabled.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this.view.getActivity());
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        final DeviceScannerFragment view = this.view;
        final GPSTracker thisTrack = this;

        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                if (view.getScanManager().isScanning()) {
                    thisTrack.start();
                }
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(view.getActivity(),
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }
}
