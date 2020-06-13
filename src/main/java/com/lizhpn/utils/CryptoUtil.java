package com.lizhpn.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;

/**
 * 加密工具
 */
public class CryptoUtil {
    private static String hmac(String plainText, String key, String algorithm, String format) throws Exception {
        if (plainText == null || key == null) {
            return null;
        }

        byte[] hmacResult = null;

        Mac mac = Mac.getInstance(algorithm);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), algorithm);
        mac.init(secretKeySpec);
        hmacResult = mac.doFinal(plainText.getBytes());
        return String.format(format, new BigInteger(1, hmacResult));
    }

    /**
     * MD5 加密
     * @param plainText 明文
     * @param key   密码
     * @return  密文
     * @throws Exception
     */
    public static String hmacMd5(String plainText, String key) throws Exception {
        return hmac(plainText,key,"HmacMD5","%032x");
    }

    /**
     * Sha1 加密
     * @param plainText 明文
     * @param key   密码
     * @return  密文
     * @throws Exception
     */
    public static String hmacSha1(String plainText, String key) throws Exception {
        return hmac(plainText,key,"HmacSHA1","%040x");
    }

    /**
     * Sha256 加密
     * @param plainText 明文
     * @param key   密码
     * @return  密文
     * @throws Exception
     */
    public static String hmacSha256(String plainText, String key) throws Exception {
        return hmac(plainText,key,"HmacSHA256","%064x");
    }
}
