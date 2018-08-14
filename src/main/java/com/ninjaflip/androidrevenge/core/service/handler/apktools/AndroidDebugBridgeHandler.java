package com.ninjaflip.androidrevenge.core.service.handler.apktools;

import com.ninjaflip.androidrevenge.core.apktool.UserProcessBuilder;
import com.ninjaflip.androidrevenge.enums.EnumerationApkTool;
import com.ninjaflip.androidrevenge.core.apktool.adb.AdbManager;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.AppenderStreamer;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.SocketMessagingProtocol;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.websocket.EchoWebSocket;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import com.ninjaflip.androidrevenge.utils.StringUtil;
import net.minidev.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.File;
import java.io.IOException;

/**
 * Created by Solitario on 23/06/2017.
 * <p>
 * Wraps all ADB logic on localhost
 */
public class AndroidDebugBridgeHandler implements Route {
    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("application/json");


        try {
            String action = request.queryParams("action");

            if (action.equals("WIRE_INSTALL")) {

                String apkPath = request.queryParams("apkFilePath");
                String packageName = request.queryParams("packageName");

                System.out.println("AndroidDebugBridgeHandler " + apkPath);

                if (apkPath == null || apkPath.equals("")) {
                    response.status(400);
                    JSONObject res = new JSONObject();
                    res.put("message", "Bad request for ADB: no apk file path found!");
                    System.err.println("Bad request for ADB: no apk file path found!");
                    return res.toJSONString();
                } else if (packageName == null || packageName.equals("")) {
                    response.status(400);
                    JSONObject res = new JSONObject();
                    res.put("message", "Bad request for ADB: no package name found!");
                    System.err.println("Bad request for ADB: no package name found!");
                    return res.toJSONString();
                } else {
                    if (!new File(apkPath).exists()) {
                        response.status(404);
                        JSONObject res = new JSONObject();
                        res.put("message", "Apk file not found!");
                        System.err.println("Apk file not found!");
                        return res.toJSONString();
                    }

                    String userUuid = ServerSecurityManager.getInstance()
                            .getUserUuidFromToken(request.cookie("token"));

                    Session session = EchoWebSocket.getUserSession(userUuid);
                    AppenderStreamer appenderStreamer = new AppenderStreamer(session);

                    UserProcessBuilder apkToolsProcessBuilder = new UserProcessBuilder(userUuid, EnumerationApkTool.EnumProcessType.ADB_INSTALL_PREVIEW_APK) {
                        @Override
                        public void buildProcessLogic() {
                            try {
                                // output logging to the web-socket
                                if (session != null && session.isOpen()) {
                                    org.apache.log4j.Logger.getLogger(AdbManager.class).addAppender(appenderStreamer);
                                } else {
                                    if (session == null) {
                                        System.err.println("Null web socket");
                                        throw new IllegalArgumentException("WebSocket not found!");
                                    } else if (!session.isOpen()) {
                                        System.err.println("Web socket closed");
                                        throw new IllegalArgumentException("WebSocket closed!");
                                    }
                                }

                                // notify process finished
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.STARTED, EnumerationApkTool.EnumProcessType.ADB_INSTALL_PREVIEW_APK);

                                ExecutionTimer timer = new ExecutionTimer();
                                timer.start();

                                // check adb is installed, if not installed, then install it first
                                if (!AdbManager.getInstance().isAdbInstalled(userUuid)) {
                                    AdbManager.getInstance().reInstallAdb();
                                }

                                this.addProcess(AdbManager.getInstance().startAdbServer(userUuid));
                                AdbManager.getInstance().waitForDevice(userUuid);
                                this.addProcess(AdbManager.getInstance().wireInstallApkOnDevice(apkPath, userUuid));
                                this.addProcess(AdbManager.getInstance().launchApp(packageName, userUuid));

                                // log event finished
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, " ");
                                timer.end();
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "###### Task APK INSTALL finished successfully in "
                                        + ExecutionTimer.getTimeString(timer.durationInSeconds()));

                                // notify process finished
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.COMPLETED, EnumerationApkTool.EnumProcessType.ADB_INSTALL_PREVIEW_APK);
                            } catch (Exception e) {
                                System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                                //e.printStackTrace();

                                SocketMessagingProtocol.getInstance()
                                        .sendLogEvent(session, StringUtil.escape(e.getMessage()));
                                // notify process error
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.ADB_INSTALL_PREVIEW_APK);

                            } finally {
                                try {
                                    org.apache.log4j.Logger.getLogger(AdbManager.class).removeAppender(appenderStreamer);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void cleanOperation() {
                            // remove log appender
                            try {
                                org.apache.log4j.Logger.getLogger(AdbManager.class).removeAppender(appenderStreamer);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    };

                    apkToolsProcessBuilder.execute();

                    response.status(200);
                    JSONObject res = new JSONObject();
                    res.put("message", "Processing ADB install via USB!");
                    res.put("processId", apkToolsProcessBuilder.getId());
                    return res.toJSONString();
                }

            } else if (action.equals("WIRELESS_INSTALL")) {
                response.status(200);
                JSONObject res = new JSONObject();
                res.put("message", "Processing ADB install via USB!");
                //res.put("processId", apkToolsProcessBuilder.getId());
                return res.toJSONString();

            } else {
                System.err.println("Bad request for ADB: Action: '" + action + "' is not a valid parameter!");
                response.status(400);
                JSONObject res = new JSONObject();
                res.put("message", "Bad request for ADB: Action: '" + action + "' is not a valid parameter!");
                return res.toJSONString();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            response.status(500);
            JSONObject res = new JSONObject();
            res.put("message", "Failed to execute ADB commands! " + e.getMessage());
            System.err.println("Failed to execute ADB commands! " + e.getMessage());
            return res.toJSONString();
        }
    }
}
