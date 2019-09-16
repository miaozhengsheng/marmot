package com.marmot.zk.listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;

import com.marmot.zk.client.impl.ZKClientImpl;
import com.marmot.zk.event.NewNodeEventManager;

public class ZookeeperStateListener implements ConnectionStateListener{

	private ZKClientImpl zookeeperClientImpl;

    private boolean isRegistToZk;

    public ZookeeperStateListener(ZKClientImpl zookeeperClientImpl, boolean isRegistToZk) {
        this.zookeeperClientImpl = zookeeperClientImpl;
        this.isRegistToZk = isRegistToZk;
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        // 连接断开时，延迟设置连接状态，绝大部分情况能快速连上。
        System.out.println("【zookeeper】client's state change:" + newState);

        // 此处不监听LOST事件，是因为真正发生丢失时，客户端根本没法收到任何通知；
        // 在丢失连接后重新获取到连接的话，会连续收到LOST和RECONNECTED事件。
        if (newState == ConnectionState.SUSPENDED) {
            try {
                // 防止web容器已经被注销，避免僵尸进程
                Class.forName("org.apache.zookeeper.proto.SetWatches");
                // TODO sleep两个连接超时时间(默认20秒)
                TimeUnit.MILLISECONDS.sleep(client.getZookeeperClient().getConnectionTimeoutMs() * 2);
            } catch (ClassNotFoundException e) {
            	System.out.println("【zookeeper】web application instance has been stopped already. ");
                Operation4internal.destroy(zookeeperClientImpl.getNodeEventManager(), client);
            } catch (InterruptedException ie) {
                // do nothing
            }

            // 在等2次连接超时时间后还未连上，才设置状态为断开。
            if (!client.getZookeeperClient().isConnected()) {
            	System.out.println("【zookeeper】zk disconnected. " + newState);
            }
        }

        if (newState == ConnectionState.CONNECTED || newState == ConnectionState.RECONNECTED) {
            System.out.println("【zookeeper】zookeeper reconnected.");

            // 异步写入临时节点，给闪电配置系统监控用，把sessionId和项目对应上
            if (isRegistToZk) {
                Operation4internal.registToZk(client);
            }
            NewNodeEventManager nodeEventManager = zookeeperClientImpl.getNodeEventManager();

            // 重新注册子节点监听器，更新缓存
            for (Map.Entry<String, CuratorWatcher> nodeWatcher : nodeEventManager.getChildrenWatcherMap().entrySet()) {
                String path = nodeWatcher.getKey();
                try {
                    List<String> previousNodes = nodeEventManager.getSubNodesCache(path);
                    List<String> childrenNodes = Operation4internal.watchChildNode(client, nodeWatcher.getValue(), path);
                    nodeEventManager.setSubNodesCache(path, childrenNodes);
                    if (childrenNodes == null) {
                        childrenNodes = Collections.emptyList();
                    } else {
                        for (String child : childrenNodes) {
                            nodeEventManager.watchNode(path + "/" + child);
                        }
                    }
                    List<String> oldNodes = null;
                    if (previousNodes != null) {
                        // 操作副本list
                        oldNodes = new ArrayList<>(previousNodes);
                    }
                    // 通知业务注册监听器（断开连接期间子节点变更）
                    nodeEventManager.reconnectInvoke4subnodes(path, oldNodes, childrenNodes);
                } catch (Exception e) {
                    nodeEventManager.removeSubNodesCache(path);
                    System.out.println("【zookeeper】Trying to reset children watcher after reconnection.");
                }
            }

            // 重新注册节点监听器，更新缓存
            for (Map.Entry<String, CuratorWatcher> nodeWatcher : nodeEventManager.getNodeWatcherMap().entrySet()) {
                try {
                    String path = nodeWatcher.getKey();
                    // 监听事件触发
                    nodeEventManager.reconnectInvoke4node(path);
                    Operation4internal.watchNode(client, nodeWatcher.getValue(), path);
                } catch (Exception e) {
                	System.out.println("【zookeeper】Trying to reset watcher after reconnection.");
                }
            }
        }
        // call back listener of biz
        for (ConnectionStateListener listener : zookeeperClientImpl.getConnectionListener()) {
            listener.stateChanged(client, newState);
        }
    }

}
