package edu.sysnet.skimmer.bluetoothscanner.network;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.CipherInputStream;

import edu.sysnet.skimmer.bluetoothscanner.layout.DeviceScannerFragment;

import static edu.sysnet.skimmer.bluetoothscanner.system.SystemUtil.pubKeyEncryptFile;

public class MultipartUploadUtility {
    private final String boundary;
    private static final String LINE_FEED = "\r\n";
    private HttpURLConnection httpConn;
    private String charset;
    private OutputStream outputStream;
    private PrintWriter writer;
    private DeviceScannerFragment view;

    /**
     * This constructor initializes a new HTTP POST request with content type
     * is set to multipart/form-data
     *
     * @param requestURL
     * @param charset
     * @throws IOException
     */
    public MultipartUploadUtility(String requestURL, String charset, String authToken,
                                  DeviceScannerFragment view)
            throws IOException {
        this.charset = charset;
        this.view = view;

        // creates a unique boundary based on time stamp
        boundary = "===" + System.currentTimeMillis() + "===";
        URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true);    // indicates POST method
        httpConn.setDoInput(true);
        httpConn.setRequestProperty("Authorization",
                "Bearer " + authToken);
        httpConn.setRequestProperty("Content-Type",
                "multipart/related; boundary=" + boundary);
        outputStream = httpConn.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
                true);
    }

    /**
     * Adds a form field to the request
     *
     * @param name  field name
     * @param value field value
     */
    public void addMetaDataPart(String name, String value) {
        writer.append("--").append(boundary).append(LINE_FEED);
        writer.append("Content-Type: application/json; charset=").append(charset).append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.append("{").append(LINE_FEED);
        writer.append("\"")
                .append(name)
                .append("\": ")
                .append("\"")
                .append(value)
                .append("\"")
                .append(LINE_FEED);
        writer.append("}").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();
    }

    /**
     * Adds a upload file section to the request
     *
     * @param uploadFile a File to be uploaded
     * @throws IOException
     */
    public void addFilePart(File uploadFile)
            throws IOException {
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Type: application/octet-stream").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();
        FileInputStream fInputStream = new FileInputStream(uploadFile);
        ByteArrayInputStream inputStream = pubKeyEncryptFile(fInputStream);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();
        writer.append(LINE_FEED);
        writer.flush();
    }

    /**
     * Completes the request and receives response from the server.
     *
     * @return a list of Strings as response in case the server returned
     * status OK, otherwise an exception is thrown.
     * @throws IOException
     */
    public List<String> finish() throws IOException {
        List<String> response = new ArrayList<>();
        writer.append(LINE_FEED).flush();
        writer.append("--").append(boundary).append("--").append(LINE_FEED);
        writer.close();

        // checks server's status code first
        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpConn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            httpConn.disconnect();
        } else {
            throw new IOException("Server returned non-OK status: " + status);
        }
        return response;
    }
}
