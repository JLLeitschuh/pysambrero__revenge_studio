package com.ninjaflip.androidrevenge.utils;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.enums.OS;
import com.ninjaflip.androidrevenge.utils.imageutils.GifDecoder;
import com.x5.template.Chunk;
import com.x5.template.Theme;
import com.x5.template.providers.NetTemplates;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Created by Solitario on 01/06/2017.
 */
public class Utils {

    /**
     * Attempts to calculate the size of a file or directory.
     * <p>
     * <p>
     * Since the operation is non-atomic, the returned value may be inaccurate.
     * However, this method is quick and does its best.
     */
    public static long fileOrFoderSize(Path path) {
        final AtomicLong size = new AtomicLong(0);
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {

                    System.out.println("skipped: " + file + " (" + exc + ")");
                    // Skip folders that can't be traversed
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

                    if (exc != null)
                        System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
                    // Ignore errors traversing a folder
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
        }
        return size.get();
    }

    /**
     * Load template html
     *
     * @param templateFolder
     * @param templateName
     * @return
     */
    public static String loadTemplateAsString(final String templateFolder, final String templateName) {
        return loadTemplateAsString(templateFolder, templateName, null);
    }

    public static String loadTemplateAsString(final String templateFolder, final String templateName, Map<String, String> varValues) {
        NetTemplates loader = new NetTemplates(Configurator.getInstance().getSPARK_HTTP_PROTOCOL()
                + "//localhost:" + Configurator.getInstance().getSPARK_PORT() + "/static/protected/template/" + templateFolder + "/");
        Theme theme = new Theme(loader);
        Chunk html = theme.makeChunk(templateName);
        html.setErrorHandling(true, System.err);
        // there is a problem of collision between Chunk and Mustache.js syntax because of braces
        // problem here '{{#isFree}}FREE{{/isFree}}{{^isFree}}PAID{{/isFree}}'
        // this is interpreted as '{' by chunk because '{#' is block tag for chunk template
        // a work around would be this code bellow
        html.set("mst_if_start", "{{#");
        html.set("mst_else_start", "{{^");

        // set template variables
        if (varValues != null) {
            for (Map.Entry<String, String> entry : varValues.entrySet()) {
                html.set(entry.getKey(), entry.getValue());
            }
        }

        // this line produces an error
        // [CHUNK_ERR: malformed content reference: '.' -- missing argument]
        // a workaround is to replace by {$mst_dot} using String.replace(src, dest)
        //html.set("mst_dot","{{.}}");
        return html.toString().replace("{$mst_dot}", "{{.}}");
    }

    /**
     * Scale image from its bytes
     */
    public static byte[] scaleImage(byte[] fileData, int width, int height) {
        return scaleImage(fileData, width, height, "png");
    }

