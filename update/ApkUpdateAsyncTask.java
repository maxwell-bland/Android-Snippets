package edu.sysnet.skimmer.bluetoothscanner.update;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.FileProvider;

import com.google.api.client.util.DateTime;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Semaphore;

import edu.sysnet.skimmer.bluetoothscanner.BuildConfig;
import edu.sysnet.skimmer.bluetoothscanner.ScanActivity;
import edu.sysnet.skimmer.bluetoothscanner.network.NetworkStateReceiver;
import edu.sysnet.skimmer.bluetoothscanner.system.SystemUtil;

import static java.lang.Thread.sleep;

/**
 * Downloads the newest version of the APK and installs it on the user's device
 */
public class ApkUpdateAsyncTask extends AsyncTask<Object, Void, Void> {
    private static final String API_KEY = "";
    // Alpha file id
    // private static final String FILE_ID = "";
    // Beta file id
    // NOTE: Removed for privacy
    private static final String FILE_ID = "";
    private static final String DRIVE_BASE_URL =
            "https://www.googleapis.com/drive/v3/files/" + FILE_ID;
    private static final String DRIVE_MODIFIED_URL =
            DRIVE_BASE_URL + "?fields=modifiedTime&key=" + API_KEY;
    private static final String DRIVE_APK_URL = DRIVE_BASE_URL +
            "?alt=media&mimeType=text/csv&key=" + API_KEY;
    private ProgressDialog dialog;
    private final Handler handler = new Handler();
    public boolean continueWithUpdate = false;
    private long downloadedApkId;
    private boolean active;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("SetWorldReadable")
    @Override
    protected Void doInBackground(Object... objects) {
        ScanActivity activity = (ScanActivity) objects[0];
        ContextWrapper c = new ContextWrapper(activity);
        final File apk = new File(c.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "bluetana.apk");

        if (apk.exists()) {
            apk.delete();
        }
        try {
            apk.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String command = "chmod " + "666" + " " + apk.getPath();
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }


        /**
         * Wait for a minute for the Network State receiver to connect.
         */
        if (!waitForNetwork(activity)) return null;

        boolean updateFound = updateNeeded(activity);
        if (updateFound) {

            if ((boolean) objects[1]) {
                grabApkFromDrive(activity, apk);
                createUpdateApkRequest(activity.getBaseContext(), apk);
                return null;
            }

            final ApkUpdateAsyncTask thisTask = this;

            Semaphore alertLock = new Semaphore(0);

            AlertRunnable alertRunnable = new AlertRunnable(activity, thisTask, alertLock);
            activity.runOnUiThread(alertRunnable);

            try {
                alertLock.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }

            if (continueWithUpdate) {
                grabApkFromDrive(activity, apk);
                createUpdateApkRequest(activity.getApplicationContext(), apk);
            }
        }
        return null;
    }

    /**
     * Waits 60 seconds for the network
     *
     * @param activity the activity for checking network connectivity
     * @return whether it was finally successful or not
     */
    private boolean waitForNetwork(ScanActivity activity) {
        for (int i = 0; i < 6; i++) {
            if (!NetworkStateReceiver.isConnected(activity)) {
                try {
                    sleep(10000);
                } catch (InterruptedException ignored) {
                }
            } else {
                break;
            }
        }
        return NetworkStateReceiver.isConnected(activity);
    }

    /**
     * Check whether an update is needed, return true if it is, return false if it is not or there
     * is an exception
     * <p>
     * TODO: Clean up exception handling in this method.
     *
     * @param context the activity, used to get the last updated time
     * @return true if an update is needed
     */
    private boolean updateNeeded(Context context) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(DRIVE_MODIFIED_URL);

        HttpResponse response;
        try {
            response = httpclient.execute(httpget);
        } catch (IOException e1) {
            e1.printStackTrace();
            return false;
        }

        if (response.getStatusLine().getStatusCode() == 200) {
            JSONObject modifiedJson;
            try {
                modifiedJson = new JSONObject(EntityUtils.toString(response.getEntity()));
            } catch (JSONException e1) {
                e1.printStackTrace();
                return false;
            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }
            String time = null;
            try {
                time = (String) modifiedJson.get("modifiedTime");
            } catch (JSONException e1) {
                e1.printStackTrace();
                return false;
            }
            long lastTouched = new DateTime(time).getValue();
            long lastUpdateTime;
            try {
                lastUpdateTime = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), 0
                ).lastUpdateTime;
            } catch (PackageManager.NameNotFoundException e1) {
                e1.printStackTrace();
                return false;
            }
            return lastUpdateTime < lastTouched;
        }
        return false;
    }

    private void createUpdateApkRequest(Context context, File apk) {
        // Ask to install the newest version of the APK
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent installApplicationIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            apk.setReadable(true, false);
            installApplicationIntent.setDataAndType(FileProvider.getUriForFile(context,
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    apk), "application/vnd.android.package-archive");
            installApplicationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            installApplicationIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(installApplicationIntent);
        } else {
            Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            myIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(
                    Uri.fromFile(apk).toString());
            String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    extension);
            myIntent.setDataAndType(Uri.fromFile(apk), mimetype);
            context.startActivity(myIntent);
        }
    }

    private void grabApkFromDrive(ScanActivity activity, File apk) {
        SystemUtil.doDownload(
                DRIVE_APK_URL, "Downloading update to the Bluetana APK",
                "Bluetana", apk.getAbsolutePath(), apk.getName(), activity
        );
    }
}

