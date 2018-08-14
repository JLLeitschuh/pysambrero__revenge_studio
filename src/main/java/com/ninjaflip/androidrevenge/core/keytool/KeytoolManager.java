package com.ninjaflip.androidrevenge.core.keytool;

import com.ninjaflip.androidrevenge.beans.KeystoreBean;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.encoders.Base64;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Created by Solitario on 02/06/2017.
 * <p>
 * this class is responsible for keystore and certificates management
 */

public class KeytoolManager {
    private static KeytoolManager INSTANCE;


    private KeytoolManager() {
    }

    public static KeytoolManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new KeytoolManager();
        }
        return INSTANCE;
    }

    /**
     * Create a new keystore containing a self-signed certificate
     *
     * @param alias              key alias
     * @param ksPassword         keystore password
     * @param keyPassword        key password
     * @param CN                 Common Name (i.e. www.example.com)
     * @param OU                 Organizational Unit (i.e. R&D)
     * @param O                  Organization (i.e. Company Ltd)
     * @param L                  Locality (i.e. Dublin city)
     * @param ST                 State Or ProvinceName (i.e. Dublin Province)
     * @param C                  country name (i.e. IE)
     * @param E                  email address (i.e. example@domain.com)
     * @param ksFileAbsolutePath the path to file contains the keystore i.e. /path/to/keystore.jks
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws SignatureException
     * @throws OperatorCreationException
     */
    public void createNewKeystore(String alias, String ksPassword, String keyPassword, String CN, String OU, String O,
                                  String L, String ST, String C, String E, int nbYears, String ksFileAbsolutePath) throws NoSuchAlgorithmException,
            KeyStoreException, IOException, CertificateException, SignatureException, OperatorCreationException {
        // validations
        validateKsParameters(ksPassword, alias, keyPassword, CN, OU, O, L, ST, C, E);
        // load keystore
        KeyPairGenerator kpgen = KeyPairGenerator.getInstance("RSA");
        kpgen.initialize(2048, new SecureRandom());
        KeyPair keyPair = kpgen.generateKeyPair();
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, ksPassword.toCharArray());
        // generate certificate
        X509Certificate myCert = generateCertificate(keyPair, CN, OU, O, L, ST, C, E, nbYears);
        // save certificate to keystore file 'filename.jks'
        saveCertificateToKeyStoreFile(myCert, ks, keyPair.getPrivate(), ksPassword, keyPassword, alias, ksFileAbsolutePath);
    }

    public byte[] createNewKeystore(String alias, String ksPassword, String keyPassword, String CN, String OU, String O,
                                  String L, String ST, String C, String E, int nbYears) throws NoSuchAlgorithmException,
            KeyStoreException, IOException, CertificateException, SignatureException, OperatorCreationException {
        // validations
        validateKsParameters(ksPassword, alias, keyPassword, CN, OU, O, L, ST, C, E);
        // load keystore
        KeyPairGenerator kpgen = KeyPairGenerator.getInstance("RSA");
        kpgen.initialize(2048, new SecureRandom());
        KeyPair keyPair = kpgen.generateKeyPair();
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, ksPassword.toCharArray());
        // generate certificate
        X509Certificate myCert = generateCertificate(keyPair, CN, OU, O, L, ST, C, E, nbYears);
        // save certificate to keystore bytes array

        X509Certificate[] certChain = new X509Certificate[]{myCert};
        ks.setKeyEntry(alias, keyPair.getPrivate(), keyPassword.toCharArray(), certChain);
        ByteArrayOutputStream  bos = new ByteArrayOutputStream();
        ks.store(bos, ksPassword.toCharArray());
        byte[] ksBytes = bos.toByteArray();
        IOUtils.closeQuietly(bos);
        return ksBytes;
    }


    /*
     * Validate keystore parameters before processing them
     */
    private void validateKsParameters(String ksPassword, String alias, String keyPassword, String CN, String OU, String O,
                                      String L, String ST, String C, String E) {
        if (alias == null || "".equals(alias)) {
            throw new IllegalArgumentException("Certificate alias must not be empty or null!");
        }
        if (ksPassword == null || "".equals(ksPassword)) {
            throw new IllegalArgumentException("Keystore password must not be empty or null!");
        }
        if (keyPassword == null || "".equals(keyPassword)) {
            throw new IllegalArgumentException("Certificate password must not be empty or null!");
        }
        if ((CN == null || "".equals(CN)) && (OU == null || "".equals(OU)) && (O == null || "".equals(O))
                && (L == null || "".equals(L)) && (ST == null || "".equals(ST))
                && (C == null || "".equals(C)) && (E == null || "".equals(E))) {
            throw new IllegalArgumentException("Certificate owner data must have at least a non empty field!");
        }
    }

    /*
    * Routine to generate an actual (though self-signed) certificate
     */
    private X509Certificate generateCertificate(KeyPair keyPair, String CN, String OU, String O,
                                                String L, String ST, String C, String E, int nbYears) throws OperatorCreationException,
            IOException, CertificateException, SignatureException, NoSuchAlgorithmException {

        Calendar calendar = Calendar.getInstance();
        Date NOT_BEFORE = calendar.getTime();
        calendar.add(Calendar.DATE, 365 * nbYears);// number of days to add
        Date NOT_AFTER = calendar.getTime();
        // signer's info
        List<String> signerList = new ArrayList<String>();
        if (CN != null && !CN.equals(""))
            signerList.add("CN=" + CN);
        if (OU != null && !OU.equals(""))
            signerList.add("OU=" + OU);
        if (O != null && !O.equals(""))
            signerList.add("O=" + O);
        if (ST != null && !ST.equals(""))
            signerList.add("ST=" + ST);
        if (C != null && !C.equals(""))
            signerList.add("C=" + C.toUpperCase());
        if (E != null && !E.equals(""))
            signerList.add("E=" + E);

        String joined = String.join(",", signerList);
        //System.out.println("Joined : " + joined);
        X500Name issuerName = new X500Name(joined);
        // subjects name - the same as we are self signed.
        X500Name subjectName = issuerName;
        // serial
        BigInteger serial = BigInteger.valueOf(new Random().nextInt());
        // public key
        PublicKey publicKey = keyPair.getPublic();
        // create the certificate - version 3
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(issuerName, serial,
                NOT_BEFORE, NOT_AFTER, subjectName, publicKey);

        JcaContentSignerBuilder builder = new JcaContentSignerBuilder("SHA256withRSA");
        ContentSigner signer = builder.build(keyPair.getPrivate());

        byte[] certBytes = certBuilder.build(signer).getEncoded();

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate x509Certificate = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certBytes));
        //System.out.println(x509Certificate.toString());
        return x509Certificate;
    }

    /*
    * Import a self-signed certificate to a keystore
     */
    private void saveCertificateToKeyStoreFile(X509Certificate myCert, KeyStore ks, Key privateKey,
                                               String ksPassword, String keyPassword, String alias, String ksFileAbsolutePath)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        X509Certificate[] certChain = new X509Certificate[]{myCert};
        ks.setKeyEntry(alias, privateKey, keyPassword.toCharArray(), certChain);
        FileOutputStream fos = new FileOutputStream(ksFileAbsolutePath);
        ks.store(fos, ksPassword.toCharArray());
        IOUtils.closeQuietly(fos);
    }


    /**
     * * Get keystore info
     *
     * @param ksBean the keystore bean
     * @return an array containing all info about every certificate in the keystore
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws KeyStoreException
     */
    public List<JSONObject> getKeystoreDetails(KeystoreBean ksBean)
            throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        List<JSONObject> result = new ArrayList<>();
        ByteArrayInputStream is = null;
        try {

            is = new ByteArrayInputStream(ksBean.getBlob());
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, ksBean.getKsPass().toCharArray());


            Enumeration enumeration = keystore.aliases();
            int i = 0;
            while (enumeration.hasMoreElements()) {
                JSONObject certInfo = new JSONObject();
                i++;
                certInfo.put("number", i);
                String alias = (String) enumeration.nextElement();
                certInfo.put("alias", alias);
                certInfo.put("dateCreated", keystore.getCreationDate(alias));

                Certificate cert = keystore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    X509Certificate x509cert = (X509Certificate) cert;

                    // Get subject
                    Principal principal = x509cert.getSubjectDN();
                    String subjectDn = principal.getName();
                    certInfo.put("owner", subjectDn);


                    // Get issuer
                    principal = x509cert.getIssuerDN();
                    String issuerDn = principal.getName();
                    certInfo.put("issuer", issuerDn);

                    // SerialNumber
                    String serialNumber = x509cert.getSerialNumber().toString();
                    certInfo.put("serialNumber", serialNumber);


                    // validity
                    Date NOT_BEFORE = x509cert.getNotBefore();
                    Date NOT_AFTER = x509cert.getNotAfter();
                    certInfo.put("validityFrom", NOT_BEFORE);
                    certInfo.put("validityTo", NOT_AFTER);

                    // Key
                    PublicKey publicKey = x509cert.getPublicKey();
                    certInfo.put("publicKey", publicKey.toString());

                    // certificate fingerprint
                    // certificate fingerprint ==> version
                    int version = x509cert.getVersion();
                    certInfo.put("version", "V" + version);

                    // certificate fingerprint ==> Signature Algorithm
                    String signAlgo = x509cert.getSigAlgName();
                    String OID = x509cert.getSigAlgOID();
                    certInfo.put("signatureAlgorithm", signAlgo + ", OID = " + OID);
                    // certificate fingerprint ==> MD5
                    byte[] encCertInfo = cert.getEncoded();
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    byte[] digest = md.digest(encCertInfo);
                    certInfo.put("fingerprintMd5", Utils.toHexString(digest));

                    // certificate fingerprint ==> SHA1
                    MessageDigest mdSHA1 = MessageDigest.getInstance("SHA-1");
                    byte[] digestSHA1 = mdSHA1.digest(cert.getEncoded());
                    certInfo.put("fingerprintSHA1", Utils.toHexString(digestSHA1));
                    // certificate fingerprint ==> SHA256
                    MessageDigest mdSHA256 = MessageDigest.getInstance("SHA-256");
                    byte[] digestSHA256 = mdSHA256.digest(cert.getEncoded());
                    certInfo.put("fingerprintSHA256", Utils.toHexString(digestSHA256));

                    // PEM
                    System.out.println("PEM :");
                    String cert_begin = "\t-----BEGIN CERTIFICATE-----\n";
                    String end_cert = "\n\t-----END CERTIFICATE-----\n";
                    byte[] derCert = x509cert.getEncoded();
                    String pemCertPre = new String(Base64.encode(derCert),"UTF-8");
                    String pemCert = cert_begin + "\t" + pemCertPre + end_cert;
                    certInfo.put("pemCert", pemCert);
                }

                result.add(certInfo);
            }

        } finally {
            if (null != is)
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return result;
    }

    /**
     * Pretty print certificate info, for debug purpose
     *
     * @param ksFileAbsolutePath absolute path to keystore file
     * @param ksPass             keystore password
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws KeyStoreException
     */
    public void printCertificateInfo(String ksFileAbsolutePath, String ksPass)
            throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        InputStream is = null;
        try {

            File file = new File(ksFileAbsolutePath);
            is = new FileInputStream(file);
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, ksPass.toCharArray());


            Enumeration enumeration = keystore.aliases();
            int i = 0;
            while (enumeration.hasMoreElements()) {
                if (i != 0)
                    System.out.println("----------------------------------------------------------");
                i++;
                System.out.println("Certificate [" + i + "]");
                String alias = (String) enumeration.nextElement();
                System.out.println("Alias : " + alias);
                System.out.println("Creation date : " + keystore.getCreationDate(alias));

                Certificate cert = keystore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    X509Certificate x509cert = (X509Certificate) cert;

                    // Get subject
                    Principal principal = x509cert.getSubjectDN();
                    String subjectDn = principal.getName();
                    System.out.println("Owner : " + subjectDn);

                    // Get issuer
                    principal = x509cert.getIssuerDN();
                    String issuerDn = principal.getName();
                    System.out.println("Issuer : " + issuerDn);

                    // SerialNumber
                    String serialNumber = x509cert.getSerialNumber().toString();
                    System.out.println("SerialNumber : " + serialNumber);

                    // validity
                    Date NOT_BEFORE = x509cert.getNotBefore();
                    Date NOT_AFTER = x509cert.getNotAfter();
                    System.out.println("Validity :");
                    System.out.println("\tForm : " + NOT_BEFORE);
                    System.out.println("\tTo   : " + NOT_AFTER);

                    // Key
                    PublicKey publicKey = x509cert.getPublicKey();
                    System.out.println("Key : " + publicKey.toString());

                    // certificate fingerprint
                    System.out.println("Certificate fingerprints:");
                    // certificate fingerprint ==> version
                    int version = x509cert.getVersion();
                    System.out.println("\tVersion : V" + version);
                    // certificate fingerprint ==> Signature Algorithm
                    String signAlgo = x509cert.getSigAlgName();
                    String OID = x509cert.getSigAlgOID();
                    System.out.println("\tSignature Algorithm : " + signAlgo + ", OID = " + OID);
                    // certificate fingerprint ==> MD5
                    byte[] encCertInfo = cert.getEncoded();
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    byte[] digest = md.digest(encCertInfo);
                    System.out.println("\tmd5 : " + Utils.toHexString(digest));
                    // certificate fingerprint ==> SHA1
                    MessageDigest mdSHA1 = MessageDigest.getInstance("SHA-1");
                    byte[] digestSHA1 = mdSHA1.digest(cert.getEncoded());
                    System.out.println("\tSHA-1 : " + Utils.toHexString(digestSHA1));
                    // certificate fingerprint ==> SHA256
                    MessageDigest mdSHA256 = MessageDigest.getInstance("SHA-256");
                    byte[] digestSHA256 = mdSHA256.digest(cert.getEncoded());
                    System.out.println("\tSHA-256 : " + Utils.toHexString(digestSHA256));

                    // PEM
                    System.out.println("PEM :");
                    String cert_begin = "\t-----BEGIN CERTIFICATE-----\n";
                    String end_cert = "\n\t-----END CERTIFICATE-----\n";
                    byte[] derCert = x509cert.getEncoded();
                    String pemCertPre = new String(Base64.encode(derCert),"UTF-8");
                    String pemCert = cert_begin + "\t" + pemCertPre + end_cert;
                    System.out.println(pemCert);
                }

                //Certificate certificate = keystore.getCertificate(alias);
                //System.out.println(certificate.toString());
            }

        } finally {
            if (null != is)
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }


    /**
     * Return all certificates info of keystore
     *
     * @param filePath         file path
     * @param keystorePassword password of the keystore
     * @return list of json objects containing certificates info
     */
    public List<JSONObject> getKeystoreCertificatesInfo(String filePath, String keystorePassword)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        List<JSONObject> info = new ArrayList<>();
        InputStream is = null;
        try {

            File file = new File(filePath);
            is = new FileInputStream(file);
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, keystorePassword.toCharArray());
            Enumeration enumeration = keystore.aliases();
            while (enumeration.hasMoreElements()) {
                JSONObject obj = new JSONObject();
                String alias = (String) enumeration.nextElement();
                obj.put("alias", alias);

                Certificate cert = keystore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    X509Certificate x509cert = (X509Certificate) cert;

                    // Get subject
                    Principal principal = x509cert.getSubjectDN();
                    String subjectDn = principal.getName();
                    obj.put("owner", subjectDn);


                    // Get issuer
                    principal = x509cert.getIssuerDN();
                    String issuerDn = principal.getName();
                    obj.put("issuer", issuerDn);

                    // validity
                    Date NOT_BEFORE = x509cert.getNotBefore();
                    Date NOT_AFTER = x509cert.getNotAfter();
                    obj.put("validityFrom", NOT_BEFORE);
                    obj.put("validityTo", NOT_AFTER);
                }

                info.add(obj);
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return info;
    }



    // checks keystore password correct and return the keystore
    public KeyStore validateKeystorePassword(String filePath, String keystorePassword) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        InputStream is = null;
        try {

            File file = new File(filePath);
            is = new FileInputStream(file);
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, keystorePassword.toCharArray());
            return keystore;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    // checks if alias password (privateKey pwd) is correct
    public void validateAliasPassword(KeyStore keystore, String alias, String aliasPw) throws UnrecoverableEntryException, KeyStoreException, NoSuchAlgorithmException {
        keystore.getEntry(alias, new KeyStore.PasswordProtection(aliasPw.toCharArray()));
    }
}
