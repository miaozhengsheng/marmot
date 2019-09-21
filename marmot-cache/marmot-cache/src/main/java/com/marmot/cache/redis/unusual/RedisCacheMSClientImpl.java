package com.marmot.cache.redis.unusual;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Tuple;

import com.marmot.cache.enums.EnumExist;
import com.marmot.cache.enums.EnumTime;
import com.marmot.cache.exception.CacheException;
import com.marmot.cache.redis.pubsub.AbstractSubscriber;

public class RedisCacheMSClientImpl extends  MasterSlaveIntellect implements IRedisCacheClient {

    public RedisCacheMSClientImpl(final IRedisCacheClient master, final IRedisCacheClient slave) throws CacheException {
        this(master, slave, false);
    }

    public RedisCacheMSClientImpl(final IRedisCacheClient master, final IRedisCacheClient slave,
            boolean readWriteSplitting) throws CacheException {
        super(master, slave, readWriteSplitting);
    }

    @Override
    public void shutdown() throws CacheException {
        super.destroy();
        master.shutdown();
        slave.shutdown();
    }

    @Override
    public boolean set(String key, Object value, int expire) throws CacheException {
        writeOpLog(key);
        return master.set(key, value, expire);
    }

    @Override
    public boolean set(String key, Object value) throws CacheException {
        writeOpLog(key);
        return master.set(key, value);
    }

    @Override
    public boolean set(String key, Object value, Date date) throws CacheException {
        writeOpLog(key);
        return master.set(key, value, date);
    }

    @Override
    public Object get(String key) throws CacheException {
        return choose(key, master, slave).get(key);
    }

    @Override
    public boolean delete(String key) throws CacheException {
        writeOpLog(key);
        return master.delete(key);
    }

    @Override
    public Map<String, Object> getMap(Collection<String> keys) throws CacheException {
        return choose(keys, master, slave).getMap(keys);
    }

    @Override
    public boolean setCounter(String key, long num) throws CacheException {
        writeOpLog(key);
        return master.setCounter(key, num);
    }

    @Override
    public boolean setCounter(String key, long num, int expire) throws CacheException {
        writeOpLog(key);
        return master.setCounter(key, num, expire);
    }

    @Override
    public long incr(String key) throws CacheException {
        writeOpLog(key);
        return master.incr(key);
    }

    @Override
    public long incr(String key, long delta) throws CacheException {
        writeOpLog(key);
        return master.incr(key, delta);
    }

    @Override
    public long decr(String key) throws CacheException {
        writeOpLog(key);
        return master.decr(key);
    }

    @Override
    public long decr(String key, long delta) throws CacheException {
        writeOpLog(key);
        return master.decr(key, delta);
    }

    @Override
    public long keyDel(String... keys) throws CacheException {
        writeOpLog(keys);
        return master.keyDel(keys);
    }

    @Override
    public Set<String> keyKeys(String pattern) throws CacheException {
        return slave.keyKeys(pattern);
    }

    @Override
    public boolean keyExists(String key) throws CacheException {
        return master.keyExists(key);
    }

    @Override
    public boolean keyExpire(String key, int seconds) throws CacheException {
        writeOpLog(key);
        return master.keyExpire(key, seconds);
    }

    @Override
    public boolean keyExpireAt(String key, long unixTime) throws CacheException {
        writeOpLog(key);
        return master.keyExpireAt(key, unixTime);
    }

    @Override
    public long keyTtl(String key) throws CacheException {
        return master.keyTtl(key);
    }

    @Override
    public boolean stringSet(String key, String value) throws CacheException {
        writeOpLog(key);
        return master.stringSet(key, value);
    }

    @Override
    public boolean stringSetex(String key, String value, int seconds) throws CacheException {
        writeOpLog(key);
        return master.stringSetex(key, value, seconds);
    }

    @Override
    public boolean stringSet(String key, String value, EnumExist nxxx, EnumTime expx, long time) throws CacheException {
        writeOpLog(key);
        return master.stringSet(key, value, nxxx, expx, time);
    }

    @Override
    public boolean stringSetnx(String key, String value) throws CacheException {
        writeOpLog(key);
        return master.stringSetnx(key, value);
    }

    @Override
    public boolean stringMset(Map<String, String> kvs) throws CacheException {
        writeOpLog(kvs.keySet());
        return master.stringMset(kvs);
    }

    @Override
    public String stringGet(String key) throws CacheException {
        return choose(key, master, slave).stringGet(key);
    }

