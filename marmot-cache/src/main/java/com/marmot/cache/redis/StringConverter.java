package com.marmot.cache.redis;

import java.nio.charset.Charset;

public class StringConverter implements Converter<String> {

    private final Charset charset = Charset.forName("UTF8");

    @Override
    public byte[] serialize(String source) throws Exception {
        return (source == null ? null : source.getBytes(charset));
    }

    @Override
    public String deserialize(byte[] source) throws Exception {
        return (source == null ? null : new String(source, charset));
    }

}
