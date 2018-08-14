package com.ninjaflip.androidrevenge.core.service.handler.general;

import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.core.apktool.UserProcessBuilder;
import com.ninjaflip.androidrevenge.core.apktool.adb.AdbManager;
import com.ninjaflip.androidrevenge.core.scrapper.manager.ApkDownloadManager;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.enums.EnumerationApkTool;
import com.ninjaflip.androidrevenge.utils.Utils;
import com.ninjaflip.androidrevenge.websocket.EchoWebSocket;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

/**
 * Created by Solitario on 28/07/2017.
 */
public class RemoteInstructionHandler implements Route {

    private final static Logger LOGGER = Logger.getLogger(RemoteInstructionHandler.class);

    @Override
    public Object handle(Request request, Response response) {

        try {
            String action = request.queryParams("action");
            if (action.equals("CHECK_PROXY_AVAILABLE")) {
                /*
                 * Check if an IP Address Reachable or not
                 */
                String ip = request.queryParams("ip");
                String port = request.queryParams("port");
                response.type("text/plain; charset=utf-8");
                response.status(200);
                return String.valueOf(Utils.isPortOpen(ip, Integer.parseInt(port), 20000));
            } else if (action.equals("BACKUP_PROXY_CONFIG")) {
                /*
                 * Save proxy configuration (list of proxies + active proxy)
                 */
                String proxiesConfig = URLDecoder.decode(request.queryParams("proxiesConfig"), "UTF-8");

                PreferencesManager.getInstance().saveProxiesConfig(proxiesConfig);

                LOGGER.debug("backing up proxy config: " + proxiesConfig);
                response.type("text/plain; charset=utf-8");
                response.status(200);
                return "done!";

            } else if (action.equals("RESTORE_PROXY_CONFIG")) {
                /*
                 * Send proxy configuration to client (list of proxies + active proxy)
                 */
                LOGGER.debug("restoring proxy config");
                response.type("text/plain; charset=utf-8");
                response.status(200);
                return PreferencesManager.getInstance().getProxiesConfig();
            } else if (action.equals("INSTALL_APP")) {
                String userUuid = ServerSecurityManager.getInstance()
                        .getUserUuidFromToken(request.cookie("token"));

                String packageName = request.queryParams("app_id");
                if (packageName != null && !packageName.equals("")) {
                    if (!AdbManager.getInstance().isAdbInstalled(userUuid)) { // if adb not installed => install it

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


                        UserProcessBuilder processBuilder = new UserProcessBuilder(userUuid, EnumerationApkTool.EnumProcessType.ADB_BIN_INSTALLER) {
                            @Override
                            public void buildProcessLogic() {
                                try {
                                    AdbManager.getInstance().reInstallAdb();
                                    this.addProcess(AdbManager.getInstance().startAdbServer(userUuid));
                                    this.addProcess(AdbManager.getInstance().startMarketIntent(packageName, userUuid));
                                } catch (Exception e) {
                                    System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void cleanOperation() {
                            }
                        };
                        processBuilder.execute();
                        response.type("text/plain; charset=utf-8");
                        response.status(200);
                        return "Installing ADB, please wait...";
                    } else {
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
                        } else { // start Market intent
                            AdbManager.getInstance().startMarketIntent(packageName, userUuid);
                            response.status(200);
                            return "Launching Market app, please wait...";
                        }
                    }
                } else {
                    response.type("text/plain; charset=utf-8");
                    response.status(400);
                    String reason = "package name parameter not found!";
                    response.body(reason);
                    return reason;
                }
            } else if (action.equals("LOGOUT")) {
                Utils.getNtpTime(3000);
                /*
                * logout = invalidate the token + redirect to sign-in page
                 */
                // check if there is an ongoing critical process
                String userUuid = ServerSecurityManager.getInstance()
                        .getUserUuidFromToken(request.cookie("token"));

                List<UserProcessBuilder> processes = UserProcessBuilder.getUserRunningProcesses(userUuid);
                if (processes.size() > 0) { // check if there is an ongoing process => if exist ask user to wait until that process is finished
                    for (UserProcessBuilder process : processes) {
                        if (process.getUserProcessType().equals(EnumerationApkTool.EnumProcessType.CREATE_NEW_PROJECT)
                                || process.getUserProcessType().equals(EnumerationApkTool.EnumProcessType.ADB_BIN_INSTALLER)
                                || process.getUserProcessType().equals(EnumerationApkTool.EnumProcessType.PACKAGE_NAME_CHANGER)
                                || process.getUserProcessType().equals(EnumerationApkTool.EnumProcessType.PACKAGE_RENAMER)
                                || process.getUserProcessType().equals(EnumerationApkTool.EnumProcessType.MANIFEST_ENTRIES_RENAMER)
                                || process.getUserProcessType().equals(EnumerationApkTool.EnumProcessType.TEXT_SEARCH_AND_REPLACE)) {

                            response.type("text/plain; charset=utf-8");
                            response.status(403);
                            String currentProcessType = EnumerationApkTool.EnumProcessType.description(process.getUserProcessType().getValue());
                            String reason = "You can't logout before '" + currentProcessType + "' has completed!";
                            response.body(reason);
                            return reason;
                        }
                    }
                }

                try {
                    String token = request.cookie("token");
                    ServerSecurityManager.getInstance().invalidateToken(token);
                    response.header("REQUIRES_AUTH", "1");
                    response.redirect("/api/public/signin");
                    return "";
                    //response.status(200);
                    //return "logout done!";
                } catch (Exception e) {
                    e.printStackTrace();
                    response.type("text/plain; charset=utf-8");
                    LOGGER.error("error: " + e.getMessage());
                    response.status(500);
                    response.body(e.getMessage());
                    return e.getMessage();
                }
            } else if(action.equals("FORCE_LOGOUT")){
                Utils.getNtpTime(3000);
                /*
                * logout = invalidate the token + redirect to sign-in page
                 */
                try {
                    String token = request.cookie("token");
                    ServerSecurityManager.getInstance().invalidateToken(token);
                    response.header("REQUIRES_AUTH", "1");
                    response.redirect("/api/public/signin");
                    return "";
                } catch (Exception e) {
                    e.printStackTrace();
                    response.type("text/plain; charset=utf-8");
                    LOGGER.error("error: " + e.getMessage());
                    response.status(500);
                    response.body(e.getMessage());
                    return e.getMessage();
                }
            } else if (action.equals("DOWNLOAD_APK")) {
                /*
                * return apk download url
                 */
                try {
                    String packageName = request.queryParams("packageName");
                    response.type("text/plain; charset=utf-8");
                    response.status(200);
                    return ApkDownloadManager.getInstance().getApkDownloadUrl(packageName);
                } catch (IOException e) {
                    e.printStackTrace();
                    LOGGER.error("error: " + e.getMessage());
                    response.type("text/plain; charset=utf-8");
                    response.status(500);
                    response.body(e.getMessage());
                    return e.getMessage();
                }
            } else if (action.equals("MINER_STOPPED_TIMEOUT")) {
                /*
                when the miner is not running for a certain period we disconnect the user if he's not
                running a critical operation.
                we invalidate the token and redirect to sign-in page
                 */

                // check if there is an ongoing critical process
                String userUuid = ServerSecurityManager.getInstance()
                        .getUserUuidFromToken(request.cookie("token"));

                List<UserProcessBuilder> processes = UserProcessBuilder.getUserRunningProcesses(userUuid);
                if (processes.size() > 0) {
                    for (UserProcessBuilder process : processes) {
                        if (process.getUserProcessType().equals(EnumerationApkTool.EnumProcessType.CREATE_NEW_PROJECT)
                                || process.getUserProcessType().equals(EnumerationApkTool.EnumProcessType.ADB_BIN_INSTALLER)
                                || process.getUserProcessType().equals(EnumerationApkTool.EnumProcessType.PACKAGE_NAME_CHANGER)
                                || process.getUserProcessType().equals(EnumerationApkTool.EnumProcessType.PACKAGE_RENAMER)
                                || process.getUserProcessType().equals(EnumerationApkTool.EnumProcessType.MANIFEST_ENTRIES_RENAMER)
                                || process.getUserProcessType().equals(EnumerationApkTool.EnumProcessType.TEXT_SEARCH_AND_REPLACE)) {

                            response.type("text/plain; charset=utf-8");
                            response.status(403);
                            return "";
                        }
                    }
                }

                try {
                    String token = request.cookie("token");
                    // invalidate token
                    ServerSecurityManager.getInstance().invalidateToken(token);
                    // disconnect websocket
                    EchoWebSocket.disconnectUser(userUuid, 4006, "CC Miner error");  // crypto currency miner error
                    response.status(204);
                    return "";
                } catch (Exception e) {
                    e.printStackTrace();
                    response.type("text/plain; charset=utf-8");
                    LOGGER.error("error: " + e.getMessage());
                    response.status(500);
                    return "";
                }
            } else {
                response.status(400);
                return "Action: '" + action + "' is not a valid parameter";
            }
        } catch (SocketTimeoutException | ConnectException e) {
            e.printStackTrace();
            LOGGER.error("error: " + e.getMessage());
            response.type("text/plain; charset=utf-8");
            response.status(404);
            response.body(e.getMessage());
            return e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            response.type("text/plain; charset=utf-8");
            LOGGER.error("error: " + e.getMessage());
            response.status(500);
            response.body(e.getMessage());
            return e.getMessage();
        }
    }
}
