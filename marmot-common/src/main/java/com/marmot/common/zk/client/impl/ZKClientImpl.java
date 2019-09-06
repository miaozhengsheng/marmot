package com.marmot.common.zk.client.impl;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.springframework.util.StringUtils;

import com.marmot.common.zk.EnumZKNameSpace;
import com.marmot.common.zk.ZKUtil;
import com.marmot.common.zk.client.IZKClient;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.framework.CuratorFrameworkFactory.Builder;
import com.netflix.curator.framework.state.ConnectionState;
import com.netflix.curator.framework.state.ConnectionStateListener;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.curator.utils.ZKPaths;
import com.netflix.curator.utils.ZKPaths.PathAndNode;

public class ZKClientImpl implements IZKClient {

	private static final Logger logger = Logger.getLogger(ZKClientImpl.class);

	private static final String ZK_CONNECT_ADDRESS = "192.168.117.130:2181,192.168.117.131:2181,192.168.117.132:2181";
	/**
	 * CuratorFramework client
	 */
	private CuratorFramework client;

	private static final int connectionTimeoutMs = 50000;

	/**
	 * 连接状态(连接正常：true; 断开：false)
	 */
	private final AtomicBoolean connectStatus = new AtomicBoolean(false);

	private static final IZKClient instance = new ZKClientImpl();

	public static IZKClient getInstance() {
		return instance;
	}

	/**
	 * 系统默认需要acl使用
	 */
	private ZKClientImpl() {
		// 该构造方法关联client用户，拥有zk集群的所有权限。但是需要通过读写鉴权
		// 客户端使用、需要ACL
		this(ZK_CONNECT_ADDRESS, "", "", true);
		/*-
		if (authorityCheck == null) {
		    authorityCheck = new AuthorityCheck(getCuratorFramework());
		}
		 */
	}

	// 自定义zookeeper集群使用
	private ZKClientImpl(String zkConnectString, final String userName,
			final String password, Boolean aclFlag) {

		if (StringUtils.isEmpty(zkConnectString)) {
			throw new IllegalArgumentException("zkConnectString is empty");
		}

		Builder builder = CuratorFrameworkFactory
				.builder()
				.connectString(zkConnectString)
				// .sessionTimeoutMs(connectionTimeoutMs)
				.retryPolicy(
						new ExponentialBackoffRetry(
								(int) (Math.random() * 500) + 1000, 3))
				.connectionTimeoutMs(connectionTimeoutMs);

		client = builder.build();
		// 启动
		client.start();
		// this.nodeEventManager = new NodeEventManager(this);
		// 等待与zk服务器连接建立完成后才返回实例
		if (waitUntilZkStart(zkConnectString)) {
			/*- 注册zk连接状态监听器
			// client.getConnectionStateListenable().addListener(connectionStateListener);
			// 初始化节点最大层级及节点最大长度的全局配置（节点最大长度和节点最深层级）
			String checkRuleJson = getStringDataFromZkNoAuth(PathUtil.joinPath(EnumNamespace.PROJECT,
			        ZkClientConst.ZK_CONF_ROOT));
			PathUtil.initCheckPattern(checkRuleJson);
			putCacheAndSetWatch(PathUtil.joinPath(EnumNamespace.PROJECT, ZkClientConst.ZK_CONF_ROOT), checkRuleJson);
			// 异步写入临时节点，给闪电配置系统监控用，把IP和项目名对应
			registToZk();
			 */
		}
	}

	@Override
	public void createNode(EnumZKNameSpace space, String path) {
		addNode(space, path, CreateMode.PERSISTENT, "");
	}

	@Override
	public boolean setTempNodeData(EnumZKNameSpace namespace, String path) {
		return addNode(namespace, path, CreateMode.EPHEMERAL, "");
	}

	@Override
	public boolean deleteTempNode(EnumZKNameSpace namespace, String path) {
		String fullPath = ZKUtil.joinPath(namespace, path);
		// 验证节点名字合法性及验证节点层级数和节点最大长度（不能超过设置的最大层级数）
		// PathUtil.checkNodePath(fullPath);
		// 检查zk连接可用
		if (!connectStatus.get()) {
			logger.warn("ZooKeeper client connect invalid.");
			return false;
		}
		Stat stat = getStat(fullPath);
		// 是临时节点才能删除
		if (stat != null && stat.getEphemeralOwner() > 0) {
			return deleteNode(fullPath);
		}
		return false;
	}
	
	/**
     * 删除zk节点
     * 
     * @param path 删除节点的路径
     * 
     */
    private boolean deleteNode(String fullPath) {
        boolean result = false;
        try {
            // 写入log用
            client.delete().forPath(fullPath);
            // 删除成功返回true
            result = true;
            // logger.info(PathUtil.getCurrentClientId() + " delete node (" +
            // fullPath + ")");
        } catch (KeeperException.NoNodeException nee) {
            // 节点不存在返回true
            result = true;
            logger.warn("delete node failure, node don't exists. path=" + fullPath);
        } catch (KeeperException.NotEmptyException nee) {
            logger.warn("delete node failure, has children nodes. path=" + fullPath);
        } catch (Exception e) {
            logger.error("delete node failure. path=" + fullPath, e);
        }
        return result;
    }
	
