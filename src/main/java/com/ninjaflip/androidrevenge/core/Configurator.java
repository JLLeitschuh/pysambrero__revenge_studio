package com.ninjaflip.androidrevenge.core;

import com.ninjaflip.androidrevenge.utils.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Solitario on 22/05/2017.
 * <p>
 * A singleton class that wraps program initialization and configuration
 */
public class Configurator {
    private static Configurator INSTANCE;

    // spark server configuration
    private String SPARK_HTTP_PROTOCOL;
    private int SPARK_PORT;

    // Workspace folders
    private String DB, SOFT, CACHE, CONF, ADB_DIR, ADB_BIN_DIR, WORK_DIR, TMP, USERS_DIR, DECODED_APK_DIR_NAME, GRAPH_FILE_NAME, SRC_APK_DIR_NAME;
    // urls
    private String URL_ADB_WINDOWS, URL_ADB_MAC, URL_ADB_LINUX;

    //public static final int IMAGE_MAX_SIZE_THUMB = 10240; // if image size in bytes exceeds this IMAGE_MAX_SIZE_THUMB, we scale it
    public static final int IMAGE_MAX_SIZE_THUMB = 30720; // if image size in bytes exceeds this IMAGE_MAX_SIZE_THUMB, we scale it (30 kb)
    public static final int THUMB_IMG_MAX_DIM_PX = 200; // thumbnail image max width and height in pixels


    private Configurator() {
        Map<String, String> config = Utils.readConfigurationFile();
        String ROOT_DIR = System.getProperty("user.home") + File.separator + config.get("root_dir_name");

        DB = ROOT_DIR + File.separator + config.get("db_dir_name");
        SOFT = ROOT_DIR + File.separator + config.get("soft_dir_name");
        ADB_DIR = SOFT + File.separator + config.get("adb_dir_name");
        ADB_BIN_DIR = ADB_DIR + File.separator + config.get("adb_bin_dir_name");
        CACHE = ROOT_DIR + File.separator + config.get("cache_dir_name");
        CONF = ROOT_DIR + File.separator + config.get("config_dir_name");
        WORK_DIR = ROOT_DIR + File.separator + config.get("work_dir_name");
        TMP = WORK_DIR + File.separator + config.get("tmp_dir_name");
        USERS_DIR = WORK_DIR + File.separator + config.get("users_dir_name");
        DECODED_APK_DIR_NAME = config.get("decoded_apk_dir_name");
        GRAPH_FILE_NAME = config.get("graph_file_name");
        SRC_APK_DIR_NAME = config.get("src_apk_dir_name");

        SPARK_HTTP_PROTOCOL = config.get("spark_default_http_protocol");
        SPARK_PORT = Integer.parseInt(config.get("spark_default_port"));

        // adb urls
        URL_ADB_WINDOWS = config.get("url_adb_windows");
        URL_ADB_MAC = config.get("url_adb_mac");
        URL_ADB_LINUX = config.get("url_adb_linux");
    }

