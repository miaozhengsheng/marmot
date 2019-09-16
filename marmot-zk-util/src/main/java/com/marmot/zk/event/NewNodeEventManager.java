package com.marmot.zk.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.utils.ZKPaths;
import org.apache.curator.utils.ZKPaths.PathAndNode;
import org.apache.log4j.Logger;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event;
import org.apache.zookeeper.Watcher.Event.EventType;
import com.google.common.collect.Lists;
import com.marmot.zk.client.IZKClient;
import com.marmot.zk.client.exception.ZookeeperException;
import com.marmot.zk.client.impl.ZKClientImpl;
import com.marmot.zk.enums.EnumChangedEvent;
import com.marmot.zk.listener.Listener;
import com.marmot.zk.listener.NewNodeChildListener;
import com.marmot.zk.listener.NewNodeListener;
import com.marmot.zk.listener.Operation4internal;
import com.marmot.zk.utils.PathUtils;
import com.marmot.zk.utils.ProxyUtils;

public class NewNodeEventManager {


    // 日志
    private static final Logger logger = Logger.getLogger(NewNodeEventManager.class);

    // 节点监听器Map
    private final ConcurrentHashMap<String, CuratorWatcher> NODE_WATCHER_MAP = new ConcurrentHashMap<String, CuratorWatcher>();

    // 子节点监听器Map
    private final ConcurrentHashMap<String, CuratorWatcher> CHILDREN_WATCHER_MAP = new ConcurrentHashMap<String, CuratorWatcher>();

    // 子节点监听器的父节点的状态Map
    private final ConcurrentHashMap<String, Stat> CHILDREN_WATCHER_STAT_MAP = new ConcurrentHashMap<String, Stat>();

    // 客户端注册节点监听事件Map
    private final ConcurrentHashMap<String, List<NewNodeListener>> NODE_EVENT_MAP = new ConcurrentHashMap<String, List<NewNodeListener>>();

    // 客户端注册子节点监听事件Map
    private final ConcurrentHashMap<String, List<NewNodeChildListener>> CHILD_EVENT_MAP = new ConcurrentHashMap<String, List<NewNodeChildListener>>();

    // 客户端注册节点监听事件Map(串行)
    private final ConcurrentHashMap<String, List<NewNodeListener>> NODE_EVENT_SINGLE_MAP = new ConcurrentHashMap<String, List<NewNodeListener>>();

    // 客户端注册子节点监听事件Map(串行)
    private final ConcurrentHashMap<String, List<NewNodeChildListener>> CHILD_EVENT_SINGLE_MAP = new ConcurrentHashMap<String, List<NewNodeChildListener>>();

    // 客户端事件执行线程池
    private final ExecutorService EVENT_TASK_POOL = Executors.newCachedThreadPool();

    // 客户端事件执行线程池(串行)
    private final ExecutorService EVENT_TASK_SINGLE_POOL = Executors.newSingleThreadExecutor();

    // 客户端链接
    private CuratorFramework client;

    // 回调事件返回无缓存的实现类
    private IZKClient zookeeperClient;

    // 节点缓存值
    private final ConcurrentHashMap<String, String> NODE_CACHE = new ConcurrentHashMap<String, String>();

    // 子节点名列表缓存
    private final ConcurrentHashMap<String, List<String>> SUB_NODES_CACHE = new ConcurrentHashMap<String, List<String>>();

    // 节点是否存在的缓存
    private final ConcurrentHashMap<String, Boolean> NODE_EXIST_CACHE = new ConcurrentHashMap<String, Boolean>();

    public void clear() {
        NODE_WATCHER_MAP.clear();
        CHILDREN_WATCHER_MAP.clear();
        CHILDREN_WATCHER_STAT_MAP.clear();
        NODE_EVENT_MAP.clear();
        CHILD_EVENT_MAP.clear();
        NODE_EVENT_SINGLE_MAP.clear();
        CHILD_EVENT_SINGLE_MAP.clear();
        EVENT_TASK_POOL.shutdown();
        EVENT_TASK_SINGLE_POOL.shutdown();
    }

