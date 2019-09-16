package com.marmot.zk.listener;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.utils.ZKPaths;
import org.apache.curator.utils.ZKPaths.PathAndNode;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import com.google.common.collect.Lists;
import com.marmot.zk.client.exception.ZookeeperException;
import com.marmot.zk.client.impl.ZKClientImpl;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.event.NewNodeEventManager;
import com.marmot.zk.utils.GzipUtil;
import com.marmot.zk.utils.PathUtils;
import com.marmot.zk.utils.PropUtil;
import com.marmot.zk.utils.SystemUtil;

public class Operation4internal {


    // 日志
    private static final Logger logger = Logger.getLogger(Operation4internal.class);

    // 压缩范围
    public static final int GZIP_CRITICAL_LENGTH = 10240;

    // 新增节点
    public static boolean addNode(CuratorFramework client, String path, CreateMode mode) throws ZookeeperException {
        try {
            // 客户端创建节点 直接继承acl
            PathAndNode pathAndNode = ZKPaths.getPathAndNode(path);

            // 循环获取存在的父节点
            while (!exist(client, pathAndNode.getPath())) {
                pathAndNode = ZKPaths.getPathAndNode(pathAndNode.getPath());
            }
            // 获取acl
            List<ACL> acls = client.getZookeeperClient().getZooKeeper().getACL(pathAndNode.getPath(), new Stat());
            byte[] date = {};
            // 新增节点
            String[] nodes = path.replaceFirst(pathAndNode.getPath(), "").split("/");
            String parentPath = PathUtils.removeLastSlash(pathAndNode.getPath());
            for (String node : nodes) {
                if (StringUtils.isBlank(node)) {
                    continue;
                }
                String sonPath = parentPath.equals("/")? "/" + node:parentPath+"/" + node;
                // 创建节点直接继承父节点的acl
                client.create().creatingParentsIfNeeded().withMode(mode).withACL(acls).forPath(sonPath, date);
                parentPath = sonPath;
            }
        } catch (NodeExistsException e) {
            // 节点已经存在
            return false;
        } catch (Exception e) {
            throw new ZookeeperException("add node error,path=" + path, e);
        }
        return true;
    }

    // 删除节点
    public static boolean deleteNode(CuratorFramework client, String path) throws ZookeeperException {
        try {
            // 删除节点
            client.delete().guaranteed().forPath(path);
        } catch (NoNodeException e) {
            return false;
        } catch (Exception e) {
            throw new ZookeeperException("delete node error,path=" + path, e);
        }
        return true;
    }

    // 获取子节点列表
    public static List<String> getSubNodes(CuratorFramework client, String path) throws ZookeeperException {
        List<String> result = Lists.newArrayList();
        try {
            // 获取子节点
            result = client.getZookeeperClient().getZooKeeper().getChildren(path, false);
        } catch (NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new ZookeeperException("getSubNodes error,path=" + path, e);
        }
        return result;
    }

    // 获取节点值
    public static String getValue(CuratorFramework client, String path) throws ZookeeperException {

        String nodeData = StringUtils.EMPTY;
        try {
            // 获取节点数据
            byte[] dataByteArray = client.getZookeeperClient().getZooKeeper().getData(path, false, null);

            if (dataByteArray == null || dataByteArray.length == 0) {
                return nodeData;
            }

            // 如果是GZip压缩过的内容，用GZIP解压
            if (GzipUtil.isGzip(dataByteArray)) {
                nodeData = GzipUtil.uncompress(dataByteArray);
            } else {
                nodeData = new String(dataByteArray, "utf-8");
            }
        } catch (NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new ZookeeperException("getdata error,path=" + path, e);
        }
        return nodeData;
    }

    // 设置节点值
    public static void setValue(CuratorFramework client, String path, String value) throws ZookeeperException {
        try {
            String jsonData = StringUtils.isNotBlank(value) ? value : StringUtils.EMPTY;
            // 存入zk
            client.setData().forPath(path, jsonData.getBytes("utf-8"));
        } catch (Exception e) {
            throw new ZookeeperException("setdata error,path=" + path, e);
        }
    }

    // 设置节点值 如果节点值过大则压缩
    public static void setValue4Compress(CuratorFramework client, String path, String value) throws ZookeeperException {
        try {
            String jsonData = StringUtils.isNotBlank(value) ? value : StringUtils.EMPTY;
            byte[] byts = jsonData.getBytes("utf-8");

            if (jsonData.length() >= GZIP_CRITICAL_LENGTH) {
                byts = GzipUtil.compressString2byte(jsonData);
            }
            // 存入zk
            client.setData().forPath(path, byts);
        } catch (Exception e) {
            throw new ZookeeperException("setdata error,path=" + path, e);
        }
    }

    // 判断节点是否存在
    public static boolean exist(CuratorFramework client, String path) throws ZookeeperException {
        return getStat(client, path) != null;
    }

    // 获取节点的acl
    public static List<ACL> getAcl(CuratorFramework client, String path) throws ZookeeperException {
        List<ACL> acls = Lists.newArrayList();
        try {
            acls = client.getZookeeperClient().getZooKeeper().getACL(path, new Stat());
        } catch (Exception e) {
            throw new ZookeeperException("get acl error,path=" + path, e);
        }
        return acls;
    }

