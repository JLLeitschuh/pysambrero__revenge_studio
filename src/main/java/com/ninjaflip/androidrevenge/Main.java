package com.ninjaflip.androidrevenge;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.core.scrapper.ScrapperProcessBuilder;
import com.ninjaflip.androidrevenge.core.scrapper.manager.ScrappingQuotaManager;
import com.ninjaflip.androidrevenge.core.security.ServerSecurityManager;
import com.ninjaflip.androidrevenge.core.apktool.UserProcessBuilder;
import com.ninjaflip.androidrevenge.core.apktool.adb.AdbManager;
import com.ninjaflip.androidrevenge.core.apktool.adb.DebuggerProcessBuilder;
import com.ninjaflip.androidrevenge.core.db.DBManager;
import com.ninjaflip.androidrevenge.core.service.SparkServerManager;
import com.ninjaflip.androidrevenge.core.ui.UiBuilder;
import com.ninjaflip.androidrevenge.utils.NetworkAddressUtil;
import com.ninjaflip.androidrevenge.utils.Utils;
import com.ninjaflip.androidrevenge.websocket.EchoWebSocket;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.PropertyConfigurator;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Created by Solitario on 19/05/2017.
 * Compatible with java 1.8+
 */
public class Main {

    private static CompletableFuture installAdbIfNotInstalled;

    //main program entry point
    public static void main(final String[] args) {

        /*PreferencesManager.getInstance().revokeTermsAndConditions();
        PreferencesManager.getInstance().deleteLicenseKey();
        PreferencesManager.getInstance().revokeMustCheckUserLicense();
        PreferencesManager.getInstance().deleteLastNtpTime();*/

        configureLog4j();
        startAsApkToolService();
    }


    private static void configureLog4j() {
        // log4j config for development
        org.apache.log4j.BasicConfigurator.configure();

        // log4j config for production
        /*
        Properties props = new Properties();
        InputStream input = null;
        try {
            input = Main.class.getClassLoader().getResourceAsStream("conf/filelog4j.properties");
            props.load(input);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            IOUtils.closeQuietly(input);
        }
        PropertyConfigurator.configure(props);
        */
    }

