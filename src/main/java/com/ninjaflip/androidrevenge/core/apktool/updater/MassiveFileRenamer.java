package com.ninjaflip.androidrevenge.core.apktool.updater;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.apktool.ApkToolsManager;
import com.ninjaflip.androidrevenge.core.apktool.filecomputing.FileComputingManager;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.enums.RenamingPolicy;
import com.ninjaflip.androidrevenge.exceptions.XmlPullParserException;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.xml.sax.SAXException;

import javax.script.ScriptException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Solitario on 02/06/2017.
 * <p>
 * Check out the manifest, read all names of activities, service and receivers, replace their occurrences wherever found in text files.
 * Then replace file names.
 * Anti-detect duplicated apk code inside google play store
 */
public class MassiveFileRenamer {

    private static final Logger LOGGER = Logger.getLogger(MassiveFileRenamer.class);

    private String userUuid;
    private String projectFolderNameUuid;
    private boolean isTemporary;

    // package name
    private String packageName = null;
    // contains activities canonical class names, and their renaming simple class names
    private List<RenamingObject> activities = new ArrayList<RenamingObject>();
    // contains services canonical class names, and their renaming simple class names
    private List<RenamingObject> services = new ArrayList<RenamingObject>();
    // contains receivers canonical class names, and their renaming simple class names
    private List<RenamingObject> receivers = new ArrayList<RenamingObject>();

    /**
     * a map containing Key: classFullName(packageName.classSimpleName), value: newClassSimpleName
     * Thus, "packageName.classSimpleName" will be renamed to "packageName.newClassSimpleName"
     */
    private Map<String, String> customNames = new HashMap<>();
    private RenamingPolicy renamingPolicy = RenamingPolicy.RANDOM_NAMES;

    /**
     * Constructor
     */
    public MassiveFileRenamer(String userUuid, String projectFolderNameUuid, boolean isTemporary) throws FileNotFoundException {
        this.userUuid = userUuid;
        this.projectFolderNameUuid = projectFolderNameUuid;
        this.isTemporary = isTemporary;
    }

    /**
     * Validate parameters before processing
     *
     * @throws FileNotFoundException if project root folder not found or if graph file not found
     */
    private void validateParameters() throws Exception {
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("APKTool thread was aborted abnormally!");
        }
        if (userUuid == null || userUuid.equals(""))
            throw new IllegalArgumentException("User uuid must not be null or empty");
        if (projectFolderNameUuid == null || projectFolderNameUuid.equals(""))
            throw new IllegalArgumentException("Project folder name must not be null or empty");

        // check project's root folder exists
        if (!new File(Configurator.getInstance().getProjectRootFolderPath(userUuid, projectFolderNameUuid, isTemporary)).exists())
            throw new FileNotFoundException("Project's root folder not found!");

        // check project's folder containing decoded apk files exists
        if (!new File(Configurator.getInstance().getDecodedApkFolderPath(userUuid, projectFolderNameUuid, isTemporary)).exists())
            throw new FileNotFoundException("Project's folder containing decoded apk not found!");

        // check project's folder containing smali files exists
        if (!new File(Configurator.getInstance().getSmaliFolderPath(userUuid, projectFolderNameUuid, isTemporary)).exists())
            throw new FileNotFoundException("Project's smali folder not found!");

