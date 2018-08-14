package com.ninjaflip.androidrevenge.core.apktool.updater;

import com.ninjaflip.androidrevenge.beans.ApkToolProjectBean;
import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.apktool.ApkToolsManager;
import com.ninjaflip.androidrevenge.core.apktool.filecomputing.FileComputingManager;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.xml.sax.SAXException;

import javax.script.ScriptException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by Solitario on 25/05/2017.
 * <p>
 * This class wraps all operations related to changing the package name of a decompiled apk
 */

public class PackageNameChanger {

    private static final Logger LOGGER = Logger.getLogger(PackageNameChanger.class);
    private String userUuid;
    private String projectFolderNameUuid;
    private boolean isTemporary;
    private String oldPackageName;
    private String newPackageName;

    private String[] oldPackageNameSplit;
    private String[] newPackageNameSplit;

    private File oldPackageDirectory;
    private File newPackageDirectory;

    /**
     * Constructor
     */
    public PackageNameChanger(String userUuid, String projectFolderNameUuid, boolean isTemporary, String oldPackageName, String newPackageName)
            throws FileNotFoundException {
        this.userUuid = userUuid;
        this.projectFolderNameUuid = projectFolderNameUuid;
        this.isTemporary = isTemporary;
        this.oldPackageName = oldPackageName;
        this.newPackageName = newPackageName;
        validateParameters();
    }


