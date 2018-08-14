package com.ninjaflip.androidrevenge.core.apktool;

import brut.androlib.Androlib;
import brut.androlib.AndrolibException;
import brut.androlib.ApkDecoder;
import brut.androlib.ApkOptions;
import brut.androlib.err.CantFindFrameworkResException;
import brut.androlib.err.InFileNotFoundException;
import brut.androlib.err.OutDirExistsException;
import brut.androlib.meta.MetaInfo;
import brut.common.BrutException;
import brut.directory.DirectoryException;
import com.ninjaflip.androidrevenge.beans.ApkToolProjectBean;
import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.apktool.apkinfo.AnalysisApk;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.core.apktool.signzipalign.SignTool;
import com.ninjaflip.androidrevenge.core.apktool.updater.MassiveFileRenamer;
import com.ninjaflip.androidrevenge.core.apktool.updater.PackageNameChanger;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import com.ninjaflip.androidrevenge.core.service.SparkServerManager;
import com.ninjaflip.androidrevenge.enums.EnumerationApkTool;
import com.ninjaflip.androidrevenge.exceptions.XmlPullParserException;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;

import javax.imageio.ImageIO;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;


/**
 * Created by Solitario on 17/05/2017.
 * <p>
 * this class wraps all operations related to Apktool jar library
 */
public class ApkToolsManager {
    private final static Logger LOGGER = Logger.getLogger(ApkToolsManager.class);
    private static ApkToolsManager INSTANCE;
    private static JSONArray availableAdNetworks;
    private static String[] excludedPackageNames;// contains the packages that can't be renamed by 'Manifest Entries Renamer' tool

    static {
        InputStream stream = null;
        try {
            stream = Utils.class.getClassLoader().getResourceAsStream("adnetworks.json");
            availableAdNetworks = (JSONArray) JSONValue.parse(Utils.convertStreamToString(stream));
        } catch (Exception e) {
            // do nothing
        }finally {
            IOUtils.closeQuietly(stream);
        }

        try {
            List<String> excludedPackageNamesList = new ArrayList<>();
            excludedPackageNamesList.add("com.google.");

            if (availableAdNetworks != null && availableAdNetworks.size() > 0) {
                for (Object an : availableAdNetworks) {
                    String networkPackageNamePrefix = (String) ((JSONObject) an).get("pn_prefix");
                    excludedPackageNamesList.add(networkPackageNamePrefix);
                }
            }

            excludedPackageNames = excludedPackageNamesList.toArray(new String[excludedPackageNamesList.size()]);
        }catch (Exception e){
            List<String> excludedPackageNamesList = new ArrayList<>();
            excludedPackageNamesList.add("com.google.");
            excludedPackageNames = excludedPackageNamesList.toArray(new String[excludedPackageNamesList.size()]);
        }
    }


    private ApkToolsManager() {
    }