    @Override
    public Map<String, String> stringMget(String... keys) throws CacheException {
        return choose(keys, master, slave).stringMget(keys);
    }

    @Override
    public String stringGetset(String key, String value) throws CacheException {
        writeOpLog(key);
        return master.stringGetset(key, value);
    }

    @Override
    public boolean hashHset(String key, String field, String value) throws CacheException {
        writeOpLog(key);
        return master.hashHset(key, field, value);
    }

    @Override
    public Boolean hashHsetnx(String key, String field, String value) throws CacheException {
        writeOpLog(key);
        return master.hashHsetnx(key, field, value);
    }

    @Override
    public boolean hashHmset(String key, Map<String, String> hash) throws CacheException {
        writeOpLog(key);
        return master.hashHmset(key, hash);
    }

    @Override
    public String hashHget(String key, String field) throws CacheException {
        return choose(key, master, slave).hashHget(key, field);
    }

    @Override
    public List<String> hashHmget(String key, String... fields) throws CacheException {
        return choose(key, master, slave).hashHmget(key, fields);
    }

    @Override
    public Map<String, String> hashHgetAll(String key) throws CacheException {
        return choose(key, master, slave).hashHgetAll(key);
    }

    @Override
    public long hashHdel(String key, String... fields) throws CacheException {
        writeOpLog(key);
        return master.hashHdel(key, fields);
    }

    @Override
    public long hashHlen(String key) throws CacheException {
        return choose(key, master, slave).hashHlen(key);
    }

    @Override
    public boolean hashHexists(String key, String field) throws CacheException {
        return choose(key, master, slave).hashHexists(key, field);
    }

    @Override
    public long hashHincrBy(String key, String field, long value) throws CacheException {
        writeOpLog(key);
        return master.hashHincrBy(key, field, value);
    }

    @Override
    public Set<String> hashHkeys(String key) throws CacheException {
        return choose(key, master, slave).hashHkeys(key);
    }

    @Override
    public List<String> hashHvals(String key) throws CacheException {
        return choose(key, master, slave).hashHvals(key);
    }

    @Override
    public long listLpush(String key, String... strings) throws CacheException {
        writeOpLog(key);
        return master.listLpush(key, strings);
    }

    @Override
    public long listRpush(String key, String... strings) throws CacheException {
        writeOpLog(key);
        return master.listRpush(key, strings);
    }

    @Override
    public String listLpop(String key) throws CacheException {
        writeOpLog(key);
        return master.listLpop(key);
    }

    @Override
    public String listBlpop(String key, int timeout) throws CacheException {
        writeOpLog(key);
        return master.listBlpop(key, timeout);
    }

    @Override
    public String listBrpop(String key, int timeout) throws CacheException {
        writeOpLog(key);
        return master.listBrpop(key, timeout);
    }

    @Override
    public String listRpop(String key) throws CacheException {
        writeOpLog(key);
        return master.listRpop(key);
    }

    @Override
    public long listLlen(String key) throws CacheException {
        return choose(key, master, slave).listLlen(key);
    }

    @Override
    public List<String> listLrange(String key, long start, long end) throws CacheException {
        return choose(key, master, slave).listLrange(key, start, end);
    }

    @Override
    public long listLrem(String key, String value) throws CacheException {
        writeOpLog(key);
        return master.listLrem(key, value);
    }

    @Override
    public boolean listLtrim(String key, long start, long end) throws CacheException {
        writeOpLog(key);
        return master.listLtrim(key, start, end);
    }

    @Override
    public Boolean listLset(String key, long index, String value) throws CacheException {
        writeOpLog(key);
        return master.listLset(key, index, value);
    }

    @Override
    public long setSadd(String key, String... members) throws CacheException {
        writeOpLog(key);
        return master.setSadd(key, members);
    }

    @Override
    public long setSrem(String key, String... members) throws CacheException {
        writeOpLog(key);
        return master.setSrem(key, members);
    }

    @Override
    public Set<String> setSmembers(String key) throws CacheException {
        return choose(key, master, slave).setSmembers(key);
    }

    @Override
    public boolean setSismember(String key, String member) throws CacheException {
        return choose(key, master, slave).setSismember(key, member);
    }

    @Override
    public long setScard(String key) throws CacheException {
        return choose(key, master, slave).setScard(key);
    }

    @Override
    public String setSpop(String key) throws CacheException {
        writeOpLog(key);
        return master.setSpop(key);
    }

    @Override
    public boolean setSmove(String srckey, String dstkey, String member) throws CacheException {
        writeOpLog(srckey);
        writeOpLog(dstkey);
        return master.setSmove(srckey, dstkey, member);
    }

