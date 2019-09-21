package com.marmot.cache.factory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import redis.clients.jedis.JedisPoolConfig;

import com.marmot.cache.ICacheClient;
import com.marmot.cache.redis.HeartbeatReportingImpl;
import com.marmot.cache.redis.IRedisCacheClient;
import com.marmot.cache.redis.impl.RedisCacheClientImpl;
import com.marmot.cache.redis.impl.ShardRedisCacheClientImpl;
import com.marmot.cache.redis.impl.ShardRedisCacheMSClientImpl;
import com.marmot.cache.redis.ms.RedisCacheMSClientImpl;
import com.marmot.cache.utils.IHeartbeatReporting;
import com.marmot.cache.utils.RedisSentinel;

public class RedisCacheClientFactory implements ICachedClientFactory {

    public static final int MAX_ACTIVE = 100; // 参数maxActive指明能从池中借出的对象的最大数目

    public static final int SO_TIMEOUT = 100;// 执行超时时间，单位毫秒

    public static final int CONNECTION_TIMEOUT = 1000;// 连接超时时间，单位毫秒

    public static final int PERSIST = 0;// 过期时间 不过期, 单位秒

    public static final boolean READ_WRITE_SPLITTING = false;// 读写分离

    public static final boolean TRANSCODE = false;// 是否转码压缩

    private String masterServer;
    private String slaveServer;

    private int defaultExpire = PERSIST;// 默认过期时间
    private int maxActive = MAX_ACTIVE;// 最大连接数

    protected String masterServers;
    protected String slaveServers;
    private int opTimeout = SO_TIMEOUT;// 执行超时时间
    private boolean readWriteSplitting = READ_WRITE_SPLITTING;// 读写分离
    private String password;
    private boolean transcode = TRANSCODE;

    public static enum Cluster {
        master, slave
    }

    private static IHeartbeatReporting heartbeatReporting = null;

    static {
        // 停止时就不初始化心跳上报处理器
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        if (!stes[2].getClassName().startsWith(".framework.SwiftApplicationContext")) {
            RedisSentinel.getInstance().listen(heartbeatReporting = new HeartbeatReportingImpl());
        }
    }

    /**
     * 返回新的缓存客户端实例
     */
    @Override
    public ICacheClient newInstance() throws Exception {
        return (ICacheClient) newRedisInstance();
    }

    /**
     * 返回新的redis 客户端实例
     * 
     * @return
     * @throws Exception
     */
    public IRedisCacheClient newRedisInstance() throws Exception {
        String[] masterArray = masterServer.split(":");
        IRedisCacheClient master = new RedisCacheClientImpl(masterArray[0], Integer.parseInt(masterArray[1]), password,
                defaultExpire, opTimeout, transcode, newJedisPoolConfig());
        // 从库
        if (slaveServer != null && slaveServer.length() > 0) {
            String[] slaveArray = slaveServer.split(":");
            return new RedisCacheMSClientImpl(
                    master, new RedisCacheClientImpl(slaveArray[0], Integer.parseInt(slaveArray[1]), password,
                            defaultExpire, opTimeout, transcode, newJedisPoolConfig(), Cluster.slave),
                    readWriteSplitting);
        } else {
            return master;
        }
    }

    /**
     * 返回新的redis 客户端实例
     * <p>
     * 接口声明了抛出异常
     * 
     * @return
     * @throws Exception
     */
    public com.marmot.cache.redis.unusual.IRedisCacheClient newRedisWithExceptionInstance() throws Exception {
        String[] masterArray = masterServer.split(":");
        com.marmot.cache.redis.unusual.IRedisCacheClient master = new com.marmot.cache.redis.unusual.RedisCacheClientImpl(
                masterArray[0], Integer.parseInt(masterArray[1]), password, defaultExpire, opTimeout, transcode,
                newJedisPoolConfig());
        // 从库
        if (slaveServer != null && slaveServer.length() > 0) {
            String[] slaveArray = slaveServer.split(":");
            return new com.marmot.cache.redis.unusual.RedisCacheMSClientImpl(master,
                    new com.marmot.cache.redis.unusual.RedisCacheClientImpl(slaveArray[0],
                            Integer.parseInt(slaveArray[1]), password, defaultExpire, opTimeout, transcode,
                            newJedisPoolConfig(), Cluster.slave),
                    readWriteSplitting);
        } else {
            return master;
        }
    }

