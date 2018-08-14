package com.ninjaflip.androidrevenge.core.service.handler.apktools.previewer;

import brut.androlib.Androlib;
import brut.androlib.res.AndrolibResources;
import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.apktool.ApkToolsManager;
import com.ninjaflip.androidrevenge.core.apktool.UserProcessBuilder;
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
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.enums.EnumerationApkTool;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import com.ninjaflip.androidrevenge.utils.StringUtil;
import com.ninjaflip.androidrevenge.utils.Utils;
import com.ninjaflip.androidrevenge.websocket.EchoWebSocket;
import net.minidev.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Solitario on 16/06/2017.
 * <p>
 * handler for apk benchmark, which means: decoding an apk, then
 * updating its package name with a random one, the building and signing it.
 */
public class BenchmarkTmpApkFileHandler implements Route {

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("application/json");

        String savedTmpApkFile = request.queryParams("savedTmpApkFilePath");

        /*System.out.println("queryParams " + request.queryParams()); // What type of data am I sending?
        System.out.println("contentType "+ request.contentType()); // What type of data am I sending?
        System.out.println("params " +request.params()); // What are the params sent?
        System.out.println("raw " +request.raw()); // What's the raw data sent?*/

        System.out.println("benchmarking tmp apk file ==> " + savedTmpApkFile);

        String userUuid = ServerSecurityManager.getInstance()
                .getUserUuidFromToken(request.cookie("token"));

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

