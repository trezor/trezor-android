package com.circlegate.liban.ws;

import android.os.SystemClock;
import android.text.TextUtils;

import com.circlegate.liban.task.TaskInterfaces.ITask;
import com.circlegate.liban.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WsUtils {
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    public static final String JSON_ROOT_KEY = "d";
    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";

    private static final int BUFFER_SIZE = 2048;

    public static OkHttpClient.Builder createClientBuilder(int connectTimeout, int writeTimeout, int readTimeout) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
        builder.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS);
        builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        return builder;
    }

    @Deprecated
    public static OkHttpClient.Builder createAllTrustClientBuilder(int connectTimeout, int writeTimeout, int readTimeout) {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = createClientBuilder(connectTimeout, writeTimeout, readTimeout);
            builder.sslSocketFactory(sslSocketFactory);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            return builder;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Request.Builder createRequestAcceptingJsonResponse(String url) {
        Request.Builder request = new Request.Builder()
                .url(url);
        request.header("Accept", "application/json");
        return request;
    }

    public static Request.Builder createRequestAcceptingJsonResponse(String url, JSONObject requestBody) {
        Request.Builder request = createRequestAcceptingJsonResponse(url);
        request.post(RequestBody.create(MEDIA_TYPE_JSON, requestBody.toString()));
        return request;
    }

    public static boolean setRequestCustomGzipResponseHandlingIfCan(Request.Builder builder, boolean useGzip) {
        Request request = builder.build();
        if (TextUtils.isEmpty(request.header(HEADER_ACCEPT_ENCODING))) {
            if (useGzip)
                builder.header(HEADER_ACCEPT_ENCODING, "gzip");
            else
                builder.header(HEADER_ACCEPT_ENCODING, "identity");

            return true;
        }
        else
            return false;
    }

    public static String readResponseString(Response acceptableResponse) throws IOException {
        return acceptableResponse.body().string();
    }

    public static JSONObject readResponseJson(Response acceptableResponse) throws IOException, JSONException {
        String response = readResponseString(acceptableResponse);

        response = response.trim();
        final JSONObject json;

        // Pokud by ve vysledku primo bylo JSONArray, tak jej obalim do json objektu
        if (response.startsWith("[")) {
            JSONArray array = new JSONArray(response);
            json = new JSONObject();
            json.put(JSON_ROOT_KEY, array);
        }
        else
            json = new JSONObject(response);

        return json;
    }

    /**
     * Vraci false v pripade, ze byl task zrusen!
     */
    public static boolean downloadResponseToStream(Response acceptableResponse, OutputStream outputStream, ITask task, boolean customGzipHandling, boolean canReportProgress, boolean canCancelWhileDownloading, String logTag) throws IOException {
        GZIPOutputStream optOutputStreamGZip = null;

        try {
            if (canCancelWhileDownloading && task.isCanceled()) {
                LogUtils.d(logTag, "downloadResponseToStream: task canceled (1)");
                return false;
            }

            InputStream input = acceptableResponse.body().byteStream();
            OutputStream outputStreamTop = outputStream;

            String contentEncoding;
            if (customGzipHandling
                    && !TextUtils.isEmpty(contentEncoding = acceptableResponse.header(HEADER_CONTENT_ENCODING))
                    && contentEncoding.toLowerCase().equals("gzip"))
            {
                optOutputStreamGZip = new GZIPOutputStream(outputStream);
                outputStreamTop = optOutputStreamGZip;
            }

            long optContentLength = acceptableResponse.body().contentLength();
            int currLength;
            long downloadedLength = 0;
            long lastReportTime = reportProgressIfCan(task, canReportProgress, downloadedLength, optContentLength, 0);
            byte[] buffer = new byte[BUFFER_SIZE];

            if (optContentLength > 0 && canReportProgress) {
                task.putProcessObj(ITask.PROCESS_BUNDLE_FILE_SIZE, optContentLength);
            }

            while ((currLength = input.read(buffer)) != -1) {
                downloadedLength += currLength;
                outputStreamTop.write(buffer, 0, currLength);
                lastReportTime = reportProgressIfCan(task, canReportProgress, downloadedLength, optContentLength, lastReportTime);

                if (canCancelWhileDownloading && task.isCanceled()) {
                    LogUtils.d(logTag, "downloadResponseToStream: task canceled (2)");
                    return false;
                }
            }
            lastReportTime = reportProgressIfCan(task, canReportProgress, downloadedLength, optContentLength, lastReportTime);
            return true;
        }
        finally {
            try {
                if (optOutputStreamGZip != null)
                    optOutputStreamGZip.close();
            }
            catch (Exception ex) { }
        }
    }


    private static long reportProgressIfCan(ITask task, boolean canReportProgress, long downloadedSize, long optTotalSize, long lastReportTime) {
        long currTime = SystemClock.elapsedRealtime();
        if (canReportProgress && optTotalSize > 0 && downloadedSize <= optTotalSize &&
                (downloadedSize == optTotalSize || Math.abs(currTime - lastReportTime) >= 500))
        {
            task.onTaskProgress((int)((downloadedSize * 10000) / optTotalSize), "WsParam.downloading");
            return currTime;
        }
        else
            return lastReportTime;
    }
}
