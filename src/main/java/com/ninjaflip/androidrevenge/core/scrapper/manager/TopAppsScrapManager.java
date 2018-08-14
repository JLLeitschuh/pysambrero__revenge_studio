package com.ninjaflip.androidrevenge.core.scrapper.manager;

import com.ninjaflip.androidrevenge.beans.App;
import com.ninjaflip.androidrevenge.utils.HttpUtils;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import rx.Observable;
import rx.functions.Func1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Solitario on 12/05/2017.
 * <p>
 * Business logic for scrapping top apps
 */
public class TopAppsScrapManager {
    private final static Logger LOGGER = Logger.getLogger(TopAppsScrapManager.class);
    private static TopAppsScrapManager INSTANCE;
    /*private static final int APP_COUNT = 150; // the amount of apps to retrieve.
    private static final int APP_COUNT_PLUS_ONE = 151; // the amount of apps to retrieve plus one => check for pagination*/

    private static final int APP_COUNT = 50; // the amount of apps to retrieve.
    private static final int APP_COUNT_PLUS_ONE = 51; // the amount of apps to retrieve plus one => check for pagination

    private TopAppsScrapManager() {
    }

    public static TopAppsScrapManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TopAppsScrapManager();
        }
        return INSTANCE;
    }

    public JSONObject scrapTopAppsFirstPage(String languageCode, String countryCode, String collection, String category,
                                            String userAgent, String proxyHost, int proxyPort) throws IOException {
        JSONObject result = new JSONObject();

        String referer = "https://play.google.com";
        List<App> apps = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("languageCode", languageCode);
        params.put("countryCode", countryCode);
        params.put("collection", collection);
        params.put("category", category);
        params.put("userAgent", userAgent);
        params.put("proxyHost", proxyHost);
        params.put("proxyPort", proxyPort);
        params.put("referer", referer);

        // Make the initial request and extract its html
        // the returned document contains first page of the pagination (contains at most 50 apps)
        Document doc = initialRequest(params);


        apps.addAll(Observable.from(doc.select(".card"))
                .map(new Func1<Element, App>() {
                    @Override
                    public App call(Element element) {
                        return parseApp(element);
                    }
                }).toList().toBlocking().first());

        //LOGGER.debug("Found a list of: " + apps.size() + " apps");

        if (apps.size() == APP_COUNT_PLUS_ONE) {
            // send only APP_COUNT elements and keep the rest for nex page
            List<App> mApps = new LinkedList<App>(apps);
            mApps.remove(mApps.size() - 1);
            result.put("apps", mApps);
            result.put("pagination", true);
        } else {
            result.put("apps", apps);
            result.put("pagination", false);
        }
        //LOGGER.debug("result : " + result.toJSONString());
        return result;
    }

    /**
     * Make the first search request as in the browser and call 'checkfinished' to
     * process the next pages.
     *
     * @return html of the first page
     * @throws IOException
     */
    private Document initialRequest(Map<String, Object> params) throws IOException {
        String languageCode = (String) params.get("languageCode");
        String countryCode = (String) params.get("countryCode");
        String collection = (String) params.get("collection");
        String category = (String) params.get("category");
        String userAgent = (String) params.get("userAgent");
        String referer = (String) params.get("referer");
        String proxyHost = (String) params.get("proxyHost");
        int proxyPort = (int) params.get("proxyPort");

        Proxy proxy = null;
        if (proxyHost != null && proxyPort != -1) {
            // Setup proxy
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        }

        String url = "https://play.google.com/store/apps";
        if (!category.equals("ALL")) {
            String[] catSplit = category.split(";");
            if(category.startsWith("FAMILY") && catSplit.length == 2) {
                url += "/category/" + catSplit[0];
                url += "/collection/" + collection;
                url += "?hl=" + languageCode + "&gl=" + countryCode
                        + "&start=0&num=" + APP_COUNT_PLUS_ONE + "&"+catSplit[1];
            }else {
                url += "/category/" + category;
                url += "/collection/" + collection;
                url += "?hl=" + languageCode + "&gl=" + countryCode + "&start=0&num=" + APP_COUNT_PLUS_ONE;
            }
        }else{
            url += "/collection/" + collection;
            url += "?hl=" + languageCode + "&gl=" + countryCode + "&start=0&num=" + APP_COUNT_PLUS_ONE;
        }

        //LOGGER.debug("url search : " + url);

        Connection conn = Jsoup.connect(url)
                .header("Accept-Encoding", "gzip, deflate")
                .userAgent(userAgent)
                .maxBodySize(0)
                .timeout(30 * 1000)
                .proxy(proxy)
                .referrer(referer)
                .followRedirects(true);
        if (proxy != null) {
            conn.proxy(proxy);
            //LOGGER.debug("using proxy host " + proxyHost + ":" + proxyPort);
        }
        return conn.get();
    }


    public JSONObject scrapTopAppsNextPage(int nexPageInt, String languageCode, String countryCode, String collection, String category,
                                           String userAgent, String proxyHost, int proxyPort) throws IOException {
        JSONObject result = new JSONObject();

        String referer = "https://play.google.com";
        List<App> apps = new ArrayList<>();


        // Make request for the next page and extract its html
        Proxy proxy = null;
        if (proxyHost != null && proxyPort != -1) {
            // Setup proxy
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        }



        /*
        during tests we notices that once we exceed 200 apps, then google starts sending HTTP 429 error code

        For example google response to this URL https://play.google.com/store/apps/collection/topselling_free?hl=en-US&gl=MA&start=199&num=120
        is:  429 Too Many Requests, Please reduce your request rate

         while this URL https://play.google.com/store/apps/collection/topselling_free?hl=en-US&gl=MA&start=199&num=120
         works great.

         this is a strange behaviour
         So, we modify our algorithm such as when we reach index 199 then seek a maximum of 120 app => total 319 app
         */
        int start, num, appListSizeForPagination = 51;
        switch (nexPageInt) {
            case 1: {
                start = 50;
                num = 51;
                appListSizeForPagination = 51;
                break;
            }
            case 2: {
                start = 100;
                num = 51;
                appListSizeForPagination = 51;
                break;
            }
            case 3: {
                start = 150;
                num = 50;
                appListSizeForPagination = 50;
                break;
            }
            case 4: {
                start = 199;
                num = 120;
                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid pagination parameter '" + nexPageInt + "', valid parameters are 1, 2, 3, and 4");
            }
        }

        String url = "https://play.google.com/store/apps";
        if (!category.equals("ALL")) {
            String[] catSplit = category.split(";");
            if(category.startsWith("FAMILY") && catSplit.length == 2) {
                url += "/category/" + catSplit[0];
                url += "/collection/" + collection;
                url += "?hl=" + languageCode + "&gl=" + countryCode + "&start=" + start + "&num=" + num + "&"+catSplit[1];
            }else {
                url += "/category/" + category;
                url += "/collection/" + collection;
                url += "?hl=" + languageCode + "&gl=" + countryCode + "&start=" + start + "&num=" + num;
            }
        }else{
            url += "/collection/" + collection;
            url += "?hl=" + languageCode + "&gl=" + countryCode + "&start=" + start + "&num=" + num;
        }

        //LOGGER.debug("url search : " + url);

        Connection conn = Jsoup.connect(url)
                .header("Accept-Encoding", "gzip, deflate")
                .userAgent(userAgent)
                .maxBodySize(0)
                .timeout(30 * 1000)
                .proxy(proxy)
                .referrer(referer)
                .followRedirects(true);
        if (proxy != null) {
            conn.proxy(proxy);
            //LOGGER.debug("using proxy host " + proxyHost + ":" + proxyPort);
        }

        Document doc = conn.get();


        apps.addAll(Observable.from(doc.select(".card"))
                .map(new Func1<Element, App>() {
                    @Override
                    public App call(Element element) {
                        return parseApp(element);
                    }
                }).toList().toBlocking().first());

        //LOGGER.debug("Found a list of: " + apps.size() + " apps");

        if (nexPageInt != 4 && apps.size() == appListSizeForPagination) {
            // send only APP_COUNT elements and keep the rest for nex page
            List<App> mApps = new LinkedList<App>(apps);
            mApps.remove(mApps.size() - 1);
            result.put("apps", mApps);
            result.put("pagination", true);
        } else {
            result.put("apps", apps);
            result.put("pagination", false);
        }
        //LOGGER.debug("$$$$$$$$$$$$$$ result : " + result.toJSONString());
        return result;
    }


    private App parseApp(Element element) {
        Float score = null;
        String price = null;
        String scoreText = element.select("div.tiny-star").attr("aria-label");
        if (scoreText != null) {
            Matcher matcher = Pattern.compile("([\\d.]+)").matcher(scoreText);
            if (matcher.find()) {
                score = Float.parseFloat(matcher.group());
            }
        }


        Element priceElement = element.select("span.display-price").first();
        if (priceElement != null) {
            price = priceElement.text();
        }
        boolean free = (price == null || !price.matches(".*\\d.*"));

        Element a = element.select("a").first();
        Element aTitle = element.select("a.title").first();
        Element aSubtitle = element.select("a.subtitle").first();
        Element aCoverImage = element.select("img.cover-image").first();

        String appId = a != null ? a.attr("href").replace("/store/apps/details?id=", "") : null;
        String title = aTitle != null ? aTitle.attr("title") : null;
        String developer = aSubtitle != null ? aSubtitle.text() : null;
        //String icon = aCoverImage != null ? aCoverImage.attr("data-cover-large") : null;
        String icon = aCoverImage != null ? aCoverImage.attr("src") : null;

        App app = new App();
        app.appId = appId;
        app.name = title;
        app.developer = developer;
        app.icon = icon;
        app.rating = score;
        app.free = free;
        if (!free) {
            app.price = price;
        }
        //LOGGER.debug(app.toString());
        return app;
    }
}

