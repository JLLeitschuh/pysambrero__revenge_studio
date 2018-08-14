package com.ninjaflip.androidrevenge.core.service.handler.apktools.debugger;

import com.ninjaflip.androidrevenge.enums.EnumerationApkTool;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.UserProcessBuilder;
import com.ninjaflip.androidrevenge.core.apktool.adb.AdbManager;
import com.ninjaflip.androidrevenge.core.apktool.adb.DebuggerProcessBuilder;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.AppenderStreamer;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.SocketMessagingProtocol;
import com.ninjaflip.androidrevenge.websocket.EchoWebSocket;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Solitario on 12/10/2017.
 * <p>
 * Handler that takes care of ADB logcat and its UI
 */
public class DebuggerHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(DebuggerHandler.class);


    @Override
    public Object handle(Request request, Response response) {
        String userUuid = ServerSecurityManager.getInstance()
                .getUserUuidFromToken(request.cookie("token"));

        String action = request.queryParams("action");

        try {
            // check if action is valid
            if (action == null || action.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "You must provide a debugger action!";
                response.body(reason);
                return reason;
            }

            switch (action) {
                case "GET_ALL_PROCESSES": {
                    if (!AdbManager.getInstance().isAdbInstalled(userUuid)) { // if adb not installed => install it only if no process is running
                        // user's current running processes
                        List<UserProcessBuilder> processes = UserProcessBuilder.getUserRunningProcesses(userUuid);
                        if (processes.size() > 0) { // check if there is an ongoing process => if exist ask user to wait until that process is finished
                        /*
                        503 service unavailable
                        The server is currently unable to handle the request due to a temporary overloading.
                        the source of the problem is an already running process from the same user, as the
                        server can process only one heavy process at once
                         */
                            response.type("text/plain; charset=utf-8");
                            response.status(503);
                            String currentProcessType = EnumerationApkTool.EnumProcessType.description(processes.get(0).getUserProcessType().getValue());
                            String reason = "'" + currentProcessType + "' is running, please wait until the current process is finished, or cancel it!";
                            response.body(reason);
                            return reason;
                        }

                        // adb not installed => installing it
                        // TODO appender for logging installation message to debugger UI
                        DebuggerProcessBuilder debuggerProcessBuilder = new DebuggerProcessBuilder(userUuid) {
                            @Override
                            public void buildProcessLogic() {
                                try {
                                    AdbManager.getInstance().reInstallAdb();
                                    this.addProcess(AdbManager.getInstance().startAdbServer(userUuid));
                                } catch (Exception e) {
                                    System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void cleanOperation() {
                                // remove log appender
                            }
                        };
                        debuggerProcessBuilder.execute();
                        response.status(204);
                        return "";
                    }

                    // check connected USB devices
                    AdbManager.getInstance().startAdbServer(userUuid);
                    Map<String, List<String>> devices = AdbManager.getInstance().getDevices(userUuid);
                    List<String> usbDevices = devices.get("usbDevices");

                    if (usbDevices.size() == 0) { // if no USB devices => error
                        response.type("text/plain; charset=utf-8");
                        response.status(404);
                        String reason = "Please connect your device to this computer's USB port!";
                        response.body(reason);
                        return reason;
                    } else {
                        List<JSONObject> listProcesses = AdbManager.getInstance().getProcesses(userUuid);
                        JSONObject result = new JSONObject();
                        result.put("listProcesses", listProcesses);
                        response.type("application/json; charset=UTF-8");
                        response.status(200);
                        return result;
                    }

                }
                case "START_DEBUGGING": {
                    if (!AdbManager.getInstance().isAdbInstalled(userUuid)) { // if adb not installed => install it only if no process is running
                        // user's current running processes
                        List<UserProcessBuilder> processes = UserProcessBuilder.getUserRunningProcesses(userUuid);
                        if (processes.size() > 0) { // check if there is an ongoing process => if exist ask user to wait until that process is finished
                        /*
                        503 service unavailable
                        The server is currently unable to handle the request due to a temporary overloading.
                        the source of the problem is an already running process from the same user, as the
                        server can process only one heavy process at once
                         */
                            response.type("text/plain; charset=utf-8");
                            response.status(503);
                            String currentProcessType = EnumerationApkTool.EnumProcessType.description(processes.get(0).getUserProcessType().getValue());
                            String reason = "'" + currentProcessType + "' is running, please wait until the current process is finished, or cancel it!";
                            response.body(reason);
                            return reason;
                        }

                        // adb not installed => installing it
                        // TODO appender for logging installation message to debugger UI
                        DebuggerProcessBuilder debuggerProcessBuilder = new DebuggerProcessBuilder(userUuid) {
                            @Override
                            public void buildProcessLogic() {
                                try {
                                    AdbManager.getInstance().reInstallAdb();
                                    this.addProcess(AdbManager.getInstance().startAdbServer(userUuid));
                                } catch (Exception e) {
                                    System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void cleanOperation() {
                                // remove log appender
                            }
                        };
                        debuggerProcessBuilder.execute();
                        response.status(204);
                        return "";
                    }

                    // adb is installed => stat logcat for process PID if device is connected
                    // check connected USB devices
                    AdbManager.getInstance().startAdbServer(userUuid);
                    Map<String, List<String>> devices = AdbManager.getInstance().getDevices(userUuid);
                    List<String> usbDevices = devices.get("usbDevices");

                    if (usbDevices.size() == 0) { // if no USB devices => error
                        response.type("text/plain; charset=utf-8");
                        response.status(404);
                        String reason = "Please connect your device to this computer's USB port!";
                        response.body(reason);
                        return reason;
                    }


                    String pid = request.queryParams("pid");
                    // check if PID is valid
                    if (pid == null || pid.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide PID (process id)!";
                        response.body(reason);
                        return reason;
                    }

                    String loglvl = request.queryParams("loglvl");
                    // check if log level is valid
                    if (loglvl == null || loglvl.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a log level!";
                        response.body(reason);
                        return reason;
                    }
                    System.out.println("==================================== LOG LVL: "+ loglvl);

                    if (!pid.equals("ALL")) {
                        boolean found = false;
                        List<JSONObject> listProcesses = AdbManager.getInstance().getProcesses(userUuid);
                        for (JSONObject obj : listProcesses) {
                            if (obj.get("PID").equals(pid)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            response.type("text/plain; charset=utf-8");
                            response.status(400);
                            String reason = "The selected process is NOT running!";
                            response.body(reason);
                            return reason;
                        }
                    }

                    // stop all previous debugger processes
                    DebuggerProcessBuilder.stopAllUserLogcatProcesses(userUuid);

                    //Thread.sleep(1500);

                    // start a new debugger session and strea output  to UI
                    Session session = EchoWebSocket.getUserSession(userUuid);
                    AppenderStreamer appenderStreamer = new AppenderStreamer(session, EnumerationApkTool.EnumLogType.DEBUGGER_LOG);
                    Logger debuggerLogger = Logger.getLogger(userUuid + "-" + UUID.randomUUID().toString());
                    debuggerLogger.setAdditivity(false);

                    DebuggerProcessBuilder debuggerProcessBuilder = new DebuggerProcessBuilder(userUuid) {
                        @Override
                        public void buildProcessLogic() {
                            try {
                                // output logging to the web-socket
                                if (session != null && session.isOpen()) {
                                    debuggerLogger.addAppender(appenderStreamer);
                                } else {
                                    if (session == null) {
                                        LOGGER.error("Null web socket");
                                        throw new IllegalArgumentException("WebSocket not found!");
                                    } else if (!session.isOpen()) {
                                        LOGGER.error("Web socket closed");
                                        throw new IllegalArgumentException("WebSocket closed!");
                                    }
                                }
                                //Process proc = AdbManager.getInstance().logProcess(pid, debuggerLogger, userUuid);
                                this.addProcess(AdbManager.getInstance().logProcess(pid, loglvl, false,debuggerLogger, userUuid));
                            } catch (Exception e) {
                                System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                                e.printStackTrace();
                                try {
                                    SocketMessagingProtocol.getInstance()
                                            .sendLogEvent(session, e.getMessage(), EnumerationApkTool.EnumLogType.DEBUGGER_LOG);
                                } catch (Exception ex) {
                                    LOGGER.error("Socket transmission error : " + ex.getMessage());
                                    ex.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void cleanOperation() {
                            // remove log appender
                            debuggerLogger.removeAppender(appenderStreamer);
                        }
                    };
                    debuggerProcessBuilder.execute();
                    response.status(204);
                    return "";
                }
                case "STOP_DEBUGGING": {
                    // if adb not installed => there is no process to stop
                    if (!AdbManager.getInstance().isAdbInstalled(userUuid)) {
                        response.status(204);
                        return "";
                    }
                    // stop all previous debugger processes
                    DebuggerProcessBuilder.stopAllUserLogcatProcesses(userUuid);
                    //clear buffer and exit logcat
                    AdbManager.getInstance().clearLogcatBuffer(null, userUuid);
                    response.status(204);
                    return "";
                }
                default: {
                    response.type("text/plain; charset=utf-8");
                    response.status(400);
                    String reason = "Unknown debugger action parameter!";
                    response.body(reason);
                    return reason;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("error: " + e.getMessage());
            response.type("text/plain; charset=UTF-8");
            String reason = e.getMessage();
            response.status(500);
            response.body(reason);
            return reason;
        }
    }
}
