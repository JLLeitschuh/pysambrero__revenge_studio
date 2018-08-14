package com.ninjaflip.androidrevenge.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * Created by Solitario on 28/07/2017.
 * <p>
 * Save to preferences
 */
public class PreferencesManager {
    private Preferences prefs;
    private static PreferencesManager INSTANCE;
    // preference keys
    private static final String PROXIES_CONFIG = "proxiesConfig";
    private static final String HAS_ACCEPTED_TERMS = "hasAcceptedTerms";
    private static final String LICENSE_KEY = "areslk";
    private static final String FOLDERS_MARKED_FOR_DELETE = "foldersMarkedForDelete-";
    private static final String LAST_NTP_TIME = "lntpt";
    private static final String SOUND_CLOUD_API_KEY = "scapikey";
    private static final String SOUND_CLOUD_USER_PLAYLISTS = "playlistsAsJson-";
    private static final String MUST_CHECK_USER_LICENSE = "muchkul";


    private PreferencesManager() {
        prefs = Preferences.userRoot().node(this.getClass().getName());
    }

    public static PreferencesManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PreferencesManager();
        }
        return INSTANCE;
    }

    /**
     * Proxies in preferences
     */
    public void saveProxiesConfig(String proxiesConfig) {
        prefs.put(PROXIES_CONFIG, proxiesConfig);
    }

    public String getProxiesConfig() {
        return prefs.get(PROXIES_CONFIG, "");
    }

    /**
     * Terms & Condition acceptance in preferences
     */
    public boolean hasAcceptedTermsAndConditions() {
        return prefs.getBoolean(HAS_ACCEPTED_TERMS, false);
    }

    public void setHasAcceptedTermsAndConditions(boolean hasAcceptedTerms) {
        prefs.putBoolean(HAS_ACCEPTED_TERMS, hasAcceptedTerms);
    }

    public void revokeTermsAndConditions() {
        prefs.remove(HAS_ACCEPTED_TERMS);
    }

    /**
     * Android RevEnge Studio License Key in preferences
     */
    public void saveLicenseKey(String newLicenseKey) {
        prefs.put(LICENSE_KEY, newLicenseKey);
    }

    public String getLicenseKey() {
        return prefs.get(LICENSE_KEY, null);
    }

    public void deleteLicenseKey() {
        prefs.remove(LICENSE_KEY);
    }

    /**
     * If the software can be used only under license
     */
    public boolean mustCheckUserLicense() {
        return prefs.getBoolean(MUST_CHECK_USER_LICENSE, false);
    }

    public void setMustCheckUserLicense(boolean mustCheckUserLicense) {
        prefs.putBoolean(MUST_CHECK_USER_LICENSE, mustCheckUserLicense);
    }

    public void revokeMustCheckUserLicense() {
        prefs.remove(MUST_CHECK_USER_LICENSE);
    }

    /**
     * save paths of folders that are marked for delete.
     * When a user delete a project, we save project root folder path in preferences, so we check
     * that it was removed at program startup.
     */
    public void addFolderAsMarkedForDelete(String userUuid, String projectFolderNameUuid) {
        ArrayList<String> foldersMarkedForDeleteList = getFoldersMarkedForDelete(userUuid);
        String projectRootFolderPath = Configurator.getInstance()
                .getProjectRootFolderPath(userUuid, projectFolderNameUuid, false);
        foldersMarkedForDeleteList.add(projectRootFolderPath);
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        prefs.put(FOLDERS_MARKED_FOR_DELETE + userUuid, gson.toJson(foldersMarkedForDeleteList));
    }

    public ArrayList getFoldersMarkedForDelete(String userUuid) {
        String foldersMarkedForDelete = prefs.get(FOLDERS_MARKED_FOR_DELETE + userUuid, "[]");
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.fromJson(foldersMarkedForDelete, ArrayList.class);
    }

    public void updateFoldersAsMarkedForDelete(String userUuid, ArrayList<String> foldersMarkedForDeleteUpdatedList) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        prefs.put(FOLDERS_MARKED_FOR_DELETE + userUuid, gson.toJson(foldersMarkedForDeleteUpdatedList));
    }


    /**
     * NTP last recorded date in preferences
     */
    public Date getLastNtpTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-M-yyyy hh:mm:ss a");
        String formattedDateB64 = prefs.get(LAST_NTP_TIME, null);
        if (formattedDateB64 != null) {
            Date date = null;
            try {
                String formattedDate = new String(Base64.getDecoder().decode(formattedDateB64));
                date = formatter.parse(formattedDate);
            } catch (Exception e) {
                // do nothing
            }
            return date;
        } else {
            return null;
        }
    }

    public void setLastNtpTime(Date date) {
        if (date != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-M-yyyy hh:mm:ss a");
            String formattedDate = formatter.format(date);
            prefs.put(LAST_NTP_TIME, new String(Base64.getEncoder().encode(formattedDate.getBytes())));
        }
    }

    public void deleteLastNtpTime() {
        prefs.remove(LAST_NTP_TIME);
    }

    /**
     * SoundCloud user's api key in preferences
     */
    public void saveScApiKey(String apiKey) {
        prefs.put(SOUND_CLOUD_API_KEY, apiKey);
    }

    public String getScApiey() {
        return prefs.get(SOUND_CLOUD_API_KEY, null);
    }

    public void deleteScApiey() {
        prefs.remove(SOUND_CLOUD_API_KEY);
    }

    /**
     * SoundCloud user's music play lists in preferences
     */
    public void savePlaylists(String userUuid, String playlistsAsJson) {
        prefs.put(SOUND_CLOUD_USER_PLAYLISTS + userUuid, playlistsAsJson);
    }

    public String getPlaylists(String userUuid) {
        String playlistsAsJson = prefs.get(SOUND_CLOUD_USER_PLAYLISTS + userUuid, "[]");
        if (playlistsAsJson.equals("[]")) {
            try {
                Map<String, String> config = Utils.readConfigurationFile();
                String name = config.get("def_playlist_name");
                String url = config.get("def_playlist_url");

                JSONObject playlist = new JSONObject();
                String newPlaylistUuid = UUID.randomUUID().toString();
                playlist.put("uuid", newPlaylistUuid);
                playlist.put("name", name);
                playlist.put("url", url);

                JSONArray playlists = new JSONArray();
                playlists.add(playlist);

                // serialize and save
                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                playlistsAsJson = gson.toJson(playlists);
                PreferencesManager.getInstance().savePlaylists(userUuid, playlistsAsJson);
            } catch (Exception e) {
                // do nothing
            }
        }
        return playlistsAsJson;
    }

}
