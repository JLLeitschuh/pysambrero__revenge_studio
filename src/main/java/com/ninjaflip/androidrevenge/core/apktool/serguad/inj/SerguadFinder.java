package com.ninjaflip.androidrevenge.core.apktool.serguad.inj;

import net.minidev.json.JSONObject;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Solitario on 16/10/2017.
 * <p>
 * Find admob methods in a smali project
 */
public class SerguadFinder {
    private String gmsFolderPath;
    private File adsFolder; // folder : ../decoded/smali/com/google/android/gms/ads

    public SerguadFinder(String projectFolderName) {
        this.gmsFolderPath = projectFolderName + File.separator + "decoded" + File.separator + "smali" + File.separator +
                "com" + File.separator + "google" + File.separator + "android" + File.separator + "gms";
        adsFolder = new File(gmsFolderPath + File.separator + "ads");
    }

    /**
     * Check if Ads package is obfuscated or not
     *
     * @return true if package is obfuscated false if not
     * @throws IOException
     */
    public boolean isAdsPackageObfuscated() throws IOException {
        if (!adsFolder.exists() || !adsFolder.isDirectory()) {
            System.out.println(adsFolder.getPath() + " not exist or not a folder");
            throw new FileNotFoundException(adsFolder.getPath() + " not exist or not a folder");
        }

        FileInputStream intersIs = null;
        FileInputStream adViewIs = null;

        try {
            File intersAdFile = new File(adsFolder + File.separator + "InterstitialAd.smali");
            File adViewFile = new File(adsFolder + File.separator + "AdView.smali");

            // check files exist
            if (!intersAdFile.exists() || !adViewFile.exists()) {
                return true;
            }

            // check methods exist (InterstitialAd.setAdUnit(String), AdView.setAdUnit(String))
            intersIs = new FileInputStream(intersAdFile);
            adViewIs = new FileInputStream(adViewFile);

            String intersStringContent = IOUtils.toString(intersIs, "UTF-8");
            String adViewStringContent = IOUtils.toString(adViewIs, "UTF-8");

            if (methodExistInsideClass(intersStringContent, "setAdUnitId", "Ljava/lang/String;", "V")
                    && methodExistInsideClass(adViewStringContent, "setAdUnitId", "Ljava/lang/String;", "V")) {
                return false;
            } else {
                return true;
            }
        } finally {
            if (intersIs != null) {
                try {
                    intersIs.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
            if (adViewIs != null) {
                try {
                    adViewIs.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }

    /**
     * Locate InterstitialAd class inside an obfuscated project and its AdUnitId setter method
     *
     * @return JSON object containing the located class name and method name
     * @throws IOException
     */
    public JSONObject locateIntersData() throws IOException {
        JSONObject result = new JSONObject();

        if (!adsFolder.exists() || !adsFolder.isDirectory()) {
            System.out.println(adsFolder.getPath() + " not exist or not a folder");
            return null;
        }
        // first check if InterstitialAd.smali exists (in case if obfuscation done only for methods)
        File intersFile = new File(adsFolder, "InterstitialAd.smali");
        if (intersFile.exists() && intersFile.isFile()) {
            result.put("className", "InterstitialAd");
            // then search for setAdUnitId method
            FileInputStream is = null;
            try {
                is = new FileInputStream(intersFile);
                String content = IOUtils.toString(is, "UTF-8");
                // before using regex search, check if InterstitialAd.smali contains method setAdUnitId (in case if obfuscation done only for file names not methods)
                if (methodExistInsideClass(content, "setAdUnitId", "Ljava/lang/String;", "V")) {
                    result.put("setIdMethodName", "setAdUnitId");
                    return result;
                } else {
                    // regex search for a method that takes a string parameter and return void
                    // if found more than one then do nothing => return null
                    List<String> methods = methodProjectionInsideClass(content, "Ljava/lang/String;", "V");
                    if (methods.size() == 1) {
                        result.put("setIdMethodName", methods.get(0));
                        return result;
                    } else {
                        return null;
                    }
                }
            } finally {
                if (is != null) {
                    IOUtils.closeQuietly(is);
                }
            }
        } else {
            /* InterstitialAd.smali file not found => we will guess it based on some criteria using its methods signatures and its super class
            our search criteria are:
            # InterstitialAd is a child of Object (Ljava/lang/Object;)
            # InterstitialAd must have a constructor .method public constructor <init>(Landroid/content/Context;)V
            # InterstitialAd must have a method (obfuscated or not) called "setAdUnitId": .method public final setAdUnitId(Ljava/lang/String;)V
            # InterstitialAd may have a method (obfuscated or not) called "show":  .method public final show()V
            # InterstitialAd may have a method (obfuscated or not) called "isLoaded":  .method public final isLoaded()Z
             */
            List<JSONObject> suspects = new ArrayList<>();
            File[] files = adsFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        FileInputStream is = null;
                        try {
                            is = new FileInputStream(file);
                            String content = IOUtils.toString(is, "UTF-8");
                            String regex = "\\.class[\\s\\w]*\\sLcom\\/google\\/android\\/gms\\/ads\\/(.+?);\\s+\\.super\\s+Ljava\\/lang\\/Object;";
                            Pattern pattern = Pattern.compile(regex);
                            Matcher matcher = pattern.matcher(content);
                            if (matcher.find()) {
                                if (methodExistInsideClass(content, "constructor <init>", "Landroid/content/Context;", "V")) {
                                    List<String> suspectedSetAdUnitMethods = methodProjectionInsideClass(content, "Ljava/lang/String;", "V");
                                    List<String> suspectedShowMethods = methodProjectionInsideClass(content, "", "V");
                                    List<String> suspectedIsLoadedMethods = methodProjectionInsideClass(content, "", "Z");

                                    if (suspectedSetAdUnitMethods.size() > 0 && suspectedShowMethods.size() > 0 && suspectedIsLoadedMethods.size() > 0) {
                                        JSONObject suspect = new JSONObject();
                                        suspect.put("class", matcher.group(1));
                                        suspect.put("path", file.getPath());
                                        suspect.put("setIdMethodNames", suspectedSetAdUnitMethods);
                                        suspects.add(suspect);
                                    }
                                }
                            }
                        } finally {
                            if (is != null) {
                                IOUtils.closeQuietly(is);
                            }
                        }
                    }
                }

                if (suspects.size() == 1) {
                    JSONObject suspect = suspects.get(0);
                    List<String> suspectedSetIdMethods = (List<String>) suspect.get("setIdMethodNames");
                    if (suspectedSetIdMethods.size() == 0) {
                        return null;
                    } else if (suspectedSetIdMethods.size() == 1) {
                        result.put("className", suspect.getAsString("class"));
                        result.put("setIdMethodName", suspectedSetIdMethods.get(0));
                        return result;
                    } else {// found more than two methods : .method public final methodName(Ljava/lang/String;)V
                        // we are going to dive deep into the code and look for our happiness
                        FileInputStream is = null;
                        FileInputStream zzClassFileInputStream = null;
                        try {
                            File file = new File(suspect.getAsString("path"));
                            is = new FileInputStream(file);
                            String content = IOUtils.toString(is, "UTF-8");

                            for (String methodName : suspectedSetIdMethods) {
                                String regex = "(?s)\\.method[\\w\\s]*\\s" + Pattern.quote(methodName) + "\\(" + Pattern.quote("Ljava/lang/String;") + "\\)" + Pattern.quote("V")
                                        + "(.*?)\\.end method";
                                Pattern pattern = Pattern.compile(regex);
                                Matcher matcher = pattern.matcher(content);
                                if (matcher.find()) {
                                    String methodBody = matcher.group(1);
                                    if (methodBody.toLowerCase().contains("The ad unit ID can only be set once".toLowerCase())) {
                                        result.put("className", suspect.getAsString("class"));
                                        result.put("setIdMethodName", methodName);
                                        return result;
                                    } else {
                                        String regexExternalCall = "invoke-virtual \\{.+?\\}, Lcom\\/google\\/android\\/gms\\/(ads\\/internal|internal)\\/(.+?);->(.+?)\\(Ljava\\/lang\\/String;\\)V";
                                        Pattern patternExternalCall = Pattern.compile(regexExternalCall);
                                        Matcher matcherExternalCall = patternExternalCall.matcher(matcher.group(0));

                                        if (matcherExternalCall.find()) {
                                            String[] match1Split = matcherExternalCall.group(1).split("/");
                                            String internalFolderPath = gmsFolderPath + File.separator + String.join(File.separator, match1Split);

                                            String[] match2Split = matcherExternalCall.group(2).split("/");
                                            String zzClassPath = internalFolderPath + File.separator + String.join(File.separator, match2Split) + ".smali";

                                            String calledMethod = matcherExternalCall.group(3);
                                            File zzClass = new File(zzClassPath);

                                            if (zzClass.exists()) {
                                                zzClassFileInputStream = new FileInputStream(zzClass);
                                                String contentZzClass = IOUtils.toString(zzClassFileInputStream);
                                                String regexZz = "(?s)\\.method[\\w\\s]*\\s" + Pattern.quote(calledMethod) + "\\(" + Pattern.quote("Ljava/lang/String;") + "\\)" + Pattern.quote("V")
                                                        + "(.*?)\\.end method";
                                                Pattern patternZz = Pattern.compile(regexZz);
                                                Matcher matcherZz = patternZz.matcher(contentZzClass);
                                                if (matcherZz.find()) {
                                                    if (matcherZz.group(0).toLowerCase().contains("The ad unit ID can only be set once".toLowerCase())) {
                                                        result.put("className", suspect.getAsString("class"));
                                                        result.put("setIdMethodName", calledMethod);
                                                        return result;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } finally {
                            if (is != null) {
                                IOUtils.closeQuietly(is);
                            }
                            if (zzClassFileInputStream != null) {
                                IOUtils.closeQuietly(zzClassFileInputStream);
                            }
                        }
                    }
                }
            }
            return null;
        }
    }


    /**
     * Locate InterstitialAd and return its file path
     */
    public String locateIntersClassPath() throws IOException {

        if (!adsFolder.exists() || !adsFolder.isDirectory()) {
            System.out.println(adsFolder.getPath() + " not exist or not a folder");
            return null;
        }
        // first check if InterstitialAd.smali exists (in case if obfuscation done only for methods)
        File intersFile = new File(adsFolder, "InterstitialAd.smali");
        if (intersFile.exists() && intersFile.isFile()) {
            return intersFile.getPath();
        } else {
            /* InterstitialAd.smali file not found => we will guess it based on some criteria using its methods signatures and its super class
            our search criteria are:
            # InterstitialAd is a child of Object (Ljava/lang/Object;)
            # InterstitialAd must have a constructor .method public constructor <init>(Landroid/content/Context;)V
            # InterstitialAd must have a method (obfuscated or not) called "setAdUnitId": .method public final setAdUnitId(Ljava/lang/String;)V
            # InterstitialAd may have a method (obfuscated or not) called "show":  .method public final show()V
            # InterstitialAd may have a method (obfuscated or not) called "isLoaded":  .method public final isLoaded()Z
             */
            List<JSONObject> suspects = new ArrayList<>();
            File[] files = adsFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        FileInputStream is = null;
                        try {
                            is = new FileInputStream(file);
                            String content = IOUtils.toString(is, "UTF-8");
                            String regex = "\\.class[\\s\\w]*\\sLcom\\/google\\/android\\/gms\\/ads\\/(.+?);\\s+\\.super\\s+Ljava\\/lang\\/Object;";
                            Pattern pattern = Pattern.compile(regex);
                            Matcher matcher = pattern.matcher(content);
                            if (matcher.find()) {
                                if (methodExistInsideClass(content, "constructor <init>", "Landroid/content/Context;", "V")) {
                                    List<String> suspectedSetAdUnitMethods = methodProjectionInsideClass(content, "Ljava/lang/String;", "V");
                                    List<String> suspectedShowMethods = methodProjectionInsideClass(content, "", "V");
                                    List<String> suspectedIsLoadedMethods = methodProjectionInsideClass(content, "", "Z");

                                    if (suspectedSetAdUnitMethods.size() > 0 && suspectedShowMethods.size() > 0 && suspectedIsLoadedMethods.size() > 0) {
                                        JSONObject suspect = new JSONObject();
                                        suspect.put("class", matcher.group(1));
                                        suspect.put("path", file.getPath());
                                        suspect.put("setIdMethodNames", suspectedSetAdUnitMethods);
                                        suspects.add(suspect);
                                    }
                                }
                            }
                        } finally {
                            if (is != null) {
                                IOUtils.closeQuietly(is);
                            }
                        }
                    }
                }

                if (suspects.size() == 1) {
                    JSONObject suspect = suspects.get(0);
                    List<String> suspectedSetIdMethods = (List<String>) suspect.get("setIdMethodNames");
                    if (suspectedSetIdMethods.size() == 0) {
                        return null;
                    } else if (suspectedSetIdMethods.size() == 1) {
                        return suspect.getAsString("path");
                    } else {// found more than two methods : .method public final methodName(Ljava/lang/String;)V
                        // we are going to dive deep into the code and look for our happiness
                        FileInputStream is = null;
                        FileInputStream zzClassFileInputStream = null;
                        try {
                            File file = new File(suspect.getAsString("path"));
                            is = new FileInputStream(file);
                            String content = IOUtils.toString(is, "UTF-8");

                            for (String methodName : suspectedSetIdMethods) {
                                String regex = "(?s)\\.method[\\w\\s]*\\s" + Pattern.quote(methodName) + "\\(" + Pattern.quote("Ljava/lang/String;") + "\\)" + Pattern.quote("V")
                                        + "(.*?)\\.end method";
                                Pattern pattern = Pattern.compile(regex);
                                Matcher matcher = pattern.matcher(content);
                                if (matcher.find()) {
                                    String methodBody = matcher.group(1);
                                    if (methodBody.toLowerCase().contains("The ad unit ID can only be set once".toLowerCase())) {
                                        return suspect.getAsString("path");
                                    } else {
                                        String regexExternalCall = "invoke-virtual \\{.+?\\}, Lcom\\/google\\/android\\/gms\\/(ads\\/internal|internal)\\/(.+?);->(.+?)\\(Ljava\\/lang\\/String;\\)V";
                                        Pattern patternExternalCall = Pattern.compile(regexExternalCall);
                                        Matcher matcherExternalCall = patternExternalCall.matcher(matcher.group(0));

                                        if (matcherExternalCall.find()) {
                                            String[] match1Split = matcherExternalCall.group(1).split("/");
                                            String internalFolderPath = gmsFolderPath + File.separator + String.join(File.separator, match1Split);

                                            String[] match2Split = matcherExternalCall.group(2).split("/");
                                            String zzClassPath = internalFolderPath + File.separator + String.join(File.separator, match2Split) + ".smali";

                                            String calledMethod = matcherExternalCall.group(3);
                                            File zzClass = new File(zzClassPath);

                                            if (zzClass.exists()) {
                                                zzClassFileInputStream = new FileInputStream(zzClass);
                                                String contentZzClass = IOUtils.toString(zzClassFileInputStream);
                                                String regexZz = "(?s)\\.method[\\w\\s]*\\s" + Pattern.quote(calledMethod) + "\\(" + Pattern.quote("Ljava/lang/String;") + "\\)" + Pattern.quote("V")
                                                        + "(.*?)\\.end method";
                                                Pattern patternZz = Pattern.compile(regexZz);
                                                Matcher matcherZz = patternZz.matcher(contentZzClass);
                                                if (matcherZz.find()) {
                                                    if (matcherZz.group(0).toLowerCase().contains("The ad unit ID can only be set once".toLowerCase())) {
                                                        return suspect.getAsString("path");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } finally {
                            if (is != null) {
                                IOUtils.closeQuietly(is);
                            }
                            if (zzClassFileInputStream != null) {
                                IOUtils.closeQuietly(zzClassFileInputStream);
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

    /**
     * Locate AdView class inside an obfuscated project and its AdUnitId setter method
     *
     * @return JSON object containing the located class name and method name
     * @throws IOException
     */
    public JSONObject locateBnrData() throws IOException {
        JSONObject result = new JSONObject();
        if (!adsFolder.exists() || !adsFolder.isDirectory()) {
            System.out.println(adsFolder.getPath() + " not exist or not a folder");
            return null;
        }
        // first check if AdView.smali exists (in case if obfuscation done only for methods)
        File adViewFile = new File(adsFolder, "AdView.smali");
        if (adViewFile.exists() && adViewFile.isFile()) {
            result.put("className", "AdView");
            // then search for setAdUnitId method
            FileInputStream is = null;
            try {
                is = new FileInputStream(adViewFile);
                String content = IOUtils.toString(is, "UTF-8");
                // before using regex search, check if AdView.smali contains method setAdUnitId (in case if obfuscation done only for file names not methods)
                if (methodExistInsideClass(content, "setAdUnitId", "Ljava/lang/String;", "V")) {
                    result.put("setIdMethodName", "setAdUnitId");
                    return result;
                } else {
                    // regex search for a method that takes a string parameter and return void
                    // if found more than one then do nothing => return null
                    List<String> methods = methodProjectionInsideClass(content, "Ljava/lang/String;", "V");
                    if (methods.size() == 1) {
                        result.put("setIdMethodName", methods.get(0));
                        return result;
                    } else {
                        return null;
                    }
                }
            } finally {
                if (is != null) {
                    IOUtils.closeQuietly(is);
                }
            }
        } else {
            /* AdView.smali file not found => we will guess it based on some criteria using its methods signatures and its super class BaseAdView
            # AdView is a child of BaseAdView and BaseAdView extends ViewGroup
            As BaseAdView is the only class in that package that extends ViewGroup, so our search will be based on that fact.
            we search for BaseAdView we get its class name and then we search for classes that extends it and has methods below => result is AdView
             */
            File[] files = adsFolder.listFiles();

            // step 1: search for BaseAdView
            String baseAdViewClassName = null;
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        FileInputStream is = null;
                        try {
                            is = new FileInputStream(file);
                            String content = IOUtils.toString(is, "UTF-8");
                            String regex = "\\.class[\\s\\w]*\\sLcom\\/google\\/android\\/gms\\/ads\\/(.+?);\\s+\\.super\\s+Landroid\\/view\\/ViewGroup;";
                            Pattern pattern = Pattern.compile(regex);
                            Matcher matcher = pattern.matcher(content);

                            if (matcher.find()) {
                                List<String> suspectedSetAdUnitMethods = methodProjectionInsideClass(content, "Ljava/lang/String;", "V");
                                List<String> suspectedOnMeasureMethods = methodProjectionInsideClass(content, "II", "V");
                                List<String> suspectedOnLayoutMethods = methodProjectionInsideClass(content, "ZIIII", "V");
                                if (suspectedSetAdUnitMethods.size() > 0 && suspectedOnMeasureMethods.size() > 0 && suspectedOnLayoutMethods.size() > 0) {
                                    baseAdViewClassName = matcher.group(1);
                                    break;
                                }
                            }
                        } finally {
                            if (is != null) {
                                IOUtils.closeQuietly(is);
                            }
                        }
                    }
                }

                // step 2: search for AdView
                if (baseAdViewClassName == null) {
                    return null;
                } else { // BaseAdView was found
                    /*
                    Let's search for classes that extends BaseAdView and satisfy few criteria based on their methods
                    */
                    for (File file : files) {
                        if (file.isFile()) {
                            FileInputStream is = null;
                            try {
                                is = new FileInputStream(file);
                                String content = IOUtils.toString(is, "UTF-8");
                                String regex = "\\.class[\\s\\w]*\\sLcom\\/google\\/android\\/gms\\/ads\\/(.+?);\\s+\\.super\\s+Lcom\\/google\\/android\\/gms\\/ads\\/" + baseAdViewClassName + ";";
                                Pattern pattern = Pattern.compile(regex);
                                Matcher matcher = pattern.matcher(content);
                                if (matcher.find()) {
                                    /*
                                    # AdView class has 3 possible constructors but they may not be implemented all at once:
                                    .method public constructor <init>(Landroid/content/Context;)V
                                    .method public constructor <init>(Landroid/content/Context;Landroid/util/AttributeSet;)V
                                    .method public constructor <init>(Landroid/content/Context;Landroid/util/AttributeSet;I)V
                                    # AdView class has setAdUnitId method:
                                    .method public final bridge synthetic setAdUnitId(Ljava/lang/String;)V
                                     */
                                    if (methodExistInsideClass(content, "constructor <init>", "Landroid/content/Context;", "V")
                                            || methodExistInsideClass(content, "constructor <init>", "Landroid/content/Context;Landroid/util/AttributeSet;", "V")
                                            || methodExistInsideClass(content, "constructor <init>", "Landroid/content/Context;Landroid/util/AttributeSet;I", "V")) {

                                        // before using regex search, check if class contains method setAdUnitId (in case if obfuscation done only for file names not methods)
                                        if (methodExistInsideClass(content, "setAdUnitId", "Ljava/lang/String;", "V")) {
                                            result.put("className", matcher.group(1));
                                            result.put("setIdMethodName", "setAdUnitId");
                                            return result;
                                        } else {
                                            // regex search for a method that takes a string parameter and return void
                                            // if found more than one then do nothing => return null
                                            List<String> suspectedSetAdUnitMethods = methodProjectionInsideClass(content, "Ljava/lang/String;", "V");
                                            if (suspectedSetAdUnitMethods.size() == 1) {
                                                result.put("className", matcher.group(1));
                                                result.put("setIdMethodName", suspectedSetAdUnitMethods.get(0));
                                                return result;
                                            } else {
                                                return null;
                                            }
                                        }
                                    }
                                }
                            } finally {
                                if (is != null) {
                                    IOUtils.closeQuietly(is);
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }
    }


    /**
     * Locate AdView and return its file path
     */
    public String locateBnrClassPath() throws IOException {
        JSONObject result = new JSONObject();
        if (!adsFolder.exists() || !adsFolder.isDirectory()) {
            System.out.println(adsFolder.getPath() + " not exist or not a folder");
            return null;
        }
        // first check if AdView.smali exists (in case if obfuscation done only for methods)
        File adViewFile = new File(adsFolder, "AdView.smali");
        if (adViewFile.exists() && adViewFile.isFile()) {
            return adViewFile.getPath();
        } else {
            /* AdView.smali file not found => we will guess it based on some criteria using its methods signatures and its super class BaseAdView
            # AdView is a child of BaseAdView and BaseAdView extends ViewGroup
            As BaseAdView is the only class in that package that extends ViewGroup, so our search will be based on that fact.
            we search for BaseAdView we get its class name and then we search for classes that extends it and has methods below => result is AdView
             */
            File[] files = adsFolder.listFiles();

            // step 1: search for BaseAdView
            String baseAdViewClassName = null;
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        FileInputStream is = null;
                        try {
                            is = new FileInputStream(file);
                            String content = IOUtils.toString(is, "UTF-8");
                            String regex = "\\.class[\\s\\w]*\\sLcom\\/google\\/android\\/gms\\/ads\\/(.+?);\\s+\\.super\\s+Landroid\\/view\\/ViewGroup;";
                            Pattern pattern = Pattern.compile(regex);
                            Matcher matcher = pattern.matcher(content);

                            if (matcher.find()) {
                                List<String> suspectedSetAdUnitMethods = methodProjectionInsideClass(content, "Ljava/lang/String;", "V");
                                List<String> suspectedOnMeasureMethods = methodProjectionInsideClass(content, "II", "V");
                                List<String> suspectedOnLayoutMethods = methodProjectionInsideClass(content, "ZIIII", "V");
                                if (suspectedSetAdUnitMethods.size() > 0 && suspectedOnMeasureMethods.size() > 0 && suspectedOnLayoutMethods.size() > 0) {
                                    baseAdViewClassName = matcher.group(1);
                                    break;
                                }
                            }
                        } finally {
                            if (is != null) {
                                IOUtils.closeQuietly(is);
                            }
                        }
                    }
                }

                // step 2: search for AdView
                if (baseAdViewClassName == null) {
                    return null;
                } else { // BaseAdView was found
                    /*
                    Let's search for classes that extends BaseAdView and satisfy few criteria based on their methods
                    */
                    for (File file : files) {
                        if (file.isFile()) {
                            FileInputStream is = null;
                            try {
                                is = new FileInputStream(file);
                                String content = IOUtils.toString(is, "UTF-8");
                                String regex = "\\.class[\\s\\w]*\\sLcom\\/google\\/android\\/gms\\/ads\\/(.+?);\\s+\\.super\\s+Lcom\\/google\\/android\\/gms\\/ads\\/" + baseAdViewClassName + ";";
                                Pattern pattern = Pattern.compile(regex);
                                Matcher matcher = pattern.matcher(content);
                                if (matcher.find()) {
                                    /*
                                    # AdView class has 3 possible constructors but they may not be implemented all at once:
                                    .method public constructor <init>(Landroid/content/Context;)V
                                    .method public constructor <init>(Landroid/content/Context;Landroid/util/AttributeSet;)V
                                    .method public constructor <init>(Landroid/content/Context;Landroid/util/AttributeSet;I)V
                                    # AdView class has setAdUnitId method:
                                    .method public final bridge synthetic setAdUnitId(Ljava/lang/String;)V
                                     */
                                    if (methodExistInsideClass(content, "constructor <init>", "Landroid/content/Context;", "V")
                                            || methodExistInsideClass(content, "constructor <init>", "Landroid/content/Context;Landroid/util/AttributeSet;", "V")
                                            || methodExistInsideClass(content, "constructor <init>", "Landroid/content/Context;Landroid/util/AttributeSet;I", "V")) {

                                        // before using regex search, check if class contains method setAdUnitId (in case if obfuscation done only for file names not methods)
                                        if (methodExistInsideClass(content, "setAdUnitId", "Ljava/lang/String;", "V")) {
                                            return file.getPath();
                                        } else {
                                            // regex search for a method that takes a string parameter and return void
                                            // if found more than one then do nothing => return null
                                            List<String> suspectedSetAdUnitMethods = methodProjectionInsideClass(content, "Ljava/lang/String;", "V");
                                            if (suspectedSetAdUnitMethods.size() == 0) {
                                                return null;
                                            } else if (suspectedSetAdUnitMethods.size() == 1) {
                                                return file.getPath();
                                            } else {
                                                String ONE_OR_MORE_BLANK = "\\s+";
                                                String fakeMethodRegex = "\\.method[\\s\\w]*\\s(.+?)" + Pattern.quote("(Ljava/lang/String;)V") + ONE_OR_MORE_BLANK
                                                        + Pattern.quote(".locals 2") + ONE_OR_MORE_BLANK
                                                        + Pattern.quote("move-object v0, p0") + ONE_OR_MORE_BLANK
                                                        + Pattern.quote("move-object v1, p1") + ONE_OR_MORE_BLANK
                                                        + Pattern.quote("invoke-static {v0, v1}, L") + "(.+?)" + Pattern.quote(";->") + "(.+?)" + Pattern.quote("(Ljava/lang/Object;Ljava/lang/String;)V") + ONE_OR_MORE_BLANK
                                                        + Pattern.quote("return-void") + ONE_OR_MORE_BLANK
                                                        + Pattern.quote(".end method");

                                                Pattern patternFakeMethod = Pattern.compile(fakeMethodRegex);
                                                Matcher matcherFakeMethod = patternFakeMethod.matcher(content);
                                                if (matcherFakeMethod.find()) {
                                                    return file.getPath();
                                                }
                                            }
                                        }
                                    }
                                }
                            } finally {
                                if (is != null) {
                                    IOUtils.closeQuietly(is);
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

    /**
     * Detect launch date inside serguad file and update it with the corresponding date : today + delayDays * days
     *
     * @param serguadFilePath path to serguad file
     * @param delayDays       number of days before start sending getAdUnits request to server
     * @return true if done false otherwise
     * @throws IOException
     */
    public boolean updateSerguadLaunchDate(String serguadFilePath, int delayDays) throws IOException {
        FileInputStream is = null;
        InputStream stream = null;
        FileOutputStream out = null;

        try {
            File serguadFile = new File(serguadFilePath);
            is = new FileInputStream(new File(serguadFilePath));
            String content = IOUtils.toString(is, "UTF-8");

            List<String> suspectedIsLaunchDateReachedMethods = methodProjectionInsideClass(content, "", "Z");
            for (String suspectedMethod : suspectedIsLaunchDateReachedMethods) {
                String isLaunchDateReachedRegex = "(?s)\\.method[\\w\\s]+" + Pattern.quote(suspectedMethod) + "\\(\\)Z" + "(.*?)\\.end method";
                Pattern pattern = Pattern.compile(isLaunchDateReachedRegex);
                Matcher matcher = pattern.matcher(content);
                while (matcher.find()) {
                    String method = matcher.group(0);
                    String dateSetterLinesRegex = "const-string v\\d+, \"(\\d{1,19})\"\\s+"
                            + "invoke-static \\{v\\d+\\}, Ljava\\/lang\\/Long;->valueOf\\(Ljava\\/lang\\/String;\\)Ljava\\/lang\\/Long;";

                    Pattern patternDateSetter = Pattern.compile(dateSetterLinesRegex);
                    Matcher matcherDateSetter = patternDateSetter.matcher(method);
                    if (matcherDateSetter.find()) {
                        String launchDate = matcherDateSetter.group(1);
                        long newLaunchDate = System.currentTimeMillis() + (1000 * 60 * 60 * 24 * delayDays);
                        String newMethod = method.replace(launchDate, String.valueOf(newLaunchDate));
                        String newContent = content.replace(method, newMethod);

                        stream = new ByteArrayInputStream(newContent.getBytes("UTF-8"));
                        out = new FileOutputStream(serguadFile);
                        IOUtils.copyLarge(stream, out);
                        return true;
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(stream);
            IOUtils.closeQuietly(out);
        }
        return false;
    }

    /**
     * @return JSON Object containing InterstitialAd's file path and class name and show method name
     */
    public JSONObject findInterstitialAdData() throws IOException {
        JSONObject result = new JSONObject();
        boolean found = false;
        if (!adsFolder.exists() || !adsFolder.isDirectory()) {
            System.out.println(adsFolder.getPath() + " not exist or not a folder");
            return result;
        }

        File[] files = adsFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileNameWithoutExtension = file.getName().replace(".smali", "");
                    String firstLine = ".method public\\s+(final|)\\s*(.+?)\\(\\)V";
                    String secondLine = ".locals 1";
                    String thirdLine = "iget-object v0, p0, Lcom\\/google\\/android\\/gms\\/ads\\/" + fileNameWithoutExtension + ";->(.+?):Lcom\\/google\\/android\\/gms\\/(ads\\/internal|internal)\\/(.+?);";
                    String fourthLine = "invoke-virtual \\{v0\\}, Lcom\\/google\\/android\\/gms\\/(ads\\/internal|internal)\\/(.+?);->(.+?)\\(\\)V";
                    String fifthLine = "return-void";
                    String lastLine = ".end method";

                    String ONE_OR_MORE_BLANK = "\\s+";

                    String REGEX = firstLine + ONE_OR_MORE_BLANK +
                            secondLine + ONE_OR_MORE_BLANK +
                            thirdLine + ONE_OR_MORE_BLANK +
                            fourthLine + ONE_OR_MORE_BLANK +
                            fifthLine + ONE_OR_MORE_BLANK +
                            lastLine;


                    FileInputStream currentFileInputStream = new FileInputStream(file);
                    String content = IOUtils.toString(currentFileInputStream);
                    Pattern aPattern = Pattern.compile(REGEX);
                    Matcher aMatcher = aPattern.matcher(content);

                    while (aMatcher.find()) {
                        /*System.out.println("mmmm = "+ aMatcher.group(0));
                        for(int i =0; i< aMatcher.groupCount(); i++){
                            System.out.println((i+1)+ " ===> "+ aMatcher.group(i+1));
                        }*/
                        if (aMatcher.group(2).equals(aMatcher.group(8)) && aMatcher.group(4).equals(aMatcher.group(6)) && aMatcher.group(5).equals(aMatcher.group(7))) {
                            /*
                             * aMatcher.group(0) contain the whole matched string
                             * aMatcher.group(2) contain the method name
                             */
                            String[] match4 = aMatcher.group(4).split("/");
                            String internalFolderPath = gmsFolderPath + File.separator + String.join(File.separator, match4);

                            String[] match5 = aMatcher.group(5).split("/");
                            String zzzClassPath = internalFolderPath + File.separator + String.join(File.separator, match5) + ".smali";

                            String methodName = aMatcher.group(2);
                            File zzzClass = new File(zzzClassPath);

                            if (zzzClass.exists()) {
                                FileInputStream zzzClassFileInputStream = new FileInputStream(zzzClass);
                                String contentZzzClass = IOUtils.toString(zzzClassFileInputStream);

                                String regexShow = "(?s).method public\\s+(final|)\\s*" + methodName + "\\(\\)V(.*?)const-string v0, \"show\"(.*?).end method";
                                Pattern patternSow = Pattern.compile(regexShow);

                                Matcher showMatcher = patternSow.matcher(contentZzzClass);
                                if (showMatcher.find()) {
                                    //System.out.println("FOUND SHOW METHOD\n" + showMatcher.group(0));
                                    result.put("intersFilePath", file.getPath());
                                    result.put("intersClassName", file.getName().replace(".smali", ""));
                                    result.put("intersShowMethodName", methodName);
                                    found = true;
                                    break;
                                }
                                zzzClassFileInputStream.close();
                            }
                        }
                    }
                    currentFileInputStream.close();
                }
            }
        }
        if (found) {
            return result;
        } else {
            return null;
        }
    }

    /**
     * Find interstitialAd.loadAd() method name, AdRequest file path, AdRequest class name,
     * AdRequestBuilder class name, AdRequestBuilder file path and AdRequestBuilder.build() method name
     *
     * @return a json object containing data above
     */
    public JSONObject findAdRequestData(String intersFilePath, String intersAdClassName) throws IOException {
        File file = new File(intersFilePath);
        boolean found = false;
        JSONObject result = new JSONObject();
        if (!file.exists() || !file.isFile()) {
            System.out.println(intersFilePath + " not exist or not a folder");
            return null;
        }

        String firstLine = ".method public\\s+(final|)\\s*(.+?)\\(Lcom\\/google\\/android\\/gms\\/ads\\/(.+?);\\)V";
        String secondLine = ".locals 2";
        String secondLineOptionalPermission = "((.annotation((.+?)\\s+)*?.end annotation\\s+)?|(.param p1((.+?)\\s+)*?.prologue\\s+)?)";
        String thirdLine = "iget-object v0, p0, Lcom\\/google\\/android\\/gms\\/ads\\/" + intersAdClassName + ";->(.+?):Lcom\\/google\\/android\\/gms\\/(ads\\/internal|internal)\\/(.+?);";
        String fourthLine = "invoke-virtual \\{p1\\}, Lcom\\/google\\/android\\/gms\\/ads\\/(.+?);->(.+?)Lcom\\/google\\/android\\/gms\\/(ads\\/internal|internal)\\/(.+?);";
        String fifthLine = "move-result-object v1";
        String sixthLine = "invoke-virtual \\{v0, v1\\}, Lcom\\/google\\/android\\/gms\\/(ads\\/internal|internal)\\/(.+?);->(.+?)\\(Lcom\\/google\\/android\\/gms\\/(ads\\/internal|internal)\\/(.+?);\\)V";
        String seventhLine = "return-void";
        String lastLine = ".end method";

        String ONE_OR_MORE_BLANK = "\\s+";

        String REGEX = firstLine + ONE_OR_MORE_BLANK +
                secondLine + ONE_OR_MORE_BLANK +
                secondLineOptionalPermission +
                thirdLine + ONE_OR_MORE_BLANK +
                fourthLine + ONE_OR_MORE_BLANK +
                fifthLine + ONE_OR_MORE_BLANK +
                sixthLine + ONE_OR_MORE_BLANK +
                seventhLine + ONE_OR_MORE_BLANK +
                lastLine;

        FileInputStream currentFileInputStream = new FileInputStream(file);
        String content = IOUtils.toString(currentFileInputStream);
        Pattern aPattern = Pattern.compile(REGEX);
        Matcher aMatcher = aPattern.matcher(content);

        /*
        matcher 2 = loadAd method name inside file
        matcher 3 = AdRequest class name inside file
         */
        while (aMatcher.find()) {
            if (aMatcher.group(3).equals(aMatcher.group(14))
                    && aMatcher.group(12).equals(aMatcher.group(16))
                    && aMatcher.group(16).equals(aMatcher.group(18))
                    && aMatcher.group(18).equals(aMatcher.group(21))
                    && aMatcher.group(17).equals(aMatcher.group(22))
                    && aMatcher.group(13).equals(aMatcher.group(19))) {


                result.put("intersLoadAdMethodName", aMatcher.group(2));
                result.put("AdRequestClassName", aMatcher.group(3));
                //System.out.println("intersLoadAdMethodName ===> " + aMatcher.group(2));
                //System.out.println("AdRequestClassName ===> " + aMatcher.group(3));

                /*System.out.println("FOUND loadAd()\n"+ aMatcher.group(0));
                for(int i =0; i< aMatcher.groupCount(); i++){
                    System.out.println((i+1)+ " ===> "+ aMatcher.group(i+1));
                }*/

                // find builder class and build method
                File[] files = adsFolder.listFiles();
                if (files != null) {

                    // start first search strategy search "AdRequest.Builder class contains _emulatorLiveAds"
                    boolean foundBuilder = false;
                    for (File f : files) {
                        if (f.isFile()) {
                            FileInputStream is = new FileInputStream(f);
                            String fContent = IOUtils.toString(is);
                            if (fContent.contains("_emulatorLiveAds")) {
                                String AdRequestBuilderClassName = f.getName().replace(".smali", "");
                                result.put("AdRequestBuilderClassName", AdRequestBuilderClassName);
                                result.put("AdRequestBuilderFilePath", f.getPath());
                                //System.out.println("AdRequestBuilderFilePath ===> " + f.getPath());
                                foundBuilder = true;
                            }
                            is.close();
                            if (foundBuilder) {
                                break;
                            }
                        }
                    }

                    if (!foundBuilder) { // second search strategy
                        long length = 0;
                        File AdRequestBuilderFile = null;
                        for (File f : files) {
                            if (f.isFile() && f.getName().startsWith(aMatcher.group(3) + "$")) {
                                if (f.length() > length) {
                                    AdRequestBuilderFile = f;
                                }
                            }
                        }

                        if (AdRequestBuilderFile != null) {
                            String AdRequestBuilderClassName = AdRequestBuilderFile.getName().replace(".smali", "");
                            result.put("AdRequestBuilderClassName", AdRequestBuilderClassName);
                            result.put("AdRequestBuilderFilePath", AdRequestBuilderFile.getPath());
                            //System.out.println("AdRequestBuilderFilePath ===> " + AdRequestBuilderFile.getPath());
                            foundBuilder = true;
                        }
                    }

                    // search Builder.build() method
                    if (foundBuilder) {
                        File adRequestBuilderFile = new File((String) result.get("AdRequestBuilderFilePath"));
                        FileInputStream is = new FileInputStream(adRequestBuilderFile);
                        String bContent = IOUtils.toString(is);

                        String adRequestClassName = (String) result.get("AdRequestClassName");
                        String adRequestBuilderClassName = (String) result.get("AdRequestBuilderClassName");

                        String l1 = ".method public\\s+(final|)\\s*(.+?)\\(\\)Lcom\\/google\\/android\\/gms\\/ads\\/" + adRequestClassName.replace("$", "\\$") + ";";
                        String l2 = ".locals 2";
                        String l3 = "new-instance v0, Lcom\\/google\\/android\\/gms\\/ads\\/" + adRequestClassName.replace("$", "\\$") + ";";
                        String l4 = "const\\/4 v1, 0x0";
                        String l5 = "invoke-direct \\{v0, p0, v1\\}, Lcom\\/google\\/android\\/gms\\/ads\\/" + adRequestClassName.replace("$", "\\$") + ";-><init>\\(Lcom\\/google\\/android\\/gms\\/ads\\/" + adRequestBuilderClassName.replace("$", "\\$") + ";(.+?)\\)V";
                        String l6 = "return-object v0";
                        String l7 = ".end method";

                        String REGEX_BUILDER_BUILD = l1 + ONE_OR_MORE_BLANK +
                                l2 + ONE_OR_MORE_BLANK +
                                l3 + ONE_OR_MORE_BLANK +
                                l4 + ONE_OR_MORE_BLANK +
                                l5 + ONE_OR_MORE_BLANK +
                                l6 + ONE_OR_MORE_BLANK +
                                l7;

                        Pattern bPattern = Pattern.compile(REGEX_BUILDER_BUILD);
                        //Pattern bPattern = Pattern.compile("(?i)" + Pattern.quote(REGEX_BUILDER_BUILD));
                        Matcher bMatcher = bPattern.matcher(bContent);

                        if (bMatcher.find()) {
                            //System.out.println("AdRequest.Builder.build() method is\n"+ bMatcher.group(0));
                            result.put("AdRequestBuilderBuildMethodName", bMatcher.group(2));
                            //System.out.println("AdRequestBuilderBuildMethodName ===> " + bMatcher.group(2));
                            found = true;
                            break;
                        }
                        is.close();
                    }
                }
            }
        }
        currentFileInputStream.close();

        if (found) {
            return result;
        } else {
            return null;
        }
    }

    /**
     * Find interstitialAd.setAdUnit() method
     *
     * @return setAdUnit method name
     */
    public String findSetAdUnitMethod(String interstitialAdFilePath, String interstitialAdClassName) throws IOException {
        File file = new File(interstitialAdFilePath);
        FileInputStream is = new FileInputStream(file);
        String bContent = IOUtils.toString(is);

        String l1 = ".method public\\s+(final|)\\s*(.+?)\\(Ljava\\/lang\\/String;\\)V";
        String l2 = ".locals 1";
        String l2_opt = "(.param p1((.+?)\\s+)*?.prologue\\s+)?";
        String l3 = "iget-object v0, p0, Lcom\\/google\\/android\\/gms\\/ads\\/" + interstitialAdClassName + ";->(.+?):Lcom\\/google\\/android\\/gms\\/(ads\\/internal|internal)\\/(.+?);";
        String l4 = "invoke-virtual \\{v0, p1\\}, Lcom\\/google\\/android\\/gms\\/(ads\\/internal|internal)\\/(.+?);->(.+?)\\(Ljava\\/lang\\/String;\\)V";
        String l5 = "return-void";
        String l6 = ".end method";

        String ONE_OR_MORE_BLANK = "\\s+";
        String REGEX = l1 + ONE_OR_MORE_BLANK +
                l2 + ONE_OR_MORE_BLANK +
                l2_opt + ONE_OR_MORE_BLANK +
                l3 + ONE_OR_MORE_BLANK +
                l4 + ONE_OR_MORE_BLANK +
                l5 + ONE_OR_MORE_BLANK +
                l6;
        Pattern bPattern = Pattern.compile(REGEX);
        Matcher bMatcher = bPattern.matcher(bContent);

        List<String> methods = new ArrayList<>();
        while (bMatcher.find()) {
            //System.out.println("InterstitialAd.setAdUnitId =============> \n" + bMatcher.group(0));
            methods.add(bMatcher.group(2));
        }
        is.close();

        if (methods.size() == 1) {
            return methods.get(0);
        } else {
            return null;
        }
    }

    /**
     * Find interstitialAd.isLoaded() method
     *
     * @return isLoaded method name
     */
    public String findIsLoadedMethod(String interstitialAdFilePath, String interstitialAdClassName) throws IOException {
        File file = new File(interstitialAdFilePath);
        FileInputStream is = new FileInputStream(file);
        String bContent = IOUtils.toString(is);

        String l1 = ".method public\\s+(final|)\\s*(.+?)\\(\\)Z";
        String l2 = ".locals 1";
        String l3 = "iget-object v0, p0, Lcom\\/google\\/android\\/gms\\/ads\\/" + interstitialAdClassName + ";->(.+?):Lcom\\/google\\/android\\/gms\\/(ads\\/internal|internal)\\/(.+?);";
        String l4 = "invoke-virtual \\{v0\\}, Lcom\\/google\\/android\\/gms\\/(ads\\/internal|internal)\\/(.+?);->(.+?)\\(\\)Z";
        String l5 = "move-result v0";
        String l6 = "return v0";
        String l7 = ".end method";

        String ONE_OR_MORE_BLANK = "\\s+";
        String REGEX = l1 + ONE_OR_MORE_BLANK +
                l2 + ONE_OR_MORE_BLANK +
                l3 + ONE_OR_MORE_BLANK +
                l4 + ONE_OR_MORE_BLANK +
                l5 + ONE_OR_MORE_BLANK +
                l6 + ONE_OR_MORE_BLANK +
                l7;
        Pattern bPattern = Pattern.compile(REGEX);
        Matcher bMatcher = bPattern.matcher(bContent);


        List<JSONObject> hits = new ArrayList<>();
        while (bMatcher.find()) {
            //System.out.println("InterstitialAd.isLoaded() =============> \n" + bMatcher.group(0));
            /*for (int i = 0; i < bMatcher.groupCount(); i++) {
                System.out.println((i + 1) + " =======> " + bMatcher.group(i + 1));
            }*/
            String[] match4 = bMatcher.group(4).split("/");
            String internalFolderPath = gmsFolderPath + File.separator + String.join(File.separator, match4);

            String[] match5 = bMatcher.group(5).split("/");
            String internalClassPath = internalFolderPath + File.separator + String.join(File.separator, match5) + ".smali";

            String methodName = bMatcher.group(2);
            //System.out.println(" isLoaded() path =============> " + internalClassPath);
            JSONObject matchedElement = new JSONObject();
            matchedElement.put("methodName", methodName);
            matchedElement.put("internalClassPath", internalClassPath);

            hits.add(matchedElement);
        }
        is.close();

        if (hits.size() == 0) {
            return null;
        } else if (hits.size() == 1) {
            return (String) hits.get(0).get("methodName");
        } else {
            for (int i = 0; i < hits.size(); i++) {
                String methodName = (String) hits.get(i).get("methodName");
                String internalClassPath = (String) hits.get(i).get("internalClassPath");
                File internalClass = new File(internalClassPath);

                if (internalClass.exists()) {
                    FileInputStream internalClassFileInputStream = new FileInputStream(internalClass);
                    String contentInternalClass = IOUtils.toString(internalClassFileInputStream);

                    String regexIsLoaded = "(?s).method public\\s+(final|)\\s*" + methodName + "\\(\\)Z(.*?)const-string v(.?), \"Failed to check if ad is ready.\"(.*?).end method";
                    Pattern patternIsLoaded = Pattern.compile(regexIsLoaded);

                    //System.out.println(" isLoaded() regex =============> " + regexIsLoaded);
                    internalClassFileInputStream.close();

                    Matcher isLoadedMatcher = patternIsLoaded.matcher(contentInternalClass);
                    if (isLoadedMatcher.find()) {
                        //System.out.println("FOUND IS_LOADED METHOD\n" + isLoadedMatcher.group(0));
                        return methodName;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Check if a certain method signature exists in a smali fila
     *
     * @param classStringContent the string content where search happen
     * @param methodName         the method name
     * @param methodParameters   its parameters
     * @param methodReturnType   its return type
     * @return true if found false if not
     */
    private boolean methodExistInsideClass(String classStringContent, String methodName, String methodParameters, String methodReturnType) {
        String regexCheckMethod = "(?s)\\.method[\\w\\s]*\\s" + Pattern.quote(methodName) + "\\(" + Pattern.quote(methodParameters) + "\\)" + Pattern.quote(methodReturnType)
                + "(.*?)\\.end method";
        Pattern patternCheckMethod = Pattern.compile(regexCheckMethod);
        Matcher matcher = patternCheckMethod.matcher(classStringContent);
        return matcher.find();
    }

    /**
     * Search for a method using only its parameters and return type
     *
     * @param classStringContent the string content where search happen
     * @param methodParameters   method parameter
     * @param methodReturnType   ethod retun type
     * @return a list of method names that matched the search
     */
    private List<String> methodProjectionInsideClass(String classStringContent, String methodParameters, String methodReturnType) {
        List<String> methodNames = new ArrayList<>();
        String regexCheckMethod = "\\.method[\\s\\w]*\\s(.+?)\\(" + Pattern.quote(methodParameters) + "\\)" + Pattern.quote(methodReturnType);
        Pattern patternCheckMethod = Pattern.compile(regexCheckMethod);
        Matcher matcher = patternCheckMethod.matcher(classStringContent);

        while (matcher.find()) {
            methodNames.add(matcher.group(1));
        }
        return methodNames;
    }
}