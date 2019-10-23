package com.marmot.cache.redis;

import java.io.Serializable;
import java.util.Date;

import org.apache.log4j.Logger;

import com.marmot.common.other.GzipUtil;
import com.marmot.common.other.SerializeUtil;

public class SerializingConverter implements Converter<Object> {

    private static final Logger logger = Logger.getLogger(SerializingConverter.class);

    /**
     * Default compression threshold value.
     */
    public static final int DEFAULT_COMPRESSION_THRESHOLD = 16384;// 16384

    private static final String DEFAULT_CHARSET = "UTF-8";

    protected int compressionThreshold = DEFAULT_COMPRESSION_THRESHOLD;

    // General flags
    static final int SERIALIZED = 1;
    static final int COMPRESSED = 2;

    // Special flags for specially handled types.
    private static final int SPECIAL_MASK = 0xff00;
    static final int SPECIAL_BOOLEAN = (1 << 8);
    static final int SPECIAL_INT = (2 << 8);
    static final int SPECIAL_LONG = (3 << 8);
    static final int SPECIAL_DATE = (4 << 8);
    static final int SPECIAL_BYTE = (5 << 8);
    static final int SPECIAL_FLOAT = (6 << 8);
    static final int SPECIAL_DOUBLE = (7 << 8);
    static final int SPECIAL_BYTEARRAY = (8 << 8);

    /*
     * user-defined header magic number.
     */
    private static final int MAGIC = 0x7e8f;

    private static final byte[] HEADER = { (byte) MAGIC, // Magic number (short)
            (byte) (MAGIC >> 8), // Magic number (short)
            8, // Compression method (GZIP)
            0, // Flags (FLG)
            0, // Flags (FLG)
            0, // Flags (FLG)
            0, // Flags (FLG)
            (byte) MAGIC // Magic number (short)
    };

    /**
     * flag填充数组起始位置
     */
    private static final int FLAG_POS = 3;

    public void setCompressionThreshold(int to) {
        compressionThreshold = to;
    }

    /**
     * 填充头信息
     * 
     * @param src
     * @param flag
     * @return
     */
    private byte[] fillHeader(byte[] src, int flag) {
        byte[] dest = new byte[src.length + HEADER.length];
        byte[] bytes = int2byte(flag);
        System.arraycopy(HEADER, 0, dest, 0, HEADER.length);
        System.arraycopy(bytes, 0, dest, FLAG_POS, bytes.length);
        System.arraycopy(src, 0, dest, HEADER.length, src.length);
        return dest;
    }

    /**
     * 校验头信息
     * 
     * @return
     */
    private boolean validate(byte[] src) {
        if (src.length < HEADER.length) {
            return false;
        }
        return (src[0] == HEADER[0]) && (src[1] == HEADER[1]) && (src[2] == HEADER[2]) && (src[7] == HEADER[7]);
    }

    /**
     * 读取flag信息
     * 
     * @param src
     * @return
     */
    private int readFlag(byte[] src) {
        byte[] tmp = new byte[4];
        for (int i = 0; i < 4; i++) {
            tmp[i] = src[FLAG_POS + i];
        }
        return byte2int(tmp);
    }

