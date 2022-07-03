package edu.sysnet.skimmer.bluetoothscanner.layout.devicelist;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.common.base.Stopwatch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import edu.sysnet.skimmer.bluetoothscanner.R;
import edu.sysnet.skimmer.bluetoothscanner.ScanActivity;
import edu.sysnet.skimmer.bluetoothscanner.bluetooth.BtAdapterUtil;
import edu.sysnet.skimmer.bluetoothscanner.bluetooth.BtDeviceDatapoint;
import edu.sysnet.skimmer.bluetoothscanner.data.HitlistUtil;
import edu.sysnet.skimmer.bluetoothscanner.layout.DeviceScannerFragment;
import edu.sysnet.skimmer.bluetoothscanner.layout.elements.CustomTypefaceSpan;

/**
 * Holds an object in a device list layout, and defines the methods by which parts of the
 * layout are generated
 */
class BtDeviceDataHolder extends DeviceDataHolder {
    private static final int C_COLOR = Color.parseColor("#AFCEB4");
    private static final int L_COLOR = Color.parseColor("#95B76D");
    private static final int D_COLOR = Color.parseColor("#296F74");
    private TextView deviceDetailText;

    BtDeviceDataHolder(HitlistUtil hitlist, View convertView,
                       final BtDeviceDatapoint deviceDatapoint) {
        super(convertView, hitlist, deviceDatapoint);
        convertView.setTag(this);
    }

    @Override
    protected void setDeviceDetailText() {
        BtDeviceDatapoint dp = ((BtDeviceDatapoint) deviceDatapoint);
        int devClass = dp.btClass.getDeviceClass();
        String deviceClass = String.format("%04X", devClass);
        deviceClass = new StringBuilder(deviceClass).insert(2, ":").toString();
        String deviceExtra = dp.getDeviceClassString();
        String macString;
        if (deviceExtra.equals(BtDeviceDatapoint.UNCLASSIFIED_DEV_STRING)) {
            macString = String.format("%s • %s",
                    dp.deviceAddress, deviceClass.toUpperCase());
        } else {
            macString = String.format("%s • %s • %s",
                    dp.deviceAddress, deviceClass.toUpperCase(), deviceExtra);
        }

        deviceDetailText = (TextView) convertView.findViewById(R.id.macAddress);
        deviceDetailText.setText(macString);
    }

    @Override
    public void setHitlistFlag() {
        if (hitlist.checkHitlist(deviceDatapoint.deviceAddress)
                ) {
            if (deviceDatapoint.deviceName.matches(NON_LETHAL_REGEX) ||
                    (deviceDatapoint.deviceType == BluetoothDevice.DEVICE_TYPE_LE) ||
                    ((BtDeviceDatapoint) deviceDatapoint).btClass.getDeviceClass() != 0x1F00) {
                titleText.setTextColor(ORANGE_ALERT);
                setDeviceDetailTextColor(ORANGE_ALERT);
            } else {
                titleText.setTextColor(Color.RED);
                setDeviceDetailTextColor(Color.RED);
            }
            titleText.setText(titleText.getText());
        } else {
            titleText.setTextColor(Color.WHITE);
            setDeviceDetailTextColor(Color.WHITE);
        }
    }


    @Override
    void setDeviceDetailTextColor(int Color) {
        deviceDetailText.setTextColor(Color);
    }

    @Override
    protected void setSignalStrengthText() {
        sigStrengthText = (TextView) convertView.findViewById(R.id.sigStrength);
        String sigStrength = String.valueOf(((BtDeviceDatapoint) deviceDatapoint).rssi);
        sigStrengthText.setText(String.format("%s ", Integer.valueOf(sigStrength).toString()));
    }

    @Override
    protected void setDeviceTypeText() {
        switch (deviceDatapoint.deviceType) {
            case (BluetoothDevice.DEVICE_TYPE_CLASSIC):
                deviceTypeText.setText("C ");
                deviceTypeText.setTextColor(C_COLOR);
                break;
            case (BluetoothDevice.DEVICE_TYPE_LE):
                deviceTypeText.setText("L ");
                deviceTypeText.setTextColor(L_COLOR);
                break;
            case (BluetoothDevice.DEVICE_TYPE_DUAL):
                deviceTypeText.setTextColor(D_COLOR);
                deviceTypeText.setText("D ");
                break;
        }
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