    public static ApkToolsManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ApkToolsManager();
        }
        return INSTANCE;
    }

    public static JSONArray getAvailableAdNetworks() {
        return availableAdNetworks;
    }

    /**
     * Decode apk file (apk to smali)
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           true if the project is a temporary project (benchmark test of an apk), false if real project
     * @param apkFile               target apk file to be decoded
     */
    public void decodeApk(String userUuid, String projectFolderNameUuid, boolean isTemporary,
                          File apkFile) throws Exception {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("*****   Decoding APK       *****");
        LOGGER.info("********************************");
        LOGGER.info(" ");

        LOGGER.info("Decoding Apk file '" + apkFile.getPath() + "'...");
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();
        ApkDecoder decoder = new ApkDecoder();
        decoder.setApkFile(apkFile);
        String decodedApkFolderFullPath = Configurator.getInstance().getDecodedApkFolderPath(userUuid, projectFolderNameUuid, isTemporary);
        try {
            decoder.setForceDelete(true);
            decoder.setOutDir(new File(decodedApkFolderFullPath));
            decoder.decode();
            timer.end();
            LOGGER.info("---> Apk decoded to folder '" + decodedApkFolderFullPath + "' in " + timer.durationInSeconds() + " seconds");
        /*} catch (OutDirExistsException var6) {
            //LOGGER.error(ExceptionUtils.getStackTrace(var6));
            LOGGER.error("Destination directory (" + decodedApkFolderFullPath + ") " + "already exists.");
        } catch (InFileNotFoundException var7) {
            //LOGGER.error(ExceptionUtils.getStackTrace(var7));
            LOGGER.error("Input file (" + apkFile.getName() + ") " + "was not found or was not readable.");
        } catch (CantFindFrameworkResException var8) {
            //LOGGER.error(ExceptionUtils.getStackTrace(var8));
            LOGGER.error("Can't find framework resources for package of id: " + String.valueOf(var8.getPkgId())
                    + ". You must install proper " + "framework files, see project website for more info.");
        } catch (IOException var9) {
            //LOGGER.error(ExceptionUtils.getStackTrace(var9));
            LOGGER.error("Could not modify file. Please ensure you have permission.");
        } catch (DirectoryException var10) {
            //LOGGER.error(ExceptionUtils.getStackTrace(var10));
            LOGGER.error("Could not modify internal dex files. Please ensure you have permission.");
        } catch (AndrolibException e) {
            LOGGER.error(e.getMessage());
            //LOGGER.error(ExceptionUtils.getStackTrace(e));
            throw e;
        } finally {
            System.gc();
        }*/
        } catch (Exception e) {
            //LOGGER.error(ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    /**
     * Build apk file from decompiled smali code and stream execution output to a web-socket
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     *                              apk will be created in /projectRootFolderPath/decoded/dist/ folder
     * @param isTemporary           true if the project is a temporary project (benchmark test of an apk), false if real project
     * @throws BrutException if an exception happen during apk build process
     */
    public void buildApk(String userUuid, String projectFolderNameUuid, boolean isTemporary) throws Exception {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("*****   Building APK       *****");
        LOGGER.info("********************************");
        LOGGER.info(" ");

        LOGGER.info("Building apk file...");
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();
        //File outFile;
        try {
            ApkOptions apkOptions = new ApkOptions();
            // force-all
            apkOptions.forceBuildAll = true;
            // debug
            //apkOptions.debugMode = true;
            // verbose
            //apkOptions.verbose = true;
            // copy-original
            //apkOptions.copyOriginalFiles = true;
            // delete framework
            apkOptions.forceDeleteFramework = true;

            /*if (apkOutputFilePath == null || apkOutputFilePath.equals("")) {
                outFile = null;
            } else {
                outFile = new File(apkOutputFilePath);
            }*/
            String decodedApkFolderFullPath = Configurator.getInstance().getDecodedApkFolderPath(userUuid, projectFolderNameUuid, isTemporary);
            (new Androlib(apkOptions)).build(new File(decodedApkFolderFullPath), null);
            timer.end();
            LOGGER.info("---> Apk-build task finished in " + timer.durationInSeconds() + " seconds");
        } catch (Exception e) {
            //LOGGER.error(ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    /**
     * Change project's package name, this method will change all occurrences of old package name and its subpackages
     * within project folder and will also update all files and folders hierarchy to fit the new package name.
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           true if the project is a temporary project (benchmark test of an apk), false if real project
     * @param newPackageName        the new package name
     * @throws ScriptException
     * @throws IOException
     * @throws XPathExpressionException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public void updatePackageName(String userUuid, String projectFolderNameUuid, boolean isTemporary,
                                  String newPackageName) throws Exception {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("***  Updating Package Name  ****");
        LOGGER.info("********************************");
        LOGGER.info(" ");

        String decodedApkFolderPath = Configurator.getInstance().getDecodedApkFolderPath(userUuid, projectFolderNameUuid, isTemporary);
        String oldPackageName = ApkToolsManager.getInstance().getPackageNameFromManifest(decodedApkFolderPath);

        LOGGER.info("Changing package name from '" + oldPackageName + "' to '" + newPackageName + "'...");
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();

        PackageNameChanger packageNameChanger = new PackageNameChanger(userUuid, projectFolderNameUuid, isTemporary, oldPackageName,
                newPackageName);
        packageNameChanger.changePackageName();
        timer.end();
        LOGGER.info("---> Package name changed from '" + oldPackageName
                + "' to '" + newPackageName + "' in " + timer.durationInSeconds() + " seconds");
    }

    /**
     * Rename manifest entries
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           true if the project is a temporary project (benchmark test of an apk), false if real project
     * @param customNames           a map containing Key: className(packageName.classSimpleName), value: newClassSimpleName
     *                              Thus, "packageName.classSimpleName" will be renamed to "packageName.newClassSimpleName"
     * @throws ScriptException
     * @throws IOException
     * @throws XPathExpressionException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public void renameManifestEntries(String userUuid, String projectFolderNameUuid, boolean isTemporary, Map<String,
            String> customNames) throws Exception,
            XPathExpressionException, SAXException, ParserConfigurationException {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("*** Renaming Manifest Entries **");
        LOGGER.info("********************************");
        LOGGER.info(" ");

        ExecutionTimer timer = new ExecutionTimer();
        timer.start();

        MassiveFileRenamer massiveFileRenamer = new MassiveFileRenamer(userUuid, projectFolderNameUuid, isTemporary);
        if (customNames == null) {
            massiveFileRenamer.renameAll();
        } else {
            massiveFileRenamer.renameAll(customNames);
        }
        timer.end();
        LOGGER.info("---> Manifest Entries Renamer done in " + timer.durationInSeconds() + " seconds");
    }


    /**
     * Rename a package
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           true if the project is a temporary project (benchmark test of an apk), false if real project
     * @param existingPackageName   the name of the original package
     * @param newPackageName        the new package name
     * @throws ScriptException
     * @throws IOException
     * @throws XPathExpressionException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public void renamePackage(String userUuid, String projectFolderNameUuid, boolean isTemporary,
                              String existingPackageName, String newPackageName) throws Exception {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("*****  Renaming Package  *******");
        LOGGER.info("********************************");
        LOGGER.info(" ");

        LOGGER.info("Renaming package from '" + existingPackageName + "' to '" + newPackageName + "'...");
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();

        PackageNameChanger packageNameChanger = new PackageNameChanger(userUuid, projectFolderNameUuid, isTemporary, existingPackageName,
                newPackageName);
        packageNameChanger.changePackageName();
        timer.end();
        LOGGER.info("---> Package renamed from '" + existingPackageName
                + "' to '" + newPackageName + "' in " + timer.durationInSeconds() + " seconds");
    }


    /**
     * Rename smali files to make apk recognition task harder.
     * New file names are randomly generated
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           true if the project is a temporary project (benchmark test of an apk), false if real project
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws ScriptException
     * @throws SAXException
     * @throws XPathExpressionException
     */
    public void renameFiles(String userUuid, String projectFolderNameUuid, boolean isTemporary)
            throws Exception,
            SAXException, ParserConfigurationException {
        MassiveFileRenamer massiveFileRenamer = new MassiveFileRenamer(userUuid, projectFolderNameUuid, isTemporary);
        massiveFileRenamer.renameAll();
    }

    /**
     * Rename smali files to make apk recognition task harder. using custom names entered by the user
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           true if the project is a temporary project (benchmark test of an apk), false if real project
     * @param customNames           a map containing replacement names of application tags present in the manifest (activities, services, receivers)
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws ScriptException
     * @throws SAXException
     * @throws XPathExpressionException
     */
    public void renameFiles(String userUuid, String projectFolderNameUuid, boolean isTemporary, Map<String, String> customNames)
            throws Exception {
        MassiveFileRenamer massiveFileRenamer = new MassiveFileRenamer(userUuid, projectFolderNameUuid, isTemporary);
        massiveFileRenamer.renameAll(customNames);
    }

    /**
     * Get Android manifest file of a decompiled app from its root folder
     * in work directory /user/.android_revenge/workfolder/projectRootFolderName
     *
     * @param decodedApkFolderPath path to directory containing decompiled smali code
     * @return AndroidManifest.xml file
     * @throws FileNotFoundException    if root folder not found or AndroidManifest.xml file not found
     * @throws IllegalArgumentException if root folder is not a directory
     */
    public File getAndroidManifestFile(String decodedApkFolderPath) throws FileNotFoundException, IllegalArgumentException {
        File rootFolder = new File(decodedApkFolderPath);

        if (!rootFolder.exists())
            throw new FileNotFoundException("The folder " + decodedApkFolderPath + "does not exist!");

        if (!rootFolder.isDirectory())
            throw new IllegalArgumentException("File " + decodedApkFolderPath + "is not a directory!");

        File[] files = rootFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().equals("AndroidManifest.xml")) {
                    return f;
                }
            }
        }
        throw new FileNotFoundException("AndroidManifest.xml file not found!");
    }

    /**
     * Get application package name from manifest file
     *
     * @param decodedApkFolderPath path to directory containing decompiled smali code
     * @return application package name
     * @throws ParserConfigurationException when parsing error
     * @throws IOException                  if file not found
     * @throws SAXException                 when parsing error
     * @throws XPathExpressionException     when parsing error
     */
    public String getPackageNameFromManifest(String decodedApkFolderPath) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        File androidManifestFile = getAndroidManifestFile(decodedApkFolderPath);
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(androidManifestFile);

        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xPath.evaluate("//manifest/@package", doc, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node != null) {
                //System.out.println(node.getTextContent());
                return node.getTextContent();
            }
        }
        return null;
    }

    /**
     * Get apktool.yml file from root folder
     *
     * @param projectRootFolderPath path to folder containing decoded apk file, generated by Apktool
     * @return apktool.yml file
     * @throws FileNotFoundException    if rootFolder not  found or apktool.yml not found
     * @throws IllegalArgumentException if rootFolder is not a directory
     */
    private File getApktoolYamlFile(String projectRootFolderPath) throws FileNotFoundException, IllegalArgumentException {
        File rootFolder = new File(projectRootFolderPath);

        if (!rootFolder.exists())
            throw new FileNotFoundException("The folder " + projectRootFolderPath + "does not exist!");

        if (!rootFolder.isDirectory())
            throw new IllegalArgumentException("File " + projectRootFolderPath + "is not a directory!");

        File[] files = rootFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().equals("apktool.yml")) {
                    return f;
                }
            }
        }
        throw new FileNotFoundException("apktool.yml file not found!");
    }

    /**
     * Get application info directly from apk file
     *
     * @param apkPath path to apk file
     * @return Map containing all non null info about the apk
     * package : package name
     * versionCode: version code
     * versionName: version name
     * ic_launcher: icon relative path
     * permissions : list of permissions
     * minSdkVersion: min sdk version
     * targetSdkVersion: target sdk
     * maxSdkVersion: max sdk version
     * packageActivities: list of activities that belongs the the app
     * foreignActivities: list of dependencies activities (ex: google ads, facebook analytics,...)
     * services: list of services
     * receivers: list of receivers
     * assets: list of assets (apk resources relative paths paths)
     * adNetworks: list of integrated ad networks inside this apk
     * @throws IOException
     */
    public Map<String, Object> getAppInfoFromApk(String apkPath) throws IOException {
        return AnalysisApk.unZip(apkPath);
    }

    /**
     * Get application info directly from apk file and put them in a pretty string ready for print
     *
     * @param info       Map containing all non null info about the apk
     * @param showAssets if set to true, it will print all paths of apk zipped files. if false, will do nothing
     * @return a pretty printable string containing all apk info
     * @throws IOException
     */
    public String getSummaryFromAppInfo(Map<String, Object> info, boolean showAssets) throws IOException {

        String apkPath = (String) info.get("apkPath");
        String apkSize = String.valueOf(info.get("size_in_MB"));
        String packageName = (String) info.get("package");
        String versionCode = (String) info.get("versionCode");
        String versionName = (String) info.get("versionName");

        String minSdkVersion = (String) info.get("minSdkVersion");
        String targetSdkVersion = (String) info.get("targetSdkVersion");
        String maxSdkVersion = (String) info.get("maxSdkVersion");

        String ic_launcher = (String) info.get("ic_launcher");

        String summary = "APK INFO FOR ===> " + apkPath + "\n";
        //summary += "APK SIZE =======> " + apkSize + " Mb\n";
        summary += "\tapk size " + apkSize + " Mb\n";
        summary += "\tpackage '" + packageName + "'\n";
        summary += "\tversion\n";
        summary += "\t\tversionCode '" + versionCode + "'\n";
        summary += "\t\tversionName '" + versionName + "'\n";
        summary += "\tsdk\n";
        summary += "\t\tminSdkVersion '" + minSdkVersion + "'\n";
        summary += "\t\ttargetSdkVersion '" + targetSdkVersion + "'\n";
        if (maxSdkVersion != null)
            summary += "\t\tmaxSdkVersion '" + maxSdkVersion + "'\n";

        if (ic_launcher != null)
            summary += "\tic_launcher '" + ic_launcher + "'\n";

        // Permissions
        List<String> permissions = (List<String>) info.get("permissions");
        if (permissions != null) {
            summary += "\t" + permissions.size() + " permissions\n";
            StringBuilder perms = new StringBuilder("");
            for (String perms_ : permissions) {
                perms.append("\t\t" + perms_ + "\n");
            }
            summary += perms.toString();
        }

        // Activities
        List<String> packageActivities = (List<String>) info.get("packageActivities");
        if (packageActivities != null) {
            summary += "\t" + packageActivities.size() + " activities\n";
            StringBuilder pckgAct = new StringBuilder("");
            for (String pckgAct_ : packageActivities) {
                pckgAct.append("\t\t" + pckgAct_ + "\n");
            }
            summary += pckgAct.toString();
        }

        // Foreign activities
        List<String> foreignActivities = (List<String>) info.get("foreignActivities");
        if (foreignActivities != null) {
            summary += "\t" + foreignActivities.size() + " other activities\n";
            StringBuilder frgAct = new StringBuilder("");
            for (String frgAct_ : foreignActivities) {
                frgAct.append("\t\t" + frgAct_ + "\n");
            }
            summary += frgAct.toString();
        }

        // Package Services
        List<String> packageServices = (List<String>) info.get("packageServices");
        if (packageServices != null) {
            summary += "\t" + packageServices.size() + " services\n";
            StringBuilder serv = new StringBuilder("");
            for (String serv_ : packageServices) {
                serv.append("\t\t" + serv_ + "\n");
            }
            summary += serv.toString();
        }

        // Foreign Services
        List<String> foreignServices = (List<String>) info.get("foreignServices");
        if (foreignServices != null) {
            summary += "\t" + foreignServices.size() + " other services\n";
            StringBuilder serv = new StringBuilder("");
            for (String serv_ : foreignServices) {
                serv.append("\t\t" + serv_ + "\n");
            }
            summary += serv.toString();
        }

        // Receivers
        List<String> packageReceivers = (List<String>) info.get("packageReceivers");
        if (packageReceivers != null) {
            summary += "\t" + packageReceivers.size() + " receivers\n";
            StringBuilder recvr = new StringBuilder("");
            for (String recvr_ : packageReceivers) {
                recvr.append("\t\t" + recvr_ + "\n");
            }
            summary += recvr.toString();
        }

        // Foreign Receivers
        List<String> foreignReceivers = (List<String>) info.get("foreignReceivers");
        if (foreignReceivers != null) {
            summary += "\t" + foreignReceivers.size() + " other receivers\n";
            StringBuilder recvr = new StringBuilder("");
            for (String recvr_ : foreignReceivers) {
                recvr.append("\t\t" + recvr_ + "\n");
            }
            summary += recvr.toString();
        }

        // assets
        if (showAssets) {
            List<String> assets = (List<String>) info.get("assets");
            if (assets != null) {
                summary += "\t" + assets.size() + " assets\n";
                StringBuilder asts = new StringBuilder("");
                for (String asts_ : assets) {
                    asts.append("\t\t" + asts_ + "\n");
                }
                summary += asts.toString();
            }
        }
        return summary;
    }

    /**
     * Get application info name from apktool.yml file
     *
     * @param decodedApkFolderPath path to folder containing decompiled apk files, generated by apktool
     * @return a map containing versionCode, versionName, minSdkVersion, targetSdkVersion, apkFileName
     * @throws FileNotFoundException if yaml file not found
     */
    public Map<String, String> getAppInfoFromYaml(String decodedApkFolderPath) throws FileNotFoundException {
        File apktoolYamlFile = getApktoolYamlFile(decodedApkFolderPath);
        Map<String, String> appInfo = new HashMap<String, String>();
        Yaml yaml = new Yaml();
        InputStream is = new FileInputStream(apktoolYamlFile);
        MetaInfo values = (MetaInfo) yaml.load(is);
        //System.out.println(yaml.dump(yaml.load(new FileInputStream(apktoolYamlFile))));

        try {
            appInfo.put("versionCode", values.versionInfo.versionCode);
        } catch (Exception e1) {
            //Do nothing
        }
        try {
            appInfo.put("versionName", values.versionInfo.versionName);
        } catch (Exception e2) {
            //Do nothing
        }
        try {
            appInfo.put("minSdkVersion", values.sdkInfo.get("minSdkVersion"));
        } catch (Exception e3) {
            //Do nothing}
        }
        try {
            appInfo.put("targetSdkVersion", values.sdkInfo.get("targetSdkVersion"));
        } catch (Exception e4) {
            //Do nothing}
        }
        try {
            appInfo.put("apkFileName", values.apkFileName);
        } catch (Exception e5) {
            //Do nothing}}
        }

        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return appInfo;
    }

    // update versionCode, versionName, minSdkVersion, targetSdkVersion in apktool.yml file
    public void updateAppInfoInYaml(String decodedApkFolderPath, String newVersionCode, String newVersionName,
                                    String newMinSdkVersion, String newTargetSdkVersion) throws IOException {
        File apktoolYamlFile = getApktoolYamlFile(decodedApkFolderPath);
        Yaml yaml = new Yaml();
        //System.out.println(yaml.dump(yaml.load(new FileInputStream(apktoolYamlFile))));
        InputStream is = new FileInputStream(apktoolYamlFile);


        MetaInfo values = (MetaInfo) yaml.load(is);

        if (newVersionCode != null)
            values.versionInfo.versionCode = newVersionCode;

        if (newVersionName != null)
            values.versionInfo.versionName = newVersionName;

        if (newMinSdkVersion != null)
            values.sdkInfo.put("minSdkVersion", newMinSdkVersion);

        if (newTargetSdkVersion != null)
            values.sdkInfo.put("targetSdkVersion", newTargetSdkVersion);

        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Yaml newYaml = new Yaml();
        FileWriter writer = new FileWriter(apktoolYamlFile.getPath());
        newYaml.dump(values, writer);
        writer.close();

    }

    // update only apk file name in apktool.yml file
    public void updateAppInfoWithNewApkFileNameInYaml(String decodedApkFolderPath, String newApkFileName) throws IOException {
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(new InterruptedException("APKTool thread was aborted abnormally!"));
        }
        File apktoolYamlFile = getApktoolYamlFile(decodedApkFolderPath);
        Yaml yaml = new Yaml();
        //System.out.println(yaml.dump(yaml.load(new FileInputStream(apktoolYamlFile))));
        InputStream is = new FileInputStream(apktoolYamlFile);
        MetaInfo values = (MetaInfo) yaml.load(is);

        values.apkFileName = newApkFileName;

        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Yaml newYaml = new Yaml();
        FileWriter writer = new FileWriter(apktoolYamlFile.getPath());
        newYaml.dump(values, writer);
        writer.close();
    }

    /**
     * Sign an apk and zipalign it using release keystore
     *
     * @param targetApkFileOrFolderPath path to apk file or folder containing one or multiple apk
     * @param apkDestFolderPath         path to folder where the signed apk will be created
     * @param ksFilePath                path to keystore file
     * @param ksAlias                   keystore alias
     * @param ksPass                    keystore password
     * @param ksKeyPass                 keystore key password
     * @param verbose                   Prints more output, especially useful for sign verify.
     */
    public void signAndZipAlignApkForRelease(String targetApkFileOrFolderPath, String apkDestFolderPath,
                                             String ksFilePath, String ksAlias, String ksPass, String ksKeyPass,
                                             boolean verbose) throws Exception {
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(new InterruptedException("APKTool thread was aborted abnormally!"));
        }
        LOGGER.info(" ");
        LOGGER.info("**************************************");
        LOGGER.info("****  Sign/ZipAlign Release APK  *****");
        LOGGER.info("**************************************");
        LOGGER.info(" ");

        String[] tmpArgs = {"-a", targetApkFileOrFolderPath,
                "-o", apkDestFolderPath,
                "--allowResign",
                "--ks", ksFilePath,
                "--ksAlias", ksAlias,
                "--ksPass", ksPass,
                "--ksKeyPass", ksKeyPass
        };
        List<String> myArgsList = new LinkedList<>(Arrays.asList(tmpArgs));
        if (verbose) {
            myArgsList.add("--verbose");
        }

        String[] myArgs = new String[myArgsList.size()];
        myArgs = myArgsList.toArray(myArgs);

        SignTool.Result result = SignTool.mainExecute(myArgs);
        if (result != null && result.error) {
            throw new Exception("Failed to sign Apk for release!");
        } else if (result != null && result.unsuccessful > 0) {
            throw new Exception("Unsuccessful signing of Apk for release!");
        }
    }

    /**
     * Sign an apk and zipalign it using debug keystore.
     * will try to sign with a debug keystore.
     * The debug keystore will be searched in the same dir as execution
     * and 'user_home/.android' folder. If it is not found there a
     * built-in keystore will be used for convenience.
     *
     * @param targetApkFileOrFolderPath path to apk file or folder containing one or multiple apk
     * @param apkDestFolderPath         path to folder where the signed apk will be created
     * @param verbose                   Prints more output, especially useful for sign verify
     */
    public void signAndZipAlignApkForDebug(String targetApkFileOrFolderPath, String apkDestFolderPath, boolean verbose) throws Exception {
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(new InterruptedException("APKTool thread was aborted abnormally!"));
        }
        LOGGER.info(" ");
        LOGGER.info("**************************************");
        LOGGER.info("*****   Sign/ZipAlign Debug APK  *****");
        LOGGER.info("**************************************");
        LOGGER.info(" ");

        String[] tmpArgs = {"-a", targetApkFileOrFolderPath,
                "-o", apkDestFolderPath,
                "--allowResign"
        };
        List<String> myArgsList = new LinkedList<>(Arrays.asList(tmpArgs));
        if (verbose) {
            myArgsList.add("--verbose");
        }

        String[] myArgs = new String[myArgsList.size()];
        myArgs = myArgsList.toArray(myArgs);


        SignTool.Result result = SignTool.mainExecute(myArgs);
        if (result != null && result.error) {
            throw new Exception("Failed to sign Apk for debug!");
        } else if (result != null && result.unsuccessful > 0) {
            throw new Exception("Unsuccessful signing of Apk for debug!");
        }
    }


    /**
     * Only verify the signature and alignment of one or multiple Apk(s)
     *
     * @param targetApkFileOrFolderPath path to apk file or folder containing one or multiple apk
     * @param verbose                   Prints more output, especially useful for sign verify.
     */
    public void verifyApk(String targetApkFileOrFolderPath, boolean verbose) throws Exception {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("**  Verifying APK Signature  ***");
        LOGGER.info("********************************");
        LOGGER.info(" ");

        String[] tmpArgs = {"-a", targetApkFileOrFolderPath,
                "--onlyVerify"
        };
        List<String> myArgsList = new LinkedList<>(Arrays.asList(tmpArgs));
        if (verbose) {
            myArgsList.add("--verbose");
        }
        String[] myArgs = new String[myArgsList.size()];
        myArgs = myArgsList.toArray(myArgs);

        SignTool.Result result = SignTool.mainExecute(myArgs);
        if (result != null && result.error) {
            throw new Exception("Failed to verify Apk!");
        } else if (result != null && result.unsuccessful > 0) {
            throw new Exception("Unsuccessful verification of this Apk!");
        }
    }


    /**
     * This method return app info to populate the apk-ready-template
     *
     * @param pathToFolderContainingDebugApk absolute path to folder ccontaining the debug apk file
     * @return a map containing apk info (url, build type, icon bytes, size in mega bytes)
     * @throws IOException
     */
    public Map<String, Object> getDebugApkReadyInfo(String pathToFolderContainingDebugApk) throws IOException {
        Map<String, Object> result = new HashMap<>();
        File folder = new File(pathToFolderContainingDebugApk);
        if (!folder.exists())
            throw new IllegalArgumentException("Folder '" + pathToFolderContainingDebugApk + "' not fount!");
        if (!folder.isDirectory())
            throw new IllegalArgumentException("'" + pathToFolderContainingDebugApk + "' is not a folder!");

        File[] files = folder.listFiles();
        if (files == null)
            throw new IllegalArgumentException("Folder '" + pathToFolderContainingDebugApk + "' is empty!");
        else {
            for (File file : files) {
                String fileName = file.getName();
                // we got the rat
                if (fileName.contains("-debugSigned")) {
                    // build url from file path
                    String url = SparkServerManager.getStaticFileUrlFromPath_localIp(file.getPath());
                    result.put("url", url);
                    // other info
                    Map<String, Object> info = ApkToolsManager.getInstance().getAppInfoFromApk(file.getPath());
                    result.put("buildType", EnumerationApkTool.EnumBuildType.DEBUG);
                    result.put("packageName", info.get("package"));
                    result.put("iconBase64", info.get("ic_launcher_bytes_resized"));
                    result.put("size_in_MB", info.get("size_in_MB"));
                    result.put("filePath", file.getPath());
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * This method returns all project info.
     * This method is called when the user clicks on 'Project info' button in the project editor toolbar
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @return a json object containing all information about the project (date created, app name, package name, version, sdk min/max, permissions,
     * activities (foreign activities), launcher activity, services (foreign services), receivers (foreign receivers), app icons...)
     */
    public JSONObject getProjectInfoForProjectEditor(String userUuid, String projectFolderNameUuid, Graph graph)
            throws ParserConfigurationException, SAXException, XPathExpressionException, IOException, ScriptException, XmlPullParserException {

        JSONObject info = new JSONObject();
        // path to /workfolder/users/userUuid/projectFolderNameUuid/decoded/
        String decodedApkFolderPath = Configurator.getInstance().getDecodedApkFolderPath(userUuid, projectFolderNameUuid, false);
        File folder = new File(decodedApkFolderPath);
        if (!folder.exists())
            throw new IllegalArgumentException("Folder '" + folder.getPath() + "' not fount!");
        if (!folder.isDirectory())
            throw new IllegalArgumentException("'" + folder.getPath() + "' is not a folder!");

        File[] files = folder.listFiles();
        if (files == null || files.length == 0)
            throw new IllegalArgumentException("Folder '" + folder.getPath() + "' is empty!");
        else { // parse the AndroidManifest.xml and get all project data from it

            // get the manifest file
            File androidManifestFile = getAndroidManifestFile(decodedApkFolderPath);
            if (!androidManifestFile.exists())
                throw new IllegalArgumentException("AndroidManifest.xml not fount!");

            // variables that will contain the results
            String packageName = null;
            String appLabel = null;
            String appIcon = null;
            String launcherActivity = null;


            List<JSONObject> appNames = new ArrayList<JSONObject>();
            List<JSONObject> appIcons = new ArrayList<JSONObject>();

            List<String> listPermission = new ArrayList<String>();
            List<String> packageActivities = new ArrayList<String>();
            List<String> foreignActivities = new ArrayList<String>();
            List<String> packageServices = new ArrayList<String>();
            List<String> foreignServices = new ArrayList<String>();
            List<String> packageReceivers = new ArrayList<String>();
            List<String> foreignReceivers = new ArrayList<String>();
            List<JSONObject> adNetworks = new ArrayList<>();


            // start parsing AndroidManifest.xml
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            //Document doc = dBuilder.parse(androidManifestFile);
            Path path = Paths.get(androidManifestFile.getPath());
            ByteArrayInputStream bais = new ByteArrayInputStream(Files.readAllBytes(path));
            InputSource is = new InputSource(bais);
            is.setEncoding("UTF-8");
            //is.setEncoding("ISO-8859-1");
            Document doc = dBuilder.parse(is);
            try {
                bais.close();
            } catch (Exception e) {
                e.printStackTrace();
            }


            doc.getDocumentElement().normalize();

            Element manifestRootElement = doc.getDocumentElement();

            packageName = manifestRootElement.getAttribute("package");

            // application icon and name
            NodeList application = doc.getElementsByTagName("application");
            if (application != null && application.getLength() == 1) {
                if (application.item(0).getNodeType() == Node.ELEMENT_NODE) {
                    Element applicationElement = (Element) application.item(0);
                    appLabel = applicationElement.getAttribute("android:label");
                    appIcon = applicationElement.getAttribute("android:icon");
                }
            }

            // permissions
            NodeList permissionList = doc.getElementsByTagName("uses-permission");
            for (int i = 0; i < permissionList.getLength(); i++) {
                Node nNode = permissionList.item(i);
                //System.out.println("\nCurrent Element :" + nNode.getNodeName());
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String permissionName = eElement.getAttribute("android:name");
                    listPermission.add(permissionName);
                }
            }

            // activities
            NodeList activitiesList = doc.getElementsByTagName("activity");
            boolean foundLauncherActivity = false;

            for (int i = 0; i < activitiesList.getLength(); i++) {
                Node nNode = activitiesList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String activityName = eElement.getAttribute("android:name");

                    if (activityName != null && packageName != null) {
                        if (activityName.startsWith("."))
                            packageActivities.add(packageName + activityName);
                        else if (activityName.startsWith(packageName))
                            packageActivities.add(activityName);
                        else if (!activityName.contains("."))
                            packageActivities.add(packageName + "." + activityName);
                        else
                            foreignActivities.add(activityName);
                    }

                    NodeList intentFilter = eElement
                            .getElementsByTagName("intent-filter");

                    if (!foundLauncherActivity) {
                        if (intentFilter != null && intentFilter.getLength() > 0) {
                            for (int k = 0; k < intentFilter.getLength(); k++) {
                                if (intentFilter.item(k).getNodeType() == Node.ELEMENT_NODE) {
                                    Element iElement = (Element) intentFilter.item(k);
                                    NodeList category = iElement
                                            .getElementsByTagName("category");
                                    if (category != null && category.getLength() > 0) {
                                        for (int j = 0; j < category.getLength(); j++) {
                                            if (category.item(j).getNodeType() == Node.ELEMENT_NODE) {
                                                Element catElement = (Element) category.item(j);
                                                String categoryName = catElement.getAttribute("android:name");
                                                if (categoryName.equals("android.intent.category.LAUNCHER")) {
                                                    foundLauncherActivity = true;
                                                    if (activityName != null && activityName.startsWith("."))
                                                        launcherActivity = packageName + activityName;
                                                    else if (activityName != null && !activityName.contains("."))
                                                        launcherActivity = packageName + "." + activityName;
                                                    else
                                                        launcherActivity = activityName;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // services
            NodeList servicesList = doc.getElementsByTagName("service");

            for (int i = 0; i < servicesList.getLength(); i++) {
                Node nNode = servicesList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String serviceName = eElement.getAttribute("android:name");

                    if (serviceName != null && packageName != null) {
                        if (serviceName.startsWith("."))
                            packageServices.add(packageName + serviceName);
                        else if (serviceName.startsWith(packageName))
                            packageServices.add(serviceName);
                        else if (!serviceName.contains("."))
                            packageServices.add(packageName + "." + serviceName);
                        else
                            foreignServices.add(serviceName);
                    }
                }
            }

            // receivers
            NodeList receiverList = doc.getElementsByTagName("receiver");

            for (int i = 0; i < receiverList.getLength(); i++) {
                Node nNode = receiverList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String receiverName = eElement.getAttribute("android:name");

                    if (receiverName != null && packageName != null) {
                        if (receiverName.startsWith("."))
                            packageReceivers.add(packageName + receiverName);
                        else if (receiverName.startsWith(packageName))
                            packageReceivers.add(receiverName);
                        else if (!receiverName.contains("."))
                            packageReceivers.add(packageName + "." + receiverName);
                        else
                            foreignReceivers.add(receiverName);
                    }
                }
            }


            // populate the result object and send it back to the client

            if (packageName != null) {
                info.put("package", packageName);
            }

            if (listPermission.size() > 0) {
                info.put("permissions", listPermission);
            }

            if (packageActivities.size() > 0) {
                info.put("packageActivities", packageActivities);
            }

            if (launcherActivity != null) {
                info.put("launcherActivity", launcherActivity);
            }

            if (foreignActivities.size() > 0) {
                info.put("foreignActivities", foreignActivities);
            }

            if (packageServices.size() > 0) {
                info.put("packageServices", packageServices);
            }

            if (foreignServices.size() > 0) {
                info.put("foreignServices", foreignServices);
            }

            if (packageReceivers.size() > 0) {
                info.put("packageReceivers", packageReceivers);
            }

            if (foreignReceivers.size() > 0) {
                info.put("foreignReceivers", foreignReceivers);
            }

            // version + sdk min/max from yaml file
            Map<String, String> yamlAppInfo = getAppInfoFromYaml(decodedApkFolderPath);

            if (yamlAppInfo.get("versionCode") != null) {
                info.put("versionCode", yamlAppInfo.get("versionCode"));
            }

            if (yamlAppInfo.get("versionName") != null) {
                info.put("versionName", yamlAppInfo.get("versionName"));
            }

            if (yamlAppInfo.get("minSdkVersion") != null) {
                info.put("minSdkVersion", yamlAppInfo.get("minSdkVersion"));
            }

            if (yamlAppInfo.get("targetSdkVersion") != null) {
                info.put("targetSdkVersion", yamlAppInfo.get("targetSdkVersion"));
            }

            // get app name from values folders, strings.xml file or directly from the manifest file
            if (appLabel != null) { // extract app name from values/strings.xml file
                // appName = @string/app_name
                String[] appNameSplit = appLabel.split("/");
                if (appLabel.startsWith("@") && appNameSplit.length == 2) { // app name is declared in strings.xml file
                    if (appNameSplit[0].equals("@string")) {
                        String appNameAlias = appNameSplit[1]; // appNameAlias = app_name
                        // now we are going to traverse all strings.xml files looking for values of 'app_name'
                        List<Vertex> vertices = graph.traversal().V().has("name", "strings.xml").toList();
                        for (Vertex vertex : vertices) {
                            File stringsXmlFile = new File(vertex.value("path").toString());
                            if (stringsXmlFile.exists()) { // parse it and look for app name
                                DocumentBuilderFactory dbFactory_ = DocumentBuilderFactory.newInstance();
                                DocumentBuilder dBuilder_ = dbFactory_.newDocumentBuilder();

                                Path path_ = Paths.get(stringsXmlFile.getPath());
                                ByteArrayInputStream bais_ = new ByteArrayInputStream(Files.readAllBytes(path_));
                                InputSource is_ = new InputSource(bais_);
                                is_.setEncoding("UTF-8");

                                Document doc_ = dBuilder_.parse(is_);
                                try {
                                    bais_.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }


                                doc_.getDocumentElement().normalize();

                                NodeList strings = doc_.getElementsByTagName("string");
                                if (strings != null && strings.getLength() > 0) {
                                    for (int i = 0; i < strings.getLength(); i++) {
                                        Node nNode = strings.item(i);
                                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                                            Element eElement = (Element) nNode;
                                            if (eElement.getAttribute("name") != null &&
                                                    eElement.getAttribute("name").equals(appNameAlias)) {
                                                String appName = eElement.getTextContent();
                                                File valueFolder = stringsXmlFile.getParentFile();
                                                // split folder path by file separator
                                                String pattern = Pattern.quote(System.getProperty("file.separator"));
                                                String[] valueFolderSplit = valueFolder.getPath().split(pattern);
                                                String valueFolderName = valueFolderSplit[valueFolderSplit.length - 1];

                                                //System.out.println("app name: " + appName + " inside > " + valueFolderName);
                                                String appNameKey = "default";
                                                String languageName = "default";
                                                String[] valueFolderNameSplit = valueFolderName.split("-");
                                                if (valueFolderNameSplit.length != 1) {
                                                    appNameKey = valueFolderName.replace("values-", "");
                                                    /*
                                                    * The folder name of Android string files is formatted as the following:
                                                    * without region variant: values-[locale]
                                                    * with region variant: values-[locale]-r[region]
                                                    * For example: values-en, values-en-rGB, values-el-rGR
                                                     */
                                                    if (valueFolderNameSplit.length == 2) {
                                                        Locale forLangLocale = Locale.forLanguageTag(valueFolderNameSplit[1]);
                                                        languageName = forLangLocale.getDisplayLanguage(Locale.ENGLISH);
                                                    } else if (valueFolderNameSplit.length == 3) {
                                                        if (valueFolderNameSplit[2].startsWith("r")) {
                                                            Locale forLangLocale = Locale.forLanguageTag(valueFolderNameSplit[1] + "-" + valueFolderNameSplit[2].substring(1));
                                                            languageName = forLangLocale.getDisplayLanguage(Locale.ENGLISH);
                                                        } else {
                                                            Locale forLangLocale = Locale.forLanguageTag(valueFolderNameSplit[1] + "-" + valueFolderNameSplit[2]);
                                                            languageName = forLangLocale.getDisplayLanguage(Locale.ENGLISH);
                                                        }
                                                    }
                                                }
                                                //System.out.println("app name: " + appName + " appNameKey > " + appNameKey);
                                                JSONObject appNameJs = new JSONObject();
                                                appNameJs.put("key", appNameKey);
                                                appNameJs.put("languageName", languageName);
                                                appNameJs.put("value", Utils.unescapeXmlString(appName));
                                                appNames.add(appNameJs);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else { // app name is declared directly in the manifest file
                    JSONObject appNameJs = new JSONObject();
                    appNameJs.put("key", "default");
                    appNameJs.put("languageName", "default");
                    appNameJs.put("value", Utils.unescapeXmlString(appLabel));
                    appNames.add(appNameJs);
                }
                if (appNames.size() > 0) {
                    info.put("appNames", appNames);
                }
            }

            // get app icons data (icon bytes, height, width, size, last modified, relative path from decoded folder)
            if (appIcon != null) {
                // appIcon = @mipmap/ic_launcher
                String[] appIconSplit = appIcon.split("/");
                if (appIconSplit.length == 2) {
                    String resFolderAlias = appIconSplit[0].replace("@", ""); // appIconResFolderAlias = mipmap
                    String appIconAlias = appIconSplit[1]; // appIconAlias = ic_launcher
                    // now we are going to traverse the graph looking for app icon image files
                    List<Vertex> vertices = GraphManager.getInstance().getProjectAppIconFiles(graph, resFolderAlias, appIconAlias);
                    for (Vertex vertex : vertices) {
                        File iconFile = new File(vertex.value("path").toString());
                        if (iconFile.exists()) {

                            BufferedImage bimg = ImageIO.read(iconFile);
                            int width = bimg.getWidth();
                            int height = bimg.getHeight();
                            String relativePath = iconFile.getPath().replace(decodedApkFolderPath + File.separator, "").replace(File.separator, "/");

                            JSONObject appIconJs = new JSONObject();
                            appIconJs.put("bytes", Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(iconFile.getAbsolutePath()))));
                            appIconJs.put("width", width);
                            appIconJs.put("height", height);
                            appIconJs.put("size", iconFile.length());
                            appIconJs.put("lasModified", iconFile.lastModified());
                            appIconJs.put("relativePath", relativePath);
                            appIconJs.put("title", "dimension: " + width + "x" + height + ", file: " + relativePath);

                            appIcons.add(appIconJs);
                        }
                    }
                }

                if (appIcons.size() > 0) {
                    info.put("appIcons", appIcons);
                }
            }

            // get ad networks
            FileInputStream androidManifestFileInputStream = null;
            try {
                androidManifestFileInputStream = new FileInputStream(androidManifestFile);
                String manifestFileAdsString = IOUtils.toString(androidManifestFileInputStream);
                JSONArray availableAdNetworks = ApkToolsManager.getAvailableAdNetworks();
                for (Object an : availableAdNetworks) {
                    String networkPackageNamePrefix = (String) ((JSONObject) an).get("pn_prefix");
                    if (manifestFileAdsString.contains(networkPackageNamePrefix)) {
                        JSONObject adNetwok = new JSONObject();
                        adNetwok.put("adn_name", ((JSONObject) an).get("name"));
                        adNetwok.put("adn_site", ((JSONObject) an).get("site"));
                        adNetwok.put("adn_image_src", ((JSONObject) an).get("image"));
                        adNetworks.add(adNetwok);
                    }
                }
            } catch (Exception e) {
                // do nothing
            }finally {
                IOUtils.closeQuietly(androidManifestFileInputStream);
            }

            if (adNetworks.size() > 0) {
                info.put("adNetworks", adNetworks);
            }
        }
        return info;
    }


    /**
     * This method returns all project app icons (path, and last modified date).
     * This method is called when the user close a project, so we look for the last modified app icon, thus we can update project's icons in the project list
     * It is also called by the APP ICON MODIFIER tool from the projects editor.
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param graph                 project's graph
     * @return a json object containing all information about app icons (path, last modified date)
     */
    public JSONObject getProjectAppIconsData(String userUuid, String projectFolderNameUuid, Graph graph)
            throws ParserConfigurationException, SAXException, XPathExpressionException, IOException, ScriptException, XmlPullParserException {
        JSONObject info = new JSONObject();
        // path to /workfolder/users/userUuid/projectFolderNameUuid/decoded/
        String decodedApkFolderPath = Configurator.getInstance().getDecodedApkFolderPath(userUuid, projectFolderNameUuid, false);
        File folder = new File(decodedApkFolderPath);
        if (!folder.exists())
            throw new IllegalArgumentException("Folder '" + folder.getPath() + "' not fount!");
        if (!folder.isDirectory())
            throw new IllegalArgumentException("'" + folder.getPath() + "' is not a folder!");

        File[] files = folder.listFiles();
        if (files == null || files.length == 0)
            throw new IllegalArgumentException("Folder '" + folder.getPath() + "' is empty!");
        else { // parse the AndroidManifest.xml and get all project data from it

            // get the manifest file
            File androidManifestFile = getAndroidManifestFile(decodedApkFolderPath);
            if (!androidManifestFile.exists())
                throw new IllegalArgumentException("AndroidManifest.xml not fount!");

            // variables that will contain the results
            String appIcon = null;
            List<JSONObject> appIcons = new ArrayList<JSONObject>();

            // start parsing AndroidManifest.xml
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            //Document doc = dBuilder.parse(androidManifestFile);
            Path path = Paths.get(androidManifestFile.getPath());
            ByteArrayInputStream bais = new ByteArrayInputStream(Files.readAllBytes(path));
            InputSource is = new InputSource(bais);
            is.setEncoding("UTF-8");
            //is.setEncoding("ISO-8859-1");
            Document doc = dBuilder.parse(is);
            try {
                bais.close();
            } catch (Exception e) {
                e.printStackTrace();
            }


            doc.getDocumentElement().normalize();

            // application icon and name
            NodeList application = doc.getElementsByTagName("application");
            if (application != null && application.getLength() == 1) {
                if (application.item(0).getNodeType() == Node.ELEMENT_NODE) {
                    Element applicationElement = (Element) application.item(0);
                    appIcon = applicationElement.getAttribute("android:icon");
                }
            }

            // get app icons data (icon bytes, height, width, size, last modified, relative path from decoded folder)
            if (appIcon != null) {
                // appIcon = @mipmap/ic_launcher
                String[] appIconSplit = appIcon.split("/");
                if (appIconSplit.length == 2) {
                    String resFolderAlias = appIconSplit[0].replace("@", ""); // appIconResFolderAlias = mipmap
                    String appIconAlias = appIconSplit[1]; // appIconAlias = ic_launcher
                    // now we are going to traverse the graph looking for app icon image files
                    List<Vertex> vertices = GraphManager.getInstance().getProjectAppIconFiles(graph, resFolderAlias, appIconAlias);
                    for (Vertex vertex : vertices) {
                        File iconFile = new File(vertex.value("path").toString());
                        if (iconFile.exists()) {
                            JSONObject appIconJs = new JSONObject();
                            appIconJs.put("iconNodeId", vertex.id().toString());
                            appIconJs.put("iconPath", iconFile.getPath());
                            appIconJs.put("iconLastModified", iconFile.lastModified());
                            appIcons.add(appIconJs);
                        }
                    }
                }

                if (appIcons.size() > 0) {
                    info.put("appIcons", appIcons);
                }
            }

        }
        return info;
    }

    /**
     * This method is called by "APP ICON MODIFIER" tool from the project editor, all it does is to update all existing
     * app icons by the new one, every app icon file is updated with the new resized image, and keep its original size
     *
     * @param userUuid      user uuid
     * @param project       project bean
     * @param graph         graph of the project
     * @param pathToNewIcon path to the new icon file in tmp folder
     * @return a list containing all vertices ids of the modified icons, so that the client can update these
     * file inside the editor if they are opened
     * @throws ScriptException
     * @throws SAXException
     * @throws XPathExpressionException
     * @throws XmlPullParserException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    public List<String> modifyAppIcons(String userUuid, ApkToolProjectBean project, Graph graph, String pathToNewIcon)
            throws ScriptException, SAXException, XPathExpressionException, XmlPullParserException, ParserConfigurationException, IOException {
        List<String> listUpdatedNodes = new ArrayList<>();

        JSONObject appIconsData = getProjectAppIconsData(userUuid, project.getProjectFolderNameUuid(), graph);
        if (appIconsData.get("appIcons") != null) {
            @SuppressWarnings("unchecked")
            List<JSONObject> appIcons = (List<JSONObject>) appIconsData.get("appIcons");
            Date lastOneDate = null;
            String lastOnePath = null;

            for (JSONObject appIcon : appIcons) {
                String iconNodeId = appIcon.getAsString("iconNodeId");
                String iconPath = appIcon.getAsString("iconPath");

                long iconLastModified = (long) appIcon.get("iconLastModified");
                Date lastModifiedDate = new Date(iconLastModified * 1000);


                File iconFile = new File(iconPath);

                if (iconFile.exists()) {
                    BufferedImage bimg = ImageIO.read(iconFile);
                    int width = bimg.getWidth();
                    int height = bimg.getHeight();
                    byte[] scaledImage = Utils.scaleImage(Files.readAllBytes(Paths.get(pathToNewIcon)), width, height, FilenameUtils.getExtension(iconPath));
                    // write bytes file
                    if (scaledImage == null) {
                        throw new IOException("Couldn't scale image file : " + iconFile.getPath());
                    }
                    Files.write(Paths.get(iconFile.getAbsolutePath()), scaledImage);
                    listUpdatedNodes.add(iconNodeId);

                    // update project icon
                    if (lastOneDate == null) {
                        lastOneDate = lastModifiedDate;
                        lastOnePath = iconPath;
                    } else {
                        if (lastOneDate.before(lastModifiedDate)) {
                            lastOneDate = lastModifiedDate;
                            lastOnePath = iconPath;
                        }
                    }
                }
            }

            // update project icon
            if (lastOnePath != null) {
                String iconBytesAsString = Utils.getImageThumbnailAsBase64String(new File(lastOnePath), 45);
                project.setIconBytesAsString(iconBytesAsString);
                ApkToolProjectDao.getInstance().update(project);
            }
        }
        return listUpdatedNodes;
    }

    /**
     * This method is called by the the tool called "APP NAME MODIFIER" inside the project editor. it grabs all app names
     *
     * @param userUuid              user uuid
     * @param projectFolderNameUuid project folder name
     * @param graph                 graph name
     * @return a json obejct containing two element 'declaration' its value (INSIDE_STRINGS_XML or INSIDE_MANIFEST) that indicates
     * if app_name is declared inside the manifest file or inside strings.xml resources files.
     * And 'appNames' contains all app names
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws XPathExpressionException
     * @throws IOException
     * @throws ScriptException
     * @throws XmlPullParserException
     */
    public JSONObject getAllAvailableAppNamesForAppNameModifierTool(String userUuid, String projectFolderNameUuid, Graph graph)
            throws ParserConfigurationException, SAXException, XPathExpressionException, IOException, ScriptException, XmlPullParserException {

        JSONObject info = new JSONObject();
        // path to /workfolder/users/userUuid/projectFolderNameUuid/decoded/
        String decodedApkFolderPath = Configurator.getInstance().getDecodedApkFolderPath(userUuid, projectFolderNameUuid, false);
        File folder = new File(decodedApkFolderPath);
        if (!folder.exists())
            throw new IllegalArgumentException("Folder '" + folder.getPath() + "' not fount!");
        if (!folder.isDirectory())
            throw new IllegalArgumentException("'" + folder.getPath() + "' is not a folder!");

        File[] files = folder.listFiles();
        if (files == null || files.length == 0)
            throw new IllegalArgumentException("Folder '" + folder.getPath() + "' is empty!");
        else { // parse the AndroidManifest.xml and get all project data from it

            // get the manifest file
            File androidManifestFile = getAndroidManifestFile(decodedApkFolderPath);
            if (!androidManifestFile.exists())
                throw new IllegalArgumentException("AndroidManifest.xml not fount!");

            // variables that will contain the results
            String appLabel = null;
            List<JSONObject> appNames = new ArrayList<JSONObject>();


            // start parsing AndroidManifest.xml
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            //Document doc = dBuilder.parse(androidManifestFile);
            Path path = Paths.get(androidManifestFile.getPath());
            ByteArrayInputStream bais = new ByteArrayInputStream(Files.readAllBytes(path));
            InputSource is = new InputSource(bais);
            is.setEncoding("UTF-8");
            //is.setEncoding("ISO-8859-1");
            Document doc = dBuilder.parse(is);
            try {
                bais.close();
            } catch (Exception e) {
                e.printStackTrace();
            }


            doc.getDocumentElement().normalize();

            // application name
            NodeList application = doc.getElementsByTagName("application");
            if (application != null && application.getLength() == 1) {
                if (application.item(0).getNodeType() == Node.ELEMENT_NODE) {
                    Element applicationElement = (Element) application.item(0);
                    appLabel = applicationElement.getAttribute("android:label");
                }
            }

            // get app name from values folders, strings.xml file or directly from the manifest file
            if (appLabel != null) { // extract app name from values/strings.xml file
                // appName = @string/app_name
                String[] appNameSplit = appLabel.split("/");
                if (appLabel.startsWith("@") && appNameSplit.length == 2) { // app name is declared in strings.xml file
                    info.put("declaration", "INSIDE_STRINGS_XML");

                    if (appNameSplit[0].equals("@string")) {
                        String appNameAlias = appNameSplit[1]; // appNameAlias = app_name
                        info.put("tagName", appNameAlias);
                        // now we are going to traverse all strings.xml files looking for values of 'app_name'
                        List<Vertex> vertices = graph.traversal().V().has("name", "strings.xml").toList();
                        for (Vertex vertex : vertices) {
                            File stringsXmlFile = new File(vertex.value("path").toString());
                            if (stringsXmlFile.exists()) { // parse it and look for app name
                                DocumentBuilderFactory dbFactory_ = DocumentBuilderFactory.newInstance();
                                DocumentBuilder dBuilder_ = dbFactory_.newDocumentBuilder();

                                Path path_ = Paths.get(stringsXmlFile.getPath());
                                ByteArrayInputStream bais_ = new ByteArrayInputStream(Files.readAllBytes(path_));
                                InputSource is_ = new InputSource(bais_);
                                is_.setEncoding("UTF-8");

                                Document doc_ = dBuilder_.parse(is_);
                                try {
                                    bais_.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                doc_.getDocumentElement().normalize();

                                NodeList strings = doc_.getElementsByTagName("string");
                                if (strings != null && strings.getLength() > 0) {
                                    for (int i = 0; i < strings.getLength(); i++) {
                                        Node nNode = strings.item(i);
                                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                                            Element eElement = (Element) nNode;
                                            if (eElement.getAttribute("name") != null &&
                                                    eElement.getAttribute("name").equals(appNameAlias)) {
                                                String appName = eElement.getTextContent();
                                                File valueFolder = stringsXmlFile.getParentFile();
                                                // split folder path by file separator
                                                String pattern = Pattern.quote(System.getProperty("file.separator"));
                                                String[] valueFolderSplit = valueFolder.getPath().split(pattern);
                                                String valueFolderName = valueFolderSplit[valueFolderSplit.length - 1];
                                                //System.out.println("app name: " + appName + " inside > " + valueFolderName);

                                                String appNameKey = "default";
                                                String languageName = "default";
                                                String[] valueFolderNameSplit = valueFolderName.split("-");
                                                if (valueFolderNameSplit.length != 1) {
                                                    appNameKey = valueFolderName.replace("values-", "");
                                                    /*
                                                    * The folder name of Android string files is formatted as the following (l10N):
                                                    * without region variant: values-[locale]
                                                    * with region variant: values-[locale]-r[region]
                                                    * For example: values-en, values-en-rGB, values-el-rGR
                                                     */
                                                    if (valueFolderNameSplit.length == 2) {
                                                        Locale forLangLocale = Locale.forLanguageTag(valueFolderNameSplit[1]);
                                                        languageName = forLangLocale.getDisplayLanguage(Locale.ENGLISH);
                                                    } else if (valueFolderNameSplit.length == 3) {
                                                        if (valueFolderNameSplit[2].startsWith("r")) {
                                                            Locale forLangLocale = Locale.forLanguageTag(valueFolderNameSplit[1] + "-" + valueFolderNameSplit[2].substring(1));
                                                            languageName = forLangLocale.getDisplayLanguage(Locale.ENGLISH);
                                                        } else {
                                                            Locale forLangLocale = Locale.forLanguageTag(valueFolderNameSplit[1] + "-" + valueFolderNameSplit[2]);
                                                            languageName = forLangLocale.getDisplayLanguage(Locale.ENGLISH);
                                                        }
                                                    }
                                                }
                                                //System.out.println("app name: " + appName + " appNameKey > " + appNameKey);

                                                JSONObject appNameJs = new JSONObject();
                                                appNameJs.put("key", appNameKey);
                                                appNameJs.put("languageName", languageName);
                                                appNameJs.put("value", Utils.unescapeXmlString(appName));
                                                appNames.add(appNameJs);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else { // app name is declared directly in the manifest file
                    info.put("declaration", "INSIDE_MANIFEST");
                    JSONObject appNameJs = new JSONObject();
                    appNameJs.put("key", "default");
                    appNameJs.put("languageName", "default");
                    appNameJs.put("value", Utils.unescapeXmlString(appLabel));
                    appNames.add(appNameJs);
                }

                if (appNames.size() > 0) {
                    info.put("appNames", appNames);
                }
            }
        }
        return info;
    }


    /**
     * @param userUuid
     * @param project
     * @param graph
     * @param newAppNames
     * @param declaration INSIDE_MANIFEST or INSIDE_STRINGS_XML
     * @param tagName     app_name
     * @return
     * @throws ScriptException
     * @throws SAXException
     * @throws XPathExpressionException
     * @throws XmlPullParserException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    public List<String> modifyAppName(String userUuid, ApkToolProjectBean project, Graph graph, Map<String, String> newAppNames, String declaration, String tagName)
            throws ScriptException, SAXException, XPathExpressionException, XmlPullParserException, ParserConfigurationException, IOException, TransformerException {
        List<String> listUpdatedNodes = new ArrayList<>();

        if (declaration.equals("INSIDE_MANIFEST")) {
            // newAppNames must contain only one value
            if (newAppNames.size() == 1) { // update manifest entry <application android:label="OLD APP NAME" ...></application>
                String newAppName = Utils.escapeXmlString(newAppNames.get("default"));

                String decodedApkFolderPath = Configurator.getInstance().getDecodedApkFolderPath(userUuid,
                        project.getProjectFolderNameUuid(), false);
                File folder = new File(decodedApkFolderPath);
                if (!folder.exists())
                    throw new IllegalArgumentException("Folder '" + folder.getPath() + "' not fount!");
                if (!folder.isDirectory())
                    throw new IllegalArgumentException("'" + folder.getPath() + "' is not a folder!");

                File[] files = folder.listFiles();
                if (files == null || files.length == 0)
                    throw new IllegalArgumentException("Folder '" + folder.getPath() + "' is empty!");
                else {
                    // get the manifest file
                    File androidManifestFile = getAndroidManifestFile(decodedApkFolderPath);
                    if (!androidManifestFile.exists())
                        throw new IllegalArgumentException("AndroidManifest.xml not fount!");

                    // start parsing AndroidManifest.xml
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

                    Path path = Paths.get(androidManifestFile.getPath());
                    ByteArrayInputStream bais = new ByteArrayInputStream(Files.readAllBytes(path));
                    InputSource is = new InputSource(bais);
                    is.setEncoding("UTF-8");
                    //is.setEncoding("ISO-8859-1");
                    Document doc = dBuilder.parse(is);
                    try {
                        bais.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    doc.getDocumentElement().normalize();

                    // update app name
                    NodeList application = doc.getElementsByTagName("application");
                    if (application != null && application.getLength() == 1) {
                        if (application.item(0).getNodeType() == Node.ELEMENT_NODE) {
                            Element applicationElement = (Element) application.item(0);
                            String appLabel = applicationElement.getAttribute("android:label");
                            if (appLabel != null) {
                                applicationElement.setAttribute("android:label", newAppName);

                                //write the updated document to file or console
                                doc.getDocumentElement().normalize();
                                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                Transformer transformer = transformerFactory.newTransformer();
                                DOMSource source = new DOMSource(doc);
                                StreamResult result = new StreamResult(androidManifestFile);
                                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                                transformer.transform(source, result);
                                LOGGER.debug("App name updated inside AndroidManifest.xml file successfully");

                                listUpdatedNodes.add(GraphManager.getInstance().getVertexFromFilePath(graph, androidManifestFile.getPath()).id().toString());
                            } else {
                                throw new IllegalArgumentException("Couldn't find android:label inside application tag of the AndroidManifest.xml!");
                            }
                        }
                    }
                }
            }
        } else if (declaration.equals("INSIDE_STRINGS_XML")) {
            List<Vertex> vertices = graph.traversal().V().has("name", "strings.xml").toList();
            for (String language : newAppNames.keySet()) {
                // must escape before inserting inside strings.xml otherwise we may face problem in case the new
                // name contains characters like <, > , &
                String newAppName = Utils.escapeXmlString(newAppNames.get(language));

                String pathContains = File.separator + "values-" + language + File.separator;
                if (language.equals("default")) {
                    pathContains = File.separator + "values" + File.separator;
                }

                String stringsXmlFilePath = null;
                String updatedNodeId = null;
                for (Vertex v : vertices) {
                    if (v.value("path").toString().contains(pathContains)) {
                        stringsXmlFilePath = v.value("path").toString();
                        updatedNodeId = v.id().toString();
                        break;
                    }
                }
                if (stringsXmlFilePath != null && updatedNodeId != null) {
                    File stringsXmlFile = new File(stringsXmlFilePath);
                    if (stringsXmlFile.exists()) {
                        // update strings.xml using tagName <string name="tagName">OLD APP NAME</string>
                        // start parsing AndroidManifest.xml
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

                        Path path = Paths.get(stringsXmlFile.getPath());
                        ByteArrayInputStream bais = new ByteArrayInputStream(Files.readAllBytes(path));
                        InputSource is = new InputSource(bais);
                        is.setEncoding("UTF-8");
                        //is.setEncoding("ISO-8859-1");
                        Document doc = dBuilder.parse(is);
                        try {
                            bais.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        doc.getDocumentElement().normalize();

                        // update app name
                        NodeList stringResources = doc.getElementsByTagName("string");
                        if (stringResources != null && stringResources.getLength() > 1) {
                            for (int i = 0; i < stringResources.getLength(); i++) {
                                Node nNode = stringResources.item(i);
                                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element eElement = (Element) nNode;
                                    String nameNode = eElement.getAttribute("name");
                                    if (nameNode != null && nameNode.equals(tagName)) {
                                        // update name text
                                        //eElement.setNodeValue(newAppName);
                                        eElement.setTextContent(newAppName);
                                        //write the updated document to file or console
                                        doc.getDocumentElement().normalize();
                                        TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                        Transformer transformer = transformerFactory.newTransformer();
                                        DOMSource source = new DOMSource(doc);
                                        StreamResult result = new StreamResult(stringsXmlFile);
                                        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                                        transformer.transform(source, result);
                                        LOGGER.debug("App name updated successfully inside : " + stringsXmlFilePath);

                                        listUpdatedNodes.add(updatedNodeId);
                                        break;
                                    }
                                }
                            }
                        } else {
                            throw new IllegalArgumentException("Couldn't update app name inside file : " + stringsXmlFilePath);
                        }
                    } else {
                        throw new IllegalArgumentException("Couldn't find file : " + stringsXmlFilePath);
                    }
                }
            }
        }
        return listUpdatedNodes;
    }

    /**
     * return a set contains all project's package names except paths inside filePathContainsToExclude
     *
     * @param graph                     project's graph
     * @param filePathContainsToExclude some paths to exclude
     * @param userUuid                  user uuid
     * @param projectFolderNameUuid     project folder name (uuid)
     * @return a set containing package names
     * @throws ScriptException
     * @throws IOException
     */
    public Set<String> getAllPackageNames(Graph graph, List<String> filePathContainsToExclude, String userUuid, String projectFolderNameUuid) throws ScriptException, IOException {
        Set<String> result = new HashSet<>();
        String smaliFolderPath = Configurator.getInstance()
                .getSmaliFolderPath(userUuid, projectFolderNameUuid, false);

        List<Vertex> vertices = GraphManager.getInstance().getAllSmaliFoldersVertices(graph, filePathContainsToExclude);

        List<String> prefix = new ArrayList<>();
        prefix.add("com");
        prefix.add("org");
        prefix.add("gov");
        prefix.add("edu");
        prefix.add("net");
        prefix.add("mil");

        for (Vertex vertex : vertices) {

            List<Vertex> childrenFiles = graph.traversal().V(vertex.id()).out("contains").hasLabel("file").toList();
            List<Vertex> childrenFolder = graph.traversal().V(vertex.id()).out("contains").hasLabel("directory").toList();

            // if folder contains one or more file => add it to package names
            // if folder contains only one folder =>  do not add it
            // if package name
            if (childrenFiles.size() > 0 || childrenFolder.size() != 1) {
                String packageName = vertex.value("path").toString().replace(smaliFolderPath + File.separator, "").replace(File.separator, ".");
                if (!prefix.contains(packageName)) {
                    result.add(packageName);
                }
            }
        }
        return result;
    }

    /**
     * This method return an JSON array containing all apk files information of a certain project defined by its uuid
     *
     * @param userUuid              user uuid
     * @param projectFolderNameUuid project root folder name
     * @param isTemporary           if project is in temporary folder
     * @return a list of json objects containing all information about all project's apk files
     */
    public List<JSONObject> getProjectApkFilesInfo(String userUuid, String projectFolderNameUuid, boolean isTemporary) {
        List<JSONObject> result = new ArrayList<>();

        // original apk file
        String srcApkFolderPath = Configurator.getInstance().getSrcApkFolder(userUuid, projectFolderNameUuid, isTemporary);
        File srcApkFolder = new File(srcApkFolderPath);
        if(srcApkFolder.exists() && srcApkFolder.isDirectory()){
            File[] files = srcApkFolder.listFiles();
            if(files!= null && files.length == 1){
                File originalApk = files[0];
                JSONObject apkInfo = new JSONObject();
                apkInfo.put("apkName", originalApk.getName());
                apkInfo.put("dateCreated", new Date(originalApk.lastModified()));
                apkInfo.put("dateCreatedFormatted", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(originalApk.lastModified()));
                apkInfo.put("description", "The original apk from which this project was created");
                apkInfo.put("is_original", true);
                result.add(apkInfo);
            }
        }

        // generated apk files
        String apksFolderPath = Configurator.getInstance().getGenFolderPath(userUuid, projectFolderNameUuid, isTemporary);
        File apksFolder = new File(apksFolderPath);
        if (apksFolder.exists() && apksFolder.isDirectory()) {
            File[] apksFiles = apksFolder.listFiles();
            if (apksFiles != null) {
                //sort => the oldest files
                Arrays.sort(apksFiles, (f1, f2) -> Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()));

                for (File apk : apksFiles) {
                    if (!apk.getName().startsWith("instantRun")) {
                        JSONObject apkInfo = new JSONObject();
                        apkInfo.put("apkName", apk.getName());
                        apkInfo.put("dateCreated", new Date(apk.lastModified()));
                        apkInfo.put("dateCreatedFormatted", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(apk.lastModified()));
                        apkInfo.put("description", (apk.getName().endsWith("-aligned-debugSigned.apk"))? "DEBUG" : "RELEASE");
                        apkInfo.put("is_original", false);
                        result.add(apkInfo);
                    }
                }
            }
        }
        return result;
    }


    /**
     * This method returns all manifest entries(Activities, services and receivers).
     * This method is called when the user clicks on 'Manifest entries renamer' button in the project editor toolbar > Tols
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @return a json object containing all manifest entries : activities , launcher activity, services, receivers
     */
    public JSONObject getAllManifestEntriesForRenamerTool(String userUuid, String projectFolderNameUuid, boolean isTemporary)
            throws Exception {
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("APKTool thread was aborted abnormally!");
        }


        JSONObject info = new JSONObject();
        // path to /workfolder/users/userUuid/projectFolderNameUuid/decoded/
        String decodedApkFolderPath = Configurator.getInstance().getDecodedApkFolderPath(userUuid, projectFolderNameUuid, isTemporary);
        File folder = new File(decodedApkFolderPath);
        if (!folder.exists())
            throw new IllegalArgumentException("Folder '" + folder.getPath() + "' not fount!");
        if (!folder.isDirectory())
            throw new IllegalArgumentException("'" + folder.getPath() + "' is not a folder!");

        File[] files = folder.listFiles();
        if (files == null || files.length == 0)
            throw new IllegalArgumentException("Folder '" + folder.getPath() + "' is empty!");
        else { // parse the AndroidManifest.xml and get all project data from it

            // get the manifest file
            File androidManifestFile = getAndroidManifestFile(decodedApkFolderPath);
            if (!androidManifestFile.exists())
                throw new IllegalArgumentException("AndroidManifest.xml not fount!");

            // variables that will contain the results
            String packageName = null;
            String launcherActivity = null;

            List<String> packageActivities = new ArrayList<String>();
            List<String> foreignActivities = new ArrayList<String>();
            List<String> packageServices = new ArrayList<String>();
            List<String> foreignServices = new ArrayList<String>();
            List<String> packageReceivers = new ArrayList<String>();
            List<String> foreignReceivers = new ArrayList<String>();


            // start parsing AndroidManifest.xml
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            //Document doc = dBuilder.parse(androidManifestFile);
            Path path = Paths.get(androidManifestFile.getPath());
            ByteArrayInputStream bais = new ByteArrayInputStream(Files.readAllBytes(path));
            InputSource is = new InputSource(bais);
            is.setEncoding("UTF-8");
            //is.setEncoding("ISO-8859-1");
            Document doc = dBuilder.parse(is);
            try {
                bais.close();
            } catch (Exception e) {
                e.printStackTrace();
            }


            doc.getDocumentElement().normalize();

            Element manifestRootElement = doc.getDocumentElement();

            packageName = manifestRootElement.getAttribute("package");

            // activities
            NodeList activitiesList = doc.getElementsByTagName("activity");
            boolean foundLauncherActivity = false;

            for (int i = 0; i < activitiesList.getLength(); i++) {
                Node nNode = activitiesList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String activityName = eElement.getAttribute("android:name");

                    if (activityName != null && packageName != null) {
                        if (activityName.startsWith("."))
                            packageActivities.add(packageName + activityName);
                        else if (activityName.startsWith(packageName))
                            packageActivities.add(activityName);
                        else if (!activityName.contains("."))
                            packageActivities.add(packageName + "." + activityName);
                        else if (!StringUtils.startsWithAny(activityName, excludedPackageNames)) {
                            foreignActivities.add(activityName);
                        }
                    }

                    NodeList intentFilter = eElement
                            .getElementsByTagName("intent-filter");

                    if (!foundLauncherActivity) {
                        if (intentFilter != null && intentFilter.getLength() > 0) {
                            for (int k = 0; k < intentFilter.getLength(); k++) {
                                if (intentFilter.item(k).getNodeType() == Node.ELEMENT_NODE) {
                                    Element iElement = (Element) intentFilter.item(k);
                                    NodeList category = iElement
                                            .getElementsByTagName("category");
                                    if (category != null && category.getLength() > 0) {
                                        for (int j = 0; j < category.getLength(); j++) {
                                            if (category.item(j).getNodeType() == Node.ELEMENT_NODE) {
                                                Element catElement = (Element) category.item(j);
                                                String categoryName = catElement.getAttribute("android:name");
                                                if (categoryName.equals("android.intent.category.LAUNCHER")) {
                                                    foundLauncherActivity = true;
                                                    if (activityName != null && activityName.startsWith("."))
                                                        launcherActivity = packageName + activityName;
                                                    else if (activityName != null && !activityName.contains("."))
                                                        launcherActivity = packageName + "." + activityName;
                                                    else
                                                        launcherActivity = activityName;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // services
            NodeList servicesList = doc.getElementsByTagName("service");

            for (int i = 0; i < servicesList.getLength(); i++) {
                Node nNode = servicesList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String serviceName = eElement.getAttribute("android:name");

                    if (serviceName != null && packageName != null) {
                        if (serviceName.startsWith("."))
                            packageServices.add(packageName + serviceName);
                        else if (serviceName.startsWith(packageName))
                            packageServices.add(serviceName);
                        else if (!serviceName.contains("."))
                            packageServices.add(packageName + "." + serviceName);
                        else if (!StringUtils.startsWithAny(serviceName, excludedPackageNames)) {
                            foreignServices.add(serviceName);
                        }
                    }
                }
            }

            // receivers
            NodeList receiverList = doc.getElementsByTagName("receiver");

            for (int i = 0; i < receiverList.getLength(); i++) {
                Node nNode = receiverList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String receiverName = eElement.getAttribute("android:name");

                    if (receiverName != null && packageName != null) {
                        if (receiverName.startsWith("."))
                            packageReceivers.add(packageName + receiverName);
                        else if (receiverName.startsWith(packageName))
                            packageReceivers.add(receiverName);
                        else if (!receiverName.contains("."))
                            packageReceivers.add(packageName + "." + receiverName);
                        else if (!StringUtils.startsWithAny(receiverName, excludedPackageNames)) {
                            foreignReceivers.add(receiverName);
                        }
                    }
                }
            }


            // populate the result object and send it back to the client
            if (packageName != null) {
                info.put("package", packageName);
            }

            if (launcherActivity != null) {
                info.put("launcherActivity", launcherActivity);
            }

            if (packageActivities.size() > 0) {
                info.put("packageActivities", packageActivities);
            }

            if (foreignActivities.size() > 0) {
                info.put("foreignActivities", foreignActivities);
            }

            if (packageServices.size() > 0) {
                info.put("packageServices", packageServices);
            }

            if (foreignServices.size() > 0) {
                info.put("foreignServices", foreignServices);
            }

            if (packageReceivers.size() > 0) {
                info.put("packageReceivers", packageReceivers);
            }

            if (foreignReceivers.size() > 0) {
                info.put("foreignReceivers", foreignReceivers);
            }
        }
        return info;
    }


    /**
     * This method returns the necessary information about the project
     * such as : packageName, launcherActivity, packageActivities, foreignActivities
     * This method is used by serguad
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           boolean indicating if the project is temporary on not
     * @return a json object containing information about the project (package name, activities
     * and foreign activities, launcher activity
     */
    public JSONObject getProjectInfoForSerg(String userUuid, String projectFolderNameUuid, boolean isTemporary)
            throws ParserConfigurationException, SAXException, XPathExpressionException, IOException, ScriptException, XmlPullParserException {

        JSONObject info = new JSONObject();
        // path to /workfolder/users/userUuid/projectFolderNameUuid/decoded/
        String decodedApkFolderPath = Configurator.getInstance().getDecodedApkFolderPath(userUuid, projectFolderNameUuid, isTemporary);
        File folder = new File(decodedApkFolderPath);
        if (!folder.exists())
            throw new IllegalArgumentException("Folder '" + folder.getPath() + "' not fount!");
        if (!folder.isDirectory())
            throw new IllegalArgumentException("'" + folder.getPath() + "' is not a folder!");

        File[] files = folder.listFiles();
        if (files == null || files.length == 0)
            throw new IllegalArgumentException("Folder '" + folder.getPath() + "' is empty!");
        else { // parse the AndroidManifest.xml and get all project data from it

            // get the manifest file
            File androidManifestFile = getAndroidManifestFile(decodedApkFolderPath);
            if (!androidManifestFile.exists())
                throw new IllegalArgumentException("AndroidManifest.xml not fount!");

            // variables that will contain the results
            String packageName = null;

            String launcherActivity = null;

            List<String> packageActivities = new ArrayList<String>();
            List<String> foreignActivities = new ArrayList<String>();


            // start parsing AndroidManifest.xml
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            //Document doc = dBuilder.parse(androidManifestFile);
            Path path = Paths.get(androidManifestFile.getPath());
            ByteArrayInputStream bais = new ByteArrayInputStream(Files.readAllBytes(path));
            InputSource is = new InputSource(bais);
            is.setEncoding("UTF-8");
            //is.setEncoding("ISO-8859-1");
            Document doc = dBuilder.parse(is);
            try {
                bais.close();
            } catch (Exception e) {
                e.printStackTrace();
            }


            doc.getDocumentElement().normalize();

            Element manifestRootElement = doc.getDocumentElement();

            packageName = manifestRootElement.getAttribute("package");

            // activities
            NodeList activitiesList = doc.getElementsByTagName("activity");
            boolean foundLauncherActivity = false;

            for (int i = 0; i < activitiesList.getLength(); i++) {
                Node nNode = activitiesList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String activityName = eElement.getAttribute("android:name");

                    if (activityName != null && packageName != null) {
                        if (activityName.startsWith("."))
                            packageActivities.add(packageName + activityName);
                        else if (activityName.startsWith(packageName))
                            packageActivities.add(activityName);
                        else if (!activityName.contains("."))
                            packageActivities.add(packageName + "." + activityName);
                        else
                            foreignActivities.add(activityName);
                    }

                    NodeList intentFilter = eElement
                            .getElementsByTagName("intent-filter");

                    if (!foundLauncherActivity) {
                        if (intentFilter != null && intentFilter.getLength() > 0) {
                            for (int k = 0; k < intentFilter.getLength(); k++) {
                                if (intentFilter.item(k).getNodeType() == Node.ELEMENT_NODE) {
                                    Element iElement = (Element) intentFilter.item(k);
                                    NodeList category = iElement
                                            .getElementsByTagName("category");
                                    if (category != null && category.getLength() > 0) {
                                        for (int j = 0; j < category.getLength(); j++) {
                                            if (category.item(j).getNodeType() == Node.ELEMENT_NODE) {
                                                Element catElement = (Element) category.item(j);
                                                String categoryName = catElement.getAttribute("android:name");
                                                if (categoryName.equals("android.intent.category.LAUNCHER")) {
                                                    foundLauncherActivity = true;
                                                    if (activityName != null && activityName.startsWith("."))
                                                        launcherActivity = packageName + activityName;
                                                    else if (activityName != null && !activityName.contains("."))
                                                        launcherActivity = packageName + "." + activityName;
                                                    else
                                                        launcherActivity = activityName;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (packageName != null) {
                info.put("package", packageName);
            }

            if (launcherActivity != null) {
                info.put("launcherActivity", launcherActivity);
            }

            if (packageActivities.size() > 0) {
                info.put("packageActivities", packageActivities);
            }

            if (foreignActivities.size() > 0) {
                info.put("foreignActivities", foreignActivities);
            }
        }
        return info;
    }

}
