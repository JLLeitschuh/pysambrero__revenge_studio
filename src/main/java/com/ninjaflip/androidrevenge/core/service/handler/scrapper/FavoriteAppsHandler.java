package com.ninjaflip.androidrevenge.core.service.handler.scrapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ninjaflip.androidrevenge.beans.FavoriteAppBean;
import com.ninjaflip.androidrevenge.beans.UserBean;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.db.dao.FavoriteAppDao;
import com.ninjaflip.androidrevenge.core.db.dao.UserDao;
import com.ninjaflip.androidrevenge.core.scrapper.UserAgents;
import com.ninjaflip.androidrevenge.core.scrapper.manager.AppScrappingManager;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Solitario on 12/11/2017.
 * <p>
 * a handler for favorite apps operations: add, remove, getAll
 */
public class FavoriteAppsHandler implements Route {

    private final static Logger LOGGER = Logger.getLogger(FavoriteAppsHandler.class);
    private static final int MAX_FAVORITE_APPS_PER_USER = 100;

    @Override
    public Object handle(Request request, Response response) {
        String userUuid = ServerSecurityManager.getInstance()
                .getUserUuidFromToken(request.cookie("token"));

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
                case "GET_ALL": {
                    long startTime = System.currentTimeMillis();
                    List<FavoriteAppBean> favoriteApps = FavoriteAppDao.getInstance().getAll(userUuid);
                    // group by app created on the same day

                    Map<String, ArrayList<FavoriteAppBean>> timeline = new HashMap<>();
                    for(FavoriteAppBean fa : favoriteApps){
                        // hide user data (password...)
                        fa.hideSensibleData();
                        ArrayList<FavoriteAppBean> list = timeline.get(fa.getFormattedDateCreatedAsString());
                        if(list == null){
                            list = new ArrayList<>();
                            list.add(fa);
                            timeline.put(fa.getFormattedDateCreatedAsString(), list);
                        }else{
                            list.add(fa);
                        }
                    }

                    // serialize
                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                    String favoriteAppsJsonAsString = gson.toJson(timeline);
                    // send response
                    long endTime = System.currentTimeMillis();
                    double duration = (endTime - startTime) / (double) 1000;
                    //LOGGER.debug("Got all favorite apps in " + duration + " seconds");
                    response.type("application/json; charset=UTF-8");
                    response.status(200);
                    return favoriteAppsJsonAsString;
                }
                case "ADD": {
                    // no more than MAX_FAVORITE_APPS_PER_USER apps
                    if (FavoriteAppDao.getInstance().countFavoriteAppsPerUser(userUuid) < MAX_FAVORITE_APPS_PER_USER) {
                        String appId = request.queryParams("appId");
                        if (appId == null || appId.equals("")) {
                            response.type("text/plain; charset=utf-8");
                            response.status(400);
                            String reason = "You must provide appId parameter (package name)!";
                            response.body(reason);
                            return reason;
                        }
                        // check if already exist
                        if (FavoriteAppDao.getInstance().favoriteAppExistsAndBelongsToUser(appId, userUuid)) {
                            response.type("text/plain; charset=utf-8");
                            response.status(409);
                            String reason = "Already exists in your favorite Apps!";
                            response.body(reason);
                            return reason;
                        } else {
                            // logic here (scrap app page and add it to database)
                            String language = "en-US";
                            String countryCode = "us"; // canada

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

                            long startTime = System.currentTimeMillis();
                            //LOGGER.debug("Started mono-language scrapping for " + appId + " *****************************");
                            // Start scrapping
                            JSONObject monoLanguageResult = AppScrappingManager.getInstance()
                                    .scrapMonoLanguageApp(appId, language, countryCode, UserAgents.randomDesktop(), proxyHost, proxyPort);


                            UserBean owner = UserDao.getInstance().getByUuid(userUuid);
                            // save bean
                            FavoriteAppBean newFavoriteApp = new FavoriteAppBean(appId, monoLanguageResult.getAsString("title"),
                                    monoLanguageResult.getAsString("developer"), monoLanguageResult.getAsString("shortDesc"),
                                    monoLanguageResult.getAsString("icon"), Float.valueOf(monoLanguageResult.getAsString("score")),
                                    monoLanguageResult.getAsString("price"), monoLanguageResult.getAsString("price").equals("0"), owner);

                            FavoriteAppDao.getInstance().insert(newFavoriteApp);



                            JSONObject resultJsonObject = new JSONObject();
                            resultJsonObject.put("appId", newFavoriteApp.getAppId());
                            resultJsonObject.put("title", newFavoriteApp.getName());
                            resultJsonObject.put("developer", newFavoriteApp.getDeveloper());
                            resultJsonObject.put("rating", newFavoriteApp.getRating());
                            resultJsonObject.put("shortDesc", newFavoriteApp.getShortDesc());
                            resultJsonObject.put("price", newFavoriteApp.getPrice());
                            resultJsonObject.put("isFree", newFavoriteApp.isFree());
                            resultJsonObject.put("icon", newFavoriteApp.getIcon());
                            resultJsonObject.put("dataCreatedFormatted", newFavoriteApp.getFormattedDateCreatedAsString());



                            long endTime = System.currentTimeMillis();
                            double duration = (endTime - startTime) / (double) 1000;
                            //LOGGER.debug("Ended mono-language scrapping in " + duration + " seconds ********************");

                            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                            String resultJsonAsString = gson.toJson(resultJsonObject);

                            response.type("text/plain; charset=utf-8");
                            response.status(200);
                            return resultJsonAsString;
                        }
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(403);
                        String reason = "You can't store more than " + MAX_FAVORITE_APPS_PER_USER + " FAVORITE app, please remove few ones then continue!";
                        response.body(reason);
                        return reason;
                    }
                }
                case "REMOVE": {
                    String appId = request.queryParams("appId");
                    if (appId == null || appId.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide appId parameter (package name)!";
                        response.body(reason);
                        return reason;
                    }
                    // check if already exist
                    FavoriteAppBean favoriteApp = FavoriteAppDao.getInstance().getByAppId(appId);
                    if (favoriteApp != null) {
                        FavoriteAppDao.getInstance().delete(favoriteApp);
                    }
                    response.type("text/plain; charset=utf-8");
                    response.status(204);
                    return "";
                }
                default: {
                    response.type("text/plain; charset=utf-8");
                    response.status(400);
                    String reason = "Unknown action parameter for favorite apps!";
                    response.body(reason);
                    return reason;
                }
            }
        } catch (Exception e) {
            //LOGGER.error("error: " + e.getMessage());
            //e.printStackTrace();
            response.type("text/plain; charset=utf-8");
            response.status(500);
            response.body(e.getMessage());
            return e.getMessage();
        }
    }
}
