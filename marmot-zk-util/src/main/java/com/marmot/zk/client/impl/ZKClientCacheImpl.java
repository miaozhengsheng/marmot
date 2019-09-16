package com.marmot.zk.client.impl;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.marmot.zk.client.IZKClient;
import com.marmot.zk.client.exception.ZookeeperException;
import com.marmot.zk.client.leader.NewLeader;
import com.marmot.zk.client.lock.DistributedReadWriteLock;
import com.marmot.zk.client.lock.DistributedReentrantLock;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.event.NewNodeEventManager;
import com.marmot.zk.listener.Listener;
import com.marmot.zk.utils.JsonUtil;
import com.marmot.zk.utils.PathUtils;

public class ZKClientCacheImpl implements IZKClient {

    // 日志
    private static final Logger logger = Logger.getLogger(ZKClientCacheImpl.class);

    // 无缓存的类
    private IZKClient zookeeperClientImpl;

    // 缓存管理
    private NewNodeEventManager nodeEventManager;

    public ZKClientCacheImpl(IZKClient zookeeperClient, NewNodeEventManager nodeEventManager) {
        this.zookeeperClientImpl = zookeeperClient;
        this.nodeEventManager = nodeEventManager;
    }

    @Override
    public List<String> getSubNodes(EnumZKNameSpace namespace, String path) throws ZookeeperException {

        // 获取全路径
        String fullPath = PathUtils.joinPath(namespace, path);

        // 从缓存取
        List<String> subNodes = nodeEventManager.getSubNodesCache(fullPath);
        if (subNodes != null) {
            return subNodes;
        }

        // 从zk取
        subNodes = zookeeperClientImpl.getSubNodes(namespace, path);
        // 存入缓存及监听
        putChildCacheAndSetWatch(fullPath, subNodes);

        return subNodes;
    }

    @Override
    public Map<String, Object> getMap(EnumZKNameSpace namespace, String path) throws ZookeeperException {

        String value = getString(namespace, path);
        if (value == null) {
            return null;
        }
        // 转换为map
        Map<String, Object> results = StringUtils.isBlank(value) ? Maps.newHashMap() : JsonUtil.json2map(value);
        if (results == null) {
            throw new ZookeeperException("node value is not json data,path=" + path + ",value=" + value);
        }
        return results;
    }

    @Override
    public String getString(EnumZKNameSpace namespace, String path) throws ZookeeperException {
        // 获取全路径
        String fullPath = PathUtils.joinPath(namespace, path);

        // 从缓存取 缓存空不传null
        String value = nodeEventManager.getNodeCache(fullPath);
        if (value != null) {
            return value;
        }
        // 从zk取
        value = zookeeperClientImpl.getString(namespace, path);
        if(value == null) {
            return null;
        }
        // 存入缓存及监听
        putCacheAndSetWatch(fullPath, value);
        return value;
    }

    @Override
    public boolean exist(EnumZKNameSpace namespace, String path) throws ZookeeperException {
        // 先走缓存
        String fullPath = PathUtils.joinPath(namespace, path);
        Boolean isExit = nodeEventManager.getNodeExistCache(fullPath);
        if (isExit != null) {
            return isExit;
        }
        // 无缓存 从zk读取
        isExit = zookeeperClientImpl.exist(namespace, path);
        // 设置监听及缓存
        putExistCacheAndSetWatch(fullPath, isExit);
        return isExit;
    }

    @Override
    public boolean addTempNode(EnumZKNameSpace namespace, String path) throws ZookeeperException {
        // 无需做缓存
        return zookeeperClientImpl.addTempNode(namespace, path);
    }

    @Override
    public boolean addNode(EnumZKNameSpace namespace, String path) throws ZookeeperException {
        // 无需做缓存
        return zookeeperClientImpl.addNode(namespace, path);
    }

    @Override
    public boolean deleteNode(EnumZKNameSpace namespace, String path) throws ZookeeperException {
        // 无需做缓存
        return zookeeperClientImpl.deleteNode(namespace, path);
    }

    @Override
    public void setTempNode4String(EnumZKNameSpace namespace, String path, String value) throws ZookeeperException {
        // 无需做缓存
        if (value == null) {
            throw new RuntimeException("value can not be null,path=" + path);
        }
        zookeeperClientImpl.setTempNode4String(namespace, path, value);
    }

