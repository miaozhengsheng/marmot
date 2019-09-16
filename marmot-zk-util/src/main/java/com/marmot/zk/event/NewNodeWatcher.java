package com.marmot.zk.event;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.log4j.Logger;
import org.apache.zookeeper.WatchedEvent;

import com.marmot.zk.listener.NewNodeListener;
import com.marmot.zk.listener.Operation4internal;

public class NewNodeWatcher implements CuratorWatcher {

    // 日志
    private static final Logger logger = Logger.getLogger(NewNodeWatcher.class);

    // 客户端链接
    private CuratorFramework client;
    // 监听路径
    private String path;
    // 是否是子节点监听
    private boolean isChildWatcher = false;
    // watcher是否被注销
    private AtomicBoolean isCancell = new AtomicBoolean(false);
    // 事件管理类
    private NewNodeEventManager manager;

    // 节点监听器
    public NewNodeWatcher(CuratorFramework client, NewNodeEventManager manager, String path) {
        this.client = client;
        this.path = path;
        this.manager = manager;
    }

    // 子节点监听器
    public NewNodeWatcher(CuratorFramework client, NewNodeEventManager manager, String path, boolean isChildWatcher) {
        this.client = client;
        this.path = path;
        this.isChildWatcher = isChildWatcher;
        this.manager = manager;
    }

    // 注销监听器
    public void cancellWatcher() {
        isCancell.set(true);
    }

    @Override
    public void process(WatchedEvent event) throws Exception {
        if (isCancell.get()) {
            // 当前watcher已经被注销
            logger.warn("【zookeeper】watcher has been cancell when processListener，path=" + path);
            return;
        }
        if (!Operation4internal.isConnect(client)) {
            // 当前连接已经被关闭
            logger.warn("【zookeeper】client has been closed when processListener，path=" + path);
            return;
        }

        try {
            // zookeeper节点变更通知
            switch (event.getType()) {
            case NodeCreated:
            case NodeDataChanged:
                // TODO 当监听事件未被重新注册，但是节点又发生改变时，会发生事件丢失
                // 重新注册监听事件
                Operation4internal.watchNode(client, this, path);

                try {
                    // TODO 避免监听事件响应了，却获取不到最新的数据来同步缓存数据
                    // TODO 休眠显然不是合理的方式，待改进
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    // 忽略
                }

                String nodeData = StringUtils.EMPTY;
                try {
                    // 获取节点内容，并将其更新到缓存
                    nodeData = Operation4internal.getValue(client, path);
                    manager.setNodeCache(path, nodeData);
                    manager.setNodeExistCache(path, true);
                } catch (Exception e) {
                    // 发生异常，清空缓存
                    manager.removeNodeCache(path);
                    manager.removeNodeExistCache(path);
                    logger.warn("【zookeeper】处理监听事件更新缓存时发生错误", e);
                }

                // 调用客户端在该节点上的注册事件
                manager.processListener(event.getType(), path);
                break;
            case NodeDeleted:

                // 清空缓存Exist
                manager.removeNodeExistCache(path);

                // 清空缓存的子节点列表值
                manager.removeSubNodesCache(path);

                // 清空缓存的节点内容
                manager.removeNodeCache(path);

                // 获取业务监听器，判断当前是否还有监听
                List<NewNodeListener> nodeEventList = manager.getNodeEventMap().get(path);
                List<NewNodeListener> singleNodeEventList = manager.getNodeEventSingleMap().get(path);
                if (!CollectionUtils.isEmpty(nodeEventList) || !CollectionUtils.isEmpty(singleNodeEventList)) {
                    // 只要还有监听器，就重新注册监听
                    Operation4internal.watchNode(client, this, path);
                } else {
                    // 没有监听器，清空watcher缓存
                    manager.removeNodeWatcher(path);
                }

                // 监听不存在节点的子节点会抛错，所以需要清空子节点监听器
                manager.removeChildWatcher(path);

                if (!isChildWatcher) {
                    // 若非子节点监听，则调用客户端在该节点上的注册事件
                    manager.processListener(event.getType(), path);
                }
                break;

            case NodeChildrenChanged:
                if (Operation4internal.exist(client, path)) {
                    // 父节点存在，重新注册子节点监听事件
                    Operation4internal.watchChildNode(client, this, path);
                } else {
                    // 父节点不存在，清空子节点缓存返回
                    manager.removeSubNodesCache(path);
                    manager.removeChildWatcher(path);
                    return;
                }
                // 调用客户端在该节点上的注册事件
                manager.processListener(event.getType(), path);
                break;
            case None:
                break;
            }
        } catch (Exception e) {
            // 已经shutdown
            if (!Operation4internal.isConnect(client)) {
                logger.error("【zookeeper】zkclient shutdown. ignore Exception");
            } else {
                logger.error("【zookeeper】watcher.process() failure. " + event.toString(), e);
            }
        }

    }
}

