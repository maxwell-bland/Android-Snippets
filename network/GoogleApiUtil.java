package edu.sysnet.skimmer.bluetoothscanner.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.sysnet.skimmer.bluetoothscanner.layout.DeviceScannerFragment;

/**
 * Utility for dealing with google API services, such as grabbing refresh tokens, etc.
 */
public class GoogleApiUtil {
    public static final String GOOGLE_API_UPLOAD_URL =
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart";

    /**
     * Gets the API token to access the files on the google drive through oauth2
     * @return the string of the token
     * @throws IOException if there is a network error
     */
    @Nullable
    public static String getDriveAPIToken() throws IOException {
        String token;
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost refreshPost = new HttpPost("https://accounts.google.com/o/oauth2/token");
        // Add your data
        List<NameValuePair> nameValuePairs = createAuthBodyParams();

        refreshPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        HttpResponse response = httpclient.execute(refreshPost);
        if (response.getStatusLine().getStatusCode() == 200) {
            String responseBody = EntityUtils.toString(response.getEntity());
            token = responseBody.split(":")[1].split("\"")[1];
            Log.d("Recorder", "response " + responseBody);
        } else {
            token = null;
        }
        return token;
    }

    /**
     * This grabs the body parameters of an http post request for renewing upload permission to
     * google drive.
     *
     * @return a list of name-value pairs corresponding to the required parameters of the renewal
     * request
     */
    @NonNull
    private static List<NameValuePair> createAuthBodyParams() {
        List<NameValuePair> nameValuePairs = new ArrayList<>(5);
        nameValuePairs.add(new BasicNameValuePair("grant_type", "refresh_token"));
        /* NOTE: redacted */
        nameValuePairs.add(new BasicNameValuePair("client_id",
                ""));
        nameValuePairs.add(new BasicNameValuePair("client_secret",
                ""));
        nameValuePairs.add(new BasicNameValuePair("refresh_token",
                ""));
        return nameValuePairs;
    }

    /**
     * Uploads a zip file to the google drive, moves that zip file to the uploaded directory
     *
     * @param view the view to update the state of on upload
     * @param file the zip to upload
     * @throws IOException upon bad file
     */
    public static void driveUpload(DeviceScannerFragment view, File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        String token = getDriveAPIToken();
        if (token == null) {
            return;
        }

        MultipartUploadUtility multipart = new MultipartUploadUtility(
                GOOGLE_API_UPLOAD_URL,
                "UTF-8", token, view
        );
        multipart.addMetaDataPart("name", file.getName() + ".enc");
        multipart.addFilePart(file);
        multipart.finish();
    }
}
