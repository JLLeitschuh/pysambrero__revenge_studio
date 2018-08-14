package com.ninjaflip.androidrevenge.core.service.handler.scrapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ninjaflip.androidrevenge.beans.RestrictedOpBean;
import com.ninjaflip.androidrevenge.beans.UserBean;
import com.ninjaflip.androidrevenge.beans.containers.FixedSizeLinkedHashMap;
import com.ninjaflip.androidrevenge.core.db.dao.RestrictedOpDao;
import com.ninjaflip.androidrevenge.core.db.dao.UserDao;
import com.ninjaflip.androidrevenge.core.scrapper.manager.ScrappingQuotaManager;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.SocketMessagingProtocol;
import com.ninjaflip.androidrevenge.core.scrapper.ScrapperProcessBuilder;
import com.ninjaflip.androidrevenge.enums.EnumerationScrapper;
import com.ninjaflip.androidrevenge.enums.EnumerationScrapperSpeed;
import com.ninjaflip.androidrevenge.core.scrapper.UserAgents;
import com.ninjaflip.androidrevenge.core.scrapper.manager.AppScrappingManager;
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
 * Created by Solitario on 29/07/2017.
 *
 * Single App Scrapping handler
 */
public class AppScrappingRequestHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(AppScrappingRequestHandler.class);
    private static FixedSizeLinkedHashMap<String, JSONObject> scrappingResultsCache = new FixedSizeLinkedHashMap<>(5);

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
                    templateVarValues.put("quota_description", ScrappingQuotaManager.QUOTA_DESCRIPTION_APP_DETAILS);

                    JSONObject quotaInfo = ScrappingQuotaManager.getInstance().getAppDetailsQuotaInfo(userUuid);
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

                    String stringResponse = Utils.loadTemplateAsString("scrapper/single_app_details",
                            "mst_tmpl_modal_quota_info", templateVarValues);
                    response.type("text/html; charset=utf-8");
                    response.status(200);
                    return stringResponse;
                }
                case "SINGLE_APP_SCRAPPER": {
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
                        JSONObject quotaInfo = ScrappingQuotaManager.getInstance().getAppDetailsQuotaInfo(userUuid);
                        int remaining = quotaInfo.getAsNumber("remaining").intValue();
                        if (remaining == 0) {
                            int ttw = (int) quotaInfo.getAsNumber("ttw").longValue();
                            response.type("text/plain; charset=utf-8");
                            response.status(429);// too many requests
                            String reason = "You reached Quota limit of "
                                    + ScrappingQuotaManager.QUOTA_DESCRIPTION_APP_DETAILS + ", you have to wait for "
                                    + ScrappingQuotaManager.getInstance().timeToWaitAsString(ttw);
                            response.body(reason);
                            return reason;
                        }
                    } catch (Exception e) {
                        // do nothing
                    }


                    Session session = EchoWebSocket.getUserSession(userUuid);
                    String appId = request.queryParams("appId-singleapp");
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
                        //LOGGER.debug("AppID is PACKAGE NAME");
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


                    int nbThreads = 4;
                    int availableCores = Runtime.getRuntime().availableProcessors();
                    if(availableCores < 4){
                        nbThreads = availableCores;
                    }
                    final ExecutorService pool = Executors.newFixedThreadPool(nbThreads);

                    ScrapperProcessBuilder appDetailsScrapperProcessBuilder = new ScrapperProcessBuilder(userUuid, EnumerationScrapper.EnumProcessType.SINGLE_APP_PROGRESS) {

                        @Override
                        public void buildProcessLogic() {
                            try {
                                //LOGGER.debug("Start multi-language scrapping *******************************************");
                                long startTime = System.currentTimeMillis();
                                // output logging to the web-socket
                                if (session != null && session.isOpen()) {
                                    // notify process finished
                                    SocketMessagingProtocol.getInstance()
                                            .sendScrapperProcessState(session, this.getId(), EnumerationScrapper.EnumProcessType.SINGLE_APP_PROGRESS,
                                                    EnumerationScrapper.EnumProcessState.STARTED);
                                }

                                EnumerationScrapperSpeed.ScrapperSpeed scrapperSpeed = EnumerationScrapperSpeed.ScrapperSpeed.MEDIUM;

                                // Start scrapping app details
                                JSONObject multiLanguageResult = AppScrappingManager.getInstance()
                                        .scrapMultiLanguageApp(appId_, UserAgents.randomDesktop(), proxyHost_, proxyPort_, scrapperSpeed, session, pool);
                                long endTime = System.currentTimeMillis();
                                double duration = (endTime - startTime) / (double) 1000;

                                JSONObject result = new JSONObject();
                                result.put("type", "multi");
                                result.put("app_id", appId_);
                                result.put("content", multiLanguageResult);

                                JSONObject cachedJsonObject = new JSONObject();
                                cachedJsonObject.put("duration", duration);
                                cachedJsonObject.put("result", result);

                                // cache result
                                scrappingResultsCache.put(this.getId(), cachedJsonObject);

                                // increment Quota
                                RestrictedOpDao.getInstance().insert(
                                        new RestrictedOpBean(RestrictedOpType.APP_DETAILS, UserDao.getInstance().getByUuid(userUuid)));


                                if (session != null && session.isOpen()) {
                                    // notify process finished
                                    SocketMessagingProtocol.getInstance()
                                            .sendScrapperProcessState(session, this.getId(),
                                                    EnumerationScrapper.EnumProcessType.SINGLE_APP_PROGRESS,
                                                    EnumerationScrapper.EnumProcessState.COMPLETED);
                                }
                                //LOGGER.debug("Ended multi-language scrapping in " + duration + " seconds ********************");
                            } catch (Exception e) {
                                if (session != null && session.isOpen()) {
                                    if ((e instanceof org.jsoup.HttpStatusException || e instanceof java.util.concurrent.ExecutionException) && e.toString().contains("404")) {
                                        try {
                                            SocketMessagingProtocol.getInstance()
                                                    .sendScrapperProcessState(session, this.getId(), EnumerationScrapper.EnumProcessType.SINGLE_APP_PROGRESS,
                                                            EnumerationScrapper.EnumProcessState.ERROR, "This app does not exist in the store!");
                                        } catch (Exception ex) {
                                            // do nothing
                                        }
                                    }else if(e instanceof java.net.ConnectException
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
                                                    .sendScrapperProcessState(session, this.getId(), EnumerationScrapper.EnumProcessType.SINGLE_APP_PROGRESS,
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
                                System.out.println("attempt to shutdown Single App scrapper");
                                pool.shutdown();
                                // wait 500 milliseconds at max then force shutdown
                                pool.awaitTermination(500, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException e1) {
                                System.err.println("Single App scrapper => tasks interrupted");
                            } finally {
                                if (!pool.isTerminated()) {
                                    System.err.println("Single App scrapper => cancel non-finished tasks");
                                }
                                pool.shutdownNow();
                            }
                        }
                    };
                    appDetailsScrapperProcessBuilder.execute();
                    response.type("text/plain; charset=utf-8");
                    response.status(200);
                    return appDetailsScrapperProcessBuilder.getId();


                }
                case "GET_SINGLE_APP_RESULT": {
                    //processId
                    String processId = request.queryParams("processId");
                    // check if processId is valid
                    if (processId == null || processId.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a processId parameter!";
                        response.body(reason);
                        return reason;
                    }
                    JSONObject cachedJsonObject = scrappingResultsCache.get(processId);
                    if (cachedJsonObject == null) {
                        response.type("text/plain; charset=utf-8");
                        response.status(404);
                        String reason = "This item was not found in Single App Search results!";
                        response.body(reason);
                        return reason;
                    }

                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                    String cachedJsonAsString = gson.toJson(cachedJsonObject);

                    scrappingResultsCache.remove(processId);

                    response.type("application/json; charset=UTF-8");
                    response.status(200);
                    return cachedJsonAsString;
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
                    String reason = "Unknown AppDetails action parameter!";
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
