package com.marmot.cache.redis.impl;

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
import com.marmot.cache.redis.IRedisCacheClient;
import com.marmot.cache.redis.ms.MasterSlaveIntellect;
import com.marmot.cache.redis.pubsub.AbstractSubscriber;

public class ShardRedisCacheMSClientImpl extends MasterSlaveIntellect implements IRedisCacheClient {

    public ShardRedisCacheMSClientImpl(final IRedisCacheClient master, final IRedisCacheClient slave) {
        this(master, slave, false);
    }

    public ShardRedisCacheMSClientImpl(final IRedisCacheClient master, final IRedisCacheClient slave,
            boolean readWriteSplitting) {
        super(master, slave, readWriteSplitting);
    }

    @Override
    public void shutdown() {
        super.destroy();
        master.shutdown();
        slave.shutdown();
    }

    @Override
    public Boolean set(String key, Object value, int expire) {
        writeOpLog(key);
        return master.set(key, value, expire);
    }

    @Override
    public Boolean set(String key, Object value) {
        writeOpLog(key);
        return master.set(key, value);
    }

    @Override
    public Boolean set(String key, Object value, Date date) {
        writeOpLog(key);
        return master.set(key, value, date);
    }

    @Override
    public Object get(String key) {
        return choose(key, master, slave).get(key);
    }

    @Override
    public Boolean delete(String key) {
        writeOpLog(key);
        return master.delete(key);
    }

    @Override
    public Map<String, Object> getMap(Collection<String> keys) {
        return choose(keys, master, slave).getMap(keys);
    }

    @Override
    public Boolean setCounter(String key, long num) {
        writeOpLog(key);
        return master.setCounter(key, num);
    }

    @Override
    public Boolean setCounter(String key, long num, int expire) {
        writeOpLog(key);
        return master.setCounter(key, num, expire);
    }

    @Override
    public Long incr(String key) {
        writeOpLog(key);
        return master.incr(key);
    }

    @Override
    public Long incr(String key, long delta) {
        writeOpLog(key);
        return master.incr(key, delta);
    }

    @Override
    public Long decr(String key) {
        writeOpLog(key);
        return master.decr(key);
    }

    @Override
    public Long decr(String key, long delta) {
        writeOpLog(key);
        return master.decr(key, delta);
    }

    @Override
    public Long keyDel(String... keys) {
        writeOpLog(keys);
        return master.keyDel(keys);
    }

    @Override
    public Set<String> keyKeys(String pattern) {
        return slave.keyKeys(pattern);
    }

    @Override
    public Boolean keyExists(String key) {
        // 注意：选择主库避免主从延时影响
        return master.keyExists(key);
    }

    @Override
    public Boolean keyExpire(String key, int seconds) {
        writeOpLog(key);
        return master.keyExpire(key, seconds);
    }

    @Override
    public Boolean keyExpireAt(String key, long unixTime) {
        writeOpLog(key);
        return master.keyExpireAt(key, unixTime);
    }

    @Override
    public Long keyTtl(String key) {
        return choose(key, master, slave).keyTtl(key);
    }

    @Override
    public Boolean stringSet(String key, String value) {
        writeOpLog(key);
        return master.stringSet(key, value);
    }

    @Override
    public Boolean stringSetex(String key, String value, int seconds) {
        writeOpLog(key);
        return master.stringSetex(key, value, seconds);
    }

    @Override
    public Boolean stringSet(String key, String value, EnumExist nxxx, EnumTime expx, long time) {
        writeOpLog(key);
        return master.stringSet(key, value, nxxx, expx, time);
    }

    @Override
    public Boolean stringSetnx(String key, String value) {
        writeOpLog(key);
        return master.stringSetnx(key, value);
    }

    @Override
    public Boolean stringMset(Map<String, String> kvs) {
        writeOpLog(kvs.keySet());
        return master.stringMset(kvs);
    }

    @Override
    public String stringGet(String key) {
        return choose(key, master, slave).stringGet(key);
    }

    @Override
    public Map<String, String> stringMget(String... keys) {
        return choose(keys, master, slave).stringMget(keys);
    }

