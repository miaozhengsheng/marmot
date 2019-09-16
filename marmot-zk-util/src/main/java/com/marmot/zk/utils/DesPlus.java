package com.marmot.zk.utils;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

public class DesPlus {


    private static String strDefaultKey = "chenxmlietou0615";

    private final Map<String, Cipher> encryptMap = new HashMap<String, Cipher>();

    private final Map<String, Cipher> decryptMap = new HashMap<String, Cipher>();

    private static final ThreadLocal<DesPlus> localDesPlus = new ThreadLocal<DesPlus>() {

        @Override
        protected DesPlus initialValue() {
            return new DesPlus();
        }

    };

    private DesPlus() {
    }

    public static DesPlus getInstance() {
        return localDesPlus.get();
    }

    public String encrypt(String str) {
        return encrypt(str, strDefaultKey);
    }

    public String encrypt(String str, String key) {
        if (str == null) {
            return null;
        }
        if (key == null || key.length() < 8) {
            return null;
        }

        Cipher encryptCipher = encryptMap.get(key);

        if (encryptCipher == null) {
            try {
                Key k = getKey(key);
                encryptCipher = Cipher.getInstance("DES");
                encryptCipher.init(Cipher.ENCRYPT_MODE, k);

                encryptMap.put(key, encryptCipher);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        try {
            byte[] bs = str.getBytes("utf-8");
            bs = encryptCipher.doFinal(bs);

            return StringUtil.byteArr2HexStr(bs);
        } catch (Exception e) {
            return null;
        }
    }

    public String decrypt(String str) {
        return decrypt(str, strDefaultKey);
    }

    public String decrypt(String str, String key) {
        if (str == null) {
            return null;
        }
        if (key == null || key.length() < 8) {
            return null;
        }

        Cipher decryptCipher = decryptMap.get(key);

        if (decryptCipher == null) {
            try {
                Key k = getKey(key);
                decryptCipher = Cipher.getInstance("DES");
                decryptCipher.init(Cipher.DECRYPT_MODE, k);

                decryptMap.put(key, decryptCipher);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }


        try {
            byte[] bs = StringUtil.hexStr2ByteArr(str);
            bs = decryptCipher.doFinal(bs);

            return new String(bs, "utf-8");
        } catch (Exception e) {
            return str;
        }
    }

    private Key getKey(String key) throws Exception {
        byte[] arrBTmp = key.getBytes();
        // 创建一个空的8位字节数组（默认值为0）
        byte[] arrB = new byte[8];

        // 将原始字节数组转换为8位
        for (int i = 0; i < arrBTmp.length && i < arrB.length; i++) {
            arrB[i] = arrBTmp[i];
        }

        // 生成密钥
        Key k = new javax.crypto.spec.SecretKeySpec(arrB, "DES");

        return k;
    }


}
