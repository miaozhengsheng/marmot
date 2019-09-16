package com.marmot.zk.client.lock;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException.NotEmptyException;

import com.marmot.zk.constants.ZKConstants;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.utils.PathUtils;


/**
 * 与JDK的ReentrantReadWriteLock类似。读锁不排斥其他读锁，但排斥写锁；写锁排斥其它锁<br>
 * 对zk上的某个节点请求锁，会在其下生成临时序列子节点。释放锁后会删除临时序列子节点。<br>
 * 务必在finally块中释放锁; 重入了几次对应释放几次。<br>
 * 此锁是可重入的。一个拥有写锁的线程可重入读锁，但是读锁却不能重入升级为写锁。<br>
 * 写锁可以降级成读锁， 比如请求写锁→读锁 →释放写锁。 从读锁升级成写锁是不行的。<br>
 * 
 * <pre>
 * DistributedReadWriteLock lock = null;
 * try {
 *     lock = ZKClientImpl.getInstance().createDistributedReadWriteLock(&quot;lockNodeName&quot;);
 *     lock.tryLockWrite();
 *     if (!lock.isWriteLocked()) {
 *         // do something write...
 *     }
 * } catch (Exception e) {
 *     e.printStackTrace();
 * } finally {
 *     if (lock != null) {
 *         lock.releaseAll();
 *     }
 * }
 * </pre>
 * 
 */
public class DistributedReadWriteLock {

    private InterProcessReadWriteLock lock;
    // 锁路径fullpath
    private final String lockPath;
    // 锁对象创建
    private InterProcessMutex readLock;
    private InterProcessMutex writeLock;
    private CuratorFramework client;
    private static final Logger logger = Logger.getLogger(DistributedReadWriteLock.class);

    public DistributedReadWriteLock(CuratorFramework client, EnumZKNameSpace namespace, String path) {

        // 参数验证
        if (namespace == null || PathUtils.isEmptyPath(path) || !path.startsWith(ZKConstants.LOCK_PREFIX)) {
            throw new IllegalArgumentException(
                    "DistributedReadWriteLock() argument is invalid. namespace=" + namespace + ", path=" + path);
        }
        String fullPath = PathUtils.joinPath(namespace, path);
        PathUtils.checkNodePath(fullPath);

        this.lockPath = fullPath;
        this.client = client;
        lock = new InterProcessReadWriteLock(client, fullPath);
        readLock = lock.readLock();
        writeLock = lock.writeLock();

    }

    /**
     * 阻塞方法，直到获取读锁（可重入）
     * 
     * @throws Exception
     *             ZK errors, connection interruptions
     */
    public void tryLockRead() throws Exception {
        readLock.acquire();
    }

    /**
     * 在指定时间内获取读锁（可重入）<br>
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
    public boolean tryLockRead(long time, TimeUnit unit) throws Exception {
        return readLock.acquire(time, unit);
    }

    /**
     * 是否拥有读锁
     * 
     * @return true/false
     */
    public boolean isReadLocked() {
        return readLock.isAcquiredInThisProcess();
    }

    /**
     * 释放读锁。每次获取锁都需要对应释放一次。
     * 
     * @throws Exception
     *             ZK errors, interruptions, current thread does not own the
     *             lock
     */
    public void unlockRead() throws Exception {
        if (isReadLocked()) {
            readLock.release();
        }
    }

    /**
     * 阻塞方法，直到获取写锁（可重入）
     * 
     * @throws Exception
     *             ZK errors, connection interruptions
     */
    public void tryLockWrite() throws Exception {
        writeLock.acquire();
    }

    /**
     * 在指定时间内获取写锁（可重入）<br>
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
    public boolean tryLockWrite(long time, TimeUnit unit) throws Exception {
        return writeLock.acquire(time, unit);
    }

    /**
     * 是否拥有写锁
     * 
     * @return true/false
     */
    public boolean isWriteLocked() {
        return writeLock.isAcquiredInThisProcess();
    }

    /**
     * 释放写锁。每次获取锁都需要对应释放一次。
     * 
     * @throws Exception
     *             ZK errors, interruptions, current thread does not own the
     *             lock
     */
    public void unlockWrite() throws Exception {
        if (isWriteLocked()) {
            writeLock.release();
        }
    }

    /**
     * 清除所有锁资源
     * 
     * @throws Exception
     *             ZK errors, interruptions, current thread does not own the
     *             lock
     */
    public void releaseAll() throws Exception {
        // 释放所有锁资源
        while (writeLock.isAcquiredInThisProcess()) {
            writeLock.release();
        }
        while (readLock.isAcquiredInThisProcess()) {
            readLock.release();
        }
        // 删除节点
        try {
            client.delete().guaranteed().forPath(lockPath);
        } catch (NotEmptyException e) {
            // 若节点下仍有子节点，忽略
        }catch (Exception e) {
            logger.warn("【zookeeper】清除锁信息失败", e);
        }
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
