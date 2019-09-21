package com.marmot.cache;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

public interface ICacheClient {


    /**
     * 设置Key-Value对象到缓存中
     * 
     * @param key
     * @param value
     * @param expire 过期时间，单位秒
     * @return
     */
    public Boolean set(String key, Object value, int expire);

    /**
     * 使用缺省过期时间设置Key-Value对象到缓存中
     * 
     * @param key
     * @param value
     * @return
     */
    public Boolean set(String key, Object value);

    /**
     * 设置Key-Value对象到缓存中
     * 
     * @param key
     * @param value
     * @param date 终止时间
     */
    public Boolean set(String key, Object value, Date date);

    /**
     * 从memcached中获取对象
     * 
     * @param key
     * @return
     */
    public Object get(String key);

    /**
     * 删除缓存中的对象
     * 
     * @param key
     * @return
     */
    public Boolean delete(String key);

    /**
     * 批量获取缓存中的对象
     * 
     * @param keys
     * @return
     */
    public Map<String, Object> getMap(Collection<String> keys);

    /**
     * 初始化一个用作计数器的对象
     * 
     * @param key
     * @param num 初始数值
     * @return
     */
    public Boolean setCounter(String key, long num);

    /**
     * 初始化计数器
     * 
     * @param key
     * @param num 初始值
     * @param expire 过期时间
     * @return
     */
    public Boolean setCounter(String key, long num, int expire);

    /**
     * 将一个计数器加1并返回结果
     * 
     * @param key
     * @return the new value (Memcached: -1 if the key doesn't exist)
     */
    public Long incr(String key);

    /**
     * 将一个计数器加delta值，并返回结果
     * 
     * @param key
     * @param delta
     * @return the new value (Memcached: -1 if the key doesn't exist)
     */
    public Long incr(String key, long delta);

    /**
     * 将一个计数器减1并返回结果
     * 
     * @param key
     * @return the new value (Memcached: -1 if the key doesn't exist)
     */
    public Long decr(String key);

    /**
     * 将一个计数器减delta值，并返回结果
     * 
     * @param key
     * @param delta
     * @return the new value (Memcached: -1 if the key doesn't exist)
     */
    public Long decr(String key, long delta);

    /**
     * 关闭服务，释放链接
     */
    public void shutdown();


}
