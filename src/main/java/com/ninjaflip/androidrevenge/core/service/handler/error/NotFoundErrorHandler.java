package com.ninjaflip.androidrevenge.core.service.handler.error;

import com.ninjaflip.androidrevenge.utils.Utils;
import com.x5.template.Chunk;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by Solitario on 15/12/2017.
 * <p>
 * Handler for 404 error
 */
public class NotFoundErrorHandler implements Route {
    private String errorMessage;

    public NotFoundErrorHandler(String errorMessage){
        this.errorMessage = errorMessage;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        try {

            URL url = NotFoundErrorHandler.class.getResource("/www/static/public/html/error_page.html");

            InputStream stream = url.openStream();
            String htmlContent = new String(Utils.readBytesFromStream(stream), "UTF-8");
            stream.close();

            Chunk html = new Chunk();
            html.append(htmlContent);

            html.setErrorHandling(true, System.err);
            html.set("error", this.errorMessage);
            return html.toString();
        } catch (IOException e) {
            // Add your own exception handlers here.
        }
        return null;
    }
}

