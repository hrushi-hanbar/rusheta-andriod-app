package com.example.rusheta.utils;

import android.util.Base64;
import android.util.Log;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoClass {

    final private static String RSAServerPublic = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA56OPXD+MhPC1+/UoVtop\n" +
            "F7nl/LLG4afNx6MxvZhtNsc2eu4EkWcQefjOx0Jfrrix+Nmn7aR5+R0BfZ0LhHHu\n" +
            "2F2PugjSu/UMQriZSBwNrFbIH25SBfm1cWzCtr9mN3HSNdeQj09vyJEigxRG01Cc\n" +
            "Lz13JUVERU1I0mJs8doNDy0q9SfpoKUhIHTl4FHZ9BChNdykRxK3aksfuMXjYZpG\n" +
            "BJLHOpGg/CItL9X0LiykjeL1CUfWKSy13XskkyYxHH1ojcUzUUcYiTNaP+bT/8Id\n" +
            "wNc0JEKfWRRnNe1yMJnbl1XRPceaIX7v0q1oy0Qg9k5d5SjnnecQkBx8pFw7/s3k\n" +
            "ZQIDAQAB\n" +
            "-----END PUBLIC KEY-----";

    private static byte[] IV;
    private static byte[] key;
    private static PublicKey publicKey;

    public CryptoClass() throws Exception {
        // Load Public Key
        String pubKeyPEM = RSAServerPublic.replace("-----BEGIN PUBLIC KEY-----\n", "");
        pubKeyPEM = pubKeyPEM.replace("-----END PUBLIC KEY-----", "");
        byte[] encoded = Base64.decode(pubKeyPEM, Base64.DEFAULT);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        publicKey = kf.generatePublic(keySpec);

        // Gen AES Key
        key = new byte[32];
        new SecureRandom().nextBytes(key);
        IV = new byte[16];
        new SecureRandom().nextBytes(IV);
        String s = new String(Base64.encode(key, Base64.DEFAULT));
        String b = new String(Base64.encode(IV, Base64.DEFAULT));
        Log.i("AES key", s);
        Log.i("IV", b);
    }

    public CryptoClass(byte[] key, byte[] IV){
        CryptoClass.key = key;
        CryptoClass.IV = IV;
    }

    public static byte[] getIV() {
        return IV;
    }

    public static byte[] getKey() {
        return key;
    }

    public static PublicKey getPublicKey() {
        return publicKey;
    }

    public static String encryptToRSAString(byte[] encrypt) {
        String encryptedBase64 = "";
        try {
            final Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
            // encrypt the plain text using the public key
            cipher.init(Cipher.ENCRYPT_MODE,publicKey);
            byte[] encryptedBytes = cipher.doFinal(encrypt);
            encryptedBase64 = new String(Base64.encode(encryptedBytes, Base64.DEFAULT));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedBase64.replaceAll("(\\r|\\n)", "");
    }


    public byte[] encrypt(byte[] plaintext){
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] cipherText = cipher.doFinal(plaintext);
            return cipherText;
        }catch (Exception e ){
            e.printStackTrace();
        }
        return null;
    }

    public  byte[] decrypt(byte[]  cipherText){
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decryptedText = cipher.doFinal(cipherText);
            return decryptedText;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
