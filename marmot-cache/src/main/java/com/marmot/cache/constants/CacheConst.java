package com.marmot.cache.constants;

public class CacheConst {


    public static final String LOG_PREFIX = "MarmotCache,";

    public static final String CACHE_REDIS_GROUPINFO_NODE_PATH = "/cache/redis/groupInfo/";
    public static final String CACHE_REDIS_PROJECTINFO_NODE_PATH = "/cache/redis/projectInfo/";
    public static final String CACHE_MEMCACHED_GROUPINFO_NODE_PATH = "/cache/memcached/groupInfo/";
    public static final String CACHE_MEMCACHED_PROJECTINFO_NODE_PATH = "/cache/memcached/projectInfo/";

    public static final String AUTH_NODE = "security";// 区别是否需要密码鉴权的节点名
    public static final String REDIS_PASSWORD_NODE = "/security/redis_pwd";
    public static final String MEMCACHED_PASSWORD_NODE = "/security/memcached_pwd";
    public static final String GROUP_DEFAULT_NAME = "default";


}
