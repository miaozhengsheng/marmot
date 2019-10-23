package com.marmot.cache.redis;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import com.marmot.cache.factory.RedisCacheClientFactory;
import com.marmot.common.util.PropUtil;

public class RedisCacheClientBean implements FactoryBean<IRedisCacheClient>, DisposableBean {

    protected String masterServer;// format ip:port
    protected String slaveServer;// format ip:port
    protected String password;
    protected int defaultExpire = PropUtil.getInstance().getInt("redis.defaultExpire", RedisCacheClientFactory.PERSIST);
    protected int maxActive = PropUtil.getInstance().getInt("redis.maxActive", RedisCacheClientFactory.MAX_ACTIVE);
    protected int opTimeout = PropUtil.getInstance().getInt("redis.optimeout", RedisCacheClientFactory.SO_TIMEOUT);
    protected boolean readWriteSplitting = PropUtil.getInstance().getBoolean("redis.readWriteSplitting",
            RedisCacheClientFactory.READ_WRITE_SPLITTING);
    protected boolean transcode = PropUtil.getInstance().getBoolean("redis.transcode",
            RedisCacheClientFactory.TRANSCODE);

    protected IRedisCacheClient redisCacheClient;

    @Override
    public void destroy() throws Exception {
        if (this.redisCacheClient != null) {
            this.redisCacheClient.shutdown();
        }
    }

    @Override
    public IRedisCacheClient getObject() throws Exception {
        if (this.redisCacheClient == null) {
            RedisCacheClientFactory factory = new RedisCacheClientFactory();
            factory.setMaxActive(maxActive);
            factory.setDefaultExpire(defaultExpire);
            factory.setOpTimeout(opTimeout);
            factory.setMasterServer(masterServer);
            factory.setSlaveServer(slaveServer);
            factory.setReadWriteSplitting(readWriteSplitting);
            factory.setPassword(password);
            factory.setTranscode(transcode);
            this.redisCacheClient = factory.newRedisInstance();
        }
        return this.redisCacheClient;
    }

    @Override
    public Class<?> getObjectType() {
        return IRedisCacheClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void setMasterServer(String masterServer) {
        this.masterServer = masterServer;
    }

    public void setSlaveServer(String slaveServer) {
        this.slaveServer = slaveServer;
    }

    public void setOpTimeout(int opTimeout) {
        this.opTimeout = opTimeout;
    }

    public void setDefaultExpire(int defaultExpire) {
        this.defaultExpire = defaultExpire;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    public void setReadWriteSplitting(boolean readWriteSplitting) {
        this.readWriteSplitting = readWriteSplitting;
    }

    public void setTranscode(boolean transcode) {
        this.transcode = transcode;
    }
}
