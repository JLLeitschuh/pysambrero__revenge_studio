package com.ninjaflip.androidrevenge.core.service.handler.apktools.keytool;

import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.beans.KeystoreBean;
import com.ninjaflip.androidrevenge.core.db.dao.KeystoreDao;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.servlet.ServletOutputStream;
import java.io.*;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Created by Solitario on 28/08/2017.
 * <p>
 * Downloading keystores *.jks files
 */
public class FeDownloadKeystoreHandler implements Route {

    private final static Logger LOGGER = Logger.getLogger(FeDownloadKeystoreHandler.class);

    @Override
    public Object handle(Request request, Response response) throws Exception {



        String userUuid = ServerSecurityManager.getInstance()
                .getUserUuidFromToken(request.cookie("token"));

        String keystoreUuid = request.queryParams("keystoreUuid");
        if (keystoreUuid == null || keystoreUuid.equals("")) {
            response.type("text/plain; charset=utf-8");
            response.status(400);
            String reason = "You must provide a keystore uuid parameter!";
            response.body(reason);
            return reason;
        }

        // check ks record exists in the database
        KeystoreBean ksBean = KeystoreDao.getInstance().getByUuid(keystoreUuid);
        if(ksBean == null){
            response.type("text/plain; charset=utf-8");
            response.status(400);
            String reason = "This keystore does not exists in the database!";
            response.body(reason);
            return reason;
        }

        // check ks belongs to user uuid
        if(!KeystoreDao.getInstance().keystoreExistsAndBelongsToUser(keystoreUuid, userUuid)){
            response.type("text/plain; charset=utf-8");
            response.status(400);
            String reason = "This keystore does not belong to you!!";
            response.body(reason);
            return reason;
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(ksBean.getBlob());
        ServletOutputStream out = response.raw().getOutputStream();
        try {
            response.raw().setContentType("application/octet-stream");
            response.raw().setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(ksBean.getAlias(),"UTF-8") + ".jks");
            response.raw().setHeader("Content-Length", String.valueOf(ksBean.getBlob().length));
            response.status(200);
            stream(inputStream, out);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(out);
        }
        return null;
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
