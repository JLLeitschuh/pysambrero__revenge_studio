package com.ninjaflip.androidrevenge.core.service.handler.apktools.projects;

import brut.androlib.Androlib;
import brut.androlib.res.AndrolibResources;
import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.ApkToolsManager;
import com.ninjaflip.androidrevenge.core.apktool.UserProcessBuilder;
import com.ninjaflip.androidrevenge.enums.EnumerationApkTool;
import com.ninjaflip.androidrevenge.core.apktool.apkinfo.AnalysisApk;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.AppenderStreamer;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.HandlerStreamer;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.SocketMessagingProtocol;
import com.ninjaflip.androidrevenge.beans.ApkToolProjectBean;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import com.ninjaflip.androidrevenge.beans.UserBean;
import com.ninjaflip.androidrevenge.core.db.dao.UserDao;
import com.ninjaflip.androidrevenge.websocket.EchoWebSocket;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Solitario on 04/08/2017.
 * <p>
 * handle creation of new projects from an apk file
 */
public class CreateNewProjectHandler implements Route {

    private final static Logger LOGGER = Logger.getLogger(CreateNewProjectHandler.class);

    @Override
    public Object handle(Request request, Response response) {
        try {
            String userUuid = ServerSecurityManager.getInstance()
                    .getUserUuidFromToken(request.cookie("token"));

            // get parameters : apkPath + projectName
            String apkTmpFilePathParam = request.queryParams("apkTmpFilePath");
            String projectNameParam = request.queryParams("projectName");

            // validate project name
            if (apkTmpFilePathParam == null || apkTmpFilePathParam.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "Apk file path parameter not found!";
                response.body(reason);
                return reason;
            }

            // validate project name
            if (projectNameParam == null || projectNameParam.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "Project name parameter not found!";
                response.body(reason);
                return reason;
            }


            String apkTmpFilePath = URLDecoder.decode(apkTmpFilePathParam, "UTF-8");
            String projectName = URLDecoder.decode(projectNameParam, "UTF-8").trim();

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

            // check if apk file exist
            File apkTmpFile = new File(apkTmpFilePath);
            if (!apkTmpFile.exists()) {
                response.type("text/plain; charset=utf-8");
                response.status(404);
                String reason = "APK file was not found in the specified location!";
                response.body(reason);
                return reason;
            }


            if (projectName.length() > 30 || projectName.length() < 5) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "Project name must be between 5 and 30 characters long!";
                response.body(reason);
                return reason;
            }

            // check if a project with the same name already exist
            if (ApkToolProjectDao.getInstance().existProjectWithName(userUuid, projectName)) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "A project with the same name already exist, please change project name!";
                response.body(reason);
                return reason;
            }

            // business code
            String projectFolderNameUuid = UUID.randomUUID().toString();
            boolean isTemporary = false;
            Session session = EchoWebSocket.getUserSession(userUuid);
            AppenderStreamer appenderStreamer = new AppenderStreamer(session);
            HandlerStreamer handlerStreamer = new HandlerStreamer(session);

