package com.ninjaflip.androidrevenge.core.service.handler.general;

import com.ninjaflip.androidrevenge.beans.UserBean;
import com.ninjaflip.androidrevenge.core.db.dao.UserDao;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.utils.Utils;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Solitario on 23/12/2017.
 * <p>
 * Handler for updating profile
 */
public class UserProfileHandler implements Route {
    private final static Logger LOGGER = Logger.getLogger(UserProfileHandler.class);

    @Override
    public Object handle(Request request, Response response) throws Exception {

        try {
            String userUuid = ServerSecurityManager.getInstance()
                    .getUserUuidFromToken(request.cookie("token"));

            String action = request.queryParams("action");
            if (action == null || action.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "You must provide an action parameter!";
                response.body(reason);
                return reason;
            }

            switch (action) {
                case "GET_MODAL_HTML_USERNAME": {
                    UserBean currentUser = UserDao.getInstance().getByUuid(userUuid);
                    response.type("text/html; charset=utf-8");
                    Map<String, String> templateVarValues = new HashMap<>();
                    templateVarValues.put("old_username", currentUser.getUserId());
                    String stringResponse = Utils.loadTemplateAsString("index",
                            "mst_tmpl_modal_content_update_username", templateVarValues);
                    response.status(200);
                    return stringResponse;
                }
                case "GET_MODAL_HTML_PASSWORD": {
                    response.type("text/html; charset=utf-8");
                    String stringResponse = Utils.loadTemplateAsString("index",
                            "mst_tmpl_modal_content_update_password");
                    response.status(200);
                    return stringResponse;
                }
                case "SUBMIT_UPDATE_USERNAME": {
                    String new_username = request.queryParams("new_username");
                    if (new_username == null || new_username.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Please enter your new username!";
                        response.body(reason);
                        return reason;
                    }

                    if (new_username.length() < 5 || new_username.length() > 20) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Username must contain between 5 and 20 characters!";
                        response.body(reason);
                        return reason;
                    }

                    String regex = "^\\w+$";
                    Pattern p = Pattern.compile(regex);
                    Matcher m = p.matcher(new_username);
                    if (!m.matches()) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Username can only contain English letters [a-z,A-Z], numbers [0-9] and underscores _";
                        response.body(reason);
                        return reason;
                    }

                    String current_pwd = request.queryParams("current_pwd");
                    if (current_pwd == null || current_pwd.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Please enter your current password!";
                        response.body(reason);
                        return reason;
                    }

                    UserBean currentUser = UserDao.getInstance().getByUuid(userUuid);
                    if(!currentUser.getPassword().equals(current_pwd)){
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "The password you entered is incorrect!";
                        response.body(reason);
                        return reason;
                    }

                    // update user profile
                    currentUser.setUId(new_username);
                    UserDao.getInstance().update(currentUser);
                    response.status(204);
                    return "";
                }
                case "SUBMIT_UPDATE_PASSWORD": {
                    String current_pwd = request.queryParams("current_pwd");
                    if (current_pwd == null || current_pwd.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Please enter your current password!";
                        response.body(reason);
                        return reason;
                    }

                    String profile_new_pwd = request.queryParams("profile_new_pwd");
                    if (profile_new_pwd == null || profile_new_pwd.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Please enter your new password!";
                        response.body(reason);
                        return reason;
                    }

                    if (profile_new_pwd.length() < 5 || profile_new_pwd.length() > 20) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "New password must contain between 5 and 20 characters!";
                        response.body(reason);
                        return reason;
                    }

                    String regex = "^\\w+$";
                    Pattern p = Pattern.compile(regex);
                    Matcher m = p.matcher(profile_new_pwd);
                    if (!m.matches()) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "New password can only contain English letters [a-z,A-Z], numbers [0-9] and underscores _";
                        response.body(reason);
                        return reason;
                    }

                    String profile_new_pwd_confirm = request.queryParams("profile_new_pwd_confirm");
                    if (profile_new_pwd_confirm == null || profile_new_pwd_confirm.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Please confirm your new password!";
                        response.body(reason);
                        return reason;
                    }

                    if (!profile_new_pwd.equals(profile_new_pwd_confirm)) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "New password and the confirm password does not match!";
                        response.body(reason);
                        return reason;
                    }

                    String profile_reminder_ph = request.queryParams("profile_reminder_ph");
                    if (profile_reminder_ph == null || profile_reminder_ph.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Please enter a password reminder phrase!";
                        response.body(reason);
                        return reason;
                    }

                    if (profile_reminder_ph.length() < 10 || profile_reminder_ph.length() > 80) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "Password reminder phrase must contain between 10 and 80 characters!";
                        response.body(reason);
                        return reason;
                    }

                    UserBean currentUser = UserDao.getInstance().getByUuid(userUuid);
                    if(!currentUser.getPassword().equals(current_pwd)){
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "The password you entered is incorrect!";
                        response.body(reason);
                        return reason;
                    }

                    // update user profile
                    currentUser.setUPwd(profile_new_pwd);
                    currentUser.setReminderPhrase(profile_reminder_ph);
                    UserDao.getInstance().update(currentUser);
                    response.status(204);
                    return "";
                }
                default: {
                    response.type("text/plain; charset=utf-8");
                    response.status(400);
                    String reason = "Unknown profile action parameter!";
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