    @Override
    public String stringGetset(String key, String value) {
        writeOpLog(key);
        return master.stringGetset(key, value);
    }

    @Override
    public Boolean hashHset(String key, String field, String value) {
        writeOpLog(key);
        return master.hashHset(key, field, value);
    }

    @Override
    public Boolean hashHsetnx(String key, String field, String value) {
        writeOpLog(key);
        return master.hashHsetnx(key, field, value);
    }

    @Override
    public Boolean hashHmset(String key, Map<String, String> hash) {
        writeOpLog(key);
        return master.hashHmset(key, hash);
    }

    @Override
    public String hashHget(String key, String field) {
        return choose(key, master, slave).hashHget(key, field);
    }

    @Override
    public List<String> hashHmget(String key, String... fields) {
        return choose(key, master, slave).hashHmget(key, fields);
    }

    @Override
    public Map<String, String> hashHgetAll(String key) {
        return choose(key, master, slave).hashHgetAll(key);
    }

    @Override
    public Long hashHdel(String key, String... fields) {
        writeOpLog(key);
        return master.hashHdel(key, fields);
    }

    @Override
    public Long hashHlen(String key) {
        return choose(key, master, slave).hashHlen(key);
    }

    @Override
    public Boolean hashHexists(String key, String field) {
        return choose(key, master, slave).hashHexists(key, field);
    }

    @Override
    public Long hashHincrBy(String key, String field, long value) {
        writeOpLog(key);
        return master.hashHincrBy(key, field, value);
    }

    @Override
    public Set<String> hashHkeys(String key) {
        return choose(key, master, slave).hashHkeys(key);
    }

    @Override
    public List<String> hashHvals(String key) {
        return choose(key, master, slave).hashHvals(key);
    }

    @Override
    public Long listLpush(String key, String... strings) {
        writeOpLog(key);
        return master.listLpush(key, strings);
    }

    @Override
    public Long listRpush(String key, String... strings) {
        writeOpLog(key);
        return master.listRpush(key, strings);
    }

    @Override
    public String listLpop(String key) {
        writeOpLog(key);
        return master.listLpop(key);
    }

    @Override
    public String listBlpop(String key, int timeout) {
        writeOpLog(key);
        return master.listBlpop(key, timeout);
    }

    @Override
    public String listBrpop(String key, int timeout) {
        writeOpLog(key);
        return master.listBrpop(key, timeout);
    }

    @Override
    public String listRpop(String key) {
        writeOpLog(key);
        return master.listRpop(key);
    }

    @Override
    public Long listLlen(String key) {
        return choose(key, master, slave).listLlen(key);
    }

    @Override
    public List<String> listLrange(String key, long start, long end) {
        return choose(key, master, slave).listLrange(key, start, end);
    }

    @Override
    public Long listLrem(String key, String value) {
        writeOpLog(key);
        return master.listLrem(key, value);
    }

    @Override
    public Boolean listLtrim(String key, long start, long end) {
        writeOpLog(key);
        return master.listLtrim(key, start, end);
    }

    @Override
    public Boolean listLset(String key, long index, String value) {
        writeOpLog(key);
        return master.listLset(key, index, value);
    }

    @Override
    public Long setSadd(String key, String... members) {
        writeOpLog(key);
        return master.setSadd(key, members);
    }

    @Override
    public Long setSrem(String key, String... members) {
        writeOpLog(key);
        return master.setSrem(key, members);
    }

    @Override
    public Set<String> setSmembers(String key) {
        return choose(key, master, slave).setSmembers(key);
    }

    @Override
    public Boolean setSismember(String key, String member) {
        return choose(key, master, slave).setSismember(key, member);
    }

    @Override
    public Long setScard(String key) {
        return choose(key, master, slave).setScard(key);
    }

    @Override
    public String setSpop(String key) {
        writeOpLog(key);
        return master.setSpop(key);
    }

    @Override
    public Boolean setSmove(String srckey, String dstkey, String member) {
        writeOpLog(srckey);
        writeOpLog(dstkey);
        return master.setSmove(srckey, dstkey, member);
    }

