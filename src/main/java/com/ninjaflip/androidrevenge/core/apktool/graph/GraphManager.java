package com.ninjaflip.androidrevenge.core.apktool.graph;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.db.dao.ApkToolProjectDao;
import com.ninjaflip.androidrevenge.core.service.cache.GraphCache;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import com.ninjaflip.androidrevenge.utils.Utils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerVertex;

import javax.script.Bindings;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.hasId;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.in;
import static org.apache.tinkerpop.gremlin.structure.io.IoCore.graphson;

/**
 * Created by Solitario on 22/05/2017.
 * <p>
 * A wrapper glass for all graph operations
 */

public class GraphManager {
    private final static Logger LOGGER = Logger.getLogger(GraphManager.class);
    private static GraphManager INSTANCE;
    private List<String> ignoredFolders;
    private List<String> ignoredFiles;

    private GraphManager() {
        ignoredFolders = new ArrayList<String>();
        ignoredFiles = new ArrayList<String>();

        ignoredFolders.add(File.separator + "decoded" + File.separator + "dist");
        ignoredFolders.add(File.separator + "decoded" + File.separator + "original");
        ignoredFolders.add(File.separator + "decoded" + File.separator + "build");
        ignoredFolders.add(File.separator + "decoded" + File.separator + "unknown");

        ignoredFiles.add(File.separator + "decoded" + File.separator + "apktool.yml");
    }

