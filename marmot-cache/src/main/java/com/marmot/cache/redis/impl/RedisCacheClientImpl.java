package com.marmot.cache.redis.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Tuple;

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
import com.marmot.cache.redis.RedisContext;
import com.marmot.cache.redis.pubsub.AbstractSubscriber;
import com.marmot.cache.redis.pubsub.SubscribeThread;
import com.marmot.cache.utils.FailureCollecter;
import com.marmot.cache.utils.RedisSentinel;
import com.marmot.cache.utils.ToolUtil;
import com.marmot.cache.utils.ToolUtil.Handle;
import com.marmot.common.system.SystemUtil;
import com.marmot.common.util.ThreadLocalUtil;

public class RedisCacheClientImpl implements IRedisCacheClient, IRedisReload<String>, IKeyShard, IBlockHandle<Jedis> {

    private static final Logger logger = Logger.getLogger(RedisCacheClientImpl.class);

    private final IRedisCacheClient redisCacheClientProxy;

    private final RedisContext redisContext;

    private final AtomicReference<JedisPool> atomicJedisPoolReference;

    private final Set<Jedis> blockedJedis = Collections.synchronizedSet(new HashSet<Jedis>());
    private final AtomicBoolean reloading = new AtomicBoolean(false);// 切换状态
    private final Lock lock = new ReentrantLock();

    private Cluster cluster = Cluster.master;

    private static ConcurrentMap<Method, Method> methodMap = new ConcurrentHashMap<Method, Method>();
    private final JedisPoolConfig jpc;
    private final int soTimeout;
    private String host;
    private int port;
    private volatile boolean shutdown = false;

    public RedisCacheClientImpl(String host, int port, String password, int defaultExpire, int soTimeout,
            boolean transcode, JedisPoolConfig jpc) {
        this(host, port, password, defaultExpire, soTimeout, transcode, jpc, Cluster.master);
    }

    public RedisCacheClientImpl(String host, int port, String password, int defaultExpire, int soTimeout,
            boolean transcode, JedisPoolConfig jpc, Cluster cluster) {
        this.host = host;
        this.port = port;
        this.jpc = jpc;
        this.soTimeout = soTimeout;
        this.atomicJedisPoolReference = new AtomicReference<JedisPool>(new JedisPool(jpc, host, port,
                RedisCacheClientFactory.CONNECTION_TIMEOUT, soTimeout, password, Protocol.DEFAULT_DATABASE, null));
        RedisSentinel.getInstance().watch(host, port, password);
        this.redisContext = new RedisContext(defaultExpire, transcode);
        this.redisCacheClientProxy = (IRedisCacheClient) Proxy.newProxyInstance(
                IRedisCacheClient.class.getClassLoader(), new Class[] { IRedisCacheClient.class },
                new RedisCacheClientInterceptor());
        this.cluster = cluster;
        logger.info("Redis client[" + host + ":" + port + "] starting succeed");
    }