    public NewNodeEventManager(CuratorFramework client, ZKClientImpl zookeeperClient, boolean isAuth) {
        this.client = client;
        this.zookeeperClient = ProxyUtils.getProxy4ZKClient(zookeeperClient, client, isAuth);
    }


    // 获取节点缓存值
    public String getNodeCache(String path) {
        return NODE_CACHE.get(path);
    }

    // 修改节点缓存值
    public void setNodeCache(String path, String value) {

        if (value == null) {
            // value不允许为null
            value = StringUtils.EMPTY;
        }
        NODE_CACHE.put(path, value);
    }

    // 清空节点缓存值
    public void removeNodeCache(String path) {
        NODE_CACHE.remove(path);
    }

    // 获取子节点缓存
    public List<String> getSubNodesCache(String path) {
        List<String> subNodes = SUB_NODES_CACHE.get(path);
        if (subNodes == null) {
            return null;
        }
        // 返回副本 避免误操作
        return Lists.newArrayList(subNodes);
    }

    // 设置子节点缓存
    public void setSubNodesCache(String path, List<String> subNodes) {
        if (subNodes == null) {
            // subNodes不允许为null
            subNodes = Lists.newArrayList();
        }
        SUB_NODES_CACHE.put(path, subNodes);
    }

    // 清空子节点缓存
    public void removeSubNodesCache(String path) {
        SUB_NODES_CACHE.remove(path);
    }

    // 获取节点是否存在的缓存
    public Boolean getNodeExistCache(String path) {
        return NODE_EXIST_CACHE.get(path);
    }

    // 设置节点是否存在的缓存
    public void setNodeExistCache(String path, boolean isExist) {
        NODE_EXIST_CACHE.put(path, isExist);
    }

    // 清空节点是否存在的缓存
    public void removeNodeExistCache(String path) {
        NODE_EXIST_CACHE.remove(path);
    }

    // 获取zookeeperClient
    public IZKClient getZookeeperClient() {
        return zookeeperClient;
    }

    // 获取所有监听节点的watcher
    public ConcurrentHashMap<String, CuratorWatcher> getNodeWatcherMap() {
        return NODE_WATCHER_MAP;
    }

    // 获取所有监听子节点的watcher
    public ConcurrentHashMap<String, CuratorWatcher> getChildrenWatcherMap() {
        return CHILDREN_WATCHER_MAP;
    }

    // 注册节点监听器(为该节点生成一个监听器实例并注册到节点上)
    public void watchNode(String path) throws ZookeeperException {

        CuratorWatcher curatorWatcher = NODE_WATCHER_MAP.get(path);

        // 如果curatorWatcher为非空表明已经注册过监听
        if (curatorWatcher == null) {
            // 监听器加入缓存
            NODE_WATCHER_MAP.putIfAbsent(path, new NewNodeWatcher(client, this, path));
            curatorWatcher = NODE_WATCHER_MAP.get(path);

            // 如果zk连接已断开，直接返回
            if (!Operation4internal.isConnect(client)) {
                return;
            }
            // 注册监听
            Operation4internal.watchNode(client, curatorWatcher, path);
        }

    }

    // 注册子节点监听器(为该节点生成一个监听器实例，在getChild()方法上注册监听器)
    public void watchChild(String parentPath) throws ZookeeperException {

        // 已经注册过
        if (CHILDREN_WATCHER_MAP.get(parentPath) != null) {
            return;
        }
        // 子节点监听器加入缓存
        CuratorWatcher curatorWatcher = getChildWatcher(parentPath);

        // 注册监听父节点
        watchNode(parentPath);

        // 如果zk连接已断开，直接返回
        if (!Operation4internal.isConnect(client)) {
            return;
        }
        // 注册子节点监听
        List<String> children = Operation4internal.watchChildNode(client, curatorWatcher, parentPath);
        Stat stat = Operation4internal.getStat(client, parentPath);
        if (stat != null) {
            // 缓存加入父节点状态
            CHILDREN_WATCHER_STAT_MAP.put(parentPath, stat);
        }

        // 为父节点下的所有子节点设置监听器，否则子节点变化时无法知道是哪个节点的变化。
        if (children != null) {
            for (String node : children) {
                String fullPath = parentPath + "/" + node;
                watchNode(fullPath);
            }
        }
    }

