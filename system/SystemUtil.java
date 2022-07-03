package edu.sysnet.skimmer.bluetoothscanner.system;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.common.util.ArrayUtils;

import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.openssl.PEMKeyPair;
import org.spongycastle.openssl.PEMParser;
import org.spongycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.channels.FileChannel;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EventListener;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import edu.sysnet.skimmer.bluetoothscanner.data.DataRecorder;

/**
 * Simple class for various system utility functions
 */
public class SystemUtil {
    /* NOTE: removed for privacy */
    private static String PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n" +
            "-----END PUBLIC KEY-----";
    static SecureRandom rnd = new SecureRandom();

    /**
     * Gets yesterdats date
     */
    public static Date getYesterdaysDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

    /**
     * Performs a file download and moves the file to the desired directory
     *
     * @param url             the url to grab the downloaded file from
     * @param description     the description to show while downloading
     * @param title           the title to show while performing the download
     * @param destinationPath the destination path of the download
     * @param downloadName    the name of the downloaded file
     * @param context         the context in which to execute the download
     */
    public static void doDownload(String url, String description,
                                  String title, final String destinationPath,
                                  String downloadName, Context context) {
        // Set up the download request
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(description);
        request.setTitle(title);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, downloadName);

        // Set up the download manager and the mechanism for receiving the completed download
        final DownloadManager manager =
                (DownloadManager) context.getApplicationContext()
                        .getSystemService(Context.DOWNLOAD_SERVICE);
        assert manager != null;
        final long downloadedFileId = manager.enqueue(request);
        final Semaphore downloadSem = new Semaphore(0);

        BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Make sure this is the right file
                final long downloadId = intent.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID, 0
                );
                if (downloadId == 0 || downloadedFileId != downloadId) return;

                final Cursor cursor = manager.query(
                        new DownloadManager.Query().setFilterById(downloadId));

                if (cursor.moveToFirst()) {
                    final String downloadedTo = cursor.getString(
                            cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        // Move the result to the desired directory
                        //noinspection ResultOfMethodCallIgnored
                        try {
                            moveFile(
                                    new File(Uri.parse(downloadedTo).getPath()),
                                    new File(destinationPath)
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        downloadSem.release();
                        context.unregisterReceiver(this);
                    }
                }

                cursor.close();

            }
        };
        context.getApplicationContext().registerReceiver(downloadReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        // Wait for the download to complete
        try {
            downloadSem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Moves a file between any two places on the phone, external or internal.
     * Deletes the old file. Needed since android doesn't support this itself
     *
     * @param src the source to move from
     * @param dst the destination to move to
     * @throws IOException in the case of an error
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void moveFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            outChannel.close();
            src.delete();
        }
    }

    /**
     * Zips a given file to an output directory
     *
     * @param inputPath the input directory path
     * @param files     the files to zip in that directory
     * @param zipPath   the output directory path
     * @param zipName   the output zipfile name
     * @return the zipped file
     */
    public static File zip(File inputPath, String[] files, File zipPath, String zipName) {
        try {
            final File _zipFile = new File(zipPath, zipName);
            //noinspection ResultOfMethodCallIgnored
            _zipFile.createNewFile();
            FileOutputStream dest = new FileOutputStream(_zipFile);

            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

            byte data[] = new byte[1024];

            for (String file : files) {
                Log.d("add:", file);
                Log.v("Compress", "Adding: " + file);
                final File inputFile = new File(inputPath, file);
                FileInputStream fi = new FileInputStream(inputFile);
                BufferedInputStream origin = new BufferedInputStream(fi, 1024);
                ZipEntry entry = new ZipEntry(file);
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, 1024)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }

            out.close();
            return _zipFile;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the size of a current folder or file in total number of bytes
     *
     * @param f the file that contains the bytes we need
     * @return the long value of the size of the file in bytes
     */
    public static long getFolderSize(File f) {
        long size = 0;
        if (f.isDirectory()) {
            File[] dirFiles = f.listFiles();
            for (File file : dirFiles) {
                size += getFolderSize(file);
            }
        } else {
            size = f.length();
        }
        return size;
    }

    /**
     * Calculates SHA256 of input text.
     *
     * @param text the text we care about
     * @return The encoded string
     * @throws NoSuchAlgorithmException only for API < 1, so shouldn't be an issue
     */
    public static String SHA256(String text) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        md.update(text.getBytes());
        byte[] digest = md.digest();

        return Base64.encodeToString(digest, Base64.DEFAULT);
    }

    /**
     * Gets a file's content as a list of bytes
     *
     * @param fis      input stream to read from
     * @return list of bytes of the file
     * @throws IOException if something goes terribly wrong.
     */
    private static byte[] getFileContent(FileInputStream fis) throws IOException {
        final List<Byte> list = new ArrayList<>();
        while (fis.available() != 0) {
            list.add((byte) fis.read());
        }

        byte[] fileContent = new byte[list.size()];
        int i = 0;
        for (Byte b : list) {
            fileContent[i++] = b;
        }
        return fileContent;
    }

    /**
     * Takes a file input stream and encrypts it using the app's pub key.
     *
     * @param encrypt the stream to encrypt
     * @return the input stream (fails to non-encrypted )^:
     */
    public static ByteArrayInputStream pubKeyEncryptFile(FileInputStream encrypt) {
        try {
            IvParameterSpec iv = new IvParameterSpec(rnd.generateSeed(16));

            // Generate symmetric AES Key
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256); // The AES key size in number of bits
            SecretKey secKey = generator.generateKey();

            // Encypt file contents using AES
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCipher.init(Cipher.ENCRYPT_MODE, secKey, iv);
            byte[] byteCipherText = aesCipher.doFinal(getFileContent(encrypt));

            // Parse RSA Public Key
            PEMParser pemParser = new PEMParser(new StringReader(PUBLIC_KEY));
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            Object object = pemParser.readObject();
            PublicKey pubKey = converter.getPublicKey((SubjectPublicKeyInfo) object);

            // Encrypt
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] encryptedKey = cipher.doFinal(secKey.getEncoded());
            byte[] encryptedIV = cipher.doFinal(iv.getIV());

            // Convert to input stream for sending over the internet
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(byteCipherText);
            outputStream.write("(BEGIN ENCRYPTED KEY)".getBytes());
            outputStream.write(encryptedKey);
            outputStream.write("(BEGIN IV)".getBytes());
            outputStream.write(encryptedIV);
            outputStream.write("(END)".getBytes());
            return new ByteArrayInputStream(outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
