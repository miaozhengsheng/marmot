package com.marmot.cache.redis.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.springframework.util.StopWatch;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.util.Sharded;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.marmot.cache.constants.CacheConst;
import com.marmot.cache.enums.EnumExist;
import com.marmot.cache.enums.EnumTime;
import com.marmot.cache.factory.RedisCacheClientFactory;
import com.marmot.cache.factory.RedisCacheClientFactory.Cluster;
import com.marmot.cache.redis.IBlockHandle;
import com.marmot.cache.redis.IKeyShard;
import com.marmot.cache.redis.IRedisCacheClient;
import com.marmot.cache.redis.IRedisReload;
import com.marmot.cache.redis.KeySharded;
import com.marmot.cache.redis.ShardRedisContext;
import com.marmot.cache.redis.pubsub.AbstractSubscriber;
import com.marmot.cache.utils.FailureCollecter;
import com.marmot.cache.utils.RedisSentinel;
import com.marmot.cache.utils.ToolUtil;
import com.marmot.cache.utils.ToolUtil.Handle;
import com.marmot.common.system.SystemUtil;
import com.marmot.common.util.ThreadLocalUtil;

public class ShardRedisCacheClientImpl implements IRedisCacheClient, IRedisReload<String>, IKeyShard, IBlockHandle<ShardedJedis> {

    private static final Logger logger = Logger.getLogger(ShardRedisCacheClientImpl.class);

    private final IRedisCacheClient shardRedisCacheClientProxy;

    private final ShardRedisContext shardRedisContext;

    private final AtomicReference<ShardedJedisPool> atomicShardedJedisPoolReference;

    private final AtomicReference<KeySharded> atomicKeySharded;

    private final Set<ShardedJedis> blockedJedis = Collections.synchronizedSet(new HashSet<ShardedJedis>());
    private final AtomicBoolean reloading = new AtomicBoolean(false);// 切换状态
    private final Lock lock = new ReentrantLock();

    private Cluster cluster = Cluster.master;

    private static ConcurrentMap<Method, Method> methodMap = new ConcurrentHashMap<Method, Method>();

    private String server;

    private final JedisPoolConfig jpc;
    private final int soTimeout;
    private volatile boolean shutdown = false;

    public ShardRedisCacheClientImpl(String servers, String password, int defaultExpire, int soTimeout,
            boolean transcode, JedisPoolConfig jpc) {
        this(servers, password, defaultExpire, soTimeout, transcode, jpc, Cluster.master);
    }

    public ShardRedisCacheClientImpl(String servers, String password, int defaultExpire, int soTimeout,
            boolean transcode, JedisPoolConfig jpc, Cluster cluster) {
        this.server = servers;
        String[] serversArr = servers.split(",");
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        for (String ips : serversArr) {
            String[] ipsArr = ips.split(":");
            JedisShardInfo jedisShardInfo = new JedisShardInfo(ipsArr[0], Integer.parseInt(ipsArr[1]),
                    RedisCacheClientFactory.CONNECTION_TIMEOUT, soTimeout, Sharded.DEFAULT_WEIGHT);
            jedisShardInfo.setPassword(password);
            shards.add(jedisShardInfo);
        }

        this.soTimeout = soTimeout;
        this.jpc = jpc;
        this.atomicShardedJedisPoolReference = new AtomicReference<ShardedJedisPool>(new ShardedJedisPool(jpc, shards));
        for (JedisShardInfo info : shards) {
            RedisSentinel.getInstance().watch(info.getHost(), info.getPort(), password);
        }
        this.atomicKeySharded = new AtomicReference<KeySharded>(new KeySharded(shards));
        this.shardRedisContext = new ShardRedisContext(defaultExpire, transcode);
        this.shardRedisCacheClientProxy = (IRedisCacheClient) Proxy.newProxyInstance(
                IRedisCacheClient.class.getClassLoader(), new Class[] { IRedisCacheClient.class },
                new ShardRedisCacheClientInterceptor());
        this.cluster = cluster;
        logger.info("Redis shard client[" + servers + "] starting succeed");
    }

