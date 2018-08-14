package com.ninjaflip.androidrevenge.core.service.handler;

import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Base64;

/**
 * Created by Solitario on 11/06/2017.
 *
 * Handler responsible for initializing the connection between the browser and the websocket server
 */
public class InitConnectionHandler implements Route {

    private final static Logger LOGGER = Logger.getLogger(InitConnectionHandler.class);

    @Override
    public Object handle(Request request, Response response) {
        /*System.out.println(request.queryParams()); // What type of data am I sending?
        System.out.println(request.contentType()); // What type of data am I sending?
        System.out.println(request.params()); // What are the params sent?
        System.out.println(request.raw()); // What's the raw data sent?*/

        response.type("application/json; charset=utf-8");

        String tokenB64 = request.queryParams("token");
        String userIdB64 = request.queryParams("userId");
        if (tokenB64 != null && userIdB64 != null) {
            String token;
            String userId;
            try {
                token = URLDecoder.decode(new String(Base64.getDecoder().decode(tokenB64)),"UTF-8");
                userId = URLDecoder.decode(new String(Base64.getDecoder().decode(userIdB64)),"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                LOGGER.error(e.getMessage());
                response.status(400);
                JSONObject res = new JSONObject();
                res.put("message", "Bad request: bad credentials encoding!");
                return res.toJSONString();
            }
            //System.out.println("InitConnectionHandler ==> token " + token);
            //System.out.println("InitConnectionHandler ==> userId " + userId);

            // validate token and userId ==> if not valid close session using status code 'Rejected'
            boolean isValidToken;
            try {
                isValidToken = ServerSecurityManager.getInstance().verifyToken(token, userId);
            } catch (Exception e) {
                //System.out.println("500");
                e.printStackTrace();
                LOGGER.error(e.getMessage());
                response.status(500);
                JSONObject res = new JSONObject();
                res.put("message", e.getMessage());
                return res.toJSONString();
            }

            if (!isValidToken) {
                //System.out.println("401");
                response.status(401);
                JSONObject res = new JSONObject();
                res.put("message", "Unauthorized: invalid credentials!");
                return res.toJSONString();
            } else{
                //System.out.println("200");
                response.status(200);
                JSONObject res = new JSONObject();
                res.put("message", "katana desktop connection success!");
                return res.toJSONString();
            }
        } else {
            //System.out.println("400 2");
            response.status(400);
            JSONObject res = new JSONObject();
            res.put("message", "Bad request: null credentials!");
            return res.toJSONString();
        }
    }
}