package com.ninjaflip.androidrevenge.core.service.handler.scrapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ninjaflip.androidrevenge.core.scrapper.UserAgents;
import com.ninjaflip.androidrevenge.core.scrapper.manager.KeywordScrapManager;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.jsoup.HttpStatusException;
import spark.Request;
import spark.Response;
import spark.Route;

import java.net.ConnectException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Solitario on 29/07/2017.
 * <p>
 * Keyword search scrapping handler
 */
public class KeywordSearchRequestHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(KeywordSearchRequestHandler.class);
    private static Map<String, JSONObject> paginationIndex = new HashMap<>();

    @Override
    public Object handle(Request request, Response response) {
        try {
            String action = request.queryParams("action");
            if (action == null)
                throw new RuntimeException("Null action parameter!");

            if (action.equals("FIRST_PAGE")) {
                Utils.getNtpTime();

                String keyword = URLDecoder.decode(request.queryParams("scrap_keyword"), "UTF-8");
                String language = URLDecoder.decode(request.queryParams("scrap_language_select"), "UTF-8");
                String country = URLDecoder.decode(request.queryParams("scrap_country_select"), "UTF-8");

                String proxyHost = null;
                if (request.queryParams("proxy_host") != null) {
                    proxyHost = URLDecoder.decode(request.queryParams("proxy_host"), "UTF-8");
                }

                int proxyPort = -1;
                if (request.queryParams("proxy_port") != null) {
                    proxyPort = Integer.valueOf(URLDecoder.decode(request.queryParams("proxy_port"), "UTF-8"));
                }

                int price = 1;
                if (request.queryParams("appprice") != null) {
                    price = Integer.valueOf(URLDecoder.decode(request.queryParams("appprice"), "UTF-8"));
                }

                String userAgent = UserAgents.randomDesktop();
                /*LOGGER.debug("Parameter : (keyword, " + keyword + "), (language," + language +
                        "), (country," + country + "), (proxy : " + proxyHost + ":" + proxyPort + "), UA: " + userAgent);*/


                if (keyword == null || "".equals(keyword)) {
                    throw new RuntimeException("Search term missing!");
                }
                if (language == null || "".equals(language)) {
                    throw new RuntimeException("Bad language parameter");
                }
                if (country == null || "".equals(country)) {
                    throw new RuntimeException("Bad country parameter");
                }
                long startTime = System.currentTimeMillis();
                // Start scrapping logic
                JSONObject result = new JSONObject();
                //LOGGER.debug("Start search by keyword scrapping *************************************");
                JSONObject appList = KeywordScrapManager.getInstance().scrapKeywordSearch(keyword, language, country, price,
                        userAgent, proxyHost, proxyPort);
                result.put("apps", appList.get("apps"));
                if (appList.get("token") != null) {
                    JSONObject pagination = new JSONObject();
                    pagination.put("token", appList.get("token"));
                    pagination.put("cpl", appList.get("cpl"));
                    pagination.put("languageCode", appList.get("languageCode"));
                    pagination.put("countryCode", appList.get("countryCode"));
                    pagination.put("userAgent", appList.get("userAgent"));

                    String key = UUID.randomUUID().toString();
                    paginationIndex.put(key, pagination);
                    result.put("pagination", key);
                }

                long endTime = System.currentTimeMillis();
                double duration = (endTime - startTime) / (double) 1000;
                //LOGGER.debug("End search by keyword scrapping *************************************");
                result.put("duration", duration);

                // serialize
                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                String resultAppListJsonAsString = gson.toJson(result);

                // display result on the console
                //Utils.logLongString(LOGGER, "Result length = " + resultAppListJsonAsString.length() + " , result = " + resultAppListJsonAsString);

                // send response
                response.type("application/json; charset=UTF-8");
                response.status(200);
                return resultAppListJsonAsString;

            } else if (action.equals("NEXT_PAGE")) {
                String pagination = null;
                if (request.queryParams("pagination") != null) {
                    pagination = URLDecoder.decode(request.queryParams("pagination"), "UTF-8");
                }

                String proxyHost = null;
                if (request.queryParams("proxy_host") != null && !request.queryParams("proxy_host").equals("")) {
                    proxyHost = URLDecoder.decode(request.queryParams("proxy_host"), "UTF-8");
                }

                int proxyPort = -1;
                if (request.queryParams("proxy_port") != null && !request.queryParams("proxy_port").equals("")) {
                    proxyPort = Integer.valueOf(URLDecoder.decode(request.queryParams("proxy_port"), "UTF-8"));
                }

                long startTime = System.currentTimeMillis();
                JSONObject result = new JSONObject();

                JSONObject page = paginationIndex.get(pagination);
                if (page == null) {
                    throw new RuntimeException("Invalid page!");
                }

                JSONObject appList = KeywordScrapManager.getInstance().
                        paginationRequest((String) page.get("token"), (String) page.get("cpl"), (String) page.get("languageCode"),
                                (String) page.get("countryCode"), (String) page.get("userAgent"), proxyHost, proxyPort);

                result.put("apps", appList.get("apps"));

                if (appList.get("token") != null) {
                    JSONObject newPagination = new JSONObject();
                    newPagination.put("token", appList.get("token"));
                    newPagination.put("cpl", appList.get("cpl"));
                    newPagination.put("languageCode", appList.get("languageCode"));
                    newPagination.put("countryCode", appList.get("countryCode"));
                    newPagination.put("userAgent", appList.get("userAgent"));

                    String key = UUID.randomUUID().toString();
                    paginationIndex.put(key, newPagination);
                    result.put("pagination", key);
                }

                long endTime = System.currentTimeMillis();
                double duration = (endTime - startTime) / (double) 1000;
                //LOGGER.debug("End search by keyword scrapping *************************************");
                result.put("duration", duration);

                // serialize
                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                String resultAppListNextPageJsonAsString = gson.toJson(result);

                // display result on the console
                //Utils.logLongString(LOGGER, "Result length = " + resultAppListNextPageJsonAsString.length() + " , result = " + resultAppListNextPageJsonAsString);

                // send response
                // send response
                response.type("application/json; charset=utf-8");
                response.status(200);
                paginationIndex.remove(pagination);
                return resultAppListNextPageJsonAsString;

            } else {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                response.body("Action: '" + action + "' is not a valid parameter");
                return "";
            }
        } catch (HttpStatusException e) {
            //LOGGER.error(e.getMessage());
            //e.printStackTrace();
            String details;
            if(e.toString().contains("404")){
                details = "Server responded with 404 error, resource not found!";
            }else{
                details = e.getMessage() + ", Status: " + e.getStatusCode() + ", URL: " + e.getUrl();
            }
            response.type("text/plain; charset=utf-8");
            response.status(e.getStatusCode());
            response.body(details);
            return details;
        } catch (Exception e) {
            //LOGGER.error(e.getMessage());
            //e.printStackTrace();
            String details;
            int status;
            if(e instanceof java.net.ConnectException
                    || e instanceof java.net.NoRouteToHostException
                    || e instanceof java.net.UnknownHostException){
                details = "Connection problem, check your internet connection or firewall. If using proxy, check if proxy is OK!";
                status =404;
            }else{
                details = e.getMessage();
                status =500;
            }
            response.type("text/plain; charset=utf-8");
            response.status(status);
            response.body(details);
            return details;
        }
    }
}
