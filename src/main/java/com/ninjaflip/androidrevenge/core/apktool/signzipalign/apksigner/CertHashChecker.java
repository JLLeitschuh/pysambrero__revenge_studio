package com.ninjaflip.androidrevenge.core.apktool.signzipalign.apksigner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Used to check if given sha hashes equals the ones found in a APKs signature
 */
public class CertHashChecker {

    public Result check(AndroidApkSignerVerify.Result verifyResult, String[] hashes) {
        if (verifyResult == null) {
            throw new IllegalArgumentException("parameters must not be null");
        }

        if (hashes == null || !verifyResult.verified) {
            return null;
        }

        if (verifyResult.certInfoList.isEmpty()) {
            throw new IllegalArgumentException("no certs info found in verify result - strange...");
        }

        if (verifyResult.certInfoList.size() != hashes.length) {
            return new Result(false, "not the same count of signatures and provided check hashes (found " + verifyResult.certInfoList.size() + " signatures)", hashes);
        }

        /*List<String> apkHashes = verifyResult.certInfoList.stream().map(certInfo -> certInfo.certSha256).distinct().sorted().collect(Collectors.toList());
        List<String> providedHashes = Arrays.stream(hashes).distinct().sorted().collect(Collectors.toList());*/

        List<String> apkHashes = new ArrayList<String>();
        for (AndroidApkSignerVerify.CertInfo certInfo : verifyResult.certInfoList) {
            if(!apkHashes.contains(certInfo.certSha256))
                apkHashes.add(certInfo.certSha256);
        }
        Collections.sort(apkHashes);

        List<String> providedHashes = new ArrayList<String>();
        for (String s : Arrays.asList(hashes)) {
            if(!providedHashes.contains(s))
                providedHashes.add(s);
        }
        Collections.sort(providedHashes);

        for (int i = 0; i < apkHashes.size(); i++) {
            if (!apkHashes.get(i).equalsIgnoreCase(providedHashes.get(i))) {
                return new Result(false, "The following hash does not match with the provided: " + apkHashes.get(i) + " <> " + providedHashes.get(i), hashes);
            }
        }
        return new Result(true, null, hashes);
    }

    public static class Result {
        public final boolean verified;
        public final String errorString;
        public final String[] sha256;

        public Result(boolean verified, String errorString, String[] sha256) {
            this.verified = verified;
            this.errorString = errorString;
            this.sha256 = sha256;
        }

        public String hashSummary() {
            StringBuilder sb = new StringBuilder();
            String sep = "";
            if (sha256 != null) {
                sb.append("(");
                for (String s : sha256) {
                    sb.append(sep);
                    sep = ",";
                    if (s.length() > 8) {
                        sb.append(s.substring(0, 8));
                    } else {
                        sb.append(s);
                    }
                }
                sb.append(")");
            }
            return sb.toString();
        }
    }
}