package com.marmot.cache.redis;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;





import com.marmot.cache.enums.EnumExist;
import com.marmot.cache.enums.EnumTime;
import com.marmot.cache.utils.CollectionUtil;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Tuple;
public class RedisContext {


    private Converter<Object> serializingTranscoder;
    private Converter<String> stringConverter = new StringConverter();

    public static final String STATUS_CODE_REPLY = "OK";
    public static final int PERSIST = 0;

    private int defaultExpire;

    public RedisContext(int expire, boolean transcode) {
        this.defaultExpire = expire;
        if (transcode) {
            serializingTranscoder = new SerializingConverter();
        } else {
            serializingTranscoder = new ObjectConverter();
        }
    }

    private static Boolean isOK(String ret) {
        return (ret == null || !ret.equals(STATUS_CODE_REPLY)) ? Boolean.FALSE : Boolean.TRUE;
    }

    private static Boolean isOK(Long ret) {
        return (ret == null || ret.longValue() != 1) ? Boolean.FALSE : Boolean.TRUE;
    }

    public Boolean set(Jedis jedis, String key, Object value, int expire) throws Exception {
        String sc = null;
        if (expire == PERSIST) {
            sc = jedis.set(stringConverter.serialize(key), serializingTranscoder.serialize(value));
        } else {
            sc = jedis.setex(stringConverter.serialize(key), expire, serializingTranscoder.serialize(value));
        }
        return isOK(sc);
    }



    public Boolean set(Jedis jedis, String key, Object value) throws Exception {
        return set(jedis, key, value, defaultExpire);
    }

    public Boolean set(Jedis jedis, String key, Object value, Date date) throws Exception {
        int expire = defaultExpire;
        if (date != null) {
            Long l = new Long((date.getTime() - System.currentTimeMillis()) / 1000L);
            expire = l.intValue();
        }
        return set(jedis, key, value, expire);
    }

    public Object get(Jedis jedis, String key) throws Exception {
        Object obj = null;
        byte[] bytes = jedis.get(stringConverter.serialize(key));
        if (bytes != null) {
            obj = serializingTranscoder.deserialize(bytes);
        }
        return obj;
    }

    public Boolean delete(Jedis jedis, String key) {
        Long del = jedis.del(key);
        return (del == null) ? null : (del.intValue() == 0) ? Boolean.FALSE : Boolean.TRUE;
    }

    public Map<String, Object> getMap(Jedis jedis, Collection<String> keys) throws Exception {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        byte[][] keyArr = new byte[keys.size()][];
        int i = 0;
        for (String key : keys) {
            keyArr[i++] = stringConverter.serialize(key);
        }
        List<byte[]> list = jedis.mget(keyArr);
        if (list != null && list.size() > 0) {
            i = 0;
            for (String key : keys) {
                byte[] tmp = list.get(i++);
                map.put(key, (tmp != null) ? serializingTranscoder.deserialize(tmp) : null);
            }
        }
        return map;
    }

    public Boolean setCounter(Jedis jedis, String key, long num) {
        return setCounter(jedis, key, num, defaultExpire);
    }

    public Boolean setCounter(Jedis jedis, String key, long num, int expire) {
        String sc = null;
        if (expire == PERSIST) {
            sc = jedis.set(key, String.valueOf(num));
        } else {
            sc = jedis.setex(key, expire, String.valueOf(num));
        }
        return isOK(sc);
    }

    public Long incr(Jedis jedis, String key) {
        return jedis.incr(key);
    }

    public Long incr(Jedis jedis, String key, long delta) {
        return jedis.incrBy(key, delta);
    }

    public Long decr(Jedis jedis, String key) {
        return jedis.decr(key);
    }

    public Long decr(Jedis jedis, String key, long delta) {
        return jedis.decrBy(key, delta);
    }

    public Long keyDel(Jedis jedis, String... keys) {
        return jedis.del(keys);
    }

    public Set<String> keyKeys(Jedis jedis, String pattern) {
        return jedis.keys(pattern);
    }

    public Boolean keyExists(Jedis jedis, String key) {
        return jedis.exists(key);
    }

    public Boolean keyExpire(Jedis jedis, String key, int seconds) {
        Long ret = jedis.expire(key, seconds);
        return isOK(ret);
    }

    public Boolean keyExpireAt(Jedis jedis, String key, long unixTime) {
        Long ret = jedis.expireAt(key, unixTime);
        return isOK(ret);
    }

    public Long keyTtl(Jedis jedis, String key) {
        return jedis.ttl(key);
    }

    public Boolean stringSet(Jedis jedis, String key, String value) {
        String ret = jedis.set(key, value);
        return isOK(ret);
    }

    public Boolean stringSetex(Jedis jedis, String key, String value, int seconds) {
        String ret = jedis.setex(key, seconds, value);
        return isOK(ret);
    }

