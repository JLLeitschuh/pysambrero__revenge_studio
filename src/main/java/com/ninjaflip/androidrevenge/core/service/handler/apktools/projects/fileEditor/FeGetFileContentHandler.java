package com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.fileEditor;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.core.apktool.graph.MimeTypes;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import com.ninjaflip.androidrevenge.core.service.cache.GraphCache;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import com.ninjaflip.androidrevenge.utils.imageutils.GifDecoder;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Created by Solitario on 11/08/2017.
 * <p>
 * Handler that gets file content as a string and send it back to the file editor for display
 */
public class FeGetFileContentHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(FeGetFileContentHandler.class);
    private static List<String> editableFileTypes = new ArrayList<>();
    static{
        editableFileTypes.add("edit_txt");
        editableFileTypes.add("edit_smali");
        editableFileTypes.add("edit_xml");
        editableFileTypes.add("edit_json");
        editableFileTypes.add("edit_html");
        editableFileTypes.add("edit_js");
        editableFileTypes.add("edit_css");
    }


    @Override
    public Object handle(Request request, Response response) {

        try {
            ExecutionTimer timer = new ExecutionTimer();
            timer.start();

            JSONObject resultJson = new JSONObject();
            String nodeId = request.queryParams("nodeId");
            String projectUuid = request.queryParams("projectUuid");
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

            // check if that project exists and belongs to current user
            if(!ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
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

            resultJson.put("projectUuid", projectUuid);
            resultJson.put("nodeId", nodeId);
            resultJson.put("fileSize", (int) file.length());

            if (editableFileTypes.contains(MimeTypes.getMimeTypeCategory(vertex.value("mimeType").toString()))) { // file is editable => contains text
                byte[] encoded = Files.readAllBytes(Paths.get(filePath));
                String fileContentsAsStr = new String(encoded, StandardCharsets.UTF_8);
                resultJson.put("fileContent", fileContentsAsStr);
            } else if (MimeTypes.getMimeTypeCategory(vertex.value("mimeType").toString()).equals("load_img")) { // file is an image
                FileInputStream fis = null;
                try {
                    if (vertex.value("mimeType").toString().equals(MimeTypes.MIME_IMAGE_GIF)) { // GIF image
                        GifDecoder d = new GifDecoder();
                        fis = new FileInputStream(file);
                        d.read(fis);
                        if (d.getFrameCount() > 0) {
                            BufferedImage frame = d.getFrame(0);  // frame i
                            resultJson.put("imageWidth", frame.getWidth());
                            resultJson.put("imageHeight", frame.getHeight());
                        } else {
                            resultJson.put("imageWidth", "NaN");
                            resultJson.put("imageHeight", "NaN");
                        }
                    } else { // png or jpg...
                        BufferedImage bimg = ImageIO.read(file);
                        resultJson.put("imageWidth", bimg.getWidth());
                        resultJson.put("imageHeight", bimg.getHeight());
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                    resultJson.put("imageWidth", "NaN");
                    resultJson.put("imageHeight", "NaN");
                }finally {
                    if(fis != null){
                        fis.close();
                    }
                }
            }else if (MimeTypes.getMimeTypeCategory(vertex.value("mimeType").toString()).equals("download")) { // file is not an editable
                byte[] bytes = Files.readAllBytes(Paths.get(filePath));
                String fileContentsAsStr = new String(bytes, StandardCharsets.UTF_8);
                resultJson.put("fileContent", fileContentsAsStr);
            }else if (MimeTypes.getMimeTypeCategory(vertex.value("mimeType").toString()).equals("load_pdf")) { // pdf file
                LOGGER.debug("Loading pdf file");
                String fileContentsAsStr = Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(filePath)));
                resultJson.put("fileContent", fileContentsAsStr);
            }

            // send response
            response.type("application/json; charset=UTF-8");
            response.status(200);
            timer.end();
            LOGGER.debug("FeGetFileContentHandler ended in : " + ExecutionTimer.getTimeString(timer.durationInSeconds()));
            return resultJson;
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
