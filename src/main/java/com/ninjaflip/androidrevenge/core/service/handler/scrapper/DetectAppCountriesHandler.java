package com.ninjaflip.androidrevenge.core.service.handler.scrapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ninjaflip.androidrevenge.beans.AppAvailableCountries;
import com.ninjaflip.androidrevenge.beans.RestrictedOpBean;
import com.ninjaflip.androidrevenge.beans.UserBean;
import com.ninjaflip.androidrevenge.core.db.dao.RestrictedOpDao;
import com.ninjaflip.androidrevenge.core.scrapper.manager.ScrappingQuotaManager;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.SocketMessagingProtocol;
import com.ninjaflip.androidrevenge.core.db.dao.AppAvailableCountriesDao;
import com.ninjaflip.androidrevenge.core.db.dao.UserDao;
import com.ninjaflip.androidrevenge.core.scrapper.ScrapperProcessBuilder;
import com.ninjaflip.androidrevenge.core.scrapper.UserAgents;
import com.ninjaflip.androidrevenge.core.scrapper.manager.DetectAppCountriesManager;
import com.ninjaflip.androidrevenge.enums.EnumerationScrapper;
import com.ninjaflip.androidrevenge.enums.RestrictedOpType;
import com.ninjaflip.androidrevenge.utils.Utils;
import com.ninjaflip.androidrevenge.websocket.EchoWebSocket;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import spark.Request;
import spark.Response;
import spark.Route;

import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Solitario on 13/11/2017.
 * <p>
 * This handler is responsible for detecting in which countries the app is available
 */
