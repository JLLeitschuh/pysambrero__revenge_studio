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
 * Handler that takes care of jsTree lazy-loading data, in the ProjectEditor => File editor UI
 */
public class FeJsTreeLazyLoadingHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(FeJsTreeLazyLoadingHandler.class);

    @Override
    public Object handle(Request request, Response response) {

        try {
            ExecutionTimer timer = new ExecutionTimer();
            timer.start();

            JSONArray resultJsonArray;
            String nodeId = request.queryParams("id");
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

            if (nodeId.equals("#")) { // get root folder
                //Vertex rootVertex = GraphManager.getInstance().getRootVertex(graph);
                List<Vertex> children = GraphManager.getInstance().getVertexChildren(graph, "0");
                resultJsonArray = getJsTreeRootJsonArray(children, ApkToolProjectDao.getInstance().getByUuid(projectUuid).getName());
            } else { // get folder
                //Vertex folderVertex = GraphManager.getInstance().getVertexById(graph, nodeId);
                List<Vertex> children = GraphManager.getInstance().getVertexChildren(graph, nodeId);
                resultJsonArray = getJsTreeJsonArray(children);
            }

            // send response
            response.type("application/json; charset=UTF-8");
            response.status(200);
            String resp = resultJsonArray.toJSONString();

            timer.end();
            LOGGER.debug("jsTree lazy-loading node[id="+nodeId+"] done in : " + timer.durationInSeconds()+ " s");
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


    /**
     * Build the json data structure that is going to populate the jstree node on the client
     *
     * @param children list of tinkerpop vertices
     * @return a valid JsTree json object
     */
    private JSONArray getJsTreeJsonArray(List<Vertex> children) {
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
        return childrenJsonArray;
    }


    /**
     * Build the json data structure for the root node of the project, that is going to populate the jstree root node on the client
     *
     * @param children list of tinkerpop vertices
     * @param projectName project name as node text
     * @return a valid JsTree json object
     */
    private JSONArray getJsTreeRootJsonArray(List<Vertex> children, String projectName) {
        JSONObject rootNodeData = new JSONObject();


        JSONObject jsTreeJson = new JSONObject();
        jsTreeJson.put("id", "0");
        jsTreeJson.put("type", "folder");

        JSONObject state = new JSONObject();
        state.put("opened", true);
        state.put("selected", true);
        jsTreeJson.put("state", state);

        jsTreeJson.put("text", projectName);

        //rootNodeData.put("path", parent.value("path").toString());
        jsTreeJson.put("data", rootNodeData);


        JSONArray childrenJsonArray = new JSONArray();
        for (Vertex child : children) {
            JSONObject jsTreeJsonChild = new JSONObject();
            JSONObject nodeData = new JSONObject();

            String childName = child.value("name").toString();
            if (child.label().equals("file")) {
                if(!childName.equals("apktool.yml")) {
                    jsTreeJsonChild.put("id", child.id().toString());
                    jsTreeJsonChild.put("type", MimeTypes.getMimeTypeCategory(child.value("mimeType").toString()));
                    jsTreeJsonChild.put("text", childName);
                    //nodeData.put("path", child.value("path").toString());
                    nodeData.put("mimeType", child.value("mimeType").toString());
                    jsTreeJsonChild.put("data", nodeData);

                    childrenJsonArray.add(jsTreeJsonChild);
                }
            } else {
                if(!childName.equals("original")) {
                    Iterator<Edge> iterEdgesChild = child.edges(Direction.OUT, "contains");
                    jsTreeJsonChild.put("id", child.id().toString());
                    jsTreeJsonChild.put("type", "folder");
                    jsTreeJsonChild.put("text", childName + " (" + Iterators.size(iterEdgesChild) + ")");
                    jsTreeJsonChild.put("children", true);
                    //nodeData.put("path", child.value("path").toString());
                    jsTreeJsonChild.put("data", nodeData);

                    childrenJsonArray.add(jsTreeJsonChild);
                }
            }
        }
        jsTreeJson.put("children", childrenJsonArray);

        JSONArray result = new JSONArray();
        result.add(jsTreeJson);
        return result;
    }
}
