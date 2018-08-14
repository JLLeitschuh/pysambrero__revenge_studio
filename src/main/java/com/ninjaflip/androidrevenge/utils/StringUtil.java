package com.ninjaflip.androidrevenge.utils;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Solitario on 26/05/2017.
 * <p>
 * Contains String utility methods
 */
public class StringUtil {

    private static final String ALPHA_NUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    private static SecureRandom rand = new SecureRandom();


    /**
     * Acts like commad line interpreter, Create args array of string from a command line string
     * by splitting strings on spaces in, except those between quotes (path fr example)
     *
     * @param commandLine entry command line
     * @return String array
     */
    public static String[] getArgsFromCommandLine(String commandLine) {
        List<String> list = new ArrayList<String>();
        //String regex = "(\"[^\"]*\"|[^\"]+)(\\s+|$)";
        String regex = "([^\"]\\S*|\".+?\")\\s*";
        Matcher m = Pattern.compile(regex).matcher(commandLine);

        while (m.find())
            list.add(m.group(1).trim()); // Add .replace("\"", "") to remove surrounding quotes.

        String[] args = new String[list.size()];
        args = list.toArray(args);
        /*for (String s : args) {
            System.out.println(s);
        }*/
        return args;
    }


    // Verify download URL.
    public static URL verifyUrl(String url) {
        // Only allow HTTP URLs.
        boolean startWithHttp = url.toLowerCase().startsWith("http://");
        boolean startWithHttps = url.toLowerCase().startsWith("https://");
        if (!startWithHttp && !startWithHttps) {
            return null;
        }
        // Verify format of URL.
        URL verifiedUrl = null;
        try {
            verifiedUrl = new URL(url);
        } catch (Exception e) {
            return null;
        }
        // Make sure URL specifies a file.
        if (verifiedUrl.getFile().length() < 2)
            return null;
        return verifiedUrl;
    }

    /* Converts a byte array to hex string */
    public static String toHexString(byte[] bytes){
        StringBuilder buf = new StringBuilder();
        int len = bytes.length;
        /* Converts a byte to hex digit and writes to the supplied buffer */
        char[] hexChars = "0123456789ABCDEF".toCharArray();
        for (int i = 0; i < len; i++) {
            int high = ((bytes[i] & 0xf0) >> 4);
            int low = (bytes[i] & 0x0f);
            buf.append(hexChars[high]);
            buf.append(hexChars[low]);

            if (i < len-1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }

    // get parameters from a query string
    public static Map<String, String> splitQuery(String queryString) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    // generate a random alphanumeric with a specific length
    public static String randomAlphanumericString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(ALPHA_NUMERIC.charAt(rand.nextInt(ALPHA_NUMERIC.length())));
        return sb.toString();
    }

    // generate a random string with a specific length for a valid subpackage name
    public static String randomStringForSubPackageName(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(ALPHABET.charAt(rand.nextInt(ALPHABET.length())));
        return sb.toString();
    }


    // build a package name containing random string and having the same number of subpackages as the original package name
    // useful when building a test apk ==> benchmark
    // test prefix => if originalPackageName starts with a common package name (com, net, org,...), then
    // generated random package name must have same prefix
    public static String buildRandomPackageName(String originalPackageName){
        String[] originalPackageNameSplit = originalPackageName.split("\\.");
        int nbSubPackages = originalPackageNameSplit.length;
        String[] newPackageNameSplit = new String[nbSubPackages];

        List<String> prefix = new ArrayList<>();
        prefix.add("com");
        prefix.add("org");
        prefix.add("gov");
        prefix.add("edu");
        prefix.add("net");
        prefix.add("mil");
        int max = 8;
        int min = 4;
        if (prefix.contains(originalPackageNameSplit[0].toLowerCase())) {
            newPackageNameSplit[0] = originalPackageNameSplit[0];
            for(int i=1; i<newPackageNameSplit.length; i++){
                int len = (int) (Math.random()*(max-min))+min;
                newPackageNameSplit[i] = randomStringForSubPackageName(len);
            }
            return String.join(".", Arrays.asList(newPackageNameSplit));
        }else{
            for(int i=0; i<newPackageNameSplit.length; i++){
                int len = (int) (Math.random()*(max-min))+min;
                newPackageNameSplit[i] = randomStringForSubPackageName(len);
            }
            return String.join(".", Arrays.asList(newPackageNameSplit));
        }
    }

    /**
     * Convert plain text to HTML text in Java
     * @param str plain text
     * @return html text
     */
    public static String escape(String str) {
        StringBuilder builder = new StringBuilder();
        boolean previousWasASpace = false;
        for( char c : str.toCharArray() ) {
            if( c == ' ' ) {
                if( previousWasASpace ) {
                    builder.append("&nbsp;");
                    previousWasASpace = false;
                    continue;
                }
                previousWasASpace = true;
            } else {
                previousWasASpace = false;
            }
            switch(c) {
                case '<': builder.append("&lt;"); break;
                case '>': builder.append("&gt;"); break;
                case '&': builder.append("&amp;"); break;
                case '"': builder.append("&quot;"); break;
                case '\n': builder.append("<br>"); break;
                // We need Tab support here, because we print StackTraces as HTML
                case '\t': builder.append("&nbsp;&nbsp;&nbsp;&nbsp;"); break;
                default:
                    if( c < 128 ) {
                        builder.append(c);
                    } else {
                        builder.append("&#").append((int)c).append(";");
                    }
            }
        }
        return builder.toString();
    }
}
