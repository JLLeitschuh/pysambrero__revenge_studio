package com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.fileEditor;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import com.ninjaflip.androidrevenge.core.service.cache.GraphCache;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import net.minidev.json.JSONArray;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.structure.Graph;
import spark.Request;
import spark.Response;
import spark.Route;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.hasId;

/**
 * Created by Solitario on 11/08/2017.
 * <p>
 * Handler that takes care of jsTree search
 */
public class FeJsTreeSearchHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(FeJsTreeSearchHandler.class);


    @Override
    public Object handle(Request request, Response response) {

        try {
            ExecutionTimer timer = new ExecutionTimer();
            timer.start();


            JSONArray resultJsonArray = new JSONArray();

            String searchQuery = request.queryParams("str");
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

            // check if searchQuery is valid
            if (searchQuery == null || searchQuery.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "Please enter a search keyword!";
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

            // gremlin script wont work if encounter any $ character => escaping it
            String escapedSearch = searchQuery.trim().replace("$", "\\$");
            resultJsonArray.addAll(GraphManager.getInstance().graphSearchForJsTreeFilter(graph,
                    escapedSearch, null,null));

            // send response
            response.type("application/json; charset=UTF-8");
            response.status(200);
            timer.end();
            String resp = resultJsonArray.toJSONString();
            LOGGER.debug("jsTree search result : " + resp);
            LOGGER.debug("jsTree search for text["+searchQuery+"] in project ["+projectUuid+"] found ["+resultJsonArray.size()+"] hits, done in : " + ExecutionTimer.getTimeString(timer.durationInSeconds()));
            return resp;
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
