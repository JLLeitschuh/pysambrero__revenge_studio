package com.ninjaflip.androidrevenge.core.service.handler.general;

import com.ninjaflip.androidrevenge.core.Configurator;
import net.minidev.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.File;
import java.net.URLDecoder;

/**
 * Created by Solitario on 14/06/2017.
 *
 */
public class DeleteTmpFileHandler implements Route {
    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("application/json");

        String savedTmpFilePath = URLDecoder.decode(request.queryParams("savedTmpFilePath"), "UTF-8");
        System.out.println("delete file : " + savedTmpFilePath);

        // safety check in order to avoid deleting user's files
        if(!savedTmpFilePath.startsWith(Configurator.getInstance().getTMP())){
            response.status(403);
            JSONObject res = new JSONObject();
            res.put("message", "forbidden access");
            return res.toJSONString();
        }

        File savedTmpFile = new File(savedTmpFilePath);
        if (savedTmpFile.exists()) {
            if(!savedTmpFile.delete()){
                savedTmpFile.deleteOnExit();
            }
        }
        response.status(200);
        JSONObject res = new JSONObject();
        res.put("message", "success remove tmp file");
        return res.toJSONString();
    }
}