    @Override
    public void reload(String servers, Set<String> instances, final String password) {
        String[] serversArr = servers.split(",");
        String server = serversArr[0];
        final String[] ipAndPort = server.split(":");

        StopWatch stopWatch = new StopWatch("JedisPool reload");
        stopWatch.start("reload new");

        lock.lock();
        try {
            // 加锁
            reloading.getAndSet(true);
            ToolUtil.retry(3, 500, new Handle() {

                @Override
                public void invoke() throws Throwable {
                    RedisSentinel.getInstance().watch(ipAndPort[0], Integer.parseInt(ipAndPort[1]), password);
                }

            });

            JedisPool oldJedisPool = atomicJedisPoolReference.getAndSet(new JedisPool(jpc, ipAndPort[0],
                    Integer.parseInt(ipAndPort[1]), RedisCacheClientFactory.CONNECTION_TIMEOUT, soTimeout, password,
                    Protocol.DEFAULT_DATABASE, null));

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
            if (!instances.contains(this.host + ":" + this.port)) {
                RedisSentinel.getInstance().unwatch(this.host, this.port);
            }
            this.host = ipAndPort[0];
            this.port = Integer.parseInt(ipAndPort[1]);
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

    private class RedisCacheClientInterceptor implements InvocationHandler {

        private Method oppositeMethod(final Method method) {
            Method oppMethod = methodMap.get(method);
            if (oppMethod == null) {
                String name = method.getName();
                Class<?>[] pTypes = method.getParameterTypes();
                Class<?>[] _pTypes = new Class<?>[pTypes.length + 1];
                _pTypes[0] = Jedis.class;
                System.arraycopy(pTypes, 0, _pTypes, 1, pTypes.length);
                try {
                    oppMethod = RedisContext.class.getDeclaredMethod(name, _pTypes);
                } catch (Exception e) {
                    throw new RuntimeException("RedisContext " + name + " method absent", e);
                }
                methodMap.put(method, oppMethod);
            }
            return oppMethod;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Jedis jedis = null;
            Object result = null;
            String params = Arrays.toString(args);

            Transaction transaction = Cat.newTransaction("Cache.redis", method.getName());
            transaction.addData("args", params);
            Cat.logEvent("Cache.redis.cluster", cluster.name());
            long createTime = System.currentTimeMillis();
            try {
                jedis = atomicJedisPoolReference.get().getResource();
                // 链接是否正常校验
                if (RedisSentinel.getInstance().valid(jedis)) {
                    // 监控堵塞的方法
                    if (isBlock(method.getName())) {
                        used(jedis);
                    }
                    Object[] _args = new Object[args.length + 1];
                    _args[0] = jedis;
                    System.arraycopy(args, 0, _args, 1, args.length);
                    result = oppositeMethod(method).invoke(redisContext, _args);
                }
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
                        address = jedis.getClient().getHost() + ":" + jedis.getClient().getPort();
                        logger.error(prefix + "IRedisCacheClient Method=[" + method.getName() + "] Params=" + params
                                + " fail on redis server=[" + address + "]", actual);
                    } else {
                        logger.error(prefix + "IRedisCacheClient Method=[" + method.getName() + "] Params=" + params
                                + " fail", actual);
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
                    logger.error("IRedisCacheClient Method=[" + method.getName() + "] Params=" + params
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
        return new Shard(host, port);
    }

    @Override
    public void shutdown() {
        lock.lock();
        try {
            // 加锁
            reloading.getAndSet(true);
            this.shutdown = true;
            atomicJedisPoolReference.get().destroy();
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
        return redisCacheClientProxy.set(key, value, expire);
    }

    @Override
    public Boolean set(String key, Object value) {
        return redisCacheClientProxy.set(key, value);
    }

    @Override
    public Boolean set(String key, Object value, Date date) {
        return redisCacheClientProxy.set(key, value, date);
    }

    @Override
    public Object get(String key) {
        return redisCacheClientProxy.get(key);
    }

    @Override
    public Boolean delete(String key) {
        return redisCacheClientProxy.delete(key);
    }

    @Override
    public Map<String, Object> getMap(Collection<String> keys) {
        return redisCacheClientProxy.getMap(keys);
    }

    @Override
    public Boolean setCounter(String key, long num) {
        return redisCacheClientProxy.setCounter(key, num);
    }

    @Override
    public Boolean setCounter(String key, long num, int expire) {
        return redisCacheClientProxy.setCounter(key, num, expire);
    }

    @Override
    public Long incr(String key) {
        return redisCacheClientProxy.incr(key);
    }

    @Override
    public Long incr(String key, long delta) {
        return redisCacheClientProxy.incr(key, delta);
    }

    @Override
    public Long decr(String key) {
        return redisCacheClientProxy.decr(key);
    }

    @Override
    public Long decr(String key, long delta) {
        return redisCacheClientProxy.decr(key, delta);
    }

    @Override
    public Long keyDel(String... keys) {
        return redisCacheClientProxy.keyDel(keys);
    }

    @Override
    public Set<String> keyKeys(String pattern) {
        return redisCacheClientProxy.keyKeys(pattern);
    }

    @Override
    public Boolean keyExists(String key) {
        return redisCacheClientProxy.keyExists(key);
    }

    @Override
    public Boolean keyExpire(String key, int seconds) {
        return redisCacheClientProxy.keyExpire(key, seconds);
    }

    @Override
    public Boolean keyExpireAt(String key, long unixTime) {
        return redisCacheClientProxy.keyExpireAt(key, unixTime);
    }

    @Override
    public Long keyTtl(String key) {
        return redisCacheClientProxy.keyTtl(key);
    }

    @Override
    public Boolean stringSet(String key, String value) {
        return redisCacheClientProxy.stringSet(key, value);
    }

    @Override
    public Boolean stringSetex(String key, String value, int seconds) {
        return redisCacheClientProxy.stringSetex(key, value, seconds);
    }

    @Override
    public Boolean stringSet(String key, String value, EnumExist nxxx, EnumTime expx, long time) {
        return redisCacheClientProxy.stringSet(key, value, nxxx, expx, time);
    }

    @Override
    public Boolean stringSetnx(String key, String value) {
        return redisCacheClientProxy.stringSetnx(key, value);
    }

    @Override
    public Boolean stringMset(Map<String, String> kvs) {
        return redisCacheClientProxy.stringMset(kvs);
    }

    @Override
    public String stringGet(String key) {
        return redisCacheClientProxy.stringGet(key);
    }

    @Override
    public Map<String, String> stringMget(String... keys) {
        return redisCacheClientProxy.stringMget(keys);
    }

    @Override
    public String stringGetset(String key, String value) {
        return redisCacheClientProxy.stringGetset(key, value);
    }

    @Override
    public Boolean hashHset(String key, String field, String value) {
        return redisCacheClientProxy.hashHset(key, field, value);
    }

    @Override
    public Boolean hashHsetnx(String key, String field, String value) {
        return redisCacheClientProxy.hashHsetnx(key, field, value);
    }

    @Override
    public Boolean hashHmset(String key, Map<String, String> hash) {
        return redisCacheClientProxy.hashHmset(key, hash);
    }

    @Override
    public String hashHget(String key, String field) {
        return redisCacheClientProxy.hashHget(key, field);
    }

    @Override
    public List<String> hashHmget(String key, String... fields) {
        return redisCacheClientProxy.hashHmget(key, fields);
    }

    @Override
    public Map<String, String> hashHgetAll(String key) {
        return redisCacheClientProxy.hashHgetAll(key);
    }

    @Override
    public Long hashHdel(String key, String... fields) {
        return redisCacheClientProxy.hashHdel(key, fields);
    }

    @Override
    public Long hashHlen(String key) {
        return redisCacheClientProxy.hashHlen(key);
    }

    @Override
    public Boolean hashHexists(String key, String field) {
        return redisCacheClientProxy.hashHexists(key, field);
    }

    @Override
    public Long hashHincrBy(String key, String field, long value) {
        return redisCacheClientProxy.hashHincrBy(key, field, value);
    }

    @Override
    public Set<String> hashHkeys(String key) {
        return redisCacheClientProxy.hashHkeys(key);
    }

    @Override
    public List<String> hashHvals(String key) {
        return redisCacheClientProxy.hashHvals(key);
    }

    @Override
    public Long listLpush(String key, String... strings) {
        return redisCacheClientProxy.listLpush(key, strings);
    }

    @Override
    public Long listRpush(String key, String... strings) {
        return redisCacheClientProxy.listRpush(key, strings);
    }

    @Override
    public String listLpop(String key) {
        return redisCacheClientProxy.listLpop(key);
    }

    @Override
    public String listBlpop(String key, int timeout) {
        return redisCacheClientProxy.listBlpop(key, timeout);
    }

    @Override
    public String listBrpop(String key, int timeout) {
        return redisCacheClientProxy.listBrpop(key, timeout);
    }

    @Override
    public String listRpop(String key) {
        return redisCacheClientProxy.listRpop(key);
    }

    @Override
    public Long listLlen(String key) {
        return redisCacheClientProxy.listLlen(key);
    }

    @Override
    public List<String> listLrange(String key, long start, long end) {
        return redisCacheClientProxy.listLrange(key, start, end);
    }

    @Override
    public Long listLrem(String key, String value) {
        return redisCacheClientProxy.listLrem(key, value);
    }

    @Override
    public Boolean listLtrim(String key, long start, long end) {
        return redisCacheClientProxy.listLtrim(key, start, end);
    }

    @Override
    public Boolean listLset(String key, long index, String value) {
        return redisCacheClientProxy.listLset(key, index, value);
    }

    @Override
    public Long setSadd(String key, String... members) {
        return redisCacheClientProxy.setSadd(key, members);
    }

    @Override
    public Long setSrem(String key, String... members) {
        return redisCacheClientProxy.setSrem(key, members);
    }

    @Override
    public Set<String> setSmembers(String key) {
        return redisCacheClientProxy.setSmembers(key);
    }

    @Override
    public Boolean setSismember(String key, String member) {
        return redisCacheClientProxy.setSismember(key, member);
    }

    @Override
    public Long setScard(String key) {
        return redisCacheClientProxy.setScard(key);
    }

    @Override
    public String setSpop(String key) {
        return redisCacheClientProxy.setSpop(key);
    }

    @Override
    public Boolean setSmove(String srckey, String dstkey, String member) {
        return redisCacheClientProxy.setSmove(srckey, dstkey, member);
    }

    @Override
    public ScanResult<String> setScan(String key, String cursor) {
        return redisCacheClientProxy.setScan(key, cursor);
    }

    @Override
    public ScanResult<String> setScan(String key, String cursor, ScanParams params) {
        return redisCacheClientProxy.setScan(key, cursor, params);
    }

    @Override
    public Long sortSetZadd(String key, String member, double score) {
        return redisCacheClientProxy.sortSetZadd(key, member, score);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Long sortSetZadd(String key, Map<Double, String> scoreMembers) {
        return redisCacheClientProxy.sortSetZadd(key, scoreMembers);
    }

    @Override
    public Long sortSetZadd2(String key, Map<String, Double> scoreMembers) {
        return redisCacheClientProxy.sortSetZadd2(key, scoreMembers);
    }

    @Override
    public Long sortSetZrem(String key, String... members) {
        return redisCacheClientProxy.sortSetZrem(key, members);
    }

    @Override
    public Long sortSetZcard(String key) {
        return redisCacheClientProxy.sortSetZcard(key);
    }

    @Override
    public Long sortSetZcount(String key, double min, double max) {
        return redisCacheClientProxy.sortSetZcount(key, min, max);
    }

    @Override
    public Double sortSetZscore(String key, String member) {
        return redisCacheClientProxy.sortSetZscore(key, member);
    }

    @Override
    public Double sortSetZincrby(String key, String member, double score) {
        return redisCacheClientProxy.sortSetZincrby(key, member, score);
    }

    @Override
    public Set<String> sortSetZrange(String key, long start, long end) {
        return redisCacheClientProxy.sortSetZrange(key, start, end);
    }

    @Override
    public Map<String, Double> sortSetZrangeWithScores(String key, long start, long end) {
        return redisCacheClientProxy.sortSetZrangeWithScores(key, start, end);
    }

    @Override
    public Set<String> sortSetZrevrange(String key, long start, long end) {
        return redisCacheClientProxy.sortSetZrevrange(key, start, end);
    }

    @Override
    public Map<String, Double> sortSetZrevrangeWithScores(String key, long start, long end) {
        return redisCacheClientProxy.sortSetZrevrangeWithScores(key, start, end);
    }

    @Override
    public Long sortSetZrank(String key, String member) {
        return redisCacheClientProxy.sortSetZrank(key, member);
    }

    @Override
    public Long sortSetZrevrank(String key, String member) {
        return redisCacheClientProxy.sortSetZrevrank(key, member);
    }

    @Override
    public Set<String> sortSetZrangeByScore(String key, double min, double max) {
        return redisCacheClientProxy.sortSetZrangeByScore(key, min, max);
    }

    @Override
    public Set<String> sortSetZrangeByScore(String key, double min, double max, int offset, int count) {
        return redisCacheClientProxy.sortSetZrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<String> sortSetZrevrangeByScore(String key, double max, double min) {
        return redisCacheClientProxy.sortSetZrevrangeByScore(key, max, min);
    }

    @Override
    public Set<String> sortSetZrevrangeByScore(String key, double max, double min, int offset, int count) {
        return redisCacheClientProxy.sortSetZrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Long sortSetZremrangeByRank(String key, long start, long end) {
        return redisCacheClientProxy.sortSetZremrangeByRank(key, start, end);
    }

    @Override
    public ScanResult<Tuple> sortSetZscan(String key, String cursor) {
        return redisCacheClientProxy.sortSetZscan(key, cursor);
    }

    @Override
    public ScanResult<Tuple> sortSetZscan(String key, String cursor, ScanParams params) {
        return redisCacheClientProxy.sortSetZscan(key, cursor, params);
    }

    @Override
    public Boolean publish(String channel, String message) {
        return redisCacheClientProxy.publish(channel, message);
    }

    @Override
    public void subscribe(final AbstractSubscriber subscriber, final String... channels) {
        SubscribeThread thread = new SubscribeThread(atomicJedisPoolReference, this, subscriber, false, channels);
        thread.start();
    }

    @Override
    public void psubscribe(final AbstractSubscriber subscriber, final String... patterns) {
        SubscribeThread thread = new SubscribeThread(atomicJedisPoolReference, this, subscriber, true, patterns);
        thread.start();
    }

    @Override
    public void used(Jedis t) {
        tryLock();
        blockedJedis.add(t);
    }

    @Override
    public void release(Jedis t) {
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
        for (Jedis jedis : blockedJedis) {
            try {
                jedis.getClient().close();
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
