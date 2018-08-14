package com.ninjaflip.androidrevenge.core.service.handler.publichandler;

import com.ninjaflip.androidrevenge.utils.Utils;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.InputStream;
import java.net.URL;

/**
 * Created by Solitario on 25/12/2017.
 *
 * Redirect to firefox download page if user's browser different from firefox
 */
public class BrowserNotSupportedHandler implements Route {

    @Override
    public Object handle(Request request, Response response) throws Exception {
        try{
            // gt html from resource template file
            URL url = getClass().getResource("/www/static/public/html/browser_not_supported.html");
            InputStream stream = url.openStream();
            String htmlContent = new String(Utils.readBytesFromStream(stream), "UTF-8");
            stream.close();

            // return index html page
            response.type("text/html; charset=utf-8");
            response.status(200);
            return htmlContent;
        }catch (Exception e){
            return "";
        }
    }
}