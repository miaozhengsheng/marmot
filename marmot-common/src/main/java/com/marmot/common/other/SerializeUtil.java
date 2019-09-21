package com.marmot.common.other;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;

public class SerializeUtil {


    /**
     * if true, remove all zero bytes from the MSB of the packed num
     */
    private static final boolean packZeros = true;

    public static byte[] encodeNum(long l, int maxBytes) {
        byte[] rv = new byte[maxBytes];
        for (int i = 0; i < rv.length; i++) {
            int pos = rv.length - i - 1;
            rv[pos] = (byte) ((l >> (8 * i)) & 0xff);
        }
        if (packZeros) {
            int firstNon0 = 0;
            // Just looking for what we can reduce
            while (firstNon0 < rv.length && rv[firstNon0] == 0) {
                firstNon0++;
            }
            if (firstNon0 > 0) {
                byte[] tmp = new byte[rv.length - firstNon0];
                System.arraycopy(rv, firstNon0, tmp, 0, rv.length - firstNon0);
                rv = tmp;
            }
        }
        return rv;
    }

    public static byte[] encodeLong(long l) {
        return encodeNum(l, 8);
    }

    public static long decodeLong(byte[] b) {
        long rv = 0;
        for (byte i : b) {
            rv = (rv << 8) | (i < 0 ? 256 + i : i);
        }
        return rv;
    }

    public static byte[] encodeInt(int in) {
        return encodeNum(in, 4);
    }

    public static int decodeInt(byte[] in) {
        assert in.length <= 4 : "Too long to be an int (" + in.length + ") bytes";
        return (int) decodeLong(in);
    }

    public static byte[] encodeByte(byte in) {
        return new byte[] { in };
    }

    public static byte decodeByte(byte[] in) {
        assert in.length <= 1 : "Too long for a byte";
        byte rv = 0;
        if (in.length == 1) {
            rv = in[0];
        }
        return rv;
    }

    public static byte[] encodeBoolean(boolean b) {
        byte[] rv = new byte[1];
        rv[0] = (byte) (b ? '1' : '0');
        return rv;
    }

    public static boolean decodeBoolean(byte[] in) {
        assert in.length == 1 : "Wrong length for a boolean";
        return in[0] == '1';
    }

    public static byte[] serializeString(String str, String charset) {
        byte[] rv = null;
        try {
            rv = str.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return rv;
    }

    public static String deserializeString(byte[] data, String charset) {
        String value = null;
        try {
            if (data != null) {
                value = new String(data, charset);
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return value;
    }

    public static byte[] serialize(Object o) throws IOException {
        byte[] bytes = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream os = null;
        try {
            bos = new ByteArrayOutputStream();
            os = new ObjectOutputStream(bos);
            os.writeObject(o);
            os.close();
            bos.close();
            bytes = bos.toByteArray();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e) {
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (Exception e) {
                }
            }
        }
        return bytes;
    }

    public static Object deserialize(byte[] bytes) throws Exception {
        Object o = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream is = null;
        try {
            bis = new ByteArrayInputStream(bytes);
            is = new ObjectInputStream(bis);
            o = is.readObject();
            is.close();
            bis.close();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (Exception e) {
                }
            }
        }
        return o;
    }


}
