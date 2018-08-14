package com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.fileEditor;

import brut.androlib.Androlib;
import brut.androlib.res.AndrolibResources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ninjaflip.androidrevenge.beans.ApkToolProjectBean;
import com.ninjaflip.androidrevenge.beans.KeystoreBean;
import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.ApkToolsManager;
import com.ninjaflip.androidrevenge.core.apktool.UserProcessBuilder;
import com.ninjaflip.androidrevenge.core.apktool.adb.AdbManager;
import com.ninjaflip.androidrevenge.core.apktool.apkinfo.AnalysisApk;
import com.ninjaflip.androidrevenge.core.apktool.filecomputing.FileComputingManager;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.core.apktool.signzipalign.SignTool;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.AppenderStreamer;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.HandlerStreamer;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.SocketMessagingProtocol;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.SocketPrintStream;
import com.ninjaflip.androidrevenge.core.apktool.updater.MassiveFileRenamer;
import com.ninjaflip.androidrevenge.core.apktool.updater.PackageNameChanger;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import com.ninjaflip.androidrevenge.core.db.dao.KeystoreDao;
import com.ninjaflip.androidrevenge.core.service.cache.GraphCache;
import com.ninjaflip.androidrevenge.enums.EnumerationApkTool;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import com.ninjaflip.androidrevenge.utils.StringUtil;
import com.ninjaflip.androidrevenge.utils.Utils;
import com.ninjaflip.androidrevenge.websocket.EchoWebSocket;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jetty.websocket.api.Session;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Solitario on 16/09/2017.
 * <p>
 * Handler that takes care of all actions related to editor tools: package name changer, app info updater, manifest entries renamer,...
 */
