package com.marmot.cache.redis;

import java.util.Set;

public interface IRedisReload <T> extends IResourceReload<T> {

    /**
     * 重新加载资源
     * 
     * @param t servers ip:port,ip:port
     * @param instances 刷新的实例列表
     * @param password
     */
    public void reload(T t, Set<String> instances, String password);

}
