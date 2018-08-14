package com.ninjaflip.androidrevenge.core.security;

import javax.crypto.*;
import javax.crypto.spec.DESedeKeySpec;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Created by Solitario on 29/11/2017.
 *
 * Encrypt and Decrypt using Triple DES
 */
public class TripleDesEncryption {
    public static final String TRIPLE_DES_KEY_SPEC = "DESede";
    public static final String TRIPLE_DES = "DESede/ECB/PKCS5Padding";

    public static String decrypt(String source, String secretKey) {
        try {
            Key key = getKey(secretKey);
            Cipher desCipher = Cipher.getInstance(TRIPLE_DES);
            byte[] dec = Base64.getDecoder().decode(source.getBytes());
            desCipher.init(Cipher.DECRYPT_MODE, key);
            byte[] cleartext = desCipher.doFinal(dec);
            // Return the clear text
            return new String(cleartext);
        } catch (Throwable t) {
            throw new RuntimeException("Error decrypting string", t);
        }
    }

    public static String encrypt(String plainText, String secretKey) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        String encryptedString = null;
        try {
            Key key = getKey(secretKey);
            Cipher desCipher = Cipher.getInstance(TRIPLE_DES);
            desCipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] cleartext = plainText.getBytes();
            byte[] ciphertext = desCipher.doFinal(cleartext);
            encryptedString = new String(Base64.getEncoder().encode(ciphertext));
        } catch (Throwable t) {
            throw t;
        }
        return encryptedString;
    }

    public static Key getKey(String secretKey) {
        try {
            byte[] bytes = secretKey.getBytes();
            DESedeKeySpec pass = new DESedeKeySpec(bytes);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(TRIPLE_DES_KEY_SPEC);
            SecretKey s = skf.generateSecret(pass);
            return s;
        } catch (Throwable t) {
            throw new RuntimeException("Error creating key", t);
        }
    }
}