        if (savedTmpApkFile == null || savedTmpApkFile.equals("")) {
            response.status(400);
            JSONObject res = new JSONObject();
            res.put("message", "Bad request: no apk file path found for benchmark!");
            return res.toJSONString();
        } else {
            if (!new File(savedTmpApkFile).exists()) {
                response.status(404);
                JSONObject res = new JSONObject();
                res.put("message", "Apk file not found!");
                return res.toJSONString();
            }

            System.out.println("Benchmarking ==> apk " + savedTmpApkFile);

            try {
                String projectFolderNameUuid = UUID.randomUUID().toString();
                boolean isTemporary = true;
                Session session = EchoWebSocket.getUserSession(userUuid);
                AppenderStreamer appenderStreamer = new AppenderStreamer(session);
                HandlerStreamer handlerStreamer = new HandlerStreamer(session);

                SocketPrintStream socketErrPrintStream = new SocketPrintStream(System.err, session);


                UserProcessBuilder apkToolsProcessBuilder = new UserProcessBuilder(userUuid, EnumerationApkTool.EnumProcessType.PREVIEW_TEST) {
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
                                org.apache.log4j.Logger.getLogger(PackageNameChanger.class).addAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(MassiveFileRenamer.class).addAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(FileComputingManager.class).addAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(SignTool.class).addAppender(appenderStreamer);
                                //org.apache.log4j.Logger.getLogger(Serguad.class).addAppender(appenderStreamer);
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
                                            EnumerationApkTool.EnumProcessState.STARTED, EnumerationApkTool.EnumProcessType.PREVIEW_TEST);

                            // get package name
                            String packageName = AnalysisApk.getPackageNameFromApkFile(savedTmpApkFile);
                            // generate random new package name
                            String newPackageName = StringUtil.buildRandomPackageName(packageName);

                            ExecutionTimer timer = new ExecutionTimer();
                            timer.start();

                            // decode apk
                            ApkToolsManager.getInstance().decodeApk(userUuid, projectFolderNameUuid, isTemporary, new File(savedTmpApkFile));
                            GraphManager.getInstance().createOrUpdateGraphAndExportItToJson(userUuid, projectFolderNameUuid, isTemporary);

                            // update package name
                            ApkToolsManager.getInstance().updatePackageName(userUuid, projectFolderNameUuid, isTemporary, newPackageName);

                            // Massive file renamer
                            //ApkToolsManager.getInstance().renameManifestEntries(userUuid, projectFolderNameUuid, isTemporary, null);

                            // SERGUAD injection
                            //Serguad serguad = new Serguad(userUuid, projectFolderNameUuid, isTemporary);
                            //serguad.processApp();

                            // build new Apk
                            ApkToolsManager.getInstance().buildApk(userUuid, projectFolderNameUuid, isTemporary);

                            // sign apk with debug keystore
                            String targetApkFileOrFolderPath = Configurator.getInstance().getDistFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                            String destSignedApkFolder = Configurator.getInstance().getGenFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                            ApkToolsManager.getInstance().signAndZipAlignApkForDebug(targetApkFileOrFolderPath, destSignedApkFolder, true);
                            ApkToolsManager.getInstance().verifyApk(destSignedApkFolder, false);

                            SocketMessagingProtocol.getInstance().sendLogEvent(session, " ");
                            SocketMessagingProtocol.getInstance().sendLogEvent(session, "********************************");
                            SocketMessagingProtocol.getInstance().sendLogEvent(session, "****  Deleting temp files  *****");
                            SocketMessagingProtocol.getInstance().sendLogEvent(session, "********************************");
                            SocketMessagingProtocol.getInstance().sendLogEvent(session, " ");

                            try {
                                // delete all files of the current tmp project except /gen/ folder that contains the generated apk
                                File tmpProjectFolder = new File(Configurator.getInstance().getProjectRootFolderPath(userUuid,
                                        projectFolderNameUuid, isTemporary));
                                if (tmpProjectFolder.exists() && tmpProjectFolder.isDirectory()) {
                                    List<String> exceptFilePaths = new ArrayList<>();
                                    File[] exceptFiles = new File(destSignedApkFolder).listFiles();
                                    if (exceptFiles != null) {
                                        for (File f : exceptFiles) {
                                            exceptFilePaths.add(f.getPath());
                                        }
                                    }
                                    SocketMessagingProtocol.getInstance().sendLogEvent(session, "Finalizing => Deleting tmp files...");
                                    System.out.println("Finalizing => Deleting tmp files EXCEPT generated apk file...");
                                    Utils.rmdirExcept(tmpProjectFolder, exceptFilePaths);
                                    System.out.println("\rFinalizing => Deleting tmp files EXCEPT generated apk file DONE!");
                                    SocketMessagingProtocol.getInstance().sendLogEvent(session, "Finalizing => Deleting tmp files DONE!");
                                }
                            } catch (Exception e) {
                                System.err.println("Error while Deleting all tmp files EXCEPT generated apk file : " + e.getMessage());
                                e.printStackTrace();
                            }
                            timer.end();

                            SocketMessagingProtocol.getInstance().sendLogEvent(session, " ");
                            SocketMessagingProtocol.getInstance().sendLogEvent(session, "###### Task finished successfully in "
                                    + ExecutionTimer.getTimeString(timer.durationInSeconds()));

                            // send apk link for download
                            Map<String, Object> apkReadyInfo = ApkToolsManager.getInstance().getDebugApkReadyInfo(destSignedApkFolder);
                            SocketMessagingProtocol.getInstance().sendApkReadyInfo(session, (String) apkReadyInfo.get("url"),
                                    (EnumerationApkTool.EnumBuildType) apkReadyInfo.get("buildType"), (String) apkReadyInfo.get("packageName"),
                                    (String) apkReadyInfo.get("iconBase64"), (String) apkReadyInfo.get("size_in_MB"),
                                    (String) apkReadyInfo.get("filePath"));

                            // notify process finished
                            SocketMessagingProtocol.getInstance()
                                    .sendProcessState(session, this.getId(),
                                            EnumerationApkTool.EnumProcessState.COMPLETED, EnumerationApkTool.EnumProcessType.PREVIEW_TEST);


                        } catch (Exception e) {
                            System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                            //e.printStackTrace();

                            SocketMessagingProtocol.getInstance()
                                    .sendLogEvent(session, StringUtil.escape(e.getMessage()));
                            // notify process error
                            SocketMessagingProtocol.getInstance()
                                    .sendProcessState(session, this.getId(),
                                            EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.PREVIEW_TEST, "Please inspect logs for more details.");

                        } finally {
                            try {
                                Logger.getLogger(Androlib.class.getName()).removeHandler(handlerStreamer);
                                Logger.getLogger(AndrolibResources.class.getName()).removeHandler(handlerStreamer);
                                Logger.getLogger("").removeHandler(handlerStreamer);

                                org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(PackageNameChanger.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(MassiveFileRenamer.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);
                                org.apache.log4j.Logger.getLogger(SignTool.class).removeAppender(appenderStreamer);
                                //org.apache.log4j.Logger.getLogger(Serguad.class).removeAppender(appenderStreamer);

                                socketErrPrintStream.close();
                                System.setErr(System.err);

                                System.gc();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    }

                    @Override
                    public void cleanOperation() {
                        try {
                            // delete files
                            File tmpProjectFolder = new File(Configurator.getInstance().getProjectRootFolderPath(userUuid,
                                    projectFolderNameUuid, isTemporary));
                            if (tmpProjectFolder.exists() && tmpProjectFolder.isDirectory()) {
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "cleanOperation => Deleting temporary files, Please wait...");
                                System.out.println("cleanOperation => Deleting temporary files, Please wait......");
                                Utils.rmdirExcept(tmpProjectFolder, new ArrayList<>());

                                /*AtomicReference<Map<String, Object>> removeStats = Utils.rmdirThreadSafe(tmpProjectFolder);

                                System.out.println("cleanOperation => Deleting files because of un-achieved operation...");
                                while (((Integer) removeStats.get().get("percent")) != -1) {
                                    System.out.print("\rcleanOperation => removed..  " + removeStats.get().get("percent") + "%");
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                System.out.print("\rcleanOperation => removed all tmp files finished");
                                ArrayList<String> notRemovedFiles = (ArrayList) removeStats.get().get("notRemovedFiles");
                                for (String path : notRemovedFiles) {
                                    System.out.println("\rcleanOperation => can't remove: " + path);
                                }*/
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "cleanOperation => Deleting temporary files DONE!");
                                System.out.println("\ncleanOperation => Deleting temporary files DONE!");
                            }


                            // remove log appender
                            SocketMessagingProtocol.getInstance().sendLogEvent(session, "cleanOperation => removing log appender");
                            System.out.println("\ncleanOperation => removing log appender");

                            Logger.getLogger(Androlib.class.getName()).removeHandler(handlerStreamer);
                            Logger.getLogger(AndrolibResources.class.getName()).removeHandler(handlerStreamer);
                            Logger.getLogger("").removeHandler(handlerStreamer);

                            org.apache.log4j.Logger.getLogger(ApkToolsManager.class).removeAppender(appenderStreamer);
                            org.apache.log4j.Logger.getLogger(GraphManager.class).removeAppender(appenderStreamer);
                            org.apache.log4j.Logger.getLogger(PackageNameChanger.class).removeAppender(appenderStreamer);
                            org.apache.log4j.Logger.getLogger(MassiveFileRenamer.class).removeAppender(appenderStreamer);
                            org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);
                            org.apache.log4j.Logger.getLogger(SignTool.class).removeAppender(appenderStreamer);
                            //org.apache.log4j.Logger.getLogger(Serguad.class).removeAppender(appenderStreamer);

                            socketErrPrintStream.close();
                            System.setErr(System.err);

                            System.out.println("cleanOperation => log appender removed");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                apkToolsProcessBuilder.execute();

                response.status(200);
                JSONObject res = new JSONObject();
                res.put("message", "Benchmarking Apk started!");
                res.put("processId", apkToolsProcessBuilder.getId());
                return res.toJSONString();

            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                response.status(500);
                JSONObject res = new JSONObject();
                res.put("message", "Failed to benchmark apk file!");
                return res.toJSONString();
            }
        }
    }
}
