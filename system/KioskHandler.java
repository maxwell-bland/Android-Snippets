package edu.sysnet.skimmer.bluetoothscanner.system;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.provider.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handler for kiosk logic for certain installs
 */
public class KioskHandler {
    /**
     * Checks a list of flagged android ids and returns true if this is a kiosk mode device
     *
     * @return whether the device should be in kiosk mode or not
     */
    public static boolean isKioskDevice(Context context) {
        File downloads = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
        );
        File kioskFlag = new File(downloads.getAbsolutePath() + "/kiosk.conf");
        return kioskFlag.exists();
    }
}