    private CuratorWatcher getChildWatcher(String parentPath) {
        if (CHILDREN_WATCHER_MAP.get(parentPath) == null) {
            CHILDREN_WATCHER_MAP.putIfAbsent(parentPath, new NewNodeWatcher(client, this, parentPath, true));
        }
        return CHILDREN_WATCHER_MAP.get(parentPath);
    }

    // 触发客户端在该节点上的注册事件
    public void processListener(EventType type, String path) throws ZookeeperException {

        // 链接断开直接返回
        if (!Operation4internal.isConnect(client)) {
            logger.warn("【zookeeper】client has been closed when processListener!");
            return;
        }

        if (type == EventType.NodeCreated || type == EventType.NodeDataChanged || type == EventType.NodeDeleted) {

            // 转换类型
            EnumChangedEvent parentType = null;
            EnumChangedEvent currentType = null;
            switch (type) {
            case NodeCreated:
                currentType = EnumChangedEvent.ADDED;
                parentType = EnumChangedEvent.CHILD_ADDED;
                break;
            case NodeDataChanged:
                currentType = EnumChangedEvent.UPDATED;
                parentType = EnumChangedEvent.CHILD_UPDATED;
                break;
            case NodeDeleted:
                currentType = EnumChangedEvent.REMOVED;
                parentType = EnumChangedEvent.CHILD_REMOVED;
                break;
            default:
                break;
            }

            // path路径父节点事件类型
            final EnumChangedEvent finalParentType = parentType;
            // path路径事件类型
            final EnumChangedEvent finalCurrentType = currentType;

            // 执行监听事件
            executeNodeListener(path, finalCurrentType);

            PathAndNode pathAndNode = ZKPaths.getPathAndNode(path);
            // path的父节点路径
            String parentPath = pathAndNode.getPath();
            // path的节点名
            final String currentnode = pathAndNode.getNode();

            // path的父节点的子节点列表
            List<String> oldSubNodes = getSubNodesCache(parentPath);

            if (parentType == EnumChangedEvent.CHILD_REMOVED && oldSubNodes != null) {
                // 子节点缓存刷新，保证后续的childChanged事件里拿到的子节点是最新的
                oldSubNodes.remove(currentnode);
                setSubNodesCache(parentPath, oldSubNodes);
            }
            // CHILD_ADD交由下面的else if去做
            if (parentType != EnumChangedEvent.CHILD_ADDED) {
                // 执行子节点监听事件
                executeSubNodesListener(parentPath, currentnode, finalParentType);
            }

        } else if (type == Event.EventType.NodeChildrenChanged) {
            // 其他服务的线程新添加的子节点，无法通过上面3种事件来响应请求
            // 获取path路径的所有旧的子节点
            List<String> oldSubNodes = getSubNodesCache(path);
            // 获取最新的子节点
            List<String> newSubNodes = Collections.emptyList();
            Stat stat = new Stat();
            try {
                newSubNodes = client.getZookeeperClient().getZooKeeper().getChildren(path, false, stat);
            } catch (Exception e) {
                logger.warn("【zookeeper】process child change error,path=" + path, e);
            }

            Stat oldStat = CHILDREN_WATCHER_STAT_MAP.get(path);
            if (oldStat != null && stat.getCversion() == oldStat.getCversion()) {
                // 子节点版本和上次相等再尝试一次
                try {
                    // TODO 采用休眠的方式不合理，以后改进
                    TimeUnit.MILLISECONDS.sleep(50);
                    newSubNodes = client.getZookeeperClient().getZooKeeper().getChildren(path, false, stat);
                } catch (Exception e) {
                    logger.warn("【zookeeper】process child change error,path=" + path, e);
                }
            }
            // 更新子节点缓存
            setSubNodesCache(path, newSubNodes);
            CHILDREN_WATCHER_STAT_MAP.put(path, stat);

            for (String node : newSubNodes) {
                String fullPath = path + "/" + node;
                // 为本节点注册监听
                watchNode(fullPath);
                // 获取子节点的状态
                Stat subNodeStat = new Stat();
                byte[] subNodeData = null;
                try {
                    subNodeData = client.getZookeeperClient().getZooKeeper().getData(fullPath, false, subNodeStat);
                } catch (Exception e) {
                    // 子节点被并发删除，此时不响应任何子节点增加事件
                    logger.warn("【zookeeper】子节点状态获取失败. node=" + fullPath + ", " + e.getMessage());
                }
                if (subNodeData == null) {
                    // 没取到值，可能被删除
                    continue;
                }
                // 判断当前节点是否是新增节点
                if (oldStat != null) {
                    // 该节点的创建zxid比父节点的Pzxid小，是旧节点跳过
                    if (subNodeStat.getCzxid() <= oldStat.getPzxid()) {
                        continue;
                    }
                } else {
                    // 子节点的改变也通过子节点的监听器来实现，这里只需要给其他服务的线程新添加的子节点设置监听器即可
                    if (oldSubNodes != null && oldSubNodes.contains(node)) {
                        continue;
                    }
                }
                // 这里只需做add事件，其余事件交由上面去做

                final String addedNode = node;
                // 执行子节点监听事件
                executeSubNodesListener(path, addedNode, EnumChangedEvent.CHILD_ADDED);
            }
        }
    }

