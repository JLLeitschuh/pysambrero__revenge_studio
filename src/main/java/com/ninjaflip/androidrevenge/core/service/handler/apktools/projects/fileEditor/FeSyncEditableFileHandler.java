package com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.fileEditor;

import com.google.gson.Gson;
import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import com.ninjaflip.androidrevenge.core.service.cache.GraphCache;
import com.ninjaflip.androidrevenge.utils.GZip;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Solitario on 20/08/2017.
 * <p>
 * Handler that wraps synchronisation of project files, called when user open a file
 * in the project editor and make some changes on it
 */
public class FeSyncEditableFileHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(FeSyncEditableFileHandler.class);
    private static Gson gson = new Gson();
    private static Map<String , Map<Integer,ChunkedFile>> chunksCache = new HashMap<>();

    @Override
    public Object handle(Request request, Response response) {

        try {
            String nodeId = request.queryParams("nodeId");
            String projectUuid = request.queryParams("projectUuid");
            String content = request.queryParams("content");
            String is_compressed = request.queryParams("is_compressed");
            String is_chunked = request.queryParams("is_chunked");

            String userUuid = ServerSecurityManager.getInstance()
                    .getUserUuidFromToken(request.cookie("token"));


            if (nodeId == null || nodeId.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "node id parameter not found!";
                response.body(reason);
                return reason;
            }

            if (projectUuid == null || projectUuid.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "project id parameter not found!";
                response.body(reason);
                return reason;
            }

            if (is_compressed == null || is_compressed.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "compression parameter not found!";
                response.body(reason);
                return reason;
            }

            if (is_chunked == null || is_chunked.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "chunk parameter not found!";
                response.body(reason);
                return reason;
            }

            if (content == null) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "content parameter not found!";
                response.body(reason);
                return reason;
            }


            // check if that project exists and belongs to current user
            if (!ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "This project does not exists or does not belongs to you!";
                response.body(reason);
                return reason;
            }

            // **********Start Logic*********************
            Graph graph = GraphCache.getInstance().getGraph(projectUuid);
            if(graph == null) {
                String projectFolderNameUuid = ApkToolProjectDao.getInstance().getByUuid(projectUuid).getProjectFolderNameUuid();
                String graphPath = Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, false);
                graph = GraphManager.getInstance().loadGraphFromJson(graphPath);
                GraphCache.getInstance().cacheGraph(projectUuid, graph);
            }

            boolean isChunked = Boolean.parseBoolean(is_chunked);

            if (isChunked) { // received a chunk => checks if received all chunks , it done update file else save chunks and send success
                return processChunkedContent(content, nodeId, graph, response);
            } else { // file is not chunked (regular) => just save it
                return processRegularContent(content, nodeId, graph, is_compressed, response);
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



    // this method wraps the logic for a chunked content
    private Object processChunkedContent(String content, String nodeId, Graph graph, Response response) throws ScriptException, IOException {
        // received a chunk => checks if received all chunks , it done update file else save chunks and send success
        ChunkedFile chunk = gson.fromJson(content, ChunkedFile.class);
        String chunkId = chunk.getUuid();

        chunksCache.computeIfAbsent(chunkId, k -> new HashMap<>());
        Map<Integer,ChunkedFile> currentFileChunks = chunksCache.get(chunkId);
        currentFileChunks.put(chunk.getPos(), chunk);

        //LOGGER.debug("currentFileChunks.size() = "+currentFileChunks.size()+"/"+chunk.getTotal() +" for chunkId : "+chunkId);
        if(currentFileChunks.size() == chunk.getTotal()){ // all chunks are received
            // build the file and save it
            StringBuilder reconstitution = new StringBuilder();
            for(int i= 0; i< currentFileChunks.size(); i++){
                ChunkedFile chunkTmp = currentFileChunks.get(i);
                reconstitution.append(chunkTmp.getData());
            }

            Vertex vertex = GraphManager.getInstance().getVertexById(graph, nodeId);
            if (vertex == null) {
                response.type("text/plain; charset=utf-8");
                response.status(404);
                String reason = "This file was not found in project graph!";
                response.body(reason);
                return reason;
            }

            String filePath = vertex.value("path").toString();
            File file = new File(filePath);
            if (!file.exists()) {
                response.type("text/plain; charset=utf-8");
                response.status(404);
                String reason = "This file was not found in the filesystem!";
                response.body(reason);
                return reason;
            }

            // decode base64 content
            byte[] base64decodedBytes = Base64.getDecoder().decode(reconstitution.toString());
            // restore break lines, decompress and write content to file
            FileUtils.writeStringToFile(file,
                    GZip.gzDecompress(base64decodedBytes)
                            .replaceAll("%%br%%", "\n"),  "UTF-8");
            // free memory by releasing the cached chunks once finished
            chunksCache.remove(chunkId);
            LOGGER.debug("chunked content saved with success for : " + file.getName());
            // send response
            response.type("text/plain; charset=utf-8");
            response.status(200);
            response.body("success");
            return "";
        }else{ // waiting for other chunks
            // send response, 204 success without body
            response.status(204);
            return "";
        }
    }

    // this method wraps the logic for a regular content (not chunked)
    private Object processRegularContent(String content, String nodeId, Graph graph, String is_compressed, Response response) throws ScriptException, IOException {
        Vertex vertex = GraphManager.getInstance().getVertexById(graph, nodeId);
        if (vertex == null) {
            response.type("text/plain; charset=utf-8");
            response.status(404);
            String reason = "This file was not found in project graph!";
            response.body(reason);
            return reason;
        }

        String filePath = vertex.value("path").toString();
        File file = new File(filePath);
        if (!file.exists()) {
            response.type("text/plain; charset=utf-8");
            response.status(404);
            String reason = "This file was not found in the filesystem!";
            response.body(reason);
            return reason;
        }

        boolean isCompressed = Boolean.parseBoolean(is_compressed);
        // decode base64 content
        byte[] base64decodedBytes = Base64.getDecoder().decode(content);
        if (isCompressed) {
            // restore break lines, decompress and write content to file
            FileUtils.writeStringToFile(file,
                    GZip.gzDecompress(base64decodedBytes)
                            .replaceAll("%%br%%", "\n"), "UTF-8");
        } else {
            // restore break lines and write content to file
            FileUtils.writeStringToFile(file,
                    new String(base64decodedBytes, "UTF-8")
                            .replaceAll("%%br%%", "\n"), "UTF-8");
        }
        LOGGER.debug("regular content saved with success for : " + file.getName());
        // send response
        response.type("text/plain; charset=utf-8");
        response.status(200);
        response.body("success");
        return "";
    }


    // inner class that representing the structure of a chunked file
    private class ChunkedFile{
        private String data;
        private String uuid;
        private int pos;
        private int total;

        public ChunkedFile(){
        }

        public ChunkedFile(String data, String uuid, int pos, int total) {
            this.data = data;
            this.uuid = uuid;
            this.pos = pos;
            this.total = total;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public int getPos() {
            return pos;
        }

        public void setPos(int pos) {
            this.pos = pos;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }
}