    public Boolean stringSet(Jedis jedis, String key, String value, EnumExist nxxx, EnumTime expx, long time) {
        String ret = jedis.setex(key, (int)time/1000, value);
        return isOK(ret);
    }

    public Boolean stringSetnx(Jedis jedis, String key, String value) {
        Long ret = jedis.setnx(key, value);
        return (ret != null) ? ((ret.intValue() == 1) ? Boolean.TRUE : Boolean.FALSE) : null;
    }

    public Boolean stringMset(Jedis jedis, Map<String, String> kvs) {
        String[] keysvalues = new String[kvs.size() * 2];
        int i = 0;
        for (Map.Entry<String, String> entry : kvs.entrySet()) {
            keysvalues[i++] = entry.getKey();
            keysvalues[i++] = entry.getValue();
        }
        jedis.mset(keysvalues);
        return Boolean.TRUE;
    }

    public String stringGet(Jedis jedis, String key) {
        return jedis.get(key);
    }

    public Map<String, String> stringMget(Jedis jedis, String... keys) {
        List<String> list = jedis.mget(keys);
        if (list == null) {
            return null;
        }
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int i = 0; i < keys.length; i++) {
            result.put(keys[i], list.get(i));
        }
        return result;
    }

    public String stringGetset(Jedis jedis, String key, String value) {
        return jedis.getSet(key, value);
    }

    public Boolean hashHset(Jedis jedis, String key, String field, String value) {
        jedis.hset(key, field, value);
        return Boolean.TRUE;
    }

    public Boolean hashHsetnx(Jedis jedis, String key, String field, String value) {
        Long ret = jedis.hsetnx(key, field, value);
        return (ret != null) ? ((ret.intValue() == 1) ? Boolean.TRUE : Boolean.FALSE) : null;
    }

    public Boolean hashHmset(Jedis jedis, String key, Map<String, String> hash) {
        jedis.hmset(key, hash);
        return Boolean.TRUE;
    }

    public String hashHget(Jedis jedis, String key, String field) {
        return jedis.hget(key, field);
    }

    public List<String> hashHmget(Jedis jedis, String key, String... fields) {
        return jedis.hmget(key, fields);
    }

    public Map<String, String> hashHgetAll(Jedis jedis, String key) {
        return jedis.hgetAll(key);
    }

    public Long hashHdel(Jedis jedis, String key, String... fields) {
        return jedis.hdel(key, fields);
    }

    public Long hashHlen(Jedis jedis, String key) {
        return jedis.hlen(key);
    }

    public Boolean hashHexists(Jedis jedis, String key, String field) {
        return jedis.hexists(key, field);
    }

    public Long hashHincrBy(Jedis jedis, String key, String field, long value) {
        return jedis.hincrBy(key, field, value);
    }

    public Set<String> hashHkeys(Jedis jedis, String key) {
        return jedis.hkeys(key);
    }

    public List<String> hashHvals(Jedis jedis, String key) {
        return jedis.hvals(key);
    }

    public Long listLpush(Jedis jedis, String key, String... strings) {
        return jedis.lpush(key, strings);
    }

    public Long listRpush(Jedis jedis, String key, String... strings) {
        return jedis.rpush(key, strings);
    }

    public String listLpop(Jedis jedis, String key) {
        return jedis.lpop(key);
    }

    public String listBlpop(Jedis jedis, String key, int timeout) {
        List<String> arr = jedis.blpop(timeout, key);
        return (arr == null || arr.isEmpty()) ? null : arr.get(1);
    }

    public String listBrpop(Jedis jedis, String key, int timeout) {
        List<String> arr = jedis.brpop(timeout, key);
        return (arr == null || arr.isEmpty()) ? null : arr.get(1);
    }

    public String listRpop(Jedis jedis, String key) {
        return jedis.rpop(key);
    }

    public Long listLlen(Jedis jedis, String key) {
        return jedis.llen(key);
    }

    public List<String> listLrange(Jedis jedis, String key, long start, long end) {
        return jedis.lrange(key, start, end);
    }

    public Long listLrem(Jedis jedis, String key, String value) {
        return jedis.lrem(key, 0, value);
    }

    public Boolean listLtrim(Jedis jedis, String key, long start, long end) {
        String ret = jedis.ltrim(key, start, end);
        return isOK(ret);
    }

    public Boolean listLset(Jedis jedis, String key, long index, String value) {
        String ret = jedis.lset(key, index, value);
        return isOK(ret);
    }

    public Long setSadd(Jedis jedis, String key, String... members) {
        return jedis.sadd(key, members);
    }

    public Long setSrem(Jedis jedis, String key, String... members) {
        return jedis.srem(key, members);
    }

    public Set<String> setSmembers(Jedis jedis, String key) {
        return jedis.smembers(key);
    }

    public Boolean setSismember(Jedis jedis, String key, String member) {
        return jedis.sismember(key, member);
    }

    public Long setScard(Jedis jedis, String key) {
        return jedis.scard(key);
    }

    public String setSpop(Jedis jedis, String key) {
        return jedis.spop(key);
    }

    public Boolean setSmove(Jedis jedis, String srckey, String dstkey, String member) {
        Long ret = jedis.smove(srckey, dstkey, member);
        return (ret != null) ? ((ret.intValue() == 1) ? Boolean.TRUE : Boolean.FALSE) : null;
    }

    public ScanResult<String> setScan(Jedis jedis, String key, String cursor) {
        return jedis.sscan(key, cursor);
    }

    public ScanResult<String> setScan(Jedis jedis, String key, String cursor, ScanParams params) {
        return jedis.sscan(key, cursor, params);
    }

    public Long sortSetZadd(Jedis jedis, String key, String member, double score) {
        return jedis.zadd(key, score, member);
    }

    public Long sortSetZadd(Jedis jedis, String key, Map<Double, String> scoreMembers) {
        return jedis.zadd(key, CollectionUtil.transform(scoreMembers));
    }

    public Long sortSetZadd2(Jedis jedis, String key, Map<String, Double> scoreMembers) {
        return jedis.zadd(key, scoreMembers);
    }

    public Long sortSetZrem(Jedis jedis, String key, String... members) {
        return jedis.zrem(key, members);
    }

    public Long sortSetZcard(Jedis jedis, String key) {
        return jedis.zcard(key);
    }

    public Long sortSetZcount(Jedis jedis, String key, double min, double max) {
        return jedis.zcount(key, min, max);
    }

    public Double sortSetZscore(Jedis jedis, String key, String member) {
        return jedis.zscore(key, member);
    }

    public Double sortSetZincrby(Jedis jedis, String key, String member, double score) {
        return jedis.zincrby(key, score, member);
    }

    public Set<String> sortSetZrange(Jedis jedis, String key, long start, long end) {
        return jedis.zrange(key, start, end);
    }

    public Map<String, Double> sortSetZrangeWithScores(Jedis jedis, String key, long start, long end) {
        Set<Tuple> tuples = jedis.zrangeWithScores(key, start, end);
        if (tuples == null) {
            return null;
        }
        Map<String, Double> map = new LinkedHashMap<String, Double>();
        for (Tuple tuple : tuples) {
            map.put(tuple.getElement(), tuple.getScore());
        }
        return map;
    }

    public Set<String> sortSetZrevrange(Jedis jedis, String key, long start, long end) {
        return jedis.zrevrange(key, start, end);
    }

    public Map<String, Double> sortSetZrevrangeWithScores(Jedis jedis, String key, long start, long end) {
        Set<Tuple> tuples = jedis.zrevrangeWithScores(key, start, end);
        if (tuples == null) {
            return null;
        }
        Map<String, Double> map = new LinkedHashMap<String, Double>();
        for (Tuple tuple : tuples) {
            map.put(tuple.getElement(), tuple.getScore());
        }
        return map;
    }

    public Long sortSetZrank(Jedis jedis, String key, String member) {
        return jedis.zrank(key, member);
    }

    public Long sortSetZrevrank(Jedis jedis, String key, String member) {
        return jedis.zrevrank(key, member);
    }

    public Set<String> sortSetZrangeByScore(Jedis jedis, String key, double min, double max) {
        return jedis.zrangeByScore(key, min, max);
    }

    public Set<String> sortSetZrangeByScore(Jedis jedis, String key, double min, double max, int offset, int count) {
        return jedis.zrangeByScore(key, min, max, offset, count);
    }

    public Set<String> sortSetZrevrangeByScore(Jedis jedis, String key, double max, double min) {
        return jedis.zrevrangeByScore(key, max, min);
    }

    public Set<String> sortSetZrevrangeByScore(Jedis jedis, String key, double max, double min, int offset, int count) {
        return jedis.zrevrangeByScore(key, max, min, offset, count);
    }

    public Long sortSetZremrangeByRank(Jedis jedis, String key, long start, long end) {
        return jedis.zremrangeByRank(key, start, end);
    }

    public ScanResult<Tuple> sortSetZscan(Jedis jedis, String key, String cursor) {
        return jedis.zscan(key, cursor);
    }

    public ScanResult<Tuple> sortSetZscan(Jedis jedis, String key, String cursor, ScanParams params) {
        return jedis.zscan(key, cursor, params);
    }

    public Boolean publish(Jedis jedis, String channel, String message) {
        jedis.publish(channel, message);
        return Boolean.TRUE;
    }

}
