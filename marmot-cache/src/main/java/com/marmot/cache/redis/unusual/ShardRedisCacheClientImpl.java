package com.marmot.cache.redis.unusual;

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
import com.marmot.cache.exception.CacheException;
import com.marmot.cache.factory.RedisCacheClientFactory;
import com.marmot.cache.factory.RedisCacheClientFactory.Cluster;
import com.marmot.cache.redis.IBlockHandle;
import com.marmot.cache.redis.IKeyShard;
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
    private final ReentrantLock lock = new ReentrantLock();

    private Cluster cluster = Cluster.master;

    private static ConcurrentMap<Method, Method> methodMap = new ConcurrentHashMap<Method, Method>();
    private static Set<Method> basicTypes = Collections.synchronizedSet(new HashSet<Method>());

    private final JedisPoolConfig jpc;
    private final int soTimeout;
    private String server;
    private volatile boolean shutdown = false;

    public ShardRedisCacheClientImpl(String servers, String password, int defaultExpire, int soTimeout,
            boolean transcode, JedisPoolConfig jpc) throws CacheException {
        this(servers, password, defaultExpire, soTimeout, transcode, jpc, Cluster.master);
    }

    public ShardRedisCacheClientImpl(String servers, String password, int defaultExpire, int soTimeout,
            boolean transcode, JedisPoolConfig jpc, Cluster cluster) throws CacheException {
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
                if (method.getReturnType() != void.class && method.getReturnType().isPrimitive()) {
                    basicTypes.add(method);
                }
            }
            return oppMethod;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            ShardedJedis jedis = null;
            Object result = null;
            boolean vaild = true;
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
                Throwable actual = t;
                if (actual instanceof InvocationTargetException) {
                    actual = ((InvocationTargetException) actual).getTargetException();
                }
                if (shutdown && isBlock(method.getName())) {
                    // do nothing 项目停止时并且是阻塞方法调用时抛错不输出错误日志和埋点Cat
                } else {
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
                                + params + " fail on redis server=[" + address + "]\n" + actual);
                    } else {
                        logger.error(prefix + "IRedisCacheClient shard Method=[" + method.getName() + "] Params="
                                + params + " fail\n" + actual);
                    }
                    FailureCollecter.redisFailure(currentUrl, method.getName(), params, address, createTime);
                    Cat.logEvent("Cache.redis.fail", SystemUtil.getInNetworkIp() + "->" + address);
                    transaction.setStatus(actual);
                    Cat.logError(actual);
                }
                throw new CacheException(actual);
            } finally {
                try {
                    if (jedis != null) {
                        if (args != null && args.length > 0) {
                            Object obj = args[0];
                            if (obj instanceof String) {
                                vaild = RedisSentinel.getInstance().valid(jedis.getShard((String) obj));
                            }
                        }
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
            if (!vaild) {
                throw new CacheException("caching is unavailable, relegated to null");
            }
            if (result == null && basicTypes.contains(method)) {
                throw new CacheException("return the basic type of data for null");
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
    public void shutdown() throws CacheException {
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
    public boolean set(String key, Object value, int expire) throws CacheException {
        return shardRedisCacheClientProxy.set(key, value, expire);
    }

    @Override
    public boolean set(String key, Object value) throws CacheException {
        return shardRedisCacheClientProxy.set(key, value);
    }

    @Override
    public boolean set(String key, Object value, Date date) throws CacheException {
        return shardRedisCacheClientProxy.set(key, value, date);
    }

    @Override
    public Object get(String key) throws CacheException {
        return shardRedisCacheClientProxy.get(key);
    }

    @Override
    public boolean delete(String key) throws CacheException {
        return shardRedisCacheClientProxy.delete(key);
    }

    @Override
    public Map<String, Object> getMap(Collection<String> keys) throws CacheException {
        return shardRedisCacheClientProxy.getMap(keys);
    }

    @Override
    public boolean setCounter(String key, long num) throws CacheException {
        return shardRedisCacheClientProxy.setCounter(key, num);
    }

    @Override
    public boolean setCounter(String key, long num, int expire) throws CacheException {
        return shardRedisCacheClientProxy.setCounter(key, num, expire);
    }

    @Override
    public long incr(String key) throws CacheException {
        return shardRedisCacheClientProxy.incr(key);
    }

    @Override
    public long incr(String key, long delta) throws CacheException {
        return shardRedisCacheClientProxy.incr(key, delta);
    }

    @Override
    public long decr(String key) throws CacheException {
        return shardRedisCacheClientProxy.decr(key);
    }

    @Override
    public long decr(String key, long delta) throws CacheException {
        return shardRedisCacheClientProxy.decr(key, delta);
    }

    @Override
    public long keyDel(String... keys) throws CacheException {
        return shardRedisCacheClientProxy.keyDel(keys);
    }

    @Override
    public Set<String> keyKeys(String pattern) throws CacheException {
        return shardRedisCacheClientProxy.keyKeys(pattern);
    }

    @Override
    public boolean keyExists(String key) throws CacheException {
        return shardRedisCacheClientProxy.keyExists(key);
    }

    @Override
    public boolean keyExpire(String key, int seconds) throws CacheException {
        return shardRedisCacheClientProxy.keyExpire(key, seconds);
    }

    @Override
    public boolean keyExpireAt(String key, long unixTime) throws CacheException {
        return shardRedisCacheClientProxy.keyExpireAt(key, unixTime);
    }

    @Override
    public long keyTtl(String key) throws CacheException {
        return shardRedisCacheClientProxy.keyTtl(key);
    }

    @Override
    public boolean stringSet(String key, String value) throws CacheException {
        return shardRedisCacheClientProxy.stringSet(key, value);
    }

    @Override
    public boolean stringSetex(String key, String value, int seconds) throws CacheException {
        return shardRedisCacheClientProxy.stringSetex(key, value, seconds);
    }

    @Override
    public boolean stringSet(String key, String value, EnumExist nxxx, EnumTime expx, long time) throws CacheException {
        return shardRedisCacheClientProxy.stringSet(key, value, nxxx, expx, time);
    }

    @Override
    public boolean stringSetnx(String key, String value) throws CacheException {
        return shardRedisCacheClientProxy.stringSetnx(key, value);
    }

    @Override
    public boolean stringMset(Map<String, String> kvs) throws CacheException {
        return shardRedisCacheClientProxy.stringMset(kvs);
    }

    @Override
    public String stringGet(String key) throws CacheException {
        return shardRedisCacheClientProxy.stringGet(key);
    }

    @Override
    public Map<String, String> stringMget(String... keys) throws CacheException {
        return shardRedisCacheClientProxy.stringMget(keys);
    }

    @Override
    public String stringGetset(String key, String value) throws CacheException {
        return shardRedisCacheClientProxy.stringGetset(key, value);
    }

    @Override
    public boolean hashHset(String key, String field, String value) throws CacheException {
        return shardRedisCacheClientProxy.hashHset(key, field, value);
    }

    @Override
    public Boolean hashHsetnx(String key, String field, String value) throws CacheException {
        return shardRedisCacheClientProxy.hashHsetnx(key, field, value);
    }

    @Override
    public boolean hashHmset(String key, Map<String, String> hash) throws CacheException {
        return shardRedisCacheClientProxy.hashHmset(key, hash);
    }

    @Override
    public String hashHget(String key, String field) throws CacheException {
        return shardRedisCacheClientProxy.hashHget(key, field);
    }

    @Override
    public List<String> hashHmget(String key, String... fields) throws CacheException {
        return shardRedisCacheClientProxy.hashHmget(key, fields);
    }

    @Override
    public Map<String, String> hashHgetAll(String key) throws CacheException {
        return shardRedisCacheClientProxy.hashHgetAll(key);
    }

    @Override
    public long hashHdel(String key, String... fields) throws CacheException {
        return shardRedisCacheClientProxy.hashHdel(key, fields);
    }

    @Override
    public long hashHlen(String key) throws CacheException {
        return shardRedisCacheClientProxy.hashHlen(key);
    }

    @Override
    public boolean hashHexists(String key, String field) throws CacheException {
        return shardRedisCacheClientProxy.hashHexists(key, field);
    }

    @Override
    public long hashHincrBy(String key, String field, long value) throws CacheException {
        return shardRedisCacheClientProxy.hashHincrBy(key, field, value);
    }

    @Override
    public Set<String> hashHkeys(String key) throws CacheException {
        return shardRedisCacheClientProxy.hashHkeys(key);
    }

    @Override
    public List<String> hashHvals(String key) throws CacheException {
        return shardRedisCacheClientProxy.hashHvals(key);
    }

    @Override
    public long listLpush(String key, String... strings) throws CacheException {
        return shardRedisCacheClientProxy.listLpush(key, strings);
    }

    @Override
    public long listRpush(String key, String... strings) throws CacheException {
        return shardRedisCacheClientProxy.listRpush(key, strings);
    }

    @Override
    public String listLpop(String key) throws CacheException {
        return shardRedisCacheClientProxy.listLpop(key);
    }

    @Override
    public String listBlpop(String key, int timeout) throws CacheException {
        return shardRedisCacheClientProxy.listBlpop(key, timeout);
    }

    @Override
    public String listBrpop(String key, int timeout) throws CacheException {
        return shardRedisCacheClientProxy.listBrpop(key, timeout);
    }

    @Override
    public String listRpop(String key) throws CacheException {
        return shardRedisCacheClientProxy.listRpop(key);
    }

    @Override
    public long listLlen(String key) throws CacheException {
        return shardRedisCacheClientProxy.listLlen(key);
    }

    @Override
    public List<String> listLrange(String key, long start, long end) throws CacheException {
        return shardRedisCacheClientProxy.listLrange(key, start, end);
    }

    @Override
    public long listLrem(String key, String value) throws CacheException {
        return shardRedisCacheClientProxy.listLrem(key, value);
    }

    @Override
    public boolean listLtrim(String key, long start, long end) throws CacheException {
        return shardRedisCacheClientProxy.listLtrim(key, start, end);
    }

    @Override
    public Boolean listLset(String key, long index, String value) throws CacheException {
        return shardRedisCacheClientProxy.listLset(key, index, value);
    }

    @Override
    public long setSadd(String key, String... members) throws CacheException {
        return shardRedisCacheClientProxy.setSadd(key, members);
    }

    @Override
    public long setSrem(String key, String... members) throws CacheException {
        return shardRedisCacheClientProxy.setSrem(key, members);
    }

    @Override
    public Set<String> setSmembers(String key) throws CacheException {
        return shardRedisCacheClientProxy.setSmembers(key);
    }

    @Override
    public boolean setSismember(String key, String member) throws CacheException {
        return shardRedisCacheClientProxy.setSismember(key, member);
    }

    @Override
    public long setScard(String key) throws CacheException {
        return shardRedisCacheClientProxy.setScard(key);
    }

    @Override
    public String setSpop(String key) throws CacheException {
        return shardRedisCacheClientProxy.setSpop(key);
    }

    @Override
    public boolean setSmove(String srckey, String dstkey, String member) throws CacheException {
        return shardRedisCacheClientProxy.setSmove(srckey, dstkey, member);
    }

    @Override
    public ScanResult<String> setScan(String key, String cursor) throws CacheException {
        return shardRedisCacheClientProxy.setScan(key, cursor);
    }

    @Override
    public ScanResult<String> setScan(String key, String cursor, ScanParams params) throws CacheException {
        return shardRedisCacheClientProxy.setScan(key, cursor, params);
    }

    @Override
    public long sortSetZadd(String key, String member, double score) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZadd(key, member, score);
    }

    @SuppressWarnings("deprecation")
    @Override
    public long sortSetZadd(String key, Map<Double, String> scoreMembers) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZadd(key, scoreMembers);
    }

    @Override
    public long sortSetZadd2(String key, Map<String, Double> scoreMembers) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZadd2(key, scoreMembers);
    }

    @Override
    public long sortSetZrem(String key, String... members) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZrem(key, members);
    }

    @Override
    public long sortSetZcard(String key) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZcard(key);
    }

    @Override
    public long sortSetZcount(String key, double min, double max) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZcount(key, min, max);
    }

    @Override
    public Double sortSetZscore(String key, String member) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZscore(key, member);
    }

    @Override
    public double sortSetZincrby(String key, String member, double score) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZincrby(key, member, score);
    }

    @Override
    public Set<String> sortSetZrange(String key, long start, long end) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZrange(key, start, end);
    }

    @Override
    public Map<String, Double> sortSetZrangeWithScores(String key, long start, long end) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZrangeWithScores(key, start, end);
    }

    @Override
    public Set<String> sortSetZrevrange(String key, long start, long end) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZrevrange(key, start, end);
    }

    @Override
    public Map<String, Double> sortSetZrevrangeWithScores(String key, long start, long end) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZrevrangeWithScores(key, start, end);
    }

    @Override
    public Long sortSetZrank(String key, String member) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZrank(key, member);
    }

    @Override
    public Long sortSetZrevrank(String key, String member) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZrevrank(key, member);
    }

    @Override
    public Set<String> sortSetZrangeByScore(String key, double min, double max) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZrangeByScore(key, min, max);
    }

    @Override
    public Set<String> sortSetZrangeByScore(String key, double min, double max, int offset, int count)
            throws CacheException {
        return shardRedisCacheClientProxy.sortSetZrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<String> sortSetZrevrangeByScore(String key, double max, double min) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZrevrangeByScore(key, max, min);
    }

    @Override
    public Set<String> sortSetZrevrangeByScore(String key, double max, double min, int offset, int count)
            throws CacheException {
        return shardRedisCacheClientProxy.sortSetZrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public long sortSetZremrangeByRank(String key, long start, long end) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZremrangeByRank(key, start, end);
    }

    @Override
    public ScanResult<Tuple> sortSetZscan(String key, String cursor) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZscan(key, cursor);
    }

    @Override
    public ScanResult<Tuple> sortSetZscan(String key, String cursor, ScanParams params) throws CacheException {
        return shardRedisCacheClientProxy.sortSetZscan(key, cursor, params);
    }

    @Override
    public boolean publish(String channel, String message) throws CacheException {
        throw new UnsupportedOperationException("Sharding Does not support publish");
    }

    @Override
    public void subscribe(AbstractSubscriber subscriber, String... channels) throws CacheException {
        throw new UnsupportedOperationException("Sharding Does not support subscribe");
    }

    @Override
    public void psubscribe(AbstractSubscriber subscriber, String... patterns) throws CacheException {
        throw new UnsupportedOperationException("Sharding Does not support psubscribe");
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