    @Override
    public byte[] serialize(Object o) throws Exception {
        if (o == null) {
            return null;
        }
        byte[] bytes = null;
        int flags = 0;
        if (o instanceof String) {
            bytes = SerializeUtil.serializeString((String) o, DEFAULT_CHARSET);
        } else if (o instanceof Long) {
            bytes = SerializeUtil.encodeLong((Long) o);
            flags |= SPECIAL_LONG;
        } else if (o instanceof Integer) {
            bytes = SerializeUtil.encodeInt((Integer) o);
            flags |= SPECIAL_INT;
        } else if (o instanceof Boolean) {
            bytes = SerializeUtil.encodeBoolean((Boolean) o);
            flags |= SPECIAL_BOOLEAN;
        } else if (o instanceof Date) {
            bytes = SerializeUtil.encodeLong(((Date) o).getTime());
            flags |= SPECIAL_DATE;
        } else if (o instanceof Byte) {
            bytes = SerializeUtil.encodeByte((Byte) o);
            flags |= SPECIAL_BYTE;
        } else if (o instanceof Float) {
            bytes = SerializeUtil.encodeInt(Float.floatToRawIntBits((Float) o));
            flags |= SPECIAL_FLOAT;
        } else if (o instanceof Double) {
            bytes = SerializeUtil.encodeLong(Double.doubleToRawLongBits((Double) o));
            flags |= SPECIAL_DOUBLE;
        } else if (o instanceof byte[]) {
            bytes = (byte[]) o;
            flags |= SPECIAL_BYTEARRAY;
        } else {
            if (!(o instanceof Serializable)) {
                throw new IllegalArgumentException(getClass().getSimpleName() + " requires implements Serializable");
            }
            bytes = SerializeUtil.serialize(o);
            flags |= SERIALIZED;
        }
        assert bytes != null;
        if (bytes.length > compressionThreshold) {
            byte[] compressed = GzipUtil.compress(bytes);
            if (compressed.length < bytes.length) {
                logger.debug(
                        "Compressed " + o.getClass().getName() + " from " + bytes.length + " to " + compressed.length);
                bytes = compressed;
                flags |= COMPRESSED;
            } else {
                logger.warn("Compressed increased the size of " + o.getClass().getName() + " from " + bytes.length
                        + " to " + compressed.length);
            }
        }
        return fillHeader(bytes, flags);
    }

    @Override
    public Object deserialize(byte[] src) throws Exception {
        if (src == null) {
            return null;
        }
        // 校验
        if (!validate(src)) {
            return SerializeUtil.deserialize(src);
        }
        // 标志
        int flag = readFlag(src);
        // 真实数据
        byte[] data = new byte[src.length - HEADER.length];
        System.arraycopy(src, HEADER.length, data, 0, src.length - HEADER.length);

        Object object = null;
        if ((flag & COMPRESSED) != 0) {
            data = GzipUtil.decompress(data);
        }
        int flags = flag & SPECIAL_MASK;
        if ((flag & SERIALIZED) != 0 && data != null) {
            object = SerializeUtil.deserialize(data);
        } else if (flags != 0 && data != null) {
            switch (flags) {
                case SPECIAL_BOOLEAN:
                    object = Boolean.valueOf(SerializeUtil.decodeBoolean(data));
                    break;
                case SPECIAL_INT:
                    object = Integer.valueOf(SerializeUtil.decodeInt(data));
                    break;
                case SPECIAL_LONG:
                    object = Long.valueOf(SerializeUtil.decodeLong(data));
                    break;
                case SPECIAL_DATE:
                    object = new Date(SerializeUtil.decodeLong(data));
                    break;
                case SPECIAL_BYTE:
                    object = Byte.valueOf(SerializeUtil.decodeByte(data));
                    break;
                case SPECIAL_FLOAT:
                    object = new Float(Float.intBitsToFloat(SerializeUtil.decodeInt(data)));
                    break;
                case SPECIAL_DOUBLE:
                    object = new Double(Double.longBitsToDouble(SerializeUtil.decodeLong(data)));
                    break;
                case SPECIAL_BYTEARRAY:
                    object = data;
                    break;
                default:
                    logger.warn("Undecodeable with flags=" + flags);
            }
        } else {
            object = SerializeUtil.deserializeString(data, DEFAULT_CHARSET);
        }
        return object;
    }

    private byte[] int2byte(int res) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (res & 0xff);// 最低位
        bytes[1] = (byte) ((res >> 8) & 0xff);// 次低位
        bytes[2] = (byte) ((res >> 16) & 0xff);// 次高位
        bytes[3] = (byte) (res >>> 24);// 最高位,无符号右移。
        return bytes;
    }

    private int byte2int(byte[] bytes) {
        int v = bytes[0] & 0xFF;
        v += (bytes[1] & 0xFF) << 8;
        v += (bytes[2] & 0xFF) << 16;
        v += (bytes[3] & 0xFF) << 24;
        return v;
    }

}
