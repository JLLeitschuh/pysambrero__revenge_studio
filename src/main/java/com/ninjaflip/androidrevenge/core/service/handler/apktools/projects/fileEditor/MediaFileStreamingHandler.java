package com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.fileEditor;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import com.ninjaflip.androidrevenge.core.service.cache.GraphCache;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.servlet.ServletOutputStream;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Created by Solitario on 19/08/2017.
 * <p>
 * Media files streamer such as videos, audios and images
 */
public class MediaFileStreamingHandler implements Route {

    private final static Logger LOGGER = Logger.getLogger(MediaFileStreamingHandler.class);

    @Override
    public Object handle(Request request, Response response) {
        try {
            String nodeId = request.queryParams("nodeId");
            String projectUuid = request.queryParams("projectUuid");
            String userUuid = ServerSecurityManager.getInstance()
                    .getUserUuidFromToken(request.cookie("token"));


            if (nodeId == null || nodeId.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "file id parameter not found!";
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

            Graph graph = GraphCache.getInstance().getGraph(projectUuid);
            if(graph == null) {
                String projectFolderNameUuid = ApkToolProjectDao.getInstance().getByUuid(projectUuid).getProjectFolderNameUuid();
                String graphPath = Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, false);
                graph = GraphManager.getInstance().loadGraphFromJson(graphPath);
                GraphCache.getInstance().cacheGraph(projectUuid, graph);
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

            LOGGER.info("File type = " + vertex.value("mimeType").toString());
            response.type(vertex.value("mimeType").toString());


            InputStream inputStream = new FileInputStream(file);
            ServletOutputStream out = response.raw().getOutputStream();

            try {
                response.header("Content-Length", String.valueOf(file.length()));
                response.status(200);
                stream(inputStream, out);
            } finally {
                inputStream.close();
                out.close();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("error: " + e.getMessage());
            response.type("text/plain; charset=utf-8");
            response.status(500);
            response.body(e.getMessage());
            return e.getMessage();
        }
    }

    // writes input stream in blocks of 10KB. This way we end up with a consistent memory usage of only 10KB instead
    // of the complete content length.
    // Also the end-user will start getting parts of the content much sooner.
    public static long stream(InputStream input, OutputStream output) throws IOException {
        try (
                ReadableByteChannel inputChannel = Channels.newChannel(input);
                WritableByteChannel outputChannel = Channels.newChannel(output);
        ) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(10240);
            long size = 0;

            while (inputChannel.read(buffer) != -1) {
                buffer.flip();
                size += outputChannel.write(buffer);
                buffer.clear();
            }
            return size;
        }
    }
}
