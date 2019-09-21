package com.marmot.common.other;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Base64;

public class GzipUtil {


    private final static int GZIP_MAGIC = 0x8b1f;
    private final static byte[] GZIP_HEADER = new byte[] { (byte) GZIP_MAGIC, // Magic
                                                                              // number
                                                                              // (short)
            (byte) (GZIP_MAGIC >> 8), // Magic number (short)
            Deflater.DEFLATED, // Compression method (CM)
            0, // Flags (FLG)
            0, // Modification time MTIME (int)
            0, // Modification time MTIME (int)
            0, // Modification time MTIME (int)
            0, // Modification time MTIME (int)
            0, // Extra flags (XFLG)
            0 // Operating system (OS)
    };

    /**
     * GZIP 压缩字符串（utf-8）,并且BASE64编码
     * 
     * @param String str
     * @return String
     * @throws IOException
     */
    public static String compressString(String str) throws IOException {
        return compressString(str, "UTF-8");
    }

    /**
     * GZIP 压缩字符串,并且BASE64编码
     * 
     * @param String str
     * @param String charsetName
     * @return String
     * @throws IOException
     */
    public static String compressString(String str, String charsetName) throws IOException {
        if (str == null || str.trim().length() == 0) {
            return str;
        }
        return Base64.encodeBase64String(compressString2byte(str, charsetName));
    }

    /**
     * GZIP 压缩字符串（utf-8）
     * 
     * @param String str
     * @param String charsetName
     * @return byte[]
     * @throws IOException
     */
    public static byte[] compressString2byte(String str) throws IOException {
        return compressString2byte(str, "UTF-8");
    }

