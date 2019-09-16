package com.marmot.zk.utils;

import java.security.Security;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.log4j.Logger;

import com.marmot.zk.client.IZKClient;
import com.marmot.zk.client.exception.ZookeeperException;
import com.marmot.zk.client.impl.ZKClientCacheImpl;
import com.marmot.zk.client.impl.ZKClientImpl;
import com.marmot.zk.constants.ZKConstants;
import com.marmot.zk.listener.Operation4internal;

public class ZookeeperFactory {


    // 带缓存的zk实现类
    private static IZKClient zookeeperClientCacheImpl;

    // 不带缓存的zk实现类
    private static IZKClient zookeeperClientImpl;

   

    private static Logger logger = Logger.getLogger(ZookeeperFactory.class);

    static {
        // ZKclientWrapper无需加载链接
        Boolean isNeedInit = true;
        StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
        for (StackTraceElement stack : stacks) {
            if ("com.marmot.zk.client.ZKClientWrapper.destroy"
                    .equals(stack.getClassName() + "." + stack.getMethodName())) {
                isNeedInit = false;
                break;
            }
        }

        if (isNeedInit) {
            // 构造默认集群的链接
        	/*-
            CuratorFramework client = getclient(getDefaultZookeeperUrl(), ZKConstants.ZK_CLIENT_USER,
                    getDefaultZkPassword());
                    */
            
            CuratorFramework client = getclient(getDefaultZookeeperUrl(), ZKConstants.ZK_CLIENT_USER,
                    "");

            // 初始化不带缓存的对象
            ZKClientImpl nocacheZookeeperClientImpl = new ZKClientImpl(client, true);

            // 初始化zk信息
            try {
                Operation4internal.initZookeeperClient(client, nocacheZookeeperClientImpl, true);
            } catch (Exception e) {
                // 抛出异常
                logger.error("【zookeeper】start zookeeper error...", e);
                throw new RuntimeException("start zookeeper error...", e);
            }

            // 初始化不带缓存的对象 代理
            zookeeperClientImpl = ProxyUtils.getProxy4ZKClient(nocacheZookeeperClientImpl, client, false);

            // 初始化带缓存的对象 一层代理
            zookeeperClientCacheImpl = new ZKClientCacheImpl(zookeeperClientImpl,
                    nocacheZookeeperClientImpl.getNodeEventManager());

        }
    }

    // 获取默认zk的集群地址
    private static String getDefaultZookeeperUrl() {
        String zkUrl = ZKConstants.ZK_URL_ONLINE;
        String env = System.getProperty("SystemRuntimeEnvironment");
        if (env == null) {
            logger.error("【zookeeper】System Property(SystemRuntimeEnvironment) is null");
            throw new IllegalArgumentException("System Property(SystemRuntimeEnvironment) is null");
        }
        // dev、qa、online
        switch (env) {
        case "dev":
            zkUrl = ZKConstants.ZK_URL_DEV;
            break;
        case "qa":
            zkUrl = ZKConstants.ZK_URL_QA;
            break;
        case "online":
            zkUrl = ZKConstants.ZK_URL_ONLINE;
            break;
        default:
            zkUrl = ZKConstants.ZK_URL_ONLINE;
        }
        return zkUrl;
    }

    // 获取默认zk集群的密码
    private static String getDefaultZkPassword() {

        // dev、qa无需解密 online解密
        String userName = ZKConstants.ZK_CLIENT_USER;
        String encryptPwd;
        try {
            encryptPwd = UserAuthUtil.getEncryptPwdByName(null, userName);
        } catch (ZookeeperException e) {
            logger.error("【zookeeper】获取zk密码失败", e);
            throw new RuntimeException("获取zk密码失败", e);
        }
        return UserAuthUtil.isOnline() ? decryptPwd(encryptPwd) : encryptPwd;
    }

    // 密码解密
    private static String decryptPwd(String encryptPwd) {
        // dev、qa无需解密 online解密
        if (UserAuthUtil.isOnline()) {
            String pk = Security.getProperty(ZKConstants.ZK_SECURITY_PK);
            if (StringUtils.isBlank(pk) || pk.length() < 8) {
                logger.error("【zookeeper】ZK_SECURITY_PV error!");
                throw new RuntimeException("ZK_SECURITY_PV error!");
            }
            return DesPlus.getInstance().decrypt(encryptPwd, pk);
        }
        return encryptPwd;
    }