    public static Configurator getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Configurator();
        }
        return INSTANCE;
    }

    public String getSPARK_HTTP_PROTOCOL() {
        return SPARK_HTTP_PROTOCOL;
    }

    public int getSPARK_PORT() {
        return SPARK_PORT;
    }

    public String getWork_DIR() {
        return WORK_DIR;
    }


    public String getDB() {
        return DB;
    }

    public String getSOFT() {
        return SOFT;
    }

    public String getADB_DIR() {
        return ADB_DIR;
    }

    public String getADB_BIN_DIR() {
        return ADB_BIN_DIR;
    }

    public String getCACHE() {
        return CACHE;
    }

    public String getTMP() {
        return TMP;
    }

    public String getUSERS_DIR() {
        return USERS_DIR;
    }

    public String getURL_ADB_WINDOWS() {
        return URL_ADB_WINDOWS;
    }

    public String getURL_ADB_MAC() {
        return URL_ADB_MAC;
    }

    public String getURL_ADB_LINUX() {
        return URL_ADB_LINUX;
    }



    /**
     * Build path to user's temp folder
     *
     * @param userUuid the uuid of the current user
     * @return the full path to user's temp directory
     */
    public String getUserTmpFolderPath(String userUuid) {
        return TMP + File.separator + userUuid;
    }


    /**
     * This method builds the path to the project root folder
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           true if the project is a temporary project (benchmark test of an apk), false if real project
     * @return the full path to project folder
     */
    public String getProjectRootFolderPath(String userUuid, String projectFolderNameUuid, boolean isTemporary) {
        String root;
        if (isTemporary)
            root = TMP;
        else
            root = USERS_DIR;
        return root
                + File.separator + userUuid
                + File.separator + projectFolderNameUuid;
    }

    /**
     * This method builds a path to the folder that will contain decoded apk files, it depends on the user's uuid.
     * Path=use.home/.android_revenge/workfolder/users/user-uuid/project-uuid/decoded/
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           true if the project is a temporary project (benchmark test of an apk), false if real project
     * @return the full path to folder containing decoded apk files
     */
    public String getDecodedApkFolderPath(String userUuid, String projectFolderNameUuid, boolean isTemporary) {
        String root;
        if (isTemporary)
            root = TMP;
        else
            root = USERS_DIR;
        return root
                + File.separator + userUuid
                + File.separator + projectFolderNameUuid
                + File.separator + DECODED_APK_DIR_NAME;
    }

    /**
     * This method builds the path to the smali folder containing smali files
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           true if the project is a temporary project (benchmark test of an apk), false if real project
     * @return the full path to folder containing smali files
     */
    public String getSmaliFolderPath(String userUuid, String projectFolderNameUuid, boolean isTemporary) {
        String root;
        if (isTemporary)
            root = TMP;
        else
            root = USERS_DIR;
        return root
                + File.separator + userUuid
                + File.separator + projectFolderNameUuid
                + File.separator + DECODED_APK_DIR_NAME
                + File.separator + "smali";
    }

    /**
     * This method builds the path to the dist folder containing the built apk, made by apktool
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           true if the project is a temporary project (benchmark test of an apk), false if real project
     * @return the full path to "dist" folder built apk
     */
    public String getDistFolderPath(String userUuid, String projectFolderNameUuid, boolean isTemporary) {
        String root;
        if (isTemporary)
            root = TMP;
        else
            root = USERS_DIR;
        return root
                + File.separator + userUuid
                + File.separator + projectFolderNameUuid
                + File.separator + DECODED_APK_DIR_NAME
                + File.separator + "dist";
    }

    /**
     * This method builds the path to the build folder containing the build files, made by apktool
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           true if the project is a temporary project (benchmark test of an apk), false if real project
     * @return the full path to "dist" folder built apk
     */
    public String getBuildFolderPath(String userUuid, String projectFolderNameUuid, boolean isTemporary) {
        String root;
        if (isTemporary)
            root = TMP;
        else
            root = USERS_DIR;
        return root
                + File.separator + userUuid
                + File.separator + projectFolderNameUuid
                + File.separator + DECODED_APK_DIR_NAME
                + File.separator + "build";
    }


    /**
     * This method builds the path to the folder contains signed/zipaligned apk generated by uber apk tool
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           true if the project is a temporary project (benchmark test of an apk), false if real project
     * @return the full path to "dist" folder built apk
     */
    public String getGenFolderPath(String userUuid, String projectFolderNameUuid, boolean isTemporary) {
        String root;
        if (isTemporary)
            root = TMP;
        else
            root = USERS_DIR;
        return root
                + File.separator + userUuid
                + File.separator + projectFolderNameUuid
                + File.separator + "gen";
    }

    /**
     * This method builds the path to the project's graph file
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           true if the project is a temporary project (benchmark test of an apk), false if real project
     * @return the full path to project's graph file
     */
    public String getGraphFilePath(String userUuid, String projectFolderNameUuid, boolean isTemporary) {
        String root;
        if (isTemporary)
            root = TMP;
        else
            root = USERS_DIR;
        return root
                + File.separator + userUuid
                + File.separator + projectFolderNameUuid
                + File.separator + GRAPH_FILE_NAME;
    }

    /**
     * This method build the path to a folder inside project's folder called 'SRC_APK_DIR_NAME' which
     * contains a copy f the original apk files, from which the project was creates (decompiled original apk)
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           true if the project is a temporary project, false if real project
     * @return the full path to project's containing the original apk file
     */
    public String getSrcApkFolder(String userUuid, String projectFolderNameUuid, boolean isTemporary) {
        String root;
        if (isTemporary)
            root = TMP;
        else
            root = USERS_DIR;
        return root
                + File.separator + userUuid
                + File.separator + projectFolderNameUuid
                + File.separator + SRC_APK_DIR_NAME;
    }


    /**
     * Creates the necessary directories if they don't exist:
     * cache: for caching
     * libs: contains jars
     * db for database
     */
    public void createWorkspaceIfNotExists() {
        new File(DB).mkdirs();
        new File(SOFT).mkdirs();
        new File(CACHE).mkdirs();
        new File(CONF).mkdirs();
        new File(TMP).mkdirs();
        new File(DB).mkdirs();
        new File(USERS_DIR).mkdirs();
    }


    /**
     * Delete all folders marked as delete later
     * @param userUuid the owner of those folders
     */
    public void deleteFoldersMarkedForDeleteOnStart(String userUuid){
        ArrayList<String> foldersMarkedForDeleteList = PreferencesManager.getInstance().getFoldersMarkedForDelete(userUuid);
        List<String> arrayCopy = new ArrayList<>(foldersMarkedForDeleteList);
        // remove folders
        for(String path : foldersMarkedForDeleteList){
            File folder = new File(path);
            if(folder.exists() && folder.isDirectory()){
                try{
                    FileUtils.deleteDirectory(folder);
                }catch (Exception e){
                    try{
                        FileUtils.deleteDirectory(folder);
                    }catch (Exception e1){
                        // do nothing
                    }
                }
            }
        }

        // clean reference
        for (String path : arrayCopy) {
            File folder = new File(path);
            if (!folder.exists() && foldersMarkedForDeleteList.contains(path)) {
                foldersMarkedForDeleteList.remove(path);
            }
        }
        // update reference
        PreferencesManager.getInstance().updateFoldersAsMarkedForDelete(userUuid, foldersMarkedForDeleteList);
    }


    /**
     * Delete all temporary files on app start
     */
    public void deleteTmpFilesOnStart() {
        File tmpFolder = new File(Configurator.getInstance().getTMP());
        if (tmpFolder.exists() && tmpFolder.isDirectory()) {
            System.out.println("app on start => Deleting temp files...");
            File[] tmpFiles = tmpFolder.listFiles();
            if (tmpFiles != null) {
                for (File f : tmpFiles) {
                    if (f.isDirectory()) {
                        try {
                            FileUtils.deleteDirectory(f);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (f.isFile()) {
                        try {
                            FileUtils.forceDelete(f);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            System.out.println("app on start => Deleting temp files DONE!");
        }
    }

    /**
     * Delete all temporary files on app exit
     */
    public void deleteTmpFilesOnExit(){
        File tmpFolder = new File(Configurator.getInstance().getTMP());
        if (tmpFolder.exists() && tmpFolder.isDirectory()) {
            AtomicReference<Map<String, Object>> removeStats = Utils.rmdirThreadSafe(tmpFolder);

            System.out.println("app on exit => Deleting temp files...");
            while (((Integer) removeStats.get().get("percent")) != -1) {
                String msg = "Deleting temp files...  " + removeStats.get().get("percent") + "%";
                System.out.print("\r" + msg);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("\r");
            ArrayList<String> notRemovedFiles = (ArrayList) removeStats.get().get("notRemovedFiles");
            for (String path : notRemovedFiles) {
                System.out.println("can't remove: " + path);
            }
            System.out.println("app on exit => Deleting temp files DONE!");
        }
    }

    /**
     * Remove the jar main file
     */
    public void selfDelete(){
        String path = Configurator.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            System.out.println("Jar path is : " + decodedPath);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
