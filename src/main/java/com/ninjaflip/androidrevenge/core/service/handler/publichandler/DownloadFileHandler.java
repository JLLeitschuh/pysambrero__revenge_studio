package com.ninjaflip.androidrevenge.core.service.handler.publichandler;

import com.ninjaflip.androidrevenge.core.Configurator;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Solitario on 31/07/2017.
 *
 * File download handler, files are located in the wor directory.
 *
 */
public class DownloadFileHandler implements Route {

    private final static Logger LOGGER = Logger.getLogger(DownloadFileHandler.class);
    @Override
    public Object handle(Request request, Response response) throws Exception {
        String relative_path = URLDecoder.decode(request.queryParams("relative_path"), "UTF-8");
        File file = new File(Configurator.getInstance().getWork_DIR() + File.separator + relative_path);
        response.raw().setContentType("application/octet-stream");
        response.raw().setHeader("Content-Disposition", "attachment; filename=" + file.getName());
        try {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(response.raw().getOutputStream()));
                 BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file))) {
                ZipEntry zipEntry = new ZipEntry(file.getName());

                zipOutputStream.putNextEntry(zipEntry);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = bufferedInputStream.read(buffer)) > 0) {
                    zipOutputStream.write(buffer, 0, len);
                }
            }
        } catch (Exception e) {
            Spark.halt(405, "server error");
        }
        return null;
    }
}
