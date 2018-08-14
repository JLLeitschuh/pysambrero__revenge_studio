package com.ninjaflip.androidrevenge.core.service.handler.apktools.previewer;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.ApkToolsManager;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.servlet.MultipartConfigElement;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Solitario on 13/06/2017.
 */
public class UploadApkPreviewerHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(UploadApkPreviewerHandler.class);
    @Override
    public Object handle(Request request, Response response) throws Exception {

        try {
            response.type("application/json");


            String userUuid = ServerSecurityManager.getInstance()
                    .getUserUuidFromToken(request.cookie("token"));
            //System.out.println("UploadApkPreviewerHandler => user uuid : " + userUuid);

            if (userUuid == null) {
                response.status(400);
                JSONObject res = new JSONObject();
                res.put("message", "Bad request: null user uuid!");
                return res.toJSONString();
            }

            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

            InputStream is = request.raw().getPart("uploaded_file").getInputStream();
            // save file to tmp folder
            byte[] buffer = new byte[is.available()];
            is.read(buffer);

            String savedTmpApkFilePath = Configurator.getInstance().getUserTmpFolderPath(userUuid)
                    + File.separator + UUID.randomUUID().toString() + ".apk";
            File savedTmpApkFile = new File(savedTmpApkFilePath);

            if (savedTmpApkFile.exists())
                savedTmpApkFile.delete();
            else {
                savedTmpApkFile.getParentFile().mkdirs();
                savedTmpApkFile.createNewFile();
            }

            OutputStream outStream = new FileOutputStream(savedTmpApkFile);
            outStream.write(buffer);
            outStream.close();
            is.close();

            Map<String, Object> info = ApkToolsManager.getInstance().getAppInfoFromApk(savedTmpApkFile.getPath());
            //System.out.println(ApkToolsManager.getInstance().getSummaryFromAppInfo(info, false));
            response.status(200);
            JSONObject res = new JSONObject(info);
            res.put("savedTmpApkFilePath", URLEncoder.encode(savedTmpApkFilePath, "UTF-8").replace("+", "%20"));
            return res.toJSONString();
        }catch (Exception e){
            LOGGER.error("error: " + e.getMessage());
            e.printStackTrace();
            response.type("text/plain; charset=utf-8");
            response.status(500);
            response.body(e.getMessage());
            return e.getMessage();
        }
    }
}
