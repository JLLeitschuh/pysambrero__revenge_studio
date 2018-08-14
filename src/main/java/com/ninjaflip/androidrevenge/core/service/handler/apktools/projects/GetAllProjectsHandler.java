package com.ninjaflip.androidrevenge.core.service.handler.apktools.projects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ninjaflip.androidrevenge.beans.ApkToolProjectBean;
import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.apktool.ApkToolsManager;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.service.cache.GraphCache;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.structure.Graph;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Solitario on 02/08/2017.
 * <p>
 * Return a list of all apktool projects associated to current user
 */
public class GetAllProjectsHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(GetAllProjectsHandler.class);


    @Override
    public Object handle(Request request, Response response) {
        try {
            String userUuid = ServerSecurityManager.getInstance()
                    .getUserUuidFromToken(request.cookie("token"));

            String lastOpenedProjectUuid = request.queryParams("lastOpenedProjectUuid");

            if (lastOpenedProjectUuid != null && !lastOpenedProjectUuid.equals("")) {
                // update last opened project's icons
                ApkToolProjectBean lastOpenedProject = ApkToolProjectDao.getInstance().getByUuid(lastOpenedProjectUuid);
                Graph graph = GraphCache.getInstance().getGraph(lastOpenedProjectUuid);
                if (graph == null) {
                    String projectFolderNameUuid = ApkToolProjectDao.getInstance().getByUuid(lastOpenedProjectUuid).getProjectFolderNameUuid();
                    String graphPath = Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, false);
                    graph = GraphManager.getInstance().loadGraphFromJson(graphPath);
                }
                JSONObject appIconsData = ApkToolsManager.getInstance().getProjectAppIconsData(userUuid, lastOpenedProject.getProjectFolderNameUuid(), graph);

                Object appIconsObj = appIconsData.get("appIcons");
                if (appIconsObj != null) {
                    @SuppressWarnings("unchecked")
                    ArrayList<JSONObject> appIcons = (ArrayList<JSONObject>) appIconsObj;
                    Date lastOneDate = null;
                    String lastOnePath = null;

                    for (JSONObject appIcon : appIcons) {
                        String iconPath = (String) appIcon.get("iconPath");
                        long iconLastModified = (long) appIcon.get("iconLastModified");
                        Date lastModifiedDate = new Date(iconLastModified * 1000);

                        if (lastOneDate == null) {
                            lastOneDate = lastModifiedDate;
                            lastOnePath = iconPath;
                        } else {
                            if (lastOneDate.before(lastModifiedDate)) {
                                lastOneDate = lastModifiedDate;
                                lastOnePath = iconPath;
                            }
                        }
                    }

                    // update project icon
                    if (lastOnePath != null) {
                        String iconBytesAsString = Utils.getImageThumbnailAsBase64String(new File(lastOnePath), 45);
                        lastOpenedProject.setIconBytesAsString(iconBytesAsString);
                        ApkToolProjectDao.getInstance().update(lastOpenedProject);
                    }
                }
            }

            List<ApkToolProjectBean> listProjects = ApkToolProjectDao.getInstance().getAll(userUuid);
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            String resultJsonAsString = gson.toJson(listProjects);
            //LOGGER.debug("LIST PROJECTS = " + resultJsonAsString);
            // send response
            response.type("application/json; charset=UTF-8");
            response.status(200);
            return resultJsonAsString;
        } catch (Exception e) {
            // notify the user ==> the scrapper has failed
            //Utils.speak(R.string.scrapper_failed_msg);
            e.printStackTrace();
            LOGGER.error("error: " + e.getMessage());
            response.type("text/plain; charset=utf-8");
            response.status(500);
            response.body(e.getMessage());
            return e.getMessage();
        }
    }
}
