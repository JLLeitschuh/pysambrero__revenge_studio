package com.ninjaflip.androidrevenge.utils;


import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class HttpUtils {

    public static String ajaxPostGetHtml(Map<String, Object> postParams,
                                         String userAgent, String referer,
                                         int timeout, String proxyHost, int proxyPort) throws IOException {

        Proxy proxy = null;
        if (proxyHost != null && proxyPort != -1) {
            // Setup proxy
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        }

        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, Object> param : postParams.entrySet()) {
            if (postData.length() != 0)
                postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }
        byte[] postDataBytes = postData.toString().getBytes("UTF-8");

        URL obj = new URL("https://play.google.com/store/apps/collection/search_results_cluster_apps");
        HttpURLConnection conn;

        if (proxy != null) {
            conn = (HttpURLConnection) obj.openConnection(proxy);
        } else {
            conn = (HttpURLConnection) obj.openConnection();
        }
        conn.setReadTimeout(timeout);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        //conn.setRequestProperty("charset", "utf-8");
        conn.setUseCaches(false);
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Accept", "*/*");
        //conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        conn.setRequestProperty("Accept-Encoding", "gzip");
        conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
        conn.addRequestProperty("User-Agent", userAgent);
        conn.addRequestProperty("Referer", referer);
        conn.setInstanceFollowRedirects(true);

        conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        // must set do output to true with POST requests
        conn.setDoOutput(true);
        conn.getOutputStream().write(postDataBytes);

        int status = conn.getResponseCode();
        //System.out.println("ajax Post URL Response Code ... " + status);
        Reader reader;
        if ("gzip".equals(conn.getContentEncoding())) {
            reader = new InputStreamReader(new GZIPInputStream(conn.getInputStream()));
        } else {
            reader = new InputStreamReader(conn.getInputStream());
        }

        String html = IOUtils.toString(reader);
        reader.close();
        conn.disconnect();
        return html;
    }
}
