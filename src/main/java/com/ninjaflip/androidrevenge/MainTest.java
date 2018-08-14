package com.ninjaflip.androidrevenge;


import brut.androlib.ApkDecoder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ninjaflip.androidrevenge.beans.AppAvailableCountries;
import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.core.apktool.graph.GraphManager;
import com.ninjaflip.androidrevenge.core.db.dao.AppAvailableCountriesDao;
import com.ninjaflip.androidrevenge.core.keytool.KeytoolManager;
import com.ninjaflip.androidrevenge.core.security.LicenseManager;
import com.ninjaflip.androidrevenge.utils.ExecutionTimer;
import com.ninjaflip.androidrevenge.utils.NetworkAddressUtil;
import com.ninjaflip.androidrevenge.utils.Utils;
import com.x5.template.Chunk;
import com.x5.template.Theme;
import com.x5.template.providers.NetTemplates;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rakesh on 29/07/2017.
 */
public class MainTest {

    public static void main(final String[] args) throws Exception {
        org.apache.log4j.BasicConfigurator.configure();

        /*ObjectRepository<ApkToolProjectBean> repository = DBManager.getInstance().getDb().getRepository(ApkToolProjectBean.class);
        String userUuid = "7e22c861-c6c0-4caf-bce8-04bceb478aeb";
        String projectName = "AppLock";

        System.out.println("parameters ===> userUuid = " + userUuid + ", projectName = " + projectName);
        List<ApkToolProjectBean> projects = ApkToolProjectDao.getInstance().getAll(userUuid);
        System.out.println("projects.size = " + projects.size());
        for(ApkToolProjectBean proj : projects){
            System.out.println("projects.name = " + proj.getName());
        }
        System.out.println("--------------------------------------");

        List<ApkToolProjectBean> projects2 = repository.find(ObjectFilters.eq("owner.uuid",userUuid),
                FindOptions.sort("dateCreated", SortOrder.Descending)).toList();

        for(ApkToolProjectBean proj2 : projects2){
            System.out.println("projects.name = " + proj2.getName()+" ==> owner: " + proj2.getOwner().getUserId());
        }

        System.out.println("--------------------------------------");
        System.out.println("found = " + repository.find(ObjectFilters.regex("name", "TIKCHBILA")).size());
        ApkToolProjectBean proj3 = repository.find(ObjectFilters.regex("name", "gtaguide")).firstOrDefault();

        System.out.println("proj3 = " + proj3);*/


        /*
        System.out.println("Start calculating Folder size...");
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();

        //Path folderPath = Paths.get(Configurator.getInstance().getWork_DIR());
        Path folderPath = Paths.get(Configurator.getInstance().getUSERS_DIR() + File.separator + "7e22c861-c6c0-4caf-bce8-04bceb478aeb");
        long size = Utils.fileOrFoderSize(folderPath);

        double sizeInMB = size / 1048576.0;
        String size_in_MB = new DecimalFormat("#").format(sizeInMB);


        timer.end();
        System.out.println("Folder long size = " +size);
        System.out.println("Folder size = " +size_in_MB + " MByte,  time: " + ExecutionTimer.getTimeString(timer.durationInSeconds()));
        */

        //System.out.println(StringEscapeUtils.escapeJava("decoded"+File.separator+ "original"));

        /*String apkPath = "C:\\Users\\rakesh\\Desktop\\WORKSMAL\\TUPEMATE.apk";
        try (ApkFile apkFile = new ApkFile(new File(apkPath))) {
            ApkMeta apkMeta = apkFile.getApkMeta();

            System.out.println(apkMeta.getIcon());
            System.out.println(apkMeta.getName());
            System.out.println(apkMeta.getPackageName());
            System.out.println(apkMeta.getVersionCode());
            for (UseFeature feature : apkMeta.getUsesFeatures()) {
                System.out.println(feature.getName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        //String script = "graph.traversal().V().hasLabel(\"root\")"; // get root vertex
        //String script = "graph.traversal().V(0)"; // get vertex by its Id id=0 in this case


        /*
        ExecutionTimer timer = new ExecutionTimer();
        timer.start();

        Graph graph = GraphManager.getInstance()
                .loadGraphFromJson("C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\fd2043ab-6f55-4556-a4d7-31926c79f882\\2265b6e4-f0c4-4c9c-817f-7943904e2fe9\\graph.json");




        // get root vertex
        String scriptGetRoot = "graph.traversal().V().hasLabel(\"root\")"; // get root vertex
        Map<String, Object> bindingsValuesGetRoot = new HashMap<>();
        bindingsValuesGetRoot.put("graph", graph);
        // execute the script
        List<Vertex> resultsGetRoot = GraphManager.getInstance().executeGremlinGroovyScript(scriptGetRoot, bindingsValuesGetRoot);
        System.out.println("resultsGetRoot.size(): " + resultsGetRoot.size());
        if (resultsGetRoot.size() == 1) {
            // good
            Vertex root = resultsGetRoot.get(0);
            Object rootId = root.id();
            System.out.println("root id : " + root.id());
            String scriptGetRootChildren = "graph.traversal().V("+rootId.toString()+").out()"; // get all outgoing vertices from vertex having id=rootId
            Map<String, Object> bindingsValuesGetRootChildren = new HashMap<>();
            bindingsValuesGetRootChildren.put("graph", graph);

            List<Vertex> resultsGetRootChildren = GraphManager.getInstance()
                    .executeGremlinGroovyScript(scriptGetRootChildren, bindingsValuesGetRootChildren);
            System.out.println("resultsGetRootChildren.size(): " + resultsGetRootChildren.size());

            for(Vertex child : resultsGetRootChildren){
                if(child.label().equals("file"))
                    System.out.println("path " + child.value("path").toString()+ ", label: " +child.label()+ ", mimeType: "+child.value("mimeType").toString());
                else {
                    Iterator<Edge> iterEdges = child.edges(Direction.OUT, "contains");
                    int nbChildren = Iterators.size(iterEdges);
                    System.out.println("path " + child.value("path").toString() + ", label: " + child.label() + " children count: " + nbChildren);
                }
            }
        } else {
            // error
            // size = 0 => not found
            // size > 1 => Error, only one root element must exist
            System.err.println("Found " + resultsGetRoot.size() + "root");
        }

        timer.end();
        Double duration = timer.durationInSeconds();
        System.out.println("---> graph search performed  in " + duration + " seconds)");

        */


        ExecutionTimer timer = new ExecutionTimer();
        timer.start();

        /*
        String graphPath = "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\fae9e024-febc-4690-aa4b-06cc5d07bbf0\\6aa0a607-d915-466f-baa3-1ba8d9b589ae\\graph.json";
        Graph graph = GraphManager.getInstance()
                .loadGraphFromJson(graphPath);

        String script = "graph.traversal().V("+0+").property(\"name\",\"rakesh\")"; // get root vertex
        Map<String, Object> bindingsValuesGetRoot = new HashMap<>();
        bindingsValuesGetRoot.put("graph", graph);
        // execute the script
        GraphManager.getInstance().executeGremlinGroovyScript(script, bindingsValuesGetRoot);


        File graphJson = new File(graphPath);
        if (graphJson.exists())
            graphJson.delete();
        graph.io(graphson()).writeGraph(graphJson.getPath());
        */

        //String path = "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\fae9e024-febc-4690-aa4b-06cc5d07bbf0\\6aa0a607-d915-466f-baa3-1ba8d9b589ae\\graph.json";
        //String path= "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\fae9e024-febc-4690-aa4b-06cc5d07bbf0\\eea4ac99-b3cf-49d7-bb1c-4d5253c1ea10\\decoded\\AndroidManifest.xml";
        //File fileToRename = new File(path);


        //String graphPathRename = "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\fae9e024-febc-4690-aa4b-06cc5d07bbf0\\6aa0a607-d915-466f-baa3-1ba8d9b589ae\\src_apk_hoho";
        //File graphFileRename = new File(graphPathRename);

        //fileToRename.renameTo(new File(fileToRename.getParent(),"bambara.xml"));


        // bitmoji graph
        /*
        String graphPath = "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\18971fcc-1de6-4116-b15c-a73bae3f381e\\cc65c625-f265-4e2b-88c1-5d84e5115c94\\graph.json";
        Graph graph = GraphManager.getInstance()
                .loadGraphFromJson(graphPath);



        for(int i = 0; i< 20; i++) {
            List<Vertex> lv = graph.traversal().V().has("path", "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\18971fcc-1de6-4116-b15c-a73bae3f381e\\cc65c625-f265-4e2b-88c1-5d84e5115c94\\decoded\\smali\\com\\index.js").toList();
            System.out.println("size === " + lv.size());
        }*/




        /*Pipe<Integer,JSONArray> pipeline = new TransformFunctionPipe<Integer,JSONArray>(new PipeFunction<Integer, JSONArray>() {
            @Override
            public JSONArray compute(Integer vertexId) {
                try {
                    List<Vertex> children = graph.traversal().V(vertexId).out("contains").toList();
                    JSONArray childrenJsonArray = new JSONArray();
                    for (Vertex child : children) {
                        JSONObject jsTreeJsonChild = new JSONObject();
                        JSONObject nodeData = new JSONObject();

                        if (child.label().equals("file")) {
                            jsTreeJsonChild.put("id", child.id().toString());
                            jsTreeJsonChild.put("type", MimeTypes.getMimeTypeCategory(child.value("mimeType").toString()));
                            jsTreeJsonChild.put("text", child.value("name").toString());
                            jsTreeJsonChild.put("children", false);

                            nodeData.put("mimeType", child.value("mimeType").toString());

                        } else {
                            Iterator<Edge> iterEdgesChild = child.edges(Direction.OUT, "contains");
                            jsTreeJsonChild.put("id", child.id().toString());
                            jsTreeJsonChild.put("type", "folder");
                            jsTreeJsonChild.put("text", child.value("name").toString() + " (" + Iterators.size(iterEdgesChild) + ")");
                            jsTreeJsonChild.put("children", true);
                        }
                        jsTreeJsonChild.put("data", nodeData);
                        childrenJsonArray.add(jsTreeJsonChild);
                    }
                    return childrenJsonArray;
                }catch (Exception e){
                    e.printStackTrace();
                    System.err.println(e.getMessage());
                    return null;
                }
            }
        });

        pipeline.setStarts(Arrays.asList(2070,2232,3002,2671,2196,2394,2151,2590,2349,2789,2106,2304,2545,2500,2223,2421,2662,2906,2509,2707,2626,2088,2241,3011,2482,2680,2160,0,2635,2358,2798,2115,2313,2599,2753,2554,2079,2518,2716,2133,2331,2771,2572,2097,2295,2052,2250,3020,2491,1998,2205,2403,2689,2644,2169,2367,2124,2322,2762,2563,2807,2608,2527,2725,2187,2385,2142,2340,2780,2061,2536,2259,3029,2214,2412,2698,2653,2178,2376,2816,41));

        for(JSONArray arrayOfChildren : pipeline) {
            System.out.println(arrayOfChildren.toJSONString());
        }*/


        /*
        Pipe<String,Vertex> pipeline = new TransformFunctionPipe<Integer,JSONArray>(new PipeFunction<String, Vertex>() {
            @Override
            public Vertex compute(String filePath) {
                try {
                    Vertex vertex = graph.traversal().V(vertexId).out("contains").toList();
                    List<Vertex> children = graph.traversal().V(vertexId).out("contains").toList();
                    JSONArray childrenJsonArray = new JSONArray();
                    for (Vertex child : children) {
                        JSONObject jsTreeJsonChild = new JSONObject();
                        JSONObject nodeData = new JSONObject();

                        if (child.label().equals("file")) {
                            jsTreeJsonChild.put("id", child.id().toString());
                            jsTreeJsonChild.put("type", MimeTypes.getMimeTypeCategory(child.value("mimeType").toString()));
                            jsTreeJsonChild.put("text", child.value("name").toString());
                            jsTreeJsonChild.put("children", false);

                            nodeData.put("mimeType", child.value("mimeType").toString());

                        } else {
                            Iterator<Edge> iterEdgesChild = child.edges(Direction.OUT, "contains");
                            jsTreeJsonChild.put("id", child.id().toString());
                            jsTreeJsonChild.put("type", "folder");
                            jsTreeJsonChild.put("text", child.value("name").toString() + " (" + Iterators.size(iterEdgesChild) + ")");
                            jsTreeJsonChild.put("children", true);
                        }
                        jsTreeJsonChild.put("data", nodeData);
                        childrenJsonArray.add(jsTreeJsonChild);
                    }
                    return childrenJsonArray;
                }catch (Exception e){
                    e.printStackTrace();
                    System.err.println(e.getMessage());
                    return null;
                }
            }
        });

        pipeline.setStarts(Arrays.asList(2070,2232,3002,2671,2196,2394,2151,2590,2349,2789,2106,2304,2545,2500,2223,2421,2662,2906,2509,2707,2626,2088,2241,3011,2482,2680,2160,0,2635,2358,2798,2115,2313,2599,2753,2554,2079,2518,2716,2133,2331,2771,2572,2097,2295,2052,2250,3020,2491,1998,2205,2403,2689,2644,2169,2367,2124,2322,2762,2563,2807,2608,2527,2725,2187,2385,2142,2340,2780,2061,2536,2259,3029,2214,2412,2698,2653,2178,2376,2816,41));

        for(JSONArray arrayOfChildren : pipeline) {
            System.out.println(arrayOfChildren.toJSONString());
        }
        */


        //System.out.println(IteratorUtils.count(graph.edges()));


        /*Pipe<String,Integer> pipeline = new TransformFunctionPipe<String,Integer>(new PipeFunction<String, Integer>() {
            @Override
            public Integer compute(String s) {
                return s.length();
            }
        });

        pipeline.setStarts(Arrays.asList("tell", "me", "your", "name"));

        for(Integer len : pipeline) {
            System.out.println(len);
        }*/




        /*int nbVertices = toIntExact(IteratorUtils.count(graph.vertices()));
        boolean done = false;
        int newFolderId = -1;
        int j = 0;
        while(!done){
            System.out.println("iteeration " + j);
            j++;
            newFolderId = generateRandomInt(nbVertices, nbVertices+ 100000);
            if(GraphManager.getInstance().getVertexById(graph, String.valueOf(newFolderId)) == null){
                done=true;
            }

        }

        System.out.println("newFolderId : " + newFolderId);
        File newFolder = new File("C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\03fadfea-8c9c-4162-9a39-642c6e1ff704\\080587ea-c24f-4f5b-826d-aa8755617277\\decoded\\smali\\NEW_FOLDER_OK");
        Vertex newFolderVertex = graph.addVertex(T.id, newFolderId,T.label, "directory", "path", newFolder.getPath(), "name", newFolder.getName());
        //parentVertex.addEdge("contains", newFolderVertex);
        System.out.println("added new folder vertex to graph, its id is :" + newFolderVertex.id().toString());*/





        /*String nodeId = "143"; // assets folder
        Vertex folderVertex = GraphManager.getInstance().getVertexById(graph, nodeId);
        String folderPath = folderVertex.value("path").toString();
        String script = "graph.traversal().V().filter{it.get().value(\"path\").contains(\"" + StringEscapeUtils.escapeJava(folderPath) + "\")}";


        Map<String, Object> bindingsValuesGetRoot = new HashMap<>();
        bindingsValuesGetRoot.put("graph", graph);
        // execute the script

        List<Vertex> vertices = GraphManager.getInstance().executeGremlinGroovyScript(script, bindingsValuesGetRoot);

        for (Vertex v : vertices) {
            System.out.println(v.property("path").value().toString());
        }*/
        //GraphManager.getInstance().printVertices(vertices, "path");

        //testParseManifest0();
        //testParseManifest1();
        //textGetStringsXmlFiles();

        //searchTextIndexStartEnd();

        //printCertInfo();
        //systemInfo();

        //testJavaRegexSearch2();

        //testInjectInitMethod();

        //testJoin();

        //testTemplate();


        //testDetectShowInterstitlaShow();
        //testRegex();

        //testSerg();

        //anothertest();

        //testInjectBanner();

        //testInjectInitMethod2();

        //testRegexFresh();

        //locateClassesAndMethodsInsideObfuscatedProject();
        //methodExist();

        //sysProperties();

        //testStego();

        //testDDM();
        //testDateDiff();

        /*try {

            String now = String.valueOf(System.currentTimeMillis());
            long launchDateMillis = Long.valueOf(now);
            System.out.println("launchDateMillis = "+ launchDateMillis);
            Date launchDate = new Date(launchDateMillis);

            Thread.sleep(2000);

            Date today = new Date();
            today.after(launchDate);

            System.out.println("today.after(launchDate) = "+ today.after(launchDate));
        }catch (Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }*/

        //testWorkDone();

        //testGetLaunchDate();

        //downloadImage();

        //simpleTest1();
        //testScaleImage();

        //Locale from forLanguageTag method
        //Locale forLangLocale = Locale.forLanguageTag("sq-AL");
        //System.out.println("forLangLocale: "+forLangLocale.getDisplayLanguage(Locale.ENGLISH));


        //System.out.println("escape : " + StringEscapeUtils.escapeHtml("uh&uie<<>"));
        //getMotherBoeard();

        //*********************** SERIAL HERE ***********************************
        //generateUserSerial();
        //*********************** SERIAL HERE ***********************************
        //PreferencesManager.getInstance().revokeTermsAndConditions();
        //PreferencesManager.getInstance().deleteLicenseKey();
        //PreferencesManager.getInstance().revokeMustCheckUserLicense();


        //checkDns("www.b217c203b1ab1710cf357e23fd7c595f.com");




        /*SimpleDateFormat formatter = new SimpleDateFormat("dd-M-yyyy hh:mm:ss a");
        String ff = formatter.format(Utils.getNtpTime());
        System.out.println("internet time>>>> " + ff);
        System.out.println("internet time<<<< " + formatter.parse(ff));*/

        /*Date lastNtp = PreferencesManager.getInstance().getLastNtpTime();
        System.out.println("last ntp time>>>>" + lastNtp);
        Date now = new Date();
        System.out.println("check now after lastNtp >>>>" + (now.after(lastNtp) || now.equals(lastNtp)));*/


        /*
        SocketAddress ca = new InetSocketAddress("0:0:0:0:0:0:0:1", 43594);
        SocketAddress cb = new InetSocketAddress("0:0:0:0:0:0:0:1", 43594);
        System.out.println(ca.equals(cb));


        String ip = "127.0.0.1";
        String[] octets = ip.split("\\.");
        byte[] octetBytes = new byte[4];
        for (int i = 0; i < 4; ++i) {
            octetBytes[i] = (byte) Integer.parseInt(octets[i]);
        }

        byte ipv4asIpV6addr[] = new byte[16];
        ipv4asIpV6addr[10] = (byte)0xff;
        ipv4asIpV6addr[11] = (byte)0xff;
        ipv4asIpV6addr[12] = octetBytes[0];
        ipv4asIpV6addr[13] = octetBytes[1];
        ipv4asIpV6addr[14] = octetBytes[2];
        ipv4asIpV6addr[15] = octetBytes[3];
        */


        //System.out.println("Folder marked as delete later: "+ PreferencesManager.getInstance().getFoldersMarkedForDelete("ca81b294-526f-4d5a-b46a-883e72d479bd"));


        //testThreadStop();


        //textGraphUpdateProperty();
        //PreferencesManager.getInstance().deleteLicenseKey();
        //PreferencesManager.getInstance().revokeTermsAndConditions();


        //System.out.println("decoded string : "+ Utils.decdes("9Oyt23MMNYBQJkYlEvtYEGCPSyLSTXjTUWHYx/w53h0=", "com.yourtube.videoplayer"));
        System.out.println("encoded string : "+ Utils.encdes("com.kids.videoplayer", "com.kids.videoplayer"));




        /*String generatedSerial = "34304479335A597667374C5835365257734A2B36396A335A2B536F37674D6F3362342B47316B493968502B6E4E2B61547936563732413D3D";
        System.out.println("generatedSerial : " + generatedSerial);
        System.out.println("checkRealSerialIsValid : " + LicenseManager.getInstance().checkGeneratedSerialIsValid(generatedSerial));
*/


        //manifestEntriesRenamer();
        //System.out.println("encode :" + URLEncoder.encode("uui rrr < > ? / toto .jks","UTF-8"));

        //Preferences.userRoot().node(PreferencesManager.class.getName()).remove("playlistsAsJson");

        //testKeystorePArameters();


        //FavoriteAppDao.getInstance().deleteAll("ca81b294-526f-4d5a-b46a-883e72d479bd");


        //AdbManager.getInstance().getProcesses();
        //AdbManager.getInstance().logProcess("24868", null, "USER12345");
        //AdbManager.getInstance().startAdbServer("USER12345");
        //AdbManager.getInstance().logProcess("1379", "E", false,null, "USER12345");
        //Thread.sleep(5000);
        //AdbManager.getInstance().exitLogcat();
        // subtree

        //getAppDetails(null);
        timer.end();
        Double duration = timer.durationInSeconds();
        System.out.println("---> operation performed  in " + duration + " seconds)");
        Thread.sleep(100);
    }


