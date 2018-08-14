package com.ninjaflip.androidrevenge.core.service;


import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.core.security.LicenseManager;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.service.handler.*;
import com.ninjaflip.androidrevenge.core.service.handler.apktools.*;
import com.ninjaflip.androidrevenge.core.service.handler.apktools.adb.AdbHandler;
import com.ninjaflip.androidrevenge.core.service.handler.apktools.debugger.DebuggerHandler;
import com.ninjaflip.androidrevenge.core.service.handler.apktools.keytool.FeDownloadKeystoreHandler;
import com.ninjaflip.androidrevenge.core.service.handler.apktools.keytool.KeytoolHandler;
import com.ninjaflip.androidrevenge.core.service.handler.apktools.previewer.BenchmarkTmpApkFileHandler;
import com.ninjaflip.androidrevenge.core.service.handler.apktools.previewer.DownloadTmpApkHandler;
import com.ninjaflip.androidrevenge.core.service.handler.apktools.previewer.UploadApkPreviewerHandler;
import com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.CloseProjectHandler;
import com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.CreateNewProjectHandler;
import com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.DeleteProjectHandler;
import com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.fileEditor.*;
import com.ninjaflip.androidrevenge.core.service.handler.apktools.projects.GetAllProjectsHandler;
import com.ninjaflip.androidrevenge.core.service.handler.general.*;
import com.ninjaflip.androidrevenge.core.service.handler.general.UserProfileHandler;
import com.ninjaflip.androidrevenge.core.service.handler.publichandler.*;
import com.ninjaflip.androidrevenge.core.service.handler.scrapper.*;
import com.ninjaflip.androidrevenge.websocket.EchoWebSocket;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import com.ninjaflip.androidrevenge.utils.Utils;
import spark.Filter;
import spark.Spark;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.HashMap;

/**
 * Created by Solitario on 20/05/2017.
 * <p>
 * A server instance based on Spark Framework (http://sparkjava.com/)
 * it wraps the communication between the browser and the desktop app
 */

public class SparkServerManager {
    private static SparkServerManager INSTANCE;

    private SparkServerManager() {
    }

