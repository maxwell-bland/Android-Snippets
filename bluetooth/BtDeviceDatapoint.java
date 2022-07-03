package edu.sysnet.skimmer.bluetoothscanner.bluetooth;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Log;

import edu.sysnet.skimmer.bluetoothscanner.location.GPSTracker;
import edu.sysnet.skimmer.bluetoothscanner.data.DeviceDatapoint;
import edu.sysnet.skimmer.bluetoothscanner.scan.ScanManager;

public class BtDeviceDatapoint extends DeviceDatapoint {
    public static final String UNCLASSIFIED_DEV_STRING = "Unclassified";
    // Bluetooth data
    private ParcelUuid[] uuids;
    public BluetoothClass btClass;
    private String scanMode;
    public int rssi;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    BtDeviceDatapoint(BluetoothDevice device, String scanMode, int rssi, int scanNumber,
                      GPSTracker tracker, Context context) {
        super(context, tracker, scanNumber, device.getAddress(), device.getType(), "");
        if (device.getName() != null) {
            deviceName = device.getName().replaceAll("\\p{Cntrl}", "");
        }
        uuids = device.getUuids();
        btClass = device.getBluetoothClass();
        this.scanMode = scanMode;
        this.rssi = rssi;
        Log.d("Device Datapoint", device.toString());
    }

    /**
     * Gets the device class string in a human readable format rather than hex
     *
     * @return the string for the device class
     */
    public String getDeviceClassString() {
        switch (btClass.getDeviceClass()) {
            case BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER:
                return "Camcorder";
            case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO:
                return "Car Audio";
            case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:
                return "A/V Handsfree";
            case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
                return "Headphones";
            case BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO:
                return "Hifi Audio";
            case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
                return "Loudspeaker";
            case BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE:
                return "Microphone";
            case BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO:
                return "Portable A/V";
            case BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX:
                return "Set Top Box";
            case BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED:
                return "A/V Uncat.";
            case BluetoothClass.Device.AUDIO_VIDEO_VCR:
                return "VCR";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA:
                return "Video Camera";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING:
                return "Video Conf.";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER:
                return "Disp & Loudspkr";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY:
                return "Video Game Toy";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR:
                return "Video Monitor";
            case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
                return "Wear. Headset";
            case BluetoothClass.Device.COMPUTER_DESKTOP:
                return "Desktop";
            case BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA:
                return "PDA";
            case BluetoothClass.Device.COMPUTER_LAPTOP:
                return "Laptop";
            case BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA:
                return "Palm Size PDA";
            case BluetoothClass.Device.COMPUTER_SERVER:
                return "Server";
            case BluetoothClass.Device.COMPUTER_UNCATEGORIZED:
                return "Computer";
            case BluetoothClass.Device.COMPUTER_WEARABLE:
                return "Wear. Computer";
            case BluetoothClass.Device.HEALTH_BLOOD_PRESSURE:
                return "HLTH-Bld Press";
            case BluetoothClass.Device.HEALTH_DATA_DISPLAY:
                return "HLTH-Data Disp";
            case BluetoothClass.Device.HEALTH_GLUCOSE:
                return "HLTH-Glucose";
            case BluetoothClass.Device.HEALTH_PULSE_OXIMETER:
                return "HLTH-Pulse Oxi";
            case BluetoothClass.Device.HEALTH_PULSE_RATE:
                return "HLTH-Pulse Rate";
            case BluetoothClass.Device.HEALTH_THERMOMETER:
                return "HLTH-Therm.";
            case BluetoothClass.Device.HEALTH_UNCATEGORIZED:
                return "HLTH-Uncat.";
            case BluetoothClass.Device.HEALTH_WEIGHING:
                return "HLTH-Weighing";
            case BluetoothClass.Device.PHONE_CELLULAR:
                return "Cellphone";
            case BluetoothClass.Device.PHONE_CORDLESS:
                return "Cordless Phone";
            case BluetoothClass.Device.PHONE_ISDN:
                return "ISDN Phone";
            case BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY:
                return "Phone Modem/GW";
            case BluetoothClass.Device.PHONE_SMART:
                return "Smartphone";
            case BluetoothClass.Device.PHONE_UNCATEGORIZED:
                return "Uncat. Phone";
            case BluetoothClass.Device.TOY_CONTROLLER:
                return "Toy Controller";
            case BluetoothClass.Device.TOY_DOLL_ACTION_FIGURE:
                return "Action Figure";
            case BluetoothClass.Device.TOY_GAME:
                return "Toy Game";
            case BluetoothClass.Device.TOY_ROBOT:
                return "Toy Robot";
            case BluetoothClass.Device.TOY_UNCATEGORIZED:
                return "Uncat. Toy";
            case BluetoothClass.Device.TOY_VEHICLE:
                return "Toy Vehicle";
            case BluetoothClass.Device.WEARABLE_GLASSES:
            case BluetoothClass.Device.WEARABLE_HELMET:
            case BluetoothClass.Device.WEARABLE_JACKET:
            case BluetoothClass.Device.WEARABLE_UNCATEGORIZED:
                return "Wearable";
            case BluetoothClass.Device.WEARABLE_PAGER:
                return "Pager";
            case BluetoothClass.Device.WEARABLE_WRIST_WATCH:
                return "Wrist Watch";
            default:
                return UNCLASSIFIED_DEV_STRING;
        }
    }

    public String toCsv() {
        String csvString = "";
        /* NOTE: remvoed for privacy of collected skimmer detection information */
        return csvString;
    }
}
