package edu.sysnet.skimmer.bluetoothscanner.system;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Records an error stack trace to a file. Temporary until a server is set up for the project
 */
public class ErrorLogHandler extends AsyncTask<String, Void, Void> {

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected Void doInBackground(String... strings) {
        final String path = Environment.DIRECTORY_DOCUMENTS + "/Bluetana/";
        File errorFile = Environment.getExternalStoragePublicDirectory(path);
        //noinspection ResultOfMethodCallIgnored
        errorFile.mkdirs();
        try {
            //noinspection ResultOfMethodCallIgnored
            new File(errorFile, "error.log").createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            writeStackTrace(errorFile, strings[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint({"SetWorldWritable", "SetWorldReadable"})
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void writeStackTrace(File path, String stackTrace) throws IOException {
        File outputFile = new File(path, "error.log");
        outputFile.setReadable(true, false);
        outputFile.setWritable(true, false);
        FileOutputStream output = new FileOutputStream(outputFile);

        try (PrintStream out = new PrintStream(output)) {
            out.print(stackTrace);
        }

        output.close();
    }
}