    public static GraphManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GraphManager();
        }
        return INSTANCE;
    }

    /**
     * Create a graph representing the decoded apk files hierarchy in the file system
     *
     * @param userUuid              the uuid of the current user
     * @param projectFolderNameUuid the name of the project folder
     * @param isTemporary           true if the project is a temporary project (benchmark test of an apk), false if real project
     * @throws IOException when can't create graph
     */
    public void createOrUpdateGraphAndExportItToJson(String userUuid, String projectFolderNameUuid, boolean isTemporary) throws Exception {
        File rootFolder = new File(Configurator.getInstance().getDecodedApkFolderPath(userUuid, projectFolderNameUuid, isTemporary));
        String graphPath = Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, isTemporary);
        createOrUpdateGraphAndExportItToJson(rootFolder, graphPath);
    }

    /**
     * Create a graph representing the decoded apk files hierarchy in the file system
     *
     * @param rootFolder Folder to be traversed
     * @param graphPath  full path to generated/updated graph file
     * @throws IOException when can't create graph
     */
    public void createOrUpdateGraphAndExportItToJson(File rootFolder, String graphPath) throws Exception {
        LOGGER.info(" ");
        LOGGER.info("********************************");
        LOGGER.info("*****   Updating Graph     *****");
        LOGGER.info("********************************");
        LOGGER.info(" ");

        LOGGER.info("Creating/updating graph...");
        Graph graph = TinkerGraph.open(); // open in-memory Graph
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();
        File[] files = rootFolder.listFiles();
        Vertex rootVertex = graph.addVertex(T.label, "root", "path", rootFolder.getPath(), "name", rootFolder.getName());
        // traverse graph
        traverseFiles(files, graph, rootVertex);
        // export graph to json
        File graphJson = new File(graphPath);
        if (graphJson.exists())
            graphJson.delete();
        graph.io(graphson()).writeGraph(graphJson.getPath());
        timer.end();
        LOGGER.info("---> Graph created/updated in " + timer.durationInSeconds() + " seconds");
    }


    /**
     * Utility method use by method createOrUpdateGraphAndExportItToJson for recursive traversal of a folder, when grating the graph
     *
     * @param files
     * @param graph
     * @param parent
     */
    private void traverseFiles(File[] files, Graph graph, Vertex parent) throws InterruptedException {
        for (File file : files) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("APKTool thread was aborted abnormally!");
            }
            String path = file.getPath();
            if (file.isDirectory()) {
                if (!Utils.containsASubstring(path, ignoredFolders)) {
                    Vertex directory = graph.addVertex(T.label, "directory", "path", path, "name", file.getName());
                    parent.addEdge("contains", directory);
                    traverseFiles(file.listFiles(), graph, directory); // recursive call
                }
            } else {
                String fileExtension = FilenameUtils.getExtension(file.getName());
                if (!Utils.containsASubstring(path, ignoredFiles) && !fileExtension.equals("orig")) {
                    Vertex fileVertex = graph.addVertex(T.label, "file", "path", path,
                            "name", file.getName(), "mimeType", MimeTypes.getMimeType(fileExtension));
                    parent.addEdge("contains", fileVertex);
                }
            }
        }
    }


    /**
     * When a folder is removed then all its content is removed (folders and file)
     * This methd makes sure that we remove all subtree of a vertex that is being removed, if that vertex represents a folder then
     * we must remove all its children vertices recursively
     * @param graph the object grapj
     * @param vertex the vertex to remove
     * @throws ScriptException
     */
    public void recursiveRemoveVertex(Graph graph, Vertex vertex) throws ScriptException {
        if(vertex.label().equals("directory")){
            List<Vertex> children = getVertexChildren(graph, vertex.id().toString());
            for(Vertex child : children){
                recursiveRemoveVertex(graph, child);
            }
        }
        vertex.remove();
    }


    /**
     * get all vertices of a subtree except its root
     * @param graph the graph object
     * @param vertex the root node of the subtree
     * @return
     * @throws ScriptException
     */
    public List<Vertex> getVertexChildrenSubTree(Graph graph, Vertex vertex) throws ScriptException {
        List<Vertex> subtree = new ArrayList<>();
        if(vertex.label().equals("directory")){
            List<Vertex> children = getVertexChildren(graph, vertex.id().toString());
            for(Vertex child : children){
                if(child.label().equals("directory")){
                    subtree.addAll(getVertexChildrenSubTree(graph, child));
                }
                subtree.add(child);
            }
        }
        return subtree;
    }

    /**
     * Imports a json file and translate it into an in memory graph using apache tinkerpop3
     *
     * @param filePath path of the json representation of the graph
     * @return a gremlin graph
     * @throws IOException when a problem happen with json file
     */
    // TODO make it private
    public Graph loadGraphFromJson(String filePath) throws IOException {
        LOGGER.info("Loading graph...");
        Graph graph = TinkerGraph.open();
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();
        graph.io(graphson()).readGraph(filePath);
        timer.end();
        LOGGER.info("Graph loaded in " + timer.durationInSeconds() + " seconds");
        return graph;
    }

    /**
     * Perform a search for files with mimeType containing a string within 'mimeTypeContainsToInclude' array (ex: [image, video,..]) and exclude files
     * having path contains strings within 'excludeFilePathContains' array
     * use-case : when the user starts reskinning an app, he only wants to explore its media files (images, videos and audio)==> include all media mimeTypes + exclude generated files paths
     *
     * @param userUuid                  the uuid of the current user
     * @param projectFolderNameUuid     the name of the project folder
     * @param isTemporary               true if the project is a temporary project (benchmark test of an apk), false if real project
     * @param mimeTypeContainsToInclude a list of mimeTypes to exclude ()
     * @param filePathContainsToExclude
     * @return a list of vertices
     * @throws ScriptException when the gremlin script crashes
     * @throws IOException     if can't load json file
     */
    public List<Vertex> graphSearchIncludeMimeTypes(String userUuid, String projectFolderNameUuid, boolean isTemporary,
                                                    List<String> mimeTypeContainsToInclude,
                                                    List<String> filePathContainsToExclude) throws ScriptException, IOException {
        LOGGER.info("Performing graph filter...");
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();

        // build the script
        String filterMimeTypes = "", filterPath = "";

        // filter for included mimeTypes
        if (mimeTypeContainsToInclude != null && mimeTypeContainsToInclude.size() != 0) {
            if (mimeTypeContainsToInclude.size() == 1) {
                filterMimeTypes = "it.get().value(\"mimeType\").contains(\"" + mimeTypeContainsToInclude.get(0) + "\")";
            } else {
                StringBuilder __filterMimeTypes = new StringBuilder("it.get().value(\"mimeType\").contains(\"" + mimeTypeContainsToInclude.get(0) + "\")");
                for (int i = 1; i < mimeTypeContainsToInclude.size(); i++) {
                    __filterMimeTypes.append(" | it.get().value(\"mimeType\").contains(\"").append(mimeTypeContainsToInclude.get(i)).append("\")");
                }
                filterMimeTypes = __filterMimeTypes.toString();
            }
        }

        // filter for excluded paths
        if (filePathContainsToExclude != null && filePathContainsToExclude.size() != 0) {
            if (filePathContainsToExclude.size() == 1) {
                filterPath = "!it.get().value(\"path\").contains(\"" + filePathContainsToExclude.get(0) + "\")";
            } else {
                StringBuilder __filterPath = new StringBuilder("!it.get().value(\"path\").contains(\"" + filePathContainsToExclude.get(0) + "\")");
                for (int i = 1; i < filePathContainsToExclude.size(); i++) {
                    __filterPath.append(" & !it.get().value(\"path\").contains(\"").append(filePathContainsToExclude.get(i)).append("\")");
                }
                filterPath = __filterPath.toString();
            }
        }

        String script = "graph.traversal().V().hasLabel(\"file\")";
        if (!filterMimeTypes.equals("") && !filterPath.equals("")) {
            script += ".filter{" + filterMimeTypes + "}.filter{" + filterPath + "}";
        } else if (!filterMimeTypes.equals("") && filterPath.equals("")) {
            script += ".filter{" + filterMimeTypes + "}";
        } else if (filterMimeTypes.equals("") && !filterPath.equals("")) {
            script += ".filter{" + filterPath + "}";
        }

        Graph graph = loadGraphFromJson(Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, isTemporary));
        Map<String, Object> bindingsValues = new HashMap<String, Object>();
        bindingsValues.put("graph", graph);

        // execute the script
        List<Vertex> results = executeGremlinGroovyScript(script, bindingsValues);
        timer.end();
        LOGGER.info("Graph filter performed in " + timer.durationInSeconds() + " seconds");
        return results;
    }

    /**
     * Perform a file search by excluding all files with mimeType containing a string within 'mimeTypeContainsToExclude' array
     * (ex: [image, video,..]) and exclude files having path contains strings within 'excludeFilePathContains' array
     * use-case : when we want to search for text, we first need to exclude all image, audio and video files
     *
     * @param userUuid                  the uuid of the current user
     * @param projectFolderNameUuid     the name of the project folder
     * @param isTemporary               true if the project is a temporary project (benchmark test of an apk), false if real project
     * @param mimeTypeContainsToExclude mimeType to exclude
     * @param filePathContainsToExclude file path to exclude
     * @return a list of vertices
     * @throws ScriptException when the gremlin script crashes
     * @throws IOException     when a problem happen with json file
     */
    public List<Vertex> graphSearchExcludeMimeTypes(String userUuid, String projectFolderNameUuid, boolean isTemporary,
                                                    List<String> mimeTypeContainsToExclude,
                                                    List<String> filePathContainsToExclude) throws ScriptException, IOException {
        return graphSearchExcludeMimeTypes(Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, isTemporary),
                mimeTypeContainsToExclude, filePathContainsToExclude);
    }

    /**
     * Perform a file search by excluding all files with mimeType containing a string within 'mimeTypeContainsToExclude' array
     * (ex: [image, video,..]) and exclude files having path contains strings within 'excludeFilePathContains' array
     * use-case : when we want to search for text, we first need to exclude all image, audio and video files
     *
     * @param graphPath                 full path to graph file
     * @param mimeTypeContainsToExclude mimeType to exclude
     * @param filePathContainsToExclude file path to exclude
     * @return a list of vertices
     * @throws ScriptException when the gremlin script crashes
     * @throws IOException     when a problem happen with json file
     */
    public List<Vertex> graphSearchExcludeMimeTypes(String graphPath, List<String> mimeTypeContainsToExclude,
                                                    List<String> filePathContainsToExclude) throws ScriptException, IOException {
        LOGGER.info("Performing graph filter...");
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();

        // build the script
        String filterMimeTypes = "", filterPath = "";

        // filter for included mimeTypes
        if (mimeTypeContainsToExclude != null && mimeTypeContainsToExclude.size() != 0) {
            if (mimeTypeContainsToExclude.size() == 1) {
                filterMimeTypes = "!it.get().value(\"mimeType\").contains(\"" + mimeTypeContainsToExclude.get(0) + "\")";
            } else {
                StringBuilder __filterMimeTypes = new StringBuilder("!it.get().value(\"mimeType\").contains(\"" + mimeTypeContainsToExclude.get(0) + "\")");
                for (int i = 1; i < mimeTypeContainsToExclude.size(); i++) {
                    __filterMimeTypes.append(" & !it.get().value(\"mimeType\").contains(\"").append(mimeTypeContainsToExclude.get(i)).append("\")");
                }
                filterMimeTypes = __filterMimeTypes.toString();
            }
        }

        // filter for excluded paths
        if (filePathContainsToExclude != null && filePathContainsToExclude.size() != 0) {
            if (filePathContainsToExclude.size() == 1) {
                filterPath = "!it.get().value(\"path\").contains(\"" + filePathContainsToExclude.get(0) + "\")";
            } else {
                StringBuilder __filterPath = new StringBuilder("!it.get().value(\"path\").contains(\"" + filePathContainsToExclude.get(0) + "\")");
                for (int i = 1; i < filePathContainsToExclude.size(); i++) {
                    __filterPath.append(" & !it.get().value(\"path\").contains(\"").append(filePathContainsToExclude.get(i)).append("\")");
                }
                filterPath = __filterPath.toString();
            }
        }

        String script = "graph.traversal().V().hasLabel(\"file\")";
        if (!filterMimeTypes.equals("") && !filterPath.equals("")) {
            script += ".filter{" + filterMimeTypes + "}.filter{" + filterPath + "}";
        } else if (!filterMimeTypes.equals("") && filterPath.equals("")) {
            script += ".filter{" + filterMimeTypes + "}";
        } else if (filterMimeTypes.equals("") && !filterPath.equals("")) {
            script += ".filter{" + filterPath + "}";
        }

        Graph graph = loadGraphFromJson(graphPath);
        Map<String, Object> bindingsValues = new HashMap<String, Object>();
        bindingsValues.put("graph", graph);

        // execute the script
        List<Vertex> results = executeGremlinGroovyScript(script, bindingsValues);
        timer.end();
        LOGGER.info("Graph filter performed in " + timer.durationInSeconds() + " seconds");
        return results;
    }

    /**
     * search for all parent nodes in the graph having one or more child name containing the search query
     *
     * @param graphPath                 full path to graph file
     * @param searchQuery               search term
     * @param mimeTypeContainsToExclude mimeType to exclude
     * @param filePathContainsToExclude file path to exclude
     * @return a list of vertices
     * @throws ScriptException when the gremlin script crashes
     * @throws IOException     when a problem happen with json file
     */
    public List<Vertex> graphSearchNameContains(String graphPath, String searchQuery, List<String> mimeTypeContainsToExclude,
                                                List<String> filePathContainsToExclude) throws ScriptException, IOException {
        LOGGER.info("Performing graph search for [" + searchQuery + "]");
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();

        // build the script
        String filterMimeTypes = "", filterPath = "";

        // filter for included mimeTypes
        if (mimeTypeContainsToExclude != null && mimeTypeContainsToExclude.size() != 0) {
            if (mimeTypeContainsToExclude.size() == 1) {
                filterMimeTypes = "!it.get().value(\"mimeType\").contains(\"" + mimeTypeContainsToExclude.get(0) + "\")";
            } else {
                StringBuilder __filterMimeTypes = new StringBuilder("!it.get().value(\"mimeType\").contains(\"" + mimeTypeContainsToExclude.get(0) + "\")");
                for (int i = 1; i < mimeTypeContainsToExclude.size(); i++) {
                    __filterMimeTypes.append(" & !it.get().value(\"mimeType\").contains(\"").append(mimeTypeContainsToExclude.get(i)).append("\")");
                }
                filterMimeTypes = __filterMimeTypes.toString();
            }
        }

        // filter for excluded paths
        if (filePathContainsToExclude != null && filePathContainsToExclude.size() != 0) {
            if (filePathContainsToExclude.size() == 1) {
                filterPath = "!it.get().value(\"path\").contains(\"" + filePathContainsToExclude.get(0) + "\")";
            } else {
                StringBuilder __filterPath = new StringBuilder("!it.get().value(\"path\").contains(\"" + filePathContainsToExclude.get(0) + "\")");
                for (int i = 1; i < filePathContainsToExclude.size(); i++) {
                    __filterPath.append(" & !it.get().value(\"path\").contains(\"").append(filePathContainsToExclude.get(i)).append("\")");
                }
                filterPath = __filterPath.toString();
            }
        }

        String script = "graph.traversal().V()";
        if (!filterMimeTypes.equals("") && !filterPath.equals("")) {
            script += ".filter{" + filterMimeTypes + "}.filter{" + filterPath + "}";
        } else if (!filterMimeTypes.equals("") && filterPath.equals("")) {
            script += ".filter{" + filterMimeTypes + "}";
        } else if (filterMimeTypes.equals("") && !filterPath.equals("")) {
            script += ".filter{" + filterPath + "}";
        }
        //script += ".filter{it.get().value(\"name\").contains(\"" + searchQuery + "\")}"; // ==> this will get all graph node that contains the search query, but we only wants their parent node (jstree specifications)
        script += ".filter{it.get().value(\"name\").contains(\"" + searchQuery + "\")}.in(\"contains\")"; // this will get only their parents. coool

        Graph graph = loadGraphFromJson(graphPath);
        Map<String, Object> bindingsValues = new HashMap<String, Object>();
        bindingsValues.put("graph", graph);

        // execute the script
        List<Vertex> results = executeGremlinGroovyScript(script, bindingsValues);
        timer.end();
        LOGGER.info("Graph graph search for [" + searchQuery + "] performed in " + timer.durationInSeconds() + " seconds");
        return results;
    }


    /**
     * This method performs a search for all vertices having their name contains searchQuery (ignore case)
     * This method is triggered by jstree search plugin
     *
     * @param graph                     project's tinkerpop graph
     * @param searchQuery               the search keyword
     * @param mimeTypeContainsToExclude mimeType to exclude
     * @param filePathContainsToExclude file path to exclude
     * @return a set of node IDs.  all IDs of the subtree that only contains nodes having their name contains searchQuery
     * @throws ScriptException
     * @throws IOException
     */
    public Set<String> graphSearchForJsTreeFilter(Graph graph, String searchQuery, List<String> mimeTypeContainsToExclude,
                                                  List<String> filePathContainsToExclude) throws ScriptException, IOException {
        LOGGER.info("Performing graph search for [" + searchQuery + "]");
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();

        Set<String> noDuplicate = new HashSet<>();

        // build the script
        String filterMimeTypes = "", filterPath = "";

        // filter for included mimeTypes
        if (mimeTypeContainsToExclude != null && mimeTypeContainsToExclude.size() != 0) {
            if (mimeTypeContainsToExclude.size() == 1) {
                filterMimeTypes = "!it.get().value(\"mimeType\").contains(\"" + mimeTypeContainsToExclude.get(0) + "\")";
            } else {
                StringBuilder __filterMimeTypes = new StringBuilder("!it.get().value(\"mimeType\").contains(\"" + mimeTypeContainsToExclude.get(0) + "\")");
                for (int i = 1; i < mimeTypeContainsToExclude.size(); i++) {
                    __filterMimeTypes.append(" & !it.get().value(\"mimeType\").contains(\"").append(mimeTypeContainsToExclude.get(i)).append("\")");
                }
                filterMimeTypes = __filterMimeTypes.toString();
            }
        }

        // filter for excluded paths
        if (filePathContainsToExclude != null && filePathContainsToExclude.size() != 0) {
            if (filePathContainsToExclude.size() == 1) {
                filterPath = "!it.get().value(\"path\").contains(\"" + filePathContainsToExclude.get(0) + "\")";
            } else {
                StringBuilder __filterPath = new StringBuilder("!it.get().value(\"path\").contains(\"" + filePathContainsToExclude.get(0) + "\")");
                for (int i = 1; i < filePathContainsToExclude.size(); i++) {
                    __filterPath.append(" & !it.get().value(\"path\").contains(\"").append(filePathContainsToExclude.get(i)).append("\")");
                }
                filterPath = __filterPath.toString();
            }
        }

        String script = "graph.traversal().V()";
        if (!filterMimeTypes.equals("") && !filterPath.equals("")) {
            script += ".filter{" + filterMimeTypes + "}.filter{" + filterPath + "}";
        } else if (!filterMimeTypes.equals("") && filterPath.equals("")) {
            script += ".filter{" + filterMimeTypes + "}";
        } else if (filterMimeTypes.equals("") && !filterPath.equals("")) {
            script += ".filter{" + filterPath + "}";
        }
        //script += ".filter{it.get().value(\"name\").contains(\"" + searchQuery + "\")}"; // ==> this will get all graph node that contains the search query, but we only wants their parent node (jstree specifications)
        script += ".filter{it.get().value(\"name\").toLowerCase().contains(\"" + searchQuery.toLowerCase() + "\".toLowerCase())}.in(\"contains\")"; // this will get only their parents. coool

        //Graph graph = loadGraphFromJson(graphPath);
        Map<String, Object> bindingsValues = new HashMap<String, Object>();
        bindingsValues.put("graph", graph);

        // new execute the script to get only those nodes who have their names containing the searchQuery
        List<Vertex> results = executeGremlinGroovyScript(script, bindingsValues);

        // as of jstree specifications, we must send also all ancestors of these nodes.
        // now let's get all nodes between the result nodes and the the root and add them to the set (a java Set guarantees no duplication)
        for (Vertex v : results) {
            GraphTraversal<Vertex, Path> query = graph.traversal().V(v.id()).repeat(in().simplePath()).until(hasId(0)).path();
            while (query.hasNext()) {
                Path result = query.next();
                for (int i = 0; i < result.size(); i++) {
                    TinkerVertex vrtx = result.get(i);
                    noDuplicate.add(vrtx.id().toString());
                }
            }
        }

        timer.end();
        LOGGER.info("Graph search for [" + searchQuery + "], found [" + results.size() + "] hits in " + timer.durationInSeconds() + " seconds");
        return noDuplicate;
    }


    /**
     * Execute a gremlin-groovy script using GremlinGroovyScriptEngine and return result as a list of vertices
     *
     * @param script         the script to evaluate
     * @param bindingsValues a map containing script init values
     * @return a list of vertices as a result
     * @throws ScriptException when there is an issue with the script
     */
    // TODO make it private
    public List<Vertex> executeGremlinGroovyScript(String script, Map<String, Object> bindingsValues) throws ScriptException {
        GremlinGroovyScriptEngine engine = new GremlinGroovyScriptEngine();
        List<Vertex> results = new ArrayList<Vertex>();
        Bindings bindings = engine.createBindings();
        for (Map.Entry<String, Object> entry : bindingsValues.entrySet()) {
            bindings.put(entry.getKey(), entry.getValue());
        }
        if (!script.endsWith(".fill(results)")) {
            script += ".fill(results)";
        }
        bindings.put("results", results);
        //LOGGER.info("found ["+results.size()+"] using gremlin script ==> " + script);
        engine.eval(script, bindings);
        return results;
    }


    /**
     * Get the root Vertex of a given gremlin Graph object
     *
     * @param graph gremlin graph
     * @return the root vertex
     * @throws ScriptException       when there is an issue with the script
     * @throws IllegalStateException if root not found, or found more than one root
     */
    public Vertex getRootVertex(Graph graph) throws ScriptException {
        String scriptGetRoot = "graph.traversal().V().hasLabel(\"root\")"; // get root vertex
        Map<String, Object> bindingsValuesGetRoot = new HashMap<>();
        bindingsValuesGetRoot.put("graph", graph);
        // execute the script
        List<Vertex> resultsGetRoot = GraphManager.getInstance().executeGremlinGroovyScript(scriptGetRoot, bindingsValuesGetRoot);
        if (resultsGetRoot.size() == 1) {
            return resultsGetRoot.get(0);
        } else {
            // error
            // size = 0 => not found
            // size > 1 => Error, only one root element must exist
            LOGGER.error("Found " + resultsGetRoot.size() + "root");
            throw new IllegalStateException("Graph contains " + resultsGetRoot.size() + " root element, expected is 1");
        }
    }


    /**
     * Get the Vertex of a given file path
     *
     * @param graph gremlin graph
     * @param filePath path to file
     * @return a vertex that is attached to the file having path equal filePath
     * @throws ScriptException       when there is an issue with the script
     * @throws IllegalStateException if root not found, or found more than one root
     */
    public Vertex getVertexFromFilePath(Graph graph, String filePath) throws ScriptException {
        return graph.traversal().V().has("path", filePath).toList().get(0);
    }

    /**
     * Get a vertex by its id from a given gremlin Graph object
     *
     * @param graph    gremlin graph
     * @param vertexId id of the vertex in the gremlin graph
     * @return the vertex if found, or null if not found or throw IllegalStateException if found more than one
     * @throws ScriptException       when there is an issue with the script
     * @throws IllegalStateException if more than one element was found
     */
    public Vertex getVertexById(Graph graph, String vertexId) throws ScriptException {
        String script = "graph.traversal().V(" + vertexId + ")";
        Map<String, Object> bindingsValues = new HashMap<>();
        bindingsValues.put("graph", graph);
        // execute the script
        List<Vertex> results = GraphManager.getInstance().executeGremlinGroovyScript(script, bindingsValues);
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            // error
            // size = 0 => not found
            // size > 1 => Error, only one vertex element must exist because id is unique
            LOGGER.error("Found " + results.size() + " vertex => expected is 1");
            throw new IllegalStateException("Graph contains " + results.size() + " element having id = " + vertexId + ", expected is 1");
        } else {
            return null;
        }
    }


    /**
     * Get an edge by its id from a given gremlin Graph object
     *
     * @param graph  gremlin graph
     * @param edgeId id of the edge in the gremlin graph
     * @return the edge if found, or null if not found or throw IllegalStateException if found more than one
     * @throws ScriptException       when there is an issue with the script
     * @throws IllegalStateException if more than one element was found
     */
    public Edge getEdgeById(Graph graph, String edgeId) throws ScriptException {
        String script = "graph.traversal().E(" + edgeId + ")";
        Map<String, Object> bindingsValues = new HashMap<>();
        bindingsValues.put("graph", graph);
        // execute the script
        GremlinGroovyScriptEngine engine = new GremlinGroovyScriptEngine();
        List<Edge> results = new ArrayList<Edge>();
        Bindings bindings = engine.createBindings();
        for (Map.Entry<String, Object> entry : bindingsValues.entrySet()) {
            bindings.put(entry.getKey(), entry.getValue());
        }
        if (!script.endsWith(".fill(results)")) {
            script += ".fill(results)";
        }
        bindings.put("results", results);
        engine.eval(script, bindings);

        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() > 1) {
            // error
            // size = 0 => not found
            // size > 1 => Error, only one edge must exist because id is unique
            LOGGER.error("Found " + results.size() + " edges");
            throw new IllegalStateException("Graph contains " + results.size() + " edges having id = " + edgeId + ", expected is 1");
        } else {
            return null;
        }
    }


    /**
     * This method returns the list of children vertices than of a vertex having its id.tString() equal to vertexId
     *
     * @param graph    gremlin graph on which we perform the search
     * @param vertexId the id of which we want to grab children
     * @return a list of children vertices
     * @throws ScriptException when there is an issue with the script
     */
    public List<Vertex> getVertexChildren(Graph graph, String vertexId) throws ScriptException {
        // get all outgoing vertices from vertex having id=vertexId
        String scriptGetChildren = "graph.traversal().V(" + vertexId + ").out(\"contains\")";
        Map<String, Object> bindingsValues = new HashMap<>();
        bindingsValues.put("graph", graph);
        return GraphManager.getInstance()
                .executeGremlinGroovyScript(scriptGetChildren, bindingsValues);
    }

    /**
     * This method updates the graph with a new one ans save the nw graph to disk as a json file
     *
     * @param graph       the new graph instance
     * @param projectUuid the uuid of the project represented by the graph
     * @param userUuid    the owner of the graph
     * @param isTemporary true if the project is a temporary project
     * @throws IOException
     */
    public void updateProjectGraphAndSaveItToDisk(Graph graph, String projectUuid, String userUuid, boolean isTemporary) throws IOException {
        LOGGER.debug("rewriting graph...");
        String projectFolderNameUuid = ApkToolProjectDao.getInstance().getByUuid(projectUuid).getProjectFolderNameUuid();
        String graphPath = Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, isTemporary);
        File graphJson = new File(graphPath);
        if (graphJson.exists())
            graphJson.delete();
        graph.io(graphson()).writeGraph(graphJson.getPath());
        LOGGER.debug("rewriting graph done");
    }


    /**
     * This method is used to update a vertex property and save changed graph
     *
     * @param graph         gremlin graph on which we perform the update
     * @param vertexId      target vertex
     * @param keyValueMap   map containing the properties (keys and values)
     * @param updateCache   if true graph cache will be updated
     * @param userUuid      the owner of the graph
     * @param projectUuid   the uuid of the project represented by the graph
     * @param isTemporary   true if the project is a temporary project
     * @throws IOException
     */
    public void updateVertexPropertyAndSaveGraph(Graph graph, String vertexId, Map<String,String> keyValueMap, boolean updateCache,
                                                 String userUuid, String projectUuid, boolean isTemporary) throws IOException, ScriptException {
        System.out.println("************* updateVertexPropertyAndSaveGraph A");
        Vertex vertex = getVertexById(graph, vertexId);
        System.out.println("************* updateVertexPropertyAndSaveGraph B : " +vertex.toString());
        for (Map.Entry<String,String> pair : keyValueMap.entrySet()) {
            System.out.println("************* updateVertexPropertyAndSaveGraph C : (" +pair.getKey()+", "+pair.getValue()+")");
            vertex.property(VertexProperty.Cardinality.single, pair.getKey(),pair.getValue());
        }
        //System.out.println("************* updateVertexPropertyAndSaveGraph C : (" +pair.getKey()+", "+pair.getValue()+")");

        // rewrite the graph.json file
        String projectFolderNameUuid = ApkToolProjectDao.getInstance().getByUuid(projectUuid).getProjectFolderNameUuid();
        String graphPath = Configurator.getInstance().getGraphFilePath(userUuid, projectFolderNameUuid, isTemporary);
        File graphJson = new File(graphPath);
        if (graphJson.exists())
            graphJson.delete();
        graph.io(graphson()).writeGraph(graphJson.getPath());
        // update graph cache
        if (updateCache) {
            GraphCache.getInstance().cacheGraph(projectUuid, graph);
        }
    }

    public void updateVertexPropertyAndSaveGraph(Graph graph, String vertexId, String propertyKey, String propertyValue, boolean updateCache,
                                                 String userUuid, String projectUuid, boolean isTemporary) throws IOException, ScriptException {
        Map<String,String> keyValueMap = new HashMap<>();
        keyValueMap.put(propertyKey,propertyValue);
        updateVertexPropertyAndSaveGraph(graph, vertexId, keyValueMap, updateCache,userUuid, projectUuid, isTemporary);
    }


    /**
     * This method makes a search for all project's app icons and returns their vertices
     *
     * @param graph          the tinkerpop graph of the project
     * @param resFolderAlias the resource folder alias (mipmap, drawable,..) extracted from the AndroidManifest.xml => application > android:icon
     * @param appIconAlias   the app icon alias (icon, ic_launcher,...) extracted from the AndroidManifest.xml => application > android:icon
     * @return a list of vertices containing all app icons of the project
     */
    public List<Vertex> getProjectAppIconFiles(Graph graph, String resFolderAlias, String appIconAlias) throws ScriptException {
        String filterMimeTypes = "it.get().value(\"mimeType\").contains(\"image/\")";
        String filterPath = "it.get().value(\"path\").contains(\"" + StringEscapeUtils.escapeJava("decoded" + File.separator + "res" + File.separator + resFolderAlias) + "\")";
        String filterName = "it.get().value(\"name\").startsWith(\"" + appIconAlias + ".\")";


        // build the script
        String script = "graph.traversal().V().hasLabel(\"file\").filter{" + filterName + "}.filter{" + filterPath + "}.filter{" + filterMimeTypes + "}";
        Map<String, Object> bindingsValues = new HashMap<String, Object>();
        bindingsValues.put("graph", graph);

        // execute the script
        return executeGremlinGroovyScript(script, bindingsValues);
    }


    /**
     * Return a list of vertices of all directories inside the smali folder
     *
     * @param graph                     the graph representation of the projeect
     * @param filePathContainsToExclude some paths to exclude
     * @return
     * @throws IOException
     * @throws ScriptException
     */
    public List<Vertex> getAllSmaliFoldersVertices(Graph graph, List<String> filePathContainsToExclude) throws IOException, ScriptException {
        LOGGER.info("Performing graph filter > get all smali folders...");
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();

        // build the script
        String filterPath = "";

        // filter for excluded paths
        if (filePathContainsToExclude != null && filePathContainsToExclude.size() != 0) {
            if (filePathContainsToExclude.size() == 1) {
                filterPath = "!it.get().value(\"path\").contains(\"" + filePathContainsToExclude.get(0) + "\")";
            } else {
                StringBuilder __filterPath = new StringBuilder("!it.get().value(\"path\").contains(\"" + filePathContainsToExclude.get(0) + "\")");
                for (int i = 1; i < filePathContainsToExclude.size(); i++) {
                    __filterPath.append(" & !it.get().value(\"path\").contains(\"").append(filePathContainsToExclude.get(i)).append("\")");
                }
                filterPath = __filterPath.toString();
            }
        }

        String smaliFilesCommonPath = "decoded" + File.separator + "smali" + File.separator;
        String script = "graph.traversal().V().hasLabel(\"directory\").filter{it.get().value(\"path\").contains(\"" + StringEscapeUtils.escapeJava(smaliFilesCommonPath) + "\")}";
        if (!filterPath.equals("")) {
            script += ".filter{" + filterPath + "}";
        }
        Map<String, Object> bindingsValues = new HashMap<String, Object>();
        bindingsValues.put("graph", graph);

        // execute the script
        List<Vertex> results = executeGremlinGroovyScript(script, bindingsValues);
        timer.end();
        LOGGER.info("Graph filter (get all smali folders) performed in " + timer.durationInSeconds() + " seconds");
        return results;
    }

    /**
     * An utility method that logs the vertices list to console (for debug purpose)
     *
     * @param verticesList     list of vertices
     * @param verticesValueKey vertex's value that we want to log
     */
    public void printVertices(List<Vertex> verticesList, String verticesValueKey) {
        for (Vertex v : verticesList) {
            System.out.println(verticesValueKey + " : " + v.value(verticesValueKey).toString());
        }
        System.out.println("Vertices list size : " + verticesList.size());
    }

}
