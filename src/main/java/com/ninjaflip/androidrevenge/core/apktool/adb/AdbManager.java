package com.ninjaflip.androidrevenge.core.apktool.adb;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.enums.OS;
import com.ninjaflip.androidrevenge.exceptions.AdbExecutionException;
import com.ninjaflip.androidrevenge.utils.Download;
import com.ninjaflip.androidrevenge.utils.StringUtil;
import com.ninjaflip.androidrevenge.utils.Utils;
import com.ninjaflip.androidrevenge.utils.Zipper;
import net.lingala.zip4j.exception.ZipException;
import net.minidev.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Solitario on 31/05/2017.
 * <p>
 * Adroid Debug Bridge wrapper
 */
public class AdbManager {
    private final static Logger LOGGER = Logger.getLogger(AdbManager.class);
    private static AdbManager INSTANCE;
    private String adbExecutablePath;
    private Download download = null;

    private AdbManager() {
        OS osType = Utils.getOsType();
        if (osType == OS.WIN) {
            adbExecutablePath = Configurator.getInstance().getADB_BIN_DIR() + File.separator + "adb.exe";
        } else {
            adbExecutablePath = Configurator.getInstance().getADB_BIN_DIR() + File.separator + "adb";
        }
    }

    public static AdbManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AdbManager();
        }
        return INSTANCE;
    }

    /**
     * Check if adb is installed
     *
     * @return true if installed false otherwise
     */
    public boolean isAdbInstalled(String userUuid) {
        if (new File(adbExecutablePath).exists()) {
            try {
                executeCommand(userUuid, adbExecutablePath, "version");
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }


    /**
     * Install adb tool
     *
     * @throws IOException
     * @throws ZipException
     */
    public void reInstallAdb() throws IOException, ZipException {
        reInstallAdbFromLocal();
    }

    /**
     * Install adb tool by downloading it from remote server (depends on OS), and extracting it to work folder.
     *
     * @throws IOException
     * @throws ZipException
     */
    private void reInstallAdbFromRemoteServer() throws IOException, ZipException {
        /*
        * Cancel ongoing adb install
         */
        cancelAdbInstall();

        /*
        * Get download url by OS type
         */
        OS osType = Utils.getOsType();
        String urlZip;

        if (osType == OS.WIN) {
            urlZip = Configurator.getInstance().getURL_ADB_WINDOWS();
        } else if (osType == OS.MAC) {
            urlZip = Configurator.getInstance().getURL_ADB_MAC();
        } else {
            urlZip = Configurator.getInstance().getURL_ADB_LINUX();
        }

        /*
         * Download the zip file containing adb from server to TEMP file
          */
        File adbZip = File.createTempFile("ADB_install", ".zip");
        adbZip.deleteOnExit();

        //urlZip = "https://dl.google.com/android/repository/platform-tools-latest-linux.zip";
        LOGGER.info("downloading url '" + urlZip + "' to temp file : '" + adbZip + "'");
        URL verifiedUrl = StringUtil.verifyUrl(urlZip.trim());
        //FileUtils.copyURLToFile(verifiedUrl, adbZip);

        if (verifiedUrl == null) {
            throw new IllegalArgumentException("Invalid Download URL : " + urlZip);
        }
        download = new Download(verifiedUrl, adbZip);
        double downloadSizeInMb = download.getSize() / 1048576.0;

        while (download.getProgress() < 100 && download.getStatus() == Download.DOWNLOADING) {
            LOGGER.info("downloading..." + new DecimalFormat("###.##").format(download.getProgress())
                    + " % of " + downloadSizeInMb + " Mb");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        /*
         * Extracting the zip file to the libs work folder
          */
        if (download.getStatus() == Download.COMPLETE) {
            LOGGER.info("download finished");
            // TODO pass from properties and encrypted
            String password = "skafandri";
            Zipper zipper = new Zipper(password);
            zipper.unpack(adbZip.getPath(), Configurator.getInstance().getADB_DIR());
            LOGGER.info("install finished");
        } else {
            LOGGER.error("Download failed!");
            throw new RuntimeException("Failed to install adb tool:");
        }
    }

    /**
     * Install adb tool from resources (depends on OS), and extracting it to work folder.
     *
     * @throws IOException
     * @throws ZipException
     */
    private void reInstallAdbFromLocal() throws IOException, ZipException {
        LOGGER.info("installing ADB tool...");
        /*
        * Cancel ongoing adb install
         */
        cancelAdbInstall();

        /*
        * Get download url by OS type
         */
        OS osType = Utils.getOsType();
        String zipFleName = "adbzip/platform-tools-latest-windows.zip";

        /*String zipFleName;
        if (osType == OS.WIN) {
            zipFleName = "adbzip/platform-tools-latest-windows.zip";
        } else if (osType == OS.MAC) {
            zipFleName = "adbzip/platform-tools-latest-darwin.zip";
        } else {
            zipFleName = "adbzip/platform-tools-latest-linux.zip";
        }*/

        // copy zip OS corresponding adb.zip file to tmp folder
        File tmpFolder = Files.createTempDirectory("tmpadb-").toFile();
        File tmpZipFile = File.createTempFile(zipFleName, null, tmpFolder);
        Files.copy(getClass().getClassLoader().getResourceAsStream(zipFleName), tmpZipFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // extract adb.zip file to its final destination
        LOGGER.info("extracting adb to tmp folder...");

        String password = "skafandri";
        Zipper zipper = new Zipper(password);
        zipper.unpack(tmpZipFile.getPath(), Configurator.getInstance().getADB_DIR());

        LOGGER.info("installing ADB tool > DONE");

        tmpFolder.deleteOnExit();
    }

    /**
     * Cancel ongoing adb install
     *
     * @throws IOException
     */
    private void cancelAdbInstall() throws IOException {
        /*
        * Cancel ADB download if already started
         */
        if (download != null && download.getStatus() == Download.DOWNLOADING) {
            download.cancel();
        }
        /*
        * Delete ADB lib folder
         */
        File adbFolder = new File(Configurator.getInstance().getADB_BIN_DIR());
        try {
            FileUtils.deleteDirectory(adbFolder);
        } catch (Exception e1) {
            try {
                FileUtils.deleteDirectory(adbFolder);
            } catch (Exception e2) {
                try {
                    FileUtils.deleteDirectory(adbFolder);
                } catch (Exception e3) {
                    // do nothing
                }
            }
        }
    }

    /*
    * Start adb if not already started
     */
    public Process startAdbServer(String userUuid) throws InterruptedException, IOException {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("*****   Starting ADB       *****");
        LOGGER.info("********************************");
        LOGGER.info(" ");
        return executeCommand(userUuid, adbExecutablePath, "start-server");
    }

    /*
    * Restarts the adbd daemon listening on USB
     */
    public Process restartOnUSB(String userUuid) throws InterruptedException, IOException {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("***   Restarting ADB on USB  ***");
        LOGGER.info("********************************");
        LOGGER.info(" ");
        return executeCommand(userUuid, adbExecutablePath, "usb");
    }

    /*
    * Restarts the adbd daemon listening on TCP on the specified port
     */
    public Process restartOnTCP(int port, String userUuid) throws InterruptedException, IOException {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("***   Restarting ADB on TCP  ***");
        LOGGER.info("********************************");
        LOGGER.info(" ");
        return executeCommand(userUuid, adbExecutablePath, "tcpip", String.valueOf(port));
    }

    /*
    * list all connected devices
    */
    public Process listDevices(String userUuid) throws InterruptedException, IOException {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("******  ADB list devices  ******");
        LOGGER.info("********************************");
        LOGGER.info(" ");
        return executeCommand(userUuid, adbExecutablePath, "devices");
    }

    /*
    * connect <host>:<port>         - connect to a device via TCP/IP
     */
    public Process connect(String host, int port, String userUuid) throws InterruptedException, IOException {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("*****  ADB TCP/IP connect  *****");
        LOGGER.info("********************************");
        LOGGER.info(" ");
        return executeCommand(userUuid, adbExecutablePath, "connect", host + ":" + String.valueOf(port));
    }

    /*
    * disconnect <host>:<port>      - disconnect from a TCP/IP device
     */
    public Process disconnect(String host, int port, String userUuid) throws InterruptedException, IOException {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("***  ADB TCP/IP disconnect *****");
        LOGGER.info("********************************");
        LOGGER.info(" ");
        return executeCommand(userUuid, adbExecutablePath, "disconnect", host + ":" + String.valueOf(port));
    }

    /*
    * Use ADB to discover IP:
     */
    public Process discoverIpInfo(String userUuid) throws InterruptedException, IOException {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("******  ADB discover IP ********");
        LOGGER.info("********************************");
        LOGGER.info(" ");
        //adb shell ip -f inet addr show
        return executeCommand(userUuid, adbExecutablePath, "shell", "ip", "-f", "inet", "addr", "show");
    }

    /*
    * Kill the server if it is running
     */
    public Process killServer(String userUuid) throws InterruptedException, IOException {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("******  ADB kill server ********");
        LOGGER.info("********************************");
        LOGGER.info(" ");
        return executeCommand(userUuid, adbExecutablePath, "kill-server");
    }

    /*
    * Makes ADB wait for any device to be in the state of 'device'
    * This method lets the ADB script wait for user to connect his device to usb port before continuing the execution.
    * Timeout is 30 second.
     * If timeout reached, it throws an InterruptedException
     */
    public void waitForDevice(String userUuid) throws InterruptedException, IOException, AdbExecutionException {
        LOGGER.info("ADB waiting for devices...");
        executeCommandWithTimeout(userUuid, 30000, "Problem waiting for device timeout!",
                "Please connect your device to this computer via USB...",
                adbExecutablePath, "wait-for-any-device");
    }

    public Process adbUsbDebug(String packageName, String userUuid) throws IOException, InterruptedException {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("***** Adb Start USB Debug ******");
        LOGGER.info("********************************");
        LOGGER.info(" ");
        //return executeCommand(adbExecutablePath, "shell", "monkey", "-p", packageName, "1");
        return executeCommand(userUuid, adbExecutablePath, "-d", "logcat ", packageName + ":I", "*:S");
    }

    /*
    * Launch app
     */
    public Process launchApp(String packageName, String userUuid) throws InterruptedException, IOException, AdbExecutionException {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("***** Launching the app ********");
        LOGGER.info("********************************");
        LOGGER.info(" ");
        //return executeCommand(userUuid,adbExecutablePath, "shell", "monkey", "-p", packageName, "1");
        executeCommandWithTimeout(userUuid, 20000, "Problem waiting for device timeout!",
                "Please connect your device to this computer via USB...",
                adbExecutablePath, "wait-for-any-device");
        return executeCommand(userUuid, adbExecutablePath, "shell", "monkey", "-p", packageName, "-v", "3");
    }

    /*
    * clea app data
     */
    public Process adbCleanData(String packageName, String userUuid) throws InterruptedException, IOException {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("******** ADB clean data ********");
        LOGGER.info("********************************");
        LOGGER.info(" ");
        return executeCommand(userUuid, adbExecutablePath, "shell", "pm", "clean", packageName);
    }

    /*
    * uninstall package name
     */
    public Process adbUninstall(String packageName, String userUuid) throws InterruptedException, IOException, AdbExecutionException {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("******** ADB uninstall ********");
        LOGGER.info("********************************");
        LOGGER.info(" ");
        executeCommandWithTimeout(userUuid, 20000, "Problem waiting for device timeout!",
                "Please connect your device to this computer via USB...",
                adbExecutablePath, "wait-for-any-device");
        return executeCommand(userUuid, adbExecutablePath, "uninstall", packageName);
    }


    /*
    * launch market intent to install a certain package name
     */
    public Process startMarketIntent(String packageName, String userUuid) throws InterruptedException, IOException {
        return executeCommand(userUuid, adbExecutablePath, "shell", "am", "start", "-a",
                "android.intent.action.VIEW", "-d", "'market://details?id=" + packageName + "'");
    }

    /**
     * Adb get devices
     *
     * @return Map<String, List<String>> devices, devices.get("usbDevices") contains USB devices and devices.get("ipDevices") contains IP devices
     * @throws IOException
     * @throws InterruptedException
     */
    public Map<String, List<String>> getDevices(String userUuid) throws IOException, InterruptedException {
        Map<String, List<String>> devices = new HashMap<String, List<String>>();
        List<String> usbDevices = new ArrayList<>();
        List<String> ipDevices = new ArrayList<>();

        Pattern patternIp = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})(\\s+)(device)");
        Pattern patternUsb = Pattern.compile("(((?=\\S*[0-9])(?=\\S*[a-zA-Z])[a-zA-Z0-9]+)(\\s+)(device))");

        ProcessBuilder pb = new ProcessBuilder(adbExecutablePath, "devices");
        pb.redirectErrorStream(true);
        Process p = pb.start();


        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.matches(patternIp.pattern())) {
                Matcher matcher = patternIp.matcher(line);
                if (matcher.find()) {
                    String device = line.split("\\t")[0];
                    System.out.println("Ip ====> " + device);
                    ipDevices.add(device);
                }
            } else if (line.matches(patternUsb.pattern())) {
                Matcher matcher = patternUsb.matcher(line);
                if (matcher.find()) {
                    String device = line.split("\\t")[0];
                    System.out.println("usb ====> " + device);
                    usbDevices.add(device);
                }
            }
        }
        p.waitFor();

        devices.put("usbDevices", usbDevices);
        devices.put("ipDevices", ipDevices);
        return devices;
    }


    /**
     * get all user processes executing on Android device except those having
     * their package name starts with 'com.android' or 'com.sec.android'
     *
     * @return a list of json objects, each containing Process Id and Process nae
     * @throws IOException
     * @throws InterruptedException
     */
    public List<JSONObject> getProcesses(String userUuid) throws IOException, InterruptedException {
        List<JSONObject> result = new ArrayList<>();

        ProcessBuilder pb = new ProcessBuilder(adbExecutablePath, "shell",
                "ps | grep '^u'| grep -v -e 'com.sec.android' -e 'com.android.'");
        pb.redirectErrorStream(true);
        Process p = pb.start();


        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            //LOGGER.info(line);
            String row = line.trim();
            if (!row.equals("")) {
                String[] split = row.split("\\s+");
                JSONObject obj = new JSONObject();
                obj.put("PID", split[1]);
                obj.put("NAME", split[8]);
                result.add(obj);
            }
        }
        p.waitFor();
        //  sort the list
        result.sort(new ProcessComparator());
        return result;
    }

    /*
    * exit logcat (clear the entire log and exit)
     */
    public Process logProcess(String PID, Logger LOGGER, String userUuid) throws InterruptedException, IOException {
        clearLogcatBuffer(LOGGER, userUuid);

        LOGGER.info("Starting new logcat session...");
        if (PID.equals("ALL")) {
            return executeCommand(userUuid, LOGGER, adbExecutablePath, "shell", "logcat -v time");
            //executeCommand(userUuid, LOGGER, adbExecutablePath, "shell", "logcat -v threadtime");
        } else {
            return executeCommand(userUuid, LOGGER, adbExecutablePath, "shell", "logcat -v time | grep '" + PID + "'");
            //executeCommand(userUuid, LOGGER, adbExecutablePath, "shell", "logcat -v threadtime | grep '"+PID+"'");
        }
    }


    /*
    * exit logcat (clear the entire log and exit)
     */
    public Process logProcess(String PID, String logLevel, boolean clearLog, Logger LOGGER_, String userUuid) throws InterruptedException, IOException {
        if (clearLog) {
            clearLogcatBuffer(LOGGER_, userUuid);
            if (LOGGER_ != null) {
                LOGGER_.info("Starting new logcat session...");
            } else {
                LOGGER.info("Starting new logcat session...");
            }
        }
        if (PID.equals("ALL")) {
            return executeCommand(userUuid, LOGGER_, adbExecutablePath, "shell", "logcat -v time *:" + logLevel);
            //executeCommand(userUuid, LOGGER, adbExecutablePath, "shell", "logcat -v threadtime");
        } else {
            return executeCommand(userUuid, LOGGER_, adbExecutablePath, "shell", "logcat -v time *:" + logLevel + " | grep '" + PID + "'");
            //executeCommand(userUuid, LOGGER, adbExecutablePath, "shell", "logcat -v threadtime | grep '"+PID+"'");
        }
    }

    /*
    Clear all the previously recorded adb logs. So, the time you start adb logging again, it will start
    from the same time, without adding a repeated values from previous log
     */
    public Process clearLogcatBuffer(Logger LOGGER_, String userUuid) throws InterruptedException, IOException {
        if (LOGGER_ != null) {
            LOGGER_.info("Clearing log buffer...");
        } else {
            LOGGER.info("Clearing log buffer...");
        }
        return executeCommand(userUuid, LOGGER, adbExecutablePath, "shell", "logcat", "-c");
    }

    // TODO getdevice properties adb shell getpro (manufacturer, model,...)

    /*
    * install apk on emulator
     */
    public Process installApkOnEmulator(String apkPath, String userUuid) throws InterruptedException, IOException {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("*** ADB install on emulator ****");
        LOGGER.info("********************************");
        LOGGER.info(" ");
        /*
        * -e : directs command to the only running emulator; returns an error if more than one emulator is
        * -r : re-install the app if already installed
        */
        File apk = new File(apkPath);
        if (!apk.exists())
            throw new FileNotFoundException("Could not find path '" + apkPath + "'");
        if (apk.isFile()) {
            return executeCommand(userUuid, adbExecutablePath, "-e", "install", "-r", apkPath);
        } else if (apk.isDirectory()) {
            File[] content = apk.listFiles();
            if (content != null) {
                for (File file : content) {
                    if (file.getName().endsWith(".apk")) {
                        return executeCommand(userUuid, adbExecutablePath, "-e", "install", "-r", file.getPath());
                    }
                }
            }
        }
        return null;
    }

    public Process wireInstallApkOnDevice(String apkPath, String userUuid) throws InterruptedException, IOException, AdbExecutionException {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("** ADB wire install on device **");
        LOGGER.info("********************************");
        LOGGER.info(" ");
        executeCommandWithTimeout(userUuid, 20000, "Problem waiting for device timeout!",
                "Please connect your device to this computer via USB...",
                adbExecutablePath, "wait-for-any-device");
        /*
        * -d : directs command to the only connected USB device, or returns an error if more than one USB device is present.
        * -r : re-install the app if already installed
        */
        File apk = new File(apkPath);
        if (!apk.exists())
            throw new FileNotFoundException("Could not find path '" + apkPath + "'");
        if (apk.isFile()) {
            return executeCommand(userUuid, adbExecutablePath, "-d", "install", "-r", apkPath);
        } else if (apk.isDirectory()) {
            File[] content = apk.listFiles();
            if (content != null) {
                for (File file : content) {
                    if (file.getName().endsWith(".apk")) {
                        return executeCommand(userUuid, adbExecutablePath, "-d", "install", "-r", file.getPath());
                    }
                }
            }
        }
        return null;
    }

    /**
     * 1- Connect the device via USB and make sure debugging is working.
     * 2- adb tcpip 5555 : restarts the adbd daemon listening on TCP on the specified port
     * 3- find the IP address with adb shell netcfg
     * 4- adb connect <DEVICE_IP_ADDRESS>:5555
     * 5- Disconnect USB and proceed with wireless debugging.
     * 6- adb -s <DEVICE_IP_ADDRESS>:5555 usb to switch back when done
     *
     * @param apkPath
     */
    public Process wirelessInstallApkOnDevice(String apkPath, String userUuid) throws IOException, InterruptedException {
        LOGGER.info(" ");
        LOGGER.info("************************************");
        LOGGER.info("** ADB wireless install on device **");
        LOGGER.info("************************************");
        LOGGER.info(" ");
        /*
        * -r : re-install the app if already installed
        */
        File apk = new File(apkPath);
        if (!apk.exists())
            throw new FileNotFoundException("Could not find path '" + apkPath + "'");
        if (apk.isFile()) {
            return executeCommand(userUuid, adbExecutablePath, "install", "-r", apkPath);
        } else if (apk.isDirectory()) {
            File[] content = apk.listFiles();
            if (content != null) {
                for (File file : content) {
                    if (file.getName().endsWith(".apk")) {
                        return executeCommand(userUuid, adbExecutablePath, "install", "-r", file.getPath());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Execute a command and print output
     *
     * @param command command arguments to be executed
     */
    private Process executeCommand(String userUuid, String... command) throws InterruptedException, IOException {
        return executeCommand(userUuid, LOGGER, command);
    }

    /**
     * Execute a command and print output
     *
     * @param logger  the logger which will prints the output
     * @param command command arguments to be executed
     */
    private Process executeCommand(String userUuid, Logger logger, String... command) throws InterruptedException, IOException {
        Logger COMMAND_LOGGER;
        if (logger != null) {
            COMMAND_LOGGER = logger;
        } else {
            COMMAND_LOGGER = LOGGER;
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.equals("")) {
                COMMAND_LOGGER.debug(line);
            }
        }
        p.waitFor();

        if (p.isAlive())
            return p;
        else
            return null;
    }

    /**
     * Execute a command and print output, wait for process untill timeout is reached.
     * If process didn't finish execution before timeout => interrupt it and Throw
     * AdbExecutionException with a custom message
     *
     * @param timeout          timeout period to wait for process to finish execution
     * @param exceptionMessage Message to show
     * @param command          command arguments to be executed
     * @param periodMessage    a periodic message that will appear every seconds
     * @return
     * @throws IOException
     * @throws AdbExecutionException when process execution exceeds timeout
     */
    private int executeCommandWithTimeout(String userUuid, final long timeout, String exceptionMessage, String periodMessage, String... command)
            throws IOException, AdbExecutionException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);

        /* Set up process I/O. */
        Worker worker = new Worker(process, periodMessage, userUuid);
        worker.start();
        try {
            worker.join(timeout);
            if (worker.exit != null)
                return worker.exit;
            else {
                throw new AdbExecutionException(exceptionMessage);
            }
        } catch (InterruptedException ex) {
            worker.interrupt();
            //Thread.currentThread().interrupt();
            throw new AdbExecutionException(exceptionMessage);
        } finally {
            //process.destroyForcibly();
            worker.setStop();
        }
    }


    /**
     * Thread that executes a Runtime process with a timeout
     */
    private class Worker extends Thread {
        private final String userUuid;
        private final Process process;
        private Integer exit;
        private String periodMessage = null;
        private Timer timer;
        private WorkerPeriodicMsg printerTask;

        private Worker(Process process, String periodMessage, String userUuid) {
            this.process = process;
            this.periodMessage = periodMessage;
            this.userUuid = userUuid;
        }

        public void setStop() {
            if (timer != null && printerTask != null) {
                printerTask.setStop(true);
                timer.cancel();
                timer.purge();
            }
        }

        public void run() {
            try {
                if (periodMessage != null && !periodMessage.equals("")) {
                    timer = new Timer(true);
                    printerTask = new WorkerPeriodicMsg(periodMessage);
                    timer.schedule(printerTask, 1500, 5000);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    Thread.sleep(100);
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    LOGGER.info(line);
                }
                exit = process.waitFor();
            } catch (InterruptedException | IOException ignore) {
                process.destroyForcibly();
            } finally {
                if (timer != null && printerTask != null) {
                    printerTask.setStop(true);
                    timer.cancel();
                    timer.purge();
                }
            }
        }
    }

    /**
     *
     */
    class WorkerPeriodicMsg extends TimerTask {
        private String message;
        private boolean stop = false;

        WorkerPeriodicMsg(String message) {
            this.message = message;
        }

        public void setStop(boolean stop) {
            this.stop = stop;
        }

        public void run() {
            if (stop) {
                return;
            }
            LOGGER.info(message);
        }
    }

    private class ProcessComparator implements Comparator<JSONObject> {
        @Override
        public int compare(JSONObject o1, JSONObject o2) {
            return ((String) o1.get("NAME")).compareTo((String) o2.get("NAME"));
        }
    }
}
