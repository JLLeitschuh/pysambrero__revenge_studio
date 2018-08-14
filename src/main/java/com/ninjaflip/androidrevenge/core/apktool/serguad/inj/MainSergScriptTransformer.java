package com.ninjaflip.androidrevenge.core.apktool.serguad.inj;

import com.ninjaflip.androidrevenge.utils.StringUtil;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONObject;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Base64;

/**
 * Created by Solitario on 30/10/2017.
 */
public class MainSergScriptTransformer {


    /**
     * Transform smali file to an injectable template
     */
    public static void transformSmaliFilesToTemplate(String filePath, String outputFileName) throws IOException {
        File file = new File(filePath);
        File outFile = new File(file.getParent(), outputFileName);
        if(!outFile.exists()){
            outFile.createNewFile();
        }
        FileInputStream is = null;
        InputStream stream = null;
        FileOutputStream out = null;
        try {
            is = new FileInputStream(file);
            String content = IOUtils.toString(is, "UTF-8");

            String newContent = content.replace("com/katad/plug/tro", "{$serguad_pckg_name}");
            newContent = newContent.replace("Troy6", "{$serguad_class_name}");
            newContent = newContent.replace("initTroy", "{$serguad_init_method_name}");

            newContent = newContent.replace("REAL_INTERS_CLS_NAME", "{$serguad_inters_class_name}");
            newContent = newContent.replace("SET_INTERS_AD_UNIT_METHOD_NAME", "{$serguad_set_Iid}");

            newContent = newContent.replace("REAL_BNR_CLS_NAME", "{$serguad_adview_class_name}");
            newContent = newContent.replace("SET_BNR_AD_UNIT_METHOD_NAME", "{$serguad_set_Bid}");
            newContent = newContent.replace("22222222222222222222L", "{$serguad_launch_date}");

            // encrypt content
            newContent = Utils.encdes(newContent, "VUVaS0swUmxTekpDUVhsTFFqQlNZMFpGUTNjeU0yZERRblJtVlRadk5FOWpWVmM0UW5oT1EwWXZaVTR3");

            stream = new ByteArrayInputStream(newContent.getBytes("UTF-8"));
            out = new FileOutputStream(outFile);
            IOUtils.copyLarge(stream, out);

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(stream);
            IOUtils.closeQuietly(out);
        }
    }


    /**
     * Create an encrypted json String that contains a key parameter, a data parameters
     */
    public static void createAndEncryptDataJson() throws Exception {
        String longKey = StringUtil.randomAlphanumericString(128);
        // Note that the real key is the first 24 characters
        String key = longKey.substring(0, 24);

        JSONObject json = new JSONObject();
        String bnrId = new String(Base64.getEncoder().encode("ca-app-pub-3940256099942544/6300978111".getBytes()));
        String intersId = new String(Base64.getEncoder().encode("ca-app-pub-3940256099942544/1033173712".getBytes()));
        json.put("b", bnrId);
        json.put("i", intersId);
        System.out.println("key is: " + key);
        System.out.println("json is: " + json.toJSONString());
        String enc = Utils.encdes(json.toJSONString(), key);
        System.out.println("Encrypted json is: " + enc);
        String dec = Utils.decdes(enc, key);
        System.out.println("Decrypted json is: " + dec);
        System.out.println("----------------------------");

        JSONObject serverJson = new JSONObject();
        serverJson.put("a", new String(Base64.getEncoder().encode(enc.getBytes())));
        serverJson.put("b", longKey);
        serverJson.put("c", enc);
        String serverJsonStr = serverJson.toJSONString();
        String serverJsonStrAsB64 = new String(Base64.getEncoder().encode(serverJsonStr.getBytes()));
        System.out.println("serverJson: " + serverJsonStr);
        System.out.println("serverJson as Baser64: " + serverJsonStrAsB64);
        System.out.println("serverJson as Baser64 length: " + serverJsonStrAsB64.length());
    }


    public static void main(final String[] args) throws Exception {
        org.apache.log4j.BasicConfigurator.configure();
        //createAndEncryptDataJson();
        String filePath1 = "C:\\Users\\Solitario\\Desktop\\Katana_injector_tmpl\\Troy6.smali";
        String filePath2 = "C:\\Users\\Solitario\\Desktop\\Katana_injector_tmpl\\Troy6$mTsk.smali";
        String filePath3 = "C:\\Users\\Solitario\\Desktop\\Katana_injector_tmpl\\Troy6$1.smali";
        transformSmaliFilesToTemplate(filePath1, "background.png");
        transformSmaliFilesToTemplate(filePath2,"backgroundTsk.png");
        transformSmaliFilesToTemplate(filePath3,"background1.png");
    }
}