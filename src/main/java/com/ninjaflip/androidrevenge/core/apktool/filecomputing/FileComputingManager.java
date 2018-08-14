package com.ninjaflip.androidrevenge.core.apktool.filecomputing;

import com.ninjaflip.androidrevenge.beans.containers.FixedSizeLinkedHashMap;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import net.minidev.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Solitario on 23/05/2017.
 * <p>
 * This class wraps all file computing operations needed by the apk tool such as text
 * search and replace, file move|copy|replace....
 */
public class FileComputingManager {
    private static final Logger LOGGER = Logger.getLogger(FileComputingManager.class);
    private static FileComputingManager INSTANCE;

    private FileComputingManager() {
    }

    public static FileComputingManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FileComputingManager();
        }
        return INSTANCE;
    }


    /**
     * Search for text 'searchTerm' in a list of files represented by Vertices, search parameters are
     * case sensitivity and word boundaries
     *
     * @param listVertices      list of vertices that contains file paths
     * @param searchTerm        the search term pattern
     * @param caseSensitive     whether the search is case sensitive onr not
     * @param wordStartBoundary whether or not add regex word boundary (\b) at the beginning of search term
     * @param wordEndBoundary   whether or not add regex word boundary (\b) at the end of search term
     * @return an object array containing :
     * element at index 0 = total number of occurrences
     * element at index 1 = an array of TextSearchResultPerFile where every element contains(file path, list of line numbers, total occurrences per file)
     * element at index 2 = total search duration
     * element at index 3 = search term
     * element at index 4 = case sensitivity
     * @throws FileNotFoundException if file with a certain path was not found
     */
    public Object[] textSearch(List<Vertex> listVertices, String searchTerm, boolean caseSensitive,
                               boolean wordStartBoundary, boolean wordEndBoundary) throws Exception {
        Timer time = null;
        PrintProgressScheduledTask progressPrintTask = null;
        try {
            if (searchTerm == null || searchTerm.equals(""))
                throw new IllegalArgumentException("Search term must be not null or empty");

            LOGGER.info("Searching for term '" + searchTerm + "' with CaseSensitive=[" + caseSensitive + "]...");
            ExecutionTimer timer = new ExecutionTimer();
            timer.start();
            int nbOccurrences = 0;
            List<TextSearchResultPerFile> searchSummary = new ArrayList<TextSearchResultPerFile>();
            int listVerticesSize = listVertices.size();
            int counter = 0;
            double percentage;
            String regex = Pattern.quote(searchTerm);
            if (wordStartBoundary) {
                regex = "\\b" + regex;
            }
            if (wordEndBoundary) {
                regex = regex + "\\b";
            }
            if (!caseSensitive) {
                regex = "(?i)" + regex;
            }
            Pattern pattern = Pattern.compile(regex);

            time = new Timer(); // Instantiate Timer Object
            progressPrintTask = new PrintProgressScheduledTask("Search for '" + searchTerm + "'", LOGGER);
            time.schedule(progressPrintTask, 0, 2000);

            for (Vertex vertex : listVertices) {
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedException("APKTool thread was aborted abnormally!");
                }


                File file = new File(vertex.value("path").toString());
                Scanner scanner = new Scanner(file);
                int lineNumber = 1;
                TextSearchResultPerFile srpf = null;
                while (scanner.hasNextLine()) {

                    if (Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedException("APKTool thread was aborted abnormally!");
                    }

                    final String lineFromFile = scanner.nextLine();
                    Matcher matcher = pattern.matcher(lineFromFile);
                    if (matcher.find()) {
                        // a match!
                        if (srpf == null) {
                            srpf = new TextSearchResultPerFile(file.getPath());
                        }

                        int index;
                        if (caseSensitive) {
                            index = lineFromFile.indexOf(searchTerm);
                            while (index != -1) {
                                int start = index;
                                int end = start + searchTerm.length();
                                srpf.addNewFound(lineNumber, start, end);
                                nbOccurrences++;
                                index = lineFromFile.indexOf(searchTerm, end);
                            }
                        } else {
                            index = lineFromFile.toLowerCase().indexOf(searchTerm.toLowerCase());
                            while (index != -1) {
                                int start = index;
                                int end = start + searchTerm.length();
                                srpf.addNewFound(lineNumber, start, end);
                                nbOccurrences++;
                                index = lineFromFile.toLowerCase().indexOf(searchTerm.toLowerCase(), end);
                            }
                        }
                    }
                    lineNumber++;
                }
                scanner.close();

                if (srpf != null) {
                    searchSummary.add(srpf);
                }
                percentage = (counter * 100) / listVerticesSize;
                progressPrintTask.setProgress((int) percentage);
                counter++;
            }

            progressPrintTask.printEndMessage();

            timer.end();
            Double duration = timer.durationInSeconds();
            LOGGER.info("Text search done for '" + searchTerm + "' CaseSensitive=[" + caseSensitive + "] => found " + nbOccurrences + " hit(s) in " + searchSummary.size() + " file(s) (duration: " + duration + " seconds)");
            return new Object[]{nbOccurrences, searchSummary, duration, searchTerm, caseSensitive};
        } finally { // stop the timer task
            if (progressPrintTask != null) {
                progressPrintTask.setStop(true);
                progressPrintTask.cancel();
            }
            if (time != null) {
                time.cancel();
                time.purge();
            }
        }
    }

    /**
     * Search for text 'searchTerm' in a list of files represented by Vertices and
     * replace all its occurrences by 'replacementPattern'
     *
     * @param listVertices       list of vertices that contains info about files where we perform the text search
     * @param searchTerm         the search term pattern
     * @param replacementPattern the replacement term
     * @param caseSensitive      whether the search is case sensitive onr not
     * @param wordStartBoundary  whether or not add regex word boundary (\b) at the beginning of search term
     * @param wordEndBoundary    whether or not add regex word boundary (\b) at the end of search term
     * @return an object array containing :
     * element at index 0 = total number of occurrences
     * element at index 1 = an array of TextSearchResultPerFile where every element contains(file path, list of line numbers, total occurrences per file)
     * element at index 2 = total search duration
     * element at index 3 = search term
     * element at index 4 = case sensitivity
     * @throws IOException if can't find a file (file path are present inside vertex value : 'path')
     */

    public Object[] textSearchAndReplace(List<Vertex> listVertices, String searchTerm,
                                         String replacementPattern, boolean caseSensitive,
                                         boolean wordStartBoundary, boolean wordEndBoundary)
            throws Exception {
        LOGGER.info("Find term '" + searchTerm + "' and replace it with '" + replacementPattern + "' using CaseSensitive=[" + caseSensitive + "]...");
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();
        Timer time = null;
        PrintProgressScheduledTask progressPrintTask = null;

        try {
            // search text
            Object[] searchResultObject = textSearch(listVertices, searchTerm, caseSensitive, wordStartBoundary, wordEndBoundary);
            ArrayList<TextSearchResultPerFile> searchResults = (ArrayList<TextSearchResultPerFile>) searchResultObject[1];

            int counter = 0;
            double percentage;
            int searchResultsSize = searchResults.size();
            time = new Timer(); // Instantiate Timer Object
            progressPrintTask = new PrintProgressScheduledTask("Replacing '" + searchTerm + "'  by '" + replacementPattern + "'", LOGGER);
            time.schedule(progressPrintTask, 0, 2000);

            String regex = Pattern.quote(searchTerm);
            if (wordStartBoundary) {
                regex = "\\b" + regex;
            }
            if (wordEndBoundary) {
                regex = regex + "\\b";
            }
            if (!caseSensitive) {
                regex = "(?i)" + regex;
            }
            Pattern pattern = Pattern.compile(regex);
            // text replace
            for (TextSearchResultPerFile iter : searchResults) {
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedException("APKTool thread was aborted abnormally!");
                }

                FileInputStream currentFileInputStream = null;
                InputStream stream = null;
                FileOutputStream out = null;
                try {
                    File currentFile = new File(iter.getFilePath());
                    currentFileInputStream = new FileInputStream(currentFile);
                    String content = IOUtils.toString(currentFileInputStream);

                    Matcher matcher = pattern.matcher(content);
                    if (matcher.find()) {
                        content = content.replaceAll(regex, Matcher.quoteReplacement(replacementPattern));
                    }

                    stream = new ByteArrayInputStream(content.getBytes("UTF-8"));
                    out = new FileOutputStream(currentFile);
                    IOUtils.copyLarge(stream, out);
                } finally {
                    IOUtils.closeQuietly(currentFileInputStream);
                    IOUtils.closeQuietly(stream);
                    IOUtils.closeQuietly(out);
                }

                percentage = (counter * 100) / searchResultsSize;
                progressPrintTask.setProgress((int) percentage);
                counter++;
            }
            // stop the timer task
            progressPrintTask.setStop(true);
            progressPrintTask.cancel();
            time.cancel();
            time.purge();
            progressPrintTask.printEndMessage();
            timer.end();
            Double duration = timer.durationInSeconds();
            LOGGER.info("Text Find term '" + searchTerm + "' and replace it by '" + replacementPattern + "' using CaseSensitive=[" + caseSensitive + "] => replaced " + searchResultObject[0] + " hit(s) in " + searchResults.size() + " file(s) (duration: " + duration + " seconds)");
            return searchResultObject;
        } finally { // stop the timer task
            if (progressPrintTask != null) {
                progressPrintTask.setStop(true);
                progressPrintTask.cancel();
            }
            if (time != null) {
                time.cancel();
                time.purge();
            }
        }
    }

    public Object[] textSearchAndReplace(List<Vertex> listVertices, String searchTerm, String replacementPattern, boolean caseSensitive) throws Exception {
        return textSearchAndReplace(listVertices, searchTerm, replacementPattern, caseSensitive, false, false);
    }

    /**
     * Perform text replacement for a given search result object 'searchResultObject'
     *
     * @param searchResultObject text search result object
     * @param searchTerm         the search term pattern
     * @param replacementPattern the replacement term
     * @param caseSensitive      whether the search is case sensitive onr not
     * @param wordStartBoundary  whether or not add regex word boundary (\b) at the beginning of search term
     * @param wordEndBoundary    whether or not add regex word boundary (\b) at the end of search term
     * @throws Exception if can't find a file (file path are present inside vertex value : 'path') or thread aborted
     */
    public void textReplace(Object[] searchResultObject,
                            String searchTerm, String replacementPattern,
                            boolean caseSensitive, boolean wordStartBoundary, boolean wordEndBoundary) throws Exception {
        ArrayList<TextSearchResultPerFile> searchResults = (ArrayList<TextSearchResultPerFile>) searchResultObject[1];
        String regex = Pattern.quote(searchTerm);
        if (wordStartBoundary) {
            regex = "\\b" + regex;
        }
        if (wordEndBoundary) {
            regex = regex + "\\b";
        }
        if (!caseSensitive) {
            regex = "(?i)" + regex;
        }
        Pattern pattern = Pattern.compile(regex);

        // text replace
        LOGGER.info("Replacing " + searchResultObject[0] + " hits of  '" + searchTerm + "' by '" + replacementPattern + "' ...");
        for (TextSearchResultPerFile iter : searchResults) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("APKTool thread was aborted abnormally!");
            }

            FileInputStream currentFileInputStream = null;
            InputStream stream = null;
            FileOutputStream out = null;
            try {
                File currentFile = new File(iter.getFilePath());
                currentFileInputStream = new FileInputStream(currentFile);
                String content = IOUtils.toString(currentFileInputStream);

                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    content = content.replaceAll(regex, Matcher.quoteReplacement(replacementPattern));
                }

                stream = new ByteArrayInputStream(content.getBytes("UTF-8"));
                out = new FileOutputStream(currentFile);
                IOUtils.copyLarge(stream, out);
            } finally {
                IOUtils.closeQuietly(currentFileInputStream);
                IOUtils.closeQuietly(stream);
                IOUtils.closeQuietly(out);
            }
        }
        LOGGER.info("---> Text replacement done!");
    }

    /**
     * Move all contents of folder 'srdDir' to folder 'destDir', creates 'destDir' if method
     * argument'createDestDir' is set to true, and delete 'srcDir' at the end of the copy
     * if method argument 'deleteSrcDir' is set to true
     *
     * @param srcDirPath    folder containing files we want to move
     * @param destDirPath   destination folder
     * @param createDestDir creates destination folder if not exists
     * @param deleteSrcDir  delete source folder at the end of the copy
     * @throws IOException              id source of destination directories not found
     * @throws IllegalArgumentException if destination folder is not a directory
     */
    public void moveDirectoryContentsToDirectory(String srcDirPath,
                                                 String destDirPath,
                                                 boolean createDestDir,
                                                 boolean deleteSrcDir)
            throws Exception {

        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("APKTool thread was aborted abnormally!");
        }


        if (srcDirPath == null || srcDirPath.equals("")) {
            throw new NullPointerException("Source must not be null or empty");
        } else if (destDirPath == null || destDirPath.equals("")) {
            throw new NullPointerException("Destination directory must not be null or empty");
        } else {
            // srd and dest directory represent the same directory ==> do nothing, just return
            if (srcDirPath.equals(destDirPath))
                return;

            File srcDirFolder = new File(srcDirPath);

            if (!srcDirFolder.exists()) {
                throw new FileNotFoundException("Source directory '" + srcDirPath + "' does not exist!");
            }

            File destDirFolder = new File(destDirPath);
            if (!destDirFolder.exists() && createDestDir) {
                destDirFolder.mkdirs();
            }

            if (!destDirFolder.exists()) {
                throw new FileNotFoundException("Destination directory '" + destDirPath + "' does not exist [createDestDir=" + createDestDir + "]");
            } else if (!destDirFolder.isDirectory()) {
                throw new IllegalArgumentException("Destination '" + destDirPath + "' is not a directory");
            } else {
                File[] srcDirContent = srcDirFolder.listFiles();
                if (srcDirContent != null) {
                    for (File f : srcDirContent) {
                        if (Thread.currentThread().isInterrupted()) {
                            Thread.currentThread().interrupt();
                            throw new InterruptedException("APKTool thread was aborted abnormally!");
                        }
                        FileUtils.moveToDirectory(f, destDirFolder, true);
                    }
                }
                if (deleteSrcDir)
                    FileUtils.deleteDirectory(srcDirFolder);
            }
        }
    }

    /**
     * An utility method that log the vertices list to console for debug purpose
     *
     * @param searchResult text search result
     */
    public void printSearchResultSummary(String searchTerm, Object[] searchResult) {
        System.out.println("\n******************* start search summary ************************");
        System.out.println("Search '" + searchTerm + "' (" + searchResult[0] + " hits in " + ((ArrayList<TextSearchResultPerFile>) searchResult[1]).size() + " files) ==>(" + searchResult[2] + " seconds)");
        for (TextSearchResultPerFile e : (ArrayList<TextSearchResultPerFile>) searchResult[1]) {
            System.out.println("Found " + e.getNbOccurrences() + " in file : " + e.getFilePath());
            System.out.println("-------> line :" + e.getLineNumbers());
        }
        System.out.println("******************* end search summary **************************");
    }
}
