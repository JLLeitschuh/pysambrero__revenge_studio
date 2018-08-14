package com.ninjaflip.androidrevenge.core.service.handler.apktools.adb;

import com.ninjaflip.androidrevenge.enums.EnumerationApkTool;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.UserProcessBuilder;
import com.ninjaflip.androidrevenge.core.apktool.adb.AdbManager;
import com.ninjaflip.androidrevenge.core.apktool.apkinfo.AnalysisApk;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.AppenderStreamer;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.SocketMessagingProtocol;
import com.ninjaflip.androidrevenge.websocket.EchoWebSocket;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import com.ninjaflip.androidrevenge.utils.StringUtil;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;

/**
 * Created by Solitario on 27/09/2017.
 * <p>
 * Wrapper for adb install, this handler is reponsible  for installing apk files into user's devices
 */
public class AdbHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(AdbHandler.class);

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String userUuid = ServerSecurityManager.getInstance()
                .getUserUuidFromToken(request.cookie("token"));

        String action = request.queryParams("action");
        // validate action
        if (action == null || action.equals("")) {
            response.type("text/plain; charset=utf-8");
            response.status(400);
            String reason = "action parameter not found!";
            response.body(reason);
            return reason;
        }

        try {
            switch (action) {
                case "ADB_INSTALL": {
                    // check if there is an ongoing process => if exist ask user to wait until that process is finished
                    List<UserProcessBuilder> processes = UserProcessBuilder.getUserRunningProcesses(userUuid);
                    if (processes.size() > 0) {
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

                    String savedTmpApkFilePath = request.queryParams("savedTmpApkFilePath");
                    // validate savedTmpApkFilePath
                    if (savedTmpApkFilePath == null || savedTmpApkFilePath.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "tmp apk file path parameter not found!";
                        response.body(reason);
                        return reason;
                    }
                    String apkTmpFilePath = URLDecoder.decode(savedTmpApkFilePath, "UTF-8");
                    File apkTmpFile = new File(apkTmpFilePath);
                    if (!apkTmpFile.exists()) {
                        response.type("text/plain; charset=utf-8");
                        response.status(404);
                        String reason = "tmp apk file not found in filesystem!";
                        response.body(reason);
                        return reason;
                    }

                    String packageName = AnalysisApk.getPackageNameFromApkFile(apkTmpFile.getPath());
                    Session session = EchoWebSocket.getUserSession(userUuid);
                    AppenderStreamer appenderStreamer = new AppenderStreamer(session);

                    UserProcessBuilder adbInstallProcessBuilder = new UserProcessBuilder(userUuid, EnumerationApkTool.EnumProcessType.ADB_INSTALL_SINGLE) {
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

                                // notify process started
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.STARTED, EnumerationApkTool.EnumProcessType.ADB_INSTALL_SINGLE);

                                ExecutionTimer timer = new ExecutionTimer();
                                timer.start();

                                // check adb is installed, if not installed, then install it first
                                if (!AdbManager.getInstance().isAdbInstalled(userUuid)) {
                                    AdbManager.getInstance().reInstallAdb();
                                }

                                this.addProcess(AdbManager.getInstance().startAdbServer(userUuid));
                                AdbManager.getInstance().waitForDevice(userUuid);

                                Thread.sleep(100);
                                if (Thread.currentThread().isInterrupted()) {
                                    Thread.currentThread().interrupt();
                                    throw new InterruptedException("ADB thread was aborted abnormally!");
                                }

                                if (packageName != null) {
                                    this.addProcess(AdbManager.getInstance().adbUninstall(packageName, userUuid));
                                }
                                Thread.sleep(100);
                                if (Thread.currentThread().isInterrupted()) {
                                    Thread.currentThread().interrupt();
                                    throw new InterruptedException("ADB thread was aborted abnormally!");
                                }
                                this.addProcess(AdbManager.getInstance().wireInstallApkOnDevice(apkTmpFile.getPath(), userUuid));
                                Thread.sleep(100);
                                if (Thread.currentThread().isInterrupted()) {
                                    Thread.currentThread().interrupt();
                                    throw new InterruptedException("ADB thread was aborted abnormally!");
                                }
                                if (packageName != null) {
                                    this.addProcess(AdbManager.getInstance().launchApp(packageName, userUuid));
                                }
                                Thread.sleep(100);
                                if (Thread.currentThread().isInterrupted()) {
                                    Thread.currentThread().interrupt();
                                    throw new InterruptedException("ADB thread was aborted abnormally!");
                                }
                                // log event finished
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, " ");
                                timer.end();
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "###### Task APK INSTALLER finished successfully in "
                                        + ExecutionTimer.getTimeString(timer.durationInSeconds()));

                                // notify process finished
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.COMPLETED, EnumerationApkTool.EnumProcessType.ADB_INSTALL_SINGLE);
                            } catch (Exception e) {
                                System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                                //e.printStackTrace();
                                SocketMessagingProtocol.getInstance()
                                        .sendLogEvent(session, StringUtil.escape(e.getMessage()));
                                // notify process error
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.ADB_INSTALL_SINGLE, e.getMessage());

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
                    adbInstallProcessBuilder.execute();

                    response.type("text/plain; charset=utf-8");
                    response.status(200);
                    return adbInstallProcessBuilder.getId();


                }
                case "CANCEL_ADB_INSTALL": {
                    String processId = request.queryParams("processId");
                    // validate processId
                    if (processId == null || processId.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "process ID parameter not found!";
                        response.body(reason);
                        return reason;
                    }
                    UserProcessBuilder.cancelRunningProcess(processId, userUuid);
                    response.type("text/plain; charset=utf-8");
                    response.status(200);
                    return "Process " + processId + " canceled successfully!";
                }
                default: {
                    response.type("text/plain; charset=utf-8");
                    response.status(400);
                    String reason = "Unknown action parameter!";
                    response.body(reason);
                    return reason;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("error: " + e.getMessage());
            response.type("text/plain; charset=utf-8");
            response.status(500);
            String reason = e.getMessage();
            response.body(reason);
            return reason;
        }
    }
}