    // 获取zk客户端链接
    private static CuratorFramework getclient(String url, String _userName, String _password) {

        if (StringUtils.isBlank(url)) {
            logger.error("【zookeeper】zookeeper url is empty");
            throw new IllegalArgumentException("zookeeper url is empty");
        }

        // 用户自定义session超时时间，默认为30s
        int sessionTimeoutMs = PropUtil.getInstance().getInt("zkSessionTimeoutMs",
        		ZKConstants.DEFAULT_SESSION_TIMEOUT_MS);
        // 用户自定义链接超时时间，默认为10s
        int connectionTimeoutMs = PropUtil.getInstance().getInt("zkConnectionTimeoutMs",
        		ZKConstants.DEFAULT_CONNECTION_TIMEOUT_MS);
        // 构建builder
        Builder builder = CuratorFrameworkFactory.builder().connectString(url).sessionTimeoutMs(sessionTimeoutMs)
                .retryPolicy(new ExponentialBackoffRetry((int) (Math.random() * 500) + 1000, 3))// TODO
                                                                                                // 重试策略待定
                .connectionTimeoutMs(connectionTimeoutMs);

        // 用户名密码不为空 做acl鉴权
        if (!StringUtils.isBlank(_userName) && !StringUtils.isBlank(_password)) {
            builder.authorization(ZKConstants.ZK_SCHEMA,
                    (_userName + ZKConstants.ZK_INTERVAL + _password).getBytes());
        }

        CuratorFramework client = builder.build();
        // 启动
        client.start();

        return client;
    }

    /**
     * 单例模式 使用默认zk集群的缓存模式，缓存会动态更新
     * 
     * @return IZookeeperClient IZookeeperClient接口
     */
    public static IZKClient useDefaultZookeeperWithCache() {
        return zookeeperClientCacheImpl;
    }

    /**
     * 单例模式 使用默认zk集群的无缓存模式
     * 
     * @return IZookeeperClient IZookeeperClient接口
     */
    public static IZKClient useDefaultZookeeper() {
        return zookeeperClientImpl;
    }



    /**
     * 非单例模式，使用自定义zk集群的缓存模式，缓存会自动更新
     * 
     * @param url
     *            zookeeper集群地址
     * @param _userName
     *            用户名
     * @param _password
     *            密码
     * @return IZookeeperClient IZookeeperClient接口
     */
    public static IZKClient newCustomZookeeperWithCache(String url, String _userName, String _password) {
        CuratorFramework client = getclient(url, _userName, decryptPwd(_password));
        ZKClientImpl zookeeperClientImpl = new ZKClientImpl(client, false);
        // 初始化zk信息
        try {
            Operation4internal.initZookeeperClient(client, zookeeperClientImpl, false);
        } catch (Exception e) {
            // 抛出异常
            logger.error("【zookeeper】start zookeeper error...", e);
            throw new RuntimeException("start zookeeper error...", e);
        }
        return new ZKClientCacheImpl(ProxyUtils.getProxy4ZKClient(zookeeperClientImpl, client, false),
                zookeeperClientImpl.getNodeEventManager());
    }

    /**
     * 非单例模式，使用自定义zk集群的无缓存模式
     * 
     * @param url
     *            zookeeper集群地址
     * @param _userName
     *            用户名
     * @param _password
     *            密码
     * @return IZookeeperClient IZookeeperClient接口
     */
    public static IZKClient newCustomZookeeper(String url, String _userName, String _password) {
        // 根据地址建立链接，用户名密码鉴权
        CuratorFramework client = getclient(url, _userName, decryptPwd(_password));
        ZKClientImpl nocacheZookeeperClientImpl = new ZKClientImpl(client, false);
        // 初始化zk信息
        try {
            Operation4internal.initZookeeperClient(client, nocacheZookeeperClientImpl, false);
        } catch (Exception e) {
            // 抛出异常
            logger.error("【zookeeper】start zookeeper error...", e);
            throw new RuntimeException("start zookeeper error...", e);
        }
        return ProxyUtils.getProxy4ZKClient(nocacheZookeeperClientImpl, client, false);
    }

}
