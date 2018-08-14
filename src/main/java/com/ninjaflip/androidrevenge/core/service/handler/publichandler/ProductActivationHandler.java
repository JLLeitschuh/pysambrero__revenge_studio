package com.ninjaflip.androidrevenge.core.service.handler.publichandler;

import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.core.security.LicenseManager;
import com.ninjaflip.androidrevenge.utils.Utils;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Created by Solitario on 04/11/2017.
 *
 * Handler product activation using a license key
 */
public class ProductActivationHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(ProductActivationHandler.class);

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
                case "GET_LICENSE_HTML": {
                    String stringResponse = Utils.loadTemplateAsString("general",
                            "mst_tmpl_modal_content_license");
                    response.status(200);
                    return stringResponse;
                }
                case "SUBMIT_LICENSE": {
                    String licenseKey = request.queryParams("licenseKey");
                    if (licenseKey == null || licenseKey.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a licenseKey parameter!";
                        response.body(reason);
                        return reason;
                    }

                    if(LicenseManager.getInstance().updateSoftwareLicense(licenseKey)) {
                        response.status(204);
                        return "";
                    }else {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Invalid license key";
                        response.body(reason);
                        return reason;
                    }
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
            response.status(400);
            String message = e.getMessage();
            response.body(message);
            return message;
        }
    }
}
