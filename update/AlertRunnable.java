package edu.sysnet.skimmer.bluetoothscanner.update;

import android.app.AlertDialog;
import android.content.DialogInterface;

import java.util.concurrent.Semaphore;

import edu.sysnet.skimmer.bluetoothscanner.ScanActivity;

/**
 * Created by always on 5/14/18.
 */

public class AlertRunnable implements Runnable {
    private final ScanActivity context;
    private final ApkUpdateAsyncTask apkTask;
    private Semaphore alertLock;
    public boolean workDone = false;

    AlertRunnable(ScanActivity context, ApkUpdateAsyncTask apkTask, Semaphore alertLock) {
        this.context = context;
        this.apkTask = apkTask;
        this.alertLock = alertLock;
    }

    @Override
    public void run() {
        new AlertDialog.Builder(context)
                .setTitle("Update Available")
                .setMessage("A new APK is available, would you like to install it? If you respond "
                        + "yes to this dialog, the new APK will download, and you will be prompted"
                        + " to install the newest APK by the Android OS as soon as the download "
                        + "is finished.")
                .setNegativeButton("No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                apkTask.continueWithUpdate = false;
                                alertLock.release();
                            }
                        })
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                apkTask.continueWithUpdate = true;
                                alertLock.release();
                            }
                        })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
