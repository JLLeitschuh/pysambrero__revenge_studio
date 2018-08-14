package com.ninjaflip.androidrevenge.core.service.handler.general;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import net.minidev.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * Created by Solitario on 13/06/2017.
 */
public class UploadTmpFileHandler implements Route {
    @Override
    public Object handle(Request request, Response response) throws Exception {

        String userUuid = ServerSecurityManager.getInstance()
                .getUserUuidFromToken(request.cookie("token"));

        if (userUuid == null) {
            response.type("text/plain; charset=utf-8");
            response.status(400);
            String reason  = "Bad request: null uuid!";
            response.body(reason);
            return reason;
        }

        request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

        InputStream is = request.raw().getPart("uploaded_file").getInputStream();
        // save file to tmp katana file
        byte[] buffer = new byte[is.available()];
        is.read(buffer);

        String uploadedFileName = getFileName(request.raw().getPart("uploaded_file"));
        String savedTmpFilePath = Configurator.getInstance().getUserTmpFolderPath(userUuid)
                + File.separator + UUID.randomUUID().toString() + File.separator + uploadedFileName;
        File savedTmpFile = new File(savedTmpFilePath);

        if (savedTmpFile.exists())
            savedTmpFile.delete();
        else {
            savedTmpFile.getParentFile().mkdirs();
            savedTmpFile.createNewFile();
        }

        OutputStream outStream = new FileOutputStream(savedTmpFile);
        outStream.write(buffer);
        outStream.close();
        is.close();

        response.type("text/html; charset=utf-8");
        response.status(200);
        return URLEncoder.encode(savedTmpFilePath, "UTF-8").replace("+", "%20");
    }

    private String getFileName(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }
}
