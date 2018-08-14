package com.ninjaflip.androidrevenge.core.scrapper.manager;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * Created by Solitario on 01/08/2017.
 */
public class ApkDownloadManager {
    private final static Logger LOGGER = Logger.getLogger(ApkDownloadManager.class);
    private static ApkDownloadManager INSTANCE;

    private ApkDownloadManager() {
    }

    public static ApkDownloadManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ApkDownloadManager();
        }
        return INSTANCE;
    }

    public String getApkDownloadUrl(final String packageName) throws IOException {
        Elements data = Jsoup.connect("http://apk-dl.com/" + packageName).get().select(".download-btn .mdl-button");
        if (data.size() > 0) {
            Elements data2 = Jsoup.connect("http://apk-dl.com" + data.attr("href")).get()
                    .select(".detail a");
            if (data2.size() > 0) {
                System.out.println("URL = " + data2.attr("href"));
                Elements data3 = Jsoup.connect(data2.attr("href")).get()
                        .select(".contents a");
                return (data3.size() > 0) ? "http:" + data3.attr("href") : "";
            }
        }
        return "";
    }

    public static void downloadApkUsingGooglePlayApi(String login, String password,
                                                     String androidId, String assetId) {

    }
}
