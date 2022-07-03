package edu.sysnet.skimmer.bluetoothscanner.layout.devicelist;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import edu.sysnet.skimmer.bluetoothscanner.R;
import edu.sysnet.skimmer.bluetoothscanner.data.DeviceDatapoint;
import edu.sysnet.skimmer.bluetoothscanner.data.HitlistUtil;
import edu.sysnet.skimmer.bluetoothscanner.wifi.WifiDeviceDatapoint;

/**
 * UI component for the bluetooth device list. Handles taking a list of bluetooth devices and
 * mapping them to layouts
 */
public class WifiDeviceListAdapter extends DeviceListAdapter {

    public WifiDeviceListAdapter(Context context, HitlistUtil hitlist) {
        super(context, android.R.layout.simple_list_item_1,
                new ArrayList<DeviceDatapoint>(), hitlist);
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater != null ?
                    mInflater.inflate(R.layout.device_list_item, null) : null;
        }
        assert convertView != null;
        final WifiDeviceDatapoint deviceDatapoint = (WifiDeviceDatapoint) getItem(position);
        assert deviceDatapoint != null;
        new WifiDeviceDataHolder(hitlist, convertView, deviceDatapoint);
        return convertView;
    }

    @Override
    public void updateDatapoint(DeviceDatapoint point) {
        if (point instanceof WifiDeviceDatapoint) {
            super.updateDatapoint(point);
        }
    }
}
