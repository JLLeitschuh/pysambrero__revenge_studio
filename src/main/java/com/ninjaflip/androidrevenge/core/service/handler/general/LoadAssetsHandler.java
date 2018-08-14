package com.ninjaflip.androidrevenge.core.service.handler.general;

import com.ninjaflip.androidrevenge.utils.Utils;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.net.URLDecoder;

/**
 * Created by Solitario on 28/07/2017.
 * <p>
 * Load templates and its javascript
 */
public class LoadAssetsHandler implements Route {

    private final static Logger LOGGER = Logger.getLogger(LoadAssetsHandler.class);

    @Override
    public Object handle(Request request, Response response) {
        response.type("text/html; charset=utf-8");

        String action = request.queryParams("action");
        String stringResponse;

        switch (action) {
            case "GET_JS":
            /*
             * Get javascript file
             */
                try {
                    String javascriptFileRelativePath = URLDecoder.decode(request.queryParams("js_file"), "UTF-8");
                    stringResponse = Utils.readFromAsset(javascriptFileRelativePath);
                    response.status(200);
                    return stringResponse;
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.error(e.getMessage());
                    response.type("text/plain; charset=utf-8");
                    response.status(500);
                    response.body("Internal server error");
                    return e.getMessage();
                }
            case "GET_TEMPLATE":
            /*
             * Get template file
             */
                try {
                    String templateFolder = URLDecoder.decode(request.queryParams("template_folder"), "UTF-8");
                    String templateName = URLDecoder.decode(request.queryParams("template_name"), "UTF-8");
                    stringResponse = Utils.loadTemplateAsString(templateFolder, templateName);
                    response.status(200);
                    return stringResponse;
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.error(e.getMessage());
                    response.status(500);
                    response.body("Internal server error");
                    return e.getMessage();
                }
            default:
                LOGGER.debug("Invalid loadAsset action");
                response.type("text/plain; charset=utf-8");
                String msg= "Action: '" + action + "' is not a valid parameter";
                response.status(400);
                response.body(msg);
                return msg;
        }
    }
}