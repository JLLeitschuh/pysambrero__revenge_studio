package com.ninjaflip.androidrevenge.websocket;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.apktool.UserProcessBuilder;
import com.ninjaflip.androidrevenge.core.apktool.adb.DebuggerProcessBuilder;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.SocketMessagingProtocol;
import com.ninjaflip.androidrevenge.core.scrapper.ScrapperProcessBuilder;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.utils.StringUtil;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Solitario on 20/05/2017.
 */
// Set maxIdle time to 24 hours (24x60x60x1000) otherwise
// the client will keep sending web-socket upgrade request every maxIdleTime.
// Thus reconnecting will keep popping-up (close/connect) ==> which is not good for user experience
// So we set a maxIdleTime to one day to avoid this behaviour
@WebSocket(maxIdleTime = 86400000)
public class EchoWebSocket {
    private final static Logger LOGGER = Logger.getLogger(EchoWebSocket.class);

    // Store sessions if you want to, for example, broadcast a message to all users
    //private static final Queue<Session> sessions = new ConcurrentLinkedQueue<Session>();
    // contains (userUuid, session)
    private static final Map<String, Session> sessions = new ConcurrentHashMap<String, Session>();
    // cachedObjectsPerUser key = userUuid , value = Map<String, Object> containing cached objects
    private static final Map<String, Map<String, Object>> cachedObjectsPerUser = new HashMap<>();


    @OnWebSocketConnect
    public void connected(Session session) {
        try {
            String queryString = new String(Base64.getDecoder().decode(session.getUpgradeRequest().getQueryString()), "UTF-8");
            Map<String, String> params = StringUtil.splitQuery(queryString);

            String token = params.get("token");
            String userId = params.get("userId");

            if (token != null && userId != null) {
                // validate desktopToken and userId ==> if not valid close session using status code 'Rejected'
                boolean isValidToken = ServerSecurityManager.getInstance().verifyToken(token, userId);
                if (isValidToken) {
                    //LOGGER.info("Websocket Session connected");
                    //LOGGER.info("Websocket Session ==> userUuid: " + userId + ", token: " + token);

                    String userUuid = ServerSecurityManager.getInstance().getUserUuidFromToken(token);

                    // close existing socket if exists
                    Session oldSession = sessions.get(userUuid);
                    if (oldSession != null)
                        oldSession.close(4601, "Closing old user session socket, because of new connection from same user!");

                    sessions.put(userUuid, session);
                    //LOGGER.info("Websocket Session new ==> session size : " + sessions.size());
                } else {
                    //LOGGER.error("Websocket invalid credentials!");
                    session.close(4500, "invalid credentials!");
                }
            } else {
                //LOGGER.error("Websocket null credentials!");
                session.close(4501, "null credentials!");
            }
        } catch (UnsupportedEncodingException e) {
            //e.printStackTrace();
            //LOGGER.error("Websocket corrupted credentials!");
            session.close(4502, "corrupted credentials!");
        }
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        for (String key : sessions.keySet()) {
            if (sessions.get(key).equals(session)) {
                //LOGGER.info("Websocket Session closed ==> userUuid : " + key + ", StatusCode: " + statusCode + ", Reason: " + reason);
                sessions.remove(key); // must be called here before onWebSocketClosed callbacks
                UserProcessBuilder.onWebSocketClosed(key);
                ScrapperProcessBuilder.onWebSocketClosed(key);
                DebuggerProcessBuilder.onWebSocketClosed(key);
                break;
            }
        }
        //LOGGER.info("Websocket Session closed ==> session size : " + sessions.size());
    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        //LOGGER.debug("Got: " + message);   // Print message
        //session.getRemote().sendString(message); // and send it back
        webSocketMessagingProtocol(session, message);
    }


    /**
     * Get user's session
     *
     * @param userUuid user's uuid
     * @return sessions
     */
    public static Session getUserSession(String userUuid) {
        return sessions.getOrDefault(userUuid, null);
    }


    public static void disconnectUser(String userUuid, int statusCode, String reason) {
        Session session = EchoWebSocket.getUserSession(userUuid);
        if (session != null) {
            session.close(statusCode, reason);
        }
    }


