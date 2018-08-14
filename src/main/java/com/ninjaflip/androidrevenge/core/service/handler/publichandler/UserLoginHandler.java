package com.ninjaflip.androidrevenge.core.service.handler.publichandler;

import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.core.security.LicenseManager;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.beans.UserBean;
import com.ninjaflip.androidrevenge.core.db.dao.UserDao;
import com.ninjaflip.androidrevenge.utils.Utils;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.net.URLDecoder;

/**
 * Created by Solitario on 23/07/2017.
 *
 */
public class UserLoginHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(UserLoginHandler.class);

    @Override
    public Object handle(Request request, Response response) throws Exception {


        try {
            if(!ServerSecurityManager.getInstance().isClockOk()){
                Utils.getNtpTime(2000); // refresh date
                response.type("text/plain; charset=utf-8");
                response.status(449);
                String message = "Incorrect system date. Please check your system date and time settings and try again!";
                response.body(message);
                return message;
            }


            /*
            if (PreferencesManager.getInstance().mustCheckUserLicense()) {
                // check if license key is ok
                if (!LicenseManager.getInstance().checkSoftwareLicenseIsOk()) {
                    response.type("text/plain; charset=utf-8");
                    response.status(403);
                    String message = "The product needs to be activated with a license key!";
                    response.body(message);
                    return message;
                }
            }*/

            // check if had accepted license
            if(!PreferencesManager.getInstance().hasAcceptedTermsAndConditions()){
                response.type("text/plain; charset=utf-8");
                response.status(451);
                String message = "Terms and conditions not accepted yet!";
                response.body(message);
                return message;
            }

            String userName = URLDecoder.decode(request.queryParams("username"), "UTF-8");
            String password = URLDecoder.decode(request.queryParams("password"), "UTF-8");

            if (UserDao.getInstance().authenticateUser(userName, password)) {
                UserBean user = UserDao.getInstance().getByUserId(userName);
                String token = ServerSecurityManager.getInstance().generateNewServerToken(user.getUuid());
                response.cookie("/", "token", token, 86400, false, false);
                response.cookie("/", "userId", user.getUuid(), 86400, false, false);
                response.status(204);
                return "";
            } else {
                LOGGER.info("Login failed, bad credentials");
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String message = "Bad username or password";
                response.body(message);
                return message;
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
