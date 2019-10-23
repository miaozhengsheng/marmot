package com.marmot.cache.redis.conf;

import java.util.Set;

import com.marmot.cache.constants.CacheConst;
import com.marmot.cache.dynamic.DynamicLoadManager;
import com.marmot.cache.dynamic.IDynamicLoadHandle;
import com.marmot.cache.factory.RedisCacheClientFactory;
import com.marmot.cache.redis.IRedisCacheClient;
import com.marmot.cache.redis.IRedisReload;
import com.marmot.cache.redis.RedisCacheClientBean;
import com.marmot.cache.redis.impl.RedisCacheClientImpl;
import com.marmot.cache.redis.ms.RedisCacheMSClientImpl;
import com.marmot.cache.utils.ClusterUtil;
import com.marmot.cache.utils.ClusterUtil.Agreement;
import com.marmot.cache.utils.CollectionUtil;
import com.marmot.cache.utils.EnvironmentUtil;
import com.marmot.cache.utils.ZKUtil;
import com.marmot.common.conf.ClientUtil;
import com.marmot.zk.client.exception.ZookeeperException;

public class ConfRedisCacheClientBean extends RedisCacheClientBean implements IDynamicLoadHandle {

    private DynamicLoadManager dynamicLoadManager = new DynamicLoadManager();

    private String projectName;
    private String groupName;

    /**
     * projectName= 默认从config.properties读取<br>
     * groupName= default<br>
     * @throws ZookeeperException 
     */
    public ConfRedisCacheClientBean() throws ZookeeperException {
        this(ClientUtil.getProjectName());
    }

    /**
     * groupName= default<br>
     * 
     * @param projectName
     * @throws ZookeeperException 
     */
    public ConfRedisCacheClientBean(String projectName) throws ZookeeperException {
        this(projectName, CacheConst.GROUP_DEFAULT_NAME);
    }

    /**
     * @param projectName 项目名
     * @param groupName 分组名
     * @throws ZookeeperException 
     */
    public ConfRedisCacheClientBean(String projectName, String groupName) throws ZookeeperException {
        // 项目名
        this.projectName = projectName;
        this.groupName = groupName;

        Set<String> set = null;
        boolean debug = EnvironmentUtil.mayDebugRedis(this.projectName);
        if (debug) {
            // 线下调试模式
            set = ClusterUtil.getClustersFromFile(this.projectName, Agreement.redis);
        } else {
            set = ZKUtil.getRedisClusters(this.projectName, this.groupName);
        }

        // 初始化redis配置
        initCacheConfig(set);

        // 初始化redis配置变化监听器
        if (!debug && (set != null && set.size() > 0)) {
            dynamicLoadManager.set(this.projectName, this.groupName);
            dynamicLoadManager.setDynamicLoadHandle(this);
            dynamicLoadManager.createCacheConfigListener();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void invoke(Set<String> config) throws Exception {
        initCacheConfig(config);

        if (redisCacheClient instanceof RedisCacheClientImpl) {
            ((IRedisReload<String>) redisCacheClient).reload(masterServer, CollectionUtil.split(masterServer),
                    password);
        } else if (redisCacheClient instanceof RedisCacheMSClientImpl) {
            Set<String> set = CollectionUtil.split(masterServer, slaveServer);
            RedisCacheMSClientImpl ms = (RedisCacheMSClientImpl) redisCacheClient;
            ((IRedisReload<String>) ms.getMaster()).reload(masterServer, set, password);
            ((IRedisReload<String>) ms.getSlave()).reload(slaveServer, set, password);
        }
    }

    public void initCacheConfig(Set<String> currentRedisConfigSet) throws ZookeeperException {
        if (currentRedisConfigSet == null || currentRedisConfigSet.isEmpty()) {
            return;
        }

        String serverStr = currentRedisConfigSet.iterator().next();
        String[] servers = serverStr.split(",");
        masterServer = servers[0];
        if (servers.length == 2) {
            // 配置了从节点
            slaveServer = servers[1];
        } else {
            if (slaveServer != null) {
                slaveServer = masterServer;
            }
        }
        if (ZKUtil.isRedisAuth(projectName, groupName)) {
            password = ZKUtil.getRedisPassword();
        } else {
            password = null;
        }
    }

    @Override
    public IRedisCacheClient getObject() throws Exception {
        if (masterServer == null || masterServer.trim().length() == 0) {
            if (redisCacheClient == null) {
                redisCacheClient = RedisCacheClientFactory.nullRedisInstance();
            }
            return redisCacheClient;
        } else {
            return super.getObject();
        }
    }

    @Override
    public void destroy() throws Exception {
        if (masterServer != null && masterServer.trim().length() != 0) {
            super.destroy();
        }
    }

    @Override
    public void setMasterServer(String masterServer) {
        throw new UnsupportedOperationException("instead of zookeeper:/config/public/cache/redis read");
    }

    @Override
    public void setSlaveServer(String slaveServer) {
        throw new UnsupportedOperationException("instead of zookeeper:/config/public/cache/redis read");
    }

}