    /**
     * Remove a certain user's session
     */
    public static void removeSession(Session session) {
        if (session != null) {
            for (String key : sessions.keySet()) {
                if (sessions.get(key).equals(session)) {
                    UserProcessBuilder.onWebSocketClosed(key);
                    ScrapperProcessBuilder.onWebSocketClosed(key);
                    DebuggerProcessBuilder.onWebSocketClosed(key);
                    sessions.remove(key);
                    break;
                }
            }
        }
    }

    /**
     * Get number of sessions
     *
     * @return integer representing the total number of active web socket sessions
     */
    public static int getSessionCount() {
        return sessions.size();
    }

    /*
    A wrapper method that handles the message exchanging between the html5 client and the server
     */
    private void webSocketMessagingProtocol(Session session, String message) {
        // TODO call WebSocketProtocolWrapper singleton methods
        String userUuid = null;
        for (String key : sessions.keySet()) {
            if (sessions.get(key).equals(session)) {
                userUuid = key;
                //LOGGER.debug("User uuid: " + key);
                break;
            }
        }
        if (userUuid == null) {
            // problem can't find session owner
            return;
        }
        JSONObject jsonMessage = (JSONObject) JSONValue.parse(message);
        String messageType = (String) jsonMessage.get("type");
        String messageContent = (String) jsonMessage.get("content");

        switch (messageType) {
            case "INSTRUCTION": {
                if (messageContent.equals("GET_USER_PROJECTS_TOTAL_SIZE")) {
                    try {
                        // check if get path from cache
                        if (cachedObjectsPerUser.get(userUuid) != null
                                && cachedObjectsPerUser.get(userUuid).get("LAST_CALL_TOTAL_SIZE") != null
                                && cachedObjectsPerUser.get(userUuid).get("PROJECTS_TOTAL_SIZE") != null) {
                            Date lastCall = (Date) cachedObjectsPerUser.get(userUuid).get("LAST_CALL_TOTAL_SIZE");
                            Date lastCallPlusHalfMinute = DateUtils.addSeconds(lastCall, 30);
                            Date now = new Date();
                            if (now.before(lastCallPlusHalfMinute)) {
                                // from cache
                                long size = (long) cachedObjectsPerUser.get(userUuid).get("PROJECTS_TOTAL_SIZE");
                                SocketMessagingProtocol.getInstance().sendUserProjectsTotalSize(session, size);
                                //LOGGER.debug("Total projects size (from CACHE) : " + size + ", for user: " + userUuid);
                            } else {
                                // recalculate size if last call more than 30 seconds ago
                                Path folderPath = Paths.get(Configurator.getInstance().getUSERS_DIR() + File.separator + userUuid);
                                long size = Utils.fileOrFoderSize(folderPath);
                                SocketMessagingProtocol.getInstance().sendUserProjectsTotalSize(session, size);
                                cachedObjectsPerUser.get(userUuid).put("LAST_CALL_TOTAL_SIZE", new Date());
                                cachedObjectsPerUser.get(userUuid).put("PROJECTS_TOTAL_SIZE", size);
                                //LOGGER.debug("Total projects size (Recalculated): " + size + ", for user: " + userUuid);
                            }
                        } else {
                            // first call
                            Path folderPath = Paths.get(Configurator.getInstance().getUSERS_DIR() + File.separator + userUuid);
                            long size = Utils.fileOrFoderSize(folderPath);
                            SocketMessagingProtocol.getInstance().sendUserProjectsTotalSize(session, size);
                            cachedObjectsPerUser.put(userUuid, new HashMap<>());
                            cachedObjectsPerUser.get(userUuid).put("LAST_CALL_TOTAL_SIZE", new Date());
                            cachedObjectsPerUser.get(userUuid).put("PROJECTS_TOTAL_SIZE", size);
                            //LOGGER.debug("Total projects size (first call): " + size + ", for user: " + userUuid);
                        }

                    } catch (Exception e) {
                        //LOGGER.error("Websicket error : " + e.getMessage());
                        //e.printStackTrace();
                    }
                }
                break;
            }
            default: {
                break;
            }
        }
    }


    public static Map<String, Session> getAllSessions() {
        return sessions;
    }
}