	   @Override
	    public Stat getStat(String fullPath) {
	        try {
	            return client.checkExists().forPath(fullPath);
	        } catch (Exception e) {
	            logger.error("get Stat failed,path=" + fullPath, e);
	        }
	        return null;
	    }

	private boolean addNode(EnumZKNameSpace namespace, String path,
			CreateMode mode, String data) {

		// path is empty, return false
		if (StringUtils.isEmpty(path)) {
			return false;
		}
		String fullPath = ZKUtil.joinPath(namespace, path);
		boolean result = false;
		try {

			// TODO 客户端创建节点 直接继承
			// 拉取父节点acls getAcl /fullPath
			PathAndNode pathAndNode = ZKPaths.getPathAndNode(fullPath);
			// 循环判断父节点是否存在
			/*-
			while (!exists(null, pathAndNode.getPath())) {
			    pathAndNode = ZKPaths.getPathAndNode(pathAndNode.getPath());
			    if (pathAndNode.getPath().equals(namespace.getNamespace())) {
			        throw new RuntimeException("create node error,can not find acls from father");
			    }
			}
			 */
			String[] nodes = fullPath.replaceFirst(pathAndNode.getPath(), "")
					.split("/");
			String parentPath = ZKUtil.removeLastSlash(pathAndNode.getPath());
			for (String node : nodes) {
				if (StringUtils.isEmpty(node)) {
					continue;
				}
				String sonPath = parentPath + "/" + node;
				client.create().creatingParentsIfNeeded().withMode(mode)
						.forPath(sonPath, "{}".getBytes());
				parentPath = sonPath;
			}

			result = true;
			// logger.info(PathUtil.getCurrentClientId() + " create " + mode +
			// " node (" + fullPath + ")");
			// 设置缓存及监听器
			// putCacheAndSetWatch(fullPath, jsonData);
		} catch (KeeperException.NodeExistsException e) {
			logger.warn("Add node failure, this node exists. path=" + fullPath);
		} catch (KeeperException.NoChildrenForEphemeralsException e) {
			logger.warn("Add node failure, parent node can not be temp node. path="
					+ fullPath);
		} catch (Exception e) {
			logger.warn("Add node failure. path=" + fullPath, e);
		}
		return result;
	}

	// 判断指定路径是否存在
	@Override
	public boolean exists(EnumZKNameSpace namespace, String path) {
		String fullPath = ZKUtil.joinPath(namespace, path);
		// 验证节点名字合法性及验证节点层级数和节点最大长度（不能超过设置的最大层级数）
		// PathUtil.checkNodePath(fullPath);
		// 检查zk连接可用
		if (!connectStatus.get()) {
			logger.warn("ZooKeeper client connect invalid.");
			return false;
		}
		return checkExist(fullPath);
	}

	private boolean checkExist(String path) {
		try {
			return client.checkExists().forPath(path) != null;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("check exist error. path=" + path, e);
			return false;
		}
	}

	private boolean waitUntilZkStart(String zkConnectString) {
		final CountDownLatch latch = new CountDownLatch(1);
		client.getConnectionStateListenable().addListener(
				new ConnectionStateListener() {
					@Override
					public void stateChanged(CuratorFramework client,
							ConnectionState newState) {
						if (newState == ConnectionState.CONNECTED) {
							latch.countDown();
							// zkclient实例第一次生成时使用，连接建立后销毁
							client.getConnectionStateListenable()
									.removeListener(this);
						}
					}
				});
		boolean connectedResult = false;
		try {
			connectedResult = latch.await(connectionTimeoutMs,
					TimeUnit.MILLISECONDS);
			// 超过5秒还未能与zk建立连接，抛出例外
			if (!connectedResult
					&& !client.getZookeeperClient().getZooKeeper().getState()
							.isConnected()) {
				logger.error("start zk latch.await() overtime. zkConnectString="
						+ zkConnectString);
				throw new RuntimeException(
						"ZooKeeper client connecting overtime...");
			}

			System.err.println("connected......");
		} catch (InterruptedException e) {
			 e.printStackTrace();
			logger.error("start zk latch.await() error. zkConnectString="
					+ zkConnectString, e);
			Thread.currentThread().interrupt();
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("getZooKeeper() error. zkConnectString="
					+ zkConnectString, e);
			return false;
		}
		connectStatus.compareAndSet(false, true);
		return true;
	}

	@Override
	public boolean deleteNormalNode(EnumZKNameSpace namespace, String path) {
		
		String fullPath = ZKUtil.joinPath(namespace, path);
		// 验证节点名字合法性及验证节点层级数和节点最大长度（不能超过设置的最大层级数）
		// PathUtil.checkNodePath(fullPath);
		// 检查zk连接可用
		if (!connectStatus.get()) {
			logger.warn("ZooKeeper client connect invalid.");
			return false;
		}

		return deleteNode(fullPath);
	}

	@Override
	public List<String> listSubNodes(EnumZKNameSpace nameSpace, String path) throws Exception {
		String fullPath = ZKUtil.joinPath(nameSpace, path);
		List<String> forPath = client.getChildren().forPath(fullPath);
		return forPath;
	}

}