    // 重连后节点变更，触发客户端在该节点上的注册事件
    public void reconnectInvoke4node(String path) throws ZookeeperException {
        // 判断重连前后节点状态变化
        Boolean oldExist = getNodeExistCache(path);
        Boolean newExist = Operation4internal.exist(client, path);
        if (oldExist != null) {
            // 断开网络之前做过缓存
            if (oldExist && !newExist) {
                // 断网之前存在，断网之后不存在。节点被删除
                logger.warn("【zookeeper】reconnect invoke. path=" + path + ", event=NodeREMOVED");
                executeNodeListener(path, EnumChangedEvent.REMOVED);
            }
            if (!oldExist && newExist) {
                // 断网之前不存在，断网之后存在。节点被新增
                logger.warn("【zookeeper】reconnect invoke. path=" + path + ", event=NodeADDED");
                executeNodeListener(path, EnumChangedEvent.ADDED);
            }
        } else {
            // 有监听在，但无缓存.无法判断断开之前节点是否存在。不做推送
            logger.warn("【zookeeper】reconnect invoke. path=" + path + ", 无法获取断网前节点是否存在，不做推送");
        }
        if (newExist) {
            // 节点存在
            // 判断重连前后节点值状态变化
            String oldValue = getNodeCache(path);
            String newValue = null;
            try {
                newValue = Operation4internal.getValue(client, path);
                // 修改缓存
                setNodeExistCache(path, true);
                setNodeCache(path, newValue);
            } catch (Exception e) {
                // 节点被并发删除 忽略
                removeNodeCache(path);
                removeNodeExistCache(path);
                removeSubNodesCache(path);
                logger.warn("【zookeeper】reconnect invoke error. path=" + path, e);
                return;
            }
            if (oldValue != null) {
                if (!oldValue.equals(newValue)) {
                    // 节点值发生改变
                    logger.warn("【zookeeper】reconnect invoke. path=" + path + ", event=NodeDataChanged");
                    executeNodeListener(path, EnumChangedEvent.UPDATED);
                }
            } else {
                // 有监听在，但无缓存
                logger.warn("【zookeeper】reconnect invoke. path=" + path + ", 无法获取断网前节点值，不做推送");
            }

        } else {
            // 节点被删除
            removeNodeCache(path);
            setNodeExistCache(path, false);
            removeSubNodesCache(path);
        }
    }

