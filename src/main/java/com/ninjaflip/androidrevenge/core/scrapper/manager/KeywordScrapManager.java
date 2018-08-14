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
 */

public class KeywordScrapManager {
    private final static Logger LOGGER = Logger.getLogger(KeywordScrapManager.class);
    private static KeywordScrapManager INSTANCE;

    private KeywordScrapManager() {
    }

    public static KeywordScrapManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new KeywordScrapManager();
        }
        return INSTANCE;
    }

    public JSONObject paginationRequest(String token, String clp, String languageCode, String countryCode,
                                        String userAgent, String proxyHost, int proxyPort) throws IOException{
        JSONObject result = new JSONObject();
        List<App> apps = new ArrayList<>();
        String referer = "https://play.google.com";
        Document nextPageDoc = getNexPageDocument(token, clp, languageCode,
                countryCode, userAgent, referer, proxyHost, proxyPort);

        String newToken = getNextPageToken(nextPageDoc.html());
        //Log.d(TAG, "Found new Token: " + token);
        // extract the clp
        String newClp = getClp(nextPageDoc.html());
        if(newClp == null){
            newClp = clp;
        }

        /*Elements listNextMappedApps = nextPageDoc.select(".card");
        for(Element elApp : listNextMappedApps){
            apps.add(parseApp(elApp));
        }*/

        apps.addAll(Observable.from(nextPageDoc.select(".card"))
                .map(new Func1<Element, App>() {
                    @Override
                    public App call(Element element) {
                        return parseApp(element);
                    }
                }).toList().toBlocking().first());




        //LOGGER.debug("Found a list of: " + apps.size() + " apps");

        result.put("apps",apps);
        result.put("token",newToken);
        result.put("cpl",newClp);
        result.put("languageCode",languageCode);
        result.put("countryCode",countryCode);
        result.put("userAgent",userAgent);

        return result;
    }

    /**
     *
     */

    public JSONObject scrapKeywordSearch(String keyword, String languageCode,
                                         String countryCode,int price, String userAgent,
                                         String proxyHost, int proxyPort) throws IOException {
        JSONObject result = new JSONObject();

        String referer = "https://play.google.com";
        List<App> apps = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("languageCode", languageCode);
        params.put("countryCode", countryCode);
        params.put("price", price);
        params.put("userAgent", userAgent);
        params.put("referer", referer);
        params.put("proxyHost", proxyHost);
        params.put("proxyPort", proxyPort);


        // Make the initial request and extract its html
        // the returned document contains first page of the pagination (contains at least 49 apps)
        Document doc = initialRequest(params);


        // extract the token for the next page request
        String token = getNextPageToken(doc.html());
        //LOGGER.debug("Found Token: " + token);

        String clp = getClp(doc.html());
        //LOGGER.debug("Found Clp: " + clp);


        /*Elements listMappedApps = doc.select(".card");
        for(Element elApp : listMappedApps){
            apps.add(parseApp(elApp));
        }*/

        apps.addAll(Observable.from(doc.select(".card"))
                .map(new Func1<Element, App>() {
                    @Override
                    public App call(Element element) {
                        return parseApp(element);
                    }
                }).toList().toBlocking().first());


        if(token == null){
            // the result of search is not paginated => zero or few apps
            //LOGGER.debug("Found a list of: " + apps.size() + " apps");
            result.put("apps",apps);
            return result;
        }
        // the result of search is paginated => many apps in one or more pages
        if(apps.size()<49){
            // sometime google play displays 10 results and fetches the rest using ajax.
            // we don't want to show 10, so we check if result size is less than 49,
            // then we request the next page
            Document nextPageDoc = getNexPageDocument(token, clp, languageCode,
                    countryCode, userAgent, referer, proxyHost, proxyPort);
            // extract the token for the next page request
            String newToken = getNextPageToken(nextPageDoc.html());
            //Log.d(TAG, "Found new Token: " + token);
            // extract the clp
            String newClp = getClp(nextPageDoc.html());
            if(newClp == null){
                newClp = clp;
                //Log.d(TAG, "Using old clp: " + token);
            }else{
                //Log.d(TAG, "Found new Clp: " + newClp);
            }


            /*Elements listNextMappedApps = nextPageDoc.select(".card");
            for(Element elApp : listNextMappedApps){
                apps.add(parseApp(elApp));
            }*/

            apps.addAll(Observable.from(nextPageDoc.select(".card"))
                    .map(new Func1<Element, App>() {
                        @Override
                        public App call(Element element) {
                            return parseApp(element);
                        }
                    }).toList().toBlocking().first());


            //LOGGER.debug("Found a list of: " + apps.size() + " apps");

            result.put("apps",apps);
            result.put("token",newToken);
            result.put("cpl",newClp);
            result.put("languageCode",languageCode);
            result.put("countryCode",countryCode);
            result.put("userAgent",userAgent);
        }else{

            result.put("apps",apps);
            result.put("token",token);
            result.put("cpl",clp);
            result.put("languageCode",languageCode);
            result.put("countryCode",countryCode);
            result.put("userAgent",userAgent);
        }
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
        String keyword = (String) params.get("keyword");
        String languageCode = (String) params.get("languageCode");
        String countryCode = (String) params.get("countryCode");
        int price = (int) params.get("price");
        String userAgent = (String) params.get("userAgent");
        String referer = (String) params.get("referer");
        String proxyHost = (String) params.get("proxyHost");
        int proxyPort = (int) params.get("proxyPort");

        Proxy proxy = null;
        if (proxyHost != null && proxyPort != -1) {
            // Setup proxy
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        }

        String url = "https://play.google.com/store/search?c=apps&q="
                + keyword + "&hl=" + languageCode + "&gl=" + countryCode + "&price=" + price;
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
    private Document skipClusterPage(Document doc,Map<String, Object> params) throws IOException {
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
        }else {
            //LOGGER.warn("NO cluster page...");
            return doc;
        }
    }


    private Document getNexPageDocument(String nextPageToken, String clp, String languageCode,
                                String countryCode, String userAgent, String referer, String proxyHost, int proxyPort) throws IOException {
        Map<String, Object> postParams = new LinkedHashMap<>();
        //postParams.put("num", appsSize == 49? 0 : 48);
        postParams.put("num", 49);
        //postParams.put("start", appsSize - 49);
        postParams.put("start", 0);
        postParams.put("pagTok", nextPageToken);
        postParams.put("clp", clp);
        postParams.put("pagtt", 3);
        postParams.put("hl", languageCode);
        postParams.put("gl", countryCode);

        String html = HttpUtils.ajaxPostGetHtml(postParams,userAgent, referer ,60*1000, proxyHost, proxyPort);
        return Jsoup.parse(html);
    }


    /**
     * extract the token for the next page request for the record
     *
     * @param html page to iterate
     * @return a token gor google-play pagination
     */
    String getNextPageToken(String html) {
        // const s = html.match(/\\42(GAE.+?)\\42/);
        Pattern pattern = Pattern.compile("\\\\x22-p6(.*?):S:(.*?)\\\\x22");
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            String token = matcher.group(0).replaceAll("\\\\\\\\u003d", "=").replaceAll("\\\\x22", "");
            return token;
        }
        return null;
    }


    /**
     * Try to find clp from "next page" html elem.
     * ... if we don't have it, we're probably on innerPage;
     * try to parse it from search_collection_more_results_cluster instead
     *
     * @param html the html page
     * @return clp
     */
    String getClp(String html) {

        //Pattern pattern1 = Pattern.compile("\\?clp=(.*?)\">");
        Pattern pattern1 = Pattern.compile("\\?clp=([^\"]*?)\">");
        Matcher matcher1 = pattern1.matcher(html);
        if (!matcher1.find()) {
            Pattern pattern2 = Pattern.compile("\\?clp\\\\x3d(.*?)';");
            Matcher matcher2 = pattern2.matcher(html);
            if (matcher2.find()) {
                return matcher2.group(1).replaceAll("%3D", "=");
            }
        } else {
            return matcher1.group(1).replaceAll("%3D", "=");
        }
        return null;
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

