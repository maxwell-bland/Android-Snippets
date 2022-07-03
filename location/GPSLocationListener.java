package edu.sysnet.skimmer.bluetoothscanner.location;

import android.location.Location;

import edu.sysnet.skimmer.bluetoothscanner.layout.DeviceScannerFragment;
import io.nlopez.smartlocation.OnLocationUpdatedListener;

/**
 * Created by always on 4/10/18.
 */

public class GPSLocationListener implements OnLocationUpdatedListener {
    Location location; // location
    public double latitude; // latitude
    public double longitude; // longitude
    public double altitude; // altitude
    public float accuracy; // altitude
    public DeviceScannerFragment view;

    public GPSLocationListener(DeviceScannerFragment view) {
        this.view = view;
    }

    @Override
    public void onLocationUpdated(Location location) {
        this.location = location;
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        altitude = location.getAltitude();
        accuracy = location.getAccuracy();

        if (view != null) {
            view.updateGeoLoc(latitude,longitude);
        }
    }
}
