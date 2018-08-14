package com.ninjaflip.androidrevenge.core.scrapper.manager;


import com.google.common.collect.*;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.SocketMessagingProtocol;
import com.ninjaflip.androidrevenge.enums.EnumerationScrapper;
import com.ninjaflip.androidrevenge.enums.EnumerationScrapperSpeed;
import com.ninjaflip.androidrevenge.beans.ScrappedContentNoDuplicate;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppScrappingManager {
    private final static Logger LOGGER = Logger.getLogger(AppScrappingManager.class);
    private static AppScrappingManager INSTANCE;
    private static HashMap<String, String> languageCodeLanguageName;
    private HashMap<String, Boolean> defaultOptions = new HashMap<>();
    private int threadPoolSize;

    private AppScrappingManager() {
        languageCodeLanguageName = Utils.loadJsonLanguagesFileAsMap();

        defaultOptions.put("title", true);
        defaultOptions.put("developer", true);
        defaultOptions.put("shortDesc", true);
        defaultOptions.put("longDesc", true);
        defaultOptions.put("genre", true);
        defaultOptions.put("price", true);
        defaultOptions.put("icon", true);
        defaultOptions.put("revenueModel", true);
        defaultOptions.put("versioning", true);
        defaultOptions.put("rating", true);
        defaultOptions.put("size", true);
        defaultOptions.put("installs", true);
        defaultOptions.put("developerWebsite", true);
        defaultOptions.put("comments", true);
        defaultOptions.put("video", true);
        defaultOptions.put("screenshots", true);
        defaultOptions.put("recentChanges", true);

        threadPoolSize = Runtime.getRuntime().availableProcessors();
    }

    public static AppScrappingManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AppScrappingManager();
        }
        return INSTANCE;
    }

    /**
     * Get app full details (all associated titles, short and long descriptions for every language)
     *
     * @param appId
     * @param userAgent
     * @param proxyHost
     * @param proxyPort
     * @return
     */
    public JSONObject scrapMultiLanguageApp(String appId, String userAgent, String proxyHost,
                                            int proxyPort, EnumerationScrapperSpeed.ScrapperSpeed scrapperSpeed, Session session, ExecutorService pool) throws IOException, ExecutionException, InterruptedException {

        final JSONObject result = new JSONObject();


        // grab all details except long descriptions from the english-American page
        HashMap<String, Boolean> optionsEnglish = new HashMap<>();
        optionsEnglish.put("title", true);
        optionsEnglish.put("developer", true);
        optionsEnglish.put("shortDesc", true);
        optionsEnglish.put("longDesc", false);
        optionsEnglish.put("genre", true);
        optionsEnglish.put("price", true);
        optionsEnglish.put("icon", true);
        optionsEnglish.put("revenueModel", true);
        optionsEnglish.put("versioning", true);
        optionsEnglish.put("rating", true);
        optionsEnglish.put("size", true);
        optionsEnglish.put("installs", true);
        optionsEnglish.put("developerWebsite", true);
        optionsEnglish.put("comments", true);
        optionsEnglish.put("video", true);
        optionsEnglish.put("screenshots", true);
        optionsEnglish.put("recentChanges", true);


        // grab all other short descriptions, long descriptions and titles only
        HashMap<String, Boolean> optionsAll = new HashMap<>();
        optionsAll.put("title", true);
        optionsAll.put("developer", false);
        optionsAll.put("shortDesc", true);
        optionsAll.put("longDesc", true);
        optionsAll.put("genre", false);
        optionsAll.put("price", false);
        optionsAll.put("icon", false);
        optionsAll.put("revenueModel", false);
        optionsAll.put("versioning", false);
        optionsAll.put("rating", false);
        optionsAll.put("size", false);
        optionsAll.put("installs", false);
        optionsAll.put("developerWebsite", false);
        optionsAll.put("comments", false);
        optionsAll.put("video", false);
        optionsAll.put("screenshots", false);
        optionsAll.put("recentChanges", false);

        Map<String, Future<JSONObject>> tasks = new HashMap<>();


        // create a thread for 'app full details in english' and start it
        String countryCode = "us";
        DetailsScrappingCallable mainDetails = new DetailsScrappingCallable(appId, "en-US", countryCode,
                userAgent, proxyHost, proxyPort, optionsEnglish, scrapperSpeed.getValue()) {
            @Override
            public void sendLogEvent() {
                if (session != null && session.isOpen()) {
                    try {
                        SocketMessagingProtocol.getInstance()
                                .sendScrapperLogEvent(session, String.valueOf(0),
                                        EnumerationScrapper.EnumScrapperLogType.SINGLE_APP_PROGRESS);
                    } catch (Exception e) {
                        // do nothing
                    }
                } else {
                    System.err.println("Socket null or not open");
                }
            }
        };
        Future mainDetailsFuture = pool.submit(mainDetails);
        try {
            result.put("details", mainDetailsFuture.get());
        } catch (Exception e) {
            //LOGGER.error(e.toString());
            throw e;
        }

        // create threads for 'Long and Short Description for all languages' and start them all
        final JSONObject descriptions = new JSONObject();
        int i = 0;
        final int LANGUAGES_SIZE = languageCodeLanguageName.size();
        for (Map.Entry<String, String> entry : languageCodeLanguageName.entrySet()) {

            int finalI = i;
            DetailsScrappingCallable dc = new DetailsScrappingCallable(appId, entry.getKey(), countryCode,
                    userAgent, proxyHost, proxyPort, optionsAll, scrapperSpeed.getValue()) {
                @Override
                public void sendLogEvent() {
                    if (session != null && session.isOpen()) {
                        try {
                            SocketMessagingProtocol.getInstance()
                                    .sendScrapperLogEvent(session, String.valueOf(String.valueOf((finalI * 100) / LANGUAGES_SIZE)),
                                            EnumerationScrapper.EnumScrapperLogType.SINGLE_APP_PROGRESS);
                        } catch (Exception e) {
                            // do nothing
                        }
                    } else {
                        System.err.println("Socket null or not open");
                    }
                }
            };
            Future future = pool.submit(dc);
            tasks.put(entry.getKey(), future);
            i++;
        }

        for (Map.Entry<String, Future<JSONObject>> entry : tasks.entrySet()) {
            try {
                JSONObject resultCall = entry.getValue().get();
                if (resultCall != null) {
                    descriptions.put(entry.getKey(), entry.getValue().get());
                }
            } catch (Exception e) {
                for (Map.Entry<String, Future<JSONObject>> entryToCancel : tasks.entrySet()) {
                    entryToCancel.getValue().cancel(true);
                }
                pool.shutdownNow();
                throw e;
            }
        }

        // Start removing duplicates here
        JSONArray noDuplicates = new JSONArray();

        Map<String, String> mapLanguageCode2LongDesc = new HashMap<>();
        Map<String, JSONObject> mapLanguageCode2ShortDescAndTitle = new HashMap<>();
        for (Object key : descriptions.keySet()) {
            String keyStr = (String) key;
            Object keyValue = descriptions.get(keyStr);

            if (keyValue != null) {
                mapLanguageCode2LongDesc.put(keyStr, (String) ((JSONObject) keyValue).get("longDesc"));

                JSONObject titleAndShortDescObj = new JSONObject();
                titleAndShortDescObj.put("lang", keyStr);
                titleAndShortDescObj.put("lang_name", languageCodeLanguageName.get(keyStr));
                titleAndShortDescObj.put("title", ((JSONObject) keyValue).get("title"));
                titleAndShortDescObj.put("shortDesc", ((JSONObject) keyValue).get("shortDesc"));
                mapLanguageCode2ShortDescAndTitle.put(keyStr, titleAndShortDescObj);
            }
        }

        Multimap<String, String> multiMap = HashMultimap.create();
        for (Map.Entry<String, String> entry : mapLanguageCode2LongDesc.entrySet()) {
            multiMap.put(entry.getValue(), entry.getKey());
        }

        for (Map.Entry<String, Collection<String>> entry : multiMap.asMap().entrySet()) {
            ScrappedContentNoDuplicate sc = new ScrappedContentNoDuplicate();
            sc.setAssociatedLanguageCodes(entry.getValue());
            sc.setLongDescription(entry.getKey());

            sc.setShortDescriptionsAndTitlesPerLanguage(new ArrayList<JSONObject>());
            for (Map.Entry<String, JSONObject> entry2 : mapLanguageCode2ShortDescAndTitle.entrySet()) {
                if (sc.getAssociatedLanguageCodes().contains(entry2.getKey())) {
                    sc.getShortDescriptionsAndTitlesPerLanguage().add(entry2.getValue());
                }
            }

            noDuplicates.add(sc);
        }
        // Ended removing duplicates

        result.put("noDuplicates", noDuplicates);
        return result;
    }

    /**
     * Get app details and all associated description for one language only
     *
     * @param appId
     * @param language
     * @param userAgent
     * @param proxyHost
     * @param proxyPort
     * @param proxyPort
     * @return
     */
    public JSONObject scrapMonoLanguageApp(String appId, String language, String countryCode, String userAgent, String proxyHost, int proxyPort) throws IOException {
        return getAppDetails(appId, language, countryCode, userAgent, proxyHost, proxyPort, null);
    }

    /**
     * Scrapp an app page from google plays
     * This method contains the whole scrapping logic
     *
     * @param appId            thr package name
     * @param language         page language
     * @param countryCode      country code
     * @param userAgent        the user agent sent with header
     * @param proxyHost        proxy IP address used to scrap
     * @param proxyPort        proxy Host address used to scrap
     * @param scrappingOptions option table containing the desired data
     * @return a json object containing all the data and ready to be sent to the client
     * @throws IOException
     */
    private JSONObject getAppDetails(String appId, String language, String countryCode, String userAgent, String proxyHost, int proxyPort, HashMap<String, Boolean> scrappingOptions) throws HttpStatusException, IOException {

        HashMap<String, Boolean> options;
        if (scrappingOptions == null) {
            options = defaultOptions;
        } else {
            options = scrappingOptions;
        }

        // The object that will contain the results
        JSONObject result = new JSONObject();

        Proxy proxy = null;
        if (proxyHost != null && proxyPort != -1) {
            // Setup proxy
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        }

        // init jsoup
        Connection conn = Jsoup.connect("https://play.google.com/store/apps/details?id=" + appId + "&hl=" + language + "&gl=" + countryCode)
                .timeout(30000)
                .userAgent(userAgent)
                .referrer("http://www.google.com")
                .followRedirects(true);
        if (proxy != null) {
            conn.proxy(proxy);
            //LOGGER.debug("scrap using proxy host " + proxyHost + ":" + proxyPort + " for language " + language + " and country " + countryCode);
        }



        Document doc;
        try {
            doc = conn.get();
        } catch (org.jsoup.HttpStatusException ex) {
            LOGGER.error("error scrapper : " + ex.getStatusCode() + " : " + ex.getMessage());
            throw ex;
        }

        String titleCssSelector = ".AHFaub > span";
        String developerCssSelector = ".hrTbp.R8zArc";
        String shortDescCssSelector = "meta[itemprop=description]";
        String longDescCssSelector = "div[jsname=sngebd]";
        String genreCssSelector = "a[itemprop=genre]";
        String iconCssSelector = "img.T75of.ujDFqe";
        String inappCssSelector = ".rxic6";
        String adsSupportedCssSelector = ".rxic6";
        String scoreCssSelector = ".BHMmbe";
        String nbReviewsCssSelector = ".AYi5wd.TBRnV > span";



        // Parse the page
        if (options.get("title")) {
            String title = doc.select(titleCssSelector).first().text();
            result.put("title", title);
        }

        if (options.get("developer")) {
            String developer = doc.select(developerCssSelector).first().text();
            result.put("developer", developer);
        }

        if (options.get("shortDesc")) {
            /*
            String shortDesc = doc.select(shortDescCssSelector).first().attr("content");
            result.put("shortDesc", shortDesc);
            */
        }

        if (options.get("longDesc")) {
            // we must remove all <br> tags as it becomes two break-lines on the client side (\n<br>)
            String longDesc = doc.select(longDescCssSelector).first().html().replaceAll("<br>", "");
            result.put("longDesc", longDesc);
        }

        if (options.get("genre")) {
            Element mainGenre = doc.select(genreCssSelector).first();
            String genreText = mainGenre.text().trim();
            String genreId = mainGenre.attr("href").split("/")[4];
            result.put("genreText", genreText);
            result.put("genreId", genreId);
        }

        if (options.get("price")) {
            String price = doc.select("meta[itemprop=price]").attr("content");
            result.put("price", price);
        }

        if (options.get("icon")) {
            String icon = doc.select(iconCssSelector).attr("src");
            result.put("icon", icon);
        }


        if (options.get("revenueModel")) {
            /*
            boolean offersIAP = doc.select(inappCssSelector).size() != 0;
            boolean adSupported = doc.select(adsSupportedCssSelector).size() != 0;
            result.put("offersIAP", offersIAP);
            result.put("adSupported", adSupported);
            */
        }



        Elements additionalInfo = doc.select(".details-section-contents");
        if (options.get("versioning")) {
            /*
            try {
                String version = additionalInfo.select("div.content[itemprop=softwareVersion]").text().trim();
                String updated = additionalInfo.select("div.content[itemprop=datePublished]").first().text().trim();
                String androidVersionText = additionalInfo.select("div.content[itemprop=operatingSystems]").first().text().trim();
                String androidVersion = normalizeAndroidVersion(androidVersionText);
                result.put("version", version);
                result.put("updated", updated);
                result.put("androidVersion", androidVersion);
            } catch (Exception e) {
                // do nothing
            }
            */
        }



        if (options.get("rating")) {
            long nbReviews = cleanLong(doc.select(nbReviewsCssSelector).first().text());

            result.put("nbReviews", nbReviews);


            Float score = Float.parseFloat(doc.select(scoreCssSelector).first().text().replace(',', '.'));
            result.put("score", score);

        }

        if (options.get("size")) {
            /*
            Elements sizeEl = additionalInfo.select("div.content[itemprop=fileSize]");
            if (sizeEl.size() != 0) {
                String size = sizeEl.text().trim();
                result.put("size", size);
            }
            */
        }

        if (options.get("installs")) {
            /*
            Elements preregister = doc.select(".preregistration-container");
            if (preregister.size() == 0) {
                String[] installs = installNumbers(additionalInfo.select("div.content[itemprop=numDownloads]"));
                if (installs != null) {
                    long minInstalls = cleanLong(installs[0]);
                    long maxInstalls = cleanLong(installs[1]);
                    result.put("minInstalls", minInstalls);
                    result.put("maxInstalls", maxInstalls);
                }
            }
            */
        }

        if (options.get("developerWebsite")) {
            /*
            Elements developerWebsiteEl = additionalInfo.select(".hrTbp[href^=http]");
            if (developerWebsiteEl.size() > 0) {
                String developerWebsite = developerWebsiteEl.first().attr("href");
                // extract clean url wrapped in google url
                Map<String, List<String>> map = Utils.splitQuery(new URL(developerWebsite));
                developerWebsite = map.get("q").get(0);
                result.put("developerWebsite", developerWebsite);
            }
            */
        }


        if (options.get("comments")) {
            /*
            Elements commentsEls = doc.select(".quoted-review");
            if (commentsEls.size() != 0) {
                JSONArray commentsArray = new JSONArray();
                for (Element com : commentsEls) {
                    commentsArray.add(com.text().trim());
                }
                result.put("comments", commentsArray);
            }
            */
        }

        if (options.get("video")) {
            /*
            Elements videoEl = doc.select(".screenshots span.preview-overlay-container[data-video-url]");
            if (videoEl.size() != 0) {
                String video = videoEl.first().attr("data-video-url").split("\\?")[0];
                result.put("video", video);
            }
            */
        }

        if (options.get("screenshots")) {
            /*
            Elements screenshotsEls = doc.select(".thumbnails .screenshot");
            if (screenshotsEls.size() != 0) {
                JSONArray screenshotsArray = new JSONArray();
                for (Element sc : screenshotsEls) {
                    screenshotsArray.add(sc.attr("src"));
                }
                result.put("screenshots", screenshotsArray);
            }
            */
        }

        if (options.get("recentChanges")) {
            /*
            Elements recentChangesEls = doc.select(".recent-change");
            if (recentChangesEls.size() != 0) {
                JSONArray recentChangesArray = new JSONArray();
                for (Element rc : recentChangesEls) {
                    recentChangesArray.add(rc.text());
                }
                result.put("recentChanges", recentChangesArray);
            }
            */
        }

        //LOGGER.debug("scapping result : " + result);
        return result;
    }

    // transform string to integer
    private String normalizeAndroidVersion(String androidVersionText) {
        Pattern p = Pattern.compile("([0-9\\.]+)[^0-9\\.].+");
        Matcher matcher = p.matcher(androidVersionText);
        if (!matcher.matches()) {
            return "VARY";
        }
        return matcher.group(1);
    }


    // transform string to long (installs, rating...)
    // we use long instead of integer => problem with facebook app number of downloads > 5.000.000.000 => java.lang.NumberFormatException
    long cleanLong(String number) {
        number = number.replaceAll("[\\D]", "");
        return Long.parseLong(number);
    }

    int cleanInt(String number) {
        number = number.replaceAll("[\\D]", "");
        return Integer.parseInt(number);
    }

    // parse install number string to get the min and max installs
    private String[] installNumbers(Elements downloads) {

        if (downloads.size() == 0) {
            return new String[]{"0", "0"};
        }
        String[] separators = {" - ", " et ", "–", "-", "~", " a "};
        String downloadsStr = downloads.first().text().trim();
        for (String separator : separators) {
            String[] split = downloadsStr.split(separator);
            if (split.length == 2) {
                return split;
            }
        }
        return null;
    }


    /**
     * A callable object used for multithreading process of scrapping
     * This object is manipulated through the ExecutorService
     */
    abstract class DetailsScrappingCallable implements Callable {
        private String appId;
        private String language;
        private String countryCode;
        private String userAgent;
        private String proxyHost;
        private int proxyPort;
        private HashMap<String, Boolean> scrappingOptions;
        private int threadSleepDuration;

        public DetailsScrappingCallable(String appId, String language, String countryCode, String userAgent, String proxyHost,
                                        int proxyPort, HashMap<String, Boolean> scrappingOptions) {
            this(appId, language, countryCode, userAgent, proxyHost, proxyPort, scrappingOptions, 200);
        }

        public DetailsScrappingCallable(String appId, String language, String countryCode, String userAgent, String proxyHost,
                                        int proxyPort, HashMap<String, Boolean> scrappingOptions, int threadSleepDuration) {
            this.appId = appId;
            this.language = language;
            this.countryCode = countryCode;
            this.userAgent = userAgent;
            this.proxyHost = proxyHost;
            this.proxyPort = proxyPort;
            this.scrappingOptions = scrappingOptions;
            this.threadSleepDuration = threadSleepDuration;
        }

        public abstract void sendLogEvent();

        public JSONObject call() throws Exception {
            JSONObject result = getAppDetails(appId, language, countryCode, userAgent, proxyHost, proxyPort, scrappingOptions);
            // LOGGER.debug("Finished scrapping for language ===> (" + language + ")");
            Thread.sleep(threadSleepDuration);
            sendLogEvent();
            return result;

        }

        public String getLanguage() {
            return language;
        }

        public String getAppId() {
            return appId;
        }
    }
}
