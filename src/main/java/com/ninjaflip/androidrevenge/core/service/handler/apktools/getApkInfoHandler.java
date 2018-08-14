package com.ninjaflip.androidrevenge.core.service.handler.apktools;

import com.ninjaflip.androidrevenge.core.apktool.ApkToolsManager;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.net.URLDecoder;
import java.util.Map;

/**
 * Created by Solitario on 03/08/2017.
 *
 * Get Apk info handler
 */
public class getApkInfoHandler implements Route {

    private final static Logger LOGGER = Logger.getLogger(getApkInfoHandler.class);

    @Override
    public Object handle(Request request, Response response) {
        response.type("application/json; charset=UTF-8");

        try {
            String filePath = URLDecoder.decode(request.queryParams("filePath"), "UTF-8");
            // get apk info
            Map<String, Object> info = ApkToolsManager.getInstance().getAppInfoFromApk(filePath);
            //LOGGER.debug("APK Info = " + ApkToolsManager.getInstance().getSummaryFromAppInfo(info, false));
            // send response
            response.status(200);
            JSONObject res = new JSONObject(info);
            return res.toJSONString();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("error: " + e.getMessage());
            response.type("text/plain; charset=utf-8");
            response.status(500);
            response.body(e.getMessage());
            return e.getMessage();
        }
    }
}
