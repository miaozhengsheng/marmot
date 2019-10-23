package com.marmot.cache.redis.unusual;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.marmot.cache.redis.IKeyShard;
import com.marmot.cache.redis.IKeyShard.Shard;
import com.marmot.cache.utils.RedisSentinel;
import com.marmot.common.collections.ImprovedExpiredHashMap;

public class MasterSlaveIntellect {


    /**
     * 记录key上次写操作时间 <br>
     */
    private final ImprovedExpiredHashMap<String, Long> lastWriteOpMap = new ImprovedExpiredHashMap<String, Long>(
            MASTER_SLAVE_DELAY_THRESHOLD, TimeUnit.SECONDS);

    /**
     * 主从延时的判断的最小阀值. 大于此阀值直接走主库
     */
    private static final long MASTER_SLAVE_DELAY_THRESHOLD = 10; // 秒

    /**
     * 请求次数 计数
     */
    protected final AtomicLong requestCount = new AtomicLong(0);

    protected final IRedisCacheClient master;
    protected final IRedisCacheClient slave;

    /**
     * 读写分离，如果是true，所有读走从库
     */
    protected final boolean readWriteSplitting;

    public MasterSlaveIntellect(final IRedisCacheClient master, final IRedisCacheClient slave,
            final boolean readWriteSplitting) {
        this.master = master;
        this.slave = slave;
        this.readWriteSplitting = readWriteSplitting;
    }

    public IRedisCacheClient getMaster() {
        return master;
    }

    public IRedisCacheClient getSlave() {
        return slave;
    }

    public void writeOpLog(String key) {
        lastWriteOpMap.put(key, System.currentTimeMillis());
    }

    public void writeOpLog(String[] keys) {
        for (String key : keys) {
            writeOpLog(key);
        }
    }

    public void writeOpLog(Collection<String> keys) {
        for (String key : keys) {
            writeOpLog(key);
        }
    }

    public IRedisCacheClient choose(String key, final IRedisCacheClient master, final IRedisCacheClient slave) {
        return (readWriteSplitting) ? slave : choose(lastWriteOpMap.containsKey(key), master, slave, key);
    }

    public IRedisCacheClient choose(Collection<String> keys, final IRedisCacheClient master,
            final IRedisCacheClient slave) {
        if (keys == null || keys.isEmpty()) {
            return slave;
        }
        if (readWriteSplitting) {
            return slave;
        }
        boolean flag = false;
        for (String key : keys) {
            if (lastWriteOpMap.containsKey(key)) {
                flag = true;
                break;
            }
        }
        return choose(flag, master, slave, keys.iterator().next());// 暂时取第一个key作判断，后续优化
    }

    public IRedisCacheClient choose(String[] keys, final IRedisCacheClient master, final IRedisCacheClient slave) {
        if (keys == null || keys.length == 0) {
            return slave;
        }
        if (readWriteSplitting) {
            return slave;
        }
        boolean flag = false;
        for (String key : keys) {
            if (lastWriteOpMap.containsKey(key)) {
                flag = true;
                break;
            }
        }
        return choose(flag, master, slave, keys[0]);// 暂时取第一个key作判断，后续优化
    }

    private IRedisCacheClient choose(boolean isMaster, final IRedisCacheClient master, final IRedisCacheClient slave,
            String key) {
        if (isMaster) {
            return master;
        }
        IRedisCacheClient client = master;
        for (int i = 0; i < 2; i++) {
            int mod = (int) (requestCount.getAndIncrement() % 2);
            switch (mod) {
                case 0:
                    client = master;
                    break;
                case 1:
                    client = slave;
                    break;
            }
            IKeyShard keyShard = (IKeyShard) client;
            Shard shard = keyShard.getShard(key);
            if (shard == null) {
                continue;
            }
            if (RedisSentinel.getInstance().valid(shard.getHost(), shard.getPort())) {
                break;
            }
        }
        return client;
    }

    /**
     * 释放资源
     */
    public void destroy() {
        lastWriteOpMap.destroy();
    }


}
