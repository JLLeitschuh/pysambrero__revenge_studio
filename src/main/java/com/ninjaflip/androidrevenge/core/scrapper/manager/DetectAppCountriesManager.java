package com.ninjaflip.androidrevenge.core.scrapper.manager;

import com.ninjaflip.androidrevenge.beans.App;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.SocketMessagingProtocol;
import com.ninjaflip.androidrevenge.enums.EnumerationScrapper;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import rx.Observable;
import rx.functions.Func1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DetectAppCountriesManager {
    private final static Logger LOGGER = Logger.getLogger(DetectAppCountriesManager.class);
    private static DetectAppCountriesManager INSTANCE;
    private static HashMap<String, String> countryCodeCountryName = Utils.loadJsonCountriesFileAsMap();
    //private int threadPoolSize;

    private DetectAppCountriesManager() {
        //countryCodeCountryName = Utils.loadJsonCountriesFileAsMap();
        //threadPoolSize = Runtime.getRuntime().availableProcessors();
    }

    public static DetectAppCountriesManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DetectAppCountriesManager();
        }
        return INSTANCE;
    }

    public JSONObject scrapNonAvailableCountries(String appId, String userAgent, String proxyHost, int proxyPort, Session session, ExecutorService pool)
            throws IOException, ExecutionException, InterruptedException {
        JSONObject result = new JSONObject();
        /*
         check if item is available in the app store
         if available get its icon, title, developer
         else stop scrapping => app not exists
          */
        Proxy proxy = null;
        if (proxyHost != null && proxyPort != -1) {
            // Setup proxy
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        }

        // init jsoup
        Connection conn = Jsoup.connect("https://play.google.com/store/apps/details?id=" + appId + "&hl=en-US")
                .timeout(30000)
                .userAgent(userAgent)
                .referrer("http://www.google.com")
                .followRedirects(true);
        if (proxy != null) {
            conn.proxy(proxy);
        }

        Document doc;
        try {
            doc = conn.get();
        } catch (Exception ex) {
            //LOGGER.error(ex.toString());
            throw ex;
        }

        // Parse the page
        String titleCssSelector = ".AHFaub > span";
        String developerCssSelector = ".hrTbp.R8zArc";
        String iconCssSelector = "img.T75of.ujDFqe";

        String title = doc.select(titleCssSelector).first().text();
        result.put("title", title);
        String developer = doc.select(developerCssSelector).first().text();
        result.put("developer", developer);
        String icon = doc.select(iconCssSelector).attr("src");
        if(icon.endsWith("-rw")){
            icon = icon.substring(0, icon.length() - 3);// remove last three characters "-rw"
        }
        result.put("icon", icon);
        result.put("appId", appId);


        Map<String, Future<Boolean>> tasks = new HashMap<>();

        int i = 0;
        final int COUNTRIES_SIZE = countryCodeCountryName.size();

        for (Map.Entry<String, String> entry : countryCodeCountryName.entrySet()) {
            // best config to avoid 404 response from google play is threadSleepDuration = (700, 3000)
            int threadSleepDuration = 700;
            if (i % 5 == 0) {
                threadSleepDuration = 3000;
            }

            int finalI = i;
            CountryScrappingCallable cc = new CountryScrappingCallable(appId, "en-US", entry.getKey(),
                    userAgent, proxyHost, proxyPort, threadSleepDuration) {
                @Override
                public void sendLogEvent() {
                    if (session != null && session.isOpen()) {
                        try {
                            SocketMessagingProtocol.getInstance()
                                    .sendScrapperLogEvent(session, String.valueOf((finalI * 100) / COUNTRIES_SIZE),
                                            EnumerationScrapper.EnumScrapperLogType.COUNTRY_PROGRESS);
                        } catch (Exception e) {
                            // do nothing
                        }
                    } else {
                        System.err.println("Socket null or not open");
                    }
                }
            };
            Future future = pool.submit(cc);
            tasks.put(entry.getKey(), future);
            i++;
        }

        List<String> listNonAvailableCountries = new ArrayList<>();
        for (Map.Entry<String, Future<Boolean>> entry : tasks.entrySet()) {
            try {
                boolean isAvailable = entry.getValue().get();
                if (!isAvailable) {
                    listNonAvailableCountries.add(entry.getKey());
                }
            } catch (Exception e) {
                for (Map.Entry<String, Future<Boolean>> entryToCancel : tasks.entrySet()) {
                    entryToCancel.getValue().cancel(true);
                }
                pool.shutdownNow();
                throw e;
            }
        }
        result.put("listNonAvailableCountries", listNonAvailableCountries);
        return result;
    }


    private boolean isAppAvailableForCountry(String appId, String language, String countryCode, String userAgent, String proxyHost, int proxyPort) throws IOException {

        Map<String, Object> params = new HashMap<>();
        params.put("keyword", "\"" + appId + "\"");
        params.put("languageCode", language);
        params.put("countryCode", countryCode);
        params.put("userAgent", userAgent);
        params.put("referer", "https://play.google.com");
        params.put("proxyHost", proxyHost);
        params.put("proxyPort", proxyPort);

        // Make the initial request and extract its html
        // the returned document contains first page of the pagination (contains at least 49 apps)
        Document doc;
        try {
            doc = initialRequest(params);
        } catch (Exception ex) {
            //LOGGER.error(ex.getMessage());
            //ex.printStackTrace();
            throw ex;
        }

        List<App> apps = new ArrayList<>();
        apps.addAll(Observable.from(doc.select(".card"))
                .map(new Func1<Element, App>() {
                    @Override
                    public App call(Element element) {
                        return parseApp(element);
                    }
                }).toList().toBlocking().first());

        if (apps.size() == 0) {
            return false;
        } else {
            for (App app : apps) {
                if (app.appId.equals(appId)) {
                    return true;
                }
            }
            return false;
        }
    }


    private Document initialRequest(Map<String, Object> params) throws IOException {
        String keyword = (String) params.get("keyword");
        String languageCode = (String) params.get("languageCode");
        String countryCode = (String) params.get("countryCode");
        String userAgent = (String) params.get("userAgent");
        String referer = (String) params.get("referer");
        String proxyHost = (String) params.get("proxyHost");
        int proxyPort = (int) params.get("proxyPort");

        Proxy proxy = null;
        if (proxyHost != null && proxyPort != -1) {
            // Setup proxy
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        }

        String url = "https://play.google.com/store/search?c=apps&q=" + keyword
                + "&hl=" + languageCode + "&gl=" + countryCode;
        //LOGGER.debug("url search : " + url);

        Connection conn = Jsoup.connect(url)
                .header("Accept-Encoding", "gzip, deflate")
                .userAgent(userAgent)
                .maxBodySize(0)
                .timeout(30 * 1000)
                .referrer(referer)
                .followRedirects(true);
        if (proxy != null) {
            conn.proxy(proxy);
            //LOGGER.debug("using proxy host " + proxyHost + ":" + proxyPort);
        }
        final Document doc = conn.get();
        return skipClusterPage(doc, params);
    }

    /**
     * sometimes the first result page is a cluster of subsections,
     * we need to skip to the full results page
     */
    private Document skipClusterPage(Document doc, Map<String, Object> params) throws IOException {
        Pattern pattern = Pattern.compile("href=\"\\/store\\/apps\\/collection\\/search_collection_more_results_cluster?(.*?)\"");
        Matcher matcher = pattern.matcher(doc.html());

        if (matcher.find()) { // we are in the cluster page ==> get the inner url and visit it
            //LOGGER.warn("Cluster page detected");
            // Skip the cluster page
            String innerUrl = "https://play.google.com/" + matcher.group(0).split("\"")[1].replaceAll("%3D", "=");
            //LOGGER.warn("got inner url (cluster page): " + innerUrl);
            //Utils.speak("Cluster page");

            String userAgent = (String) params.get("userAgent");
            String referer = (String) params.get("referer");
            String proxyHost = (String) params.get("proxyHost");
            int proxyPort = (int) params.get("proxyPort");

            Connection newConn = Jsoup.connect(innerUrl)
                    .header("Accept-Encoding", "gzip, deflate")
                    .userAgent(userAgent)
                    .maxBodySize(0)
                    .timeout(60 * 1000)
                    .referrer(referer)
                    .followRedirects(true);
            if (proxyHost != null && proxyPort != -1) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                newConn.proxy(proxy);
            }
            return newConn.get();
        } else {
            //LOGGER.warn("NO cluster page...");
            return doc;
        }
    }

    private App parseApp(Element element) {
        Element a = element.select("a").first();
        String appId = a != null ? a.attr("href").replace("/store/apps/details?id=", "") : null;
        App app = new App();
        app.appId = appId;
        return app;
    }


    private App getAppDetails(Element element) {
        return null;
    }


    /**
     * A callable object used for multithreading process of scrapping
     * This object is manipulated through the ExecutorService
     */
    abstract class CountryScrappingCallable implements Callable<Boolean> {
        private String appId;
        private String language;
        private String countryCode;
        private String userAgent;
        private String proxyHost;
        private int proxyPort;
        private int threadSleepDuration;
        private Session session;

        public CountryScrappingCallable(String appId, String language, String countryCode, String userAgent, String proxyHost,
                                        int proxyPort, Session session) {
            this(appId, language, countryCode, userAgent, proxyHost, proxyPort, 200);
        }

        public CountryScrappingCallable(String appId, String language, String countryCode, String userAgent, String proxyHost,
                                        int proxyPort, int threadSleepDuration) {
            this.appId = appId;
            this.language = language;
            this.countryCode = countryCode;
            this.userAgent = userAgent;
            this.proxyHost = proxyHost;
            this.proxyPort = proxyPort;
            this.threadSleepDuration = threadSleepDuration;
        }

        public abstract void sendLogEvent();

        public Boolean call() throws Exception {
            boolean result = isAppAvailableForCountry(appId, language, countryCode, userAgent, proxyHost, proxyPort);
            //LOGGER.debug("App availability in county " + countryCode + ": " + countryCodeCountryName.get(countryCode) + " ==> " + result);
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

        public String getCountryCode() {
            return countryCode;
        }
    }
}
