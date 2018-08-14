package com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.fileEditor;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.ApkToolsManager;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.beans.ApkToolProjectBean;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import com.ninjaflip.androidrevenge.core.service.cache.GraphCache;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.structure.Graph;
import spark.Request;
import spark.Response;
import spark.Route;

import java.text.SimpleDateFormat;

/**
 * Created by Solitario on 09/09/2017.
 * <p>
 * Handler for getting project info. Project info is like a screenshot of the project.
 * Whenever the user clicks on the button get 'project info' from the editor's toolbar, this handler gets called
 */
public class FeGetProjectInfoHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(FeGetProjectInfoHandler.class);


    @Override
    public Object handle(Request request, Response response) {

        LOGGER.info("Getting Project info...");
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();

        String projectUuid = request.queryParams("projectUuid");
        String userUuid = ServerSecurityManager.getInstance()
                .getUserUuidFromToken(request.cookie("token"));
        // check if projectUuid is valid
        if (projectUuid == null || projectUuid.equals("")) {
            response.type("text/plain; charset=utf-8");
            response.status(400);
            String reason = "You must provide project uuid!";
            response.body(reason);
            return reason;
        }

        try {
            // parse the AndroidManifest.xml and get all project data:
            // data created, app name, package name, version, sdk min/max, permissions, activity (foreign activities), services, receivers,...
            // app icons
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

            Graph graph = GraphCache.getInstance().getGraph(projectUuid);
            if(graph == null) {
                String projectFolderNameUuid = ApkToolProjectDao.getInstance().getByUuid(projectUuid).getProjectFolderNameUuid();
                String graphPath = Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, false);
                graph = GraphManager.getInstance().loadGraphFromJson(graphPath);
                GraphCache.getInstance().cacheGraph(projectUuid, graph);
            }

            JSONObject result = ApkToolsManager.getInstance()
                    .getProjectInfoForProjectEditor(userUuid, project.getProjectFolderNameUuid(), graph);
            // also send date of creation formatted ISO 8601 2017-09-10T13:02:15Z that caan be used to display time ago since creation
            result.put("dateCreated", project.getDateCreated());
            result.put("dateCreatedFormatted", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(project.getDateCreated()));
            response.type("application/json; charset=UTF-8");
            response.status(200);
            timer.end();
            LOGGER.info("Got project info in " + timer.durationInSeconds() + " seconds");
            return result;
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
