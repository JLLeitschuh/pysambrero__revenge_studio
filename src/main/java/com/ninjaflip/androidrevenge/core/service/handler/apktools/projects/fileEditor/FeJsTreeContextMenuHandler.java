package com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.fileEditor;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.core.apktool.graph.MimeTypes;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.service.cache.GraphCache;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerVertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.script.ScriptException;
import javax.servlet.MultipartConfigElement;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.toIntExact;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.hasId;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.in;

/**
 * Created by Solitario on 11/08/2017.
 * <p>
 * Handler that takes care of jsTree context menu actions
 */
public class FeJsTreeContextMenuHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(FeJsTreeContextMenuHandler.class);
    private static final Pattern regexInvalidFileName = Pattern.compile(".*[,;?|/\\\\'<>^*%!\" \\t].*");
    private static Random ran = new Random();


    @Override
    public Object handle(Request request, Response response) {
        try {
            ExecutionTimer timer = new ExecutionTimer();
            timer.start();

            String action = request.queryParams("action");
            String projectUuid = request.queryParams("projectUuid");
            String userUuid = ServerSecurityManager.getInstance()
                    .getUserUuidFromToken(request.cookie("token"));

            // check if action is valid
            if (action == null || action.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "You must provide an action!";
                response.body(reason);
                return reason;
            }

            // check if projectUuid is valid
            if (projectUuid == null || projectUuid.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "You must provide project uuid!";
                response.body(reason);
                return reason;
            }

            Graph graph = GraphCache.getInstance().getGraph(projectUuid);
            if (graph == null) {
                String projectFolderNameUuid = ApkToolProjectDao.getInstance().getByUuid(projectUuid).getProjectFolderNameUuid();
                String graphPath = Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, false);
                graph = GraphManager.getInstance().loadGraphFromJson(graphPath);
                GraphCache.getInstance().cacheGraph(projectUuid, graph);
            }

            switch (action) {
                case "RENAME_NODE": {
                    String nodeId = request.queryParams("nodeId");
                    String newName = request.queryParams("new_name");

                    // check if nodeId is valid
                    if (nodeId == null || nodeId.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide node id!";
                        response.body(reason);
                        return reason;
                    }

                    // check newName
                    if (newName == null || newName.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a new name";
                        response.body(reason);
                        return reason;
                    } else {
                        // check newName is valid using regex
                        newName = newName.trim();
                        Matcher matcher = regexInvalidFileName.matcher(newName);
                        if (matcher.matches()) { // file name is not valid
                            response.type("text/plain; charset=utf-8");
                            response.status(400);
                            String reason = "Invalid file name, it must not contain: , ; ? | / \\ ' < > ^ * % ! \" space tabulation";
                            response.body(reason);
                            return reason;
                        }
                    }

                    // get vertex of that file
                    Vertex vertex = GraphManager.getInstance().getVertexById(graph, nodeId);

                    // check if vertex is not null
                    if (vertex == null) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "The element you want to rename does not exist in project graph!";
                        response.body(reason);
                        return reason;
                    }

                    File fileToRename = new File(vertex.value("path").toString());

                    LOGGER.debug("file to rename node[" + nodeId + "] path : " + vertex.value("path").toString() + " ==> " + newName);

                    // check if a file named as newName already exists
                    File newNamedFile = new File(fileToRename.getParent(), newName);
                    if (newNamedFile.exists()) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "A file with the same name already exists!";
                        response.body(reason);
                        return reason;
                    }

                    if (vertex.label().equals("directory")) { // renaming a directory
                        // get folder path before rename
                        String folderPath = fileToRename.getPath();
                        //LOGGER.debug("Client want to rename folder => " + folderPath);
                        // rename it and update all its descendant children paths
                        if (fileToRename.renameTo(newNamedFile)) {
                            // get renamed folder path before rename
                            String renamedFolderPath = newNamedFile.getPath();
                            LOGGER.debug("folder renamed, new name is  => " + renamedFolderPath);
                            // update node's name and path property

                            Map<String,String> keyValueMap = new HashMap<>();
                            keyValueMap.put("name", newName);
                            keyValueMap.put("path", renamedFolderPath);

                            GraphManager.getInstance().updateVertexPropertyAndSaveGraph(graph,
                                    nodeId, keyValueMap, true,
                                    userUuid, projectUuid, false);

                            LOGGER.debug("folder name and path properties updated inside graph");
                            /*
                            update all paths of folder descendants nodes
                            if we don't add a file separator after the folderPath it will corrupt the
                            project by renaming the wrong files, for example if you rename drawable
                            folder, it will also rename drawable-hdpi :)
                             */
                            /*String script = "graph.traversal().V().filter{it.get().value(\"path\").contains(\"" + StringEscapeUtils.escapeJava(folderPath + File.separator) + "\")}";
                            Map<String, Object> bindingsValues = new HashMap<>();
                            bindingsValues.put("graph", graph);
                            LOGGER.debug("fetching descendants of the renamed folder...");
                            List<Vertex> vertices = GraphManager.getInstance().executeGremlinGroovyScript(script, bindingsValues);*/



                            //List<Vertex> vertices = graph.traversal().V().has("path", Text.textContains(folderPath + File.separator)).toList();
                            List<Vertex> vertices = GraphManager.getInstance().getVertexChildrenSubTree(graph, vertex);
                            //GraphManager.getInstance().printVertices(vertices, "path");
                            for (Vertex v : vertices) {
                                LOGGER.debug("--- folderPath ==> " + folderPath);
                                LOGGER.debug("--- renamedFolderPath ==> " + renamedFolderPath);
                                LOGGER.debug("--- v.value(path).toString() ==> " + v.value("path").toString());
                                String newPath = v.value("path").toString().replace(folderPath, renamedFolderPath);
                                LOGGER.debug("--- newPath ==> " + newPath);
                                v.property(VertexProperty.Cardinality.single, "path", newPath);
                                LOGGER.debug("child updated name ==> " + v.value("name").toString());
                                LOGGER.debug("child updated path ==> " + v.value("path").toString());
                                LOGGER.debug("---------------");
                            }
                            LOGGER.debug("children paths are updated for " + vertices.size() + " child");

                            // rewrite the graph.json file to disk
                            GraphManager.getInstance().updateProjectGraphAndSaveItToDisk(graph, projectUuid, userUuid, false);
                            // update graph cache
                            GraphCache.getInstance().cacheGraph(projectUuid, graph);
                            LOGGER.debug("graph cache updated");
                        } else {
                            response.type("text/plain; charset=utf-8");
                            response.status(500);
                            String reason = "Could not rename the specified folder!";
                            response.body(reason);
                            return reason;
                        }
                    } else { // renaming a file
                        if (fileToRename.renameTo(newNamedFile)) { // update the graph only if file renamed correctly in the filesystem
                            String renamedFilePath = newNamedFile.getPath();
                            LOGGER.debug("file renamed, new name is  => " + renamedFilePath);
                            // update node's name and path property
                            Map<String,String> keyValueMap = new HashMap<>();
                            keyValueMap.put("name", newName);
                            keyValueMap.put("path", newNamedFile.getPath());

                            GraphManager.getInstance().updateVertexPropertyAndSaveGraph(graph,
                                    nodeId, keyValueMap, true,
                                    userUuid, projectUuid, false);
                            LOGGER.debug("file name and path properties updated inside graph");
                        } else {
                            response.type("text/plain; charset=utf-8");
                            response.status(500);
                            String reason = "Could not rename the specified file!";
                            response.body(reason);
                            return reason;
                        }
                    }
                    timer.end();
                    LOGGER.debug("FeJsTreeContextMenuHandler RENAME_NODE done in : " + timer.durationInSeconds() + " s");
                    // send response
                    response.status(204);
                    return "";
                }
                case "DELETE_NODE": {
                    String nodeId = request.queryParams("nodeId");
                    // check if nodeId is valid
                    if (nodeId == null || nodeId.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide node id!";
                        response.body(reason);
                        return reason;
                    }

                    // get vertex of that file
                    Vertex vertex = GraphManager.getInstance().getVertexById(graph, nodeId);

                    // check if vertex is not null
                    if (vertex == null) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "The element you want to delete does not exist in project graph!";
                        response.body(reason);
                        return reason;
                    }
                    File fileToDelete = new File(vertex.value("path").toString());
                    LOGGER.debug("file to delete node[" + nodeId + "] path : " + vertex.value("path").toString());

                    if (!fileToDelete.exists()) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "The element that you are trying to delete does not exists!";
                        response.body(reason);
                        return reason;
                    } else {
                        if (fileToDelete.isDirectory()) { // delete all his children first
                            try {
                                // delete it
                                try {
                                    FileUtils.deleteDirectory(fileToDelete);
                                } catch (IOException ioEx1) {
                                    Thread.sleep(500);
                                    try {
                                        FileUtils.deleteDirectory(fileToDelete);
                                    } catch (IOException ioEx2) {
                                        Thread.sleep(500);
                                        FileUtils.deleteDirectory(fileToDelete);
                                    }
                                }
                                /*
                                once file deleted from filesystem => update the graph (remove its node and its descendants from the graph)
                                if we don't add a file separator after the fileToDelete it will corrupt the
                                project by renaming the wrong files, for example if you remove drawable
                                folder, it will also remove drawable-hdpi :)
                                 */
                                /*
                                String script = "graph.traversal().V().filter{it.get().value(\"path\").contains(\"" + StringEscapeUtils.escapeJava(fileToDelete + File.separator) + "\")}";
                                Map<String, Object> bindingsValues = new HashMap<>();
                                bindingsValues.put("graph", graph);
                                LOGGER.debug("fetching descendants of the renamed folder...");
                                List<Vertex> vertices = GraphManager.getInstance().executeGremlinGroovyScript(script, bindingsValues);
                                //GraphManager.getInstance().printVertices(vertices, "path");
                                LOGGER.debug("Removing " + vertices.size() + " child from the graph...");
                                // remove its descendants vertex
                                for (Vertex v : vertices) {
                                    // remove edges parent ---contains--> child
                                    v.remove();
                                }

                                // remove folder vertex
                                vertex.remove();
                                */

                                // remove subtree recursively
                                GraphManager.getInstance().recursiveRemoveVertex(graph, vertex);

                                LOGGER.debug("all children are removed from graph");

                                // rewrite the graph.json file to disk
                                GraphManager.getInstance().updateProjectGraphAndSaveItToDisk(graph, projectUuid, userUuid, false);
                                // update graph cache
                                GraphCache.getInstance().cacheGraph(projectUuid, graph);
                                LOGGER.debug("graph cache updated");
                            } catch (Exception e) {
                                response.type("text/plain; charset=utf-8");
                                response.status(500);
                                String reason = e.getMessage();
                                response.body(reason);
                                return reason;
                            }
                        } else if (fileToDelete.isFile()) { // removing a single file
                            try {
                                // delete it
                                try {
                                    FileUtils.deleteQuietly(fileToDelete);
                                } catch (Exception ioExp1) {
                                    Thread.sleep(500);
                                    try {
                                        FileUtils.deleteQuietly(fileToDelete);
                                    } catch (Exception ioExp2) {
                                        Thread.sleep(500);
                                        FileUtils.deleteQuietly(fileToDelete);
                                    }
                                }
                                // once deleted => update the graph (remove its node from the graph)
                                //vertex.remove();
                                GraphManager.getInstance().recursiveRemoveVertex(graph, vertex);

                                LOGGER.debug("file node removed from graph");
                                // rewrite the graph.json file to disk
                                GraphManager.getInstance().updateProjectGraphAndSaveItToDisk(graph, projectUuid, userUuid, false);
                                // update graph cache
                                GraphCache.getInstance().cacheGraph(projectUuid, graph);
                                LOGGER.debug("graph cache updated");
                            } catch (Exception e) {
                                response.type("text/plain; charset=utf-8");
                                response.status(500);
                                String reason = e.getMessage();
                                response.body(reason);
                                return reason;
                            }
                        }
                    }

                    timer.end();
                    LOGGER.debug("FeJsTreeContextMenuHandler DELETE_NODE done in : " + timer.durationInSeconds() + " s");
                    // send response
                    response.status(204);
                    return "";
                }
                case "CREATE_NEW_FOLDER": {
                    String nodeId = request.queryParams("nodeId");
                    String newFolderName = request.queryParams("new_folder_name");

                    // check if nodeId is valid
                    if (nodeId == null || nodeId.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide node id!";
                        response.body(reason);
                        return reason;
                    }

                    // check newFolderName
                    if (newFolderName == null || newFolderName.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a new name";
                        response.body(reason);
                        return reason;
                    } else {
                        // check newFolderName is valid using regex
                        newFolderName = newFolderName.trim();
                        Matcher matcher = regexInvalidFileName.matcher(newFolderName);
                        if (matcher.matches()) { // folder name is not valid
                            response.type("text/plain; charset=utf-8");
                            response.status(400);
                            String reason = "Invalid folder name, it must not contain: , ; ? | / \\ ' < > ^ * % ! \" space tabulation";
                            response.body(reason);
                            return reason;
                        }
                    }

                    // get vertex of that folder that will contains the new folder
                    Vertex parentVertex = GraphManager.getInstance().getVertexById(graph, nodeId);

                    // check if vertex is not null
                    if (parentVertex == null) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Parent folder does not exist in project graph!";
                        response.body(reason);
                        return reason;
                    }

                    File parentFolder = new File(parentVertex.value("path").toString());

                    if (!parentFolder.exists()) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Parent folder does not exists in the filesystem!";
                        response.body(reason);
                        return reason;
                    }

                    if (!parentFolder.isDirectory()) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Parent folder is not a directory!";
                        response.body(reason);
                        return reason;
                    }

                    // check if a folder/file named as newFolderName already exists
                    File newFolder = new File(parentFolder, newFolderName);
                    if (newFolder.exists()) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "A file with the same name already exists!";
                        response.body(reason);
                        return reason;
                    } else { // create the new folder => update the graph
                        LOGGER.debug("creating a new folder named \"" + newFolderName + "\" inside folder: : " + parentVertex.value("path").toString());
                        if (newFolder.mkdirs()) {
                            // update the graph
                            int nbVertices = toIntExact(IteratorUtils.count(graph.vertices()));
                            int nbEdges = toIntExact(IteratorUtils.count(graph.edges()));
                            boolean doneV = false;
                            boolean doneE = false;
                            int newFolderVertexId = -1;
                            int newFolderEdgeId = -1;
                            // ensure that the vertex ID is not duplicated
                            while (!doneV) {
                                newFolderVertexId = generateRandomInt(nbVertices, nbVertices + 100000);
                                if (GraphManager.getInstance().getVertexById(graph, String.valueOf(newFolderVertexId)) == null) {
                                    doneV = true;
                                }
                            }
                            // ensure that the edge ID is not duplicated
                            while (!doneE) {
                                newFolderEdgeId = generateRandomInt(nbEdges, nbEdges + 100000);
                                if (GraphManager.getInstance().getEdgeById(graph, String.valueOf(newFolderEdgeId)) == null) {
                                    doneE = true;
                                }
                            }
                            Vertex newFolderVertex = graph.addVertex(T.id, newFolderVertexId, T.label, "directory", "path", newFolder.getPath(), "name", newFolder.getName());
                            parentVertex.addEdge("contains", newFolderVertex, T.id, newFolderEdgeId);

                            LOGGER.debug("added new folder vertex to graph, its id is :" + newFolderVertex.id().toString() + ", its inEdge id is: " + newFolderEdgeId);
                            // rewrite the graph.json file to disk
                            GraphManager.getInstance().updateProjectGraphAndSaveItToDisk(graph, projectUuid, userUuid, false);
                            // update graph cache
                            GraphCache.getInstance().cacheGraph(projectUuid, graph);
                            LOGGER.debug("graph cache updated");
                            // return new node info for jstree
                            JSONObject jsTreeJson = new JSONObject();
                            jsTreeJson.put("id", newFolderVertex.id().toString());
                            jsTreeJson.put("type", "folder");

                            JSONObject state = new JSONObject();
                            state.put("opened", true);
                            state.put("selected", false);
                            jsTreeJson.put("state", state);
                            jsTreeJson.put("text", newFolderName + " (0)");
                            jsTreeJson.put("data", new JSONObject());
                            jsTreeJson.put("children", false);
                            String res = jsTreeJson.toJSONString();
                            timer.end();
                            LOGGER.debug("FeJsTreeContextMenuHandler CREATE_NEW_FOLDER done in : " + timer.durationInSeconds() + " s, new node : " + res);
                            // send response
                            response.type("application/json; charset=UTF-8");
                            response.status(200);
                            return res;
                        } else {
                            response.type("text/plain; charset=utf-8");
                            response.status(500);
                            String reason = "Failed to create new folder!";
                            response.body(reason);
                            return reason;
                        }
                    }
                }
                case "ADD_FILES": {
                    String subAction = request.queryParams("subAction");

                    // check if subAction is valid
                    if (subAction == null || subAction.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "subAction parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    switch (subAction) {
                        case "UPLOAD_FILE": {
                            String tmpFolderName = request.queryParams("tmpFolderName");
                            if (tmpFolderName == null || tmpFolderName.equals("")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "temporary upload folder name parameter not found!";
                                response.body(reason);
                                return reason;
                            }

                            String fileNameParam = request.queryParams("fileName");
                            if (fileNameParam == null || fileNameParam.equals("")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "file name not found!";
                                response.body(reason);
                                return reason;
                            }

                            // check fileName is valid using regex
                            String fileNameDecoded = new String(Base64.getDecoder().decode(fileNameParam), "UTF-8");
                            String fileName = fileNameDecoded.trim();
                            /*
                            Matcher matcher = regexInvalidFileName.matcher(fileName);
                            if (matcher.matches()) { // folder name is not valid
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "Invalid file name \"" + fileName + "\", it must not contain: , ; ? | / \\ ' < > ^ * % ! \" space tabulation";
                                response.body(reason);
                                return reason;
                            }*/

                            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

                            InputStream is = request.raw().getPart("uploaded_file").getInputStream();
                            // save file to tmp folder
                            byte[] buffer = new byte[is.available()];
                            is.read(buffer);


                            File tmpFolder = new File(Configurator.getInstance().getUserTmpFolderPath(userUuid)
                                    + File.separator + tmpFolderName);
                            File tmpFile = new File(tmpFolder, fileName);


                            if (!tmpFolder.exists()) {
                                tmpFile.mkdirs();
                            }

                            if (tmpFile.exists()) {
                                if (!tmpFile.delete())
                                    tmpFile.deleteOnExit();
                            } else {
                                tmpFile.createNewFile();
                            }

                            OutputStream outStream = new FileOutputStream(tmpFile);
                            outStream.write(buffer);
                            outStream.close();
                            is.close();

                            response.status(204);
                            return "";
                        }
                        case "REMOVE_FILE": {
                            String tmpFolderName = request.queryParams("tmpFolderName");
                            String fileName = request.queryParams("fileName");

                            if (tmpFolderName == null || tmpFolderName.equals("")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "temporary upload folder name parameter not found!";
                                response.body(reason);
                                return reason;
                            }

                            if (fileName == null || fileName.equals("")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "file name parameter not found!";
                                response.body(reason);
                                return reason;
                            }

                            File tmpFolder = new File(Configurator.getInstance().getUserTmpFolderPath(userUuid)
                                    + File.separator + tmpFolderName);
                            File tmpFile = new File(tmpFolder, fileName);

                            if (tmpFile.exists()) {
                                if (!tmpFile.delete()) {
                                    tmpFile.deleteOnExit();
                                }
                            }

                            response.status(204);
                            return "";
                        }
                        case "REMOVE_TMP_FOLDER": {
                            String tmpFolderName = request.queryParams("tmpFolderName");

                            if (tmpFolderName == null || tmpFolderName.equals("")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "temporary upload folder parameter not found!";
                                response.body(reason);
                                return reason;
                            }

                            File tmpFolder = new File(Configurator.getInstance().getUserTmpFolderPath(userUuid)
                                    + File.separator + tmpFolderName);

                            if (tmpFolder.exists()) {
                                try {
                                    FileUtils.deleteDirectory(tmpFolder);
                                } catch (Exception e) {
                                    LOGGER.error(e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                            response.status(204);
                            return "";
                        }
                        case "SUBMIT": {
                            String tmpFolderName = request.queryParams("tmpFolderName");
                            String nodeId = request.queryParams("nodeId");

                            if (tmpFolderName == null || tmpFolderName.equals("")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "temporary upload folder parameter not found!";
                                response.body(reason);
                                return reason;
                            }

                            if (nodeId == null || nodeId.equals("")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "nodeId parameter not found!";
                                response.body(reason);
                                return reason;
                            }

                            File tmpFolder = new File(Configurator.getInstance().getUserTmpFolderPath(userUuid)
                                    + File.separator + tmpFolderName);

                            // check tmp upload folder exists
                            if (!tmpFolder.exists()) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "The temporary folder does not exist in the filesystem!";
                                response.body(reason);
                                return reason;
                            }

                            // check tmp upload is a folder
                            if (!tmpFolder.isDirectory()) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "The temporary folder is not a directory!";
                                response.body(reason);
                                return reason;
                            }

                            // get vertex of the destination folder (folder where we will move uploaded files)
                            Vertex vertexDestination = GraphManager.getInstance().getVertexById(graph, nodeId);

                            // check if vertex is not null
                            if (vertexDestination == null) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "The destination folder does not exist in project graph!";
                                response.body(reason);
                                return reason;
                            }

                            // check if vertex is a folder
                            if (vertexDestination.label().equals("file")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "The destination must be a folder!";
                                response.body(reason);
                                return reason;
                            }

                            File destinationFolder = new File(vertexDestination.value("path").toString());
                            LOGGER.debug("destinationFolder path: " + destinationFolder.getPath());
                            // check destination folder
                            if (!destinationFolder.exists()) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "The destination folder does not exist in the filesystem!";
                                response.body(reason);
                                return reason;
                            }
                            // check destination is a directory
                            if (!destinationFolder.isDirectory()) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "The destination folder exists but it is not a directory!";
                                response.body(reason);
                                return reason;
                            }

                            /*
                            ********************LOGIC HERE ********************
                             */

                            // get names of newly uploaded files from the temp folder and put the in a list
                            File[] tmpFiles = tmpFolder.listFiles();
                            if (tmpFiles == null || tmpFiles.length == 0) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "The temporary folder is empty!";
                                response.body(reason);
                                return reason;
                            }
                            List<String> newFileNames = new ArrayList<>();
                            for (File newFile : tmpFiles) {
                                newFileNames.add(newFile.getName());
                                LOGGER.debug("new file name: " + newFile.getName());
                            }

                            // get names existing file names inside the destination folder and put them in a list
                            LOGGER.debug("fetching children files (not directory) of the destination folder...");
                            List<Vertex> children = graph.traversal().V(vertexDestination.id()).out("contains").hasLabel("file").toList();
                            LOGGER.debug("Found " + children.size() + " file");
                            List<String> existingFileNames = new ArrayList<>();
                            for (Vertex child : children) {
                                String existingFileName = child.value("name").toString();
                                LOGGER.debug("Found file name: " + existingFileName);
                                existingFileNames.add(existingFileName);
                            }

                            // check duplicates by comparing the two lists above (newFileNames and existingFileNames)=> ask user for copy strategy (override/rename)
                            List<String> intersection = new ArrayList<>();
                            for (String fName : newFileNames) {
                                if (existingFileNames.contains(fName)) {
                                    intersection.add(fName);
                                    LOGGER.debug("intersection file name: " + fName);
                                }
                            }

                            LOGGER.debug("intersection size : " + intersection.size());

                            if (intersection.size() > 0) { // one or more file name with same name already exists in the destination folder
                                // check the copy strategy (override/rename), if no copy strategy, prompt user to select one
                                String copyStrategy = request.queryParams("copyStrategy");
                                if (copyStrategy == null || copyStrategy.equals("")) {
                                    LOGGER.debug("waiting for copy strategy...");
                                    // ask user to select a strategy
                                    response.type("application/json; charset=UTF-8");
                                    response.status(200);
                                    JSONObject res = new JSONObject();
                                    res.put("message", "COPY_STRATEGY");
                                    res.put("intersection", intersection);
                                    return res;
                                } else {
                                    switch (copyStrategy) {
                                        case "OVERRIDE": {
                                            timer.end();
                                            LOGGER.debug("FeJsTreeContextMenuHandler ADD_FILES => OVERRIDE, done in : " + timer.durationInSeconds() + " s");
                                            response.type("application/json; charset=UTF-8");
                                            response.status(200);
                                            return logicForDuplicatesOverride(destinationFolder, tmpFolder, tmpFiles,
                                                    vertexDestination, graph, projectUuid, userUuid);
                                        }
                                        case "RENAME": {
                                            timer.end();
                                            LOGGER.debug("FeJsTreeContextMenuHandler ADD_FILES => RENAME, done in : " + timer.durationInSeconds() + " s");
                                            response.type("application/json; charset=UTF-8");
                                            response.status(200);
                                            return logicForDuplicateRename(destinationFolder, tmpFolder, tmpFiles,
                                                    vertexDestination, graph, projectUuid, userUuid);
                                        }
                                        default: {
                                            response.type("text/plain; charset=utf-8");
                                            response.status(400);
                                            String reason = "Unknown copy strategy, valid strategies are: OVERRIDE and RENAME!";
                                            response.body(reason);
                                            return reason;
                                        }
                                    }
                                }
                            } else { // zero duplicate, if copy files to filesystem with success => update graph => send new elements to jsTree
                                timer.end();
                                LOGGER.debug("FeJsTreeContextMenuHandler ADD_FILES, done in : " + timer.durationInSeconds() + " s");
                                response.type("application/json; charset=UTF-8");
                                response.status(200);
                                return logicForZeroDuplicate(destinationFolder, tmpFolder, tmpFiles,
                                        vertexDestination, graph, projectUuid, userUuid);
                            }
                        }
                        default: {
                            response.type("text/plain; charset=utf-8");
                            response.status(400);
                            String reason = "Unknown subAction parameter";
                            response.body(reason);
                            return reason;
                        }
                    }
                }
                case "REPLACE_FILE": {
                    String subAction = request.queryParams("subAction");

                    // check if subAction is valid
                    if (subAction == null || subAction.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "subAction parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    switch (subAction) {
                        case "UPLOAD_FILE": {
                            String tmpFolderName = request.queryParams("tmpFolderName");
                            String fileName = request.queryParams("fileName");

                            if (tmpFolderName == null || tmpFolderName.equals("")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "temporary upload folder name parameter not found!";
                                response.body(reason);
                                return reason;
                            }

                            if (fileName == null || fileName.equals("")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "file name parameter not found!";
                                response.body(reason);
                                return reason;
                            }

                            // check fileName is valid using regex
                            fileName = fileName.trim();
                            /*
                            Matcher matcher = regexInvalidFileName.matcher(fileName);
                            if (matcher.matches()) { // folder name is not valid
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "Invalid file name \"" + fileName + "\", it must not contain: , ; ? | / \\ ' < > ^ * % ! \" space tabulation";
                                response.body(reason);
                                return reason;
                            }*/

                            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

                            InputStream is = request.raw().getPart("uploaded_file").getInputStream();
                            // save file to tmp folder
                            byte[] buffer = new byte[is.available()];
                            is.read(buffer);


                            File tmpFolder = new File(Configurator.getInstance().getUserTmpFolderPath(userUuid)
                                    + File.separator + tmpFolderName);
                            File tmpFile = new File(tmpFolder, fileName);


                            if (!tmpFolder.exists()) {
                                tmpFile.mkdirs();
                            }

                            if (tmpFile.exists()) {
                                if (!tmpFile.delete())
                                    tmpFile.deleteOnExit();
                            } else {
                                tmpFile.createNewFile();
                            }

                            OutputStream outStream = new FileOutputStream(tmpFile);
                            outStream.write(buffer);
                            outStream.close();
                            is.close();

                            response.status(204);
                            return "";
                        }
                        case "REMOVE_FILE": {
                            String tmpFolderName = request.queryParams("tmpFolderName");
                            String fileName = request.queryParams("fileName");

                            if (tmpFolderName == null || tmpFolderName.equals("")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "temporary upload folder name parameter not found!";
                                response.body(reason);
                                return reason;
                            }

                            if (fileName == null || fileName.equals("")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "file name parameter not found!";
                                response.body(reason);
                                return reason;
                            }

                            File tmpFolder = new File(Configurator.getInstance().getUserTmpFolderPath(userUuid)
                                    + File.separator + tmpFolderName);
                            File tmpFile = new File(tmpFolder, fileName);

                            if (tmpFile.exists()) {
                                if (!tmpFile.delete()) {
                                    tmpFile.deleteOnExit();
                                }
                            }

                            response.status(204);
                            return "";
                        }
                        case "REMOVE_TMP_FOLDER": {
                            String tmpFolderName = request.queryParams("tmpFolderName");

                            if (tmpFolderName == null || tmpFolderName.equals("")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "temporary upload folder name parameter not found!";
                                response.body(reason);
                                return reason;
                            }

                            File tmpFolder = new File(Configurator.getInstance().getUserTmpFolderPath(userUuid)
                                    + File.separator + tmpFolderName);

                            if (tmpFolder.exists()) {
                                try {
                                    FileUtils.deleteDirectory(tmpFolder);
                                } catch (Exception e) {
                                    LOGGER.error(e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                            response.status(204);
                            return "";
                        }
                        case "SUBMIT": {
                            String tmpFolderName = request.queryParams("tmpFolderName");
                            String nodeId = request.queryParams("nodeId");

                            if (tmpFolderName == null || tmpFolderName.equals("")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "temporary upload folder parameter not found!";
                                response.body(reason);
                                return reason;
                            }

                            if (nodeId == null || nodeId.equals("")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "nodeId parameter not found!";
                                response.body(reason);
                                return reason;
                            }

                            File tmpFolder = new File(Configurator.getInstance().getUserTmpFolderPath(userUuid)
                                    + File.separator + tmpFolderName);

                            // check tmp upload folder exists
                            if (!tmpFolder.exists()) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "The temporary folder does not exist in the filesystem!";
                                response.body(reason);
                                return reason;
                            }

                            // check tmp upload is a folder
                            if (!tmpFolder.isDirectory()) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "The temporary folder is not a directory!";
                                response.body(reason);
                                return reason;
                            }

                            // get vertex of the file to be replaces
                            Vertex vertexFileToReplace = GraphManager.getInstance().getVertexById(graph, nodeId);

                            // check if vertex is not null
                            if (vertexFileToReplace == null) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "The file to replace does not exist in project graph!";
                                response.body(reason);
                                return reason;
                            }

                            // check if vertex is a folder
                            if (!vertexFileToReplace.label().equals("file")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "The element to replace must be a file!";
                                response.body(reason);
                                return reason;
                            }

                            File fileToReplace = new File(vertexFileToReplace.value("path").toString());
                            LOGGER.debug("fileToReplace path: " + fileToReplace.getPath());
                            // check if file exists
                            if (!fileToReplace.exists()) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "The file to replace does not exist in the filesystem!";
                                response.body(reason);
                                return reason;
                            }
                            // check if it is not a directory
                            if (!fileToReplace.isFile()) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "The element to replace exists but it is not a file!";
                                response.body(reason);
                                return reason;
                            }

                            // get the replacement file
                            File[] tmpFiles = tmpFolder.listFiles();
                            if (tmpFiles == null || tmpFiles.length == 0) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "The temporary folder is empty!";
                                response.body(reason);
                                return reason;
                            }

                            if (tmpFiles.length > 1) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "The temporary folder contains more than one file!";
                                response.body(reason);
                                return reason;
                            }

                            File parentOfFileToReplace = fileToReplace.getParentFile();
                            String fileToReplaceName = fileToReplace.getName();

                            // delete old file from filesystem
                            FileUtils.deleteQuietly(fileToReplace);
                            // copy new file to filesystem, keep the same name and path
                            FileUtils.copyFile(tmpFiles[0], new File(parentOfFileToReplace, fileToReplaceName));

                            timer.end();
                            LOGGER.debug("FeJsTreeContextMenuHandler REPLACE_FILE done in : " + timer.durationInSeconds() + " s");

                            response.status(204);
                            return "";
                        }
                    }
                }
                case "EXPLORER_FOLDER": {
                    String nodeId = request.queryParams("nodeId");
                    if (nodeId == null || nodeId.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "node ID parameter not found!";
                        response.body(reason);
                        return reason;
                    }

                    LOGGER.debug("Node id : " + nodeId);

                    // get vertex
                    Vertex vertexFolder = GraphManager.getInstance().getVertexById(graph, nodeId);
                    if (vertexFolder == null) {
                        response.type("text/plain; charset=utf-8");
                        response.status(404);
                        String reason = "This folder does not exist in the graph!";
                        response.body(reason);
                        return reason;
                    }

                    File folderFile = new File(vertexFolder.value("path").toString());
                    if (!folderFile.exists()) {
                        response.type("text/plain; charset=utf-8");
                        response.status(404);
                        String reason = "This folder does not exist filesystem!";
                        response.body(reason);
                        return reason;
                    }

                    JSONObject results = new JSONObject();

                    // fetch direct parent
                    String parentVertexId = null;
                    if(!vertexFolder.id().toString().equals("0")){
                        String parentFilePath = folderFile.getParentFile().getPath();
                        GraphTraversal<Vertex, org.apache.tinkerpop.gremlin.process.traversal.Path> query = graph
                                .traversal().V(vertexFolder.id()).repeat(in().simplePath()).until(hasId(0)).path();
                        while (query.hasNext()) {
                            org.apache.tinkerpop.gremlin.process.traversal.Path result = query.next();
                            for (int i = 0; i < result.size(); i++) {
                                TinkerVertex vrtx = result.get(i);
                                if (vrtx.value("path").toString().equals(parentFilePath)){
                                    parentVertexId = vrtx.id().toString();
                                    break;
                                }
                            }
                        }
                    }

                    LOGGER.debug("Parents vertex id : " + parentVertexId);
                    results.put("parentId", parentVertexId);
                    String projectFolderNameUuid = ApkToolProjectDao.getInstance().getByUuid(projectUuid).getProjectFolderNameUuid();
                    String currentPath = folderFile.getPath().replace(Configurator
                            .getInstance()
                            .getDecodedApkFolderPath(userUuid,projectFolderNameUuid, false), "")
                            .replace(File.separator, "/");
                    if(!currentPath.equals("")){
                        results.put("current_path", currentPath);
                    }else{
                        results.put("current_path", "/");
                    }


                    // fetch children and sort them
                    List<JSONObject> elements = new ArrayList<>();
                    List<Vertex> childrenFolders = graph.traversal().V(vertexFolder.id()).out("contains").hasLabel("directory").order().by("name").toList();
                    List<Vertex> childrenFiles = graph.traversal().V(vertexFolder.id()).out("contains").hasLabel("file").order().by("mimeType").toList();
                    List<Vertex> children = new ArrayList<>();
                    children.addAll(childrenFolders);
                    children.addAll(childrenFiles);
                    LOGGER.debug("Found " + children.size() + " file");
                    for (Vertex child : children) {
                        JSONObject element = new JSONObject();
                        element.put("nodeId", child.id().toString());
                        element.put("name", child.value("name").toString());
                        element.put("label", child.label());
                        File childFile = new File(child.value("path").toString());
                        if (!childFile.exists())
                            continue;

                        if (child.label().equals("file")) {
                            element.put("title", child.value("name").toString()+" - size: " + Utils.formatFileSize(childFile.length()));

                            String mimeTypeCategory = MimeTypes.getMimeTypeCategory(child.value("mimeType").toString());
                            element.put("mimeCategory", mimeTypeCategory);

                            switch (mimeTypeCategory) {
                                case MimeTypes.CATEGORY_LOADABLE_IMG: {
                                    // thumbnail only png, jpg and bmp extensions
                                    if (child.value("name").toString().toLowerCase().endsWith(".png")
                                            || child.value("name").toString().endsWith(".jpg")
                                            || child.value("name").toString().endsWith(".jpeg")
                                            || child.value("name").toString().endsWith(".bmp")
                                            || child.value("name").toString().endsWith(".gif")) {
                                        // create thumbnail only if image size > IMAGE_MAX_SIZE_THUMB bytes
                                        if (childFile.length() > Configurator.IMAGE_MAX_SIZE_THUMB) {
                                            String thumbImgAsBase64String =Utils.getImageThumbnailAsBase64String(childFile, Configurator.THUMB_IMG_MAX_DIM_PX);
                                            if(thumbImgAsBase64String != null) {
                                                element.put("thumb_b64", thumbImgAsBase64String);
                                            }else{
                                                element.put("thumb_link", "static/public/images/thumbnails/img_default.png");
                                            }
                                        } else { // otherwise send image bytes as base64 string
                                            element.put("thumb_b64",
                                                    Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(childFile.getPath()))));
                                        }
                                    } else {
                                        // other types of images => show default_image_icon
                                        element.put("thumb_link", "static/public/images/thumbnails/img_default.png");
                                    }
                                    break;
                                }
                                case MimeTypes.CATEGORY_EDITABLE_SMALI: {
                                    element.put("thumb_link", "static/public/images/thumbnails/img_smali.png");
                                    break;
                                }
                                case MimeTypes.CATEGORY_EDITABLE_XML: {
                                    element.put("thumb_link", "static/public/images/thumbnails/img_xml.png");
                                    break;
                                }
                                case MimeTypes.CATEGORY_EDITABLE_JSON: {
                                    element.put("thumb_link", "static/public/images/thumbnails/img_json.png");
                                    break;
                                }
                                case MimeTypes.CATEGORY_EDITABLE_HTML: {
                                    element.put("thumb_link", "static/public/images/thumbnails/img_html.png");
                                    break;
                                }
                                case MimeTypes.CATEGORY_EDITABLE_JS: {
                                    element.put("thumb_link", "static/public/images/thumbnails/img_js.png");
                                    break;
                                }
                                case MimeTypes.CATEGORY_EDITABLE: {
                                    element.put("thumb_link", "static/public/images/thumbnails/img_edit.png");
                                    break;
                                }
                                case MimeTypes.CATEGORY_EDITABLE_CSS: {
                                    element.put("thumb_link", "static/public/images/thumbnails/img_css.png");
                                    break;
                                }
                                case MimeTypes.CATEGORY_LOADABLE_PDF: {
                                    element.put("thumb_link", "static/public/images/thumbnails/img_pdf.png");
                                    break;
                                }
                                case MimeTypes.CATEGORY_LOADABLE_VIDEO: {
                                    element.put("thumb_link", "static/public/images/thumbnails/img_video.png");
                                    break;
                                }
                                case MimeTypes.CATEGORY_LOADABLE_AUDIO: {
                                    element.put("thumb_link", "static/public/images/thumbnails/img_audio.png");
                                    break;
                                }
                                case MimeTypes.CATEGORY_DOWNLOADABLE: {
                                    element.put("thumb_link", "static/public/images/thumbnails/img_unknown.png");
                                    break;
                                }
                                default: {
                                    element.put("thumb_link", "static/public/images/thumbnails/img_unknown.png");
                                    break;
                                }
                            }
                        } else {
                            element.put("title", child.value("name").toString());
                            element.put("thumb_link", "static/public/images/thumbnails/img_folder.png");
                        }
                        elements.add(element);
                    }
                    results.put("elements", elements);
                    timer.end();
                    LOGGER.debug("EXPLORER_FOLDER done in : " + timer.durationInSeconds() + " s");
                    response.type("application/json; charset=UTF-8");
                    response.status(200);
                    return results;
                }
                case "LOAD_NODE_DATA": {
                    /*
                    whenever the user clicks on a line number in the jsTree of search results, the client
                    code checks if the file is already loaded in the editor.
                    if the file is not loaded, this section of code is called.
                    The client send the GraphID of the file he wants to load, and gets in return
                    a json object that contains an array of parent node ids ( these nodes belong to the path
                    from the root to the parent folder of the file in question)
                    */
                    String nodeId = request.queryParams("nodeId");
                    // check if processId is valid
                    if (nodeId == null || nodeId.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a nodeId parameter!";
                        response.body(reason);
                        return reason;
                    }

                    JSONArray parentsArray = new JSONArray();
                    GraphTraversal<Vertex, org.apache.tinkerpop.gremlin.process.traversal.Path> query = graph.traversal().V(Integer.parseInt(nodeId)).repeat(in().simplePath()).until(hasId(0)).path();
                    while (query.hasNext()) {
                        org.apache.tinkerpop.gremlin.process.traversal.Path result = query.next();
                        for (int i = 0; i < result.size(); i++) {
                            TinkerVertex vrtx = result.get(i);
                            String id = vrtx.id().toString();
                            if(!id.equals(nodeId)) {
                                //LOGGER.debug("parent name: " + vrtx.value("name").toString());
                                parentsArray.add(id);
                            }
                        }
                    }
                    JSONObject result = new JSONObject();
                    result.put("parentsArray", parentsArray);
                    result.put("nodeId", nodeId);
                    // send response
                    response.type("application/json; charset=UTF-8");
                    response.status(200);
                    return result;
                }
                default: {
                    response.type("text/plain; charset=utf-8");
                    response.status(400);
                    String reason = "Unknown context menu action";
                    response.body(reason);
                    return reason;
                }
            }
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

    // generate a random id
    private int generateRandomInt(int min, int max) {
        return min + ran.nextInt(max - min + 1);
    }


    /*=====================================================
    ================ add new files logic ==================
     =====================================================*/


    // this method is called when adding new files with one or more duplicated names => using copy strategy "override"
    private JSONObject logicForDuplicatesOverride(File destinationFolder, File tmpFolder, File[] tmpFiles,
                                                  Vertex vertexDestination,
                                                  Graph graph, String projectUuid, String userUuid) throws ScriptException, IOException {

        JSONArray jsTreeNewNodes = new JSONArray(); // the object that will contain the new nodes for jsTree
        JSONArray jsTreeUpdatedNodes = new JSONArray(); // the object that will contain the overridden nodes for jsTree

        int nbVertices = toIntExact(IteratorUtils.count(graph.vertices()));
        int nbEdges = toIntExact(IteratorUtils.count(graph.edges()));

        for (File srcFile : tmpFiles) {
            try {
                File destFile = new File(destinationFolder, srcFile.getName());

                // if dest already exists => delete it and copy it again (override)
                // DO NOT add its vertex and edge as it already exists
                // add its jsTree data to jsTreeUpdatedNodes
                if (destFile.exists()) {
                    // delete old file from filesystem
                    FileUtils.deleteQuietly(destFile);
                    // copy new file to filesystem
                    FileUtils.copyFileToDirectory(srcFile, destinationFolder);

                    // get its vertex id by unique path
                    Vertex updatedFileVertex = graph.traversal().V().has("path", destFile.getPath()).toList().get(0);
                    LOGGER.debug("fetched existing file vertex from graph, its id is :" + updatedFileVertex.id().toString());

                    // populate the new jsTree node data to send to client
                    JSONObject jsTreeNode = new JSONObject();
                    JSONObject nodeData = new JSONObject();
                    jsTreeNode.put("id", updatedFileVertex.id().toString());
                    jsTreeNode.put("type", MimeTypes.getMimeTypeCategory(updatedFileVertex.value("mimeType").toString()));
                    jsTreeNode.put("text", updatedFileVertex.value("name").toString());
                    nodeData.put("mimeType", updatedFileVertex.value("mimeType").toString());
                    jsTreeNode.put("data", nodeData);

                    jsTreeUpdatedNodes.add(jsTreeNode);

                } else {
                    // if dest does not exists => copy new file to filesystem
                    // add its vertex and edge
                    // add its jsTree data to jsTreeNewNodes

                    FileUtils.copyFileToDirectory(srcFile, destinationFolder);

                    // generate random IDs for the vertex and for the edge
                    boolean doneV = false;
                    boolean doneE = false;
                    int newFileVertexId = -1;
                    int newFileEdgeId = -1;

                    while (!doneV) { // ensure that the vertex ID is not duplicated
                        newFileVertexId = generateRandomInt(nbVertices, nbVertices + 100000);
                        List<Vertex> lv = graph.traversal().V(newFileVertexId).toList();
                        if (lv.size() == 0) {
                            doneV = true;
                        }
                    }

                    while (!doneE) { // ensure that the edge ID is not duplicated
                        newFileEdgeId = generateRandomInt(nbEdges, nbEdges + 100000);
                        List<Edge> le = graph.traversal().E(newFileEdgeId).toList();
                        if (le.size() == 0) {
                            doneE = true;
                        }
                    }

                    // add vertex
                    Vertex newFileVertex = graph.addVertex(T.id, newFileVertexId,
                            T.label, "file", "path", destFile.getPath(), "name", destFile.getName(),
                            "mimeType", MimeTypes.getMimeType(FilenameUtils.getExtension(destFile.getName())));
                    // add edge
                    vertexDestination.addEdge("contains", newFileVertex, T.id, newFileEdgeId);
                    LOGGER.debug("added new file vertex to graph, its id is :" + newFileVertex.id().toString() + ", its inEdge id is: " + vertexDestination.id().toString());

                    // populate the new jsTree node data to send to client
                    JSONObject jsTreeNode = new JSONObject();
                    JSONObject nodeData = new JSONObject();
                    jsTreeNode.put("id", newFileVertex.id().toString());
                    jsTreeNode.put("type", MimeTypes.getMimeTypeCategory(newFileVertex.value("mimeType").toString()));
                    jsTreeNode.put("text", destFile.getName());
                    nodeData.put("mimeType", newFileVertex.value("mimeType").toString());
                    jsTreeNode.put("data", nodeData);

                    jsTreeNewNodes.add(jsTreeNode);
                }
                // increment for next iteration
                nbVertices += 1;
                nbEdges += 1;
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
            }
        }

        // rewrite the graph.json file to disk
        GraphManager.getInstance().updateProjectGraphAndSaveItToDisk(graph, projectUuid, userUuid, false);


        // update graph cache
        GraphCache.getInstance().cacheGraph(projectUuid, graph);
        LOGGER.debug("graph cache updated");

        // delete temp folder containing temp files
        try {
            FileUtils.deleteDirectory(tmpFolder);
        } catch (IOException e) {
            try {
                if (!tmpFolder.delete()) {
                    tmpFolder.deleteOnExit();
                }
            } catch (Exception e1) {
                e.printStackTrace();
            }

        }

        // return new nodes data to jsTree
        JSONObject res = new JSONObject();
        res.put("message", "COPY_SUCCESS");
        res.put("new_nodes", jsTreeNewNodes); // jsTree new nodes
        res.put("updated_nodes", jsTreeUpdatedNodes); // jsTree updates nodes => overridden
        return res;
    }

    // this method is called when adding new files with one or more duplicated names => using copy strategy "rename"
    private JSONObject logicForDuplicateRename(File destinationFolder, File tmpFolder, File[] tmpFiles,
                                               Vertex vertexDestination,
                                               Graph graph, String projectUuid, String userUuid) throws IOException {

        JSONArray jsTreeNewNodes = new JSONArray(); // the object that will contain the new nodes for jsTree

        int nbVertices = toIntExact(IteratorUtils.count(graph.vertices()));
        int nbEdges = toIntExact(IteratorUtils.count(graph.edges()));

        for (File srcFile : tmpFiles) {
            try {
                File destFile = new File(destinationFolder, srcFile.getName());

                // if dest already exists => keep it + add new file with a name : new_file_name.extension = old_file_name_NUMBER.extension
                // and add its jsTree data to jsTreeUpdatedNodes
                if (destFile.exists()) {
                    // create its new name

                    String baseName = FilenameUtils.getBaseName(destFile.getName()); // This will return the filename minus the path and extension.
                    String extension = FilenameUtils.getExtension(destFile.getName());

                    String newName = srcFile.getName();
                    boolean foundNewName = false;
                    int i = 1;
                    while (!foundNewName) {
                        if (extension.equals("")) {
                            newName = baseName + "_copy" + i;
                        } else {
                            newName = baseName + "_copy" + i + "." + extension;
                        }

                        File newFile = new File(destinationFolder, newName);
                        if (!newFile.exists()) {
                            foundNewName = true;
                        }
                        i++;
                    }
                    destFile = new File(destinationFolder, newName);
                }


                // copy new file to filesystem
                FileUtils.copyFile(srcFile, destFile);

                // generate random IDs for the vertex and for the edge
                boolean doneV = false;
                boolean doneE = false;
                int newFileVertexId = -1;
                int newFileEdgeId = -1;

                while (!doneV) { // ensure that the vertex ID is not duplicated
                    newFileVertexId = generateRandomInt(nbVertices, nbVertices + 100000);
                    List<Vertex> lv = graph.traversal().V(newFileVertexId).toList();
                    if (lv.size() == 0) {
                        doneV = true;
                    }
                }

                while (!doneE) { // ensure that the edge ID is not duplicated
                    newFileEdgeId = generateRandomInt(nbEdges, nbEdges + 100000);
                    List<Edge> le = graph.traversal().E(newFileEdgeId).toList();
                    if (le.size() == 0) {
                        doneE = true;
                    }
                }

                // add vertex
                Vertex newFileVertex = graph.addVertex(T.id, newFileVertexId,
                        T.label, "file", "path", destFile.getPath(), "name", destFile.getName(),
                        "mimeType", MimeTypes.getMimeType(FilenameUtils.getExtension(destFile.getName())));
                // add edge
                vertexDestination.addEdge("contains", newFileVertex, T.id, newFileEdgeId);
                LOGGER.debug("added new file vertex to graph, its id is :" + newFileVertex.id().toString() + ", its inEdge id is: " + vertexDestination.id().toString());

                // populate the new jsTree node data to send to client
                JSONObject jsTreeNode = new JSONObject();
                JSONObject nodeData = new JSONObject();
                jsTreeNode.put("id", newFileVertex.id().toString());
                jsTreeNode.put("type", MimeTypes.getMimeTypeCategory(newFileVertex.value("mimeType").toString()));
                jsTreeNode.put("text", destFile.getName());
                nodeData.put("mimeType", newFileVertex.value("mimeType").toString());
                jsTreeNode.put("data", nodeData);

                jsTreeNewNodes.add(jsTreeNode);

                // increment for next iteration
                nbVertices += 1;
                nbEdges += 1;
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
            }
        }

        // rewrite the graph.json file to disk
        GraphManager.getInstance().updateProjectGraphAndSaveItToDisk(graph, projectUuid, userUuid, false);


        // update graph cache
        GraphCache.getInstance().cacheGraph(projectUuid, graph);
        LOGGER.debug("graph cache updated");

        // delete temp folder containing temp files
        try {
            FileUtils.deleteDirectory(tmpFolder);
        } catch (IOException e) {
            try {
                if (!tmpFolder.delete()) {
                    tmpFolder.deleteOnExit();
                }
            } catch (Exception e1) {
                e.printStackTrace();
            }

        }

        // return new nodes data to jsTree
        JSONObject res = new JSONObject();
        res.put("message", "COPY_SUCCESS");
        res.put("new_nodes", jsTreeNewNodes); // jsTree new nodes
        res.put("updated_nodes", new JSONArray()); // jsTree updates nodes => overridden
        return res;
    }


    // this method is called when adding new files without any name duplicated => no need to override existing files or rename them
    private JSONObject logicForZeroDuplicate(File destinationFolder, File tmpFolder, File[] tmpFiles,
                                             Vertex vertexDestination,
                                             Graph graph, String projectUuid, String userUuid) throws ScriptException, IOException {
        JSONArray jsTreeNewNodes = new JSONArray(); // the object that will contain the new nodes for jsTree
        int nbVertices = toIntExact(IteratorUtils.count(graph.vertices()));
        int nbEdges = toIntExact(IteratorUtils.count(graph.edges()));

        for (File srcFile : tmpFiles) {
            try {
                File destFile = new File(destinationFolder, srcFile.getName());
                if (!destFile.exists()) { // if dest not exist copy it and add it to graph (vertex plus edge)
                    // copy new file to filesystem
                    FileUtils.copyFileToDirectory(srcFile, destinationFolder);

                    // add its vertex and edge to graph
                    boolean doneV = false;
                    boolean doneE = false;
                    int newFileVertexId = -1;
                    int newFileEdgeId = -1;

                    while (!doneV) { // ensure that the vertex ID is not duplicated
                        newFileVertexId = generateRandomInt(nbVertices, nbVertices + 100000);
                        List<Vertex> lv = graph.traversal().V(newFileVertexId).toList();
                        if (lv.size() == 0) {
                            doneV = true;
                        }
                    }

                    while (!doneE) { // ensure that the edge ID is not duplicated
                        newFileEdgeId = generateRandomInt(nbEdges, nbEdges + 100000);
                        List<Edge> le = graph.traversal().E(newFileEdgeId).toList();
                        if (le.size() == 0) {
                            doneE = true;
                        }
                    }

                    // add vertex
                    Vertex newFileVertex = graph.addVertex(T.id, newFileVertexId,
                            T.label, "file", "path", destFile.getPath(), "name", destFile.getName(),
                            "mimeType", MimeTypes.getMimeType(FilenameUtils.getExtension(destFile.getName())));
                    // add edge
                    vertexDestination.addEdge("contains", newFileVertex, T.id, newFileEdgeId);
                    LOGGER.debug("added new file vertex to graph, its id is :" + newFileVertex.id().toString() + ", its inEdge id is: " + vertexDestination.id().toString());

                    // populate the new jsTree node data to send to client
                    JSONObject jsTreeNewNode = new JSONObject();
                    JSONObject nodeData = new JSONObject();
                    jsTreeNewNode.put("id", newFileVertex.id().toString());
                    jsTreeNewNode.put("type", MimeTypes.getMimeTypeCategory(newFileVertex.value("mimeType").toString()));
                    jsTreeNewNode.put("text", destFile.getName());
                    nodeData.put("mimeType", newFileVertex.value("mimeType").toString());
                    jsTreeNewNode.put("data", nodeData);

                    jsTreeNewNodes.add(jsTreeNewNode);

                    // increment for next iteration
                    nbVertices += 1;
                    nbEdges += 1;
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                e.printStackTrace();
            }
        }

        // rewrite the graph.json file to disk
        GraphManager.getInstance().updateProjectGraphAndSaveItToDisk(graph, projectUuid, userUuid, false);

        // update graph cache
        GraphCache.getInstance().cacheGraph(projectUuid, graph);
        LOGGER.debug("graph cache updated");

        // delete temp folder containing temp files
        try {
            FileUtils.deleteDirectory(tmpFolder);
        } catch (IOException e) {
            try {
                if (!tmpFolder.delete()) {
                    tmpFolder.deleteOnExit();
                }
            } catch (Exception e1) {
                e.printStackTrace();
            }

        }

        // return new nodes data to jsTree
        JSONObject res = new JSONObject();
        res.put("message", "COPY_SUCCESS");
        res.put("new_nodes", jsTreeNewNodes); // jsTree new nodes
        res.put("updated_nodes", new JSONArray()); // jsTree updates nodes
        return res;
    }

}
