package edu.sysnet.skimmer.bluetoothscanner.layout.devicelist;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

import edu.sysnet.skimmer.bluetoothscanner.R;
import edu.sysnet.skimmer.bluetoothscanner.ScanActivity;
import edu.sysnet.skimmer.bluetoothscanner.data.HitlistUtil;
import edu.sysnet.skimmer.bluetoothscanner.layout.elements.CustomTypefaceSpan;
import edu.sysnet.skimmer.bluetoothscanner.wifi.WifiDeviceDatapoint;

/**
 * Holds an object in a device list layout, and defines the methods by which parts of the
 * layout are generated
 */
class WifiDeviceDataHolder extends DeviceDataHolder {
    private static final int PASTEL_BLUE = Color.parseColor("#779ECB");
    private TextView deviceDetailText;

    WifiDeviceDataHolder(HitlistUtil hitlist, View convertView,
                         WifiDeviceDatapoint deviceDatapoint) {
        super(convertView, hitlist, deviceDatapoint);
        convertView.setTag(this);
    }

    @Override
    protected void setDeviceDetailText() {
        WifiDeviceDatapoint dp = (WifiDeviceDatapoint) deviceDatapoint;
        String secCaps = dp.capabilities.split("]")[0];
        if (secCaps.equals("")) {
            secCaps = "N/A";
        } else {
            secCaps = secCaps.substring(1);
        }
        String macString = String.format("%s • %s", dp.deviceAddress, secCaps);
        deviceDetailText = (TextView) convertView.findViewById(R.id.macAddress);
        deviceDetailText.setText(macString);
    }

    public static int convertFrequencyToChannel(int freq) {
        if (freq >= 2412 && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
        } else if (freq >= 5170 && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else {
            return -1;
        }
    }

    @Override
    void setDeviceDetailTextColor(int Color) {
        deviceDetailText.setTextColor(Color);
    }

    @Override
    protected void setSignalStrengthText() {
        sigStrengthText = (TextView) convertView.findViewById(R.id.sigStrength);
        String sigStrength = String.valueOf(((WifiDeviceDatapoint) deviceDatapoint).level);
        sigStrengthText.setText(String.format("%s ", Integer.valueOf(sigStrength).toString()));
    }

    @Override
    protected void setDeviceTypeText() {
        WifiDeviceDatapoint dp = (WifiDeviceDatapoint) deviceDatapoint;
        int channel = convertFrequencyToChannel(dp.frequency);
        if (channel == -1) {
            deviceTypeText.setText("W ");
        } else {
            deviceTypeText.setText(String.format(Locale.ENGLISH, "%d ", channel));
        }
        deviceTypeText.setTextColor(PASTEL_BLUE);
    }

    @Override
    protected void setFont() {
        super.setFont();
        Spannable spannable = new SpannableString(deviceDetailText.getText());

        // Set the custom typeface to span over a section of the spannable object
        spannable.setSpan(
                new CustomTypefaceSpan("sans-serif", Typeface.DEFAULT),
                0,
                deviceDatapoint.deviceAddress.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        spannable.setSpan(
                new CustomTypefaceSpan("sans-serif", ScanActivity.defaultCondensedTf),
                deviceDatapoint.deviceAddress.length(),
                deviceDetailText.getText().length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        deviceDetailText.setText(spannable);
    }
}
