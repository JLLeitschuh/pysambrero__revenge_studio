package com.ninjaflip.androidrevenge.core.service.handler.apktools.projects;

import com.ninjaflip.androidrevenge.beans.ApkToolProjectBean;
import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.service.cache.GraphCache;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Created by Solitario on 02/08/2017.
 * <p>
 * Delete a project from filesystem and database
 */
public class DeleteProjectHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(DeleteProjectHandler.class);


    @Override
    public Object handle(Request request, Response response) {
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();

        try {
            String userUuid = ServerSecurityManager.getInstance()
                    .getUserUuidFromToken(request.cookie("token"));
            String projectUuid = request.queryParams("projectUuid");

            // check if projectUuid is valid
            if (projectUuid == null || projectUuid.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "You must provide project uuid!";
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
            LOGGER.debug("Removing project '" + project.getName() + "'...");

            // remove project's root folder
            String projectRootFolderPath = Configurator.getInstance().getProjectRootFolderPath(userUuid,
                    project.getProjectFolderNameUuid(), false);
            File projectRootFolder = new File(projectRootFolderPath);
            if(!projectRootFolder.exists()){
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "Project folder does not exists!";
                response.body(reason);
                return reason;
            }

            if(!projectRootFolder.isDirectory()){
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "Project folder is not a directory!";
                response.body(reason);
                return reason;
            }

            PreferencesManager.getInstance().addFolderAsMarkedForDelete(userUuid, project.getProjectFolderNameUuid());
            CompletableFuture.runAsync(() -> {
                try {
                    FileUtils.deleteDirectory(projectRootFolder);
                }catch (Exception ex1){
                    try{
                        Thread.sleep(1000);
                        FileUtils.deleteDirectory(projectRootFolder);
                    }catch (Exception x2){
                        //do nothing
                    }
                }
            });

            // delete project from database
            ApkToolProjectDao.getInstance().delete(project);

            // remove project graph from cache
            GraphCache.getInstance().removeGraph(projectUuid);
            response.status(204);
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("error: " + e.getMessage());
            response.type("text/plain; charset=utf-8");
            response.status(500);
            response.body(e.getMessage());
            return e.getMessage();
        }finally {
            timer.end();
            LOGGER.debug("Remove project done in " + timer.durationInSeconds() + " s");
        }
    }
}
