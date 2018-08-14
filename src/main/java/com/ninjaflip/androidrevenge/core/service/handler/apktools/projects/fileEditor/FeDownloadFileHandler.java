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
 * Created by Solitario on 28/08/2017.
 * <p>
 * Downloading file from project editor by selecting 'download' from jstree context menu
 */
public class FeDownloadFileHandler implements Route {

    private final static Logger LOGGER = Logger.getLogger(FeDownloadFileHandler.class);

    @Override
    public Object handle(Request request, Response response) throws Exception {


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

        Graph graph = GraphCache.getInstance().getGraph(projectUuid);
        if (graph == null) {
            String projectFolderNameUuid = ApkToolProjectDao.getInstance().getByUuid(projectUuid).getProjectFolderNameUuid();
            String graphPath = Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, false);
            graph = GraphManager.getInstance().loadGraphFromJson(graphPath);
            GraphCache.getInstance().cacheGraph(projectUuid, graph);
        }

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
            String reason = "The file you want to download does not exist in project graph!";
            response.body(reason);
            return reason;
        }
        File fileToDownload = new File(vertex.value("path").toString());
        LOGGER.debug("Download file having node[" + nodeId + "] path : " + vertex.value("path").toString());

        if (!fileToDownload.exists()) {
            response.type("text/plain; charset=utf-8");
            response.status(400);
            String reason = "The element that you are trying to download does not exists in filesystem!";
            response.body(reason);
            return reason;
        } else {
            InputStream inputStream = new FileInputStream(fileToDownload);
            ServletOutputStream out = response.raw().getOutputStream();

            try {
                response.raw().setContentType(vertex.value("mimeType").toString());
                response.raw().setHeader("Content-Disposition", "attachment; filename=" + fileToDownload.getName());
                response.raw().setHeader("Content-Length", String.valueOf(fileToDownload.length()));
                response.status(200);
                stream(inputStream, out);
            } finally {
                inputStream.close();
                out.close();
            }
            return null;
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
