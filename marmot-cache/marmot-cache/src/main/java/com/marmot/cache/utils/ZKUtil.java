package com.marmot.cache.utils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.marmot.cache.constants.CacheConst;
import com.marmot.common.structs.Pair;
import com.marmot.zk.client.exception.ZookeeperException;
import com.marmot.zk.client.impl.ZKClientImpl;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.utils.ZookeeperFactory;

public class ZKUtil {


    private static final ZKClientImpl ZKCLIENT = (ZKClientImpl) ZookeeperFactory.useDefaultZookeeper();

    /**
     * 根据项目名获取redis集群ip:port地址信息
     * <p>
     * redis格式如下：<br>
     * ip1:port1,ip11:port11<br>
     * ip2:port2,ip22:port22<br>
     * ...<br>
     * 或<br>
     * ip1:port1<br>
     * ip2:port2<br>
     * ...<br>
     * 
     * @param groupId
     * @return
     * @throws ZookeeperException 
     */
    public static Set<String> getRedisClusters(String groupId) throws ZookeeperException {
        return getCacheClusters(
                EnumZKNameSpace.PUBLIC, CacheConst.CACHE_REDIS_GROUPINFO_NODE_PATH + groupId);
    }

    /**
     * 根据项目名获取memcached集群ip:port地址信息
     * <p>
     * memcached格式如下：<br>
     * ip1:port1<br>
     * ip2:port2<br>
     * ...<br>
     * 
     * @param groupId
     * @return
     * @throws ZookeeperException 
     */
    public static Set<String> getMemcachedClusters(String groupId) throws ZookeeperException {
        return getCacheClusters(
        		EnumZKNameSpace.PUBLIC, CacheConst.CACHE_MEMCACHED_GROUPINFO_NODE_PATH + groupId);
    }

    /**
     * 自动根据环境标识获取redis集群ip:port地址信息
     * <p>
     * redis格式如下：<br>
     * ip1:port1,ip11:port11<br>
     * ip2:port2,ip22:port22<br>
     * ...<br>
     * 或<br>
     * ip1:port1<br>
     * ip2:port2<br>
     * ...<br>
     * 
     * @param projectName
     * @param groupName
     * @return
     * @throws ZookeeperException 
     */
    public static Set<String> getRedisClusters(String projectName, String groupName) throws ZookeeperException {
        String groupId = getCacheGroupId(EnumZKNameSpace.PUBLIC, CacheConst.CACHE_REDIS_PROJECTINFO_NODE_PATH + projectName + "/" + groupName);
        if (groupId != null) {
            return getRedisClusters(groupId);
        }
        return Collections.emptySet();
    }

    /**
     * 自动根据环境标识获取memcached集群ip:port地址信息
     * <p>
     * memcached格式如下：<br>
     * ip1:port1<br>
     * ip2:port2<br>
     * ...<br>
     * 
     * @param projectName
     * @param groupName
     * @return
     * @throws ZookeeperException 
     */
    public static Set<String> getMemcachedClusters(String projectName, String groupName) throws ZookeeperException {
        String groupId = getCacheGroupId(EnumZKNameSpace.PUBLIC, CacheConst.CACHE_MEMCACHED_PROJECTINFO_NODE_PATH + projectName + "/" + groupName);
        if (groupId != null) {
            return getMemcachedClusters(groupId);
        }
        return Collections.emptySet();
    }

    /**
     * 根据zk节点全路径获取缓存集群ip:port地址信息
     * <p>
     * memcached格式如下：<br>
     * ip1:port1<br>
     * ip2:port2<br>
     * ...<br>
     * <p>
     * redis格式如下：<br>
     * ip1:port1,ip11:port11<br>
     * ip2:port2,ip22:port22<br>
     * ...<br>
     * 或<br>
     * ip1:port1<br>
     * ip2:port2<br>
     * ...<br>
     * 
     * @param path
     * @return
     * @throws ZookeeperException 
     */
    public static Set<String> getCacheClusters(EnumZKNameSpace space,String path) throws ZookeeperException {
        String dataFromZk = ZKCLIENT.getString(space, path);
        if (dataFromZk == null) {
            return Collections.emptySet();
        }
        Set<String> cacheClusterSet = new LinkedHashSet<String>();
        String[] lines = dataFromZk.split("\r\n");
        for (String line : lines) {
            cacheClusterSet.add(line.trim());
        }
        return cacheClusterSet;
    }

