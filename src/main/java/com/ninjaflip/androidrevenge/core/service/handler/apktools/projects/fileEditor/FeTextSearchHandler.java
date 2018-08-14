package com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.fileEditor;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.enums.EnumerationApkTool;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.UserProcessBuilder;
import com.ninjaflip.androidrevenge.core.apktool.filecomputing.FileComputingManager;
import com.ninjaflip.androidrevenge.core.apktool.filecomputing.TextSearchResultPerFile;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.AppenderStreamer;
import com.ninjaflip.androidrevenge.core.apktool.socketstreamer.SocketMessagingProtocol;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import com.ninjaflip.androidrevenge.core.service.cache.GraphCache;
import com.ninjaflip.androidrevenge.beans.containers.FixedSizeLinkedHashMap;
import com.ninjaflip.androidrevenge.websocket.EchoWebSocket;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerVertex;
import org.eclipse.jetty.websocket.api.Session;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.hasId;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.in;

/**
 * Created by Solitario on 11/09/2017.
 * <p>
 * Handler that takes care of text search, and text search and replace
 */
public class FeTextSearchHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(FeTextSearchHandler.class);
    private static Map<String, Object> textSearchCache = new FixedSizeLinkedHashMap<>(20);

    @Override
    public Object handle(Request request, Response response) {

        String userUuid = ServerSecurityManager.getInstance()
                .getUserUuidFromToken(request.cookie("token"));

        String action = request.queryParams("action");
        String projectUuid = request.queryParams("projectUuid");


        try {
            // check if action is valid
            if (action == null || action.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "You must provide a search action!";
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


            // business code
            switch (action) {
                case "SEARCH": {
                    // user's current running processes
                    List<UserProcessBuilder> processes = UserProcessBuilder.getUserRunningProcesses(userUuid);
                    if (processes.size() > 0) { // check if there is an ongoing process => if exist ask user to wait until that process is finished
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

                    String searchQuery = request.queryParams("searchQuery");
                    // check if searchQuery is valid
                    if (searchQuery == null || searchQuery.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a search text!";
                        response.body(reason);
                        return reason;
                    }

                    String caseSensitive = request.queryParams("caseSensitive");
                    // check if searchQuery is valid
                    if (caseSensitive == null || caseSensitive.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a case sensitivity paramater!";
                        response.body(reason);
                        return reason;
                    }

                    boolean isCaseSensitive = Boolean.parseBoolean(caseSensitive);
                    Session session = EchoWebSocket.getUserSession(userUuid);
                    AppenderStreamer appenderStreamer = new AppenderStreamer(session, EnumerationApkTool.EnumLogType.TEXT_SEARCH);
                    // create the search process
                    UserProcessBuilder searchProcessBuilder = new UserProcessBuilder(userUuid, EnumerationApkTool.EnumProcessType.TEXT_SEARCH) {
                        @Override
                        public void buildProcessLogic() {
                            try {
                                // output logging to the web-socket
                                if (session != null && session.isOpen()) {
                                    org.apache.log4j.Logger.getLogger(FileComputingManager.class).addAppender(appenderStreamer);
                                } else {
                                    if (session == null) {
                                        System.err.println("Null web socket");
                                        throw new IllegalArgumentException("WebSocket not found!");
                                    } else if (!session.isOpen()) {
                                        System.err.println("Web socket closed");
                                        throw new IllegalArgumentException("WebSocket closed!");
                                    }
                                }
                                ExecutionTimer timer = new ExecutionTimer();
                                timer.start();
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.STARTED, EnumerationApkTool.EnumProcessType.TEXT_SEARCH);

                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "&nbsp;", EnumerationApkTool.EnumLogType.TEXT_SEARCH);
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "********************************", EnumerationApkTool.EnumLogType.TEXT_SEARCH);
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "********* Text Search **********", EnumerationApkTool.EnumLogType.TEXT_SEARCH);
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "********************************", EnumerationApkTool.EnumLogType.TEXT_SEARCH);
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "&nbsp;", EnumerationApkTool.EnumLogType.TEXT_SEARCH);


                                String projectFolderNameUuid = ApkToolProjectDao.getInstance().getByUuid(projectUuid).getProjectFolderNameUuid();
                                // get all non media files as vertices ==> preparing text search
                                List<String> listMime = new ArrayList<>();
                                listMime.add("image");
                                listMime.add("audio");
                                listMime.add("video");
                                List<Vertex> listVertices = GraphManager.getInstance().graphSearchExcludeMimeTypes(
                                        userUuid
                                        , projectFolderNameUuid
                                        , false
                                        , listMime, null);
                                // search result object
                                Object[] searchResultObject = FileComputingManager.getInstance()
                                        .textSearch(listVertices, searchQuery, isCaseSensitive, false, false);
                                textSearchCache.put(this.getId(), searchResultObject);
                                timer.end();

                                SocketMessagingProtocol.getInstance().sendLogEvent(session, " ", EnumerationApkTool.EnumLogType.TEXT_SEARCH);
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "###### Text Search Task finished successfully in "
                                        + ExecutionTimer.getTimeString(timer.durationInSeconds()), EnumerationApkTool.EnumLogType.TEXT_SEARCH);

                                // notify process finished
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.COMPLETED, EnumerationApkTool.EnumProcessType.TEXT_SEARCH);

                            } catch (Exception e) {
                                System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                                //e.printStackTrace();
                                try {
                                    SocketMessagingProtocol.getInstance()
                                            .sendLogEvent(session, e.getMessage(), EnumerationApkTool.EnumLogType.TEXT_SEARCH);
                                    // notify process error
                                    SocketMessagingProtocol.getInstance()
                                            .sendProcessState(session, this.getId(),
                                                    EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.TEXT_SEARCH);
                                } catch (Exception ex) {
                                    // notify process error

                                    SocketMessagingProtocol.getInstance()
                                            .sendProcessState(session, this.getId(),
                                                    EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.TEXT_SEARCH);

                                    System.err.println("Socket transmission error : " + ex.getMessage());
                                    //ex.printStackTrace();
                                }
                            } finally {
                                try {
                                    org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);
                                    System.gc();
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void cleanOperation() {
                            // remove log appender
                            try {
                                org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);
                                System.out.println("cleanOperation => log appender because of un-achieved operation");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    };
                    searchProcessBuilder.execute();
                    //res.put("processId", searchProcessBuilder.getId());
                    response.status(204);
                    return "";
                }
                case "SEARCH_AND_REPLACE": {
                    // user's current running processes
                    List<UserProcessBuilder> processes = UserProcessBuilder.getUserRunningProcesses(userUuid);
                    if (processes.size() > 0) { // check if there is an ongoing process => if exist ask user to wait until that process is finished
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

                    String searchQuery = request.queryParams("searchQuery");
                    // check if searchQuery is valid
                    if (searchQuery == null || searchQuery.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a search text!";
                        response.body(reason);
                        return reason;
                    }

                    String replacement = request.queryParams("replacement");
                    // check if replacement is valid
                    if (replacement == null || replacement.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a replacement text!";
                        response.body(reason);
                        return reason;
                    }

                    String caseSensitive = request.queryParams("caseSensitive");
                    // check if searchQuery is valid
                    if (caseSensitive == null || caseSensitive.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a case sensitivity parameter!";
                        response.body(reason);
                        return reason;
                    }

                    boolean isCaseSensitive = Boolean.parseBoolean(caseSensitive);
                    Session session = EchoWebSocket.getUserSession(userUuid);
                    AppenderStreamer appenderStreamer = new AppenderStreamer(session, EnumerationApkTool.EnumLogType.TEXT_SEARCH);

                    // create the search/replace process
                    UserProcessBuilder searchAndReplaceProcessBuilder = new UserProcessBuilder(userUuid, EnumerationApkTool.EnumProcessType.TEXT_SEARCH_AND_REPLACE) {
                        @Override
                        public void buildProcessLogic() {
                            try {
                                // output logging to the web-socket
                                if (session != null && session.isOpen()) {
                                    org.apache.log4j.Logger.getLogger(FileComputingManager.class).addAppender(appenderStreamer);
                                } else {
                                    if (session == null) {
                                        System.err.println("Null web socket");
                                        throw new IllegalArgumentException("WebSocket not found!");
                                    } else if (!session.isOpen()) {
                                        System.err.println("Web socket closed");
                                        throw new IllegalArgumentException("WebSocket closed!");
                                    }
                                }
                                ExecutionTimer timer = new ExecutionTimer();
                                timer.start();
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.STARTED, EnumerationApkTool.EnumProcessType.TEXT_SEARCH_AND_REPLACE);

                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "&nbsp;", EnumerationApkTool.EnumLogType.TEXT_SEARCH);
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "********************************", EnumerationApkTool.EnumLogType.TEXT_SEARCH);
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "***** Text Find/Replace ********", EnumerationApkTool.EnumLogType.TEXT_SEARCH);
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "********************************", EnumerationApkTool.EnumLogType.TEXT_SEARCH);
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "&nbsp;", EnumerationApkTool.EnumLogType.TEXT_SEARCH);


                                String projectFolderNameUuid = ApkToolProjectDao.getInstance().getByUuid(projectUuid).getProjectFolderNameUuid();
                                // get all non media files as vertices ==> preparing text search
                                List<String> listMime = new ArrayList<>();
                                listMime.add("image");
                                listMime.add("audio");
                                listMime.add("video");
                                List<Vertex> listVertices = GraphManager.getInstance().graphSearchExcludeMimeTypes(
                                        userUuid
                                        , projectFolderNameUuid
                                        , false
                                        , listMime
                                        , null);
                                // search result object
                                Object[] searchAndReplaceResultObject = FileComputingManager.getInstance().textSearchAndReplace(listVertices, searchQuery, replacement, isCaseSensitive);
                                textSearchCache.put(this.getId(), searchAndReplaceResultObject);

                                timer.end();

                                SocketMessagingProtocol.getInstance().sendLogEvent(session, " ", EnumerationApkTool.EnumLogType.TEXT_SEARCH);
                                SocketMessagingProtocol.getInstance().sendLogEvent(session, "###### Text Find/Replace Task finished successfully in "
                                        + ExecutionTimer.getTimeString(timer.durationInSeconds()), EnumerationApkTool.EnumLogType.TEXT_SEARCH);

                                // notify process finished
                                SocketMessagingProtocol.getInstance()
                                        .sendProcessState(session, this.getId(),
                                                EnumerationApkTool.EnumProcessState.COMPLETED, EnumerationApkTool.EnumProcessType.TEXT_SEARCH_AND_REPLACE);

                            } catch (Exception e) {
                                System.err.println("execute => process " + this.getId() + " finished with error : " + e.getMessage());
                                e.printStackTrace();
                                try {
                                    SocketMessagingProtocol.getInstance()
                                            .sendLogEvent(session, e.getMessage(), EnumerationApkTool.EnumLogType.TEXT_SEARCH);
                                    // notify process error
                                    SocketMessagingProtocol.getInstance()
                                            .sendProcessState(session, this.getId(),
                                                    EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.TEXT_SEARCH_AND_REPLACE);
                                } catch (Exception ex) {
                                    // notify process error
                                    SocketMessagingProtocol.getInstance()
                                            .sendProcessState(session, this.getId(),
                                                    EnumerationApkTool.EnumProcessState.ERROR, EnumerationApkTool.EnumProcessType.TEXT_SEARCH_AND_REPLACE);

                                    System.err.println("Socket transmission error : " + ex.getMessage());
                                    ex.printStackTrace();
                                }
                            } finally {
                                try {
                                    org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);
                                    System.gc();
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void cleanOperation() {
                            // remove log appender
                            try {
                                org.apache.log4j.Logger.getLogger(FileComputingManager.class).removeAppender(appenderStreamer);
                                System.out.println("cleanOperation => log appender because of un-achieved operation");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    };
                    searchAndReplaceProcessBuilder.execute();
                    //res.put("processId", searchAndReplaceProcessBuilder.getId());
                    response.status(204);
                    return "";
                }
                case "GET_RESULT_TEXT_SEARCH": {
                    String processId = request.queryParams("processId");
                    // check if processId is valid
                    if (processId == null || processId.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a search processId!";
                        response.body(reason);
                        return reason;
                    }

                    Object[] searchResult = (Object[]) textSearchCache.get(processId);
                    // transform search result to jsTree json  data and send it back to client for display
                    int nbOccurrences = (int) searchResult[0];
                    if (nbOccurrences == 0) {
                        // remove element from cache
                        textSearchCache.remove(processId);
                        response.type("application/json; charset=UTF-8");
                        response.status(200);
                        return "[]";
                    }

                    List<TextSearchResultPerFile> searchSummary = (ArrayList<TextSearchResultPerFile>) searchResult[1];
                    int nbFiles = searchSummary.size();


                    JSONArray resultJsonArray = new JSONArray();
                    int id = 0;

                    // jstree node object : root
                    JSONObject rootNode = new JSONObject();
                    rootNode.put("id", String.valueOf(id));
                    rootNode.put("text", "Found " + nbOccurrences + " hit(s) in " + nbFiles + " file(s) for term '" + searchResult[3] + "' using case_sensitive='" + searchResult[4] + "'");
                    rootNode.put("type", "root");
                    JSONObject stateRoot = new JSONObject();
                    stateRoot.put("opened", false);
                    stateRoot.put("selected", false);
                    rootNode.put("state", stateRoot);

                    JSONObject rootNodeData = new JSONObject();
                    rootNodeData.put("search_term", searchResult[3]);
                    rootNodeData.put("case_sensitive", searchResult[4]);
                    rootNode.put("data", rootNodeData);


                    JSONArray rootChildren = new JSONArray();
                    id++;
                    for (TextSearchResultPerFile e : searchSummary) {
                        // get file vertex by its path
                        Vertex fileVertex = graph.traversal().V().has("path", e.getFilePath()).toList().get(0);
                        // relative path
                        String projectFolderNameUuid = ApkToolProjectDao.getInstance().getByUuid(projectUuid).getProjectFolderNameUuid();
                        String relativeNodePath = e.getFilePath().replace(Configurator.getInstance()
                                .getDecodedApkFolderPath(userUuid, projectFolderNameUuid, false) + File.separator, "").replace(File.separator, "/");
                        // jstree node object : file
                        JSONObject fileNode = new JSONObject();
                        fileNode.put("id", String.valueOf(id));
                        fileNode.put("text", "Found " + e.getNbOccurrences() + " hit(s) in file '" + relativeNodePath + "'");
                        fileNode.put("type", "file");
                        JSONObject stateFile = new JSONObject();
                        stateFile.put("opened", false);
                        stateFile.put("selected", false);
                        fileNode.put("state", stateFile);
                        JSONArray fileChildren = new JSONArray();
                        id++;


                        for (int i = 0; i < e.getArrayResults().size(); i++) {
                            JSONObject obj = (JSONObject) e.getArrayResults().get(i);
                            // jstree node object : line
                            JSONObject lineNode = new JSONObject();
                            lineNode.put("id", String.valueOf(id));
                            lineNode.put("text", "Line number " + obj.get("line"));
                            lineNode.put("type", "line");
                            JSONObject stateLine = new JSONObject();
                            stateLine.put("opened", false);
                            stateLine.put("selected", false);
                            lineNode.put("state", stateLine);
                            JSONObject nodeData = new JSONObject();
                            nodeData.put("fileNodeId", fileVertex.id().toString());
                            nodeData.put("lineNumber", obj.get("line"));
                            nodeData.put("start", obj.get("start"));
                            nodeData.put("end", obj.get("end"));
                            lineNode.put("data", nodeData);
                            id++;

                            fileChildren.add(lineNode);
                        }
                        fileNode.put("children", fileChildren);

                        rootChildren.add(fileNode);
                    }
                    rootNode.put("children", rootChildren);

                    resultJsonArray.add(rootNode);

                    // remove element from cache
                    textSearchCache.remove(processId);

                    // send response
                    response.type("application/json; charset=UTF-8");
                    response.status(200);
                    return resultJsonArray;
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
                    GraphTraversal<Vertex, Path> query = graph.traversal().V(Integer.parseInt(nodeId)).repeat(in().simplePath()).until(hasId(0)).path();
                    while (query.hasNext()) {
                        Path result = query.next();
                        for (int i = 0; i < result.size(); i++) {
                            TinkerVertex vrtx = result.get(i);
                            String id = vrtx.id().toString();
                            if (!id.equals(nodeId)) {
                                LOGGER.debug("parent name: " + vrtx.value("name").toString());
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
                case "GET_RESULT_TEXT_SEARCH_AND_REPLACE": {
                    String processId = request.queryParams("processId");
                    // check if processId is valid
                    if (processId == null || processId.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a search processId!";
                        response.body(reason);
                        return reason;
                    }

                    JSONObject result = new JSONObject();

                    Object[] searchAnReplaceResult = (Object[]) textSearchCache.get(processId);
                    // transform search result to jsTree json  data and send it back to client for display
                    int nbOccurrences = (int) searchAnReplaceResult[0];
                    if (nbOccurrences == 0) {
                        // remove element from cache
                        textSearchCache.remove(processId);
                        result.put("modified_file_ids", new HashSet<>());
                        result.put("nb_files", 0);
                        response.type("application/json; charset=UTF-8");
                        response.status(200);
                        return result;
                    }

                    List<TextSearchResultPerFile> searchSummary = (ArrayList<TextSearchResultPerFile>) searchAnReplaceResult[1];
                    int nbFiles = searchSummary.size();

                    Set<String> graphIDsOfModifiedFiles = new HashSet<>();

                    for (TextSearchResultPerFile e : searchSummary) {
                        // get file vertex by its path
                        Vertex fileVertex = graph.traversal().V().has("path", e.getFilePath()).toList().get(0);
                        graphIDsOfModifiedFiles.add(fileVertex.id().toString());
                    }

                    // remove element from cache
                    textSearchCache.remove(processId);
                    // send response
                    result.put("modified_file_ids", graphIDsOfModifiedFiles);
                    result.put("nb_files", nbFiles);
                    response.type("application/json; charset=UTF-8");
                    response.status(200);
                    return result;
                }
                default: {
                    response.type("text/plain; charset=utf-8");
                    response.status(400);
                    String reason = "Unknown search action parameter!";
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
}