    public static byte[] scaleImage(byte[] fileData, int width, int height, String formatName) {
        ByteArrayOutputStream buffer = null;
        ByteArrayInputStream in = new ByteArrayInputStream(fileData);
        try {
            BufferedImage img = ImageIO.read(in);
            if (height == 0) {
                height = (width * img.getHeight()) / img.getWidth();
            }
            if (width == 0) {
                width = (height * img.getWidth()) / img.getHeight();
            }
            Image scaledImage = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            BufferedImage imageBuff = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            imageBuff.getGraphics().drawImage(scaledImage, 0, 0, null);
            buffer = new ByteArrayOutputStream();
            ImageIO.write(imageBuff, formatName, buffer);
            return buffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("scale image error: " + e.getMessage());
            return null;
        } finally {
            try {
                in.close();
                if (buffer != null)
                    buffer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Get a thumbnail an image file as base64 string
     */
    public static String getImageThumbnailAsBase64String(File imageFile, int maxDim) {
        ByteArrayInputStream in = null;
        ByteArrayOutputStream buffer = null;
        FileInputStream fis = null;
        BufferedImage img = null;
        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(imageFile.getPath()));
            if (imageFile.getName().endsWith(".gif")) {
                GifDecoder d = new GifDecoder();
                fis = new FileInputStream(imageFile);
                d.read(fis);
                if (d.getFrameCount() > 0) {
                    img = d.getFrame(0);  // frame i
                }

            } else if (imageFile.getName().endsWith(".png")
                    || imageFile.getName().endsWith(".jpg")
                    || imageFile.getName().endsWith(".jpeg")
                    || imageFile.getName().endsWith(".bmp")) {
                in = new ByteArrayInputStream(fileBytes);
                img = ImageIO.read(in);
            }

            if (img == null)
                return null;

            int newWidth = img.getWidth();
            int newHeight = img.getHeight();
            if (newWidth > maxDim && newHeight > maxDim) {
                if (newWidth == newHeight) {
                    newWidth = maxDim;
                    newHeight = maxDim;
                } else if (newWidth > newHeight) {
                    float scale = (float) maxDim / newWidth;
                    newWidth = maxDim;
                    newHeight = Math.round(newHeight * scale);
                } else {
                    float scale = (float) maxDim / newHeight;
                    newHeight = maxDim;
                    newWidth = Math.round(newWidth * scale);
                }
            } else if (newWidth > maxDim) {
                float scale = (float) maxDim / newWidth;
                newWidth = maxDim;
                newHeight = Math.round(newHeight * scale);
            } else if (newHeight > maxDim) {
                float scale = (float) maxDim / newHeight;
                newHeight = maxDim;
                newWidth = Math.round(newWidth * scale);
            }

            Image scaledImage = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            BufferedImage imageBuff = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            imageBuff.getGraphics().drawImage(scaledImage, 0, 0, null);
            buffer = new ByteArrayOutputStream();
            ImageIO.write(imageBuff, "png", buffer);
            return Base64.getEncoder().encodeToString(buffer.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("scale image error: " + e.getMessage());
            return null;
        } finally {
            try {
                if (in != null)
                    in.close();
                if (fis != null)
                    fis.close();
                if (buffer != null)
                    buffer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Format file size to bytes human readable size
     *
     * @param size long size
     * @return String size
     */
    public static String formatFileSize(long size) {
        String hrSize = null;

        double b = size;
        double k = size / 1024.0;
        double m = ((size / 1024.0) / 1024.0);
        double g = (((size / 1024.0) / 1024.0) / 1024.0);
        double t = ((((size / 1024.0) / 1024.0) / 1024.0) / 1024.0);

        DecimalFormat dec = new DecimalFormat("0.00");

        if (t > 1) {
            hrSize = dec.format(t).concat(" TB");
        } else if (g > 1) {
            hrSize = dec.format(g).concat(" GB");
        } else if (m > 1) {
            hrSize = dec.format(m).concat(" MB");
        } else if (k > 1) {
            hrSize = dec.format(k).concat(" KB");
        } else {
            hrSize = dec.format(b).concat(" Bytes");
        }

        return hrSize;
    }


    public static boolean containsASubstring(String myString, List<String> subStrings) {
        for (String subs : subStrings) {
            if (myString.contains(subs)) {
                return true;
            }
        }
        return false; // Never found match.
    }

    /**
     * Pars url and get its query parameters
     *
     * @param url url to parse
     * @return
     * @throws UnsupportedEncodingException
     */
    public static Map<String, List<String>> splitQuery(URL url) throws UnsupportedEncodingException {
        final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
        final String[] pairs = url.getQuery().split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
            if (!query_pairs.containsKey(key)) {
                query_pairs.put(key, new LinkedList<String>());
            }
            final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
            query_pairs.get(key).add(value);
        }
        return query_pairs;
    }


    /**
     * log a long string to the console as the console doesn't support strings longer than 4kb and prevent it from truncating it automatically
     *
     * @param LOGGER  log4j Logger
     * @param longStr the long string
     */
    public static void logLongString(Logger LOGGER, String longStr) {
        if (longStr.length() > 4000) {
            int chunkCount = longStr.length() / 4000;     // integer division
            for (int i = 0; i <= chunkCount; i++) {
                int max = 4000 * (i + 1);
                if (max >= longStr.length()) {
                    LOGGER.debug(longStr.substring(4000 * i));
                } else {
                    LOGGER.debug(longStr.substring(4000 * i, max));
                }
            }
        } else {
            LOGGER.debug(longStr);
        }
    }


    public static HashMap<String, String> loadJsonLanguagesFileAsMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        InputStream in_s = null;
        try {
            in_s = Utils.class.getClassLoader().getResourceAsStream("supported_languages.json");

            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            String stringContent = new String(b, "UTF-8");

            JSONArray jsonArray = (JSONArray) JSONValue.parse(stringContent);
            for (int i = 0; i < jsonArray.size(); i++) {
                Object e = jsonArray.get(i);
                map.put((String) ((JSONObject) e).get("code"), (String) ((JSONObject) e).get("language"));
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally {
            IOUtils.closeQuietly(in_s);
        }
    }

    public static HashMap<String, String> loadJsonCountriesFileAsMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        InputStream stream = null;
        try {
            //InputStream in_s = Utils.class.getClassLoader().getResourceAsStream("supported_countries.json");
            stream = Utils.class.getClassLoader().getResourceAsStream("supported_countries.json");
            /*byte[] b = new byte[in_s.available()];
            in_s.read(b);
            String stringContent = new String(b,"UTF-8");
            in_s.close();*/

            JSONArray jsonArray = (JSONArray) JSONValue.parse(convertStreamToString(stream));
            for (int i = 0; i < jsonArray.size(); i++) {
                Object e = jsonArray.get(i);
                map.put((String) ((JSONObject) e).get("code"), (String) ((JSONObject) e).get("name"));
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public static String convertStreamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static byte[] readBytesFromStream(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytes = new byte[1024];

        int var3;
        do {
            var3 = in.read(bytes);
            if (var3 == -1) {
                break;
            }

            baos.write(bytes, 0, var3);
        } while (var3 >= 1024);

        return baos.toByteArray();
    }


    public static String readFromAsset(String filename) {
        InputStream stream = null;
        try {
            stream = Utils.class.getClassLoader().getResourceAsStream("www/" + filename);
            return convertStreamToString(stream);

        } finally {
            IOUtils.closeQuietly(stream);
        }

        /*try {
            URL url = Utils.class.getResource("/www/"+ filename);
            Path path = Paths.get(url.toURI());
            return new String(Files.readAllBytes(path), Charset.defaultCharset());
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            return null;
        }*/
    }

    // check ip is reachable and port open
    public static boolean isPortOpen(final String ip, final int port, final int timeout) throws IOException {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            return true;
        } catch (Exception ex) {
            return false;
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    /* Converts a byte array to hex string */
    public static String toHexString(byte[] bytes) {
        StringBuilder buf = new StringBuilder();
        int len = bytes.length;
        /* Converts a byte to hex digit and writes to the supplied buffer */
        char[] hexChars = "0123456789ABCDEF".toCharArray();
        for (int i = 0; i < len; i++) {
            int high = ((bytes[i] & 0xf0) >> 4);
            int low = (bytes[i] & 0x0f);
            buf.append(hexChars[high]);
            buf.append(hexChars[low]);

            if (i < len - 1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }

    public static OS getOsType() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return OS.WIN;
        }
        if (osName.contains("mac")) {
            return OS.MAC;
        }

        return OS._NIX;
    }

    /*
    * Thread-safe recursive file delete inside a directory
     */
    public static AtomicReference<Map<String, Object>> rmdirThreadSafe(File dir) {
        Map<String, Object> removeStats = new HashMap<>();
        removeStats.put("percent", 0);
        removeStats.put("notRemovedFiles", new ArrayList<String>());

        final AtomicReference<Map<String, Object>> removeStatsAtomicRef = new AtomicReference<>();
        removeStatsAtomicRef.set(removeStats);

        final AtomicReference<Integer> nbRemoved = new AtomicReference<>();
        nbRemoved.set(0);

        Thread rmdirs = new Thread(() -> {
            try {
                int totalFiles = Files.walk(Paths.get(dir.getPath())).collect(Collectors.toList()).size();
                Files.walk(Paths.get(dir.getPath()), FileVisitOption.FOLLOW_LINKS)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(file -> {
                            try {
                                if (file.exists()) {
                                    if (file.delete()) {
                                        nbRemoved.set(nbRemoved.get() + 1);

                                        int percent = (int) ((nbRemoved.get() * 100.0f) / totalFiles);

                                        removeStats.put("percent", percent);
                                        removeStatsAtomicRef.set(removeStats);
                                    } else {
                                        ((ArrayList) removeStats.get("notRemovedFiles")).add(file.getPath());
                                        removeStatsAtomicRef.set(removeStats);
                                        //System.out.println("\rcan't remove: " + file.getPath());
                                        file.deleteOnExit();
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println(e.getMessage());
                            }
                        });
                removeStats.put("percent", -1);
                removeStatsAtomicRef.set(removeStats);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        rmdirs.setPriority(Thread.MAX_PRIORITY);
        rmdirs.start();
        return removeStatsAtomicRef;
    }


    /*
    * Thread-safe recursive file delete files and directories inside a root
    * directory, except files and dir having their absolute paths in the except list
     */
    public static AtomicReference<Map<String, Object>> rmdirExceptThreadSafe(File dir, List<String> exceptFilePaths) {
        if (exceptFilePaths == null || exceptFilePaths.size() == 0)
            return rmdirThreadSafe(dir);

        Map<String, Object> removeStats = new HashMap<>();
        removeStats.put("percent", 0);
        removeStats.put("notRemovedFiles", new ArrayList<String>());

        final AtomicReference<Map<String, Object>> removeStatsAtomicRef = new AtomicReference<>();
        removeStatsAtomicRef.set(removeStats);

        final AtomicReference<Integer> nbRemoved = new AtomicReference<>();
        nbRemoved.set(0);

        Thread rmdirs = new Thread(() -> {
            try {
                int totalFiles = Files.walk(Paths.get(dir.getPath())).collect(Collectors.toList()).size();
                Files.walk(Paths.get(dir.getPath()), FileVisitOption.FOLLOW_LINKS)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(file -> {
                            try {
                                if (file.exists() && !exceptFilePaths.contains(file.getPath())) {
                                    if (file.delete()) {
                                        nbRemoved.set(nbRemoved.get() + 1);

                                        int percent = (int) ((nbRemoved.get() * 100.0f) / totalFiles);

                                        removeStats.put("percent", percent);
                                        removeStatsAtomicRef.set(removeStats);
                                    } else {
                                        ((ArrayList) removeStats.get("notRemovedFiles")).add(file.getPath());
                                        removeStatsAtomicRef.set(removeStats);
                                        //System.out.println("\rcan't remove: " + file.getPath());
                                        file.deleteOnExit();
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println(e.getMessage());
                            }
                        });
                removeStats.put("percent", -1);
                removeStatsAtomicRef.set(removeStats);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        rmdirs.setPriority(Thread.MAX_PRIORITY);
        rmdirs.start();
        return removeStatsAtomicRef;
    }


    /*
     * Recursive delete files inside a folder, no thread
     */
    public static void rmdirExcept(File dir, List<String> exceptFilePaths) {
        Map<String, Object> removeStats = new HashMap<>();
        removeStats.put("percent", 0);
        removeStats.put("notRemovedFiles", new ArrayList<String>());

        final AtomicReference<Map<String, Object>> removeStatsAtomicRef = new AtomicReference<>();
        removeStatsAtomicRef.set(removeStats);

        final AtomicReference<Integer> nbRemoved = new AtomicReference<>();
        nbRemoved.set(0);
        try {
            int totalFiles = Files.walk(Paths.get(dir.getPath())).collect(Collectors.toList()).size();
            Files.walk(Paths.get(dir.getPath()), FileVisitOption.FOLLOW_LINKS)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(file -> {
                        try {
                            if (file.exists() && !exceptFilePaths.contains(file.getPath())) {
                                if (file.delete()) {
                                    nbRemoved.set(nbRemoved.get() + 1);

                                    int percent = (int) ((nbRemoved.get() * 100.0f) / totalFiles);

                                    removeStats.put("percent", percent);
                                    removeStatsAtomicRef.set(removeStats);

                                    System.out.print("\rremoved..  " + percent + "%");

                                } else {
                                    ((ArrayList) removeStats.get("notRemovedFiles")).add(file.getPath());
                                    removeStatsAtomicRef.set(removeStats);
                                    //System.out.println("\rcan't remove: " + file.getPath());
                                    file.deleteOnExit();
                                }
                            }
                        } catch (Exception e) {
                            System.err.println(e.getMessage());
                        }
                    });
            removeStats.put("percent", -1);
            removeStatsAtomicRef.set(removeStats);

            System.out.print("\rremoving files finished");
            for (String path : (ArrayList<String>) (removeStats.get("notRemovedFiles"))) {
                System.out.println("\rcan't remove: " + path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Returns an <code>InetAddress</code> object encapsulating what is most likely the machine's LAN IP address.
     * <p/>
     * This method is intended for use as a replacement of JDK method <code>InetAddress.getLocalHost</code>, because
     * that method is ambiguous on Linux systems. Linux systems enumerate the loopback network interface the same
     * way as regular LAN network interfaces, but the JDK <code>InetAddress.getLocalHost</code> method does not
     * specify the algorithm used to select the address returned under such circumstances, and will often return the
     * loopback address, which is not valid for network communication. Details
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037">here</a>.
     * <p/>
     * This method will scan all IP addresses on all network interfaces on the host machine to determine the IP address
     * most likely to be the machine's LAN address. If the machine has multiple IP addresses, this method will prefer
     * a site-local IP address (e.g. 192.168.x.x or 10.10.x.x, usually IPv4) if the machine has one (and will return the
     * first site-local address if the machine has more than one), but if the machine does not hold a site-local
     * address, this method will return simply the first non-loopback address found (IPv4 or IPv6).
     * <p/>
     * If this method cannot find a non-loopback address using this selection algorithm, it will fall back to
     * calling and returning the result of JDK method <code>InetAddress.getLocalHost</code>.
     * <p/>
     *
     * @throws UnknownHostException If the LAN address of the machine cannot be found.
     */
    public static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // Iterate all IP addresses assigned to each card...
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {

                        if (inetAddr.isSiteLocalAddress()) {
                            // Found non-loopback site-local address. Return it immediately...
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // Found non-loopback address, but not necessarily site-local.
                            // Store it as a candidate to be returned if site-local address is not subsequently found...
                            candidateAddress = inetAddr;
                            // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
                            // only the first. For subsequent iterations, candidate will be non-null.
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                // We did not find a site-local address, but we found some other non-loopback address.
                // Server might have a non-site-local address assigned to its NIC (or it might be running
                // IPv6 which deprecates the "site-local" concept).
                // Return this non-loopback candidate address...
                return candidateAddress;
            }
            // At this point, we did not find a non-loopback address.
            // Fall back to returning whatever InetAddress.getLocalHost() returns...
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }

    public static String encdes(String text, String seretkey) throws Exception {
        DESKeySpec keySpec = new DESKeySpec(seretkey.getBytes("UTF8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey key = keyFactory.generateSecret(keySpec);
        byte[] textBytes = text.getBytes("UTF8");
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        //Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        //Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encBytes = Base64.getEncoder().encode(cipher.doFinal(textBytes));
        return new String(encBytes, "UTF-8");
    }

    public static String decdes(String encryptedText, String seretkey) throws Exception {
        DESKeySpec keySpec = new DESKeySpec(seretkey.getBytes("UTF8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey key = keyFactory.generateSecret(keySpec);
        byte[] encrypedBytes = Base64.getDecoder().decode(encryptedText);
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        //Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        //Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plainBytes = cipher.doFinal(encrypedBytes);
        return new String(plainBytes, "UTF-8");
    }

    /**
     * Find duplicates inside a collection
     *
     * @param listContainingDuplicates
     * @return
     */
    public static Set<String> findDuplicates(Collection<String> listContainingDuplicates) {
        final Set<String> setContainingDuplicates = new HashSet<>();
        final Set<String> tmpSet = new HashSet<>();

        for (String str : listContainingDuplicates) {
            if (!tmpSet.add(str)) {
                setContainingDuplicates.add(str);
            }
        }
        return setContainingDuplicates;
    }

    /**
     * Load properties from configuration files
     *
     * @return configuration parameters values
     */
    public static Map<String, String> readConfigurationFile() {
        Properties prop = new Properties();
        Map<String, String> propertiesMap = new HashMap<>();
        InputStream input = null;
        try {
            //input = new FileInputStream("/conf/config.properties");
            // load from resources folder
            input = Utils.class.getClassLoader().getResourceAsStream("conf/config.properties");
            // load a properties file
            prop.load(input);

            for (String key : prop.stringPropertyNames()) {
                String value = prop.getProperty(key);
                propertiesMap.put(key, value);
            }
            return propertiesMap;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String stringToHex(String text) throws UnsupportedEncodingException {
        byte[] myBytes = text.getBytes("UTF-8");
        return DatatypeConverter.printHexBinary(myBytes);
    }

    public static String hexToString(String hexString) throws UnsupportedEncodingException {
        byte[] bytes = DatatypeConverter.parseHexBinary(hexString);
        return new String(bytes, "UTF-8");
    }

    /**
     * Generate a random integer between max and min (min and max included)
     *
     * @param min minimum value
     * @param max maximum value
     * @return a random int between min and max included
     */
    public static int generateRandomIntBetween(int min, int max) {
        return (int) (Math.random() * (max - min + 1)) + min;
    }


    public static String escapeXmlString(String xmlString) {
        StringBuilder out = new StringBuilder(Math.max(16, xmlString.length()));
        for (int i = 0; i < xmlString.length(); i++) {
            char c = xmlString.charAt(i);
            if (c == '"') {
                out.append("\\u0022");
            } else if (c == '\'') {
                out.append("\\u0027");
            } else if (c == '<') {
                out.append("\\u003c");
            } else if (c == '>') {
                out.append("\\u003e");
            } else if (c == '&') {
                out.append("\\u0026");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Get time from time server
     */
    public static Date getNtpTime(){
        return getNtpTime(4000);
    }

    /**
     * Get time from time server with response timeout,
     * then save response to Preferences
     */
    public static Date getNtpTime(int timeout){
        try {
            // List of ntp time servers  http://www.pool.ntp.org/zone/europe
            String TIME_SERVER = "pool.ntp.org"; //  best response time

            // List of nist time servers https://tf.nist.gov/tf-cgi/servers.cgi
            //String TIME_SERVER = "time-a.nist.gov";
            //String TIME_SERVER = "time.nist.gov";

            NTPUDPClient timeClient = new NTPUDPClient();
            timeClient.open();
            timeClient.setSoTimeout(timeout);
            InetAddress inetAddress = InetAddress.getByName(TIME_SERVER);
            TimeInfo timeInfo = timeClient.getTime(inetAddress);
            long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();
            Date ntpDate = new Date(returnTime);
            PreferencesManager.getInstance().setLastNtpTime(ntpDate);
            //System.out.println("NTP Time : " + new Date(returnTime));
            return new Date(returnTime);
        } catch (Exception ex){
            // could not connect to the internet
            return null;
        }
    }


    public static String unescapeXmlString(String xmlString) {
        xmlString = xmlString.replace("\\u0022", "\"");
        xmlString = xmlString.replace("\\u0027", "\'");
        xmlString = xmlString.replace("\\u003c", "<");
        xmlString = xmlString.replace("\\u003e", ">");
        xmlString = xmlString.replace("\\u0026", "&");
        return xmlString;
    }
}