    /**
     * Start as service
     */
    private static void startAsApkToolService() {
        // creating a thread pool.
        //final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
        final ExecutorService executorService = Executors.newFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                // creating a thread.
                Thread myThread = new Thread(r);
                // making it a daemon thread.
                //myThread.setDaemon(true);
                //myThread.setName("RevEnge");
                myThread.setPriority(Thread.MAX_PRIORITY);
                return myThread;
            }
        });
        executorService.submit(() -> {
            try {
                //check if java version is at least 8 ,else exit
                if (!isJava8Supported()) {
                    URL imageURL = UiBuilder.class.getClassLoader().getResource("images/tray.png");
                    ImageIcon image = new ImageIcon(imageURL, "");
                    JOptionPane.showMessageDialog(null,
                            "Bad java version: this software needs JAVA 8 or higher to work!\nPlease install JAVA 8 or higher version.",
                            "ERROR",
                            JOptionPane.ERROR_MESSAGE, image);
                    System.exit(-4);
                }

                // Always kill adb before starting spark server as it can prevent
                // app startup, if an old adb from a previous session is still running
                boolean isAdbInstalled  = AdbManager.getInstance().isAdbInstalled(null);
                if(isAdbInstalled) {
                    AdbManager.getInstance().killServer(null);
                }

                if (SparkServerManager.getInstance().isSparkServerRunning()) {
                    System.out.println("An instance is already running ===> Exit");
                    // the server is already running, we exit because only one instance of the app can run
                    URL imageURL = UiBuilder.class.getClassLoader().getResource("images/tray.png");
                    ImageIcon image = new ImageIcon(imageURL, "");
                    JOptionPane.showMessageDialog(null,
                            "RevEnge Studio is already running!","INFORMATION",
                            JOptionPane.INFORMATION_MESSAGE, image);

                    // start default browser
                    if (Desktop.isDesktopSupported()) {
                        Desktop desktop = Desktop.getDesktop();
                        if (desktop.isSupported(Desktop.Action.BROWSE)) {
                            URI uri = new java.net.URI(Configurator.getInstance().getSPARK_HTTP_PROTOCOL()
                                    + "//localhost:" + Configurator.getInstance().getSPARK_PORT()+ "/");
                            desktop.browse(uri);
                        }
                    }
                    // close splash screen if visible
                    final SplashScreen splash = SplashScreen.getSplashScreen();
                    if (splash != null && splash.isVisible()) {
                        splash.close();
                    }
                    System.exit(-2);
                } else {
                    System.out.println("No instance is running ==> OK");
                    ServerSecurityManager.resetInstance();

                    // clean tmp files asynchronously
                    CompletableFuture deleteTempFilesOnAppStart = CompletableFuture
                            .runAsync(() -> Configurator.getInstance().deleteTmpFilesOnStart());


                    // clean old quota records
                    CompletableFuture.runAsync(() -> ScrappingQuotaManager.getInstance().cleanOldRecords());

                    if(!isAdbInstalled) {
                        // install adb asynchronously (if not installed)
                        installAdbIfNotInstalled = CompletableFuture.runAsync(() -> {
                            try {
                                AdbManager.getInstance().reInstallAdb();
                            } catch (Exception e) {
                                // do  nothing
                                installAdbIfNotInstalled.completeExceptionally(e);
                            }
                        });
                    }

                    System.out.println("============== 1");
                    UiBuilder.getInstance().createSystemTray();
                    Configurator.getInstance().createWorkspaceIfNotExists();
                    System.out.println("============== 2");
                    SparkServerManager.getInstance().startSparkServer();
                    System.out.println("============== 3");
                    DBManager.getInstance(); // must call it in order to initialize the db
                    System.out.println("============== 4");
                    UiBuilder.getInstance().showTrayMessage("RevEnge Studio", "localhost:" + Configurator.getInstance().getSPARK_PORT(),
                            TrayIcon.MessageType.INFO);
                    System.out.println("============== 5");
                    UiBuilder.getInstance().setStaticImageTray();
                    System.out.println("============== 6");

                    // start default browser
                    if (Desktop.isDesktopSupported()) {
                        System.out.println("============== 7");
                        Desktop desktop = Desktop.getDesktop();
                        if (desktop.isSupported(Desktop.Action.BROWSE)) {
                            System.out.println("============== 8");
                            URI uri = new java.net.URI(Configurator.getInstance().getSPARK_HTTP_PROTOCOL()
                                    + "//localhost:" + Configurator.getInstance().getSPARK_PORT()
                                    + "/api/public/signin");
                            System.out.println("============== 9");
                            desktop.browse(uri);
                            System.out.println("============== 10");
                        }
                    }
                    System.out.println("============== 11");

                    // close splash screen if visible
                    final SplashScreen splash = SplashScreen.getSplashScreen();
                    if (splash != null && splash.isVisible()) {
                        splash.close();
                    }

                    /*
                    * Register a Shutdown hook tos stop both web and DB servers..
                    * Shutdown hooks are called when the application terminates normally
                    * (when all threads finish, or when System.exit(0) is called).
                    * Also, when the JVM is shutting down due to external causes such as user requesting a termination
                    */
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            // setIsShuttingDown true to avoid executing onWebSocketClosed()
                            UserProcessBuilder.setIsShuttingDown(true);
                            ScrapperProcessBuilder.setIsShuttingDown(true);
                            // stop spark
                            SparkServerManager.getInstance().stopSparkServer();
                            // cancel and clean running processes => synchronously
                            UserProcessBuilder.executeShutdownHook();
                            ScrapperProcessBuilder.executeShutdownHook();
                            // close db
                            DBManager.getInstance().closeDB();
                        } catch (Exception e) {
                            // do nothing
                        } finally {
                            try {
                                AdbManager.getInstance().killServer(null);
                                DebuggerProcessBuilder.executeShutdownHook();
                            } catch (Exception e) {
                                // do nothing
                            }
                        }

                        if (!deleteTempFilesOnAppStart.isDone()) {
                            deleteTempFilesOnAppStart.cancel(true);
                        }

                        if(installAdbIfNotInstalled != null && !installAdbIfNotInstalled.isDone()){
                            installAdbIfNotInstalled.cancel(true);
                        }
                        // Remove temp files before exit
                        executorService.submit(() -> {
                            Configurator.getInstance().deleteTmpFilesOnExit();
                        });
                        // force shutdown after 90 seconds if still have running tasks
                        try {
                            System.out.println("app exit => attempt to shutdown executor");
                            executorService.shutdown();
                            // wait 90 seconds at max then force shutdown
                            executorService.awaitTermination(90, TimeUnit.SECONDS);
                        } catch (InterruptedException e1) {
                            System.err.println("app exit => tasks interrupted");
                        } finally {
                            if (!executorService.isTerminated()) {
                                System.err.println("app exit => cancel non-finished tasks");
                            }
                            executorService.shutdownNow();
                            System.out.println("app exit => shutdown finished");
                        }
                    }));
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error while starting the program : " + e.getMessage());

                URL imageURL = UiBuilder.class.getClassLoader().getResource("images/tray.png");
                ImageIcon image = new ImageIcon(imageURL, "");
                JOptionPane.showMessageDialog(null,
                        "Error while starting the program : " + e.getMessage(),"ERROR",
                        JOptionPane.ERROR_MESSAGE, image);
            }
        });
    }

    /**
     * this software needs java version 8 or higher to work correctly, so we must
     * check if user's java version is JAVA8 or higher.
     * We achieve the test using a simple lambda expression as Lambda expressions were introduced in
     * java 8 and are touted to be its biggest feature
     */
    private static boolean isJava8Supported() {
        //with type declaration
        try {
            SimpleLambda addition = (int a, int b) -> a + b;
            addition.operation(2, 5);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // define an interface for testing lambda expression
    interface SimpleLambda {
        int operation(int a, int b);
    }
}
