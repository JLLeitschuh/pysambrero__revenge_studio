package com.ninjaflip.androidrevenge.core.service.handler.general;

import com.ninjaflip.androidrevenge.core.apktool.UserProcessBuilder;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import net.minidev.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Created by Solitario on 19/06/2017.
 *
 */
public class CancelUserProcessHandler implements Route {
    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("application/json");

        String userUuid = ServerSecurityManager.getInstance()
                .getUserUuidFromToken(request.cookie("token"));

        String processId = request.queryParams("processId");
        if (processId != null && !processId.equals("")) {
            try {
                UserProcessBuilder.cancelRunningProcess(processId, userUuid);
                response.status(200);
                JSONObject res = new JSONObject();
                res.put("message", "Process "+processId+" canceled successfully!");
                return res.toJSONString();
            } catch (IllegalAccessException e) {
                response.status(401);
                JSONObject res = new JSONObject();
                res.put("message", e.getMessage());
                return res.toJSONString();
            }
        }else{
            response.status(400);
            JSONObject res = new JSONObject();
            res.put("message", "Bad request: null process id!");
            return res.toJSONString();
        }
    }
}