public class DetectAppCountriesHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(DetectAppCountriesHandler.class);
    private static final int MAX_APPS_COUNTRIES_PER_USER = 50;

    @Override
    public Object handle(Request request, Response response) {
        String userUuid = ServerSecurityManager.getInstance()
                .getUserUuidFromToken(request.cookie("token"));

        try {
            String action = request.queryParams("action");
            // check if action is valid
            if (action == null || action.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "You must provide an action parameter!";
                response.body(reason);
                return reason;
            }

            // business code
            switch (action) {
                case "GET_QUOTA_INFO": {
                    Map<String, String> templateVarValues = new HashMap<>();
                    templateVarValues.put("quota_description", ScrappingQuotaManager.QUOTA_DESCRIPTION_APP_COUNTRY);

                    JSONObject quotaInfo = ScrappingQuotaManager.getInstance().getAppCountryQuotaInfo(userUuid);
                    int remaining = quotaInfo.getAsNumber("remaining").intValue();

                    String remainingText;
                    String ttwText;
                    String date_end_quota;
                    if (remaining == 0) {
                        int ttw = (int) quotaInfo.getAsNumber("ttw").longValue();
                        remainingText = "It looks like <b><span style='color:#d53838;'>you have reached your Quota limit!</span></b>";
                        ttwText = "You have to wait for <b><span style='color:#d53838;'>"
                                + ScrappingQuotaManager.getInstance().timeToWaitAsString(ttw) + "</span></b> before submitting another request.";

                        date_end_quota = "<div style='text-align: center;margin: 20px;'>\n" +
                                "            <h1 id='ttw_countdown' data-nb-minutes-wait='"+ ttw +"' style='background: #d53838; color: #f3f3f3; padding: 10px; border-radius: 5px;'></h1>\n" +
                                "        </div>";
                    }else{
                        remainingText = "You still have <b><span style='color:#1d9e1d;'>" + remaining + " request(s)</span></b> for the current hour.";
                        ttwText = "You are free to send your requests anytime!";
                        date_end_quota = "";
                    }

                    templateVarValues.put("remaining_text", remainingText);
                    templateVarValues.put("ttw_text", ttwText);
                    templateVarValues.put("date_end_quota", date_end_quota);

                    String stringResponse = Utils.loadTemplateAsString("scrapper/app_countries",
                            "mst_tmpl_modal_quota_info", templateVarValues);
                    response.type("text/html; charset=utf-8");
                    response.status(200);
                    return stringResponse;
                }
                case "SCRAP_COUNTRIES": {
                    if (AppAvailableCountriesDao.getInstance().countItemsPerUser(userUuid) >= MAX_APPS_COUNTRIES_PER_USER) {
                        response.type("text/plain; charset=utf-8");
                        response.status(403);
                        String reason = "You can't store more than " + MAX_APPS_COUNTRIES_PER_USER + " APP_COUNTRIES result, please remove few ones then continue!";
                        response.body(reason);
                        return reason;
                    }


                    List<ScrapperProcessBuilder> processes = ScrapperProcessBuilder.getUserRunningProcesses(userUuid);
                    if (processes.size() > 0) { // check if there is an ongoing process => if exist ask user to wait until that process is finished
                        /*
                        503 service unavailable
                        The server is currently unable to handle the request due to a temporary overloading.
                        the source of the problem is an already running process from the same user, as the
                        server can process only one heavy process at once
                         */
                        response.type("text/plain; charset=utf-8");
                        response.status(503);
                        String currentProcessType = EnumerationScrapper.EnumProcessType.description(processes.get(0).getProcessType().getValue());
                        String reason = "'" + currentProcessType + "' is running, please wait until the current process is finished, or cancel it!";
                        response.body(reason);
                        return reason;
                    }

                    // check system clock is ok, because user may change clock to benefit overcome Quota limits
                    if(!ServerSecurityManager.getInstance().isClockOk()){
                        Utils.getNtpTime(3000); // refresh date
                        response.type("text/plain; charset=utf-8");
                        response.status(449);
                        String message = "Incorrect system date. Please check your system date and time settings and try again!";
                        response.body(message);
                        return message;
                    }

                    try {
                        JSONObject quotaInfo = ScrappingQuotaManager.getInstance().getAppCountryQuotaInfo(userUuid);
                        int remaining = quotaInfo.getAsNumber("remaining").intValue();
                        if (remaining == 0) {
                            int ttw = (int) quotaInfo.getAsNumber("ttw").longValue();
                            response.type("text/plain; charset=utf-8");
                            response.status(429);// too many requests
                            String reason = "You reached Quota limit of "
                                    + ScrappingQuotaManager.QUOTA_DESCRIPTION_APP_COUNTRY + ", you have to wait for "
                                    + ScrappingQuotaManager.getInstance().timeToWaitAsString(ttw);
                            response.body(reason);
                            return reason;
                        }
                    } catch (Exception e) {
                        // do nothing
                    }


                    Session session = EchoWebSocket.getUserSession(userUuid);
                    String appId = request.queryParams("appid_countries");
                    if (appId == null || appId.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide appId parameter (package name)!";
                        response.body(reason);
                        return reason;
                    }

                    if (appId.contains("https://play.google.com/store/apps/details")) {
                        //LOGGER.debug("App ID is an URL ");
                        Map<String, List<String>> map = Utils.splitQuery(new URL(appId));
                        appId = map.get("id").get(0);
                    } else {
                        //LOGGER.debug("AppID is a PACKAGE NAME");
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

                    final String appId_ = appId;
                    final String proxyHost_ = proxyHost;
                    final int proxyPort_ = proxyPort;
                    final ExecutorService pool = Executors.newFixedThreadPool(1);

                    ScrapperProcessBuilder countryScrapperProcessBuilder = new ScrapperProcessBuilder(userUuid, EnumerationScrapper.EnumProcessType.COUNTRY_PROGRESS) {

                        @Override
                        public void buildProcessLogic() {
                            try {
                                //LOGGER.debug("Start Countries Detector *******************************************");
                                long startTime = System.currentTimeMillis();
                                // output logging to the web-socket
                                if (session != null && session.isOpen()) {
                                    // notify process finished
                                    SocketMessagingProtocol.getInstance()
                                            .sendScrapperProcessState(session, this.getId(), EnumerationScrapper.EnumProcessType.COUNTRY_PROGRESS,
                                                    EnumerationScrapper.EnumProcessState.STARTED);
                                }

                                // Start scrapping countries
                                JSONObject nonAvailableCountries = DetectAppCountriesManager.getInstance()
                                        .scrapNonAvailableCountries(appId_, UserAgents.randomDesktop(), proxyHost_, proxyPort_, session, pool);

                                long endTime = System.currentTimeMillis();
                                double duration = (endTime - startTime) / (double) 1000;

                                List<String> nonAvailableCountriesList = (List<String>)nonAvailableCountries.get("listNonAvailableCountries");
                                if(nonAvailableCountriesList != null && nonAvailableCountriesList.size()>1){ // sort
                                    nonAvailableCountriesList.sort(String.CASE_INSENSITIVE_ORDER);
                                }

                                UserBean owner = UserDao.getInstance().getByUuid(userUuid);
                                AppAvailableCountries appAvailableCountries = new AppAvailableCountries(appId_, nonAvailableCountries.getAsString("title"),
                                        nonAvailableCountries.getAsString("developer"),
                                        nonAvailableCountries.getAsString("icon"),
                                        nonAvailableCountriesList, owner);
                                appAvailableCountries.setDuration(duration);
                                AppAvailableCountriesDao.getInstance().insert(appAvailableCountries);

                                // increment Quota
                                RestrictedOpDao.getInstance().insert(
                                        new RestrictedOpBean(RestrictedOpType.APP_COUNTRY, UserDao.getInstance().getByUuid(userUuid)));

                                if (session != null && session.isOpen()) {
                                    // notify process finished
                                    SocketMessagingProtocol.getInstance()
                                            .sendScrapperCompletedWithResultId(session, this.getId(),
                                                    appAvailableCountries.getUuid(), EnumerationScrapper.EnumProcessType.COUNTRY_PROGRESS);
                                }
                                //LOGGER.debug("Ended Countries Detector scrapping in " + duration + " seconds ********************");
                            } catch (Exception e) {
                                if (session != null && session.isOpen()) {
                                    if ((e instanceof org.jsoup.HttpStatusException || e instanceof java.util.concurrent.ExecutionException) && e.toString().contains("404")) {
                                        try {
                                            SocketMessagingProtocol.getInstance()
                                                    .sendScrapperProcessState(session, this.getId(), EnumerationScrapper.EnumProcessType.COUNTRY_PROGRESS,
                                                            EnumerationScrapper.EnumProcessState.ERROR, "Error 404 Resource not found!");
                                        } catch (Exception ex) {
                                            // do nothing
                                        }
                                    } else if(e instanceof java.net.ConnectException
                                            || e instanceof java.net.NoRouteToHostException
                                            || e instanceof java.net.UnknownHostException){
                                        try {
                                            SocketMessagingProtocol.getInstance()
                                                    .sendScrapperProcessState(session, this.getId(), EnumerationScrapper.EnumProcessType.SINGLE_APP_PROGRESS,
                                                            EnumerationScrapper.EnumProcessState.ERROR, "Connection problem, check your internet connection or firewall. If using proxy, check if proxy is OK!");
                                        } catch (Exception ex) {
                                            // do nothing
                                        }
                                    } else {
                                        try {
                                            SocketMessagingProtocol.getInstance()
                                                    .sendScrapperProcessState(session, this.getId(), EnumerationScrapper.EnumProcessType.COUNTRY_PROGRESS,
                                                            EnumerationScrapper.EnumProcessState.ERROR, e.getMessage());
                                        } catch (Exception ex) {
                                            // do nothing
                                        }
                                    }
                                }
                                String details = e.toString();
                                //LOGGER.error("error: " + details);
                                //e.printStackTrace();
                            }
                        }

                        @Override
                        public void cleanOperation() {
                            try {
                                System.out.println("attempt to shutdown App countries scrapper");
                                pool.shutdown();
                                // wait 500 milliseconds seconds at max then force shutdown
                                pool.awaitTermination(500, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException e1) {
                                System.err.println("App countries scrapper => tasks interrupted");
                            } finally {
                                if (!pool.isTerminated()) {
                                    System.err.println("App countries scrapper => cancel non-finished tasks");
                                }
                                pool.shutdownNow();
                            }
                        }
                    };
                    countryScrapperProcessBuilder.execute();
                    response.type("text/plain; charset=utf-8");
                    response.status(200);
                    return countryScrapperProcessBuilder.getId();
                }
                case "GET_COUNTRIES_RESULT": {
                    //resultUuid
                    String resultUuid = request.queryParams("resultUuid");
                    // check if resultUuid is valid
                    if (resultUuid == null || resultUuid.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a resultUuid parameter!";
                        response.body(reason);
                        return reason;
                    }

                    AppAvailableCountries appAvailableCountries = AppAvailableCountriesDao.getInstance().getByUuid(resultUuid);

                    if (appAvailableCountries == null) {
                        response.type("text/plain; charset=utf-8");
                        response.status(404);
                        String reason = "This item was not found in AppCountries table!";
                        response.body(reason);
                        return reason;
                    }

                    // hide owner data
                    appAvailableCountries.setOwner(null);

                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                    String resultJsonAsString = gson.toJson(appAvailableCountries);


                    response.type("application/json; charset=UTF-8");
                    response.status(200);
                    return resultJsonAsString;
                }
                case "GET_ALL": {
                    long startTime = System.currentTimeMillis();
                    List<AppAvailableCountries> appAvailableCountries = AppAvailableCountriesDao.getInstance().getAll(userUuid);
                    // hide owner data
                    for(AppAvailableCountries aac: appAvailableCountries){
                        aac.setOwner(null);
                    }

                    // serialize
                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                    String appAvailableCountriesJsonAsString = gson.toJson(appAvailableCountries);
                    // send response
                    long endTime = System.currentTimeMillis();
                    double duration = (endTime - startTime) / (double) 1000;
                    //LOGGER.debug("Got apps countries list in " + duration + " seconds");
                    response.type("application/json; charset=UTF-8");
                    response.status(200);
                    return appAvailableCountriesJsonAsString;
                }
                case "REMOVE_RESULT": {
                    //uuid
                    String uuid = request.queryParams("uuid");
                    // check if uuid is valid
                    if (uuid == null || uuid.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a uuid parameter!";
                        response.body(reason);
                        return reason;
                    }

                    AppAvailableCountries record = AppAvailableCountriesDao.getInstance().getByUuid(uuid);
                    if(record != null) {
                        AppAvailableCountriesDao.getInstance().delete(record);
                    }
                    response.type("text/plain; charset=utf-8");
                    response.status(204);
                    return "";
                }
                case "CANCEL_PROCESS": {
                    String processId = request.queryParams("processId");
                    // check if processId is valid
                    if (processId == null || processId.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a processId parameter!";
                        response.body(reason);
                        return reason;
                    }else{
                        try {
                            ScrapperProcessBuilder.cancelRunningProcess(processId, userUuid);
                            response.status(204);
                            return "";
                        } catch (IllegalAccessException e) {
                            response.type("text/plain; charset=utf-8");
                            response.status(401);
                            response.body(e.getMessage());
                            return e.getMessage();
                        }
                    }
                }
                default: {
                    response.type("text/plain; charset=utf-8");
                    response.status(400);
                    String reason = "Unknown countries action parameter!";
                    response.body(reason);
                    return reason;
                }
            }
        } catch (Exception e) {
            String details = e.getMessage();
            //LOGGER.error("error: " + details);
            //e.printStackTrace();
            response.type("text/plain; charset=utf-8");
            response.status(500);
            response.body(details);
            return details;
        }
    }
}