    private static JSONObject getAppDetails(HashMap<String, Boolean> scrappingOptions) throws HttpStatusException, IOException {

        HashMap<String, Boolean> options;
        if (scrappingOptions == null) {
            options = new HashMap<>();
            options.put("title", true);
            options.put("developer", true);
            options.put("shortDesc", true);
            options.put("longDesc", true);
            options.put("genre", true);
            options.put("price", true);
            options.put("icon", true);
            options.put("revenueModel", true);
            options.put("versioning", true);
            options.put("rating", true);
            options.put("size", true);
            options.put("installs", true);
            options.put("developerWebsite", true);
            options.put("comments", true);
            options.put("video", true);
            options.put("screenshots", true);
            options.put("recentChanges", true);
        } else {
            options = scrappingOptions;
        }

        // The object that will contain the results
        JSONObject result = new JSONObject();

        //File htmlFile = new File("C:\\Users\\Solitario\\Desktop\\mario_scrap.html");
        File htmlFile = new File("C:\\Users\\Solitario\\Desktop\\peppa_scrap.html");
        InputStream targetStream = new FileInputStream(htmlFile);
        String html = Utils.convertStreamToString(targetStream);
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        targetStream.close();


        String titleCssSelector = ".AHFaub > span";
        String developerCssSelector = ".hrTbp.R8zArc";
        String shortDescCssSelector = "meta[itemprop=description]";
        String longDescCssSelector = "div[jsname=sngebd]";
        String genreCssSelector = "a[itemprop=genre]";
        String iconCssSelector = "img.T75of.ujDFqe";
        String inappCssSelector = ".rxic6";
        String adsSupportedCssSelector = ".rxic6";
        String scoreCssSelector = ".BHMmbe";
        String nbReviewsCssSelector = ".AYi5wd.TBRnV > span";


        if (options.get("title")) {
            String title = doc.select(titleCssSelector).first().text();
            result.put("title", title);
            System.out.println("title : " + title);
        }

        if (options.get("developer")) {
            String developer = doc.select(developerCssSelector).first().text();
            result.put("developer", developer);
            System.out.println("developer : " + developer);
        }

        if (options.get("shortDesc")) {
            String shortDesc = doc.select(shortDescCssSelector).first().attr("content");
            result.put("shortDesc", shortDesc);
            System.out.println("shortDesc : " + shortDesc);
        }

        if (options.get("longDesc")) {
            // we must remove all <br> tags as it becomes two break-lines on the client side (\n<br>)
            String longDesc = doc.select(longDescCssSelector).first().html().replaceAll("<br>", "");
            result.put("longDesc", longDesc);
            System.out.println("longDesc : " + longDesc);
        }

        if (options.get("genre")) {
            org.jsoup.nodes.Element mainGenre = doc.select(genreCssSelector).first();
            String genreText = mainGenre.text().trim();
            String genreId = mainGenre.attr("href").split("/")[4];
            result.put("genreText", genreText);
            result.put("genreId", genreId);
            System.out.println("genreText : " + genreText);
            System.out.println("genreId : " + genreId);
        }

        if (options.get("price")) {
            String price = doc.select("meta[itemprop=price]").attr("content");
            result.put("price", price);
            System.out.println("price : " + price);
        }

        if (options.get("icon")) {
            String icon = doc.select(iconCssSelector).attr("src");
            result.put("icon", icon);
            System.out.println("icon : " + icon);
        }

        if (options.get("revenueModel")) {
            boolean offersIAP = doc.select(inappCssSelector).size() != 0;
            boolean adSupported = doc.select(adsSupportedCssSelector).size() != 0;
            result.put("offersIAP", offersIAP);
            result.put("adSupported", adSupported);
            System.out.println("offersIAP : " + offersIAP);
            System.out.println("adSupported : " + adSupported);
        }

        Elements additionalInfo = doc.select(".details-section-contents");
        if (options.get("versioning")) {
            try {
                String version = additionalInfo.select("div.content[itemprop=softwareVersion]").text().trim();
                String updated = additionalInfo.select("div.content[itemprop=datePublished]").first().text().trim();
                String androidVersionText = additionalInfo.select("div.content[itemprop=operatingSystems]").first().text().trim();
                String androidVersion = normalizeAndroidVersion(androidVersionText);
                result.put("version", version);
                result.put("updated", updated);
                result.put("androidVersion", androidVersion);
            } catch (Exception e) {
                // do nothing
            }
        }

        if (options.get("rating")) {
            System.out.println("scapper E 1 = "+ doc.select(nbReviewsCssSelector).first().text());

            long nbReviews = cleanLong(doc.select(nbReviewsCssSelector).first().text());

            result.put("nbReviews", nbReviews);
            System.out.println("scapper E 1");

            System.out.println("scapper E 2 = "+ doc.select(scoreCssSelector).first().text().replace(',', '.'));
            Float score = Float.parseFloat(doc.select(scoreCssSelector).first().text().replace(',', '.'));
            result.put("score", score);

            System.out.println("scapper E 2");
        }

        if (options.get("size")) {
            try {
                Elements sizeEl = additionalInfo.select("div.content[itemprop=fileSize]");
                if (sizeEl.size() != 0) {
                    String size = sizeEl.text().trim();
                    result.put("size", size);
                }
            } catch (Exception e) {
                // do nothing
            }
        }

        if (options.get("installs")) {
            try {
                Elements preregister = doc.select(".preregistration-container");
                if (preregister.size() == 0) {
                    String[] installs = installNumbers(additionalInfo.select("div.content[itemprop=numDownloads]"));
                    if (installs != null) {
                        long minInstalls = cleanLong(installs[0]);
                        long maxInstalls = cleanLong(installs[1]);
                        result.put("minInstalls", minInstalls);
                        result.put("maxInstalls", maxInstalls);
                    }
                }
            } catch (Exception e) {
                // do nothing
            }
        }

        if (options.get("developerWebsite")) {
            try {
                Elements developerWebsiteEl = additionalInfo.select(".dev-link[href^=http]");
                if (developerWebsiteEl.size() > 0) {
                    String developerWebsite = developerWebsiteEl.first().attr("href");
                    // extract clean url wrapped in google url
                    Map<String, List<String>> map = Utils.splitQuery(new URL(developerWebsite));
                    developerWebsite = map.get("q").get(0);
                    result.put("developerWebsite", developerWebsite);
                }
            } catch (Exception e) {
                // do nothing
            }
        }

        if (options.get("comments")) {
            try {
                Elements commentsEls = doc.select(".quoted-review");
                if (commentsEls.size() != 0) {
                    JSONArray commentsArray = new JSONArray();
                    for (org.jsoup.nodes.Element com : commentsEls) {
                        commentsArray.add(com.text().trim());
                    }
                    result.put("comments", commentsArray);
                }
            } catch (Exception e) {
                // do nothing
            }
        }

        if (options.get("video")) {
            try {
                Elements videoEl = doc.select(".screenshots span.preview-overlay-container[data-video-url]");
                if (videoEl.size() != 0) {
                    String video = videoEl.first().attr("data-video-url").split("\\?")[0];
                    result.put("video", video);
                }
            } catch (Exception e) {
                // do nothing
            }
        }

        if (options.get("screenshots")) {
            try {
                Elements screenshotsEls = doc.select(".thumbnails .screenshot");
                if (screenshotsEls.size() != 0) {
                    JSONArray screenshotsArray = new JSONArray();
                    for (org.jsoup.nodes.Element sc : screenshotsEls) {
                        screenshotsArray.add(sc.attr("src"));
                    }
                    result.put("screenshots", screenshotsArray);
                }
            } catch (Exception e) {
                // do nothing
            }
        }

        if (options.get("recentChanges")) {
            try {
                Elements recentChangesEls = doc.select(".recent-change");
                if (recentChangesEls.size() != 0) {
                    JSONArray recentChangesArray = new JSONArray();
                    for (org.jsoup.nodes.Element rc : recentChangesEls) {
                        recentChangesArray.add(rc.text());
                    }
                    result.put("recentChanges", recentChangesArray);
                }
            } catch (Exception e) {
                // do nothing
            }
        }
        System.out.println(result);
        return result;
    }