    @Override
    public ScanResult<String> setScan(String key, String cursor) throws CacheException {
        return choose(key, master, slave).setScan(key, cursor);
    }

    @Override
    public ScanResult<String> setScan(String key, String cursor, ScanParams params) throws CacheException {
        return choose(key, master, slave).setScan(key, cursor, params);
    }

    @Override
    public long sortSetZadd(String key, String member, double score) throws CacheException {
        writeOpLog(key);
        return master.sortSetZadd(key, member, score);
    }

    @SuppressWarnings("deprecation")
    @Override
    public long sortSetZadd(String key, Map<Double, String> scoreMembers) throws CacheException {
        writeOpLog(key);
        return master.sortSetZadd(key, scoreMembers);
    }

    @Override
    public long sortSetZadd2(String key, Map<String, Double> scoreMembers) throws CacheException {
        writeOpLog(key);
        return master.sortSetZadd2(key, scoreMembers);
    }

    @Override
    public long sortSetZrem(String key, String... members) throws CacheException {
        writeOpLog(key);
        return master.sortSetZrem(key, members);
    }

    @Override
    public long sortSetZcard(String key) throws CacheException {
        return choose(key, master, slave).sortSetZcard(key);
    }

    @Override
    public long sortSetZcount(String key, double min, double max) throws CacheException {
        return choose(key, master, slave).sortSetZcount(key, min, max);
    }

    @Override
    public Double sortSetZscore(String key, String member) throws CacheException {
        return choose(key, master, slave).sortSetZscore(key, member);
    }

    @Override
    public double sortSetZincrby(String key, String member, double score) throws CacheException {
        writeOpLog(key);
        return master.sortSetZincrby(key, member, score);
    }

    @Override
    public Set<String> sortSetZrange(String key, long start, long end) throws CacheException {
        return choose(key, master, slave).sortSetZrange(key, start, end);
    }

    @Override
    public Map<String, Double> sortSetZrangeWithScores(String key, long start, long end) throws CacheException {
        return choose(key, master, slave).sortSetZrangeWithScores(key, start, end);
    }

    @Override
    public Set<String> sortSetZrevrange(String key, long start, long end) throws CacheException {
        return choose(key, master, slave).sortSetZrevrange(key, start, end);
    }

    @Override
    public Map<String, Double> sortSetZrevrangeWithScores(String key, long start, long end) throws CacheException {
        return choose(key, master, slave).sortSetZrevrangeWithScores(key, start, end);
    }

    @Override
    public Long sortSetZrank(String key, String member) throws CacheException {
        return choose(key, master, slave).sortSetZrank(key, member);
    }

    @Override
    public Long sortSetZrevrank(String key, String member) throws CacheException {
        return choose(key, master, slave).sortSetZrevrank(key, member);
    }

    @Override
    public Set<String> sortSetZrangeByScore(String key, double min, double max) throws CacheException {
        return choose(key, master, slave).sortSetZrangeByScore(key, min, max);
    }

    @Override
    public Set<String> sortSetZrangeByScore(String key, double min, double max, int offset, int count)
            throws CacheException {
        return choose(key, master, slave).sortSetZrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<String> sortSetZrevrangeByScore(String key, double max, double min) throws CacheException {
        return choose(key, master, slave).sortSetZrevrangeByScore(key, max, min);
    }

    @Override
    public Set<String> sortSetZrevrangeByScore(String key, double max, double min, int offset, int count)
            throws CacheException {
        return choose(key, master, slave).sortSetZrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public long sortSetZremrangeByRank(String key, long start, long end) throws CacheException {
        writeOpLog(key);
        return master.sortSetZremrangeByRank(key, start, end);
    }

    @Override
    public ScanResult<Tuple> sortSetZscan(String key, String cursor) throws CacheException {
        return choose(key, master, slave).sortSetZscan(key, cursor);
    }

    @Override
    public ScanResult<Tuple> sortSetZscan(String key, String cursor, ScanParams params) throws CacheException {
        return choose(key, master, slave).sortSetZscan(key, cursor, params);
    }

    @Override
    public boolean publish(String channel, String message) throws CacheException {
        return master.publish(channel, message);
    }

    @Override
    public void subscribe(final AbstractSubscriber subscriber, final String... channels) throws CacheException {
        master.subscribe(subscriber, channels);
    }

    @Override
    public void psubscribe(AbstractSubscriber subscriber, String... patterns) throws CacheException {
        master.psubscribe(subscriber, patterns);
    }
}
