package edu.sysnet.skimmer.bluetoothscanner.data;

import android.annotation.SuppressLint;
import android.content.ContextWrapper;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.sysnet.skimmer.bluetoothscanner.layout.DeviceScannerFragment;
import edu.sysnet.skimmer.bluetoothscanner.network.GoogleApiUtil;
import edu.sysnet.skimmer.bluetoothscanner.network.NetworkStateReceiver;
import edu.sysnet.skimmer.bluetoothscanner.bluetooth.BtAdapterUtil;
import edu.sysnet.skimmer.bluetoothscanner.system.SystemUtil;

import static java.lang.Thread.sleep;

/**
 * This is a static class that handles recording new datapoints to CSV and uploading them
 * to Google Drive
 */
@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class DataRecorder implements NetworkStateReceiver.NetworkStateReceiverListener {
    private static final String WORKING_CSV_DIRECTORY = "working";
    private static final String NOT_UPLOADED_ZIP_DIRECTORY = "notUploaded";
    private static DataRecorder instance;
    private Lock cacheLock;
    private File workingPath;
    private File notUploadedPath;
    private String csvName = null;
    private DeviceScannerFragment view;
    private boolean isUploading;


    @SuppressWarnings("ResultOfMethodCallIgnored")
    public DataRecorder(DeviceScannerFragment view) {
        this.view = view;
        updateArchiveDirectories();
        cacheLock = new ReentrantLock();
    }

    @Override
    public void networkAvailable() {
        clearFileCache();
    }

    @Override
    public void networkUnavailable() {
    }

    /**
     * Gets a new csv name from the device info and from the current time
     *
     * @return a string representing the new csvname
     */
    @SuppressLint({"HardwareIds", "SimpleDateFormat"})
    private String getNewCsvName() {
        return BtAdapterUtil.getInstance().getBTAdapter().getAddress() +
                "_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "_" +
                System.currentTimeMillis() + ".csv";
    }

    /**
     * Records a given device into a csv file, also handles delegating generation of a new csv,
     * posting data
     *
     * @param dataPoint the datapoint to record into the CSV
     */
    public void recordDevice(DeviceDatapoint dataPoint) {
        if (csvName == null) {
            csvName = getNewCsvName();
        }

        final File workingCsv = new File(workingPath, csvName);
        writeDatapoint(dataPoint, workingCsv);

        long fileSize = SystemUtil.getFolderSize(workingCsv) >> 10;
        if (fileSize > 30) {
            tearFile();
        }
    }

    /**
     * Writes a datapoint to a file and initializes the file if needed
     *
     * @param dataPoint  the datapoint to write to a file
     * @param workingCsv the file to write to
     */
    private void writeDatapoint(DeviceDatapoint dataPoint, File workingCsv) {
        try {
            cacheLock.lock();
            boolean newFile = workingCsv.createNewFile();
            cacheLock.unlock();
            FileOutputStream fOut = new FileOutputStream(workingCsv, true);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            if (newFile) {
                myOutWriter.append(dataPoint.getHeaders());
            }
            myOutWriter.append(dataPoint.toCsv());
            myOutWriter.close();
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    /**
     * Handles getting the latest versions of the archive directories for the application, creates
     * them if they do not exist
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void updateArchiveDirectories() {
        // Get the directory for the user's documents, and the skimmer log
        workingPath = new File(view.getContext().getApplicationContext().getFilesDir(),
                WORKING_CSV_DIRECTORY);
        notUploadedPath = new File(view.getContext().getApplicationContext().getFilesDir(),
                NOT_UPLOADED_ZIP_DIRECTORY);

        // Make sure the workingPath directory exists.
        if (!workingPath.exists()) {
            // Make it, if it doesn't exit
            workingPath.mkdirs();
        }
        if (!notUploadedPath.exists()) {
            notUploadedPath.mkdirs();
        }
    }

    /**
     * Clears the cache of files in the device which have not been uploaded. This includes both
     * zipped and non-zipped files.
     */
    public void clearFileCache() {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        cacheLock.lock();
                        File[] workingFiles = workingPath.listFiles();
                        cacheLock.unlock();
                        for (File pending : workingFiles) {
                            if (!pending.getName().equals(csvName)) {
                                SystemUtil.zip(
                                        workingPath,
                                        new String[]{pending.getName()},
                                        notUploadedPath,
                                        pending.getName() + ".gz"
                                );
                                //noinspection ResultOfMethodCallIgnored
                                pending.delete();
                            }
                        }

                        isUploading = true;
                        cacheLock.lock();
                        File[] notUploaded = notUploadedPath.listFiles();
                        cacheLock.unlock();
                        for (final File pending : notUploaded) {
                            // Try upload three times, if it fails all three times,
                            // Reprompt user
                            for (int i = 0; i < 3; i++) {
                                if (doUpload(pending)) {
                                    break;
                                } else {
                                    try {
                                        sleep(1);
                                    } catch (InterruptedException ignored) {
                                    }
                                }
                            }
                        }
                        isUploading = false;

                    }
                }
        );
    }

    /**
     * Uploads a file, notifying the view when a upload is done.
     *
     * @param pending the file to upload
     */
    private boolean doUpload(File pending) {
        boolean success = true; // optimism
        try {
            view.setUploadProgress(1);
            GoogleApiUtil.driveUpload(view, pending);
            //noinspection ResultOfMethodCallIgnored
            pending.delete();
            view.setUploadProgress(100);
            view.setLastUploaded(new Date(System.currentTimeMillis()));
            // Sleep so that the user sees the update on the screen
            sleep(1000);
        } catch (Exception e) {
            success = false;
        }
        return success;
    }

    /**
     * Returns whether or not the datarecorder is uploading or not
     * @return whether an upload is in progress
     */
    public boolean isUploading() {
        return isUploading;
    }

    /**
     * Gets a new csv name and clears the cache
     */
    public void tearFile() {
        csvName = getNewCsvName();
        clearFileCache();
    }
}
