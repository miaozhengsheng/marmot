package com.marmot.cache.redis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ObjectConverter implements Converter<Object> {

    @Override
    public byte[] serialize(Object source) throws Exception {
        if (!(source instanceof Serializable)) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " requires a Serializable");
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(128);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream);
        objectOutputStream.writeObject(source);
        objectOutputStream.close();
        return byteStream.toByteArray();
    }

    @Override
    public Object deserialize(byte[] source) throws Exception {
        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(source));
        Object obj = objectInputStream.readObject();
        objectInputStream.close();
        return obj;
    }

}