    public static SparkServerManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SparkServerManager();
        }
        return INSTANCE;
    }

    /**
     * Check if there is an already running spark server instance
     *
     * @return true if the spark server is running, false otherwise
     */
    public boolean isSparkServerRunning() {
        String server = Configurator.getInstance().getSPARK_HTTP_PROTOCOL() + "//localhost:" + Configurator.getInstance().getSPARK_PORT();
        try {
            URLConnection hpCon = new URL(server).openConnection();
            hpCon.connect();
            return true;
        } catch (Exception e) {
            // when there is a failure it meas that the server is already running
            return false;
        }
    }

    /**
     * Spark server is responsible for http rest webservices and acts as a web-socket handler
     */
    public void startSparkServer() {
        System.out.println("Starting Spark server...");
        ExecutionTimer exTimer = new ExecutionTimer();
        exTimer.start();
        Spark.port(Configurator.getInstance().getSPARK_PORT()); // Spark will run on this port

        // TODO create a keystore and a trust store and then use ssl/https
        //Spark.secure(keystoreFilePath, keystorePassword, truststoreFilePath, truststorePassword, false);

        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("Available cores => " + cores);
        //Spark.threadPool(cores);
        //int maxThreads = 8;
        //Spark.threadPool(maxThreads);
        // set static files folder
        //Spark.staticFiles.externalLocation(Configurator.getInstance().getSparkServerStaticFilesLocation());

        // static file for app
        Spark.staticFiles.location("/www");
        // static tmp file
        //Spark.staticFiles.externalLocation(Configurator.getInstance().getTMP());

        System.out.println("============== A1");
        // Not found (code 404) custom handling
        //Spark.notFound(new NotFoundErrorHandler("Oops! HTTP ERROR 404: resource not found"));

        //Spark.staticFiles.expireTime(604800); // cache file for 7 days

        // web-socket handler
        Spark.webSocket("/echo", EchoWebSocket.class);
        System.out.println("============== A2");
        /*
         * public api
          */
        Spark.get("/browser_not_supported", new BrowserNotSupportedHandler());
        Spark.get("/api/public/signin", new SignInPageHandler());
        Spark.post("/api/public/termsAndCondition", new TermsAndConditionsHandler());
        Spark.post("/api/public/productactivation", new ProductActivationHandler());
        Spark.post("/api/public/userlogin", new UserLoginHandler());
        Spark.get("/api/public/downloadFile", new DownloadFileHandler());
        Spark.post("/api/public/forgotpwd", new ForgotPasswordHandler());
        Spark.post("/api/public/gethtmlmodal", new GetHtmlModalContentHandler());

        /*
         * protected api
          */
        // index page
        Spark.get("", new IndexPageHandler());
        Spark.get("/", new IndexPageHandler());
        Spark.get("/index.html", new IndexPageHandler());
        Spark.get("/my_index.html", new IndexPageHandler());
        Spark.get("/index", new IndexPageHandler());

        // initial calls
        Spark.post("/api/protected/initconnection", new InitConnectionHandler());
        Spark.post("/api/protected/remoteinstruction", new RemoteInstructionHandler());
        Spark.get("/api/protected/loadassets", new LoadAssetsHandler());
        // scrapper
        Spark.get("/api/protected/keywordapi", new KeywordSearchRequestHandler());
        Spark.post("/api/protected/multilangscrapperapi", new AppScrappingRequestHandler());
        Spark.get("/api/protected/topapps", new TopAppsHandler());
        Spark.post("/api/protected/favoriteapps", new FavoriteAppsHandler());
        Spark.post("/api/protected/appcountries", new DetectAppCountriesHandler());

        // apk reverse
        Spark.post("/api/protected/uploadTmpFile", new UploadTmpFileHandler());
        Spark.post("/api/protected/uploadApkPreviewerHandler", new UploadApkPreviewerHandler());
        Spark.get("/api/protected/getApkInfo", new getApkInfoHandler());
        Spark.delete("/api/protected/deleteTmpFileHandler", new DeleteTmpFileHandler());
        Spark.post("/api/protected/cancelAplToolProcessHandler", new CancelUserProcessHandler());

        Spark.post("/api/protected/benchmarkTmpApkFileHandler", new BenchmarkTmpApkFileHandler());
        Spark.post("/api/protected/androidDebugBridgeHandler", new AndroidDebugBridgeHandler());
        Spark.get("/api/protected/downloadTmpApkFile", new DownloadTmpApkHandler());

        Spark.get("/api/protected/getAllApkProjects", new GetAllProjectsHandler());
        Spark.get("/api/protected/loadProjectForEditor", new FeLoadProjectHandler());
        Spark.post("/api/protected/createNewProject", new CreateNewProjectHandler());
        Spark.post("/api/protected/deleteProject", new DeleteProjectHandler());
        Spark.post("/api/protected/closeProject", new CloseProjectHandler());

        Spark.get("/api/protected/getFileContent", new FeGetFileContentHandler());
        Spark.post("/api/protected/updateFileContent", new FeSyncEditableFileHandler());
        Spark.get("/api/protected/mediaStreamer", new MediaFileStreamingHandler()); // for project file editor
        Spark.get("/api/protected/getNodeInfoForEditor", new FeJsTreeLazyLoadingHandler());
        Spark.get("/api/protected/searchNodeForEditor", new FeJsTreeSearchHandler());
        Spark.get("/api/protected/massloadNodeForEditor", new FeJsTreeMassLoadHandler());
        Spark.get("/api/protected/contextMenuForEditor/downloadFile", new FeDownloadFileHandler()); // get method
        Spark.post("/api/protected/contextMenuForEditor", new FeJsTreeContextMenuHandler());
        Spark.get("/api/protected/getProjectInfoForEditor", new FeGetProjectInfoHandler());
        Spark.post("/api/protected/textSearchForEditor", new FeTextSearchHandler());
        Spark.post("/api/protected/toolsOfEditor", new FeToolsHandler());
        Spark.post("/api/protected/keytool", new KeytoolHandler());
        Spark.get("/api/protected/downloadApkFile", new FeDownloadApkHandler()); // get method
        Spark.get("/api/protected/downloadKeystoreFile", new FeDownloadKeystoreHandler()); // get method
        Spark.post("/api/protected/adbinstallTmpApkFileHandler", new AdbHandler());
        Spark.post("/api/protected/debuggerHandler", new DebuggerHandler());

        // profile
        Spark.post("/api/protected/profile", new UserProfileHandler());
        // music player
        Spark.post("/api/protected/musicplayer", new MusicPlayerHandler());

        System.out.println("============== A3");
        // enable CORS request and filters
        enableCORS_and_filters();
        System.out.println("============== A4");

        Spark.init(); // Needed if you don't define any HTTP routes after your WebSocket routes
        System.out.println("============== A5");
        Spark.awaitInitialization(); // Wait for server to be initialized
        System.out.println("============== A6");
        exTimer.end();
        System.out.println("Spark Server started in " + exTimer.durationInSeconds() + " seconds\n");
    }

    // Enables CORS and filters on requests. This method is an initialization method and should be called once on server startup.
    private void enableCORS_and_filters(){
        final HashMap<String, String> corsHeaders = new HashMap<String, String>();
        corsHeaders.put("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
        corsHeaders.put("Access-Control-Allow-Headers", "X-PINGOTHER, Content-Type, Authorization, X-Requested-With, Content-Length, Accept, Origin, *");
        corsHeaders.put("Access-Control-Allow-Credentials", "true");

        // Filter for public requests
        Filter filterPublic = (request, response) -> {
            corsHeaders.forEach(response::header);
            response.header("Access-Control-Allow-Origin", request.headers("Origin"));
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            //System.out.println("Spark.Public.beforeFilter");
        };
        Spark.path("/browser_not_supported", () -> Spark.before("/browser_not_supported", filterPublic));
        Spark.path("/api/public", () -> Spark.before("/*", filterPublic));

        // Filter for protected requests that needs authentication credentials
        Filter filterProtected = (request, response) -> {
            corsHeaders.forEach(response::header);
            response.header("Access-Control-Allow-Origin", request.headers("Origin"));
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            if (PreferencesManager.getInstance().mustCheckUserLicense()) {
                // check license, if not OK => exit program
                LicenseManager.getInstance().checkpoint();
            }

            // check credentials if request method is not OPTIONS
            if (!request.requestMethod().toUpperCase().equals("OPTIONS")) {
                String token = request.cookie("token");
                String userId = request.cookie("userId");

                //System.out.println("Spark.Protected.beforeFilter => Cookie desktopToken " + token);
                //System.out.println("Spark.Protected.beforeFilter => Cookie userId " + userId);

                if (token != null && userId != null) {
                    boolean isValidToken = false;
                    try {
                        isValidToken = ServerSecurityManager.getInstance().verifyToken(token, userId);
                    } catch (Exception e) {
                        Spark.halt(500, "error : " + e.getMessage());
                    }
                    if (!isValidToken) { // invalid credentials
                        if(request.requestMethod().toUpperCase().equals("GET")) {
                            response.header("REQUIRES_AUTH","1");
                            response.redirect("/api/public/signin");
                            Spark.halt();
                        }else{
                            Spark.halt(401, "invalid credentials!");
                        }
                    }
                } else {
                    // null credentials
                    response.header("REQUIRES_AUTH","1");
                    response.redirect("/api/public/signin");
                    Spark.halt();
                    //Spark.halt(400, "null credentials!");
                }
            }
        };
        Spark.path("/api/protected", () -> Spark.before("/*", filterProtected));
        // also on index page
        Spark.path("", () -> Spark.before("", filterProtected));
        Spark.path("/", () -> Spark.before("/", filterProtected));
        Spark.path("/index.html", () -> Spark.before("/index.html", filterProtected));
        Spark.path("/my_index.html", () -> Spark.before("/my_index.html", filterProtected));
        Spark.path("/index", () -> Spark.before("/index", filterProtected));

        ServerSecurityManager.getInstance().checkIfMustBlockAccess();
    }

    /**
     * Stops the server instance
     */
    public void stopSparkServer() {
        System.out.println("\nStopping Spark Server...");
        Spark.stop();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Spark Server stopped");
    }

    /**
     * Build url from static file's path
     * this url is only available from local network
     *
     * @param filePath static file absolute path
     * @return HTTP url to static file only available from local network
     */
    public static String getStaticFileUrlFromPath_localIp(String filePath) {
        String host;
        try {
            host = Utils.getLocalHostLANAddress().getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            try {
                InetAddress IP = InetAddress.getLocalHost();
                host = IP.getHostAddress();
            } catch (UnknownHostException e1) {
                System.err.println(e1.getMessage());
                e1.printStackTrace();
                host = "localhost";
            }
        }


        String serverUrl = Configurator.getInstance().getSPARK_HTTP_PROTOCOL() + "//"
                + host + ":" + Configurator.getInstance().getSPARK_PORT();

        String fileRelativePath = filePath.replace(Configurator.getInstance().getWork_DIR(), "")
                .replace(File.separator, "/");
        String downloadUrl = null;
        try {
            downloadUrl = serverUrl + "/api/public/downloadFile?relative_path=" + URLEncoder.encode(fileRelativePath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return downloadUrl;
    }
}
