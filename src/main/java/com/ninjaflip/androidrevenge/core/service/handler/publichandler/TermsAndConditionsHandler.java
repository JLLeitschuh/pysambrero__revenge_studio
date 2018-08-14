package com.ninjaflip.androidrevenge.core.service.handler.publichandler;

import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.utils.Utils;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Solitario on 04/11/2017.
 * <p>
 * Handler for terms and conditions acceptance
 */
public class TermsAndConditionsHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(TermsAndConditionsHandler.class);

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
                case "GET_TERMS_HTML": {
                    String stringResponse = Utils.loadTemplateAsString("general",
                            "mst_tmpl_modal_content_terms");
                    response.type("text/html; charset=utf-8");
                    response.status(200);
                    return stringResponse;
                }
                case "ACCEPT_TERMS": {
                    PreferencesManager.getInstance().setHasAcceptedTermsAndConditions(true);
                    LOGGER.info("User has accepted terms and conditions.");
                    response.status(204);
                    return "";
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
