package com.marmot.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class IoUtil {


    private static final Logger logger = Logger.getLogger(IoUtil.class);

    public static interface FileLoading {

        /**
         * 处理每一行记录
         * <p>
         * 返回true继续循环<br>
         * 返回false跳出循环<br>
         * 
         * @param line
         * @param n
         * @return
         */
        boolean row(String line, int n);

    }

    /**
     * 行级处理文件
     * <p>
     * 返回处理的行数
     * 
     * @param fin
     * @param charsetName
     * @param loading
     * @return
     */
    public static int load(InputStream fin, String charsetName, FileLoading loading) {
        int n = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new BufferedInputStream(fin), charsetName))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                n++;
                if (!loading.row(line, n)) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return n;
    }

    public static int load(File file, String charsetName, FileLoading loading) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(file.getName() + " not found.", e);
        }
        return load(is, charsetName, loading);
    }

    public static byte[] inputStreamToByte(InputStream in) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int count;
        while ((count = in.read(data, 0, 4096)) != -1) {
            outStream.write(data, 0, count);
        }
        return outStream.toByteArray();
    }


}
