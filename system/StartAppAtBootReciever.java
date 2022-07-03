package edu.sysnet.skimmer.bluetoothscanner.system;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.reflect.Reflection;

import edu.sysnet.skimmer.bluetoothscanner.ScanActivity;

import static com.google.common.reflect.Reflection.getPackageName;

/**
 * Created by always on 2/1/18.
 */

public class StartAppAtBootReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent i = new Intent(context, ScanActivity.class);
            Log.d("Skimmer Boot", "Received boot completed.");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            boolean check = sharedPreferences.getBoolean("runOnStartup", false);
            if (check && !KioskHandler.isKioskDevice(context)) {
                context.startActivity(i);
            }
        }
    }
}
