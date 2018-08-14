package com.ninjaflip.androidrevenge.core.service.handler.scrapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ninjaflip.androidrevenge.core.scrapper.UserAgents;
import com.ninjaflip.androidrevenge.core.scrapper.manager.TopAppsScrapManager;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.jsoup.HttpStatusException;
import spark.Request;
import spark.Response;
import spark.Route;

import java.net.URLDecoder;

/**
 * Created by Solitario on 09/11/2017.
 * <p>
 * This Handler responsible for top apps
 */
public class TopAppsHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(TopAppsHandler.class);

    @Override
    public Object handle(Request request, Response response) {
        try {
            String action = request.queryParams("action");
            if (action == null || action.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "You must provide action parameter!";
                response.body(reason);
                return reason;
            }

            switch (action) {
                case "FIRST_PAGE": {
                    String language = request.queryParams("language");
                    if (language == null || language.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide language parameter!";
                        response.body(reason);
                        return reason;
                    }
                    String country = request.queryParams("country");
                    if (country == null || country.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide country parameter!";
                        response.body(reason);
                        return reason;
                    }
                    String collection = request.queryParams("collection");
                    if (collection == null || collection.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide collection parameter!";
                        response.body(reason);
                        return reason;
                    }
                    String category = request.queryParams("category");
                    if (category == null || category.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide category parameter!";
                        response.body(reason);
                        return reason;
                    }

                    String proxyHost = null;
                    if (request.queryParams("proxy_host") != null) {
                        proxyHost = URLDecoder.decode(request.queryParams("proxy_host"), "UTF-8");
                    }

                    int proxyPort = -1;
                    if (request.queryParams("proxy_port") != null) {
                        proxyPort = Integer.valueOf(URLDecoder.decode(request.queryParams("proxy_port"), "UTF-8"));
                    }

                    String userAgent = UserAgents.randomDesktop();
                    /*LOGGER.debug("Parameter : (language," + language + "), (country," + country + "), (collection," + collection
                            + "), (category," + category + "), (proxy : " + proxyHost + ":" + proxyPort + "), UA: " + userAgent);*/

                    long startTime = System.currentTimeMillis();
                    // Start top apps scrapping logic
                    JSONObject result = new JSONObject();
                    //LOGGER.debug("Start top apps FIRST_PAGE scrapping *************************************");

                    JSONObject appList = TopAppsScrapManager.getInstance().scrapTopAppsFirstPage(language, country, collection, category,
                            userAgent, proxyHost, proxyPort);
                    result.put("apps", appList.get("apps"));

                    if (appList.get("pagination") != null) {
                        boolean pagination = (boolean) appList.get("pagination");
                        if (pagination) {
                            result.put("pagination", true);
                            result.put("nextPageNumber", 1);
                            result.put("language", language);
                            result.put("country", country);
                            result.put("collection", collection);
                            result.put("category", category);
                        } else {
                            result.put("pagination", false);
                        }
                    }

                    long endTime = System.currentTimeMillis();
                    double duration = (endTime - startTime) / (double) 1000;
                    //LOGGER.debug("End top apps FIRST_PAGE scrapping *************************************");
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
                }
                case "LOAD_MORE": {
                    String nextPage = request.queryParams("nextPage");
                    if (nextPage == null || nextPage.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide NextPage parameter!";
                        response.body(reason);
                        return reason;
                    }
                    int nexPageInt;
                    try {
                        nexPageInt = Integer.valueOf(nextPage);
                    } catch (Exception exep) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = nextPage + " is a bad NexPage parameter!";
                        response.body(reason);
                        return reason;
                    }

                    String language = request.queryParams("language");
                    if (language == null || language.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide language parameter!";
                        response.body(reason);
                        return reason;
                    }
                    String country = request.queryParams("country");
                    if (country == null || country.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide country parameter!";
                        response.body(reason);
                        return reason;
                    }
                    String collection = request.queryParams("collection");
                    if (collection == null || collection.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide collection parameter!";
                        response.body(reason);
                        return reason;
                    }
                    String category = request.queryParams("category");
                    if (category == null || category.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide category parameter!";
                        response.body(reason);
                        return reason;
                    }

                    String proxyHost = null;
                    if (request.queryParams("proxy_host") != null) {
                        try {
                            proxyHost = URLDecoder.decode(request.queryParams("proxy_host"), "UTF-8");
                        } catch (Exception e) {
                            // do nothing
                        }
                    }

                    int proxyPort = -1;
                    if (request.queryParams("proxy_port") != null) {
                        try {
                            proxyPort = Integer.valueOf(URLDecoder.decode(request.queryParams("proxy_port"), "UTF-8"));
                        } catch (Exception e) {
                            // do nothing
                        }
                    }

                    String userAgent = UserAgents.randomDesktop();
                    /*LOGGER.debug("Parameter : (language," + language + "), (country," + country + "), (collection," + collection
                            + "), (category," + category + "), (proxy : " + proxyHost + ":" + proxyPort + "), UA: " + userAgent);*/

                    long startTime = System.currentTimeMillis();
                    // Start top apps scrapping logic
                    JSONObject result = new JSONObject();
                    //LOGGER.debug("Start top apps NEXT_PAGE scrapping *************************************");

                    JSONObject appList = TopAppsScrapManager.getInstance().scrapTopAppsNextPage(nexPageInt, language, country, collection, category,
                            userAgent, proxyHost, proxyPort);
                    result.put("apps", appList.get("apps"));

                    if (appList.get("pagination") != null) {
                        boolean pagination = (boolean) appList.get("pagination");
                        if (pagination) {
                            result.put("pagination", true);
                            result.put("nextPageNumber", nexPageInt + 1);
                            result.put("language", language);
                            result.put("country", country);
                            result.put("collection", collection);
                            result.put("category", category);
                        } else {
                            result.put("pagination", false);
                        }
                    }

                    long endTime = System.currentTimeMillis();
                    double duration = (endTime - startTime) / (double) 1000;
                    //LOGGER.debug("End top apps NEXT_PAGE scrapping *************************************");
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
                }
                default: {
                    response.type("text/plain; charset=utf-8");
                    response.status(400);
                    String reason = "Unknown tools action parameter!";
                    response.body(reason);
                    return reason;
                }
            }
        } catch (HttpStatusException e) {
            //String details = e.getMessage() + ", Status: " + e.getStatusCode() + ", URL: " + e.getUrl();
            String details = "Oops , something went wrong!";
            //LOGGER.error("error: " + details);
            //e.printStackTrace();
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
