package com.marmot.zk.client;

import java.util.List;
import java.util.Map;

import org.apache.curator.framework.state.ConnectionStateListener;

import com.marmot.zk.client.exception.ZookeeperException;
import com.marmot.zk.client.leader.NewLeader;
import com.marmot.zk.client.lock.DistributedReadWriteLock;
import com.marmot.zk.client.lock.DistributedReentrantLock;
import com.marmot.zk.enums.EnumMethodAuthType;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.listener.Listener;
import com.marmot.zk.utils.BeforeMethod;


public interface IZKClient {
	


    /**
     * 获取节点的子节点列表，若节点不存在则返回null
     * 
     * @param namespace
     *            EnumNamespace
     * @param path
     *            节点路径
     * @return List
     * @throws ZookeeperException
     *             自定义zk异常 若节点不存在也会抛异常
     */
    @BeforeMethod(authType = EnumMethodAuthType.READ)
    public List<String> getSubNodes(EnumZKNameSpace namespace, String path) throws ZookeeperException;

    /**
     * 获取节点的map值，若节点值不是map类型，则抛错。若节点不存在则返回null
     * 
     * @param namespace
     *            EnumNamespace
     * @param path
     *            节点路径
     * @return Map
     * @throws ZookeeperException
     *             自定义zk异常 若节点不存在也会抛异常
     */
    @BeforeMethod(authType = EnumMethodAuthType.READ)
    public Map<String, Object> getMap(EnumZKNameSpace namespace, String path) throws ZookeeperException;

    /**
     * 获取节点的字符串值，若节点不存在则返回null
     * 
     * @param namespace
     *            EnumNamespace
     * @param path
     *            节点路径
     * @return String
     * @throws ZookeeperException
     *             自定义zk异常 若节点不存在也会抛异常
     */
    @BeforeMethod(authType = EnumMethodAuthType.READ)
    public String getString(EnumZKNameSpace namespace, String path) throws ZookeeperException;

    /**
     * 判断节点是否存在
     * 
     * @param namespace
     *            EnumNamespace
     * @param path
     *            节点路径
     * @return boolean
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod(authType = EnumMethodAuthType.READ)
    public boolean exist(EnumZKNameSpace namespace, String path) throws ZookeeperException;

    /**
     * 创建临时节点（临时节点下不能有任何子节点），若节点已经存在则返回false，添加成功则返回true
     * 
     * @param namespace
     *            EnumNamespace
     * @param path
     *            节点路径
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod(authType = EnumMethodAuthType.WRITE)
    public boolean addTempNode(EnumZKNameSpace namespace, String path) throws ZookeeperException;

    /**
     * 创建永久节点，若节点已经存在则返回false，添加成功则返回true
     * 
     * @param namespace
     *            EnumNamespace
     * @param path
     *            节点路径
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod(authType = EnumMethodAuthType.WRITE)
    public boolean addNode(EnumZKNameSpace namespace, String path) throws ZookeeperException;

    /**
     * 删除节点，若节点不存在则返回false，删除成功则返回true
     * 
     * @param namespace
     *            EnumNamespace
     * @param path
     *            节点路径
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod(authType = EnumMethodAuthType.WRITE)
    public boolean deleteNode(EnumZKNameSpace namespace, String path) throws ZookeeperException;

    /**
     * 为临时节点设置字符串类型的值，若节点不存在则创建节点
     * 
     * @param namespace
     *            EnumNamespace
     * @param path
     *            节点路径
     * @param value
     *            节点值
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod(authType = EnumMethodAuthType.WRITE)
    public void setTempNode4String(EnumZKNameSpace namespace, String path, String value) throws ZookeeperException;

    /**
     * 为临时节点设置map类型的值，若节点不存在则创建节点
     * 
     * @param namespace
     *            EnumNamespace
     * @param path
     *            节点路径
     * @param value
     *            节点值
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod(authType = EnumMethodAuthType.WRITE)
    public void setTempNode4Map(EnumZKNameSpace namespace, String path, Map<String, Object> value)
            throws ZookeeperException;

    /**
     * 为永久节点设置map类型的值，若节点不存在则创建节点
     * 
     * @param namespace
     *            EnumNamespace
     * @param path
     *            节点路径
     * @param value
     *            节点值
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod(authType = EnumMethodAuthType.WRITE)
    public void setNode4Map(EnumZKNameSpace namespace, String path, Map<String, Object> value) throws ZookeeperException;

    /**
     * 为永久节点设置String类型的值，若节点不存在则创建节点
     * 
     * @param namespace
     *            EnumNamespace
     * @param path
     *            节点路径
     * @param value
     *            节点值
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod(authType = EnumMethodAuthType.WRITE)
    public void setNode4String(EnumZKNameSpace namespace, String path, String value) throws ZookeeperException;

    /**
     * 增加监听器
     * 
     * @param listener
     *            监听器
     * @param single
     *            通知是否串型执行
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod(authType = EnumMethodAuthType.READ)
    public void addListener(Listener listener, boolean single) throws ZookeeperException;

    /**
     * 增加监听器（默认不是串型执行）
     * 
     * @param listener
     *            监听器
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod(authType = EnumMethodAuthType.READ)
    public void addListener(Listener listener) throws ZookeeperException;

    /**
     * 增加链接监听
     * 
     * @param listener
     *            监听器
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod()
    public void addConnectionStateListener(ConnectionStateListener listener) throws ZookeeperException;

    /**
     * 销毁监听器
     * 
     * @param listener
     *            监听器
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod()
    public void removeListener(Listener listener) throws ZookeeperException;

    /**
     * 选举leader
     * 
     * @param selectNode
     *            选举的节点
     * @param id
     *            唯一标识
     * @return leader
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod()
    public NewLeader createLeader(String selectNode, String id) throws ZookeeperException;

    /**
     * 选举leader（由客户端定义唯一标识）
     * 
     * @param selectNode
     *            选举的节点
     * @return leader
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod()
    public NewLeader createLeader(String selectNode) throws ZookeeperException;

    /**
     * 创建分布式可重入锁
     * 
     * @param lockNode
     *            竞争锁节点
     * @return DistributedReentrantLock
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod()
    public DistributedReentrantLock createDistributedReentrantLock(String lockNode) throws ZookeeperException;

    /**
     * 创建分布式可重入读写锁
     * 
     * @param lockNode
     *            竞争锁节点
     * @return DistributedReadWriteLock
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod()
    public DistributedReadWriteLock createDistributedReadWriteLock(String lockNode) throws ZookeeperException;

    /**
     * 读取项目自身配置
     * 
     * @param key
     *            key
     * @return Object
     * @throws ZookeeperException
     *             自定义zk异常
     */
    @BeforeMethod()
    public Object getSelfConfigDataByKey(String key) throws ZookeeperException;

    /**
     * 销毁链接
     */
    @BeforeMethod()
    public void destroy();

}