    /**
     * 返回新的redis sharding 客户端实例
     * 
     * @return
     * @throws Exception
     */
    public IRedisCacheClient newShardRedisInstance() throws Exception {
        IRedisCacheClient master = new ShardRedisCacheClientImpl(masterServers, password, defaultExpire, opTimeout,
                transcode, newJedisPoolConfig());
        // 从库
        if (slaveServers != null && slaveServers.length() > 0) {
            return new ShardRedisCacheMSClientImpl(master, new ShardRedisCacheClientImpl(slaveServers, password,
                    defaultExpire, opTimeout, transcode, newJedisPoolConfig(), Cluster.slave), readWriteSplitting);
        } else {
            return master;
        }
    }

    /**
     * 返回新的redis sharding 客户端实例
     * <p>
     * 接口声明了抛出异常
     * 
     * @return
     * @throws Exception
     */
    public com.marmot.cache.redis.unusual.IRedisCacheClient newShardRedisWithExceptionInstance() throws Exception {
        com.marmot.cache.redis.unusual.IRedisCacheClient master = new com.marmot.cache.redis.unusual.ShardRedisCacheClientImpl(
                masterServers, password, defaultExpire, opTimeout, transcode, newJedisPoolConfig());
        // 从库
        if (slaveServers != null && slaveServers.length() > 0) {
            return new com.marmot.cache.redis.unusual.ShardRedisCacheMSClientImpl(
                    master, new com.marmot.cache.redis.unusual.ShardRedisCacheClientImpl(slaveServers, password,
                            defaultExpire, opTimeout, transcode, newJedisPoolConfig(), Cluster.slave),
                    readWriteSplitting);
        } else {
            return master;
        }
    }

    public JedisPoolConfig newJedisPoolConfig() {
        JedisPoolConfig jpc = new JedisPoolConfig();
        jpc.setMaxTotal(maxActive);
        // 最大空闲数
        jpc.setMaxIdle(20);
        // 表示当borrow一个jedis实例时，最大的等待时间，如果超过等待时间(毫秒)，则直接抛出异常
        jpc.setMaxWaitMillis(300);
        // 设定在借出对象时是否进行有效性检查
        jpc.setTestOnBorrow(false);
        // 设定在还回对象时是否进行有效性检查
        jpc.setTestOnReturn(false);
        // 表示有一个线程对idle object进行扫描，如果validate失败，此object会被从pool中drop掉
        jpc.setTestWhileIdle(true);
        // 表示线程两次扫描之间要sleep的毫秒数
        jpc.setTimeBetweenEvictionRunsMillis(30 * 60 * 1000);
        return jpc;
    }

    public void setDefaultExpire(int defaultExpire) {
        this.defaultExpire = defaultExpire;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    public void setOpTimeout(int opTimeout) {
        this.opTimeout = opTimeout;
    }

    public void setMasterServer(String masterServer) {
        this.masterServer = masterServer;
    }

    public void setSlaveServer(String slaveServer) {
        this.slaveServer = slaveServer;
    }

    public void setMasterServers(String masterServers) {
        this.masterServers = masterServers;
    }

    public void setSlaveServers(String slaveServers) {
        this.slaveServers = slaveServers;
    }

    public void setReadWriteSplitting(boolean readWriteSplitting) {
        this.readWriteSplitting = readWriteSplitting;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setTranscode(boolean transcode) {
        this.transcode = transcode;
    }

    /**
     * 空操作实例
     * 
     * @return
     * @throws Exception
     */
    public static IRedisCacheClient nullRedisInstance() throws Exception {
        return (IRedisCacheClient) Proxy.newProxyInstance(IRedisCacheClient.class.getClassLoader(),
                new Class[] { IRedisCacheClient.class }, new NullRedisCacheClientInterceptor());
    }

    /**
     * 空操作实例
     * <p>
     * 接口声明了抛出异常
     * 
     * @return
     * @throws Exception
     */
    public static com.marmot.cache.redis.unusual.IRedisCacheClient nullRedisWithExceptionInstance() throws Exception {
        return (com.marmot.cache.redis.unusual.IRedisCacheClient) Proxy.newProxyInstance(
                com.marmot.cache.redis.unusual.IRedisCacheClient.class.getClassLoader(),
                new Class[] { com.marmot.cache.redis.unusual.IRedisCacheClient.class },
                new NullRedisCacheClientInterceptor());
    }

    private static class NullRedisCacheClientInterceptor implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            throw new UnsupportedOperationException("please config zookeeper:/config/public/cache/redis redis");
        }
    }

    /**
     * 关闭心跳检查和异常上报功能
     * <p>
     * 服务停止时调用
     */
    public static void closeHeartbeatReporting() {
        RedisSentinel.getInstance().shutdown();
        if (heartbeatReporting != null) {
            ((HeartbeatReportingImpl) heartbeatReporting).close();
        }
    }
}