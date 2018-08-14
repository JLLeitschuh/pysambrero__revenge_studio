package com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Locale;

/**
 * Main method for parser apk
 *
 */
public class Main {
    public static void main(String[] args) throws IOException, CertificateException {
        String apkFile = args[0];
        String xml = ApkParsers.getManifestXml(apkFile, Locale.getDefault());
        System.out.println(xml);
    }
}