    /**
     * GZIP 压缩字符串
     * 
     * @param String str
     * @param String charsetName
     * @return byte[]
     * @throws IOException
     */
    public static byte[] compressString2byte(String str, String charsetName) throws IOException {
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str.getBytes(charsetName));
        gzip.close();
        return out.toByteArray();
    }

    /**
     * GZIP 压缩字节流（utf-8）
     * 
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static byte[] compressInputStream(InputStream inputStream) throws IOException {
        return compressInputStream(inputStream, "UTF-8");
    }

    /**
     * GZIP 压缩字节流
     * 
     * @param inputStream
     * @param charsetName
     * @return
     * @throws IOException
     */
    public static byte[] compressInputStream(InputStream inputStream, String charsetName) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        byte[] buffer = new byte[1024];
        int offset = -1;
        while ((offset = inputStream.read(buffer)) != -1) {
            gzip.write(buffer, 0, offset);
        }
        gzip.close();
        return out.toByteArray();
    }

    /**
     * GZIP 压缩字符串（utf-8）,并且BASE64编码
     * 
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static String compressInputStream2String(InputStream inputStream) throws IOException {
        return compressInputStream2String(inputStream, "UTF-8");
    }

    /**
     * GZIP 压缩字符串, 并且BASE64编码
     * 
     * @param inputStream
     * @param charsetName
     * @return
     * @throws IOException
     */
    public static String compressInputStream2String(InputStream inputStream, String charsetName) throws IOException {
        return Base64.encodeBase64String(compressInputStream(inputStream, charsetName));
    }

    /**
     * BASE64解码,并且GZIP解压缩字符串（utf-8）
     * 
     * @param String str
     * @return String
     * @throws IOException
     */
    public static String uncompressString(String str) throws IOException {
        return uncompressString(str, "UTF-8");
    }

    /**
     * BASE64解码,并且GZIP解压缩字符串
     * 
     * @param String str
     * @param String charsetName
     * @return String
     * @throws IOException
     */
    public static String uncompressString(String str, String charsetName) throws IOException {
        if (str == null || str.trim().length() == 0) {
            return str;
        }
        return uncompress(Base64.decodeBase64(str), charsetName);
    }

    /**
     * GZIP解压缩（utf-8）
     * 
     * @param byte[] bytes
     * @return String
     * @throws IOException
     */
    public static String uncompress(byte[] bytes) throws IOException {
        return uncompress(bytes, "UTF-8");
    }

    /**
     * GZIP解压缩
     * 
     * @param byte[] bytes
     * @param String charsetName
     * @return String
     * @throws IOException
     */
    public static String uncompress(byte[] bytes, String charsetName) throws IOException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPInputStream gunzip = new GZIPInputStream(new ByteArrayInputStream(bytes));
        byte[] buffer = new byte[1024];
        int offset = -1;
        while ((offset = gunzip.read(buffer)) != -1) {
            out.write(buffer, 0, offset);
        }
        gunzip.close();
        return out.toString(charsetName);
    }

    /**
     * GZIP解压缩,utf-8
     * 
     * @param InputStream inputStream
     * @return String
     * @throws IOException
     */
    public static String uncompress(InputStream inputStream) throws IOException {
        return uncompress(inputStream, "UTF-8");
    }

    /**
     * GZIP解压缩
     * 
     * @param InputStream inputStream
     * @param String charsetName
     * @return String
     * @throws IOException
     */
    public static String uncompress(InputStream inputStream, String charsetName) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPInputStream gunzip = new GZIPInputStream(inputStream);
        byte[] buffer = new byte[1024];
        int offset = -1;
        while ((offset = gunzip.read(buffer)) != -1) {
            out.write(buffer, 0, offset);
        }
        gunzip.close();
        return out.toString(charsetName);
    }

    /**
     * 先BASE64解码，再GZIP解压缩
     * 
     * @param str
     * @return
     * @throws IOException
     */
    public static byte[] uncompress(String str) throws IOException {
        byte[] bytes = Base64.decodeBase64(str);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPInputStream gunzip = new GZIPInputStream(new ByteArrayInputStream(bytes));
        byte[] buffer = new byte[1024];
        int offset = -1;
        while ((offset = gunzip.read(buffer)) != -1) {
            out.write(buffer, 0, offset);
        }
        gunzip.close();
        return out.toByteArray();
    }

    /**
     * 使用zip进行压缩
     * 
     * @param str 压缩前的文本
     * @return 返回压缩后的文本
     * @throws IOException
     */
    public static String zip(String str) throws IOException {
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(out);

        zout.putNextEntry(new ZipEntry("0"));
        zout.write(str.getBytes());
        zout.closeEntry();
        zout.close();
        return Base64.encodeBase64String(out.toByteArray());
    }

    /**
     * 使用zip进行解压缩
     * 
     * @param compressed 压缩后的文本
     * @return 解压后的字符串
     * @throws IOException
     */
    public static String unzip(String str) throws IOException {
        if (str == null || str.trim().length() == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(Base64.decodeBase64(str)));
        zin.getNextEntry();

        byte[] buffer = new byte[1024];
        int offset = -1;
        while ((offset = zin.read(buffer)) != -1) {
            out.write(buffer, 0, offset);
        }
        zin.close();
        return out.toString();
    }

    public static byte[] compress(byte[] input) throws IOException {
        if (input == null) {
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gz = null;
        try {
            gz = new GZIPOutputStream(bos);
            gz.write(input);
        } finally {
            if (gz != null) {
                try {
                    gz.close();
                } catch (Exception e) {
                }
            }
        }
        return bos.toByteArray();
    }

    public static byte[] decompress(byte[] input) throws IOException {
        if (input == null) {
            return null;
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(input);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPInputStream gis = null;
        try {
            gis = new GZIPInputStream(bis);

            byte[] buf = new byte[1024];
            int r = -1;
            while ((r = gis.read(buf)) > 0) {
                bos.write(buf, 0, r);
            }
        } finally {
            if (gis != null) {
                try {
                    gis.close();
                } catch (Exception e) {
                }
            }
        }
        return bos.toByteArray();
    }

    /**
     * 判断byte数组内容是否被Gzip压缩过
     * 
     * @param content 节点内容
     * @return true/false
     */
    public static boolean isGzip(byte[] content) {
        if (content == null || content.length <= GZIP_HEADER.length) {
            return false;
        }
        for (int i = 0; i < GZIP_HEADER.length; i++) {
            if (content[i] != GZIP_HEADER[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 对象压缩转byte数组
     * 
     * @param _object
     * @return
     */
    public static byte[] writeCompressObject(Object _object) throws IOException {
        if (_object == null) {
            return null;
        }
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        GZIPOutputStream gzout = null;
        ObjectOutputStream out = null;
        try {
            gzout = new GZIPOutputStream(o);
            out = new ObjectOutputStream(gzout);
            out.writeObject(_object);
        } finally {
            if (gzout != null) {
                try {
                    gzout.close();
                } catch (Exception e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
        }
        return o.toByteArray();
    }

    public static Object readCompressObject(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream i = new ByteArrayInputStream(data);
        GZIPInputStream gzin = null;
        ObjectInputStream in = null;
        try {
            gzin = new GZIPInputStream(i);
            in = new ObjectInputStream(gzin);
            return in.readObject();
        } finally {
            if (gzin != null) {
                try {
                    gzin.close();
                } catch (Exception e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
    }


}