    @Override
    public void setTempNode4Map(EnumZKNameSpace namespace, String path, Map<String, Object> value)
            throws ZookeeperException {
        // 无需做缓存
        if (value == null) {
            throw new RuntimeException("value can not be null,path=" + path);
        }
        String jsonData = (value != null) ? StringUtils.stripToEmpty(JsonUtil.toJson(value)) : StringUtils.EMPTY;
        setTempNode4String(namespace, path, jsonData);
    }

    @Override
    public void setNode4Map(EnumZKNameSpace namespace, String path, Map<String, Object> value) throws ZookeeperException {
        // 无需做缓存
        if (value == null) {
            throw new RuntimeException("value can not be null,path=" + path);
        }
        String jsonData = (value != null) ? StringUtils.stripToEmpty(JsonUtil.toJson(value)) : StringUtils.EMPTY;
        setNode4String(namespace, path, jsonData);
    }

    @Override
    public void setNode4String(EnumZKNameSpace namespace, String path, String value) throws ZookeeperException {
        // 无需做缓存
        if (value == null) {
            throw new RuntimeException("value can not be null,path=" + path);
        }
        zookeeperClientImpl.setNode4String(namespace, path, value);
    }

    @Override
    public void addListener(Listener listener, boolean single) throws ZookeeperException {
        // 无需做缓存（有监听必有缓存）
        zookeeperClientImpl.addListener(listener, single);
    }

    @Override
    public void addListener(Listener listener) throws ZookeeperException {
        // 无需做缓存（有监听必有缓存）
        zookeeperClientImpl.addListener(listener);
    }

    @Override
    public void addConnectionStateListener(ConnectionStateListener listener) throws ZookeeperException {
        // 无需做缓存
        zookeeperClientImpl.addConnectionStateListener(listener);
    }

    @Override
    public void removeListener(Listener listener) throws ZookeeperException {
        // 无需做缓存
        zookeeperClientImpl.removeListener(listener);
    }

    @Override
    public NewLeader createLeader(String selectNode, String id) throws ZookeeperException {
        // 无需做缓存
        return zookeeperClientImpl.createLeader(selectNode, id);
    }

    @Override
    public NewLeader createLeader(String selectNode) throws ZookeeperException {
        // 无需做缓存
        return zookeeperClientImpl.createLeader(selectNode);
    }

    @Override
    public DistributedReentrantLock createDistributedReentrantLock(String lockNode) throws ZookeeperException {
        // 无需做缓存
        return zookeeperClientImpl.createDistributedReentrantLock(lockNode);
    }

    @Override
    public DistributedReadWriteLock createDistributedReadWriteLock(String lockNode) throws ZookeeperException {
        // 无需做缓存
        return zookeeperClientImpl.createDistributedReadWriteLock(lockNode);
    }

    @Override
    public Object getSelfConfigDataByKey(String key) throws ZookeeperException {
        Map<String, Object> config = getMap(EnumZKNameSpace.PROJECT, PathUtils.getCurrentClientId() + "/config");
        return config != null ? config.get(key) : null;
    }

    @Override
    public void destroy() {
        // 无需做缓存
        zookeeperClientImpl.destroy();
    }

    // 设置监听器及缓存
    private void putCacheAndSetWatch(String fullPath, String value) {
        // 写入缓存
        nodeEventManager.setNodeCache(fullPath, value);
        try {
            nodeEventManager.watchNode(fullPath);
        } catch (Exception e) {
            logger.warn("watch node failure. path=" + fullPath, e);
        }
    }

    // 设置节点是否存在缓存及监听
    private void putExistCacheAndSetWatch(String fullPath, boolean isExist) {
        // 写入缓存
        nodeEventManager.setNodeExistCache(fullPath, isExist);
        try {
            nodeEventManager.watchNode(fullPath);
        } catch (Exception e) {
            logger.warn("watch node failure. path=" + fullPath, e);
        }
    }

    // 设置子节点缓存及监听
    private void putChildCacheAndSetWatch(String fullPath, List<String> childrenNodes) {
        // 写入缓存
        List<String> finalChildrenNodes = childrenNodes == null ? Lists.newArrayList()
                : Lists.newArrayList(childrenNodes);
        nodeEventManager.setSubNodesCache(fullPath, finalChildrenNodes);
        try {
            nodeEventManager.watchChild(fullPath);
        } catch (Exception e) {
            logger.warn("watch Child node failure. path=" + fullPath, e);
        }
    }
}
