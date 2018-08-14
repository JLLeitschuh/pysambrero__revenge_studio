package com.ninjaflip.androidrevenge.core.service.handler.publichandler;

import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.utils.Utils;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Created by Solitario on 28/12/2017.
 *
 * Handler that return on-demand Static HTML to be displayed inside the abstract modal
 */
public class GetHtmlModalContentHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(GetHtmlModalContentHandler.class);

    @Override
    public Object handle(Request request, Response response) throws Exception {

        try {
            String action = request.queryParams("action");
            // check if action is valid
            if (action == null || action.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "action parameter not found!";
                response.body(reason);
                return reason;
            }

            switch (action) {
                case "GET_MODAL_DONATE_XMR": {
                    String stringResponse = Utils.loadTemplateAsString("general",
                            "mst_tmpl_modal_content_donate_xmr");
                    response.type("text/html; charset=utf-8");
                    response.status(200);
                    return stringResponse;
                }
                case "GET_MODAL_TERMS": {
                    String stringResponse = Utils.loadTemplateAsString("general",
                            "mst_tmpl_modal_content_read_terms");
                    response.type("text/html; charset=utf-8");
                    response.status(200);
                    return stringResponse;
                }
                case "GET_MODAL_CREDIT": {
                    String stringResponse = Utils.loadTemplateAsString("general",
                            "mst_tmpl_modal_content_read_credit");
                    response.type("text/html; charset=utf-8");
                    response.status(200);
                    return stringResponse;
                }
                case "GET_MODAL_ABOUT_US":{
                    String stringResponse = Utils.loadTemplateAsString("general",
                            "mst_tmpl_modal_content_about_us");
                    response.type("text/html; charset=utf-8");
                    response.status(200);
                    return stringResponse;
                }
                case "GET_MODAL_ADBLOCK":{
                    String stringResponse = Utils.loadTemplateAsString("general",
                            "mst_tmpl_modal_content_adblock_detected");
                    response.type("text/html; charset=utf-8");
                    response.status(200);
                    return stringResponse;
                }
                default: {
                    response.type("text/plain; charset=utf-8");
                    response.status(400);
                    String reason = "Unknown action parameter '" + action + "'";
                    response.body(reason);
                    return reason;
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            response.type("text/plain; charset=utf-8");
            response.status(500);
            String message = e.getMessage();
            response.body(message);
            return message;
        }
    }
}