    // 执行节点监听事件
    private void executeNodeListener(String path, EnumChangedEvent event) {
        List<NewNodeListener> singleNodeListenerList = null;
        List<NewNodeListener> nodeListenerList = null;
        if (NODE_EVENT_SINGLE_MAP.get(path) != null) {
            singleNodeListenerList = Lists.newArrayList(NODE_EVENT_SINGLE_MAP.get(path));
        }
        if (NODE_EVENT_MAP.get(path) != null) {
            nodeListenerList = Lists.newArrayList(NODE_EVENT_MAP.get(path));
        }
        if (singleNodeListenerList != null) {
            synchronized (singleNodeListenerList) {
                for (final NewNodeListener listener : singleNodeListenerList) {
                    EVENT_TASK_SINGLE_POOL.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                listener.nodeChanged(zookeeperClient, event);
                            } catch (Exception e) {
                                logger.error("【zookeeper】listener execute failure. path=" + listener.listeningPath(),
                                        e);
                            }
                        }
                    });
                }
            }
        }
        if (nodeListenerList != null) {
            synchronized (nodeListenerList) {
                for (final NewNodeListener listener : nodeListenerList) {
                    EVENT_TASK_POOL.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                listener.nodeChanged(zookeeperClient, event);
                            } catch (Exception e) {
                                logger.error("【zookeeper】listener execute failure. path=" + listener.listeningPath(),
                                        e);
                            }
                        }
                    });
                }
            }
        }
    }

    // 执行子节点监听事件
    private void executeSubNodesListener(String path, String node, EnumChangedEvent event) {
        List<NewNodeChildListener> singleChildListenerList = null;
        List<NewNodeChildListener> childListenerList = null;
        if (CHILD_EVENT_SINGLE_MAP.get(path) != null) {
            singleChildListenerList = CHILD_EVENT_SINGLE_MAP.get(path);
        }
        if (CHILD_EVENT_MAP.get(path) != null) {
            childListenerList = CHILD_EVENT_MAP.get(path);
        }

        if (singleChildListenerList != null) {
            synchronized (singleChildListenerList) {
                for (final NewNodeChildListener listener : singleChildListenerList) {
                    EVENT_TASK_SINGLE_POOL.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                listener.childChanged(zookeeperClient, node, event);
                            } catch (Exception e) {
                                logger.error("【zookeeper】listener execute failure. path=" + listener.listeningPath(),
                                        e);
                            }
                        }
                    });
                }
            }
        }
        if (childListenerList != null) {
            synchronized (childListenerList) {
                for (final NewNodeChildListener listener : childListenerList) {
                    EVENT_TASK_POOL.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                listener.childChanged(zookeeperClient, node, event);
                            } catch (Exception e) {
                                logger.error("【zookeeper】listener execute failure. path=" + listener.listeningPath(),
                                        e);
                            }
                        }
                    });
                }
            }
        }
    }

    // 重连后子节点变更，触发客户端在该节点上的注册事件
    public void reconnectInvoke4subnodes(String path, List<String> oldNodes, List<String> newNodes) throws Exception {
        List<NewNodeChildListener> singleChildListenerList = CHILD_EVENT_SINGLE_MAP.get(path);
        List<NewNodeChildListener> childListenerList = CHILD_EVENT_MAP.get(path);
        if ((singleChildListenerList == null || singleChildListenerList.isEmpty())
                && (childListenerList == null || childListenerList.isEmpty())) {
            return;
        }

        for (String node : newNodes) {
            if (oldNodes != null && oldNodes.remove(node)) {
                // 连接恢复后依然存在的子节点
                String fullPath = path + "/" + node;
                String oldValue = getNodeCache(fullPath);
                try {
                    String newValue = Operation4internal.getValue(client, fullPath);
                    setNodeExistCache(path, true);
                    if (oldValue != null && !oldValue.equals(newValue)) {
                        setNodeCache(fullPath, newValue);
                        executeSubNodesListener(path, node, EnumChangedEvent.CHILD_UPDATED);
                    }
                } catch (Exception e) {
                    removeNodeCache(path);
                    removeNodeExistCache(path);
                    removeSubNodesCache(path);
                }
                continue;
            }
            // 连接断开期间新加节点
            logger.warn("【zookeeper】reconnect invoke. path=" + path + ", event=CHILD_ADDED, node=" + node);
            executeSubNodesListener(path, node, EnumChangedEvent.CHILD_ADDED);
        }

        if (oldNodes == null) {
            return;
        }

        for (String node : oldNodes) {
            // 连接断开期间删除节点
            logger.warn("【zookeeper】reconnect invoke. path=" + path + ", event=CHILD_REMOVED, node=" + node);
            executeSubNodesListener(path, node, EnumChangedEvent.CHILD_REMOVED);
        }

    }

    // 把监听事件存放到Map，并初始化监听器Watch。
    public void initListener(Listener listener, boolean single) throws ZookeeperException {

        // 节点监听事件
        if (listener instanceof NewNodeListener) {
            init4NodeListener(listener, single);
        } else if (listener instanceof NewNodeChildListener) {
            init4SubNodesListener(listener, single);
        }
    }

    private String checkPath(String path) throws ZookeeperException {
        // 路径为空，忽略该监听器
        if (PathUtils.isEmptyPath(path)) {
            throw new ZookeeperException("【zookeeper】listening path is not right. path=" + path);
        }
        // 监听路径去掉路径最后的'/'
        path = PathUtils.removeLastSlash(path);
        // 验证路径有效，忽略无效的路径
        try {
        	PathUtils.checkNodePath(path);
        } catch (IllegalArgumentException iae) {
            throw new ZookeeperException("【zookeeper】listening path is not right. path=" + path, iae);
        }
        return path;
    }

    private void init4NodeListener(Listener listener, boolean single) throws ZookeeperException {

        String path = checkPath(listener.listeningPath());

        List<NewNodeListener> nodeEventList = null;
        if (single) {
            nodeEventList = NODE_EVENT_SINGLE_MAP.get(path);
            if (nodeEventList == null) {
                NODE_EVENT_SINGLE_MAP.putIfAbsent(path, new ArrayList<NewNodeListener>());
                nodeEventList = NODE_EVENT_SINGLE_MAP.get(path);
            }
        } else {
            nodeEventList = NODE_EVENT_MAP.get(path);
            if (nodeEventList == null) {
                NODE_EVENT_MAP.putIfAbsent(path, new ArrayList<NewNodeListener>());
                nodeEventList = NODE_EVENT_MAP.get(path);
            }
        }

        // 如果之前沒有在这个节点上设置过监听器，则注册watcher
        if (nodeEventList.isEmpty()) {
            try {
                // 注册节点监听器(为该节点生成一个监听器实例)
                watchNode(path);
            } catch (Exception e) {
                throw new ZookeeperException("【zookeeper】watching path failure. path=" + path, e);
            }
        }

        // 把监听事件保存到NODE_EVENT_MAP
        synchronized (nodeEventList) {
            nodeEventList.add((NewNodeListener) listener);
        }
        try {
            // 用户只要监听该节点就需要加入缓存
            boolean isExist = Operation4internal.exist(client, path);
            if (getNodeExistCache(path) == null) {
                setNodeExistCache(path, isExist);
            }

            if (getNodeCache(path) == null && isExist) {
                String value = Operation4internal.getValue(client, path);
                setNodeCache(path, value);
            }

        } catch (Exception e) {
            // 设置缓存失败，忽略
        }
    }

    private void init4SubNodesListener(Listener listener, boolean single) throws ZookeeperException {

        String path = checkPath(listener.listeningPath());
        // 无法为不存在的节点监听子节点
        if (!Operation4internal.exist(client, path)) {
            throw new ZookeeperException("【zookeeper】node is not exist,path=" + path);
        }

        List<NewNodeChildListener> childEventList = null;
        if (single) {
            childEventList = CHILD_EVENT_SINGLE_MAP.get(path);
            if (childEventList == null) {
                CHILD_EVENT_SINGLE_MAP.putIfAbsent(path, new ArrayList<NewNodeChildListener>());
                childEventList = CHILD_EVENT_SINGLE_MAP.get(path);
            }
        } else {
            childEventList = CHILD_EVENT_MAP.get(path);
            if (childEventList == null) {
                CHILD_EVENT_MAP.putIfAbsent(path, new ArrayList<NewNodeChildListener>());
                childEventList = CHILD_EVENT_MAP.get(path);
            }
        }
        // 如果之前沒有在这个节点上设置过监听器，则注册watcher
        if (childEventList.isEmpty()) {
            try {
                // 注册子节点监听器(为该节点生成一个监听器实例，在getChild()方法上注册监听器)
                watchChild(path);
            } catch (Exception e) {
                throw new ZookeeperException("【zookeeper】watching children path failure. path=" + path, e);
            }
        }

        // 把监听事件保存到CHILD_EVENT_MAP
        synchronized (childEventList) {
            childEventList.add((NewNodeChildListener) listener);
        }

        // 用户只要监听该节点就需要加入缓存
        if (getSubNodesCache(path) == null) {
            try {
                List<String> subNodes = Operation4internal.getSubNodes(client, path);
                setSubNodesCache(path, subNodes);
            } catch (Exception e) {
                // 设置子节点缓存失败，应该打印日志，不应忽略。子节点变化通知依赖缓存
                logger.error("【zookeeper】get subNodes error,path=" + path, e);
            }
        }
    }

    // 手动去掉监听器。
    public void removeListener(Listener listener) throws ZookeeperException {

        if (listener instanceof NewNodeListener) {
            remove4NodeListener(listener);
        } else if (listener instanceof NewNodeChildListener) {
            remove4SubNodesListener(listener);
        }
    }

    private void remove4NodeListener(Listener listener) throws ZookeeperException {
        String path = checkPath(listener.listeningPath());

        List<NewNodeListener> nodeEventList = NODE_EVENT_MAP.get(path);
        if (nodeEventList != null) {
            // 删除节点监听事件
            synchronized (nodeEventList) {
                nodeEventList.remove(listener);
            }
        }

        List<NewNodeListener> singleNodeEventList = NODE_EVENT_SINGLE_MAP.get(path);
        if (singleNodeEventList != null) {
            synchronized (singleNodeEventList) {
                singleNodeEventList.remove(listener);
            }
        }

        if ((nodeEventList == null || nodeEventList.isEmpty())
                && (singleNodeEventList == null || singleNodeEventList.isEmpty())) {
            // 该节点已无监听 删除监听器和清空缓存

            // 删除注册的watcher
            @SuppressWarnings("unused")
            CuratorWatcher watcher = removeNodeWatcher(path);
            watcher = null;
            // 清空缓存
            removeNodeExistCache(path);
            removeNodeCache(path);
        }
    }

    private void remove4SubNodesListener(Listener listener) throws ZookeeperException {
        String path = checkPath(listener.listeningPath());
        List<NewNodeChildListener> childEventList = CHILD_EVENT_MAP.get(path);
        if (childEventList != null) {
            // 删除子节点监听事件
            synchronized (childEventList) {
                childEventList.remove(listener);
            }
        }

        List<NewNodeChildListener> singleChildEventList = CHILD_EVENT_SINGLE_MAP.get(path);
        if (singleChildEventList != null) {
            synchronized (singleChildEventList) {
                singleChildEventList.remove(listener);
            }
        }

        if ((childEventList == null || childEventList.isEmpty())
                && (singleChildEventList == null || singleChildEventList.isEmpty())) {
            // 该节点已无监听 删除监听器和清空缓存
            // 删除注册的watcher
            @SuppressWarnings("unused")
            CuratorWatcher watcher = removeChildWatcher(path);
            watcher = null;
            // 清空子节点缓存
            removeSubNodesCache(path);
        }
    }

    // 删除子节点监听器
    public CuratorWatcher removeChildWatcher(String path) {
        CHILDREN_WATCHER_STAT_MAP.remove(path);
        CuratorWatcher watcher = CHILDREN_WATCHER_MAP.remove(path);
        if (watcher != null && watcher instanceof NewNodeWatcher) {
            ((NewNodeWatcher) watcher).cancellWatcher();
        }
        return watcher;
    }

    // 删除节点监听器
    public CuratorWatcher removeNodeWatcher(String path) {
        CuratorWatcher watcher = NODE_WATCHER_MAP.remove(path);
        if (watcher != null && watcher instanceof NewNodeWatcher) {
            ((NewNodeWatcher) watcher).cancellWatcher();
        }
        return watcher;
    }

    // 获取所有客户端定义监听节点事件
    public ConcurrentHashMap<String, List<NewNodeListener>> getNodeEventMap() {
        return NODE_EVENT_MAP;
    }

    // 获取所有客户端定义监听节点事件
    public ConcurrentHashMap<String, List<NewNodeListener>> getNodeEventSingleMap() {
        return NODE_EVENT_SINGLE_MAP;
    }

}