    // transform string to long (installs, rating...)
    // we use long instead of integer => problem with facebook app number of downloads > 5.000.000.000 => java.lang.NumberFormatException
    static long cleanLong(String number) {
        number = number.replaceAll("[\\D]", "");
        return Long.parseLong(number);
    }

    static int cleanInt(String number) {
        number = number.replaceAll("[\\D]", "");
        return Integer.parseInt(number);
    }

    // parse install number string to get the min and max installs
    static private String[] installNumbers(Elements downloads) {

        if (downloads.size() == 0) {
            return new String[]{"0", "0"};
        }
        String[] separators = {" - ", " et ", "–", "-", "~", " a "};
        String downloadsStr = downloads.first().text().trim();
        for (String separator : separators) {
            String[] split = downloadsStr.split(separator);
            if (split.length == 2) {
                return split;
            }
        }
        return null;
    }

    // transform string to integer
    private static String normalizeAndroidVersion(String androidVersionText) {
        Pattern p = Pattern.compile("([0-9\\.]+)[^0-9\\.].+");
        Matcher matcher = p.matcher(androidVersionText);
        if (!matcher.matches()) {
            return "VARY";
        }
        return matcher.group(1);
    }

    private static void checkDns(String dns) throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getByName(dns);
        System.out.println(inetAddress.getHostName());
        System.out.println(inetAddress.getHostAddress());
    }

    private static void textGraphUpdateProperty() throws IOException, ParserConfigurationException, SAXException, ScriptException {
        String graphPath = "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\ca81b294-526f-4d5a-b46a-883e72d479bd\\e139032f-8bc6-482b-89ae-9a2721b289e3\\graph.json";
        Graph graph = GraphManager.getInstance()
                .loadGraphFromJson(graphPath);
        //List<Vertex> vertices = graph.traversal().V().has("name", "myAspect.java").toList();
        //System.out.println("is = "+ vertices.get(0).id());

        //graph.traversal().V().has("path", Text.textContains(".png")).toList();

        //System.out.println("nb png = "+ graph.traversal().V().has("path", Text.textContains(".html")).toList().size());

        /*Vertex v = GraphManager.getInstance().getVertexById(graph, "73559");
        v.property("name", "NEW_NAME_UPDATED");
        v.property("path", "NEW_NAME_UPDATED");


        //System.out.println("new path = "+ graph.traversal().V("73559").values("path").toString());
        System.out.println("new path = "+ v.value("path").toString());

        File graphJson = new File(graphPath);
        if (graphJson.exists())
            graphJson.delete();
        graph.io(graphson()).writeGraph(graphJson.getPath());*/
    }

    public static void testThreadStop() throws Exception {
        Thread.currentThread().isInterrupted();
        Thread thread = new Thread(() -> {
            // Run a task specified by a Runnable Object asynchronously.
            CompletableFuture<Void> future = CompletableFuture.runAsync(new Runnable() {
                @Override
                public void run() {
                    File apkFile = new File("C:\\Users\\rakesh\\Desktop\\apks\\trtrt.apk");
                    ApkDecoder decoder = new ApkDecoder();
                    decoder.setApkFile(apkFile);
                    try {
                        decoder.setForceDelete(true);
                        decoder.setOutDir(new File("C:\\Users\\rakesh\\Desktop\\apks\\trtrt"));
                        System.out.println("---> decoding APK ");
                        decoder.decode();
                        System.out.println("---> Apk decoded to folder");
                    } catch (Exception ex) {
                        System.err.println(ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            });

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    future.cancel(true);
                    future.completeExceptionally(new Exception("TTTTTTTTTTTTTTTT"));
                    System.out.println("=============================== exit");
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        thread.start();

        System.out.println("thread is isInterrupted = " + thread.isInterrupted());
        System.out.println("thread is isAlive = " + thread.isAlive());

        Thread.sleep(3000);
        System.out.println("---------------------> interrupting the thread");
        thread.interrupt();

        Thread.sleep(30000);

        System.out.println("thread is isInterrupted = " + thread.isInterrupted());
        System.out.println("thread is isAlive = " + thread.isAlive());

        Thread.sleep(10000);

    }


    public static void testThreadStop3() throws Exception {
        Thread thread = new Thread(() -> {
            File apkFile = new File("C:\\Users\\rakesh\\Desktop\\apks\\trtrt.apk");
            ApkDecoder decoder = new ApkDecoder();
            decoder.setApkFile(apkFile);
            try {
                decoder.setForceDelete(true);
                decoder.setOutDir(new File("C:\\Users\\rakesh\\Desktop\\apks\\trtrt"));
                System.out.println("---> decoding APK ");
                decoder.decode();
                System.out.println("---> Apk decoded to folder");
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace();
            }
        });
        // Run a task specified by a Runnable Object asynchronously.
        CompletableFuture<Void> future = CompletableFuture.runAsync(thread);

        // Block and wait for the future to complete
        //future.get();
        System.out.println("Future submitted");
        Thread.sleep(3000);
        System.out.println("---> interrupting the thread");
        future.cancel(true);
        future.completeExceptionally(new Exception("hihihihi"));

        System.out.println("thread is isCancelled = " + future.isCancelled());
        System.out.println("thread is isCompletedExceptionally = " + future.isCompletedExceptionally());
        System.out.println("thread is isDone = " + future.isDone());

        Thread.sleep(30000);

        System.out.println("thread is isCancelled = " + future.isCancelled());
        System.out.println("thread is isCompletedExceptionally = " + future.isCompletedExceptionally());
        System.out.println("thread is isDone = " + future.isDone());
        //Thread.sleep(10000);
    }

    public static void testThreadStop2() throws Exception {
        Thread thread = new Thread(() -> {
            File apkFile = new File("C:\\Users\\rakesh\\Desktop\\apks\\trtrt.apk");
            ApkDecoder decoder = new ApkDecoder();
            decoder.setApkFile(apkFile);
            try {
                decoder.setForceDelete(true);
                decoder.setOutDir(new File("C:\\Users\\rakesh\\Desktop\\apks\\trtrt"));
                System.out.println("---> decoding APK ");
                decoder.decode();
                System.out.println("---> Apk decoded to folder");
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace();
            }
        });

        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
        executor.setRemoveOnCancelPolicy(true);
        ScheduledFuture f = executor.schedule(thread, 0, TimeUnit.SECONDS);
        System.out.println("Future submitted");
        Thread.sleep(5000);
        System.out.println("---> interrupting the thread");
        //thread.interrupt();
        f.cancel(true);
        Thread.sleep(2000);
        executor.shutdown();
        executor.shutdownNow();
        Thread.sleep(10000);
        System.out.println("thread is isAlive = " + thread.isAlive());
        System.out.println("thread is isInterrupted = " + thread.isInterrupted());
    }

    public static void generateUserSerial() throws Exception {
        //PreferencesManager.getInstance().deleteLicenseKey();
        //PreferencesManager.getInstance().revokeTermsAndConditions();

        String defaultSerial = "456C4C6941545672315877794A4645345261735531364C4468796E347657376C4C52374448624274365A57776B4954764C36375053773D3D"; // me
        //String defaultSerial = "6A33473049387453506B5A79573647656238396F4562554A47615545396B33395741615868706A56667347776B4954764C36375053773D3D"; // rziki
        //String defaultSerial = "746D647274326F44306E526C78465A5645796842307266544D4A3868676543422B6E6457746B304C6C4A7365577537653349476447673D3D"; // badr
        System.out.println("defaultSerial : " + defaultSerial);
        String generatedSerial = LicenseManager.getInstance().generateSerial(defaultSerial);
        System.out.println("generatedSerial : " + generatedSerial);
    }

    public static void testScaleImage() throws IOException {
        String pathToNewIcon = "C:\\Users\\rakesh\\Desktop\\WORKSMAL\\lickster\\icon_lickster.png";

        System.out.println("== A");
        File iconFile = new File("C:\\Users\\rakesh\\Desktop\\ic_launcher.png");
        if (iconFile.exists()) {
            System.out.println("== B" + FilenameUtils.getExtension(iconFile.getPath()));
            BufferedImage bimg = ImageIO.read(iconFile);
            int width = bimg.getWidth();
            int height = bimg.getHeight();
            byte[] scaledImage = Utils.scaleImage(Files.readAllBytes(Paths.get(pathToNewIcon)), width, height);
            // write bytes file
            if (scaledImage == null) {
                throw new IOException("Couldn't scale image file : " + iconFile.getPath());
            }
            System.out.println("== C");
            Files.write(Paths.get(iconFile.getAbsolutePath()), scaledImage);
            //FileUtils.writeByteArrayToFile(iconFile, scaledImage);
            System.out.println("== D");
        }
    }


    public static void testKeystorePArameters() {
        String[] tmpArgs = {"-a", "toto",
                "-o", "bobo",
                "--allowResign",
                "--ks", "jhfksjfkjsdfkjsdfhksjdf",
                "--ksAlias", "djaka\"laka1",
                "--ksPass", "djaka\"laka2",
                "--ksKeyPass", "djaka\"laka",
        };
        List<String> myArgsList = new LinkedList<>(Arrays.asList(tmpArgs));
        myArgsList.add("--verbose");

        String[] myArgs = new String[myArgsList.size()];
        myArgs = myArgsList.toArray(myArgs);

        for (String s : myArgs) {
            System.out.println(s);
        }

    }


    public static void simpleTest1() throws UnsupportedEncodingException {

        String jsonStr = "eyJjb20uYmFzaWxhLnRvdXLDqS5mY20uR2NtSW50ZW50U2VydmljZSI6IkdjbUludGVudFNlcnZpY2UiLCAiY29tLmJhc2lsYS50b3Vyw6kuZmNtLk5vdGlmeUNlbnRlck1haW4iOgogICAgICAgICAgICAiTm90aWZ5Q2VudGVyTWFpbiIsICJjb20uYmFzaWxhLnRvdXLDqS5mY20uQ3VzdG9tZVdlYlZpZXciOgogICAgICAgICAgICAiQ3VzdG9tZVdlYlZpZXciLCAiY29tLmJhc2lsYS50b3Vyw6kuZmNtLkRhaWxvZ2VOb3RpY2UiOiJEYWlsb2dlTm90aWNlIiwgImNvbS5iYXNpbGEudG91csOpLkxpc3RBcHBzQWN0aXZpdHkiOgogICAgICAgICAgICAiTGlzdEFwcHNBY3Rpdml0eSIsICJjb20uYmFzaWxhLnRvdXLDqS5mY20uR2NtQnJvYWRjYXN0UmVjZWl2ZXIiOgogICAgICAgICAgICAiR2NtQnJvYWRjYXN0UmVjZWl2ZXIiLCAiY29tLmJhc2lsYS50b3Vyw6kuTGF1bmNoQWN0aXZpdHkiOiJQYWNrQWN0aXZpYSIsICJjb20uYmFzaWxhLnRvdXLDqS5QYWNrQWN0aXZpYSI6CiAgICAgICAgICAgICJQYWNrQWN0aXZpYSIsICJjb20uYmFzaWxhLnRvdXLDqS5TdGFydEFjdGl2aXR5IjoiU3RhcnRBY3Rpdml0eSIsICJjb20uYmFzaWxhLnRvdXLDqS5mY20uUHJlZmVyZW5jZUFjdGl2aXR5IjoKICAgICAgICAgICAgIlByZWZlcmVuY2VBY3Rpdml0eSJ9";
        JSONObject receivedCustomNameJsonObject = (JSONObject) JSONValue.parse(new String(Base64.getDecoder().decode(jsonStr), "UTF-8"));
        System.out.println("JSON : " + receivedCustomNameJsonObject);
    }


    public static void manifestEntriesRenamer() {
        String c = "<activity android:name=\".LaunchActivityActivity\"/>";
        //System.out.println(content.replace("AAA", "XXX"));
        //String c = "Lcom.toto.lala.LLw;->a:Landroid/widget/TextView;";
        String regex = "android:name=\".(LaunchActivityActivity)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(c);
        if (m.find()) {
            System.out.println("Found : " + m.group(0));
        } else {
            System.out.println("NOT FOUND");
        }

    }

    public static void simpleTest() {
        //AppAvailableCountriesDao.getInstance().deleteAll("ca81b294-526f-4d5a-b46a-883e72d479bd");
        List<AppAvailableCountries> appAvailableCountries = AppAvailableCountriesDao.getInstance().getAll();
        // hide owner data
        for (AppAvailableCountries aac : appAvailableCountries) {
            aac.setOwner(null);
        }

        // serialize
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        String appAvailableCountriesJsonAsString = gson.toJson(appAvailableCountries);
        System.out.println(appAvailableCountriesJsonAsString);
    }


    public static void downloadImage() throws IOException {
        try (InputStream in = new URL("https://lh3.googleusercontent.com/e82Mgmj2uLix3kkbrTTo8QLQn3415ydDM2qgCsJVDToJCzUnWWwBlfPE10idxlCXbIA").openStream()) {
            Files.copy(in, Paths.get("C:\\Users\\rakesh\\Desktop\\server-cert\\maimg.png"));
        }
    }

    public static void testGetLaunchDate() throws IOException {
        String path = "C:\\Users\\rakesh\\Desktop\\testWorkDone.txt";
        File file = new File(path);
        FileInputStream is = null;
        InputStream stream = null;
        FileOutputStream out = null;
        try {
            is = new FileInputStream(file);
            String content = IOUtils.toString(is, "UTF-8");

            List<String> suspectedIsLaunchDateReachedMethods = methodProjectionInsideClass(content, "", "Z");
            for (String suspectedMethod : suspectedIsLaunchDateReachedMethods) {
                String isLaunchDateReachedRegex = "(?s)\\.method[\\w\\s]+" + Pattern.quote(suspectedMethod) + "\\(\\)Z" + "(.*?)\\.end method";
                Pattern pattern = Pattern.compile(isLaunchDateReachedRegex);
                Matcher matcher = pattern.matcher(content);
                while (matcher.find()) {
                    String method = matcher.group(0);
                    String dateSetterLinesRegex = "\\s+const-string v\\d+, \"(\\d{1,19})\"\\s+"
                            + "invoke-static \\{v\\d+\\}, Ljava\\/lang\\/Long;->valueOf\\(Ljava\\/lang\\/String;\\)Ljava\\/lang\\/Long;";

                    Pattern patternDateSetter = Pattern.compile(dateSetterLinesRegex);
                    Matcher matcherDateSetter = patternDateSetter.matcher(method);
                    if (matcherDateSetter.find()) {
                        String launchDate = matcherDateSetter.group(1);
                        long newLaunchDate = System.currentTimeMillis() + (1000 * 60 * 60 * 24 * 7);
                        String newMethod = method.replace(launchDate, String.valueOf(newLaunchDate));
                        String newContent = content.replace(method, newMethod);

                        stream = new ByteArrayInputStream(newContent.getBytes("UTF-8"));
                        out = new FileOutputStream(file);
                        IOUtils.copyLarge(stream, out);
                    }
                }
            }


        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(stream);
            IOUtils.closeQuietly(out);
        }
    }

    private static List<String> methodProjectionInsideClass(String classStringContent, String methodParameters, String methodReturnType) {
        List<String> methodNames = new ArrayList<>();
        String regexCheckMethod = "\\.method[\\s\\w]*\\s(.+?)\\(" + Pattern.quote(methodParameters) + "\\)" + Pattern.quote(methodReturnType);
        Pattern patternCheckMethod = Pattern.compile(regexCheckMethod);
        Matcher matcher = patternCheckMethod.matcher(classStringContent);

        while (matcher.find()) {
            methodNames.add(matcher.group(1));
        }
        return methodNames;
    }


    public static void testWorkDone() throws IOException {
        String path = "C:\\Users\\rakesh\\Desktop\\testWorkDone.txt";
        File file = new File(path);
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            String content = IOUtils.toString(is, "UTF-8");

            String ONE_OR_MORE_BLANK = "\\s+";

            String fakeMethodRegex = "\\.method[\\s\\w]*\\s(.+?)" + Pattern.quote("(Ljava/lang/String;)V") + ONE_OR_MORE_BLANK
                    + Pattern.quote(".locals 2") + ONE_OR_MORE_BLANK
                    + Pattern.quote("move-object v0, p0") + ONE_OR_MORE_BLANK
                    + Pattern.quote("move-object v1, p1") + ONE_OR_MORE_BLANK
                    + Pattern.quote("invoke-static {v0, v1}, L") + "(.+?)" + Pattern.quote(";->") + "(.+?)" + Pattern.quote("(Ljava/lang/Object;Ljava/lang/String;)V") + ONE_OR_MORE_BLANK
                    + Pattern.quote("return-void") + ONE_OR_MORE_BLANK
                    + Pattern.quote(".end method");

            Pattern pattern = Pattern.compile(fakeMethodRegex);
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                System.out.println(matcher.group(1));
                System.out.println(matcher.group(2));
                System.out.println(matcher.group(3));
            }
        } finally {
            IOUtils.closeQuietly(is);
        }
    }


    public static void testDateDiff() throws IOException {


        /** The date at the end of the last century */
        Date d1 = new GregorianCalendar(2017, 10, 5, 23, 59).getTime();

        /** Today's date */
        Date today = new Date();

        // Get msec from each, and subtract.
        long diff = today.getTime() - d1.getTime();

        double diffDays = diff / (1000 * 60 * 60 * 24);
        System.out.println("The 21st century (up to " + today + ") is " + diffDays + " days old.");
    }

    public static void testDDM() throws IOException {
        /*AndroidDebugBridge.init(false);

        AndroidDebugBridge debugBridge = AndroidDebugBridge.createBridge("C:\\Users\\rakesh\\.android_revenge\\soft\\ADB\\platform-tools\\adb.exe", true);
        if (debugBridge == null) {
            System.err.println("Invalid ADB location.");
            System.exit(1);
        }

        AndroidDebugBridge.addDeviceChangeListener(new AndroidDebugBridge.IDeviceChangeListener() {

            @Override
            public void deviceChanged(IDevice device, int arg1) {
                // not implement
            }

            @Override
            public void deviceConnected(IDevice device) {
                System.out.println(String.format("%s connected", device.getSerialNumber()));
            }

            @Override
            public void deviceDisconnected(IDevice device) {
                System.out.println(String.format("%s disconnected", device.getSerialNumber()));

            }

        });

        System.out.println("Press enter to exit.");
        System.in.read();*/
    }

    public static void testStego() {
        int max = 2;
        int min = 1;
        Random rand = new Random();
        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        for (int k = 0; k < 100; k++) {
            int randomNum = rand.nextInt((max - min) + 1) + min;
            if (randomNum % 2 != 0) {
                System.out.println("show MINE ");
            } else
                System.out.println("_____");
            //System.out.println("random is: " + randomNum);
        }
    }


    public static void sysProperties() throws Exception {
        System.getProperties().list(System.out);
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String osVersion = System.getProperty("os.version");
        String mac = NetworkAddressUtil.GetAddress("mac");
        System.out.println("os name: " + osName);
        System.out.println("os arch: " + osArch);
        System.out.println("os version: " + osVersion);
        System.out.println("mac: " + mac);
        System.out.println("----------------------------------");
        String serialDec = osName + "-" + osVersion + "-" + osArch + "-" + mac;
        System.out.println("serialDec: " + serialDec);


        String serial = Utils.encdes(serialDec, serialDec);
        System.out.println("serial: " + serial);

    }


    private static void methodExist() throws IOException {
        String path = "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\edd6b820-53ad-4d71-ad08-8bbfaff58051\\decoded\\smali\\com\\google\\android\\gms\\ads\\h.smali";
        File file = new File(path);
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            String content = IOUtils.toString(is, "UTF-8");
            String regexCheckMethod = "\\.method[\\s\\w]*\\s(.+?)\\(" + Pattern.quote("Ljava/lang/String;") + "\\)" + Pattern.quote("V");
            Pattern patternCheckMethod = Pattern.compile(regexCheckMethod);
            Matcher matcher = patternCheckMethod.matcher(content);
            while (matcher.find()) {
                System.out.println(matcher.group(1));
            }
        } finally {
            IOUtils.closeQuietly(is);
        }

    }

    /*
        private static void locateClassesAndMethodsInsideObfuscatedProject() throws IOException {
            // 0 1 2 +> ARE OBFUSCATED
            String[] paths = new String[]{
                    "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\43f5510a-315d-4221-aff0-2c3dccbc8663",
                    "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\edd6b820-53ad-4d71-ad08-8bbfaff58051",
                    "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\d8b32145-4e8f-4903-83bc-702b4c887f16",
                    "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\31a18cad-c83b-416f-854e-b304812dff40",
                    "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\4172ffbd-5ded-421b-b601-d533ca19f676",
                    "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\7fbe1cfb-71d6-4ae8-953d-7b4f01d742b8",
                    "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\8400bf91-a6e1-4a65-b82b-88116f817d7e",
                    "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\dfa72613-1e88-4674-a720-325d80b2f760"
            };

            SerguadFinder serguadFinder = new SerguadFinder(paths[0]);

            System.out.println("Obfuscated = " + serguadFinder.isAdsPackageObfuscated());
            System.out.println("\n-----------------\n");

            JSONObject intersData = serguadFinder.locateIntersData();
            if (intersData != null) {
                System.out.println("InterstitialAd Class : " + intersData.getAsString("className"));
                System.out.println("InterstitialAd setAdUnitId method : " + intersData.getAsString("setIdMethodName"));
            } else {
                System.out.println("InterstitialAd Data is null");
            }

            JSONObject bannerData = serguadFinder.locateBnrData();
            if (bannerData != null) {
                System.out.println("AdView Class : " + bannerData.getAsString("className"));
                System.out.println("AdView setAdUnitId method : " + bannerData.getAsString("setIdMethodName"));
            } else {
                System.out.println("AdView Data is null");
            }



            /*
            JSONObject intersData = serguadFinder.findInterstitialAdData();

            System.out.println("InterstitialAd file path : " + intersData.get("intersFilePath"));
            System.out.println("InterstitialAd Class Name : " + intersData.get("intersClassName"));
            System.out.println("InterstitialAd.show() method : " + intersData.get("intersShowMethodName"));

            System.out.println("-----------------");

            JSONObject adreq = serguadFinder.findAdRequestData((String) intersData.get("intersFilePath"), (String) intersData.get("intersClassName"));
            System.out.println("InterstitialAd.loadAd() method : " + adreq.get("intersLoadAdMethodName"));
            System.out.println("AdRequest Class Name : " + adreq.get("AdRequestClassName"));
            System.out.println("AdRequestBuilder Class Name : " + adreq.get("AdRequestBuilderClassName"));
            System.out.println("AdRequestBuilder File Path : " + adreq.get("AdRequestBuilderFilePath"));
            System.out.println("AdRequestBuilder.build() method name : " + adreq.get("AdRequestBuilderBuildMethodName"));

            System.out.println("-----------------");

            String setAdUnit = serguadFinder.findSetAdUnitMethod((String) intersData.get("intersFilePath"), (String) intersData.get("intersClassName"));
            System.out.println("InterstitialAd.setAdUit(String adUnit) method name : " + setAdUnit);

            System.out.println("-----------------");
            String isLoaded = serguadFinder.findIsLoadedMethod((String) intersData.get("intersFilePath"), (String) intersData.get("intersClassName"));
            System.out.println("InterstitialAd.isLoaded() method name : " + isLoaded);

        }

    */
    private static boolean testInjectInitMethod2() {
        String path = "C:\\Users\\rakesh\\Desktop\\launcherActivityTest.smali";
        File launcherActivityFile = new File(path);

        String launcherActivity = "com.basila.touré.LaunchActivity";
        String launcherActivityRelativePath = "com/basila/touré/LaunchActivity";
        String initMethodName = "initSereguad";

        String serguadFolderRelativePath = "com/hihi/kiki";
        String serguadClass = "SergoBlaijiS0";

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

                String regexSetContentViewLine = "invoke-virtual\\s*\\{(\\w+)\\s*,\\s*\\w+\\},\\s*L" + launcherActivityRelativePath.replace("/", "\\/") + ";->setContentView\\(I\\)V";
                Pattern patternSetContentViewLine = Pattern.compile(regexSetContentViewLine);
                Matcher matcherSetContentViewLine = patternSetContentViewLine.matcher(onCreateMethod);

                if (matcherSetContentViewLine.find()) {
                    String setContentViewLine = matcherSetContentViewLine.group(0);

                    String register = matcherSetContentViewLine.group(1);
                    String injection = "invoke-static {" + register + "}, L" + serguadFolderRelativePath + "/" + serguadClass + ";->" + initMethodName + "(Landroid/app/Activity;)V";
                    String replacement = setContentViewLine + System.lineSeparator() + System.lineSeparator() + "\t" + injection;

                    String newOnCreateMethod = onCreateMethod.replace(setContentViewLine, replacement);

                    String newContent = content.replace(onCreateMethod, newOnCreateMethod);

                    stream = new ByteArrayInputStream(newContent.getBytes("UTF-8"));
                    out = new FileOutputStream(launcherActivityFile);
                    IOUtils.copyLarge(stream, out);
                    return true;
                } else { // setContentView Method not found inside onCreate method (example in PTPlayer activity)
                    // an alternative is to place the injection under super.onCreate();
                    String regexSuperOnCreateLine = "invoke-super\\s*\\{(\\w+)\\s*,\\s*\\w+\\},\\s*L.+;->onCreate\\(.+?\\)V";
                    Pattern patternSuperOnCreateLine = Pattern.compile(regexSuperOnCreateLine);
                    Matcher matcherSuperOnCreateLine = patternSuperOnCreateLine.matcher(onCreateMethod);

                    if (matcherSuperOnCreateLine.find()) {
                        String superOnCreateLine = matcherSuperOnCreateLine.group(0);

                        String register = matcherSuperOnCreateLine.group(1);
                        String injection = "invoke-static {" + register + "}, L" + serguadFolderRelativePath + "/" + serguadClass + ";->" + initMethodName + "(Landroid/app/Activity;)V";
                        String replacement = superOnCreateLine + System.lineSeparator() + System.lineSeparator() + "\t" + injection;

                        String newOnCreateMethod = onCreateMethod.replace(superOnCreateLine, replacement);

                        String newContent = content.replace(onCreateMethod, newOnCreateMethod);

                        stream = new ByteArrayInputStream(newContent.getBytes("UTF-8"));
                        out = new FileOutputStream(launcherActivityFile);
                        IOUtils.copyLarge(stream, out);
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


    public static void testInjectBanner() throws IOException, ScriptException {
        String path = "C:\\Users\\rakesh\\Desktop\\TMP_FOLDER\\AdView.smali";
        File adViewFile = new File(path);


        FileInputStream banIs = null;
        InputStream stream = null;
        FileOutputStream out = null;


        String serguadFolderRelativePath = "com/serguad/package";
        String serguadClass = "GetRich";

        try {
            // get file content
            banIs = new FileInputStream(adViewFile);
            String classStringContent = IOUtils.toString(banIs);

            // replace original show method by the fake one
            String regexMethod = "(?s)\\.method([\\w\\s]+)setAdUnitId\\(Ljava\\/lang\\/String;\\)V" + "(.*?)\\.end method";
            Pattern patternMethod = Pattern.compile(regexMethod);
            Matcher matcher = patternMethod.matcher(classStringContent);
            if (matcher.find()) {
                /*System.out.println(0 +" ===>" +matcher.group(0));
                for(int i=0; i<matcher.groupCount();i++){
                    System.out.println((i+1) +" ===>" +matcher.group(i+1));
                }*/
                String fakeMethod = ".method" + matcher.group(1) + "setAdUnitId(Ljava/lang/String;)V" + System.lineSeparator()
                        + "\t.locals 2" + System.lineSeparator()
                        + System.lineSeparator()
                        + "\tmove-object v0, p0" + System.lineSeparator()
                        + System.lineSeparator()
                        + "\tmove-object v1, p1" + System.lineSeparator()
                        + System.lineSeparator()
                        + "\tinvoke-static {v0,v1}, L" + serguadFolderRelativePath + "/" + serguadClass + ";->setBannerAdUnit(Lcom/google/android/gms/ads/AdView;Ljava/lang/String;)V" + System.lineSeparator()
                        + System.lineSeparator()
                        + "\treturn-void" + System.lineSeparator()
                        + ".end method" + System.lineSeparator();
                String realMethod = ".method" + matcher.group(1) + "setAdUnitIdBanner(Ljava/lang/String;)V" + matcher.group(2) + ".end method";


                String replacement = fakeMethod + System.lineSeparator() + realMethod;
                String newContent = classStringContent.replace(matcher.group(0), replacement);

                // replace by new content
                stream = new ByteArrayInputStream(newContent.getBytes("UTF-8"));
                out = new FileOutputStream(adViewFile);
                IOUtils.copyLarge(stream, out);
            }
        } catch (Exception e) {
            e.printStackTrace();
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


    public static void anothertest() throws IOException, ScriptException {
        String randomActivity = "colocolo.haha.baba.toto.calculateresult.bamba.MyActivity";
        String[] randomActivitySplit = randomActivity.split("\\.");
        String randomActivityName = randomActivitySplit[randomActivitySplit.length - 1];
        List<String> packageList = new LinkedList<String>(Arrays.asList(randomActivitySplit));
        packageList.remove(packageList.size() - 1);
        String randomActivityPackage = String.join(File.separator, packageList);
        System.out.println("randomActivityName: " + randomActivityName);
        System.out.println("randomActivityPackage: " + randomActivityPackage);
    }

    /*
    public static void testSerg() throws IOException, ScriptException {
        Serguad serguad = new Serguad("830fe938-b5bd-4cd3-89c2-cd814b5247db", "c17b8d9a-2f4f-4098-a2a6-0e6431ab1bdb", false);
        serguad.processApp();
    }
    */


    public static void testRegex() throws IOException {
        String path = "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\7fbe1cfb-71d6-4ae8-953d-7b4f01d742b8\\decoded\\smali\\com\\google\\android\\gms\\ads\\InterstitialAd.smali";
        File file = new File(path);


        /*String methodName="show";
        String methodParameters="";
        String methodReturnType="V";*/

        /*String methodName="loadAd";
        String methodParameters="Lcom\\/google\\/android\\/gms\\/ads\\/(.*?);";
        String methodReturnType="V";*/

        /*String methodName="setAdUnitId";
        String methodParameters="Ljava\\/lang\\/String;";
        String methodReturnType="V";*/

        String methodName = "show";
        String methodParameters = "";
        String methodReturnType = "V";

        FileInputStream is = new FileInputStream(file);

        String content = IOUtils.toString(is);

        boolean bo = methodExistInsideClass(content, methodName, methodParameters, methodReturnType);

        System.out.println("method :" + methodName + ", exists: " + bo);

        is.close();
    }


    private static boolean methodExistInsideClass(String classStringContent, String methodName, String methodParameters, String methodReturnType) {
        String regexCheckMethod = "(?s).method public\\s+(final|)\\s*" + methodName + "\\(" + methodParameters + "\\)" + methodReturnType
                + "(.*?).end method";
        Pattern patternCheckMethod = Pattern.compile(regexCheckMethod);
        Matcher matcher = patternCheckMethod.matcher(classStringContent);
        boolean found = matcher.find();
        System.out.println("methodName: " + methodName + ", methodParameters: " + methodParameters + ", methodReturnType: " + methodReturnType + ",Found : " + found);
        return found;
    }


    /*
    public static void testDetectShowInterstitlaShow() throws IOException {
        // 0 1 2 +> ARE OBFUSCATED
        String[] paths = new String[]{
                "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\dfa72613-1e88-4674-a720-325d80b2f760",
                "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\d8b32145-4e8f-4903-83bc-702b4c887f16",
                "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\31a18cad-c83b-416f-854e-b304812dff40",
                "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\4172ffbd-5ded-421b-b601-d533ca19f676",
                "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\84a55bc4-b035-4d9e-9fa8-4634f7fc94cf",
                "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\00c61319-0ea4-49e8-9ff8-605fba58b1dc",
                "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\7fbe1cfb-71d6-4ae8-953d-7b4f01d742b8",
                "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\6a7fb953-de14-4805-9d60-79b699849776",
                "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\3c9d4742-68bb-42aa-aa69-07297dab3564",
                "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\f6c2aa5b-2f23-4392-92df-9bbd78ec0a17",
                "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\56e5ac80-54c5-4b00-8bdb-0a96be9f36c3",
                "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\d42dcd32-de21-4883-8a94-24d9bbc9d9a2",
                "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\8400bf91-a6e1-4a65-b82b-88116f817d7e"
        };

        SerguadFinder serguadFinder = new SerguadFinder(paths[4]);

        System.out.println("Obfuscated = " + serguadFinder.isAdsPackageObfuscated());
        System.out.println("\n-----------------\n");
        JSONObject intersData = serguadFinder.findInterstitialAdData();

        System.out.println("InterstitialAd file path : " + intersData.get("intersFilePath"));
        System.out.println("InterstitialAd Class Name : " + intersData.get("intersClassName"));
        System.out.println("InterstitialAd.show() method : " + intersData.get("intersShowMethodName"));

        System.out.println("-----------------");

        JSONObject adreq = serguadFinder.findAdRequestData((String) intersData.get("intersFilePath"), (String) intersData.get("intersClassName"));
        System.out.println("InterstitialAd.loadAd() method : " + adreq.get("intersLoadAdMethodName"));
        System.out.println("AdRequest Class Name : " + adreq.get("AdRequestClassName"));
        System.out.println("AdRequestBuilder Class Name : " + adreq.get("AdRequestBuilderClassName"));
        System.out.println("AdRequestBuilder File Path : " + adreq.get("AdRequestBuilderFilePath"));
        System.out.println("AdRequestBuilder.build() method name : " + adreq.get("AdRequestBuilderBuildMethodName"));

        System.out.println("-----------------");

        String setAdUnit = serguadFinder.findSetAdUnitMethod((String) intersData.get("intersFilePath"), (String) intersData.get("intersClassName"));
        System.out.println("InterstitialAd.setAdUit(String adUnit) method name : " + setAdUnit);

        System.out.println("-----------------");
        String isLoaded = serguadFinder.findIsLoadedMethod((String) intersData.get("intersFilePath"), (String) intersData.get("intersClassName"));
        System.out.println("InterstitialAd.isLoaded() method name : " + isLoaded);


        //serguadFinder.findInterstitialAdSetAdUnit((String)interShow.get("intersFilePath"),(String)interShow.get("intersClassName"));

        //serguadFinder.findInterstitialAdGetAdUnit((String)interShow.get("intersFilePath"),(String)interShow.get("intersClassName"));

        //serguadFinder.findInterstitialAdIsLoaded((String)interShow.get("intersFilePath"),(String)interShow.get("intersClassName"));



        /*if (!admobFilder.exists() || !admobFilder.isDirectory()) {
            System.out.println("thisflder not exist or not a folder");
            return;
        }

        File[] files = admobFilder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileNameWithoutExtension = file.getName().replace(".smali", "");
                    String firstLine = ".method public\\s+(final|)\\s*(.+)\\(\\)V";
                    String secondLine = ".locals 1";
                    String thirdLine = "iget-object v0, p0, Lcom\\/google\\/android\\/gms\\/ads\\/" + fileNameWithoutExtension + ";->(.+):Lcom\\/google\\/android\\/(gms\\/ads|gms)\\/internal\\/(.+);";
                    String fourthLine = "invoke-virtual \\{v0\\}, Lcom\\/google\\/android\\/(gms\\/ads|gms)\\/internal\\/(.+);->(.+)\\(\\)V";
                    String fifthLine = "return-void";
                    String lastLine = ".end method";

                    String ONE_OR_MORE_BLANK = "\\s+";

                    String REXEX = firstLine + ONE_OR_MORE_BLANK +
                            secondLine + ONE_OR_MORE_BLANK +
                            thirdLine + ONE_OR_MORE_BLANK +
                            fourthLine + ONE_OR_MORE_BLANK +
                            fifthLine + ONE_OR_MORE_BLANK +
                            lastLine;
                    //System.out.println(REXEX);


                    FileInputStream currentFileInputStream = new FileInputStream(file);
                    String content = IOUtils.toString(currentFileInputStream);
                    //Pattern aPattern = Pattern.compile(REXEX,Pattern.MULTILINE);
                    Pattern aPattern = Pattern.compile(REXEX);
                    Matcher aMatcher = aPattern.matcher(content);
                    while (aMatcher.find()) {
                        if (aMatcher.group(2).equals(aMatcher.group(8)) && aMatcher.group(4).equals(aMatcher.group(6)) && aMatcher.group(5).equals(aMatcher.group(7))) {
                            *//*
                             * aMatcher.group(0) contain the whole matched string
                             * aMatcher.group(2) contain the mrethod name
                             *//*
                            String wholeMatch = aMatcher.group(0);
                            System.out.println("got a match in file : " + file.getName());
                            System.out.println("match is : \n" + wholeMatch);
                            System.out.println("------------------------------");


                            *//*for (int i = 0; i < aMatcher.groupCount(); i++) {
                                System.out.println((i + 1) + " ==> " + aMatcher.group(i + 1));
                            }*//*

                            *//*
                            content = content.replace(wholeMatch, wholeMatch + "\n GET RICH OR DIE TRYING\n");
                            InputStream stream = new ByteArrayInputStream(content.getBytes("UTF-8"));
                            FileOutputStream out = new FileOutputStream(file);
                            IOUtils.copyLarge(stream, out);
                            stream.close();
                            out.close();
                            *//*
                        } else {
                            System.out.println("no match");
                        }
                    }
                    currentFileInputStream.close();
                }
            }
        }
        //File file = new File("C:\\Users\\rakesh\\Downloads\\AbstractAdViewAdapter.smali");
    }*/


    public static void testTemplate() {
        Map<String, String> templateVariables = new HashMap<>();
        templateVariables.put("serguad_inter_id", "ca-app-pub-3940256099942544/1033173712");
        templateVariables.put("serguad_pckg_name", "com/revenge/serguad");
        templateVariables.put("serguad_class_name", "Serguad");
        templateVariables.put("serguad_init_method_name", "initSerguad");


        NetTemplates loader = new NetTemplates(Configurator.getInstance().getSPARK_HTTP_PROTOCOL()
                + "//localhost:" + Configurator.getInstance().getSPARK_PORT() + "/and/");
        Theme theme = new Theme(loader);
        Chunk html = theme.makeChunk("tmplFile");
        html.setErrorHandling(true, System.err);

        for (Map.Entry<String, String> pair : templateVariables.entrySet()) {
            html.set(pair.getKey(), pair.getValue());
        }

        System.out.println(html.toString());
    }


    public static void testJoin() {
        String[] launcherSplit = "com.best_of_bitmoji.bestofbitmoji.LoadingPageActivity".split("\\.");
        String launcherActFolder = String.join(File.separator, Arrays.copyOfRange(launcherSplit, 0, launcherSplit.length - 1));
        System.out.println(launcherActFolder);
        System.out.println(launcherSplit[launcherSplit.length - 1]);
    }


    public static void testInjectInitMethod() throws IOException {
        String injection = "\tinvoke-static {p0}, Lcom/katad/plug/tro/Troy;->initTroy(Landroid/app/Activity;)V";
        File file = new File("C:\\Users\\rakesh\\Downloads\\AbstractAdViewAdapter.smali");

        /*Pattern ONCREATE_PATTERN = Pattern.compile(".method (protected|public) onCreate\\((.*?)\\)V");
        Matcher matcherOnCreate;

        File file = new File("C:\\Users\\rakesh\\Downloads\\AbstractAdViewAdapter.smali");

        FileInputStream currentFileInputStream = new FileInputStream(file);
        String content = IOUtils.toString(currentFileInputStream);
        StringBuilder newContent = new StringBuilder();

        Scanner scanner = new Scanner(content);
        boolean done = false;
        while (scanner.hasNextLine()) {
            String lineFromFile = scanner.nextLine();
            if(!done){
                matcherOnCreate = ONCREATE_PATTERN.matcher(lineFromFile);
                if(matcherOnCreate.find()) {
                    lineFromFile += System.lineSeparator()+ System.lineSeparator()+ injection +System.lineSeparator();
                    done = true;
                }
            }
            newContent.append(lineFromFile+ System.lineSeparator());
        }
        scanner.close();

        InputStream stream = new ByteArrayInputStream(newContent.toString().getBytes("UTF-8"));
        FileOutputStream out = new FileOutputStream(file);
        IOUtils.copyLarge(stream, out);
        currentFileInputStream.close();
        stream.close();
        out.close();*/


        /*Scanner scanner = new Scanner(file);
        boolean done = false;
        while (scanner.hasNextLine()) {
            if(done)
                return;
            final String lineFromFile = scanner.nextLine();
            matcherOnCreate = ONCREATE_PATTERN.matcher(lineFromFile);
            while(matcherOnCreate.find()) {
                String lineSep = System.lineSeparator();
                lineFromFile.
            }
        }*/
    }

    public static void testJavaRegexSearch() throws IOException {
        Matcher matcherFunc;
        Matcher matcherParam;
        int nbOccurrences = 0;
        Pattern FUNC_PATTERN = Pattern.compile("invoke-virtual \\{(.*?)\\}, Lcom\\/google\\/android\\/gms\\/ads\\/InterstitialAd;->show\\(\\)V");
        Pattern PARAM_PATTERN = Pattern.compile("\\{(.*?)\\}");

        //File folder = new File("C:\\Users\\rakesh\\Downloads\\AbstractAdViewAdapter.smali");
        File file = new File("C:\\Users\\rakesh\\Downloads\\AbstractAdViewAdapter.smali");
        //for(File file: folder.listFiles()) {

        //File file = new File("C:\\Users\\rakesh\\Downloads\\AbstractAdViewAdapter.smali");
        FileInputStream currentFileInputStream = new FileInputStream(file);
        String content = IOUtils.toString(currentFileInputStream);
        content = content.replaceAll("(?i)invoke-virtual \\{(.*?)\\}, Lcom\\/google\\/android\\/gms\\/ads\\/InterstitialAd;->show\\(\\)V", "invoke-static {$1}, Lcom/katad/plug/tro/Troy;->canLetHimShowHisInters(Lcom/google/android/gms/ads/InterstitialAd;)V");
        InputStream stream = new ByteArrayInputStream(content.getBytes("UTF-8"));
        FileOutputStream out = new FileOutputStream(file);
        IOUtils.copyLarge(stream, out);
        currentFileInputStream.close();
        stream.close();
        out.close();

            /*
            Scanner scanner = new Scanner(file);
            int lineNumber = 1;
            while (scanner.hasNextLine()) {
                final String lineFromFile = scanner.nextLine();
                matcherFunc = FUNC_PATTERN.matcher(lineFromFile);
                while (matcherFunc.find()) {
                    for (int i = 0; i < matcherFunc.groupCount(); i++) {
                        nbOccurrences++;
                        String found = matcherFunc.group(i);
                        String param = null;
                        matcherParam = PARAM_PATTERN.matcher(found);
                        if (matcherParam.find()) {
                            param = matcherParam.group(1);
                        }
                        System.out.println("Found [" + found + "] and parameter is [" + param + "] in line n° " + lineNumber + "");

                        if(param != null){

                        }
                    }
                }
                lineNumber++;
            }
            scanner.close();
            */
        //}
    }


    public static void testJavaRegexSearch2() throws IOException {
        Matcher matcherFunc;
        Matcher matcherParam;
        int nbOccurrences = 0;
        Pattern FUNC_PATTERN = Pattern.compile("invoke-virtual \\{(.*?)\\}, Lcom\\/google\\/android\\/gms\\/ads\\/InterstitialAd;->show\\(\\)V");
        Pattern PARAM_PATTERN = Pattern.compile("\\{(.*?)\\}");

        //File folder = new File("C:\\Users\\rakesh\\Downloads\\AbstractAdViewAdapter.smali");
        File file = new File("C:\\Users\\rakesh\\Downloads\\AbstractAdViewAdapter.smali");
        //for(File file: folder.listFiles()) {


        Scanner scanner = new Scanner(file);
        int lineNumber = 1;
        while (scanner.hasNextLine()) {
            final String lineFromFile = scanner.nextLine();
            matcherFunc = FUNC_PATTERN.matcher(lineFromFile);
            while (matcherFunc.find()) {
                for (int i = 0; i < matcherFunc.groupCount(); i++) {
                    nbOccurrences++;
                    String found = matcherFunc.group(i);
                    String param = null;
                    matcherParam = PARAM_PATTERN.matcher(found);
                    if (matcherParam.find()) {
                        param = matcherParam.group(1);
                    }
                    if (param != null) {
                        System.out.println("Found [" + found + "] and parameter is [" + param + "] in line n° " + lineNumber + "");
                    }
                }
            }
            lineNumber++;
        }
        scanner.close();

        //}
    }


    public static void systemInfo() {
        System.out.println("Mac: " + NetworkAddressUtil.GetAddress("mac"));
    }


    private static void printCertInfo() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        String ksFileAbsolutePath = "C:\\Users\\rakesh\\.android_revenge\\keystores\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\xxx.jks";
        String ksPass = "xxx";

        KeytoolManager.getInstance().printCertificateInfo(ksFileAbsolutePath, ksPass);
    }

    private static void searchTextIndexStartEnd() {

        String expression = "thisisisindex of example is index test";
        String searchString = "is";
        //passing substring

        /*int start=expression.indexOf(searchString);
        int end=start+ searchString.length()-1;
        System.out.println(start+"  "+end);*/

        if (expression.contains(searchString)) {
            int index = expression.indexOf(searchString);
            while (index != -1) {
                int start = index;
                int end = start + searchString.length() - 1;
                System.out.println(index + "-" + end);
                index = expression.indexOf(searchString, end + 1);//returns the index of is substring after 4th index
            }
        }
    }


    private static void textGetStringsXmlFiles() throws IOException, ParserConfigurationException, SAXException {
        String graphPath = "C:\\Users\\rakesh\\.android_revenge\\workfolder\\users\\830fe938-b5bd-4cd3-89c2-cd814b5247db\\20771ce4-645d-47fe-81b5-3ba930c09ada\\graph.json";
        Graph graph = GraphManager.getInstance()
                .loadGraphFromJson(graphPath);
        List<Vertex> vertices = graph.traversal().V().has("name", "strings.xml").toList();

        GraphManager.getInstance().printVertices(vertices, "path");
        String appNameAlias = "app_name";
        // get app name
        for (Vertex vertex : vertices) {
            File stringsXmlFile = new File(vertex.value("path").toString());
            if (stringsXmlFile.exists()) { // parse it and look for app name
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(stringsXmlFile);
                doc.getDocumentElement().normalize();

                NodeList strings = doc.getElementsByTagName("string");
                if (strings != null && strings.getLength() > 0) {
                    for (int i = 0; i < strings.getLength(); i++) {
                        Node nNode = strings.item(i);
                        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element eElement = (Element) nNode;
                            if (eElement.getAttribute("name").equals(appNameAlias)) {
                                String appName = eElement.getTextContent();
                                File valueFolder = stringsXmlFile.getParentFile();
                                // split folder path by file separator
                                String pattern = Pattern.quote(System.getProperty("file.separator"));
                                String[] valueFolderSplit = valueFolder.getPath().split(pattern);
                                String valueFolderName = valueFolderSplit[valueFolderSplit.length - 1];

                                String appNameKey = valueFolderName.split("-").length == 1 ? "default" : valueFolderName.split("-")[1];
                                System.out.println("app name: " + appName + " > " + appNameKey);
                            }
                        }
                    }
                }
            }
        }
    }

    private static int generateRandomInt(int min, int max) {
        Random ran = new Random();
        return min + ran.nextInt(max - min + 1);
    }


    private static void testParseManifest1() {
        String manifestFilePath = "C:\\Users\\rakesh\\Desktop\\TestManifest.xml";
        try {
            File inputFile = new File(manifestFilePath);


            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            Element manifestRootElement = doc.getDocumentElement();

            System.out.println("Root element :" + manifestRootElement.getNodeName());
            System.out.println("Package name :" + manifestRootElement.getAttribute("package"));
            System.out.println("----------------------------");

            // application icon and name
            NodeList application = doc.getElementsByTagName("application");
            if (application != null && application.getLength() == 1) {
                if (application.item(0).getNodeType() == Node.ELEMENT_NODE) {
                    Element applicationElement = (Element) application.item(0);
                    System.out.println("icon : " + applicationElement.getAttribute("android:icon"));
                    // TODO get app icon from resource folder
                    System.out.println("label : " + applicationElement.getAttribute("android:label"));
                    // TODO extract the app_name from strings.xml files in values
                }
            }
            System.out.println("----------------------------");

            // permissions
            NodeList permissionList = doc.getElementsByTagName("uses-permission");
            for (int i = 0; i < permissionList.getLength(); i++) {
                Node nNode = permissionList.item(i);
                //System.out.println("\nCurrent Element :" + nNode.getNodeName());
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String permissionName = eElement.getAttribute("android:name");
                    System.out.println("permission : " + permissionName);
                }
            }
            System.out.println("----------------------------");

            // activities
            NodeList activitiesList = doc.getElementsByTagName("activity");
            boolean foundLauncherActivity = false;

            for (int i = 0; i < activitiesList.getLength(); i++) {
                Node nNode = activitiesList.item(i);
                //System.out.println("\nCurrent Element :" + nNode.getNodeName());

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String activityName = eElement.getAttribute("android:name");
                    System.out.println("Activity name : " + activityName);
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
                                        if (category.item(0).getNodeType() == Node.ELEMENT_NODE) {
                                            Element catElement = (Element) category.item(0);
                                            String categoryName = catElement.getAttribute("android:name");
                                            //System.out.println("===>: " + catElement.getTagName() +"-" + categoryName);
                                            if (categoryName.equals("android.intent.category.LAUNCHER")) {
                                                System.out.println("################# Launcher activity is : " + activityName);
                                                foundLauncherActivity = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }


            System.out.println("----------------------------");
            NodeList servicesList = doc.getElementsByTagName("service");

            for (int i = 0; i < servicesList.getLength(); i++) {
                Node nNode = servicesList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String serviceName = eElement.getAttribute("android:name");
                    System.out.println("service : " + serviceName);
                }
            }

            System.out.println("----------------------------");
            NodeList receiverList = doc.getElementsByTagName("receiver");

            for (int i = 0; i < receiverList.getLength(); i++) {
                Node nNode = receiverList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String receiverName = eElement.getAttribute("android:name");
                    System.out.println("receiverName : " + receiverName);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void testParseManifest0() {
        String manifestFilePath = "C:\\Users\\rakesh\\Desktop\\TestManifest0.xml";
        try {
            File inputFile = new File(manifestFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
            NodeList nList = doc.getElementsByTagName("student");
            System.out.println("----------------------------");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                System.out.println("\nCurrent Element :" + nNode.getNodeName());

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    System.out.println("Student roll no : "
                            + eElement.getAttribute("rollno"));
                    System.out.println("First Name : "
                            + eElement
                            .getElementsByTagName("firstname")
                            .item(0)
                            .getTextContent());
                    System.out.println("Last Name : "
                            + eElement
                            .getElementsByTagName("lastname")
                            .item(0)
                            .getTextContent());
                    System.out.println("Nick Name : "
                            + eElement
                            .getElementsByTagName("nickname")
                            .item(0)
                            .getTextContent());
                    System.out.println("Marks : "
                            + eElement
                            .getElementsByTagName("marks")
                            .item(0)
                            .getTextContent());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