public class FeToolsHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(FeToolsHandler.class);
    private static final Pattern regexInvalidFileName = Pattern.compile(".*[\\\\.,;?|/\\\\'<>^*%!\" \\t].*");

    @Override
    public Object handle(Request request, Response response) {
        String userUuid = ServerSecurityManager.getInstance()
                .getUserUuidFromToken(request.cookie("token"));

        String action = request.queryParams("action");
        String projectUuid = request.queryParams("projectUuid");
        try {
            // check system clock is ok, because user may change clock to benefit overcome Quota limits
            if(!ServerSecurityManager.getInstance().isClockOk()){
                Utils.getNtpTime(3000); // refresh date
                response.type("text/plain; charset=utf-8");
                response.status(449);
                String message = "Incorrect system date. Please check your system date and time settings and try again!";
                response.body(message);
                return message;
            }

            // check if action is valid
            if (action == null || action.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "You must provide a search action!";
                response.body(reason);
                return reason;
            }

            // check if projectUuid is valid
            if (projectUuid == null || projectUuid.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "You must provide project uuid!";
                response.body(reason);
                return reason;
            }

            Graph graph = GraphCache.getInstance().getGraph(projectUuid);
            if (graph == null) {
                String projectFolderNameUuid = ApkToolProjectDao.getInstance().getByUuid(projectUuid).getProjectFolderNameUuid();
                String graphPath = Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, false);
                graph = GraphManager.getInstance().loadGraphFromJson(graphPath);
                GraphCache.getInstance().cacheGraph(projectUuid, graph);
            }


            // business code
            switch (action) {
                case "GET_INFO": {
                    String infoType = request.queryParams("infoType");
                    // check if infoType is valid
                    if (infoType == null || infoType.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide an infoType parameter!";
                        response.body(reason);
                        return reason;
                    }

                    ApkToolProjectBean project;
                    // check if that project exists and belongs to current user
                    if (ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
                        project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This project does not exists or does not belongs to you!";
                        response.body(reason);
                        return reason;
                    }


                    switch (infoType) {
                        case "CURRENT_PACKAGE_NAME": {
                            String decodedApkFolderPath = Configurator.getInstance().getDecodedApkFolderPath(userUuid, project.getProjectFolderNameUuid(), false);
                            String currentPackageName = ApkToolsManager.getInstance().getPackageNameFromManifest(decodedApkFolderPath);
                            JSONObject result = new JSONObject();
                            result.put("currentPackageName", currentPackageName);
                            response.type("application/json; charset=UTF-8");
                            response.status(200);
                            return result;
                        }
                        case "CURRENT_APP_INFO": {
                            String decodedApkFolderPath = Configurator.getInstance().getDecodedApkFolderPath(userUuid, project.getProjectFolderNameUuid(), false);

                            // version + sdk min/max from yaml file
                            Map<String, String> yamlAppInfo = ApkToolsManager.getInstance().getAppInfoFromYaml(decodedApkFolderPath);

                            JSONObject result = new JSONObject();

                            String versionCode = yamlAppInfo.get("versionCode");
                            if (versionCode != null) {
                                result.put("versionCode", versionCode);
                            }
                            String versionName = yamlAppInfo.get("versionName");
                            if (versionName != null) {
                                result.put("versionName", versionName);
                            }
                            String minSdkVersion = yamlAppInfo.get("minSdkVersion");
                            if (minSdkVersion != null) {
                                result.put("minSdkVersion", minSdkVersion);
                            }
                            String targetSdkVersion = yamlAppInfo.get("targetSdkVersion");
                            if (targetSdkVersion != null) {
                                result.put("targetSdkVersion", targetSdkVersion);
                            }
                            response.type("application/json; charset=UTF-8");
                            response.status(200);
                            return result;
                        }
                        case "ALL_AVAILABLE_PACKAGES": { // get list of available packages
                            ExecutionTimer timer = new ExecutionTimer();
                            timer.start();
                            JSONObject result = new JSONObject();

                            List<String> filePathContainsToExclude = new ArrayList<>();
                            filePathContainsToExclude.add(StringEscapeUtils.escapeJava("smali" + File.separator + "com" + File.separator + "google"));
                            filePathContainsToExclude.add(StringEscapeUtils.escapeJava("smali" + File.separator + "com" + File.separator + "actionbarsherlock"));
                            filePathContainsToExclude.add(StringEscapeUtils.escapeJava("smali" + File.separator + "android" + File.separator + "support"));
                            filePathContainsToExclude.add(StringEscapeUtils.escapeJava("smali" + File.separator + "com" + File.separator + "startapp"));

                            result.put("availablePackages", ApkToolsManager.getInstance().getAllPackageNames(graph, filePathContainsToExclude, userUuid, project.getProjectFolderNameUuid()));

                            timer.end();
                            LOGGER.debug("Get all available packages finished in " + timer.durationInSeconds() + " s");

                            response.type("application/json; charset=UTF-8");
                            response.status(200);
                            return result;
                        }
                        case "MANIFEST_ENTRIES": {
                            ExecutionTimer timer = new ExecutionTimer();
                            timer.start();
                            JSONObject manifestEntries = ApkToolsManager.getInstance().getAllManifestEntriesForRenamerTool(userUuid, project.getProjectFolderNameUuid(), false);
                            timer.end();
                            LOGGER.debug("Get manifest entries finished in " + timer.durationInSeconds() + " s");
                            response.type("application/json; charset=UTF-8");
                            response.status(200);
                            return manifestEntries;
                        }
                        case "ALL_AVAILABLE_APP_NAMES": {
                            ExecutionTimer timer = new ExecutionTimer();
                            timer.start();
                            JSONObject appNames = ApkToolsManager.getInstance()
                                    .getAllAvailableAppNamesForAppNameModifierTool(userUuid, project.getProjectFolderNameUuid(), graph);
                            timer.end();
                            LOGGER.debug("Get all available app names finished in " + timer.durationInSeconds() + " s");
                            //LOGGER.debug("available app names : " + appNames);
                            response.type("application/json; charset=UTF-8");
                            response.status(200);
                            return appNames;
                        }
                        case "ALL_AVAILABLE_APKS": {
                            ExecutionTimer timer = new ExecutionTimer();
                            timer.start();
                            JSONObject result = new JSONObject();
                            result.put("apkFiles", ApkToolsManager.getInstance().getProjectApkFilesInfo(userUuid, project.getProjectFolderNameUuid(), false));
                            timer.end();
                            LOGGER.debug("Get all available APK info finished in " + timer.durationInSeconds() + " s");
                            response.type("application/json; charset=UTF-8");
                            response.status(200);
                            return result;
                        }
                        case "ALL_AVAILABLE_KEYSTORES": {
                            ExecutionTimer timer = new ExecutionTimer();
                            timer.start();
                            List<JSONObject> keystoresObjects = new ArrayList<>();

                            JSONObject result = new JSONObject();
                            // list of {ksUuid, ksAlias, ksFileName}
                            List<KeystoreBean> listKeystores = KeystoreDao.getInstance().getAll(userUuid);
                            for (KeystoreBean ksb : listKeystores) {
                                JSONObject keystoresObject = new JSONObject();
                                keystoresObject.put("uuid", ksb.getUuid());
                                keystoresObject.put("alias", ksb.getAlias());
                                keystoresObject.put("description", ksb.getDescription());
                                keystoresObjects.add(keystoresObject);
                            }
                            result.put("keystores", keystoresObjects);

                            timer.end();
                            LOGGER.debug("Get all keystores finished in " + timer.durationInSeconds() + " s");
                            response.type("application/json; charset=UTF-8");
                            response.status(200);
                            return result;
                        }
                        default: {
                            response.type("text/plain; charset=utf-8");
                            response.status(400);
                            String reason = "Unknown infoType parameter!";
                            response.body(reason);
                            return reason;
                        }
                    }
                }
                case "SUBMIT_CHANGE_PACKAGE_NAME": {
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

                    String newPackageName = request.queryParams("newPackageName");
                    // check if newPackageName is valid
                    if (newPackageName == null || newPackageName.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Empty package name parameter!";
                        response.body(reason);
                        return reason;
                    }

                    ApkToolProjectBean project;
                    // check if that project exists and belongs to current user
                    if (ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
                        project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This project does not exists or does not belongs to you!";
                        response.body(reason);
                        return reason;
                    }

                    // validate newPackage name
                    try {
                        PackageNameChanger.validateNewPackageName(projectUuid, newPackageName, userUuid, false);
                    } catch (Exception ex) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = ex.getMessage();
                        response.body(reason);
                        return reason;
                    }

                    Session session = EchoWebSocket.getUserSession(userUuid);
                    AppenderStreamer appenderStreamer = new AppenderStreamer(session, EnumerationApkTool.EnumLogType.EDITOR_LOG);
                    boolean isTemporary = false;
                    String projectFolderNameUuid = project.getProjectFolderNameUuid();

                    // create package name changer process
                    UserProcessBuilder packageNameChangerProcessBuilder = new UserProcessBuilder(userUuid, EnumerationApkTool.EnumProcessType.PACKAGE_NAME_CHANGER) {
                        @Override
                        public void buildProcessLogic() {
                            try {
                                // output logging to the web-socket
                                if (session != null && session.isOpen()) {
                                    org.apache.log4j.Logger.getLogger(ApkToolsManager.class).addAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(GraphManager.class).addAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(PackageNameChanger.class).addAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(FileComputingManager.class).addAppender(appenderStreamer);
                                } else {
                                    if (session == null) {
                                        LOGGER.error("Null web socket");
                                        throw new IllegalArgumentException("WebSocket not found!");
                                    } else if (!session.isOpen()) {
                                        LOGGER.error("Web socket closed");
                                        throw new IllegalArgumentException("WebSocket closed!");
                                    }
                                }
                                ExecutionTimer timer = new ExecutionTimer();
                                timer.start();
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.STARTED, EnumerationApkTool.EnumProcessType.PACKAGE_NAME_CHANGER);

                                // update package name
                                ApkToolsManager.getInstance().updatePackageName(userUuid, projectFolderNameUuid, isTemporary, newPackageName.trim());

                                // update project bean in database when package name changed
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "Updating project bean in database with new package name: '" + newPackageName.trim() + "'"
                                        + ExecutionTimer.getTimeString(timer.durationInSeconds()), EnumerationApkTool.EnumLogType.EDITOR_LOG);
                                project.setPackageName(newPackageName.trim());
                                ApkToolProjectDao.getInstance().update(project);

                                // reload graph and update graph cache
                                String graphPath = Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, isTemporary);
                                GraphCache.getInstance().cacheGraph(projectUuid, GraphManager.getInstance().loadGraphFromJson(graphPath));

                                timer.end();

                                SocketMessagingProtocol.getInstance().sendLogEvent(session, " ", EnumerationApkTool.EnumLogType.EDITOR_LOG);
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "###### Change Package Name Task finished successfully in "
                                        + ExecutionTimer.getTimeString(timer.durationInSeconds()), EnumerationApkTool.EnumLogType.EDITOR_LOG);

                                // notify process finished
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.COMPLETED, EnumerationApkTool.EnumProcessType.PACKAGE_NAME_CHANGER);

                            } catch (Exception e) {
                                System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                                //e.printStackTrace();
                                try {
                                    SocketMessagingProtocol.getInstance()
                                            .sendLogEvent(session, e.getMessage(), EnumerationApkTool.EnumLogType.EDITOR_LOG);
                                    // notify process error
                                    SocketMessagingProtocol.getInstance()
                                            .sendProcessState(session, this.getId(),
                                                    EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.PACKAGE_NAME_CHANGER, e.getMessage());
                                } catch (Exception ex) {
                                    // notify process error

                                    SocketMessagingProtocol.getInstance()
                                            .sendProcessState(session, this.getId(),
                                                    EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.PACKAGE_NAME_CHANGER, e.getMessage());

                                    LOGGER.error("Socket transmission error : " + ex.getMessage());
                                    //ex.printStackTrace();
                                }
                            } finally {
                                try {
                                    org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(PackageNameChanger.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);

                                    System.gc();
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void cleanOperation() {
                            // remove log appender
                            try {
                                org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(PackageNameChanger.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);

                                LOGGER.info("cleanOperation => log appender because of un-achieved operation");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    };
                    packageNameChangerProcessBuilder.execute();

                    //res.put("processId", searchAndReplaceProcessBuilder.getId());
                    response.status(204);
                    return "";
                }
                case "SUBMIT_RENAME_PACKAGE": {
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

                    String existingPackageName = request.queryParams("existingPackageName");
                    // check if existingPackageName is valid
                    if (existingPackageName == null || existingPackageName.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Existing package name parameter is empty!";
                        response.body(reason);
                        return reason;
                    }


                    String packageNewName = request.queryParams("packageNewName");
                    // check if packageNewName is valid
                    if (packageNewName == null || packageNewName.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "New package name parameter is empty!";
                        response.body(reason);
                        return reason;
                    }

                    ApkToolProjectBean project;
                    // check if that project exists and belongs to current user
                    if (ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
                        project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This project does not exists or does not belongs to you!";
                        response.body(reason);
                        return reason;
                    }

                    // validate existingPackageName and packageNewName
                    try {
                        PackageNameChanger.validatePackageRenaming(projectUuid, existingPackageName, packageNewName, userUuid, false);
                    } catch (Exception ex) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = ex.getMessage();
                        response.body(reason);
                        return reason;
                    }

                    Session session = EchoWebSocket.getUserSession(userUuid);
                    AppenderStreamer appenderStreamer = new AppenderStreamer(session, EnumerationApkTool.EnumLogType.EDITOR_LOG);
                    boolean isTemporary = false;
                    String projectFolderNameUuid = project.getProjectFolderNameUuid();

                    // create package renaming process
                    UserProcessBuilder packageRenamerProcessBuilder = new UserProcessBuilder(userUuid, EnumerationApkTool.EnumProcessType.PACKAGE_RENAMER) {
                        @Override
                        public void buildProcessLogic() {
                            try {
                                // output logging to the web-socket
                                if (session != null && session.isOpen()) {
                                    org.apache.log4j.Logger.getLogger(ApkToolsManager.class).addAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(GraphManager.class).addAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(PackageNameChanger.class).addAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(FileComputingManager.class).addAppender(appenderStreamer);
                                } else {
                                    if (session == null) {
                                        LOGGER.error("Null web socket");
                                        throw new IllegalArgumentException("WebSocket not found!");
                                    } else if (!session.isOpen()) {
                                        LOGGER.error("Web socket closed");
                                        throw new IllegalArgumentException("WebSocket closed!");
                                    }
                                }
                                ExecutionTimer timer = new ExecutionTimer();
                                timer.start();
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.STARTED, EnumerationApkTool.EnumProcessType.PACKAGE_RENAMER);

                                // update package name
                                ApkToolsManager.getInstance().renamePackage(userUuid, projectFolderNameUuid,
                                        isTemporary, existingPackageName.trim(), packageNewName.trim());

                                // reload graph and update graph cache
                                String graphPath = Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, isTemporary);
                                GraphCache.getInstance().cacheGraph(projectUuid, GraphManager.getInstance().loadGraphFromJson(graphPath));

                                timer.end();

                                SocketMessagingProtocol.getInstance().sendLogEvent(session, " ", EnumerationApkTool.EnumLogType.EDITOR_LOG);
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "###### Package Renamer Task finished successfully in "
                                        + ExecutionTimer.getTimeString(timer.durationInSeconds()), EnumerationApkTool.EnumLogType.EDITOR_LOG);

                                // notify process finished
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.COMPLETED, EnumerationApkTool.EnumProcessType.PACKAGE_RENAMER);

                            } catch (Exception e) {
                                System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                                //e.printStackTrace();
                                try {
                                    SocketMessagingProtocol.getInstance()
                                            .sendLogEvent(session, e.getMessage(), EnumerationApkTool.EnumLogType.EDITOR_LOG);
                                    // notify process error
                                    SocketMessagingProtocol.getInstance()
                                            .sendProcessState(session, this.getId(),
                                                    EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.PACKAGE_RENAMER, e.getMessage());
                                } catch (Exception ex) {
                                    // notify process error

                                    SocketMessagingProtocol.getInstance()
                                            .sendProcessState(session, this.getId(),
                                                    EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.PACKAGE_RENAMER, e.getMessage());
                                    LOGGER.error("Socket transmission error : " + ex.getMessage());
                                    //ex.printStackTrace();
                                }
                            } finally {
                                try {
                                    org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(PackageNameChanger.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);

                                    System.gc();
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void cleanOperation() {
                            // remove log appender
                            try {
                                org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(PackageNameChanger.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);

                                LOGGER.info("cleanOperation => log appender because of un-achieved operation");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    };
                    packageRenamerProcessBuilder.execute();
                    //res.put("processId", packageRenamerProcessBuilder.getId());
                    response.status(204);
                    return "";
                }
                case "SUBMIT_UPDATE_APP_INFO": {
                    String versionCode = request.queryParams("versionCode");
                    // check if versionCode is valid
                    if (versionCode == null) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "version code parameter not found!";
                        response.body(reason);
                        return reason;
                    } else {
                        if (versionCode.equals("")) {
                            response.type("text/plain; charset=utf-8");
                            response.status(400);
                            String reason = "version code is empty!";
                            response.body(reason);
                            return reason;
                        } else {
                            /* check if versionCode is a valid integer between 1 and 2100000000
                             from google developer documentation: https://developer.android.com/studio/publish/versioning.html
                             Warning: The greatest value Google Play allows for versionCode is 2100000000.
                            */
                            try {
                                int versionCodeNumber = Integer.parseInt(versionCode);
                                if (versionCodeNumber > 2100000000 || versionCodeNumber < 1) {
                                    response.type("text/plain; charset=utf-8");
                                    response.status(400);
                                    String reason = "version code must be a number between 1 and 2100000000!";
                                    response.body(reason);
                                    return reason;
                                }
                            } catch (NumberFormatException e) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "version code must be a number between 1 and 2100000000!";
                                response.body(reason);
                                return reason;
                            }
                        }
                    }


                    String versionName = request.queryParams("versionName");
                    // check if versionName is valid
                    if (versionName == null) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "version name parameter not found!";
                        response.body(reason);
                        return reason;
                    } else {
                        if (versionName.equals("")) {
                            response.type("text/plain; charset=utf-8");
                            response.status(400);
                            String reason = "version name is empty!";
                            response.body(reason);
                            return reason;
                        }
                    }


                    String minSdkVersion = request.queryParams("minSdkVersion");
                    // check if minSdkVersion is valid
                    if (minSdkVersion != null) {
                        if (minSdkVersion.equals("")) {
                            response.type("text/plain; charset=utf-8");
                            response.status(400);
                            String reason = "min sdk version is empty!";
                            response.body(reason);
                            return reason;
                        } else {
                            // check if minSdkVersion is a valid integer greater than 1
                            try {
                                int minSdkVersionNumber = Integer.parseInt(minSdkVersion);
                                if (minSdkVersionNumber < 1) {
                                    response.type("text/plain; charset=utf-8");
                                    response.status(400);
                                    String reason = "min sdk version must be a number greater than 1!";
                                    response.body(reason);
                                    return reason;
                                }
                            } catch (NumberFormatException e) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "min sdk version must be a number greater than 1!";
                                response.body(reason);
                                return reason;
                            }
                        }
                    }

                    String targetSdkVersion = request.queryParams("targetSdkVersion");
                    // check if targetSdkVersion is valid
                    if (targetSdkVersion != null) {
                        if (targetSdkVersion.equals("")) {
                            response.type("text/plain; charset=utf-8");
                            response.status(400);
                            String reason = "target sdk version is empty!";
                            response.body(reason);
                            return reason;
                        } else {
                            // check if targetSdkVersion is a valid integer greater than 1
                            try {
                                int targetSdkVersionNumber = Integer.parseInt(targetSdkVersion);
                                if (targetSdkVersionNumber < 1) {
                                    response.type("text/plain; charset=utf-8");
                                    response.status(400);
                                    String reason = "target sdk version must be a number greater than 1!";
                                    response.body(reason);
                                    return reason;
                                }
                            } catch (NumberFormatException e) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "target sdk version must be a number greater than 1!";
                                response.body(reason);
                                return reason;
                            }
                        }
                    }

                    ApkToolProjectBean project;
                    // check if that project exists and belongs to current user
                    if (ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
                        project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This project does not exists or does not belongs to you!";
                        response.body(reason);
                        return reason;
                    }

                    String decodedApkFolderPath = Configurator.getInstance()
                            .getDecodedApkFolderPath(userUuid, project.getProjectFolderNameUuid(), false);
                    ApkToolsManager.getInstance().updateAppInfoInYaml(decodedApkFolderPath, versionCode, versionName,
                            minSdkVersion, targetSdkVersion);

                    response.status(204);
                    return "";
                }
                case "SUBMIT_MANIFEST_ENTRIES_RENAMER": {
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
                    ApkToolProjectBean project;
                    // check if that project exists and belongs to current user
                    if (ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
                        project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This project does not exists or does not belongs to you!";
                        response.body(reason);
                        return reason;
                    }

                    String customNamesParam = request.queryParams("customNames");
                    // check if customNamesParam is valid
                    if (customNamesParam == null || "".equals(customNamesParam)) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "customNames parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    // convert customName to json object
                    JSONObject receivedCustomNameJsonObject = (JSONObject) JSONValue.parse(customNamesParam);
                    LOGGER.info("Received customNames JSON: " + receivedCustomNameJsonObject);
                    if (receivedCustomNameJsonObject != null) {
                        Map<String, String> customNames = new HashMap<>();
                        // keep only changed elements
                        for (String originalClassFullName : receivedCustomNameJsonObject.keySet()) {
                            String newClassName = receivedCustomNameJsonObject.getAsString(originalClassFullName);
                            String[] originalClassFullNameSplit = originalClassFullName.split("\\.");
                            String originalClassName = originalClassFullNameSplit[originalClassFullNameSplit.length - 1];

                            if (!originalClassName.equals(newClassName)) {
                                customNames.put(originalClassFullName, newClassName);
                            }
                        }

                        LOGGER.info("Filtered customNames: " + customNames);

                        // if customNames.size()== 0 means that all NewClassNames are equal to OldClassNames => nothing to do
                        if (customNames.size() > 0) {
                            // check if customNames has some duplicates
                            Set<String> duplicates = Utils.findDuplicates(customNames.values());
                            if (duplicates.size() != 0) {
                                String duplicatesAsStr = String.join(", ", duplicates);
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "New manifest entries contain " + duplicates.size() + " duplicate(s) : " + duplicatesAsStr;
                                response.body(reason);
                                return reason;
                            }

                            Pattern p = Pattern.compile("^[A-Za-z]{1}[A-Za-z0-9\\\\d_]*$");
                            // check if class names respoct java naming convention
                            for (String newClassName__ : customNames.values()) {
                                Matcher m = p.matcher(newClassName__);
                                if (!m.find()) {
                                    response.type("text/plain; charset=utf-8");
                                    response.status(400);
                                    String reason = "'" + newClassName__ + "' is not a valid class name";
                                    response.body(reason);
                                    return reason;
                                }
                            }

                            // check if new Classes already exists in the project => conflict
                            String smaliFolderPath = Configurator.getInstance().getSmaliFolderPath(userUuid, project.getProjectFolderNameUuid(), false);
                            for (String classToBeRenamed : customNames.keySet()) {
                                String[] classToBeRenamedSplit = classToBeRenamed.split("\\.");
                                String classToBeRenamedFolder = String.join(File.separator, Arrays.copyOfRange(classToBeRenamedSplit, 0, classToBeRenamedSplit.length - 1));
                                String newClassFileName = customNames.get(classToBeRenamed);
                                File newClassFile = new File(smaliFolderPath + File.separator + classToBeRenamedFolder, newClassFileName + ".smali");
                                if (newClassFile.exists() && newClassFile.isFile()) {
                                    response.type("text/plain; charset=utf-8");
                                    response.status(409);
                                    String reason = "\"" + newClassFileName + "\" already exist!";
                                    response.body(reason);
                                    return reason;
                                }
                            }

                            Session session = EchoWebSocket.getUserSession(userUuid);
                            AppenderStreamer appenderStreamer = new AppenderStreamer(session, EnumerationApkTool.EnumLogType.EDITOR_LOG);
                            boolean isTemporary = false;
                            String projectFolderNameUuid = project.getProjectFolderNameUuid();

                            // create process for manifest entries renamer
                            UserProcessBuilder manifestEntriesRenamerProcessBuilder = new UserProcessBuilder(userUuid, EnumerationApkTool.EnumProcessType.MANIFEST_ENTRIES_RENAMER) {
                                @Override
                                public void buildProcessLogic() {
                                    try {
                                        // output logging to the web-socket
                                        if (session != null && session.isOpen()) {
                                            org.apache.log4j.Logger.getLogger(ApkToolsManager.class).addAppender(appenderStreamer);
                                            org.apache.log4j.Logger.getLogger(GraphManager.class).addAppender(appenderStreamer);
                                            org.apache.log4j.Logger.getLogger(FileComputingManager.class).addAppender(appenderStreamer);
                                            org.apache.log4j.Logger.getLogger(MassiveFileRenamer.class).addAppender(appenderStreamer);
                                        } else {
                                            if (session == null) {
                                                LOGGER.error("Null web socket");
                                                throw new IllegalArgumentException("WebSocket not found!");
                                            } else if (!session.isOpen()) {
                                                LOGGER.error("Web socket closed");
                                                throw new IllegalArgumentException("WebSocket closed!");
                                            }
                                        }
                                        ExecutionTimer timer = new ExecutionTimer();
                                        timer.start();
                                        SocketMessagingProtocol.getInstance()
                                                .sendProcessState(session, this.getId(),
                                                        EnumerationApkTool.EnumProcessState.STARTED, EnumerationApkTool.EnumProcessType.MANIFEST_ENTRIES_RENAMER);

                                        ApkToolsManager.getInstance().renameManifestEntries(userUuid, projectFolderNameUuid, isTemporary, customNames);

                                        timer.end();

                                        SocketMessagingProtocol.getInstance().sendLogEvent(session, " ", EnumerationApkTool.EnumLogType.EDITOR_LOG);
                                        SocketMessagingProtocol.getInstance().sendLogEvent(session, "###### MANIFEST ENTRIES RENAMER Task finished successfully in "
                                                + ExecutionTimer.getTimeString(timer.durationInSeconds()), EnumerationApkTool.EnumLogType.EDITOR_LOG);

                                        // notify process finished
                                        SocketMessagingProtocol.getInstance()
                                                .sendProcessState(session, this.getId(),
                                                        EnumerationApkTool.EnumProcessState.COMPLETED, EnumerationApkTool.EnumProcessType.MANIFEST_ENTRIES_RENAMER);

                                    } catch (Exception e) {
                                        System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                                        //e.printStackTrace();
                                        try {
                                            SocketMessagingProtocol.getInstance()
                                                    .sendLogEvent(session, e.getMessage(), EnumerationApkTool.EnumLogType.EDITOR_LOG);
                                            // notify process error
                                            SocketMessagingProtocol.getInstance()
                                                    .sendProcessState(session, this.getId(),
                                                            EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.MANIFEST_ENTRIES_RENAMER, "Please inspect logs for more details.");
                                        } catch (Exception ex) {
                                            // notify process error

                                            SocketMessagingProtocol.getInstance()
                                                    .sendProcessState(session, this.getId(),
                                                            EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.MANIFEST_ENTRIES_RENAMER, "Please inspect logs for more details.");
                                            LOGGER.error("Socket transmission error : " + ex.getMessage());
                                            //ex.printStackTrace();
                                        }
                                    } finally {
                                        try {
                                            org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                                            org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
                                            org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);
                                            org.apache.log4j.Logger.getLogger(MassiveFileRenamer.class).removeAppender(appenderStreamer);

                                            System.gc();
                                        } catch (Exception e1) {
                                            e1.printStackTrace();
                                        }
                                    }
                                }

                                @Override
                                public void cleanOperation() {
                                    // remove log appender
                                    try {
                                        org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                                        org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
                                        org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);
                                        org.apache.log4j.Logger.getLogger(MassiveFileRenamer.class).removeAppender(appenderStreamer);

                                        LOGGER.info("cleanOperation => log appender because of un-achieved operation");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            };
                            manifestEntriesRenamerProcessBuilder.execute();
                            response.status(200);
                            return manifestEntriesRenamerProcessBuilder.getId();
                        } else {
                            // do nothing => tell the client that it has nothing to do
                            LOGGER.info("Manifest Entries Renamer has nothing to do > customNames are equal to existing names!");
                            response.status(204);
                            return "";
                        }
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "NULL customNames can't be performed!";
                        response.body(reason);
                        return reason;
                    }
                }
                case "SUBMIT_APP_ICON_MODIFIER": {
                    ApkToolProjectBean project;
                    // check if that project exists and belongs to current user
                    if (ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
                        project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This project does not exists or does not belongs to you!";
                        response.body(reason);
                        return reason;
                    }

                    String savedTmpFilePathParam = request.queryParams("savedTmpFilePath");
                    // check if savedTmpFilePathParam is valid
                    if (savedTmpFilePathParam == null || "".equals(savedTmpFilePathParam)) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide an image file path!";
                        response.body(reason);
                        return reason;
                    }
                    String savedTmpFilePath = URLDecoder.decode(savedTmpFilePathParam, "UTF-8");

                    File savedTmpFile = new File(savedTmpFilePath);
                    if (!savedTmpFile.exists()) {
                        response.type("text/plain; charset=utf-8");
                        response.status(404);
                        String reason = "The image file does not exists in filesystem!";
                        response.body(reason);
                        return reason;
                    }

                    // modify icons => replace every app icon with the new one and keep old size
                    List<String> listUpdatedNodes = ApkToolsManager.getInstance().modifyAppIcons(userUuid,
                            project, graph, savedTmpFilePath);

                    // serialize the vertexes nodes and send them to client so he can update reload changed files in the project editor
                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                    String listUpdatedNodesAsJson = gson.toJson(listUpdatedNodes);

                    response.type("application/json; charset=UTF-8");
                    response.status(200);
                    return listUpdatedNodesAsJson;
                }
                case "SUBMIT_APP_NAME_MODIFIER": {
                    ApkToolProjectBean project;
                    // check if that project exists and belongs to current user
                    if (ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
                        project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This project does not exists or does not belongs to you!";
                        response.body(reason);
                        return reason;
                    }

                    String declaration = request.queryParams("declaration");
                    // check if declaration is valid
                    if (declaration == null || "".equals(declaration)) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "declaration parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    String tagName = request.queryParams("tagName");
                    // check if tagName is valid
                    if (tagName == null || "".equals(tagName)) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "tagName parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    String newAppNamesParam = request.queryParams("newAppNames");
                    // check if newAppNamesParam is valid
                    if (newAppNamesParam == null || "".equals(newAppNamesParam)) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "newAppNames parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    // convert newAppNames to json object
                    JSONArray newAppNamesJsonArray = (JSONArray) JSONValue.parse(newAppNamesParam);
                    //LOGGER.info("Received new app names JSON: " + newAppNamesJsonArray);

                    if (newAppNamesJsonArray != null) {
                        //
                        Map<String, String> newAppNames = new HashMap<>();
                        // keep only changed elements
                        for (Object newAppNameObj : newAppNamesJsonArray) {
                            JSONObject newAppNameJsonObj = (JSONObject) newAppNameObj;
                            String languageCode = newAppNameJsonObj.getAsString("language_code");
                            String oldAppName = newAppNameJsonObj.getAsString("old_app_name");
                            String newAppName = newAppNameJsonObj.getAsString("new_app_name");

                            if (!oldAppName.equals(newAppName)) {
                                newAppNames.put(languageCode, newAppName);
                            }
                        }

                        //LOGGER.info("Kept newAppNames : " + newAppNames);

                        if (newAppNames.size() > 0) {
                            // modify icons => replace every app icon with the new one and keep old size
                            List<String> listUpdatedNodes = ApkToolsManager.getInstance().modifyAppName(userUuid,
                                    project, graph, newAppNames, declaration, tagName);

                            // serialize the vertexes nodes and send them to client so he can update reload changed files in the project editor
                            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                            String listUpdatedNodesAsJson = gson.toJson(listUpdatedNodes);

                            response.type("application/json; charset=UTF-8");
                            response.status(200);
                            return listUpdatedNodesAsJson;
                        } else {
                            response.status(204);
                            return "";
                        }
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "NULL newAppNames can't be performed!";
                        response.body(reason);
                        return reason;
                    }
                }
                case "SUBMIT_BUILD_APK_DEBUG": {
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
                    ApkToolProjectBean project;
                    // check if that project exists and belongs to current user
                    if (ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
                        project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This project does not exists or does not belongs to you!";
                        response.body(reason);
                        return reason;
                    }

                    Session session = EchoWebSocket.getUserSession(userUuid);
                    AppenderStreamer appenderStreamer = new AppenderStreamer(session, EnumerationApkTool.EnumLogType.EDITOR_LOG);
                    HandlerStreamer handlerStreamer = new HandlerStreamer(session, EnumerationApkTool.EnumLogType.EDITOR_LOG);
                    boolean isTemporary = false;
                    String projectFolderNameUuid = project.getProjectFolderNameUuid();
                    SocketPrintStream socketErrPrintStream = new SocketPrintStream(System.err, session, EnumerationApkTool.EnumLogType.EDITOR_LOG);

                    // create process for debug apk builder
                    UserProcessBuilder apkForDebugProcessBuilder = new UserProcessBuilder(userUuid, EnumerationApkTool.EnumProcessType.BUILD_DEBUG_APK) {
                        @Override
                        public void buildProcessLogic() {
                            try {
                                // output logging to the web-socket
                                if (session != null && session.isOpen()) {
                                    System.setErr(socketErrPrintStream);

                                    java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
                                    rootLogger.setLevel(Level.WARNING);
                                    rootLogger.addHandler(handlerStreamer);

                                    java.util.logging.Logger androlibLogger = java.util.logging.Logger.getLogger(Androlib.class.getName());
                                    androlibLogger.setLevel(Level.ALL);
                                    androlibLogger.setUseParentHandlers(false);
                                    androlibLogger.addHandler(handlerStreamer);

                                    java.util.logging.Logger androlibResourcesLogger = java.util.logging.Logger.getLogger(AndrolibResources.class.getName());
                                    androlibResourcesLogger.setLevel(Level.ALL);
                                    androlibResourcesLogger.setUseParentHandlers(false);
                                    androlibResourcesLogger.addHandler(handlerStreamer);

                                    org.apache.log4j.Logger.getLogger(ApkToolsManager.class).addAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(GraphManager.class).addAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(FileComputingManager.class).addAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(SignTool.class).addAppender(appenderStreamer);
                                } else {
                                    if (session == null) {
                                        LOGGER.error("Null web socket");
                                        throw new IllegalArgumentException("WebSocket not found!");
                                    } else if (!session.isOpen()) {
                                        LOGGER.error("Web socket closed");
                                        throw new IllegalArgumentException("WebSocket closed!");
                                    }
                                }
                                ExecutionTimer timer = new ExecutionTimer();
                                timer.start();
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.STARTED, EnumerationApkTool.EnumProcessType.BUILD_DEBUG_APK);

                                // update apk file name set it to buildApk.apk
                                String decodedApkFolderPath = Configurator.getInstance()
                                        .getDecodedApkFolderPath(userUuid, project.getProjectFolderNameUuid(), isTemporary);
                                ApkToolsManager.getInstance().updateAppInfoWithNewApkFileNameInYaml(decodedApkFolderPath, "debugApk.apk");

                                // delete dist folder if already exist
                                String targetApkFileOrFolderPath = Configurator.getInstance().getDistFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                                File distFolder = new File(targetApkFileOrFolderPath);
                                if (distFolder.exists() && distFolder.isDirectory()) {
                                    try {
                                        FileUtils.deleteDirectory(distFolder);
                                    } catch (IOException ex1) {
                                        try {
                                            FileUtils.deleteDirectory(distFolder);
                                        } catch (IOException ex2) {
                                            // do nothing
                                        }
                                    }
                                }


                                // delete build folder if already exist
                                String buildFolderPath = Configurator.getInstance().getBuildFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                                File buildFolder = new File(buildFolderPath);
                                if (buildFolder.exists() && buildFolder.isDirectory()) {
                                    try {
                                        FileUtils.deleteDirectory(buildFolder);
                                    } catch (IOException ex1) {
                                        try {
                                            FileUtils.deleteDirectory(buildFolder);
                                        } catch (IOException ex2) {
                                            // do nothing
                                        }
                                    }
                                }

                                // build new Apk
                                ApkToolsManager.getInstance().buildApk(userUuid, projectFolderNameUuid, isTemporary);

                                // sign apk with debug keystore
                                String destSignedApkFolder = Configurator.getInstance().getGenFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                                ApkToolsManager.getInstance().signAndZipAlignApkForDebug(targetApkFileOrFolderPath, destSignedApkFolder, true);
                                //ApkToolsManager.getInstance().verifyApk(destSignedApkFolder, false);

                                timer.end();

                                SocketMessagingProtocol.getInstance().sendLogEvent(session, " ", EnumerationApkTool.EnumLogType.EDITOR_LOG);
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "###### BUILD APK for DEBUG Task finished successfully in "
                                        + ExecutionTimer.getTimeString(timer.durationInSeconds()), EnumerationApkTool.EnumLogType.EDITOR_LOG);

                                // notify process finished
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.COMPLETED, EnumerationApkTool.EnumProcessType.BUILD_DEBUG_APK);

                            } catch (Exception e) {
                                System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                                //e.printStackTrace();
                                try {
                                    SocketMessagingProtocol.getInstance()
                                            .sendLogEvent(session, e.getMessage(), EnumerationApkTool.EnumLogType.EDITOR_LOG);
                                    // notify process error
                                    SocketMessagingProtocol.getInstance()
                                            .sendProcessState(session, this.getId(),
                                                    EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.BUILD_DEBUG_APK, "Please inspect logs for more details.");
                                } catch (Exception ex) {
                                    // notify process error

                                    SocketMessagingProtocol.getInstance()
                                            .sendProcessState(session, this.getId(),
                                                    EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.BUILD_DEBUG_APK, "Please inspect logs for more details.");
                                    LOGGER.error("Socket transmission error : " + ex.getMessage());
                                    //ex.printStackTrace();
                                }
                            } finally {
                                try {
                                    java.util.logging.Logger.getLogger(Androlib.class.getName()).removeHandler(handlerStreamer);
                                    java.util.logging.Logger.getLogger(AndrolibResources.class.getName()).removeHandler(handlerStreamer);
                                    java.util.logging.Logger.getLogger("").removeHandler(handlerStreamer);

                                    org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(SignTool.class).removeAppender(appenderStreamer);

                                    socketErrPrintStream.close();
                                    System.setErr(System.err);

                                    System.gc();
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void cleanOperation() {

                            try {
                                // delete dist folder if already exist
                                String targetApkFileOrFolderPath = Configurator.getInstance().getDistFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                                File distFolder = new File(targetApkFileOrFolderPath);
                                if (distFolder.exists() && distFolder.isDirectory()) {
                                    try {
                                        LOGGER.info("cleanOperation => removing dist folder because of un-achieved operation");
                                        FileUtils.deleteDirectory(distFolder);
                                    } catch (IOException ex1) {
                                        try {
                                            FileUtils.deleteDirectory(distFolder);
                                        } catch (IOException ex2) {
                                            // do nothing
                                        }
                                    }
                                }

                                // delete build folder if already exist
                                String buildFolderPath = Configurator.getInstance().getBuildFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                                File buildFolder = new File(buildFolderPath);
                                if (buildFolder.exists() && buildFolder.isDirectory()) {
                                    try {
                                        LOGGER.info("cleanOperation => removing build folder because of un-achieved operation");
                                        FileUtils.deleteDirectory(buildFolder);
                                    } catch (IOException ex1) {
                                        try {
                                            FileUtils.deleteDirectory(buildFolder);
                                        } catch (IOException ex2) {
                                            // do nothing
                                        }
                                    }
                                }

                                // remove log appender
                                LOGGER.info("cleanOperation => log appender because of un-achieved operation");
                                java.util.logging.Logger.getLogger(Androlib.class.getName()).removeHandler(handlerStreamer);
                                java.util.logging.Logger.getLogger(AndrolibResources.class.getName()).removeHandler(handlerStreamer);
                                java.util.logging.Logger.getLogger("").removeHandler(handlerStreamer);

                                org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(SignTool.class).removeAppender(appenderStreamer);

                                socketErrPrintStream.close();
                                System.setErr(System.err);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    };
                    apkForDebugProcessBuilder.execute();
                    //res.put("processId", apkForDebugProcessBuilder.getId());
                    response.status(204);
                    return "";
                }
                case "SUBMIT_BUILD_APK_RELEASE": {
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

                    String apkName = request.queryParams("apkName");
                    // check if apkName is valid
                    if (apkName == null || apkName.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Apk name parameter is empty!";
                        response.body(reason);
                        return reason;
                    }

                    // check if apkName is valid using regex
                    Matcher matcher = regexInvalidFileName.matcher(apkName);
                    if (matcher.matches()) { // apkName is not valid
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Invalid apk name, it must not contain: , ; . ? | / \\ ' < > ^ * % ! \" space tabulation";
                        response.body(reason);
                        return reason;
                    }

                    String keystoreUuid = request.queryParams("keystoreUuid");
                    // check if keystoreUuid is valid
                    if (keystoreUuid == null || keystoreUuid.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Please select a keystore!";
                        response.body(reason);
                        return reason;
                    }

                    // check keystore bean exists in database
                    KeystoreBean ksBean = KeystoreDao.getInstance().getByUuid(keystoreUuid);
                    if (ksBean == null) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "The selected keystore does not exists in database!";
                        response.body(reason);
                        return reason;
                    }

                    ApkToolProjectBean project;
                    // check if that project exists and belongs to current user
                    if (ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
                        project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This project does not exists or does not belongs to you!";
                        response.body(reason);
                        return reason;
                    }

                    Session session = EchoWebSocket.getUserSession(userUuid);
                    AppenderStreamer appenderStreamer = new AppenderStreamer(session, EnumerationApkTool.EnumLogType.EDITOR_LOG);
                    HandlerStreamer handlerStreamer = new HandlerStreamer(session, EnumerationApkTool.EnumLogType.EDITOR_LOG);
                    boolean isTemporary = false;
                    String projectFolderNameUuid = project.getProjectFolderNameUuid();
                    SocketPrintStream socketErrPrintStream = new SocketPrintStream(System.err, session, EnumerationApkTool.EnumLogType.EDITOR_LOG);
                    File tempJksFile = File.createTempFile("tmp-ks-", ".jks"); // create tmp file for keystore from bytes

                    // create process for release apk builder
                    UserProcessBuilder apkForReleaseProcessBuilder = new UserProcessBuilder(userUuid, EnumerationApkTool.EnumProcessType.BUILD_RELEASE_APK) {
                        @Override
                        public void buildProcessLogic() {
                            try {
                                // output logging to the web-socket
                                if (session != null && session.isOpen()) {
                                    System.setErr(socketErrPrintStream);

                                    java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
                                    rootLogger.setLevel(Level.WARNING);
                                    rootLogger.addHandler(handlerStreamer);

                                    java.util.logging.Logger androlibLogger = java.util.logging.Logger.getLogger(Androlib.class.getName());
                                    androlibLogger.setLevel(Level.FINE);
                                    androlibLogger.setUseParentHandlers(false);
                                    androlibLogger.addHandler(handlerStreamer);

                                    java.util.logging.Logger androlibResourcesLogger = java.util.logging.Logger.getLogger(AndrolibResources.class.getName());
                                    androlibResourcesLogger.setLevel(Level.FINE);
                                    androlibResourcesLogger.setUseParentHandlers(false);
                                    androlibResourcesLogger.addHandler(handlerStreamer);

                                    org.apache.log4j.Logger.getLogger(ApkToolsManager.class).addAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(GraphManager.class).addAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(FileComputingManager.class).addAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(SignTool.class).addAppender(appenderStreamer);
                                } else {
                                    if (session == null) {
                                        LOGGER.error("Null web socket");
                                        throw new IllegalArgumentException("WebSocket not found!");
                                    } else if (!session.isOpen()) {
                                        LOGGER.error("Web socket closed");
                                        throw new IllegalArgumentException("WebSocket closed!");
                                    }
                                }
                                ExecutionTimer timer = new ExecutionTimer();
                                timer.start();
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.STARTED, EnumerationApkTool.EnumProcessType.BUILD_RELEASE_APK);

                                // update apk file name set it to apkName.apk
                                String decodedApkFolderPath = Configurator.getInstance()
                                        .getDecodedApkFolderPath(userUuid, project.getProjectFolderNameUuid(), isTemporary);
                                ApkToolsManager.getInstance().updateAppInfoWithNewApkFileNameInYaml(decodedApkFolderPath, apkName + ".apk");

                                // delete dist folder if already exist
                                String targetApkFileOrFolderPath = Configurator.getInstance().getDistFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                                File distFolder = new File(targetApkFileOrFolderPath);
                                if (distFolder.exists() && distFolder.isDirectory()) {
                                    try {
                                        FileUtils.deleteDirectory(distFolder);
                                    } catch (IOException ex1) {
                                        try {
                                            FileUtils.deleteDirectory(distFolder);
                                        } catch (IOException ex2) {
                                            // do nothing
                                        }
                                    }
                                }


                                // delete build folder if already exist
                                String buildFolderPath = Configurator.getInstance().getBuildFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                                File buildFolder = new File(buildFolderPath);
                                if (buildFolder.exists() && buildFolder.isDirectory()) {
                                    try {
                                        FileUtils.deleteDirectory(buildFolder);
                                    } catch (IOException ex1) {
                                        try {
                                            FileUtils.deleteDirectory(buildFolder);
                                        } catch (IOException ex2) {
                                            // do nothing
                                        }
                                    }
                                }
                                // build new Apk
                                ApkToolsManager.getInstance().buildApk(userUuid, projectFolderNameUuid, isTemporary);


                                // sign apk with release keystore
                                String destSignedApkFolder = Configurator.getInstance().getGenFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                                FileUtils.writeByteArrayToFile(tempJksFile, ksBean.getBlob());


                                ApkToolsManager.getInstance().signAndZipAlignApkForRelease(targetApkFileOrFolderPath, destSignedApkFolder,
                                        tempJksFile.getPath(), ksBean.getAlias(), ksBean.getKsPass(), ksBean.getKeyPass(), true);
                                //ApkToolsManager.getInstance().verifyApk(destSignedApkFolder, false);

                                timer.end();

                                SocketMessagingProtocol.getInstance().sendLogEvent(session, " ", EnumerationApkTool.EnumLogType.EDITOR_LOG);
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "###### BUILD APK for RELEASE Task finished successfully in "
                                        + ExecutionTimer.getTimeString(timer.durationInSeconds()), EnumerationApkTool.EnumLogType.EDITOR_LOG);

                                // notify process finished
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.COMPLETED, EnumerationApkTool.EnumProcessType.BUILD_RELEASE_APK);

                            } catch (Exception e) {
                                System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                                //e.printStackTrace();
                                try {
                                    SocketMessagingProtocol.getInstance()
                                            .sendLogEvent(session, e.getMessage(), EnumerationApkTool.EnumLogType.EDITOR_LOG);
                                    // notify process error
                                    SocketMessagingProtocol.getInstance()
                                            .sendProcessState(session, this.getId(),
                                                    EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.BUILD_RELEASE_APK, "Please inspect logs for more details.");
                                } catch (Exception ex) {
                                    // notify process error

                                    SocketMessagingProtocol.getInstance()
                                            .sendProcessState(session, this.getId(),
                                                    EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.BUILD_RELEASE_APK, "Please inspect logs for more details.");
                                    LOGGER.error("Socket transmission error : " + ex.getMessage());
                                    //ex.printStackTrace();
                                }
                            } finally {
                                try {
                                    if (tempJksFile.exists()) {
                                        try {
                                            FileUtils.deleteQuietly(tempJksFile);
                                        } catch (Exception e) {
                                            FileUtils.forceDeleteOnExit(tempJksFile);
                                        }
                                    }
                                    java.util.logging.Logger.getLogger(Androlib.class.getName()).removeHandler(handlerStreamer);
                                    java.util.logging.Logger.getLogger(AndrolibResources.class.getName()).removeHandler(handlerStreamer);
                                    java.util.logging.Logger.getLogger("").removeHandler(handlerStreamer);

                                    org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(SignTool.class).removeAppender(appenderStreamer);

                                    socketErrPrintStream.close();
                                    System.setErr(System.err);

                                    System.gc();
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void cleanOperation() {
                            try {
                                // delete dist folder if already exist
                                String targetApkFileOrFolderPath = Configurator.getInstance().getDistFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                                File distFolder = new File(targetApkFileOrFolderPath);
                                if (distFolder.exists() && distFolder.isDirectory()) {
                                    try {
                                        LOGGER.info("cleanOperation => removing dist folder because of un-achieved operation");
                                        FileUtils.deleteDirectory(distFolder);
                                    } catch (IOException ex1) {
                                        try {
                                            FileUtils.deleteDirectory(distFolder);
                                        } catch (IOException ex2) {
                                            // do nothing
                                        }
                                    }
                                }

                                // delete build folder if already exist
                                String buildFolderPath = Configurator.getInstance().getBuildFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                                File buildFolder = new File(buildFolderPath);
                                if (buildFolder.exists() && buildFolder.isDirectory()) {
                                    try {
                                        LOGGER.info("cleanOperation => removing build folder because of un-achieved operation");
                                        FileUtils.deleteDirectory(buildFolder);
                                    } catch (IOException ex1) {
                                        try {
                                            FileUtils.deleteDirectory(buildFolder);
                                        } catch (IOException ex2) {
                                            // do nothing
                                        }
                                    }
                                }
                                // delete temporary keystore file
                                if (tempJksFile.exists()) {
                                    try {
                                        FileUtils.deleteQuietly(tempJksFile);
                                    } catch (Exception e) {
                                        FileUtils.forceDeleteOnExit(tempJksFile);
                                    }
                                }
                                // remove log appender
                                LOGGER.info("cleanOperation => log appender because of un-achieved operation");
                                java.util.logging.Logger.getLogger(Androlib.class.getName()).removeHandler(handlerStreamer);
                                java.util.logging.Logger.getLogger(AndrolibResources.class.getName()).removeHandler(handlerStreamer);
                                java.util.logging.Logger.getLogger("").removeHandler(handlerStreamer);

                                org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(SignTool.class).removeAppender(appenderStreamer);

                                socketErrPrintStream.close();
                                System.setErr(System.err);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    };
                    apkForReleaseProcessBuilder.execute();
                    //res.put("processId", apkForDebugProcessBuilder.getId());
                    response.status(204);
                    return "";

                }
                case "REMOVE_APK": {
                    String apkFileName = request.queryParams("apkFileName");
                    // check if apkName is valid
                    if (apkFileName == null || apkFileName.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Apk file name parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    ApkToolProjectBean project;
                    // check if that project exists and belongs to current user
                    if (ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
                        project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This project does not exists or does not belongs to you!";
                        response.body(reason);
                        return reason;
                    }

                    String genApkFolderPath = Configurator.getInstance().getGenFolderPath(userUuid, project.getProjectFolderNameUuid(), false);

                    File apkFileToRemove = new File(genApkFolderPath, apkFileName);
                    if (apkFileToRemove.exists()) {
                        FileUtils.deleteQuietly(apkFileToRemove);
                    }

                    response.status(204);
                    return "";
                }
                case "GET_APK_INFO": {
                    String apkFileName = request.queryParams("apkFileName");
                    if (apkFileName == null || apkFileName.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Apk file name parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    String apkType = request.queryParams("apkType");
                    if (apkType == null || apkType.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "apkType parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    ApkToolProjectBean project;
                    // check if that project exists and belongs to current user
                    if (ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
                        project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This project does not exists or does not belongs to you!";
                        response.body(reason);
                        return reason;
                    }

                    String apkFolderPath;
                    switch (apkType){
                        case "orig":{
                            apkFolderPath = Configurator.getInstance().getSrcApkFolder(userUuid, project.getProjectFolderNameUuid(), false);
                            break;
                        }
                        case "gen":{
                            apkFolderPath = Configurator.getInstance().getGenFolderPath(userUuid, project.getProjectFolderNameUuid(), false);
                            break;
                        }
                        default:{
                            response.type("text/plain; charset=utf-8");
                            response.status(400);
                            String reason = "Unknown apkType parameter '"+apkType+"'";
                            response.body(reason);
                            return reason;
                        }
                    }

                    File apkFile = new File(apkFolderPath, apkFileName);
                    if (!apkFile.exists()) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Apk file '" + apkFileName + "' does not exists!";
                        response.body(reason);
                        return reason;
                    }

                    Map<String, Object> info = ApkToolsManager.getInstance().getAppInfoFromApk(apkFile.getPath());
                    response.status(200);
                    JSONObject res = new JSONObject(info);
                    return res.toJSONString();
                }
                case "INSTALL_APK_DEVICE": {
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

                    String apkFileName = request.queryParams("apkFileName");
                    // check if apkName is valid
                    if (apkFileName == null || apkFileName.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Apk file name parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    String apkType = request.queryParams("apkType");
                    if (apkType == null || apkType.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "apkType parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    ApkToolProjectBean project;
                    // check if that project exists and belongs to current user
                    if (ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
                        project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This project does not exists or does not belongs to you!";
                        response.body(reason);
                        return reason;
                    }

                    String apkFolderPath;
                    switch (apkType){
                        case "orig":{
                            apkFolderPath = Configurator.getInstance().getSrcApkFolder(userUuid, project.getProjectFolderNameUuid(), false);
                            break;
                        }
                        case "gen":{
                            apkFolderPath = Configurator.getInstance().getGenFolderPath(userUuid, project.getProjectFolderNameUuid(), false);
                            break;
                        }
                        default:{
                            response.type("text/plain; charset=utf-8");
                            response.status(400);
                            String reason = "Unknown apkType parameter '"+apkType+"'";
                            response.body(reason);
                            return reason;
                        }
                    }

                    File apkFile = new File(apkFolderPath, apkFileName);
                    if (!apkFile.exists()) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Apk file '" + apkFileName + "' does not exists!";
                        response.body(reason);
                        return reason;
                    }

                    String packageName = AnalysisApk.getPackageNameFromApkFile(apkFile.getPath());
                    Session session = EchoWebSocket.getUserSession(userUuid);
                    AppenderStreamer appenderStreamer = new AppenderStreamer(session, EnumerationApkTool.EnumLogType.EDITOR_LOG);

                    UserProcessBuilder adbInstallProcessBuilder = new UserProcessBuilder(userUuid, EnumerationApkTool.EnumProcessType.ADB_INSTALL) {
                        @Override
                        public void buildProcessLogic() {
                            try {
                                // output logging to the web-socket
                                if (session != null && session.isOpen()) {
                                    org.apache.log4j.Logger adbLogger = org.apache.log4j.Logger.getLogger(AdbManager.class);
                                    adbLogger.setAdditivity(false);
                                    adbLogger.addAppender(appenderStreamer);
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
                                                EnumerationApkTool.EnumProcessState.STARTED, EnumerationApkTool.EnumProcessType.ADB_INSTALL);

                                ExecutionTimer timer = new ExecutionTimer();
                                timer.start();

                                // check adb is installed, if not installed, then install it first
                                if (!AdbManager.getInstance().isAdbInstalled(userUuid)) {
                                    AdbManager.getInstance().reInstallAdb();
                                }

                                this.addProcess(AdbManager.getInstance().startAdbServer(userUuid));
                                AdbManager.getInstance().waitForDevice(userUuid);

                                if (packageName != null) {
                                    this.addProcess(AdbManager.getInstance().adbUninstall(packageName, userUuid));
                                }
                                this.addProcess(AdbManager.getInstance().wireInstallApkOnDevice(apkFile.getPath(), userUuid));
                                if (packageName != null) {
                                    this.addProcess(AdbManager.getInstance().launchApp(packageName, userUuid));
                                }

                                // log event finished
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, " ", EnumerationApkTool.EnumLogType.EDITOR_LOG);
                                timer.end();
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "###### Task APK INSTALL finished successfully in "
                                        + ExecutionTimer.getTimeString(timer.durationInSeconds()), EnumerationApkTool.EnumLogType.EDITOR_LOG);

                                // notify process finished
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.COMPLETED, EnumerationApkTool.EnumProcessType.ADB_INSTALL);
                            } catch (Exception e) {
                                System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                                //e.printStackTrace();

                                SocketMessagingProtocol.getInstance()
                                        .sendLogEvent(session, StringUtil.escape(e.getMessage()), EnumerationApkTool.EnumLogType.EDITOR_LOG);
                                // notify process error
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.ADB_INSTALL, e.getMessage());

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
                    //res.put("processId", adbInstallProcessBuilder.getId());
                    response.status(204);
                    return "";
                }
                case "INSTANT_RUN": {
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


                    ApkToolProjectBean project;
                    // check if that project exists and belongs to current user
                    if (ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
                        project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
                    } else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "This project does not exists or does not belongs to you!";
                        response.body(reason);
                        return reason;
                    }

                    Session session = EchoWebSocket.getUserSession(userUuid);
                    AppenderStreamer appenderStreamer = new AppenderStreamer(session, EnumerationApkTool.EnumLogType.EDITOR_LOG);
                    HandlerStreamer handlerStreamer = new HandlerStreamer(session, EnumerationApkTool.EnumLogType.EDITOR_LOG);
                    boolean isTemporary = false;
                    String projectFolderNameUuid = project.getProjectFolderNameUuid();

                    SocketPrintStream socketErrPrintStream = new SocketPrintStream(System.err, session, EnumerationApkTool.EnumLogType.EDITOR_LOG);


                    // create process for instant run
                    UserProcessBuilder instantRunProcessBuilder = new UserProcessBuilder(userUuid, EnumerationApkTool.EnumProcessType.INSTANT_RUN) {
                        @Override
                        public void buildProcessLogic() {
                            try {
                                // output logging to the web-socket
                                if (session != null && session.isOpen()) {
                                    System.setErr(socketErrPrintStream);

                                    java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
                                    rootLogger.setLevel(Level.WARNING);
                                    rootLogger.addHandler(handlerStreamer);

                                    java.util.logging.Logger androlibLogger = java.util.logging.Logger.getLogger(Androlib.class.getName());
                                    androlibLogger.setLevel(Level.FINE);
                                    androlibLogger.setUseParentHandlers(false);
                                    androlibLogger.addHandler(handlerStreamer);

                                    java.util.logging.Logger androlibResourcesLogger = java.util.logging.Logger.getLogger(AndrolibResources.class.getName());
                                    androlibResourcesLogger.setLevel(Level.FINE);
                                    androlibResourcesLogger.setUseParentHandlers(false);
                                    androlibResourcesLogger.addHandler(handlerStreamer);

                                    org.apache.log4j.Logger.getLogger(ApkToolsManager.class).addAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(GraphManager.class).addAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(FileComputingManager.class).addAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(SignTool.class).addAppender(appenderStreamer);

                                    org.apache.log4j.Logger adbLogger = org.apache.log4j.Logger.getLogger(AdbManager.class);
                                    adbLogger.setAdditivity(false);
                                    adbLogger.addAppender(appenderStreamer);
                                } else {
                                    if (session == null) {
                                        LOGGER.error("Null web socket");
                                        throw new IllegalArgumentException("WebSocket not found!");
                                    } else if (!session.isOpen()) {
                                        LOGGER.error("Web socket closed");
                                        throw new IllegalArgumentException("WebSocket closed!");
                                    }
                                }
                                ExecutionTimer timer = new ExecutionTimer();
                                timer.start();


                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.STARTED, EnumerationApkTool.EnumProcessType.INSTANT_RUN);

                                // check adb is installed
                                if (!AdbManager.getInstance().isAdbInstalled(userUuid)) {
                                    AdbManager.getInstance().reInstallAdb();
                                }
                                // start adb and wait for user's device
                                this.addProcess(AdbManager.getInstance().startAdbServer(userUuid));
                                AdbManager.getInstance().waitForDevice(userUuid);

                                // update apk file name set it to instantRun.apk
                                String decodedApkFolderPath = Configurator.getInstance()
                                        .getDecodedApkFolderPath(userUuid, project.getProjectFolderNameUuid(), isTemporary);
                                ApkToolsManager.getInstance().updateAppInfoWithNewApkFileNameInYaml(decodedApkFolderPath, "instantRun.apk");

                                // delete dist folder if already exist
                                String targetApkFileOrFolderPath = Configurator.getInstance().getDistFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                                File distFolder = new File(targetApkFileOrFolderPath);
                                if (distFolder.exists() && distFolder.isDirectory()) {
                                    try {
                                        FileUtils.deleteDirectory(distFolder);
                                    } catch (IOException ex1) {
                                        try {
                                            FileUtils.deleteDirectory(distFolder);
                                        } catch (IOException ex2) {
                                            // do nothing
                                        }
                                    }
                                }

                                // delete build folder if already exist
                                String buildFolderPath = Configurator.getInstance().getBuildFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                                File buildFolder = new File(buildFolderPath);
                                if (buildFolder.exists() && buildFolder.isDirectory()) {
                                    try {
                                        FileUtils.deleteDirectory(buildFolder);
                                    } catch (IOException ex1) {
                                        try {
                                            FileUtils.deleteDirectory(buildFolder);
                                        } catch (IOException ex2) {
                                            // do nothing
                                        }
                                    }
                                }

                                // build new instantRun.Apk
                                ApkToolsManager.getInstance().buildApk(userUuid, projectFolderNameUuid, isTemporary);

                                // sign apk with debug keystore
                                String destSignedApkFolder = Configurator.getInstance().getGenFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                                ApkToolsManager.getInstance().signAndZipAlignApkForDebug(targetApkFileOrFolderPath, destSignedApkFolder, true);

                                // install on device
                                File apkFile = new File(destSignedApkFolder, "instantRun-aligned-debugSigned.apk");
                                if (!apkFile.exists()) {
                                    throw new IllegalStateException("Apk file for instant run not found!");
                                }

                                String packageName = AnalysisApk.getPackageNameFromApkFile(apkFile.getPath());
                                if (packageName != null) {
                                    this.addProcess(AdbManager.getInstance().adbUninstall(packageName, userUuid));
                                }
                                this.addProcess(AdbManager.getInstance().wireInstallApkOnDevice(apkFile.getPath(), userUuid));
                                if (packageName != null) {
                                    this.addProcess(AdbManager.getInstance().launchApp(packageName, userUuid));
                                }

                                timer.end();

                                SocketMessagingProtocol.getInstance().sendLogEvent(session, " ", EnumerationApkTool.EnumLogType.EDITOR_LOG);
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "###### INSTANT RUN Task finished successfully in "
                                        + ExecutionTimer.getTimeString(timer.durationInSeconds()), EnumerationApkTool.EnumLogType.EDITOR_LOG);

                                // notify process finished
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.COMPLETED, EnumerationApkTool.EnumProcessType.INSTANT_RUN);

                            } catch (Exception e) {
                                System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                                //e.printStackTrace();
                                try {
                                    SocketMessagingProtocol.getInstance()
                                            .sendLogEvent(session, e.getMessage(), EnumerationApkTool.EnumLogType.EDITOR_LOG);
                                    // notify process error
                                    SocketMessagingProtocol.getInstance()
                                            .sendProcessState(session, this.getId(),
                                                    EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.INSTANT_RUN, "Please inspect logs for more details.");
                                } catch (Exception ex) {
                                    // notify process error

                                    SocketMessagingProtocol.getInstance()
                                            .sendProcessState(session, this.getId(),
                                                    EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.INSTANT_RUN, "Please inspect logs for more details.");
                                    LOGGER.error("Socket transmission error : " + ex.getMessage());
                                    //ex.printStackTrace();
                                }
                            } finally {
                                try {
                                    java.util.logging.Logger.getLogger(Androlib.class.getName()).removeHandler(handlerStreamer);
                                    java.util.logging.Logger.getLogger(AndrolibResources.class.getName()).removeHandler(handlerStreamer);
                                    java.util.logging.Logger.getLogger("").removeHandler(handlerStreamer);

                                    org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(SignTool.class).removeAppender(appenderStreamer);
                                    org.apache.log4j.Logger.getLogger(AdbManager.class).removeAppender(appenderStreamer);

                                    socketErrPrintStream.close();
                                    System.setErr(System.err);

                                    System.gc();
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void cleanOperation() {
                            try {
                                // delete dist folder if already exist
                                String targetApkFileOrFolderPath = Configurator.getInstance().getDistFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                                File distFolder = new File(targetApkFileOrFolderPath);
                                if (distFolder.exists() && distFolder.isDirectory()) {
                                    try {
                                        LOGGER.info("cleanOperation => removing dist folder because of un-achieved operation");
                                        FileUtils.deleteDirectory(distFolder);
                                    } catch (IOException ex1) {
                                        try {
                                            FileUtils.deleteDirectory(distFolder);
                                        } catch (IOException ex2) {
                                            // do nothing
                                        }
                                    }
                                }

                                // delete build folder if already exist
                                String buildFolderPath = Configurator.getInstance().getBuildFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                                File buildFolder = new File(buildFolderPath);
                                if (buildFolder.exists() && buildFolder.isDirectory()) {
                                    try {
                                        LOGGER.info("cleanOperation => removing build folder because of un-achieved operation");
                                        FileUtils.deleteDirectory(buildFolder);
                                    } catch (IOException ex1) {
                                        try {
                                            FileUtils.deleteDirectory(buildFolder);
                                        } catch (IOException ex2) {
                                            // do nothing
                                        }
                                    }
                                }

                                // remove log appender
                                LOGGER.info("cleanOperation => log appender because of un-achieved operation");
                                java.util.logging.Logger.getLogger(Androlib.class.getName()).removeHandler(handlerStreamer);
                                java.util.logging.Logger.getLogger(AndrolibResources.class.getName()).removeHandler(handlerStreamer);
                                java.util.logging.Logger.getLogger("").removeHandler(handlerStreamer);

                                org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(SignTool.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(AdbManager.class).removeAppender(appenderStreamer);

                                socketErrPrintStream.close();
                                System.setErr(System.err);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    };
                    instantRunProcessBuilder.execute();
                    //res.put("processId", apkForDebugProcessBuilder.getId());
                    response.status(204);
                    return "";
                }
                default: {
                    response.type("text/plain; charset=utf-8");
                    response.status(400);
                    String reason = "Unknown tools action parameter!";
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
