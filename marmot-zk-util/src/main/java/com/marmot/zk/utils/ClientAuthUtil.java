package com.marmot.zk.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.log4j.Logger;
import org.apache.zookeeper.WatchedEvent;

import com.marmot.zk.constants.ZKConstants;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.listener.Operation4internal;

public class ClientAuthUtil {


    private final Logger logger = Logger.getLogger(ClientAuthUtil.class);

    // 缓存节点配置值
    private Map<String, Object> CONF_CACHE = new HashMap<String, Object>();

    // 客户端连接
    private CuratorFramework client;

    // 配置监听路径
    private String confPaths = PathUtils.joinPath(EnumZKNameSpace.PROJECT, ZKConstants.ZK_ACL_CONF_ROOT);

    // 是否需要鉴权
    private boolean isAuth;

    public ClientAuthUtil(CuratorFramework client, boolean isAuth) {
        // 是否需要鉴权
        this.isAuth = isAuth;
        if (isAuth) {
            this.client = client;
            try {
                watchNode();
            } catch (Exception e) {
                throw new RuntimeException("watch node error,path=" + confPaths, e);
            }
        }
    }

    private void watchNode() throws Exception {
        // 监听配置
        client.checkExists().usingWatcher(new ConfWatcher(client)).forPath(confPaths);
        String value = Operation4internal.getValue(client, confPaths);
        if (value != null) {
            CONF_CACHE = JsonUtil.json2map(value);
        }
    }

    private class ConfWatcher implements CuratorWatcher {

        private ConfWatcher(CuratorFramework client) {
        }

        @Override
        public void process(WatchedEvent event) throws Exception {
            try {
                switch (event.getType()) {
                case NodeDataChanged:
                    // 重新注册监听事件
                    client.checkExists().usingWatcher(this).forPath(confPaths);
                    // 避免监听事件响应了，却获取不到最新的数据来同步缓存数据
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                    } catch (InterruptedException e) {
                    }
                    String nodeData = Operation4internal.getValue(client, confPaths);
                    // 节点内容更新到缓存
                    if (nodeData != null) {
                        CONF_CACHE = JsonUtil.json2map(nodeData);
                    }
                    break;

                default:
                    break;
                }
            } catch (Exception e) {
                logger.error("watcher.process() failure. " + event.toString(), e);
            }
        }

    }

    @SuppressWarnings("unchecked")
    /**
     * 
     * @描述：判断客户端是否拥有project节点下的写权限
     * @param client
     * @param fullPath
     * @return
     * @return boolean
     * @exception @createTime：2018年10月15日
     * @author: ch
     */
    public boolean validateWrite(String fullPath) {
        if (!isAuth) {
            return true;
        }
        if (StringUtils.isBlank(fullPath)) {
            return false;
        }
        String clientId = PathUtils.getCurrentClientId();
        // 校验
        // 检查/config/20078/authority下是否注册过当前clineId的写权限

        Map<String, List<String>> authority4Write = null;

        if (CONF_CACHE == null) {
            // 无配置 所有客户端可写
            return true;
        }
        Object authority4Writeobj = CONF_CACHE.get(ZKConstants.KEY_CLIENT_WRITE);
        if (authority4Writeobj == null) {
            // 无写配置 所有客户端不可写
            return false;
        }
        authority4Write = (Map<String, List<String>>) authority4Writeobj;

        // 获取fullPath路径上的每个节点
        String[] nodeNames = getNodesByPath(fullPath);
        // 正向循环节点，判断是否有读权限
        StringBuilder checkPath = new StringBuilder();
        for (String nodeName : nodeNames) {
            if (StringUtils.isBlank(nodeName)) {
                checkPath.append(ZKConstants.ZK_ROOT_NODE);
            } else {
                checkPath.append(nodeName);
            }
            List<String> auth4clientIds = authority4Write.get(checkPath.toString());
            if (auth4clientIds != null) {
                if (auth4clientIds.contains(clientId) || auth4clientIds.contains("*")) {
                    return true;
                }
            }
            if (!ZKConstants.ZK_ROOT_NODE.equals(checkPath.toString())) {
                checkPath.append(ZKConstants.ZK_ROOT_NODE);
            }
        }
        return false;
    }

    // 判断客户端是否拥有project节点下的读权限，避免交叉访问
    @SuppressWarnings("unchecked")
    public boolean validateRead(String fullPath) {
        if (!isAuth) {
            return true;
        }
        if (StringUtils.isBlank(fullPath)) {
            return false;
        }

        String clientId = PathUtils.getCurrentClientId();
        // 读权限校验 无状态 配置了才设置权限
        // 校验
        // 检查是否注册过当前clineId的读权限
        Map<String, List<String>> authority4Read = null;

        if (CONF_CACHE == null) {
            // 无配置 所有客户端可读
            return true;
        }

        Object authority4Readobj = CONF_CACHE.get(ZKConstants.KEY_CLIENT_RO);
        if (authority4Readobj == null) {
            // 无写配置 所有客户端可读
            return true;
        }
        authority4Read = (Map<String, List<String>>) authority4Readobj;

        // 获取fullPath路径上的每个节点
        String[] nodeNames = getNodesByPath(fullPath);
        // 正向循环节点，判断是否有读权限
        StringBuilder checkPath = new StringBuilder("");
        // 当前节点及父节点是否配置了读权限
        Boolean isNullConfig = true;
        for (String nodeName : nodeNames) {
            if (StringUtils.isBlank(nodeName)) {
                checkPath.append(ZKConstants.ZK_ROOT_NODE);
            } else {
                checkPath.append(nodeName);
            }
            List<String> auth4clientIds = authority4Read.get(checkPath.toString());
            if (auth4clientIds != null && auth4clientIds.size() > 0) {
                // 当前节点或父节点已经配置了读权限
                isNullConfig = false;
                if (auth4clientIds.contains(clientId)) {
                    return true;
                }
            }
            if (!ZKConstants.ZK_ROOT_NODE.equals(checkPath.toString())) {
                checkPath.append(ZKConstants.ZK_ROOT_NODE);
            }
        }
        return isNullConfig;
    }

    private String[] getNodesByPath(String fullPath) {
        String[] nodeNames = fullPath.split(ZKConstants.ZK_ROOT_NODE);
        return nodeNames;
    }

}