    @Override
    public void reload(String servers, Set<String> instances, final String password) {
        String[] serversArr = servers.split(",");
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        for (String ips : serversArr) {
            String[] ipsArr = ips.split(":");
            JedisShardInfo jedisShardInfo = new JedisShardInfo(ipsArr[0], Integer.parseInt(ipsArr[1]),
                    RedisCacheClientFactory.CONNECTION_TIMEOUT, soTimeout, Sharded.DEFAULT_WEIGHT);
            jedisShardInfo.setPassword(password);
            shards.add(jedisShardInfo);
        }
        StopWatch stopWatch = new StopWatch("JedisPool reload");
        stopWatch.start("reload new");

        lock.lock();
        try {
            // 加锁
            reloading.getAndSet(true);
            for (final JedisShardInfo info : shards) {

                ToolUtil.retry(3, 500, new Handle() {
                    @Override
                    public void invoke() throws Throwable {
                        RedisSentinel.getInstance().watch(info.getHost(), info.getPort(), password);
                    }
                });

            }

            atomicKeySharded.getAndSet(new KeySharded(shards));
            ShardedJedisPool oldJedisPool = atomicShardedJedisPoolReference
                    .getAndSet(new ShardedJedisPool(jpc, shards));

            stopWatch.stop();
            if (oldJedisPool != null) {
                try {
                    // 等待长时间执行的redis命令
                    TimeUnit.MILLISECONDS.sleep(ToolUtil.justTime(soTimeout));
                } catch (InterruptedException e) {
                }
                stopWatch.start("destory old");
                oldJedisPool.destroy();
                interruptAll();
                stopWatch.stop();
            }
            // 心跳刷新
            serversArr = this.server.split(",");
            for (String ips : serversArr) {
                String[] ipsArr = ips.split(":");
                if (!instances.contains(ips)) {
                    RedisSentinel.getInstance().unwatch(ipsArr[0], Integer.parseInt(ipsArr[1]));
                }
            }
            this.server = servers;
            logger.warn(
                    CacheConst.LOG_PREFIX + "重新加载Redis资源成功, servers:" + servers + ", 耗时:" + stopWatch.prettyPrint());
        } finally {
            try {
                // 释放锁
                reloading.getAndSet(false);
            } finally {
                lock.unlock();
            }
        }
    }

    private class ShardRedisCacheClientInterceptor implements InvocationHandler {

        private Method oppositeMethod(final Method method) throws Exception {
            Method oppMethod = methodMap.get(method);
            if (oppMethod == null) {
                String name = method.getName();
                Class<?>[] pTypes = method.getParameterTypes();
                Class<?>[] _pTypes = new Class<?>[pTypes.length + 1];
                _pTypes[0] = ShardedJedis.class;
                System.arraycopy(pTypes, 0, _pTypes, 1, pTypes.length);
                oppMethod = ShardRedisContext.class.getDeclaredMethod(name, _pTypes);
                methodMap.put(method, oppMethod);
            }
            return oppMethod;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            ShardedJedis jedis = null;
            Object result = null;
            String params = Arrays.toString(args);

            Transaction transaction = Cat.newTransaction("Cache.redis", method.getName());
            transaction.addData("args", params);
            Cat.logEvent("Cache.redis.cluster", cluster.name());
            long createTime = System.currentTimeMillis();
            try {
                jedis = atomicShardedJedisPoolReference.get().getResource();
                // 监控堵塞的方法
                if (isBlock(method.getName())) {
                    used(jedis);
                }
                Object[] _args = new Object[args.length + 1];
                _args[0] = jedis;
                System.arraycopy(args, 0, _args, 1, args.length);
                result = oppositeMethod(method).invoke(shardRedisContext, _args);
                transaction.setStatus(Message.SUCCESS);
            } catch (Throwable t) {
                if (shutdown && isBlock(method.getName())) {
                    // do nothing 项目停止时并且是阻塞方法调用时抛错不输出错误日志和埋点Cat
                } else {
                    Throwable actual = t;
                    if (actual instanceof InvocationTargetException) {
                        actual = ((InvocationTargetException) actual).getTargetException();
                    }
                    String prefix = "";
                    String currentUrl = ThreadLocalUtil.getInstance().getCurrentUrl();
                    if (currentUrl != null && !"".equals(currentUrl)) {
                        prefix = "Current url=" + currentUrl + " ";
                    }
                    String address = "unknow";
                    if (jedis != null) {
                        if (args != null && args.length > 0) {
                            Object obj = args[0];
                            if (obj instanceof String) {
                                String key = (String) obj;
                                address = jedis.getShard(key).getClient().getHost() + ":"
                                        + jedis.getShard(key).getClient().getPort();
                            }
                        }
                        logger.error(prefix + "IRedisCacheClient shard Method=[" + method.getName() + "] Params="
                                + params + " fail on redis server=[" + address + "]", actual);
                    } else {
                        logger.error(prefix + "IRedisCacheClient shard Method=[" + method.getName() + "] Params="
                                + params + " fail", actual);
                    }
                    FailureCollecter.redisFailure(currentUrl, method.getName(), params, address, createTime);
                    // throw t;//catch it
                    Cat.logEvent("Cache.redis.fail", SystemUtil.getInNetworkIp() + "->" + address);
                    transaction.setStatus(actual);
                    Cat.logError(actual);
                }
            } finally {
                try {
                    if (jedis != null) {
                        if (isBlock(method.getName())) {
                            release(jedis);
                        }
                        jedis.close();
                    }
                } catch (Exception e) {
                    logger.error("IRedisCacheClient shard Method=[" + method.getName() + "] Params=" + params
                            + " return resource fail.", e);
                }
                transaction.complete();
            }
            return result;
        }
    }