    /**
     * 根据zk节点全路径获取缓存集群分组ID信息
     * 
     * @param path
     * @return
     * @throws ZookeeperException 
     */
    public static String getCacheGroupId(EnumZKNameSpace space,String path) throws ZookeeperException {
        return ZKCLIENT.getString(space,path);
    }

    public static String getRedisPassword() throws ZookeeperException {
        return ZKCLIENT.getString(EnumZKNameSpace.PUBLIC, CacheConst.REDIS_PASSWORD_NODE);
    }

    public static Pair<String, String> getMemcachedPassword() throws ZookeeperException {
        Pair<String, String> pair = null;
        Map<String, Object> data = ZKCLIENT.getMap(EnumZKNameSpace.PUBLIC,
                CacheConst.MEMCACHED_PASSWORD_NODE);
        if (data != null) {
            pair = new Pair<String, String>((String) data.get("username"), (String) data.get("password"));
        }
        return pair;
    }


    /**
     * 自动根据环境标识判断项目引用的redis是否需要密码鉴权
     * 
     * @param projectName
     * @return
     * @throws ZookeeperException 
     */
    public static boolean isRedisAuth(String projectName, String groupName) throws ZookeeperException {
        String path = null;
        String groupId = getCacheGroupId(EnumZKNameSpace.PUBLIC
                , CacheConst.CACHE_REDIS_PROJECTINFO_NODE_PATH + projectName + "/" + groupName);
        if (groupId != null) {
            Set<String> set = getRedisClusters(groupId);
            if (set.size() > 0) {
                path = CacheConst.CACHE_REDIS_GROUPINFO_NODE_PATH + groupId;
            }
        }
        if (path == null) {
            // 无配置时，返回什么都无意义
            return false;
        }
        return ZKCLIENT.exist(EnumZKNameSpace.PUBLIC, path + "/" + CacheConst.AUTH_NODE);
    }

    /**
     * 自动根据环境标识判断项目引用的memcached是否需要密码鉴权
     * 
     * @param projectName
     * @param groupName
     * @return
     * @throws ZookeeperException 
     */
    public static boolean isMemcachedAuth(String projectName, String groupName) throws ZookeeperException {
        String path = null;
        String groupId = getCacheGroupId(EnumZKNameSpace.PUBLIC,CacheConst.CACHE_MEMCACHED_PROJECTINFO_NODE_PATH + projectName + "/" + groupName);
        if (groupId != null) {
            Set<String> set = getMemcachedClusters(groupId);
            if (set.size() > 0) {
                path = CacheConst.CACHE_MEMCACHED_GROUPINFO_NODE_PATH + groupId;
            }
        }
        if (path == null) {
            // 无配置时，返回什么都无意义
            return false;
        }
        return ZKCLIENT.exist(EnumZKNameSpace.PUBLIC, path + "/" + CacheConst.AUTH_NODE);
    }

    /**
     * 获取集群group信息存储zk路径
     * 
     * @param projectName
     * @param groupName
     * @param cacheType
     * @return
     * @throws ZookeeperException 
     */
    public static String getGroupIdPath(String projectName, String groupName) throws ZookeeperException {
    
            String groupId = getCacheGroupId(EnumZKNameSpace.PUBLIC, CacheConst.CACHE_REDIS_PROJECTINFO_NODE_PATH + projectName + "/" + groupName);
            if (groupId != null) {
                Set<String> set = getRedisClusters(groupId);
                if (set.size() > 0) {
                    return EnumZKNameSpace.PUBLIC.getNamespace() + CacheConst.CACHE_REDIS_GROUPINFO_NODE_PATH + groupId;
                }
            }

        // 无配置时，返回什么都无意义
        return null;
    }

    /**
     * 获取集群project信息存储zk路径
     * 
     * @param projectName
     * @param groupName
     * @param cacheType
     * @return
     */
    public static String getProjectInfoPath(String projectName, String groupName) {
            return EnumZKNameSpace.PUBLIC.getNamespace() + CacheConst.CACHE_REDIS_PROJECTINFO_NODE_PATH + projectName
                    + "/" + groupName;
    }


}
