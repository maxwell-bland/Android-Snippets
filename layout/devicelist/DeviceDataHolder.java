package edu.sysnet.skimmer.bluetoothscanner.layout.devicelist;

import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import edu.sysnet.skimmer.bluetoothscanner.R;
import edu.sysnet.skimmer.bluetoothscanner.data.DeviceDatapoint;

public abstract class DeviceDataHolder {

    /* NOTE: Removed for privacy */

    static final int ORANGE_ALERT = Color.parseColor("#ffa600");
    final View convertView;
    final DeviceDatapoint deviceDatapoint;
    TextView titleText;
    TextView deviceTypeText;
    TextView sigStrengthText;

    DeviceDataHolder(View convertView, DeviceDatapoint deviceDatapoint) {
        this.convertView = convertView;
        deviceTypeText = (TextView) convertView.findViewById(R.id.devTypeText);
        titleText = (TextView) convertView.findViewById(R.id.titleTextView);
        sigStrengthText = (TextView) convertView.findViewById(R.id.sigStrength);
        this.deviceDatapoint = deviceDatapoint;
        setTitleText();
        setSignalStrengthText();
        setDeviceTypeText();
        setDeviceDetailText();
        setHitlistFlag();
        setFont();
    }

    public void setHitlistFlag() {
                /* NOTE: removed for privacy */
            titleText.setTextColor(Color.WHITE);
            setDeviceDetailTextColor(Color.WHITE);
    }

    protected abstract void setDeviceDetailText();

    abstract void setDeviceDetailTextColor(int Color);

    protected abstract void setSignalStrengthText();

    private void setTitleText() {
        // Set View Text
        titleText.setText("N/A");
        if (deviceDatapoint.deviceName != null) {
            if (!deviceDatapoint.deviceName.equals("")) {
                titleText.setText(deviceDatapoint.deviceName);
            }
        }
    }

    protected abstract void setDeviceTypeText();

    protected void setFont() {
        titleText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        deviceTypeText.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        sigStrengthText.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
    }
}
