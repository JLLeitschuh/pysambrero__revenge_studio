package com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.fileEditor;

import com.google.common.collect.Iterators;
import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.core.apktool.graph.MimeTypes;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import com.ninjaflip.androidrevenge.core.service.cache.GraphCache;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.Iterator;
import java.util.List;

/**
 * Created by Solitario on 11/08/2017.
 * <p>
 * Handler that takes care of jsTree massload plugin
 */
public class FeJsTreeMassLoadHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(FeJsTreeMassLoadHandler.class);


    @Override
    public Object handle(Request request, Response response) {
        LOGGER.debug("jsTree masssload start...");
        try {
            ExecutionTimer timer = new ExecutionTimer();
            timer.start();

            JSONObject resultJsonObject = new JSONObject();

            String ids = request.queryParams("ids");
            String projectUuid = request.queryParams("projectUuid");
            String userUuid = ServerSecurityManager.getInstance()
                    .getUserUuidFromToken(request.cookie("token"));

            Graph graph = GraphCache.getInstance().getGraph(projectUuid);
            if(graph == null) {
                String projectFolderNameUuid = ApkToolProjectDao.getInstance().getByUuid(projectUuid).getProjectFolderNameUuid();
                String graphPath = Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, false);
                graph = GraphManager.getInstance().loadGraphFromJson(graphPath);
                GraphCache.getInstance().cacheGraph(projectUuid, graph);
            }

            String[] arrayIds = ids.split(",");

            for (String arrayId : arrayIds) {
                List<Vertex> children = graph.traversal().V(Integer.valueOf(arrayId)).out("contains").toList();

                JSONArray childrenJsonArray = new JSONArray();

                for (Vertex child : children) {
                    JSONObject jsTreeJsonChild = new JSONObject();
                    JSONObject nodeData = new JSONObject();

                    if (child.label().equals("file")) {
                        jsTreeJsonChild.put("id", child.id().toString());
                        jsTreeJsonChild.put("type", MimeTypes.getMimeTypeCategory(child.value("mimeType").toString()));
                        jsTreeJsonChild.put("text", child.value("name").toString());
                        jsTreeJsonChild.put("children", false);

                        nodeData.put("mimeType", child.value("mimeType").toString());

                    } else {
                        Iterator<Edge> iterEdgesChild = child.edges(Direction.OUT, "contains");
                        jsTreeJsonChild.put("id", child.id().toString());
                        jsTreeJsonChild.put("type", "folder");
                        jsTreeJsonChild.put("text", child.value("name").toString() + " (" + Iterators.size(iterEdgesChild) + ")");
                        jsTreeJsonChild.put("children", true);
                    }
                    jsTreeJsonChild.put("data", nodeData);
                    childrenJsonArray.add(jsTreeJsonChild);
                }

                resultJsonObject.put(arrayId, childrenJsonArray);
            }
            // send response
            response.type("application/json; charset=UTF-8");
            response.status(200);
            String resp = resultJsonObject.toJSONString();

            timer.end();
            //LOGGER.debug("massload response : " + resp);
            LOGGER.debug("jsTree masssload done in : " + ExecutionTimer.getTimeString(timer.durationInSeconds()));
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
