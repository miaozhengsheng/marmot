package com.marmot.cache.redis;

public interface IBlockHandle<T> {

    public void used(T t);

    public void release(T t);

    public void interruptAll();

    public boolean isBlock(String name);

}