package com.ninjaflip.androidrevenge.core.apktool.apkinfo;


import com.ninjaflip.androidrevenge.core.apktool.ApkToolsManager;
import com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.ApkFile;
import com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.bean.ApkMeta;
import com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.bean.UseFeature;
import com.ninjaflip.androidrevenge.core.apktool.apkinfo.manifestextractor.Extract;
import com.ninjaflip.androidrevenge.core.apktool.apkinfo.res.AXmlResourceParser;
import com.ninjaflip.androidrevenge.core.apktool.apkinfo.util.TypedValue;
import com.ninjaflip.androidrevenge.core.apktool.apkinfo.xmlpull.XmlPullParser;
import com.ninjaflip.androidrevenge.core.apktool.signzipalign.apksigner.AndroidApkSignerVerify;
import com.ninjaflip.androidrevenge.utils.StringUtil;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 *
 */
public class AnalysisApk {
    private static final Logger LOGGER = Logger.getLogger(AnalysisApk.class);


    /**
     * Unzip the zip file (apk can be used as a zip file), note that can not extract the rar file
     * <p>
     *
     * @param apkPath path to apk file
     * @return Map containing all non null info about the apk
     * package : package name
     * size_in_MB: apk file size in mega bytes
     * versionCode: version code
     * versionName: version name
     * permissions : list of permissions
     * minSdkVersion: min sdk version
     * targetSdkVersion: target sdk
     * maxSdkVersion: max sdk version
     * packageActivities: list of activities that belongs the the app
     * foreignActivities: list of dependencies activities (ex: google ads, facebook analytics,...)
     * services: list of services
     * receivers: list of receivers
     * assets: list of assets (apk resources relative paths paths)
     * @throws IOException when Negative seek offset abnormal
     */
    public static Map<String, Object> unZip(String apkPath) throws IOException {
        Map<String, Object> info = new HashMap<String, Object>();
        info.put("apkPath", apkPath);
        // 1024 x 1024 = 1048576
        double fileSizeInMB = new File(apkPath).length() / 1048576.0;
        info.put("size_in_MB", new DecimalFormat("###.##").format(fileSizeInMB));

        List<String> listPermission = new ArrayList<String>();
        List<String> packageActivities = new ArrayList<String>();
        List<String> foreignActivities = new ArrayList<String>();
        List<String> packageServices = new ArrayList<String>();
        List<String> foreignServices = new ArrayList<String>();
        List<String> packageReceivers = new ArrayList<String>();
        List<String> foreignReceivers = new ArrayList<String>();
        List<JSONObject> appIcons = new ArrayList<>();
        Set<String> assets = new HashSet<>();
        List<JSONObject> adNetworks = new ArrayList<>();
        String packageName = null;
        ZipFile zipFile = new ZipFile(new File(apkPath));
        Enumeration<?> enumeration = zipFile.entries();
        ArrayList zipEntriesList = Collections.list(enumeration);
        String appIcon = null;


        // get appname and app icon
        try (ApkFile apkFile = new ApkFile(new File(apkPath))) {
            ApkMeta apkMeta = apkFile.getApkMeta();

            // get appname
            String appName = apkMeta.getName();
            if (appName != null) {
                LOGGER.info("apkMeta.getName() " + appName);
                info.put("app_name", appName);
            }
            // get app icon
            appIcon = apkMeta.getIcon();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }

        // let's tell the program how to get all app icons
        String iconStart = null;
        String iconEnd = null;
        if (appIcon != null) {
            /*
             for example appIcon = res/mipmap-mdpi-v4/ic_launcher.png
             we split it to [res/mipmap, mdpi, v4/ic_launcher.png] using '-' as separator and we take its first element
             then we split it to [res, mipmap-mdpi-v4, ic_launcher.png] using '/' as separator ans we take its last element

             and then, in the following iterations, we check every entry name if
             it start with 'res/mipmap' and ends with 'ic_launcher.png' => its an app icon
              */

            String[] appIconHyphenSplit = appIcon.split("-");
            String[] appIconSlashSplit = appIcon.split("/");


            if (appIcon.contains("-")) {
                if (appIconHyphenSplit.length > 0) {
                    iconStart = appIconHyphenSplit[0];
                }
                if (appIconSlashSplit.length > 0) {
                    iconEnd = appIconSlashSplit[appIconSlashSplit.length - 1];
                }
            } else {
                if (appIconSlashSplit.length > 2) {
                    iconStart = appIconSlashSplit[0] + "/" + appIconSlashSplit[1];
                    iconEnd = appIconSlashSplit[appIconSlashSplit.length - 1];
                }
            }
        }

        boolean checkIcon = false;
        if (iconStart != null && iconEnd != null) {
            checkIcon = true;
        }

        ZipEntry zipEntry;
        for (Object obj : zipEntriesList) {
            zipEntry = (ZipEntry) obj;
            if (!zipEntry.isDirectory()) {
                if ("AndroidManifest.xml".equals(zipEntry.getName())) {
                    try {
                        AXmlResourceParser parser = new AXmlResourceParser();
                        parser.open(zipFile.getInputStream(zipEntry));
                        while (true) {
                            int type = parser.next();
                            if (type == XmlPullParser.END_DOCUMENT) {
                                break;
                            } else if (type == XmlPullParser.START_TAG) {
                                String nameSpace = parser.getNamespace();
                                //System.out.println("................. " + parser.getName());
                                if (parser.getName().equals("manifest")) {
                                    for (int i = 0; i != parser.getAttributeCount(); ++i) {
                                        if ("package".equals(parser.getAttributeName(i))) {
                                            info.put("package", AXmlResourceParser.getAttributeValue(parser, i));
                                            packageName = AXmlResourceParser.getAttributeValue(parser, i);
                                        } else if ("versionCode".equals(parser.getAttributeName(i))) {
                                            info.put("versionCode", AXmlResourceParser.getAttributeValue(parser, i));
                                        } else if ("versionName".equals(parser.getAttributeName(i))) {
                                            info.put("versionName", AXmlResourceParser.getAttributeValue(parser, i));
                                        }
                                    }
                                } else if (parser.getName().equals("uses-permission")) {
                                    for (int i = 0; i != parser.getAttributeCount(); ++i) {
                                        listPermission.add(parser.getAttributeValue(nameSpace, "name"));
                                    }
                                } else if (parser.getName().equals("uses-sdk")) {
                                    for (int i = 0; i != parser.getAttributeCount(); ++i) {
                                        if ("minSdkVersion".equals(parser.getAttributeName(i))) {
                                            info.put("minSdkVersion", AXmlResourceParser.getAttributeValue(parser, i));
                                        } else if ("targetSdkVersion".equals(parser.getAttributeName(i))) {
                                            info.put("targetSdkVersion", AXmlResourceParser.getAttributeValue(parser, i));
                                        } else if ("maxSdkVersion".equals(parser.getAttributeName(i))) {
                                            info.put("maxSdkVersion", AXmlResourceParser.getAttributeValue(parser, i));
                                        }
                                    }
                                } else if (parser.getName().equals("activity")) {
                                    String nameSpace_ = parser.getNamespace();
                                    String activityName = parser.getAttributeValue(nameSpace_, "name");
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
                                } else if (parser.getName().equals("service")) {
                                    String nameSpace_ = parser.getNamespace();
                                    String serviceName = parser.getAttributeValue(nameSpace_, "name");
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
                                } else if (parser.getName().equals("receiver")) {
                                    String nameSpace_ = parser.getNamespace();
                                    String receiverName = parser.getAttributeValue(nameSpace_, "name");
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
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                String zipEntryName = zipEntry.getName();
                //System.out.println(zipEntry.getName());
                assets.add("files/" + zipEntryName);

                // check zip entry is an icon
                if (checkIcon) {
                    if (zipEntryName.startsWith(iconStart) && zipEntryName.endsWith(iconEnd)) {

                        //LOGGER.debug("Found icon file: " + zipEntryName);
                        int imgHeight = -1;
                        int imgWidth = -1;
                        byte[] iconBytes = getZippedResourceFileFromApkAsBytes(apkPath, zipEntryName);
                        ByteArrayInputStream in = new ByteArrayInputStream(iconBytes);
                        try {
                            BufferedImage img = ImageIO.read(in);
                            imgHeight = img.getHeight();
                            imgWidth = img.getWidth();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                in.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        String iconBase64Str = Base64.getEncoder().encodeToString(iconBytes);

                        JSONObject objIconInfo = new JSONObject();
                        objIconInfo.put("bytes", iconBase64Str);
                        objIconInfo.put("width", imgWidth);
                        objIconInfo.put("height", imgHeight);
                        objIconInfo.put("size", iconBytes.length);
                        objIconInfo.put("relativePath", zipEntryName);
                        objIconInfo.put("title", "dimension: " + imgWidth + "x" + imgHeight + ", file: " + zipEntryName);

                        appIcons.add(objIconInfo);
                    }
                }
            }
        }

        if (listPermission.size() > 0) {
            info.put("permissions", listPermission);
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

        if (appIcons.size() > 0) {
            info.put("appIcons", appIcons);

            int largestIconSize = 0;
            String largetIconZipEntry = "";

            for (JSONObject obj : appIcons) {
                int size = (int) obj.get("size");
                if (size > largestIconSize) {
                    largestIconSize = size;
                    largetIconZipEntry = (String) obj.get("relativePath");
                }
            }
            if (!largetIconZipEntry.equals("")) {
                info.put("ic_launcher", largetIconZipEntry);
                try {
                    String resizedIconBase64Str = Base64.getEncoder().encodeToString(Utils
                            .scaleImage(getZippedResourceFileFromApkAsBytes(apkPath, largetIconZipEntry), 120, 120));
                    info.put("ic_launcher_bytes_resized", resizedIconBase64Str);
                } catch (Exception e) {
                    // do nothing
                }

            } else {
                info.put("ic_launcher", appIcon);
            }
        }

        // get signature
        info.put("signatureInfo", getApkSignatureInfo(new File(apkPath), true, true));

        // TODO replace the word 'assets' by 'entries'
        if (assets.size() > 0) {
            info.put("assets", assets);
        }

        // get ad networks
        try {
            Extract ex = new Extract();
            String manifestFileAdsString = ex.getManifest(apkPath);
            /*LOGGER.debug("#############################################################");
            LOGGER.debug(manifestFileAdsString);
            LOGGER.debug("#############################################################");*/
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
        }

        if (adNetworks.size() > 0) {
            info.put("adNetworks", adNetworks);
        }


        zipFile.close();
        return info;
    }


    /**
     * get the largest available icon file from apk resources
     */
    private static String getLargestAppIcon(String appIcon, Set<String> assets) {
        String[] tmp = appIcon.split("/");
        String iconFileName = tmp[tmp.length - 1];

        // list containing all possible paths to app icon inside the apk
        List<String> listIconPaths = new ArrayList<String>();
        listIconPaths.add("files/res/mipmap-xxxhdpi/" + iconFileName);
        listIconPaths.add("files/res/drawable-xxxhdpi/" + iconFileName);

        listIconPaths.add("files/res/mipmap-xxhdpi/" + iconFileName);
        listIconPaths.add("files/res/drawable-xxhdpi/" + iconFileName);

        listIconPaths.add("files/res/mipmap-xhdpi/" + iconFileName);
        listIconPaths.add("files/res/drawable-xhdpi/" + iconFileName);

        listIconPaths.add("files/res/mipmap-hdpi/" + iconFileName);
        listIconPaths.add("files/res/drawable-hdpi/" + iconFileName);

        listIconPaths.add("files/res/mipmap-mdpi/" + iconFileName);
        listIconPaths.add("files/res/drawable-mdpi/" + iconFileName);


        for (int i = 1; i < 26; i++) {
            listIconPaths.add("files/res/mipmap-xxxhdpi-v" + i + "/" + iconFileName);
            listIconPaths.add("files/res/drawable-xxxhdpi-v" + i + "/" + iconFileName);

            listIconPaths.add("files/res/mipmap-xxhdpi-v" + i + "/" + iconFileName);
            listIconPaths.add("files/res/drawable-xxhdpi-v" + i + "/" + iconFileName);

            listIconPaths.add("files/res/mipmap-xhdpi-v" + i + "/" + iconFileName);
            listIconPaths.add("files/res/drawable-xhdpi-v" + i + "/" + iconFileName);

            listIconPaths.add("files/res/mipmap-hdpi-v" + i + "/" + iconFileName);
            listIconPaths.add("files/res/drawable-hdpi-v" + i + "/" + iconFileName);

            listIconPaths.add("files/res/mipmap-mdpi-v" + i + "/" + iconFileName);
            listIconPaths.add("files/res/drawable-mdpi-v" + i + "/" + iconFileName);
        }

        listIconPaths.add("files/res/drawable/" + iconFileName);

        for (String iconPath : listIconPaths) {
            if (assets.contains(iconPath)) {
                return iconPath.replace("files/", "");
            }
        }
        return null;
    }

    /**
     * This method build a clone file of a given zipped resource inside apk, and try to open it using the default program
     * The resource is located inside tha apk
     *
     * @param apkPath      path to target apk
     * @param zipEntryName the relative path of the resource inside the apk file
     * @throws IOException when an IO problem happen during execution
     */
    public static void openZippedResourceFromApk(String apkPath, String zipEntryName) throws IOException {
        if (Desktop.isDesktopSupported()) {
            ZipFile zipFile = new ZipFile(new File(apkPath));
            Enumeration<?> enumeration = zipFile.entries();
            ZipEntry zipEntry;
            boolean found = false;
            File tmpFile = null;
            while (enumeration.hasMoreElements() && !found) {
                zipEntry = (ZipEntry) enumeration.nextElement();
                if (zipEntry.getName().equals(zipEntryName)) {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    String SUFFIX = "." + FilenameUtils.getExtension(zipEntryName);
                    tmpFile = stream2file(inputStream, "apkStream2file", SUFFIX);
                    found = true;
                }
            }
            zipFile.close();
            if (found && tmpFile != null) {
                try {
                    Desktop.getDesktop().open(tmpFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalStateException("Could't open file '" + zipEntryName + "'");
                }
            }
        } else {
            throw new IllegalStateException("Opening files with java is not supported by you computer!");
        }
    }


    /**
     * Get package name from a apk zip file
     *
     * @param apkPath path to apk
     * @return package name as string
     * @throws IOException
     */
    public static String getPackageNameFromApkFile(String apkPath) throws IOException {
        ZipFile zipFile = new ZipFile(new File(apkPath));
        Enumeration<?> enumeration = zipFile.entries();
        ZipEntry zipEntry = null;
        while (enumeration.hasMoreElements()) {
            zipEntry = (ZipEntry) enumeration.nextElement();
            if (zipEntry.isDirectory()) {

            } else {
                if ("AndroidManifest.xml".equals(zipEntry.getName())) {
                    try {
                        AXmlResourceParser parser = new AXmlResourceParser();
                        parser.open(zipFile.getInputStream(zipEntry));
                        while (true) {
                            int type = parser.next();
                            if (type == XmlPullParser.END_DOCUMENT) {
                                break;
                            } else if (type == XmlPullParser.START_TAG) {
                                if (parser.getName().equals("manifest")) {
                                    for (int i = 0; i != parser.getAttributeCount(); ++i) {
                                        if ("package".equals(parser.getAttributeName(i))) {
                                            return AXmlResourceParser.getAttributeValue(parser, i);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        zipFile.close();
        return null;
    }

    /**
     * Unzip the zip file (apk can be used as a zip file), note that can not extract the rar file
     * <p>
     *
     * @param apkPath  path to apk file
     * @param iconSize size of the resized icon
     * @return Map containing some info about the apk
     * package : package name
     * app_name: app name
     * ic_launcher: icon path in the apk
     * ic_launcher_bytes: original icon bytes
     * ic_launcher_bytes_resized : resized icon bytes
     * @throws IOException when Negative seek offset abnormal
     */
    public static Map<String, Object> getAppIconAndPackageNameAndAppName(String apkPath, int iconSize) throws IOException {
        Map<String, Object> info = new HashMap<String, Object>();

        ZipFile zipFile = new ZipFile(new File(apkPath));
        Enumeration<?> enumeration = zipFile.entries();
        Set<String> assets = new HashSet<>();
        ZipEntry zipEntry = null;
        while (enumeration.hasMoreElements()) {
            zipEntry = (ZipEntry) enumeration.nextElement();
            if (zipEntry.isDirectory()) {

            } else {
                if ("AndroidManifest.xml".equals(zipEntry.getName())) {
                    try {
                        AXmlResourceParser parser = new AXmlResourceParser();
                        parser.open(zipFile.getInputStream(zipEntry));
                        while (true) {
                            int type = parser.next();
                            if (type == XmlPullParser.END_DOCUMENT) {
                                break;
                            } else if (type == XmlPullParser.START_TAG) {
                                if (parser.getName().equals("manifest")) {
                                    for (int i = 0; i != parser.getAttributeCount(); ++i) {
                                        if ("package".equals(parser.getAttributeName(i))) {
                                            info.put("package", AXmlResourceParser.getAttributeValue(parser, i));
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                assets.add("files/" + zipEntry.getName());
            }
        }

        try (ApkFile apkFile = new ApkFile(new File(apkPath))) {
            ApkMeta apkMeta = apkFile.getApkMeta();

            // get appname
            String appName = apkMeta.getName();
            if (appName != null) {
                info.put("app_name", appName);
            }
            // get app icon
            String appIcon = apkMeta.getIcon();
            if (appIcon != null) {
                String largestAppIcon = getLargestAppIcon(appIcon, assets);
                if (largestAppIcon != null)
                    info.put("ic_launcher", largestAppIcon);
                else
                    info.put("ic_launcher", appIcon);

                String originalIconBase64Str = null;
                try {
                    originalIconBase64Str = getZippedResourceFileFromApkAsString(apkPath, (String) info.get("ic_launcher"));
                    info.put("ic_launcher_bytes", originalIconBase64Str);
                } catch (Exception e) {
                    // do nothing
                }

                String resizedIconBase64Str = null;
                try {
                    resizedIconBase64Str = Base64.getEncoder().encodeToString(Utils
                            .scaleImage(getZippedResourceFileFromApkAsBytes(apkPath, (String) info.get("ic_launcher")), iconSize, iconSize));
                    info.put("ic_launcher_bytes_resized", resizedIconBase64Str);
                } catch (Exception e) {
                    // do nothing
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        zipFile.close();
        return info;
    }


    /**
     * Get zipped entry as String
     *
     * @param apkPath      path to apk file
     * @param zipEntryName path to zipped file inside apk
     * @return base64 encoded string containing file as byte array
     * @throws IOException
     */
    public static byte[] getZippedResourceFileFromApkAsBytes(String apkPath, String zipEntryName) throws IOException {
        ZipFile zipFile = new ZipFile(new File(apkPath));
        Enumeration<?> enumeration = zipFile.entries();
        ZipEntry zipEntry;
        boolean found = false;
        while (enumeration.hasMoreElements() && !found) {
            zipEntry = (ZipEntry) enumeration.nextElement();
            if (zipEntry.getName().equals(zipEntryName)) {
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                IOUtils.copy(inputStream, buffer);
                zipFile.close();
                return buffer.toByteArray();
            }
        }
        try {
            zipFile.close();
            return null;
        } catch (Exception e) {//do nothing
            return null;
        }
    }

    /**
     * Get zipped entry as String
     *
     * @param apkPath      path to apk file
     * @param zipEntryName path to zipped file inside apk
     * @return base64 encoded string containing file as byte array
     * @throws IOException
     */
    public static String getZippedResourceFileFromApkAsString(String apkPath, String zipEntryName) throws IOException {
        try {
            return Base64.getEncoder().encodeToString(getZippedResourceFileFromApkAsBytes(apkPath, zipEntryName));
        } catch (Exception e) {//do nothing
            return null;
        }
    }

    /**
     * Convert an inputstream to a temporary file
     *
     * @param in     inputstream
     * @param PREFIX temp file prefix
     * @param SUFFIX temp file extension
     * @return a temporary file build fri
     * @throws IOException
     */
    private static File stream2file(InputStream in, String PREFIX, String SUFFIX) throws IOException {
        final File tempFile = File.createTempFile(PREFIX, SUFFIX);
        tempFile.deleteOnExit();
        OutputStream out = null;
        try {
            out = new FileOutputStream(tempFile);
            IOUtils.copyLarge(in, out);
        } finally {
            if (in != null)
                IOUtils.closeQuietly(in);
            if (out != null)
                IOUtils.closeQuietly(out);
        }
        return tempFile;
    }


    /**
     * Apk signature info
     *
     * @param targetApkFile apk File
     * @param verbose       more logs
     * @param escapeResult  if true escape result to html string else return a regular string
     * @return string contiaining apk signature info
     */
    private static String getApkSignatureInfo(File targetApkFile, boolean verbose, boolean escapeResult) {
        try {
            StringBuilder apkSignatureInfo = new StringBuilder("");
            AndroidApkSignerVerify verifier = new AndroidApkSignerVerify();
            AndroidApkSignerVerify.Result result = verifier.verify(targetApkFile, null, null, false);
            String logMsg;

            if (result.verified) {
                logMsg = "\t- signature verified " + result.getCertCountString() + result.getSchemaVersionInfoString();
            } else {
                logMsg = "\t- signature VERIFY FAILED (" + targetApkFile.getName() + ")";
            }

            if (!result.verified) {
                LOGGER.error(logMsg);
            } else {
                LOGGER.info(logMsg);
            }


            if (!result.errors.isEmpty()) {
                for (String e : result.errors) {
                    LOGGER.error("\t\t" + e);
                }
            }

            if (verbose && !result.warnings.isEmpty()) {
                for (String w : result.warnings) {
                    LOGGER.warn("\t\t" + w);
                }
            } else if (!result.warnings.isEmpty()) {
                LOGGER.warn("\t\t" + result.warnings.size() + " warnings");
            }

            if (result.verified) {
                for (int i = 0; i < result.certInfoList.size(); i++) {
                    AndroidApkSignerVerify.CertInfo certInfo = result.certInfoList.get(i);
                    apkSignatureInfo.append(certInfo.subjectDn).append("\n");
                    LOGGER.info("\t\t" + certInfo.subjectDn);
                    LOGGER.info("\t\tSHA256: " + certInfo.certSha256 + " / " + certInfo.sigAlgo);
                    if (verbose) {
                        LOGGER.info("\t\tSHA1: " + certInfo.certSha1);
                        apkSignatureInfo.append(certInfo.issuerDn).append("\n");
                        LOGGER.info("\t\t" + certInfo.issuerDn);
                        LOGGER.info("\t\tPublic Key SHA256: " + certInfo.pubSha256);
                        LOGGER.info("\t\tPublic Key SHA1: " + certInfo.pubSha1);
                        LOGGER.info("\t\tPublic Key Algo: " + certInfo.pubAlgo + " " + certInfo.pubKeysize);
                        apkSignatureInfo.append("Issue Date: ").append(certInfo.beginValidity).append("\n");
                        LOGGER.info("\t\tIssue Date: " + certInfo.beginValidity);

                    }
                    apkSignatureInfo.append("Expires: ").append(certInfo.expiry.toString()).append("\n");
                    LOGGER.info("\t\tExpires: " + certInfo.expiry.toString());

                    if (i < result.certInfoList.size() - 1) {
                        LOGGER.info("");
                    }
                }
            }
            if (escapeResult)
                return StringUtil.escape(apkSignatureInfo.toString());
            else
                return apkSignatureInfo.toString();
        } catch (Exception e) {
            LOGGER.error("could not verify " + targetApkFile + ": " + e.getMessage());
            return null;
        }
    }
}
