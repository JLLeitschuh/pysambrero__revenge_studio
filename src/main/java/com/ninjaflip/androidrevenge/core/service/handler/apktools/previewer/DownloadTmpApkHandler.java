package com.ninjaflip.androidrevenge.core.service.handler.apktools.previewer;

import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.servlet.ServletOutputStream;
import java.io.*;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Created by Solitario on 28/08/2017.
 * <p>
 * Downloading apk files from project editor by selecting 'download' from bottom tab 'apk files'
 */
public class DownloadTmpApkHandler implements Route {

    private final static Logger LOGGER = Logger.getLogger(DownloadTmpApkHandler.class);

    @Override
    public Object handle(Request request, Response response) throws Exception {


        // check if apkName is valid
        String tmpApkPath = URLDecoder.decode(request.queryParams("tmpApkPath"), "UTF-8");
        if (tmpApkPath == null || tmpApkPath.equals("")) {
            response.type("text/plain; charset=utf-8");
            response.status(400);
            String reason = "You must provide a file path!";
            response.body(reason);
            return reason;
        }
        System.out.println("downloading tmp file : " + tmpApkPath);

        File fileToDownload = new File(tmpApkPath);
        LOGGER.debug("Download file: " + fileToDownload.getPath()+ "");

        if (!fileToDownload.exists()) {
            response.type("text/plain; charset=utf-8");
            response.status(404);
            String reason = "The apk file that you are trying to download does not exists in filesystem!";
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
