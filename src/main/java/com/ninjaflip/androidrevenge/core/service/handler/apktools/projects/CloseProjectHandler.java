package com.ninjaflip.androidrevenge.core.service.handler.apktools.projects;

import com.ninjaflip.androidrevenge.core.apktool.UserProcessBuilder;
import com.ninjaflip.androidrevenge.core.scrapper.ScrapperProcessBuilder;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.service.cache.GraphCache;
import com.ninjaflip.androidrevenge.enums.EnumerationApkTool;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;

/**
 * Created by Solitario on 27/12/2017.
 * <p>
 * When the user wants to close a project and return to the list of projects, this handler is responsible
 * for checking if an ongoing operation is running and asks the users for a confirmation
 */
public class CloseProjectHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(CloseProjectHandler.class);


    @Override
    public Object handle(Request request, Response response) {
        try {
            String userUuid = ServerSecurityManager.getInstance()
                    .getUserUuidFromToken(request.cookie("token"));


            String action = request.queryParams("action");

            switch (action) {
                case "TRY_CLOSE": {
                    List<UserProcessBuilder> userListProcess = UserProcessBuilder.getUserRunningProcesses(userUuid);
                    if (userListProcess.size() > 0) {
                        // check if a critical task is running
                        for (UserProcessBuilder proc : userListProcess) {
                            EnumerationApkTool.EnumProcessType procType = proc.getUserProcessType();
                            if (procType.equals(EnumerationApkTool.EnumProcessType.PACKAGE_NAME_CHANGER)
                                    || procType.equals(EnumerationApkTool.EnumProcessType.PACKAGE_RENAMER)
                                    || procType.equals(EnumerationApkTool.EnumProcessType.MANIFEST_ENTRIES_RENAMER)
                                    || procType.equals(EnumerationApkTool.EnumProcessType.TEXT_SEARCH_AND_REPLACE)
                                    || procType.equals(EnumerationApkTool.EnumProcessType.TEXT_SEARCH)
                                    || procType.equals(EnumerationApkTool.EnumProcessType.BUILD_DEBUG_APK)
                                    || procType.equals(EnumerationApkTool.EnumProcessType.BUILD_RELEASE_APK)
                                    || procType.equals(EnumerationApkTool.EnumProcessType.INSTANT_RUN)
                                    || procType.equals(EnumerationApkTool.EnumProcessType.ADB_INSTALL)
                                    ) {
                                response.type("text/plain; charset=utf-8");
                                response.status(200);
                                String reason = "'" + procType + "' is running, do you want to close the project anyway?";
                                response.body(reason);
                                return reason;
                            }
                        }
                        // stop all running processes related to the project editor
                        UserProcessBuilder.cancelAllUserProcessesOnProjectEditorClose(userUuid);
                        response.status(204);
                        return "";
                    } else {
                        response.status(204);
                        return "";
                    }
                }
                case "FORCE_CLOSE": {
                    // stop all running processes related to the project editor
                    UserProcessBuilder.cancelAllUserProcessesOnProjectEditorClose(userUuid);
                    response.status(204);
                    return "";
                }

                default: {
                    response.type("text/plain; charset=utf-8");
                    response.status(400);
                    String reason = "Unknown action parameter '" + action + "'";
                    response.body(reason);
                    return reason;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("error: " + e.getMessage());
            response.type("text/plain; charset=utf-8");
            response.status(500);
            response.body(e.getMessage());
            return e.getMessage();
        }
    }
}