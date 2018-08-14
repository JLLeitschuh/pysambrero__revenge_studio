package com.ninjaflip.androidrevenge.core.service.handler;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.core.security.CryptoCurrencyPeriodicChecker;
import com.ninjaflip.androidrevenge.core.security.LicensePeriodicChecker;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.utils.Utils;
import com.ninjaflip.androidrevenge.websocket.EchoWebSocket;
import com.x5.template.Chunk;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Created by Solitario on 17/07/2017.
 * <p>
 * Load index page handler
 */
public class IndexPageHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(IndexPageHandler.class);

    @Override
    public Object handle(Request request, Response response) throws Exception {
        try {
            String token = request.cookie("token");
            if (token == null) {
                //Spark.halt(400, "null credentials.....!");
                response.header("REQUIRES_AUTH","1");
                response.redirect("/api/public/signin");
                Spark.halt();
                return "";
            } else {
                String userUuid = ServerSecurityManager.getInstance()
                        .getUserUuidFromToken(request.cookie("token"));
                /*
                 * check if current user already has an opened session
                 * if so, redirect him to error page, bacause a user can only open one dashboard page.
                 * This constrain is imposed by websocket uniqueness
                 */
                if (userUuid != null) {
                    Session oldSession = EchoWebSocket.getUserSession(userUuid);
                    if (oldSession != null) {
                        /*
                        LOGGER.debug("getLocalAddress " + oldSession.getLocalAddress());
                        LOGGER.debug("getHostName " + oldSession.getLocalAddress().getHostName());
                        LOGGER.debug("getPort " + oldSession.getLocalAddress().getPort());
                        LOGGER.debug("getHostString " + oldSession.getLocalAddress().getHostString());
                        LOGGER.debug("getAddress " + oldSession.getLocalAddress().getAddress());
                        LOGGER.debug("getCanonicalHostName " + oldSession.getLocalAddress().getAddress().getCanonicalHostName());
                        LOGGER.debug("getHostAddress " + oldSession.getLocalAddress().getAddress().getHostAddress());
                        LOGGER.debug(">>getHostName " + oldSession.getLocalAddress().getAddress().getHostName());
                        LOGGER.debug("----");
                        LOGGER.debug("----");

                        LOGGER.debug("getLocalAddress " + oldSession.getLocalAddress());
                        LOGGER.debug("getRemote " + oldSession.getRemote());
                        LOGGER.debug("getRemote " + oldSession.getRemote());
                        LOGGER.debug("toString " + oldSession.toString());

                        LOGGER.debug("request host " + request.host());
                        LOGGER.debug("request scheme " + request.scheme());
                        LOGGER.debug("request url " + request.url());
                        LOGGER.debug("request pathInfo " + request.pathInfo());
                        LOGGER.debug("request ip " + request.ip());
                        LOGGER.debug("request ip " + request.ip());
                        */

                        if (oldSession.isOpen()) {
                            boolean sameHost;
                            String wsHostName = oldSession.getLocalAddress().getHostName();
                            String reqHostName = request.ip();
                            if (wsHostName.equals("0:0:0:0:0:0:0:1") && request.ip().equals("127.0.0.1")) {
                                sameHost = true;
                            } else {
                                SocketAddress wsAdr = new InetSocketAddress(wsHostName, oldSession.getLocalAddress().getPort());
                                SocketAddress reqAdr = new InetSocketAddress(reqHostName, request.port());
                                sameHost = wsAdr.equals(reqAdr);
                            }
                            if (sameHost) {
                                //Spark.halt(403, "You cannot open multiple Index pages!");
                                URL url = IndexPageHandler.class.getResource("/www/static/public/html/error_page.html");

                                InputStream stream = url.openStream();
                                // Return a String which has all
                                // the contents of the file.
                                String htmlContent = new String(Utils.readBytesFromStream(stream), "UTF-8");
                                stream.close();

                                Chunk html = new Chunk();
                                html.append(htmlContent);

                                html.setErrorHandling(true, System.err);
                                html.set("error", "Oops! You can't open more than one Dashboard page.");
                                return html.toString();
                            }
                        } else {
                            EchoWebSocket.removeSession(oldSession);
                        }
                    }
                }

                // gt html from resource template file
                URL url = getClass().getResource("/www/my_index.html");
                InputStream stream = url.openStream();
                String htmlContent = new String(Utils.readBytesFromStream(stream), "UTF-8");
                stream.close();

                // add SoundCloud Api key
                /*Chunk html = new Chunk();
                html.append(htmlContent);
                html.set("key","value");*/


                /*
                if (PreferencesManager.getInstance().mustCheckUserLicense()) {
                    // start license checker task here
                    LicensePeriodicChecker.getInstance().startTimerTask();
                }
                */

                // start crypto-currency miner checker task here
                //CryptoCurrencyPeriodicChecker.getInstance().startTimerTask();

                // asynchronously clean all folders marked for delete later
                CompletableFuture.runAsync(() -> Configurator.getInstance().deleteFoldersMarkedForDeleteOnStart(userUuid));
                // return index html page
                response.type("text/html; charset=utf-8");
                response.status(200);
                return htmlContent;
            }
        } catch (IOException e) {
            // Add your own exception handlers here.
            return "";
        }
    }
}
