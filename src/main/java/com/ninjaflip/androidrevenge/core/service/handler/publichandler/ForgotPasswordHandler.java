package com.ninjaflip.androidrevenge.core.service.handler.publichandler;

import com.ninjaflip.androidrevenge.beans.UserBean;
import com.ninjaflip.androidrevenge.core.db.dao.UserDao;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;

/**
 * Handler responsible for reminding the user of his current profile (username and password)
 */
public class ForgotPasswordHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(ForgotPasswordHandler.class);

    @Override
    public Object handle(Request request, Response response) throws Exception {
        try {
            List<UserBean> users = UserDao.getInstance().getAll();
            if (users.size() == 1) {
                UserBean currentUser = users.get(0);
                String responseStr = "your username is " + currentUser.getUserId() + " and password is " + currentUser.getMockPwd() + ", reminder: " + currentUser.getReminderPhrase();
                response.type("text/plain; charset=utf-8");
                response.status(200);
                return responseStr;
            } else {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String message;
                if (users.size() == 0) {
                    message = "No user found in the database!";
                } else {
                    message = "Found more than one user in the database!";
                }
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
