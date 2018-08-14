package com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.fileEditor;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.beans.ApkToolProjectBean;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import org.apache.log4j.Logger;
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
 * Downloading apk files from project editor by selecting 'download' from bottom tab 'apk files'
 */
public class FeDownloadApkHandler implements Route {

    private final static Logger LOGGER = Logger.getLogger(FeDownloadApkHandler.class);

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

        // check if apkName is valid
        String apkName = request.queryParams("apkName");
        if (apkName == null || apkName.equals("")) {
            response.type("text/plain; charset=utf-8");
            response.status(400);
            String reason = "You must provide project uuid!";
            response.body(reason);
            return reason;
        }

        // check if apkType is valid
        String apkType = request.queryParams("apkType");
        if (apkType == null || apkType.equals("")) {
            response.type("text/plain; charset=utf-8");
            response.status(400);
            String reason = "apkType parameter not found!";
            response.body(reason);
            return reason;
        }

        ApkToolProjectBean project;
        // check if that project exists and belongs to current user
        if (ApkToolProjectDao.getInstance().projectExistsAndBelongsToUser(projectUuid, userUuid)) {
            project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
        } else {
            response.type("text/plain; charset=utf-8");
            response.status(400);
            String reason = "This project does not exists or does not belongs to you!";
            response.body(reason);
            return reason;
        }


        String apkFolderPath;
        switch (apkType){
            case "orig":{
                apkFolderPath = Configurator.getInstance().getSrcApkFolder(userUuid, project.getProjectFolderNameUuid(), false);
                break;
            }
            case "gen":{
                apkFolderPath = Configurator.getInstance().getGenFolderPath(userUuid, project.getProjectFolderNameUuid(), false);
                break;
            }
            default:{
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "Unknown apkType parameter '"+apkType+"'";
                response.body(reason);
                return reason;
            }
        }

        File fileToDownload = new File(apkFolderPath, apkName);
        LOGGER.debug("Download file: " + fileToDownload.getPath()+ "");

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
                response.raw().setContentType("application/vnd.android.package-archive");
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
