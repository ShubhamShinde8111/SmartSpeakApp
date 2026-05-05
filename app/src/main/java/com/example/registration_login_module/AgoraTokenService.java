package com.example.registration_login_module;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Token fetcher for Agora using a simple HTTP GET call.
 * Expected server endpoint format:
 *   GET {baseUrl}/rtcToken?channel={channel}&uid={uid}&role=publisher
 *
 * Response: plain token string OR JSON: { "token": "..." }
 */
public class AgoraTokenService {

    private static final String TAG = "AgoraTokenService";

    public interface TokenCallback {
        void onSuccess(String token);
        void onFailure(String errorMessage);
    }

    public void fetchTokenAsync(String baseUrl, String channel, int uid, TokenCallback callback) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            postFailure(callback, "Token server URL is empty");
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // Build the URL
                StringBuilder urlBuilder = new StringBuilder();
                urlBuilder.append(baseUrl);
                if (!baseUrl.endsWith("/")) urlBuilder.append("/");
                urlBuilder.append("rtcToken?");
                urlBuilder.append("channel=").append(URLEncoder.encode(channel, "UTF-8"));
                urlBuilder.append("&uid=").append(uid);
                urlBuilder.append("&role=publisher");

                String urlStr = urlBuilder.toString();
                Log.d(TAG, "Requesting token from: " + urlStr);

                // Open connection
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestMethod("GET");
                connection.setDoInput(true);

                int code = connection.getResponseCode();
                InputStream is = (code >= 200 && code < 300)
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String body = readFully(is);
                Log.d(TAG, "Server response (" + code + "): " + body);

                if (code >= 200 && code < 300) {
                    String token = parseToken(body);
                    if (token != null && !token.isEmpty()) {
                        Log.d(TAG, "Parsed token successfully");
                        postSuccess(callback, token);
                    } else {
                        Log.e(TAG, "Token missing in server response");
                        postFailure(callback, "Token missing in response");
                    }
                } else {
                    Log.e(TAG, "Token server HTTP error: " + code);
                    postFailure(callback, "Token server HTTP " + code + ": " + body);
                }

            } catch (Exception e) {
                Log.e(TAG, "Exception during token fetch", e);
                postFailure(callback, "Exception: " + e.getMessage());
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    /** Parse plain token or JSON { "token": "..." } */
    private String parseToken(String body) {
        if (body == null) return null;
        String trimmed = body.trim();

        // Simple JSON parse (no dependency)
        if (trimmed.startsWith("{") && trimmed.contains("token")) {
            int i = trimmed.indexOf("token");
            int colon = trimmed.indexOf(":", i);
            if (colon > 0) {
                int startQuote = trimmed.indexOf('"', colon);
                int endQuote = (startQuote > 0)
                        ? trimmed.indexOf('"', startQuote + 1)
                        : -1;
                if (startQuote > 0 && endQuote > startQuote) {
                    return trimmed.substring(startQuote + 1, endQuote);
                }
            }
            return null;
        }
        return trimmed; // assume plain token
    }

    /** Read entire InputStream */
    private String readFully(InputStream is) throws IOException {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    /** Post success callback to main thread */
    private void postSuccess(TokenCallback cb, String token) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (cb != null) cb.onSuccess(token);
        });
    }

    /** Post failure callback to main thread */
    private void postFailure(TokenCallback cb, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (cb != null) cb.onFailure(msg);
        });
    }
}