    /**
     * 注意：内部使用<br>
     * 使用范网：根据key查询落在那个分片上<br>
     * 
     * @param key
     * @return
     */
    @Override
    public Shard getShard(String key) {
        Jedis jedis = atomicKeySharded.get().getShard(key);
        if (jedis != null) {
            return new Shard(jedis.getClient().getHost(), jedis.getClient().getPort());
        }
        return null;
    }

    @Override
    public void shutdown() {
        lock.lock();
        try {
            // 加锁
            reloading.getAndSet(true);
            this.shutdown = true;
            atomicShardedJedisPoolReference.get().destroy();
            interruptAll();
        } finally {
            try {
                // 释放锁
                reloading.getAndSet(false);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public Boolean set(String key, Object value, int expire) {
        return shardRedisCacheClientProxy.set(key, value, expire);
    }

    @Override
    public Boolean set(String key, Object value) {
        return shardRedisCacheClientProxy.set(key, value);
    }

    @Override
    public Boolean set(String key, Object value, Date date) {
        return shardRedisCacheClientProxy.set(key, value, date);
    }

    @Override
    public Object get(String key) {
        return shardRedisCacheClientProxy.get(key);
    }

    @Override
    public Boolean delete(String key) {
        return shardRedisCacheClientProxy.delete(key);
    }

    @Override
    public Map<String, Object> getMap(Collection<String> keys) {
        return shardRedisCacheClientProxy.getMap(keys);
    }

    @Override
    public Boolean setCounter(String key, long num) {
        return shardRedisCacheClientProxy.setCounter(key, num);
    }

    @Override
    public Boolean setCounter(String key, long num, int expire) {
        return shardRedisCacheClientProxy.setCounter(key, num, expire);
    }

    @Override
    public Long incr(String key) {
        return shardRedisCacheClientProxy.incr(key);
    }

    @Override
    public Long incr(String key, long delta) {
        return shardRedisCacheClientProxy.incr(key, delta);
    }

    @Override
    public Long decr(String key) {
        return shardRedisCacheClientProxy.decr(key);
    }

    @Override
    public Long decr(String key, long delta) {
        return shardRedisCacheClientProxy.decr(key, delta);
    }

    @Override
    public Long keyDel(String... keys) {
        return shardRedisCacheClientProxy.keyDel(keys);
    }

    @Override
    public Set<String> keyKeys(String pattern) {
        return shardRedisCacheClientProxy.keyKeys(pattern);
    }

    @Override
    public Boolean keyExists(String key) {
        return shardRedisCacheClientProxy.keyExists(key);
    }

    @Override
    public Boolean keyExpire(String key, int seconds) {
        return shardRedisCacheClientProxy.keyExpire(key, seconds);
    }

    @Override
    public Boolean keyExpireAt(String key, long unixTime) {
        return shardRedisCacheClientProxy.keyExpireAt(key, unixTime);
    }

    @Override
    public Long keyTtl(String key) {
        return shardRedisCacheClientProxy.keyTtl(key);
    }

    @Override
    public Boolean stringSet(String key, String value) {
        return shardRedisCacheClientProxy.stringSet(key, value);
    }

    @Override
    public Boolean stringSetex(String key, String value, int seconds) {
        return shardRedisCacheClientProxy.stringSetex(key, value, seconds);
    }

    @Override
    public Boolean stringSet(String key, String value, EnumExist nxxx, EnumTime expx, long time) {
        return shardRedisCacheClientProxy.stringSet(key, value, nxxx, expx, time);
    }

    @Override
    public Boolean stringSetnx(String key, String value) {
        return shardRedisCacheClientProxy.stringSetnx(key, value);
    }

    @Override
    public Boolean stringMset(Map<String, String> kvs) {
        return shardRedisCacheClientProxy.stringMset(kvs);
    }

    @Override
    public String stringGet(String key) {
        return shardRedisCacheClientProxy.stringGet(key);
    }

    @Override
    public Map<String, String> stringMget(String... keys) {
        return shardRedisCacheClientProxy.stringMget(keys);
    }

    @Override
    public String stringGetset(String key, String value) {
        return shardRedisCacheClientProxy.stringGetset(key, value);
    }

    @Override
    public Boolean hashHset(String key, String field, String value) {
        return shardRedisCacheClientProxy.hashHset(key, field, value);
    }

    @Override
    public Boolean hashHsetnx(String key, String field, String value) {
        return shardRedisCacheClientProxy.hashHsetnx(key, field, value);
    }

    @Override
    public Boolean hashHmset(String key, Map<String, String> hash) {
        return shardRedisCacheClientProxy.hashHmset(key, hash);
    }

    @Override
    public String hashHget(String key, String field) {
        return shardRedisCacheClientProxy.hashHget(key, field);
    }

    @Override
    public List<String> hashHmget(String key, String... fields) {
        return shardRedisCacheClientProxy.hashHmget(key, fields);
    }

    @Override
    public Map<String, String> hashHgetAll(String key) {
        return shardRedisCacheClientProxy.hashHgetAll(key);
    }

    @Override
    public Long hashHdel(String key, String... fields) {
        return shardRedisCacheClientProxy.hashHdel(key, fields);
    }

    @Override
    public Long hashHlen(String key) {
        return shardRedisCacheClientProxy.hashHlen(key);
    }

    @Override
    public Boolean hashHexists(String key, String field) {
        return shardRedisCacheClientProxy.hashHexists(key, field);
    }

    @Override
    public Long hashHincrBy(String key, String field, long value) {
        return shardRedisCacheClientProxy.hashHincrBy(key, field, value);
    }

    @Override
    public Set<String> hashHkeys(String key) {
        return shardRedisCacheClientProxy.hashHkeys(key);
    }

    @Override
    public List<String> hashHvals(String key) {
        return shardRedisCacheClientProxy.hashHvals(key);
    }

    @Override
    public Long listLpush(String key, String... strings) {
        return shardRedisCacheClientProxy.listLpush(key, strings);
    }

    @Override
    public Long listRpush(String key, String... strings) {
        return shardRedisCacheClientProxy.listRpush(key, strings);
    }

    @Override
    public String listLpop(String key) {
        return shardRedisCacheClientProxy.listLpop(key);
    }

    @Override
    public String listBlpop(String key, int timeout) {
        return shardRedisCacheClientProxy.listBlpop(key, timeout);
    }

    @Override
    public String listBrpop(String key, int timeout) {
        return shardRedisCacheClientProxy.listBrpop(key, timeout);
    }

    @Override
    public String listRpop(String key) {
        return shardRedisCacheClientProxy.listRpop(key);
    }

    @Override
    public Long listLlen(String key) {
        return shardRedisCacheClientProxy.listLlen(key);
    }

    @Override
    public List<String> listLrange(String key, long start, long end) {
        return shardRedisCacheClientProxy.listLrange(key, start, end);
    }

    @Override
    public Long listLrem(String key, String value) {
        return shardRedisCacheClientProxy.listLrem(key, value);
    }

    @Override
    public Boolean listLtrim(String key, long start, long end) {
        return shardRedisCacheClientProxy.listLtrim(key, start, end);
    }

    @Override
    public Boolean listLset(String key, long index, String value) {
        return shardRedisCacheClientProxy.listLset(key, index, value);
    }

    @Override
    public Long setSadd(String key, String... members) {
        return shardRedisCacheClientProxy.setSadd(key, members);
    }

    @Override
    public Long setSrem(String key, String... members) {
        return shardRedisCacheClientProxy.setSrem(key, members);
    }

    @Override
    public Set<String> setSmembers(String key) {
        return shardRedisCacheClientProxy.setSmembers(key);
    }

    @Override
    public Boolean setSismember(String key, String member) {
        return shardRedisCacheClientProxy.setSismember(key, member);
    }

    @Override
    public Long setScard(String key) {
        return shardRedisCacheClientProxy.setScard(key);
    }

    @Override
    public String setSpop(String key) {
        return shardRedisCacheClientProxy.setSpop(key);
    }

    @Override
    public Boolean setSmove(String srckey, String dstkey, String member) {
        return shardRedisCacheClientProxy.setSmove(srckey, dstkey, member);
    }

    @Override
    public ScanResult<String> setScan(String key, String cursor) {
        return shardRedisCacheClientProxy.setScan(key, cursor);
    }

    @Override
    public ScanResult<String> setScan(String key, String cursor, ScanParams params) {
        return shardRedisCacheClientProxy.setScan(key, cursor, params);
    }

    @Override
    public Long sortSetZadd(String key, String member, double score) {
        return shardRedisCacheClientProxy.sortSetZadd(key, member, score);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Long sortSetZadd(String key, Map<Double, String> scoreMembers) {
        return shardRedisCacheClientProxy.sortSetZadd(key, scoreMembers);
    }

    @Override
    public Long sortSetZadd2(String key, Map<String, Double> scoreMembers) {
        return shardRedisCacheClientProxy.sortSetZadd2(key, scoreMembers);
    }

    @Override
    public Long sortSetZrem(String key, String... members) {
        return shardRedisCacheClientProxy.sortSetZrem(key, members);
    }

    @Override
    public Long sortSetZcard(String key) {
        return shardRedisCacheClientProxy.sortSetZcard(key);
    }

    @Override
    public Long sortSetZcount(String key, double min, double max) {
        return shardRedisCacheClientProxy.sortSetZcount(key, min, max);
    }

    @Override
    public Double sortSetZscore(String key, String member) {
        return shardRedisCacheClientProxy.sortSetZscore(key, member);
    }

    @Override
    public Double sortSetZincrby(String key, String member, double score) {
        return shardRedisCacheClientProxy.sortSetZincrby(key, member, score);
    }

    @Override
    public Set<String> sortSetZrange(String key, long start, long end) {
        return shardRedisCacheClientProxy.sortSetZrange(key, start, end);
    }

    @Override
    public Map<String, Double> sortSetZrangeWithScores(String key, long start, long end) {
        return shardRedisCacheClientProxy.sortSetZrangeWithScores(key, start, end);
    }

    @Override
    public Set<String> sortSetZrevrange(String key, long start, long end) {
        return shardRedisCacheClientProxy.sortSetZrevrange(key, start, end);
    }

    @Override
    public Map<String, Double> sortSetZrevrangeWithScores(String key, long start, long end) {
        return shardRedisCacheClientProxy.sortSetZrevrangeWithScores(key, start, end);
    }

    @Override
    public Long sortSetZrank(String key, String member) {
        return shardRedisCacheClientProxy.sortSetZrank(key, member);
    }

    @Override
    public Long sortSetZrevrank(String key, String member) {
        return shardRedisCacheClientProxy.sortSetZrevrank(key, member);
    }

    @Override
    public Set<String> sortSetZrangeByScore(String key, double min, double max) {
        return shardRedisCacheClientProxy.sortSetZrangeByScore(key, min, max);
    }

    @Override
    public Set<String> sortSetZrangeByScore(String key, double min, double max, int offset, int count) {
        return shardRedisCacheClientProxy.sortSetZrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<String> sortSetZrevrangeByScore(String key, double max, double min) {
        return shardRedisCacheClientProxy.sortSetZrevrangeByScore(key, max, min);
    }

    @Override
    public Set<String> sortSetZrevrangeByScore(String key, double max, double min, int offset, int count) {
        return shardRedisCacheClientProxy.sortSetZrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Long sortSetZremrangeByRank(String key, long start, long end) {
        return shardRedisCacheClientProxy.sortSetZremrangeByRank(key, start, end);
    }

    @Override
    public ScanResult<Tuple> sortSetZscan(String key, String cursor) {
        return shardRedisCacheClientProxy.sortSetZscan(key, cursor);
    }

    @Override
    public ScanResult<Tuple> sortSetZscan(String key, String cursor, ScanParams params) {
        return shardRedisCacheClientProxy.sortSetZscan(key, cursor, params);
    }

    @Override
    public Boolean publish(String channel, String message) {
        throw new UnsupportedOperationException("Sharding Does not support publish");
    }

    @Override
    public void subscribe(AbstractSubscriber subscriber, String... channels) {
        throw new UnsupportedOperationException("Sharding Does not support subscribe");
    }

    @Override
    public void psubscribe(AbstractSubscriber subscriber, String... channels) {
        throw new UnsupportedOperationException("Sharding Does not support psubscribe");
    }

    @Override
    public String toString() {
        return server;
    }

    @Override
    public void used(ShardedJedis t) {
        tryLock();
        blockedJedis.add(t);
    }

    @Override
    public void release(ShardedJedis t) {
        tryLock();
        blockedJedis.remove(t);
    }

    private void tryLock() {
        if (reloading.get()) {
            lock.lock();
            try {
                // do nothing
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void interruptAll() {
        for (ShardedJedis shardedJedis : blockedJedis) {
            try {
                for (Jedis jedis : shardedJedis.getAllShards()) {
                    jedis.getClient().close();
                }
            } catch (Exception e) {
            }
        }
        blockedJedis.clear();
    }

    @Override
    public boolean isBlock(String name) {
        return "listBlpop".equals(name) || "listBrpop".equals(name);
    }

}