    /**
     * Validate parameters before processing
     *
     * @throws FileNotFoundException if project root folder not found or if graph file not found
     */
    private void validateParameters() throws FileNotFoundException {
        if (userUuid == null || userUuid.equals(""))
            throw new IllegalArgumentException("User uuid must not be null or empty");
        if (projectFolderNameUuid == null || projectFolderNameUuid.equals(""))
            throw new IllegalArgumentException("Project folder name must not be null or empty");
        if (oldPackageName == null || oldPackageName.equals(""))
            throw new IllegalArgumentException("Original package name must be not null or empty");
        if (newPackageName == null || newPackageName.equals(""))
            throw new IllegalArgumentException("New package name must be not null or empty");

        // check project's root folder exists
        if (!new File(Configurator.getInstance().getProjectRootFolderPath(userUuid, projectFolderNameUuid, isTemporary)).exists())
            throw new FileNotFoundException("Project's root folder not found!");

        // check project's folder containing decoded apk files exists
        if (!new File(Configurator.getInstance().getDecodedApkFolderPath(userUuid, projectFolderNameUuid, isTemporary)).exists())
            throw new FileNotFoundException("Project's folder containing decoded apk not found!");

        // check project's folder containing smali files exists
        if (!new File(Configurator.getInstance().getSmaliFolderPath(userUuid, projectFolderNameUuid, isTemporary)).exists())
            throw new FileNotFoundException("Project's smali folder not found!");

        // check project's graph file exists
        if (!new File(Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, isTemporary)).exists())
            throw new FileNotFoundException("Project's graph file not found!");
    }

    /**
     * Validate newPackageName with a regex
     */
    private void validateNewPackageName() {
        // TODO check unicode control characters ==> non western
        String ID_PATTERN = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
        Pattern FQCN = Pattern.compile(ID_PATTERN + "(\\." + ID_PATTERN + ")*");
        if (!FQCN.matcher(newPackageName).matches()) {
            throw new IllegalArgumentException(newPackageName + " is not a valid package name");
        }
        // TODO check if new Package name already exist in file system
    }

    /**
     * Check if newPackageName and oldPackageName are compatible.
     * Test fails if (newPackageName == oldPackageName) is true, or if new package name does not contain any dot, or
     * if new package name contains less sub-packages than the original one
     */
    private void checkPackageCompatibility() {
        if (newPackageName.equals(oldPackageName)) {
            throw new IllegalArgumentException("New package name must be different than the original package name!");
        }

        if (oldPackageNameSplit.length == 1) {
            throw new IllegalArgumentException("Updating package name does not work for package names with zero dot!");
        }

        if (newPackageNameSplit.length == 1) { // package name does not contain any dot
            throw new IllegalArgumentException(newPackageName + " must contain at least one dot!");
        }

        if (newPackageNameSplit.length < oldPackageNameSplit.length) { // new package name contains less sub-packages than the original one
            throw new IllegalArgumentException(newPackageName + " must contain at least " + (oldPackageNameSplit.length - 1) + " dots!");
        }
    }

    /**
     * Initialize parameters
     */
    private void initialize() {
        oldPackageNameSplit = oldPackageName.split("\\."); // ==> [old,package,name]
        newPackageNameSplit = newPackageName.split("\\.");// ==> [new,package,name]

        String oldPackageRelativePath = oldPackageName.replace(".", File.separator); // ==> old/package/name
        String newPackageRelativePath = newPackageName.replace(".", File.separator); // ==> new/package/name

        // folder /path/to/.../rootFolder/smali/old/package/name
        oldPackageDirectory = new File(Configurator.getInstance().getSmaliFolderPath(userUuid, projectFolderNameUuid, isTemporary)
                + File.separator + oldPackageRelativePath);
        // folder /path/to/.../rootFolder/smali/new/package/name
        newPackageDirectory = new File(Configurator.getInstance().getSmaliFolderPath(userUuid, projectFolderNameUuid, isTemporary)
                + File.separator + newPackageRelativePath);
    }

    /**
     * Package conflicts verification!
     * <p>
     * imagine a case where old package name is 'com.mycompany.mygame' and new package is 'hello.play.mygame'
     * Problem : 'com' folder contains 'google' folder and 'somedependency' folder...
     * so we can't move the contents of 'com' folder to 'hello' folder
     * Solution: if 'com' folder contains another folder having name different than 'mycompany', then the new
     * package name must start with 'com'
     * <p>
     * We check only these prefixes [com, org, gov, edu, net, mil]
     * and the check is done only for the second subfolder 'com/companyname'
     */
    private void verifyPackageInterference() throws InterruptedException {

        // TODO check out this big problem in the next releases => may cause bugs
        if (oldPackageNameSplit.length == 1)
            return;

        List<String> prefix = new ArrayList<>();
        prefix.add("com");
        prefix.add("org");
        prefix.add("gov");
        prefix.add("edu");
        prefix.add("net");
        prefix.add("mil");

        if (prefix.contains(oldPackageNameSplit[0].toLowerCase())) {
            File folder = new File(Configurator.getInstance().getSmaliFolderPath(userUuid, projectFolderNameUuid, isTemporary)
                    + File.separator + oldPackageNameSplit[0]);
            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedException("APKTool thread was aborted abnormally!");
                    }
                    if (f.isDirectory() && !f.getName().toLowerCase().equals(oldPackageNameSplit[1].toLowerCase())) {
                        if (!newPackageNameSplit[0].equals(oldPackageNameSplit[0]))
                            throw new IllegalArgumentException("The new package name must start with "
                                    + oldPackageNameSplit[0] + ".*");
                    }
                }
            }
        }
    }

    /**
     * Replace all occurrences of old package name by new package name, replace all paths too.
     * <p>
     * example oldPackage = a.b.c , newPackage = x.y.z
     * ---------------------------------
     * ==> replace all occurrences of a.b.c by x.y.z, a.b by x.y, a by x
     * ==> same for path ==> replace all occurrences of a/b/c by x/y/z, a/b by x/y, a by x
     * </p>
     * WARNING: this methond won't work if original package name doesn't contains at least one dot.
     */
    private void replaceOccurrences() throws Exception {
        if (oldPackageNameSplit.length == 1)
            return;

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

        /*
        * Recursive replacement using dot separator
        * ex: com.mama.baba by newcom.newmama.newbaba ==> Dot sign
         */
        trackAndReplaceSubpackageNamesUsingSeparator(listVertices, ".");

        /*
        * Recursive replacement using slash separator
        * ex: com/mama/baba by newcom/newmama/newbaba ==> Slash sign
         */
        trackAndReplaceSubpackageNamesUsingSeparator(listVertices, "/");

        /*
        * Recursive replacement using dollar separator
        * ex: com$mama$baba by newcom$newmama$newbaba ==> Dollar sign
         */
        trackAndReplaceSubpackageNamesUsingSeparator(listVertices, "$");
    }


    /**
     * Search and replace of subpackages names occurrences in all project files using separator
     *
     * @param listVertices list of vertices containing file paths
     * @param separator    subpackages separators
     * @throws IOException
     */
    private void trackAndReplaceSubpackageNamesUsingSeparator(List<Vertex> listVertices, String separator)
            throws Exception {
        Stack<Map<String, Object>> searchStack = new Stack<Map<String, Object>>();
        int index = 1, nbHits;
        boolean found;
        Object[] searchResultObject;

        // Start searching and pushing to stack
        do {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("APKTool thread was aborted abnormally!");
            }
            StringBuilder searchTerm = new StringBuilder(oldPackageNameSplit[0]);
            for (int k = 1; k <= index; k++) {
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedException("APKTool thread was aborted abnormally!");
                }
                searchTerm.append(separator).append(oldPackageNameSplit[k]);
            }

            StringBuilder replaceTerm = new StringBuilder(newPackageNameSplit[0]);
            for (int l = 1; l <= ((newPackageNameSplit.length - oldPackageNameSplit.length) + index); l++) {
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedException("APKTool thread was aborted abnormally!");
                }
                replaceTerm.append(separator).append(newPackageNameSplit[l]);
            }

            searchResultObject = FileComputingManager.getInstance().textSearch(listVertices, searchTerm.toString(),
                    true, false, true);
            nbHits = (Integer) searchResultObject[0];
            found = (nbHits > 0);
            if (found) {
                // build a search result map and push it to the top of the stack
                Map<String, Object> textFound = new HashMap<String, Object>();
                textFound.put("searchTerm", searchTerm.toString());
                textFound.put("replaceTerm", replaceTerm.toString());
                textFound.put("searchResultObject", searchResultObject);
                searchStack.push(textFound);
            }
            index++;
        } while (index < oldPackageNameSplit.length && found);


        // search end ==> pop stack
        while (!searchStack.empty()) {
            Map<String, Object> textFound = searchStack.pop();
            String searchTerm = (String) textFound.get("searchTerm");
            String replaceTerm = (String) textFound.get("replaceTerm");
            Object[] foundSearchResultObject = (Object[]) textFound.get("searchResultObject");
            FileComputingManager.getInstance().textReplace(foundSearchResultObject, searchTerm, replaceTerm, true, false, true);
        }
    }

    /**
     * Update decompiled files structure when package name changed
     * replace all occurrences of old package name by new package name, replace all paths too.
     * <p>
     * example oldPackage name = a.b.c is mapped to folder a/b/c
     * So when the package name changes to x.y.z, we move files form a/b/c to x/y/y and delete old folders a/b/c
     */
    private void updateFilesAndFolders() throws Exception {
        LOGGER.info("Updating files and folders...");
        File iteratorOldPackage = oldPackageDirectory;
        File iteratorNewPackage = newPackageDirectory;
        File smaliFolder = new File(Configurator.getInstance().getSmaliFolderPath(userUuid, projectFolderNameUuid, isTemporary));
        while (!iteratorOldPackage.getPath().equals(smaliFolder.getPath())) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("APKTool thread was aborted abnormally!");
            }
            if (!iteratorNewPackage.getCanonicalPath().startsWith(iteratorOldPackage.getCanonicalPath())) {
                LOGGER.info("---> moving from " + iteratorOldPackage.getPath() + " >>>>>> " + iteratorNewPackage.getPath());
                if (iteratorOldPackage.exists()) {
                    FileComputingManager.getInstance().
                            moveDirectoryContentsToDirectory(iteratorOldPackage.getPath(),
                                    iteratorNewPackage.getPath(), true, true);
                }
            }
            iteratorOldPackage = iteratorOldPackage.getAbsoluteFile().getParentFile();
            iteratorNewPackage = iteratorNewPackage.getAbsoluteFile().getParentFile();
        }
        LOGGER.info("---> Files and folders updated...");
    }

    private void updateGraph() throws Exception {
        GraphManager.getInstance().createOrUpdateGraphAndExportItToJson(userUuid, projectFolderNameUuid, isTemporary);
    }

    /**
     * Undo all changes if there was a major execution fail
     * workflow = save package directory to tmp folder, make operations,if operations fails, recovers !!!!
     * but we have to recover also text change!!!???
     */
    private void recoverOnFails() {
    }

    /**
     * Main method that hold all the logic of changing a package name
     *
     * @throws ScriptException
     * @throws IOException
     */
    public void changePackageName() throws Exception {
        initialize();
        validateNewPackageName();
        checkPackageCompatibility();
        verifyPackageInterference();
        // checkpoints crossed successfully => start the logic
        replaceOccurrences();
        /*if (!oldPackageDirectory.exists()) {
            // in this particular case, the folder hierarchy does not follow the package name
            // example : package name a.b.c.d but the smali folder does not contain any folder /a/b/c/d
            LOGGER.info("package name isn't mapped to any folder ===> return with success :)");
            return; // do nothing, just return
        }
        // the code snippet above bugs in this particular case:
         // package name a.b.c.d and package name does not exists as a folder
         // main activity a.b.MainActivity , folder a.b exist but not a.b.c.d
         // that's why it is commented
        */
        updateFilesAndFolders();
        updateGraph();
    }

    /*private boolean isNewPackageNameMappedToFolder(){
        for()
    }*/


    /**
     * Validate app new package name (google play app Id)
     *
     * @param projectUuid    the uuid of the prject in database
     * @param newPackageName the new package name
     */
    public static void validateNewPackageName(String projectUuid, String newPackageName, String userUuid, boolean isTemporary)
            throws ParserConfigurationException, SAXException, XPathExpressionException, IOException, InterruptedException {
        // naming convention
        String ID_PATTERN = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
        Pattern FQCN = Pattern.compile(ID_PATTERN + "(\\." + ID_PATTERN + ")*");
        if (!FQCN.matcher(newPackageName).matches()) {
            throw new IllegalArgumentException(newPackageName + " is not a valid package name");
        }


        String PACKAGE_NAME_REGEX = "^(([a-z]{1}[a-z0-9\\\\d_]*\\.)+[a-z][a-z0-9\\\\d_]*)$";
        Pattern PACKAGE_NAME_PATTERN = Pattern.compile(PACKAGE_NAME_REGEX);
        if (!PACKAGE_NAME_PATTERN.matcher(newPackageName).matches()) {
            throw new IllegalArgumentException(newPackageName + " is not a valid package name (do not use upper case letter nor non-english alphabet)");
        }


        // check compatibility
        ApkToolProjectBean project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
        String decodedApkFolderPath = Configurator.getInstance().getDecodedApkFolderPath(userUuid, project.getProjectFolderNameUuid(), isTemporary);
        String oldPackageName = ApkToolsManager.getInstance().getPackageNameFromManifest(decodedApkFolderPath);
        String[] oldPackageNameSplit = oldPackageName.split("\\.");
        String[] newPackageNameSplit = newPackageName.split("\\.");


        if (newPackageName.equals(oldPackageName)) {
            throw new IllegalArgumentException("New package name must be different than the original package name!");
        }

        if (oldPackageNameSplit.length == 1) { // existing package name does not contain any dot
            throw new IllegalArgumentException("Updating package name does not work for package names with zero dot!");
        }

        if (newPackageNameSplit.length == 1) { // package name does not contain any dot
            throw new IllegalArgumentException(newPackageName + " must contain at least one dot!");
        }

        if (newPackageNameSplit.length < oldPackageNameSplit.length) { // new package name contains less sub-packages than the original one
            throw new IllegalArgumentException(newPackageName + " must contain at least " + (oldPackageNameSplit.length - 1) + " dots!");
        }


        // check interference
        List<String> prefix = new ArrayList<>();
        prefix.add("com");
        prefix.add("org");
        prefix.add("gov");
        prefix.add("edu");
        prefix.add("net");
        prefix.add("mil");

        if (prefix.contains(oldPackageNameSplit[0].toLowerCase())) {
            File folder = new File(Configurator.getInstance().getSmaliFolderPath(userUuid, project.getProjectFolderNameUuid(), isTemporary)
                    + File.separator + oldPackageNameSplit[0]);
            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedException("APKTool thread was aborted abnormally!");
                    }
                    if (f.isDirectory() && !f.getName().toLowerCase().equals(oldPackageNameSplit[1].toLowerCase())) {
                        if (!newPackageNameSplit[0].equals(oldPackageNameSplit[0]))
                            throw new IllegalArgumentException("The new package name must start with "
                                    + oldPackageNameSplit[0] + ".*");
                    }
                }
            }
        }
    }


    public static void validatePackageRenaming(String projectUuid, String existingPackageName, String newPackageName,
                                               String userUuid, boolean isTemporary)
            throws ParserConfigurationException, SAXException, XPathExpressionException, IOException, InterruptedException {
        ApkToolProjectBean project = ApkToolProjectDao.getInstance().getByUuid(projectUuid);
        String decodedApkFolderPath = Configurator.getInstance().getDecodedApkFolderPath(userUuid, project.getProjectFolderNameUuid(), isTemporary);
        String appPackageName = ApkToolsManager.getInstance().getPackageNameFromManifest(decodedApkFolderPath);

        if (appPackageName.equals(existingPackageName)) {
            throw new IllegalArgumentException("You are trying to rename the app's package name, please use 'Package name changer' tool!");
        }

        String[] existingPackageNameSplit = existingPackageName.split("\\.");
        String[] newPackageNameSplit = newPackageName.split("\\.");


        // check existingPackageName folder exists in the filesystem
        File existingPackageNameFolder = new File(Configurator.getInstance().getSmaliFolderPath(userUuid, project.getProjectFolderNameUuid(), isTemporary)
                + File.separator + String.join(File.separator, existingPackageNameSplit));
        if (!existingPackageNameFolder.exists()) {
            throw new IllegalArgumentException("Package '" + existingPackageName + "' does not exists!");
        }
        if (!existingPackageNameFolder.isDirectory()) {
            throw new IllegalArgumentException(existingPackageName + " is not a directory!");
        }


        /*
        check compatibility
         */
        if (existingPackageName.equals(newPackageName)) {
            throw new IllegalArgumentException("New package name must be different than the existing package name!");
        }

        if (existingPackageNameSplit.length == 1) { // existing package name does not contain any dot
            throw new IllegalArgumentException("Updating package name does not work for package names with zero dot!");
        }

        if (newPackageNameSplit.length == 1) { // package name does not contain any dot
            throw new IllegalArgumentException(newPackageName + " must contain at least one dot!");
        }

        if (newPackageNameSplit.length < existingPackageNameSplit.length) { // new package name contains less sub-packages than the original one
            throw new IllegalArgumentException(newPackageName + " must contain at least " + (existingPackageNameSplit.length - 1) + " dots!");
        }


        // naming convention
        String ID_PATTERN = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
        Pattern FQCN = Pattern.compile(ID_PATTERN + "(\\." + ID_PATTERN + ")*");
        if (!FQCN.matcher(newPackageName).matches()) {
            throw new IllegalArgumentException(newPackageName + " is not a valid package name");
        }

        String PACKAGE_NAME_REGEX = "^(([a-z]{1}[a-z0-9\\\\d_]*\\.)+[a-z][a-z0-9\\\\d_]*)$";
        Pattern PACKAGE_NAME_PATTERN = Pattern.compile(PACKAGE_NAME_REGEX);
        if (!PACKAGE_NAME_PATTERN.matcher(newPackageName).matches()) {
            throw new IllegalArgumentException(newPackageName + " is not a valid package name (do not use upper case letter nor non-english alphabet)");
        }


        // check interference
        List<String> prefix = new ArrayList<>();
        prefix.add("com");
        prefix.add("org");
        prefix.add("gov");
        prefix.add("edu");
        prefix.add("net");
        prefix.add("mil");

        if (prefix.contains(existingPackageNameSplit[0].toLowerCase())) {
            File folder = new File(Configurator.getInstance().getSmaliFolderPath(userUuid, project.getProjectFolderNameUuid(), isTemporary)
                    + File.separator + existingPackageNameSplit[0]);
            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedException("APKTool thread was aborted abnormally!");
                    }
                    if (f.isDirectory() && !f.getName().toLowerCase().equals(existingPackageNameSplit[1].toLowerCase())) {
                        if (!newPackageNameSplit[0].equals(existingPackageNameSplit[0]))
                            throw new IllegalArgumentException("The new package name must start with "
                                    + existingPackageNameSplit[0] + ".*");
                    }
                }
            }
        }
    }

}
