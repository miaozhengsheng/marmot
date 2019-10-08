package com.marmot.cache;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import com.marmot.cache.exception.CacheException;

public interface ICacheClientWithException {


    /**
     * 设置Key-Value对象到memcached中
     * 
     * @param key
     * @param value
     * @param expire 过期时间，单位秒
     * @return
     * @throws CacheException
     */
    public boolean set(String key, Object value, int expire) throws CacheException;

    /**
     * 使用缺省过期时间设置Key-Value对象到memcached中
     * 
     * @param key
     * @param value
     * @return
     * @throws CacheException
     */
    public boolean set(String key, Object value) throws CacheException;

    /**
     * 设置Key-Value对象到memcached中
     * 
     * @param key
     * @param value
     * @param date 终止时间
     * @retrun
     * @throws CacheException
     */
    public boolean set(String key, Object value, Date date) throws CacheException;

    /**
     * 从memcached中获取对象
     * 
     * @param key
     * @return
     * @throws CacheException
     */
    public Object get(String key) throws CacheException;

    /**
     * 删除memcached中的对象
     * 
     * @param key
     * @return
     * @throws CacheException
     */
    public boolean delete(String key) throws CacheException;

    /**
     * 批量获取memcached中的对象
     * 
     * @param keys
     * @return
     * @throws CacheException
     */
    public Map<String, Object> getMap(Collection<String> keys) throws CacheException;

    /**
     * 初始化一个用作计数器的对象
     * 
     * @param key
     * @param num 初始数值
     * @return
     * @throws CacheException
     */
    public boolean setCounter(String key, long num) throws CacheException;

    /**
     * 初始化计数器
     * 
     * @param key
     * @param num 初始值
     * @param expire 过期时间
     * @return
     * @throws CacheException
     */
    public boolean setCounter(String key, long num, int expire) throws CacheException;

    /**
     * 将一个计数器加1并返回结果
     * 
     * @param key
     * @return the new value (Memcached: -1 if the key doesn't exist)
     * @throws CacheException
     */
    public long incr(String key) throws CacheException;

    /**
     * 将一个计数器加delta值，并返回结果
     * 
     * @param key
     * @param delta
     * @return the new value (Memcached: -1 if the key doesn't exist)
     * @throws CacheException
     */
    public long incr(String key, long delta) throws CacheException;

    /**
     * 将一个计数器减1并返回结果
     * 
     * @param key
     * @return the new value (Memcached: -1 if the key doesn't exist)
     * @throws CacheException
     */
    public long decr(String key) throws CacheException;

    /**
     * 将一个计数器减delta值，并返回结果
     * 
     * @param key
     * @param delta
     * @return the new value (Memcached: -1 if the key doesn't exist)
     * @throws CacheException
     */
    public long decr(String key, long delta) throws CacheException;

    /**
     * 关闭服务，释放链接
     * 
     * @throws CacheException
     */
    public void shutdown() throws CacheException;


}