            // apktool decode apk to project's folder
            UserProcessBuilder apkToolsProcessBuilder = new UserProcessBuilder(userUuid, EnumerationApkTool.EnumProcessType.CREATE_NEW_PROJECT) {
                @Override
                public void buildProcessLogic() {
                    try {
                        // output logging to the web-socket
                        if (session != null && session.isOpen()) {
                            java.util.logging.Logger.getLogger(Androlib.class.getName()).addHandler(handlerStreamer);
                            java.util.logging.Logger.getLogger(AndrolibResources.class.getName()).addHandler(handlerStreamer);

                            org.apache.log4j.Logger.getLogger(ApkToolsManager.class).addAppender(appenderStreamer);
                            org.apache.log4j.Logger.getLogger(GraphManager.class).addAppender(appenderStreamer);
                        } else {
                            if (session == null) {
                                System.err.println("Null web socket");
                                throw new IllegalArgumentException("WebSocket not found!");
                            } else if (!session.isOpen()) {
                                System.err.println("Web socket closed");
                                throw new IllegalArgumentException("WebSocket closed!");
                            }
                        }
                        ExecutionTimer timer = new ExecutionTimer();
                        timer.start();
                        SocketMessagingProtocol.getInstance()
                                .sendProcessState(session, this.getId(),
                                        EnumerationApkTool.EnumProcessState.STARTED, EnumerationApkTool.EnumProcessType.CREATE_NEW_PROJECT);

                        SocketMessagingProtocol.getInstance().sendLogEvent(session, " ");
                        SocketMessagingProtocol.getInstance().sendLogEvent(session, "********************************");
                        SocketMessagingProtocol.getInstance().sendLogEvent(session, "****  Creating new Project *****");
                        SocketMessagingProtocol.getInstance().sendLogEvent(session, "********************************");
                        SocketMessagingProtocol.getInstance().sendLogEvent(session, " ");

                        // create project dir if not exists
                        File projectFolder = new File(Configurator.getInstance().getProjectRootFolderPath(userUuid,
                                projectFolderNameUuid, isTemporary));

                        if (!projectFolder.exists()) {
                            System.out.println("creating project folder: " + projectFolder.getPath());
                            if (projectFolder.mkdirs()) {
                                System.out.println("Project folder has been created");
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "Project folder has been created at: " + projectFolder.getPath());
                            } else {
                                throw new Exception("Could not create project root folder: " + projectFolder.getPath());
                            }
                        }

                        // create src_apk folder within path = /projectFolder/src_apk/apkFileName.apk
                        String srcApkFolderPath = Configurator.getInstance().getSrcApkFolder(userUuid, projectFolderNameUuid, isTemporary);
                        File srcApkFolder = new File(srcApkFolderPath);
                        if (!srcApkFolder.exists()) {
                            System.out.println("creating src_apk folder: " + srcApkFolder.getPath());
                            if (srcApkFolder.mkdir()) {
                                System.out.println("src_apk folder has been created");
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "src_apk folder has been created");
                            } else {
                                throw new Exception("Could not create src_apk folder!");
                            }
                        }

                        // copy apk file inside src_apk folder
                        FileUtils.copyFileToDirectory(apkTmpFile, srcApkFolder);
                        File srcApkFile = new File(srcApkFolder.getPath() + File.separator + apkTmpFile.getName());
                        if (!srcApkFile.exists() || !srcApkFile.isFile()) {
                            throw new Exception("Original Apk file not found!");
                        }
                        SocketMessagingProtocol.getInstance().sendLogEvent(session, "Copied original apk into :" + srcApkFile.getPath());

                        // create project on db
                        Map<String, Object> info = AnalysisApk.getAppIconAndPackageNameAndAppName(srcApkFile.getPath(), 45);
                        String packageName = (String) info.get("package");
                        String iconBytesAsString = (String) info.get("ic_launcher_bytes_resized");
                        UserBean owner = UserDao.getInstance().getByUuid(userUuid);

                        ApkToolProjectBean projectBean = new ApkToolProjectBean(projectName, projectFolderNameUuid,
                                iconBytesAsString, packageName, owner);
                        ApkToolProjectDao.getInstance().insert(projectBean);

                        SocketMessagingProtocol.getInstance().sendLogEvent(session, "Project added to database");

                        // decode apk and generate its graph
                        ApkToolsManager.getInstance().decodeApk(userUuid, projectFolderNameUuid, isTemporary, srcApkFile);
                        GraphManager.getInstance().createOrUpdateGraphAndExportItToJson(userUuid, projectFolderNameUuid, isTemporary);

                        timer.end();

                        SocketMessagingProtocol.getInstance().sendLogEvent(session, " ");
                        SocketMessagingProtocol.getInstance().sendLogEvent(session, "###### Task finished successfully in "
                                + ExecutionTimer.getTimeString(timer.durationInSeconds()));

                        // notify process finished
                        SocketMessagingProtocol.getInstance()
                                .sendProcessState(session, this.getId(),
                                        EnumerationApkTool.EnumProcessState.COMPLETED, EnumerationApkTool.EnumProcessType.CREATE_NEW_PROJECT);

                    } catch (Exception e) {
                        System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                        //e.printStackTrace();
                        try {
                            SocketMessagingProtocol.getInstance()
                                    .sendLogEvent(session, e.getMessage());
                            // notify process error
                            SocketMessagingProtocol.getInstance()
                                    .sendProcessState(session, this.getId(),
                                            EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.CREATE_NEW_PROJECT);
                        } catch (Exception ex) {
                            // notify process error
                            SocketMessagingProtocol.getInstance()
                                    .sendProcessState(session, this.getId(),
                                            EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.CREATE_NEW_PROJECT);
                            System.err.println("Socket transmission error : " + ex.getMessage());
                            //ex.printStackTrace();
                        }
                    } finally {
                        try {
                            java.util.logging.Logger.getLogger(Androlib.class.getName()).removeHandler(handlerStreamer);
                            java.util.logging.Logger.getLogger(AndrolibResources.class.getName()).removeHandler(handlerStreamer);
                            org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                            org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
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
                        java.util.logging.Logger.getLogger(Androlib.class.getName()).removeHandler(handlerStreamer);
                        java.util.logging.Logger.getLogger(AndrolibResources.class.getName()).removeHandler(handlerStreamer);

                        org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                        org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
                        System.out.println("cleanOperation => log appender because of un-achieved operation");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // remove project from database
                    ApkToolProjectBean projectBean = ApkToolProjectDao.getInstance().getByName(projectName);
                    ApkToolProjectDao.getInstance().delete(projectBean);

                    // delete files
                    File projectFolder = new File(Configurator.getInstance().getProjectRootFolderPath(userUuid,
                            projectFolderNameUuid, isTemporary));
                    if (projectFolder.exists() && projectFolder.isDirectory()) {
                        AtomicReference<Map<String, Object>> removeStats = Utils.rmdirThreadSafe(projectFolder);

                        System.out.println("cleanOperation => Deleting files because of un-achieved operation...");
                        while (((Integer) removeStats.get().get("percent")) != -1) {
                            System.out.print("\rcleanOperation => removed..  " + removeStats.get().get("percent") + "%");
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.print("\rcleanOperation => removing all files finished");
                        ArrayList<String> notRemovedFiles = (ArrayList) removeStats.get().get("notRemovedFiles");
                        for (String path : notRemovedFiles) {
                            System.out.println("\rcleanOperation => can't remove: " + path);
                        }
                        System.out.println("\ncleanOperation => Deleting files because of un-achieved operation DONE!");
                    }
                }

            };

            apkToolsProcessBuilder.execute();

            response.type("application/json; charset=UTF-8");
            response.status(200);
            JSONObject res = new JSONObject();
            res.put("message", "Project created with success!");
            res.put("processId", apkToolsProcessBuilder.getId());
            return res.toJSONString();
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