    // 修改节点的acl
    public static boolean setAcl(CuratorFramework client, String path, List<ACL> acls) throws ZookeeperException {
        try {
            client.getZookeeperClient().getZooKeeper().setACL(path, acls, -1);
            return true;
        } catch (Exception e) {
            throw new ZookeeperException("set acl error,path=" + path, e);
        }
    }

    // 获取节点状态
    public static Stat getStat(CuratorFramework client, String path) throws ZookeeperException {
        try {
            return client.checkExists().forPath(path);
        } catch (Exception e) {
            throw new ZookeeperException("get stat error,path=" + path, e);
        }
    }

    // 获取链接sessionId
    public static long getSessionId(CuratorFramework client) throws ZookeeperException {
        try {
            return client.getZookeeperClient().getZooKeeper().getSessionId();
        } catch (Exception e) {
            throw new ZookeeperException("get SessionId error", e);
        }
    }

    // watch节点
    public static void watchNode(CuratorFramework client, CuratorWatcher watcher, String path)
            throws ZookeeperException {
        try {
            client.checkExists().usingWatcher(watcher).forPath(path);
        } catch (Exception e) {
            throw new ZookeeperException("watch node error,", e);
        }

    }

    // watch子节点
    public static List<String> watchChildNode(CuratorFramework client, CuratorWatcher watcher, String path)
            throws ZookeeperException {
        List<String> results = null;
        try {
            results = client.getChildren().usingWatcher(watcher).forPath(path);
        } catch (Exception e) {
            throw new ZookeeperException("watch child node error,", e);
        }
        return results;
    }

    // 获取链接状态
    public static boolean isConnect(CuratorFramework client) {
        try {
            return client.getZookeeperClient().getZooKeeper().getState().isConnected();
        } catch (Exception e) {
            logger.error("get connect state error", e);
            return false;
        }
    }

    // 注销链接
    public static void destroy(NewNodeEventManager newNodeEventManager, CuratorFramework client) {

        logger.warn("Call destroy in ZKClient");
        // 释放所有资源
        newNodeEventManager.clear();
        try {
            // 关闭连接
            client.close();
            // 等待zookeeper的相关线程完全退出
            synchronized (client) {
                client.wait(200L);
            }
        } catch (Exception e) {
            logger.error("closed zk client error", e);
        }
        logger.warn("ZKClient is closed");
    }

    // 初始化zk链接
    public static void initZookeeperClient(CuratorFramework client, ZKClientImpl nocacheZookeeperClientImpl,
            boolean isRegistToZk) throws Exception {

        // 阻塞至链接成功启动
        if (!client.getZookeeperClient().getZooKeeper().getState().isConnected()) {

            final CountDownLatch latch = new CountDownLatch(1);

            client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
                @Override
                public void stateChanged(CuratorFramework client, ConnectionState newState) {
                    if (newState == ConnectionState.CONNECTED) {
                        latch.countDown();
                        // 链接第一次生成时使用，连接建立后销毁
                        client.getConnectionStateListenable().removeListener(this);
                    }
                }
            });

            // 链接超时时间 默认十秒
            int connectionTimeoutMs = 10000;

            // 等待
            boolean connectedResult = latch.await(connectionTimeoutMs, TimeUnit.MILLISECONDS);

            // 超过10秒还未能与zk建立连接，抛出例外
            if (!connectedResult && !client.getZookeeperClient().getZooKeeper().getState().isConnected()) {
                String url = client.getZookeeperClient().getZooKeeper().toString();
                throw new RuntimeException("start zk latch.await() overtime. zkConnectString=" + url);
            }
        }

        // 注册zk连接状态监听器
        client.getConnectionStateListenable()
                .addListener(new ZookeeperStateListener(nocacheZookeeperClientImpl, isRegistToZk));

        // 异步写入临时节点，给闪电配置系统监控用，把IP和项目名对应
        if (isRegistToZk) {
            registToZk(client);
        }
    }

    // 服务启动或者重新建立连接后,向zk服务器的/config/project/20078/clientRegistry下写入临时节点注册当前客户端信息
    // 给zk连接查询用
    public static void registToZk(CuratorFramework client) {

        try {
            long sessionId = client.getZookeeperClient().getZooKeeper().getSessionId();
            logger.warn(PropUtil.getInstance().get("project.name") + "建立ZK连接信息:"
                    + client.getZookeeperClient().getZooKeeper().toString());

            // 节点名: clientId_IP_sessionId
            String nodePath = EnumZKNameSpace.PROJECT.getNamespace() + "/10000/clientRegistry/"
                    + PathUtils.getCurrentClientId() + "_" + SystemUtil.getInNetworkIp() + "_0x"
                    + Long.toHexString(sessionId);

            if (exist(client, nodePath)) {
                // 存在就删除
                // 避免临时节点丢失问题
                deleteNode(client, nodePath);
            }

            // inBackground会后台不断尝试去创建
            client.create().withMode(CreateMode.EPHEMERAL).inBackground().forPath(nodePath, new byte[0]);
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
    }

}
