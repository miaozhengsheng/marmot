package com.marmot.zk.client.lock;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException.NotEmptyException;

import com.marmot.zk.constants.ZKConstants;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.utils.PathUtils;


/**
 * 与JDK的ReentrantLock类似。<br>
 * 对zk上的某个节点请求锁，会在其下生成临时序列子节点。释放锁后会删除临时序列子节点。<br>
 * 务必在finally块中释放锁。重入了几次对应释放几次。<br>
 * 
 * <pre>
 * DistributedReentrantLock lock = null;
 * try {
 *     lock = ZKClientImpl.getInstance().createDistributedReentrantLock(&quot;lockNodeName&quot;);
 *     boolean flag = lock.tryLock(10, TimeUnit.SECONDS);
 *     if (flag) {
 *         // do something...
 *     }
 * } catch (Exception e) {
 *     e.printStackTrace();
 * } finally {
 *     if (lock != null) {
 *         lock.unlock();
 *     }
 * }
 * </pre>
 * 
 */
public class DistributedReentrantLock {

    private InterProcessMutex lock;
    // 锁路径fullpath
    private final String lockPath;

    private CuratorFramework client;

    private static final Logger logger = Logger.getLogger(DistributedReentrantLock.class);

    public DistributedReentrantLock(CuratorFramework client, EnumZKNameSpace namespace, String path) {

        // 参数验证
        if (namespace == null || PathUtils.isEmptyPath(path) || !path.startsWith(ZKConstants.LOCK_PREFIX)) {
            throw new IllegalArgumentException("argument is invalid. namespace=" + namespace + ", path=" + path);
        }
        String fullPath = PathUtils.joinPath(namespace, path);
        PathUtils.checkNodePath(fullPath);
        // 锁路径fullpath
        this.lockPath = fullPath;
        // 当前客户端链接
        this.client = client;
        // 锁对象创建
        lock = new InterProcessMutex(client, this.lockPath);
    }

    /**
     * 阻塞方法，直到获取锁（可重入）
     * 
     * @throws Exception
     *             ZK errors, connection interruptions
     */
    public void tryLock() throws Exception {
        lock.acquire();
    }

    /**
     * 在指定时间内获取锁（可重入）<br>
     * 超时将返回false
     * 
     * @param time
     *            time to wait
     * @param unit
     *            time unit
     * @return true if the mutex was acquired, false if not
     * @throws Exception
     *             ZK errors, connection interruptions
     */
    public boolean tryLock(long time, TimeUnit unit) throws Exception {
        return lock.acquire(time, unit);
    }
    
    // TODO 非阻塞锁

    /**
     * 是否拥有该锁
     * 
     * @return true/false
     */
    public boolean isLocked() {
        return lock.isAcquiredInThisProcess();
    }

    /**
     * 释放锁。每次获取锁后都需要对应释放一次。
     * 
     * @throws Exception
     *             ZK errors, interruptions, current thread does not own the
     *             lock
     */
    public void unlock() throws Exception {
        if (isLocked()) {
            lock.release();
        }
    }

    /**
     * 释放所有锁资源。
     * 
     * @throws Exception
     *             ZK errors, interruptions, current thread does not own the
     *             lock
     */
    public void releaseAll() throws Exception {
        // 释放所有锁资源
        while (lock.isAcquiredInThisProcess()) {
            lock.release();
        }
        // 删除节点
        try {
            client.delete().guaranteed().forPath(lockPath);
        } catch (NotEmptyException e) {
            // 若节点下仍有子节点，忽略
        } catch (Exception e) {
            logger.warn("【zookeeper】清除锁信息失败", e);
        }
        lock = null;
    }

    /**
     * 获取锁路径
     * 
     * @return 全路径
     */
    public String getLockPath() {
        return lockPath;
    }
}