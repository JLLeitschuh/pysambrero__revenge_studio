package com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.fileEditor;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.ApkToolsManager;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.beans.ApkToolProjectBean;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import com.ninjaflip.androidrevenge.core.service.cache.GraphCache;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.structure.Graph;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;

/**
 * Created by Solitario on 06/08/2017.
 *
 * this handler is called when the user selects a certain project from the project
 * list and clicks on 'open' button to load it in the project editor
 */
public class FeLoadProjectHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(FeLoadProjectHandler.class);


    @Override
    public Object handle(Request request, Response response) {
        try {
            String userUuid = ServerSecurityManager.getInstance()
                    .getUserUuidFromToken(request.cookie("token"));
            String projectUuid = request.queryParams("projectUuid");

            ApkToolProjectBean project;
            // check if that project exists and belongs to current user
            if(ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
                project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
            }else{
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "This project does not exists or does not belongs to you!";
                response.body(reason);
                return reason;
            }

            String projectFolderNameUuid = ApkToolProjectDao.getInstance().getByUuid(projectUuid).getProjectFolderNameUuid();
            String graphPath = Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, false);
            // load graph
            Graph graph = GraphManager.getInstance().loadGraphFromJson(graphPath);
            // cache it / overrides if exist => we always want an fresh copy of the graph in our cache
            GraphCache.getInstance().cacheGraph(projectUuid, graph);

            // get all apk files information
            List<JSONObject> apkFiles = ApkToolsManager.getInstance().getProjectApkFilesInfo(userUuid, projectFolderNameUuid, false);

            // send response
            response.type("application/json; charset=UTF-8");
            response.status(200);
            JSONObject res = new JSONObject();
            res.put("projectUuid", project.getUuid());
            res.put("name", project.getName());
            res.put("apkFiles", apkFiles);
            res.put("message", "Project loaded with success!");
            return res.toJSONString();
        } catch (Exception e) {
            LOGGER.error("error: " + e.getMessage());
            e.printStackTrace();
            response.type("text/plain; charset=utf-8");
            response.status(500);
            response.body(e.getMessage());
            return e.getMessage();
        }
    }
}