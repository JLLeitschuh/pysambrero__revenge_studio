package com.ninjaflip.androidrevenge.core.service.handler.general;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.*;

/**
 * Created by Solitario on 22/11/2017.
 * <p>
 * A handler for music player, that saves playslist, add remove update playlists...
 */
public class MusicPlayerHandler implements Route {

    private final static Logger LOGGER = Logger.getLogger(MusicPlayerHandler.class);

    @Override
    public Object handle(Request request, Response response) {
        String userUuid = ServerSecurityManager.getInstance()
                .getUserUuidFromToken(request.cookie("token"));
        String action = request.queryParams("action");

        try {

            if (action == null || action.equals("")) {
                response.type("text/plain; charset=utf-8");
                response.status(400);
                String reason = "You must provide an action parameter!";
                response.body(reason);
                return reason;
            }

            switch (action) {
                case "GET_SC_APIKEY":{ // return SoundCloud api key (client id)
                    // return user's api key if exist, else return random from list
                    String apikey = PreferencesManager.getInstance().getScApiey();
                    if (apikey == null) {
                        Map<String, String> config = Utils.readConfigurationFile();
                        String apiKeysStr = config.get("list_sc_apikey");
                        String[] items = apiKeysStr.split(",");
                        List<String> itemList = new ArrayList<>(Arrays.asList(items));
                        int rnd = new Random().nextInt(itemList.size());
                        apikey =  itemList.get(rnd);
                    }

                    response.type("text/plain; charset=utf-8");
                    response.status(200);
                    return apikey;
                }
                case "UPDATE_SC_API_KEY":{
                    String sc_use = request.queryParams("sc_use");
                    if (sc_use == null || sc_use.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide an api type parameter (DEFAULT or CUSTOM)!";
                        response.body(reason);
                        return reason;
                    }
                    switch(sc_use){
                        case "DEFAULT":{
                            PreferencesManager.getInstance().deleteScApiey();
                            response.status(204);
                            return "";
                        }
                        case "CUSTOM":{
                            String api_key = request.queryParams("api_key");
                            if (api_key == null || api_key.equals("")) {
                                response.type("text/plain; charset=utf-8");
                                response.status(400);
                                String reason = "You must provide an api key parameter!";
                                response.body(reason);
                                return reason;
                            }

                            PreferencesManager.getInstance().saveScApiKey(api_key);
                            response.status(204);
                            return "";
                        }
                        default:{
                            response.type("text/plain; charset=utf-8");
                            response.status(400);
                            String reason = "Unknown SoundCloud api type parameter parameter!";
                            response.body(reason);
                            return reason;
                        }
                    }
                }
                case "GET_SC_API_CONFIG": {
                    JSONObject config = new JSONObject();
                    String apikey = PreferencesManager.getInstance().getScApiey();
                    if (apikey == null) {
                        config.put("config", "DEFAULT");
                    }else{
                        config.put("config", "CUSTOM");
                        config.put("apikey", apikey);
                    }
                    response.type("application/json; charset=UTF-8");
                    response.status(200);
                    return config;
                }
                case "GET_ALL": { // get all play lists
                    String playlistsAsJsonStr = PreferencesManager.getInstance().getPlaylists(userUuid);
                    JSONArray playlists = (JSONArray) JSONValue.parse(playlistsAsJsonStr);
                    JSONObject result = new JSONObject();
                    result.put("playlists", playlists);
                    response.type("application/json; charset=UTF-8");
                    response.status(200);
                    return result;
                }
                case "ADD_PLAYLIST": {
                    String plistName = request.queryParams("plistName");
                    if (plistName == null || plistName.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a playlist name parameter!";
                        response.body(reason);
                        return reason;
                    }
                    String plistUrl = request.queryParams("plistUrl");
                    if (plistUrl == null || plistUrl.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a playlist URL parameter!";
                        response.body(reason);
                        return reason;
                    }
                    JSONObject playlist = new JSONObject();
                    String newPlaylistUuid = UUID.randomUUID().toString();
                    playlist.put("uuid", newPlaylistUuid);
                    playlist.put("name", plistName);
                    playlist.put("url", plistUrl);

                    String playlistsAsJsonStr = PreferencesManager.getInstance().getPlaylists(userUuid);
                    JSONArray playlists = (JSONArray) JSONValue.parse(playlistsAsJsonStr);
                    playlists.add(playlist);

                    // serialize and save
                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                    PreferencesManager.getInstance().savePlaylists(userUuid, gson.toJson(playlists));

                    response.type("text/plain; charset=utf-8");
                    response.status(200);
                    return newPlaylistUuid;
                }
                case "REMOVE_PLAYLIST": {
                    String plistUuid = request.queryParams("plistUuid");
                    if (plistUuid == null || plistUuid.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a playlist uuid parameter!";
                        response.body(reason);
                        return reason;
                    }

                    String playlistsAsJsonStr = PreferencesManager.getInstance().getPlaylists(userUuid);
                    JSONArray playlists = (JSONArray) JSONValue.parse(playlistsAsJsonStr);

                    int index = 0;
                    int indexToRemove = -1;
                    for (Object object : playlists) {
                        JSONObject playlist = (JSONObject) object;
                        if (playlist.getAsString("uuid").equals(plistUuid)) {
                            indexToRemove = index;
                            break;
                        }
                        index++;
                    }

                    if (index != -1) {
                        playlists.remove(indexToRemove);
                    }

                    // serialize and save
                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                    PreferencesManager.getInstance().savePlaylists(userUuid, gson.toJson(playlists));

                    response.status(204);
                    return "";
                }
                case "UPDATE_PLAYLIST": {

                    String plistUuid = request.queryParams("plistUuid");
                    if (plistUuid == null || plistUuid.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a playlist uuid parameter!";
                        response.body(reason);
                        return reason;
                    }
                    String plistName = request.queryParams("plistName");
                    if (plistName == null || plistName.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a playlist name parameter!";
                        response.body(reason);
                        return reason;
                    }
                    String plistUrl = request.queryParams("plistUrl");
                    if (plistUrl == null || plistUrl.equals("")) {
                        response.type("text/plain; charset=utf-8");
                        response.status(400);
                        String reason = "You must provide a playlist URL parameter!";
                        response.body(reason);
                        return reason;
                    }


                    String playlistsAsJsonStr = PreferencesManager.getInstance().getPlaylists(userUuid);
                    JSONArray playlists = (JSONArray) JSONValue.parse(playlistsAsJsonStr);

                    int index = 0;
                    int indexToUpdate = -1;
                    for (Object object : playlists) {
                        JSONObject playlist = (JSONObject) object;
                        if (playlist.getAsString("uuid").equals(plistUuid)) {
                            indexToUpdate = index;
                            break;
                        }
                        index++;
                    }

                    if (index != -1) {
                        JSONObject playlist = (JSONObject) playlists.get(indexToUpdate);
                        playlist.put("name", plistName);
                        playlist.put("url", plistUrl);
                    }

                    // serialize and save
                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                    PreferencesManager.getInstance().savePlaylists(userUuid, gson.toJson(playlists));

                    response.status(204);
                    return "";

                }
                default: {
                    response.type("text/plain; charset=utf-8");
                    response.status(400);
                    String reason = "Unknown Music player action parameter!";
                    response.body(reason);
                    return reason;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.type("text/plain; charset=utf-8");
            LOGGER.error("error: " + e.getMessage());
            response.status(500);
            response.body(e.getMessage());
            return e.getMessage();
        }
    }
}