        // check if new Classes already exists in the project => conflict
        if (customNames != null && customNames.size() > 0 && renamingPolicy.equals(RenamingPolicy.CUSTOM_NAMES)) {
            String smaliFolderPath = Configurator.getInstance().getSmaliFolderPath(userUuid, projectFolderNameUuid, isTemporary);
            for (String classToBeRenamed : customNames.keySet()) {
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedException("APKTool thread was aborted abnormally!");
                }
                String[] classToBeRenamedSplit = classToBeRenamed.split("\\.");
                String classToBeRenamedFolder = String.join(File.separator, Arrays.copyOfRange(classToBeRenamedSplit, 0, classToBeRenamedSplit.length - 1));
                String newClassFileName = customNames.get(classToBeRenamed);
                File newClassFile = new File(smaliFolderPath + File.separator + classToBeRenamedFolder, newClassFileName + ".smali");
                if (newClassFile.exists() && newClassFile.isFile()) {
                    String error = "\"" + newClassFileName + "\" already exist!";
                    LOGGER.error(error);
                    throw new IllegalArgumentException(error);
                }
            }
        }
    }

    /**
     * Rename manifest entries with custom names provided by the user.
     *
     * @param customNames a map containing Key: classFullName(packageName.classSimpleName), value: newClassSimpleName.
     *                    Thus, "packageName.classSimpleName" will be renamed to "packageName.newClassSimpleName"
     */
    public void renameAll(Map<String, String> customNames) throws Exception {
        this.renamingPolicy = RenamingPolicy.CUSTOM_NAMES;
        this.customNames = customNames;
        if (customNames != null && customNames.size() > 0) {
            renameAll();
        }
    }

    public void renameAll() throws Exception {
        validateParameters();
        initialize();
        // checkpoints crossed successfully => start the logic
        replaceOccurrences();
        updateFilesNames();
        updateGraph();
    }


    private void initialize() throws Exception {
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("APKTool thread was aborted abnormally!");
        }

        if (renamingPolicy.equals(RenamingPolicy.CUSTOM_NAMES)) {
            // TODO validate custom names using "Java Class naming convention"

            // Check duplicates
            Set<String> duplicates = Utils.findDuplicates(customNames.values());
            if (duplicates.size() != 0) {
                String duplicatesAsStr = String.join(", ", duplicates);
                String reason = "New manifest entries contain " + duplicates.size() + " duplicate(s) : " + duplicatesAsStr;
                throw new IllegalArgumentException(reason);
            }
        }

        JSONObject manifestEntries = ApkToolsManager.getInstance().getAllManifestEntriesForRenamerTool(userUuid, projectFolderNameUuid, isTemporary);
        // let's build renaming objects
        packageName = manifestEntries.getAsString("package");
        if (packageName == null) {
            throw new IllegalArgumentException("Package name not found ==> could not rename manifest entries");
        }

        @SuppressWarnings("unchecked")
        List<String> packageActivities = (List<String>) manifestEntries.get("packageActivities");
        @SuppressWarnings("unchecked")
        List<String> foreignActivities = (List<String>) manifestEntries.get("foreignActivities");
        @SuppressWarnings("unchecked")
        List<String> packageServices = (List<String>) manifestEntries.get("packageServices");
        @SuppressWarnings("unchecked")
        List<String> foreignServices = (List<String>) manifestEntries.get("foreignServices");
        @SuppressWarnings("unchecked")
        List<String> packageReceivers = (List<String>) manifestEntries.get("packageReceivers");
        @SuppressWarnings("unchecked")
        List<String> foreignReceivers = (List<String>) manifestEntries.get("foreignReceivers");

        if (renamingPolicy.equals(RenamingPolicy.RANDOM_NAMES)) {
            if (packageActivities != null && packageActivities.size() > 0) {
                for (String element : packageActivities) {
                    if (Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedException("APKTool thread was aborted abnormally!");
                    }
                    activities.add(new RenamingObject(element, UUID.randomUUID().toString().replaceAll("-", "")));
                }
            }

            if (packageServices != null && packageServices.size() > 0) {
                for (String element : packageServices) {
                    if (Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedException("APKTool thread was aborted abnormally!");
                    }
                    services.add(new RenamingObject(element, UUID.randomUUID().toString().replaceAll("-", "")));
                }
            }

            if (packageReceivers != null && packageReceivers.size() > 0) {
                for (String element : packageReceivers) {
                    if (Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedException("APKTool thread was aborted abnormally!");
                    }
                    receivers.add(new RenamingObject(element, UUID.randomUUID().toString().replaceAll("-", "")));
                }
            }
        } else if (renamingPolicy.equals(RenamingPolicy.CUSTOM_NAMES)) {
            if (customNames != null) {
                if (packageActivities != null && packageActivities.size() > 0) {
                    for (String element : packageActivities) {
                        if (Thread.currentThread().isInterrupted()) {
                            Thread.currentThread().interrupt();
                            throw new InterruptedException("APKTool thread was aborted abnormally!");
                        }
                        String customName = customNames.get(element);
                        if (customName != null && !"".equals(customName))
                            activities.add(new RenamingObject(element, customName));
                    }
                }

                if (foreignActivities != null && foreignActivities.size() > 0) {
                    for (String element : foreignActivities) {
                        if (Thread.currentThread().isInterrupted()) {
                            Thread.currentThread().interrupt();
                            throw new InterruptedException("APKTool thread was aborted abnormally!");
                        }
                        String customName = customNames.get(element);
                        if (customName != null && !"".equals(customName))
                            activities.add(new RenamingObject(element, customName));
                    }
                }

                if (packageServices != null && packageServices.size() > 0) {
                    for (String element : packageServices) {
                        if (Thread.currentThread().isInterrupted()) {
                            Thread.currentThread().interrupt();
                            throw new InterruptedException("APKTool thread was aborted abnormally!");
                        }
                        String customName = customNames.get(element);
                        if (customName != null && !"".equals(customName))
                            services.add(new RenamingObject(element, customName));
                    }
                }

                if (foreignServices != null && foreignServices.size() > 0) {
                    if (Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedException("APKTool thread was aborted abnormally!");
                    }
                    for (String element : foreignServices) {
                        String customName = customNames.get(element);
                        if (customName != null && !"".equals(customName))
                            services.add(new RenamingObject(element, customName));
                    }
                }

                if (packageReceivers != null && packageReceivers.size() > 0) {
                    if (Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedException("APKTool thread was aborted abnormally!");
                    }
                    for (String element : packageReceivers) {
                        String customName = customNames.get(element);
                        if (customName != null && !"".equals(customName))
                            receivers.add(new RenamingObject(element, customName));
                    }
                }

                if (foreignReceivers != null && foreignReceivers.size() > 0) {
                    if (Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedException("APKTool thread was aborted abnormally!");
                    }
                    for (String element : foreignReceivers) {
                        String customName = customNames.get(element);
                        if (customName != null && !"".equals(customName))
                            receivers.add(new RenamingObject(element, customName));
                    }
                }
            }
        }
    }


    private void replaceOccurrences() throws Exception {
        // get all non media files as vertices ==> preparing text search
        List<String> listMime = new ArrayList<>();
        listMime.add("image");
        listMime.add("audio");
        listMime.add("video");
        List<Vertex> listVertices = GraphManager.getInstance().graphSearchExcludeMimeTypes(
                userUuid
                , projectFolderNameUuid
                , isTemporary
                , listMime
                , null);

        for (RenamingObject activity : activities) {
            renameJavaFileNameInsideSmaliFile(listVertices, activity);
            trackAndReplaceOccurrencesUsingSeparator(listVertices, activity.getCanonicalClassName(), activity.getNewClassSimpleName(), ".");
            trackAndReplaceOccurrencesUsingSeparator(listVertices, activity.getCanonicalClassName(), activity.getNewClassSimpleName(), "/");
            trackAndReplaceOccurrencesUsingSeparator(listVertices, activity.getCanonicalClassName(), activity.getNewClassSimpleName(), "$");
        }

        for (RenamingObject service : services) {
            renameJavaFileNameInsideSmaliFile(listVertices, service);
            trackAndReplaceOccurrencesUsingSeparator(listVertices, service.getCanonicalClassName(), service.getNewClassSimpleName(), ".");
            trackAndReplaceOccurrencesUsingSeparator(listVertices, service.getCanonicalClassName(), service.getNewClassSimpleName(), "/");
            trackAndReplaceOccurrencesUsingSeparator(listVertices, service.getCanonicalClassName(), service.getNewClassSimpleName(), "$");
        }

        for (RenamingObject receiver : receivers) {
            renameJavaFileNameInsideSmaliFile(listVertices, receiver);
            trackAndReplaceOccurrencesUsingSeparator(listVertices, receiver.getCanonicalClassName(), receiver.getNewClassSimpleName(), ".");
            trackAndReplaceOccurrencesUsingSeparator(listVertices, receiver.getCanonicalClassName(), receiver.getNewClassSimpleName(), "/");
            trackAndReplaceOccurrencesUsingSeparator(listVertices, receiver.getCanonicalClassName(), receiver.getNewClassSimpleName(), "$");
        }

        /*
        The last step is to check out the AndroidManifest.xml file for entries named ".EntryName" i.e
        entries that starts with a 'dot' which is an abbreviation of the package name,
        this particular case as an example : <activity android:name=".LaunchActivity"/>;
        The second case is when an entry name doesn't contain any dot,  which means that is must be prefixed with the package name.
        second particular case as an example : <activity android:name="LaunchActivity"/>;

         */
        renameSpecialManifestEntries();
    }

    /**
     * Search and replace of subpackages names in all project files.
     *
     * @param listVertices list of vertices containing file paths and other info abut project files
     * @param separator    either dot, dollar sign or slash, it is used
     *                     to search for different apparitions of package name inside text files
     */
    private void trackAndReplaceOccurrencesUsingSeparator(List<Vertex> listVertices,
                                                          String classCanonicalName, String newClassSimpleName, String separator)
            throws Exception {
        String[] classCanonicalNameSplit = classCanonicalName.split("\\.");
        if (classCanonicalNameSplit.length <= 2)
            return;

        String newClassCanonicalNameFolder = String.join(".", Arrays.copyOfRange(classCanonicalNameSplit,
                0, classCanonicalNameSplit.length - 1));
        String newClassCanonicalName = newClassCanonicalNameFolder + "." + newClassSimpleName;

        String searchTerm = classCanonicalName.replace(".", separator);
        String replaceTerm = newClassCanonicalName.replace(".", separator);

        // Start searching and replacing
        LOGGER.info("search and replace '" + searchTerm + "' by '" + replaceTerm + "'");
        FileComputingManager.getInstance().textSearchAndReplace(listVertices, searchTerm, replaceTerm, true, false, true);
    }


    /**
     * looks at the smali file and Replace:
     * .source "OldName.java"
     * by .source "NewName.java"
     *
     * @param listVertices
     * @param renamingObject
     */
    private void renameJavaFileNameInsideSmaliFile(List<Vertex> listVertices, RenamingObject renamingObject) throws Exception {
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("APKTool thread was aborted abnormally!");
        }
        String[] classCanonicalNameSplit = renamingObject.getCanonicalClassName().split("\\.");
        if (classCanonicalNameSplit.length <= 2)
            return;

        String classSimpleName = classCanonicalNameSplit[classCanonicalNameSplit.length - 1];

        String searchTerm = classSimpleName + ".java";
        String replaceTerm = renamingObject.getNewClassSimpleName() + ".java";

        // Start searching and replacing
        LOGGER.info("search and replace '" + searchTerm + "' by '" + replaceTerm + "'");
        FileComputingManager.getInstance().textSearchAndReplace(listVertices, searchTerm, replaceTerm, true, true, true);
    }

    /**
     * check out the AndroidManifest.xml file for entries named ".EntryName" i.e entries that starts
     * with a 'dot' which is an abbreviation of the package name this particular case as
     * an example : <activity android:name=".LaunchActivity"/>;
     *
     * Same for manifest entries names that do not contain any dot.
     * example : <activity android:name="LaunchActivity"/>;
     * https://stackoverflow.com/questions/3608017/is-the-activity-name-in-androidmanifest-xml-required-to-start-with-a-dot
     */
    private void renameSpecialManifestEntries() throws Exception {
        File manifestFile = ApkToolsManager.getInstance()
                .getAndroidManifestFile(Configurator.getInstance()
                        .getDecodedApkFolderPath(userUuid, projectFolderNameUuid, isTemporary));

        FileInputStream manifestFileInputStream = null;
        InputStream stream = null;
        FileOutputStream out = null;

        try {
            manifestFileInputStream = new FileInputStream(manifestFile);
            String content = IOUtils.toString(manifestFileInputStream);

            for (RenamingObject renamingObject : activities) {
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedException("APKTool thread was aborted abnormally!");
                }
                if (renamingObject.getCanonicalClassName().startsWith(packageName)) {
                    String regex = "android:name=\"\\.(" + Pattern.quote(renamingObject.getOriginalClassSimpleName()) + ")\"";
                    String replacementPattern = "android:name=\"." + renamingObject.getNewClassSimpleName() + "\"";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(content);
                    while (matcher.find()) {
                        content = content.replaceAll(regex, Matcher.quoteReplacement(replacementPattern));
                    }

                    // check entries that does not have any dot => same as prefixed with a dot
                    String regexNoDot = "android:name=\"(" + Pattern.quote(renamingObject.getOriginalClassSimpleName()) + ")\"";
                    String replacementPatternNoDot = "android:name=\"" + renamingObject.getNewClassSimpleName() + "\"";
                    Pattern patternNoDot = Pattern.compile(regexNoDot);
                    Matcher matcherNoDot = patternNoDot.matcher(content);
                    while (matcherNoDot.find()) {
                        content = content.replaceAll(regexNoDot, Matcher.quoteReplacement(replacementPatternNoDot));
                    }
                }
            }
            for (RenamingObject renamingObject : services) {
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedException("APKTool thread was aborted abnormally!");
                }
                if (renamingObject.getCanonicalClassName().startsWith(packageName)) {
                    String regex = "android:name=\"\\.(" + Pattern.quote(renamingObject.getOriginalClassSimpleName()) + ")\"";
                    String replacementPattern = "android:name=\"." + renamingObject.getNewClassSimpleName() + "\"";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(content);
                    while (matcher.find()) {
                        content = content.replaceAll(regex, Matcher.quoteReplacement(replacementPattern));
                    }

                    // check entries that does not have any dot => same as prefixed with a dot
                    String regexNoDot = "android:name=\"(" + Pattern.quote(renamingObject.getOriginalClassSimpleName()) + ")\"";
                    String replacementPatternNoDot = "android:name=\"" + renamingObject.getNewClassSimpleName() + "\"";
                    Pattern patternNoDot = Pattern.compile(regexNoDot);
                    Matcher matcherNoDot = patternNoDot.matcher(content);
                    while (matcherNoDot.find()) {
                        content = content.replaceAll(regexNoDot, Matcher.quoteReplacement(replacementPatternNoDot));
                    }
                }
            }
            for (RenamingObject renamingObject : receivers) {
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedException("APKTool thread was aborted abnormally!");
                }
                if (renamingObject.getCanonicalClassName().startsWith(packageName)) {
                    String regex = "android:name=\"\\.(" + Pattern.quote(renamingObject.getOriginalClassSimpleName()) + ")\"";
                    String replacementPattern = "android:name=\"." + renamingObject.getNewClassSimpleName() + "\"";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(content);
                    while (matcher.find()) {
                        content = content.replaceAll(regex, Matcher.quoteReplacement(replacementPattern));
                    }

                    // check entries that does not have any dot => same as prefixed with a dot
                    String regexNoDot = "android:name=\"(" + Pattern.quote(renamingObject.getOriginalClassSimpleName()) + ")\"";
                    String replacementPatternNoDot = "android:name=\"" + renamingObject.getNewClassSimpleName() + "\"";
                    Pattern patternNoDot = Pattern.compile(regexNoDot);
                    Matcher matcherNoDot = patternNoDot.matcher(content);
                    while (matcherNoDot.find()) {
                        content = content.replaceAll(regexNoDot, Matcher.quoteReplacement(replacementPatternNoDot));
                    }
                }
            }

            stream = new ByteArrayInputStream(content.getBytes("UTF-8"));
            out = new FileOutputStream(manifestFile);
            IOUtils.copyLarge(stream, out);
        } finally {
            IOUtils.closeQuietly(manifestFileInputStream);
            IOUtils.closeQuietly(stream);
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Change file names
     *
     * @throws ScriptException when gremlin script interpreter error
     * @throws IOException     if an IO issue happen during the process
     */
    private void updateFilesNames() throws Exception {
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("APKTool thread was aborted abnormally!");
        }
        // get all non media files as vertices ==> preparing text search
        List<String> listMime = new ArrayList<>();
        listMime.add("image");
        listMime.add("audio");
        listMime.add("video");
        List<Vertex> listVertices = GraphManager.getInstance().graphSearchExcludeMimeTypes(
                userUuid
                , projectFolderNameUuid
                , isTemporary
                , listMime
                , null);

        for (RenamingObject activity : activities) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("APKTool thread was aborted abnormally!");
            }
            renameFiles(listVertices, activity.getCanonicalClassName(), activity.getNewClassSimpleName());
        }

        for (RenamingObject service : services) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("APKTool thread was aborted abnormally!");
            }
            renameFiles(listVertices, service.getCanonicalClassName(), service.getNewClassSimpleName());
        }

        for (RenamingObject receiver : receivers) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("APKTool thread was aborted abnormally!");
            }
            renameFiles(listVertices, receiver.getCanonicalClassName(), receiver.getNewClassSimpleName());
        }
    }


    /**
     * Rename files related to the class
     *
     * @param listVertices       a list of vertices representing all files in the project that are not images, videos ior audio files
     * @param classCanonicalName the old class full name "com.packageName.classOldName"
     * @param newClassSimpleName the new class simple name "newClassName" => thus, "com.packageName.classOldName"
     *                           will be renamed to "com.packageName.newClassName"
     * @throws IOException
     */
    private void renameFiles(List<Vertex> listVertices, String classCanonicalName, String newClassSimpleName)
            throws Exception {
        String[] classCanonicalNameSplit = classCanonicalName.split("\\.");
        if (classCanonicalNameSplit.length <= 2)
            return;

        String newClassCanonicalNameFolder = String.join(".", Arrays.copyOfRange(classCanonicalNameSplit, 0, classCanonicalNameSplit.length - 1));
        String newClassCanonicalName = newClassCanonicalNameFolder + "." + newClassSimpleName;

        String searchTerm = classCanonicalName.replace(".", File.separator);
        String replaceTerm = newClassCanonicalName.replace(".", File.separator);

        // Start renaming files
        String regex = Pattern.quote(searchTerm) + "\\b";
        Pattern pattern = Pattern.compile(regex);

        for (Vertex vertex : listVertices) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("APKTool thread was aborted abnormally!");
            }
            String path = vertex.value("path").toString();
            Matcher matcher = pattern.matcher(path);
            if (matcher.find()) {
                File file = new File(path);
                if (file.exists()) {
                    File newFile = new File(path.replace(searchTerm, replaceTerm));
                    if (newFile.exists())
                        throw new IOException("Could not rename file, as the new name already exists for : " + newFile.getPath());
                    boolean succeeded = file.renameTo(newFile);
                    if (!succeeded) {
                        LOGGER.error("Couldn't rename file : " + file.getPath());
                    } else {
                        LOGGER.info("Renamed '" + file.getPath() + "' ==> '" + newFile.getPath());
                    }
                }
            }
        }
    }

    private void updateGraph() throws Exception {
        GraphManager.getInstance().createOrUpdateGraphAndExportItToJson(userUuid, projectFolderNameUuid, isTemporary);
    }
}
