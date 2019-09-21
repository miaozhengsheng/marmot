package com.marmot.cache.redis;

public interface Converter<T> {

    public byte[] serialize(T source) throws Exception;

    public T deserialize(byte[] source) throws Exception;
    
}