    @Override
    public ScanResult<String> setScan(String key, String cursor) {
        return choose(key, master, slave).setScan(key, cursor);
    }

    @Override
    public ScanResult<String> setScan(String key, String cursor, ScanParams params) {
        return choose(key, master, slave).setScan(key, cursor, params);
    }

    @Override
    public Long sortSetZadd(String key, String member, double score) {
        writeOpLog(key);
        return master.sortSetZadd(key, member, score);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Long sortSetZadd(String key, Map<Double, String> scoreMembers) {
        writeOpLog(key);
        return master.sortSetZadd(key, scoreMembers);
    }

    @Override
    public Long sortSetZadd2(String key, Map<String, Double> scoreMembers) {
        writeOpLog(key);
        return master.sortSetZadd2(key, scoreMembers);
    }

    @Override
    public Long sortSetZrem(String key, String... members) {
        writeOpLog(key);
        return master.sortSetZrem(key, members);
    }

    @Override
    public Long sortSetZcard(String key) {
        return choose(key, master, slave).sortSetZcard(key);
    }

    @Override
    public Long sortSetZcount(String key, double min, double max) {
        return choose(key, master, slave).sortSetZcount(key, min, max);
    }

    @Override
    public Double sortSetZscore(String key, String member) {
        return choose(key, master, slave).sortSetZscore(key, member);
    }

    @Override
    public Double sortSetZincrby(String key, String member, double score) {
        writeOpLog(key);
        return master.sortSetZincrby(key, member, score);
    }

    @Override
    public Set<String> sortSetZrange(String key, long start, long end) {
        return choose(key, master, slave).sortSetZrange(key, start, end);
    }

    @Override
    public Map<String, Double> sortSetZrangeWithScores(String key, long start, long end) {
        return choose(key, master, slave).sortSetZrangeWithScores(key, start, end);
    }

    @Override
    public Set<String> sortSetZrevrange(String key, long start, long end) {
        return choose(key, master, slave).sortSetZrevrange(key, start, end);
    }

    @Override
    public Map<String, Double> sortSetZrevrangeWithScores(String key, long start, long end) {
        return choose(key, master, slave).sortSetZrevrangeWithScores(key, start, end);
    }

    @Override
    public Long sortSetZrank(String key, String member) {
        return choose(key, master, slave).sortSetZrank(key, member);
    }

    @Override
    public Long sortSetZrevrank(String key, String member) {
        return choose(key, master, slave).sortSetZrevrank(key, member);
    }

    @Override
    public Set<String> sortSetZrangeByScore(String key, double min, double max) {
        return choose(key, master, slave).sortSetZrangeByScore(key, min, max);
    }

    @Override
    public Set<String> sortSetZrangeByScore(String key, double min, double max, int offset, int count) {
        return choose(key, master, slave).sortSetZrangeByScore(key, min, max, offset, count);
    }

    @Override
    public Set<String> sortSetZrevrangeByScore(String key, double max, double min) {
        return choose(key, master, slave).sortSetZrevrangeByScore(key, max, min);
    }

    @Override
    public Set<String> sortSetZrevrangeByScore(String key, double max, double min, int offset, int count) {
        return choose(key, master, slave).sortSetZrevrangeByScore(key, max, min, offset, count);
    }

    @Override
    public Long sortSetZremrangeByRank(String key, long start, long end) {
        writeOpLog(key);
        return master.sortSetZremrangeByRank(key, start, end);
    }

    @Override
    public ScanResult<Tuple> sortSetZscan(String key, String cursor) {
        return choose(key, master, slave).sortSetZscan(key, cursor);
    }

    @Override
    public ScanResult<Tuple> sortSetZscan(String key, String cursor, ScanParams params) {
        return choose(key, master, slave).sortSetZscan(key, cursor, params);
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
    public void psubscribe(AbstractSubscriber subscriber, String... patterns) {
        throw new UnsupportedOperationException("Sharding Does not support psubscribe");
    }

}
