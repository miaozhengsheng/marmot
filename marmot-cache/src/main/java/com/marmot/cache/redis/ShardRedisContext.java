package com.marmot.cache.redis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.marmot.cache.enums.EnumExist;
import com.marmot.cache.enums.EnumTime;
import com.marmot.cache.utils.CollectionUtil;
import com.marmot.cache.utils.RedisSentinel;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.params.SetParams;

public class ShardRedisContext {


    private Converter<Object> serializingTranscoder;

    private Converter<String> stringConverter = new StringConverter();

    public static final String STATUS_CODE_REPLY = "OK";
    public static final int PERSIST = 0;

    private int defaultExpire;

    public ShardRedisContext(int expire, boolean transcode) {
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

    public Boolean set(ShardedJedis jedis, String key, Object value, int expire) throws Exception {
        String sc = null;
        if (expire == PERSIST) {
            sc = jedis.set(stringConverter.serialize(key), serializingTranscoder.serialize(value));
        } else {
            sc = jedis.setex(stringConverter.serialize(key), expire, serializingTranscoder.serialize(value));
        }
        return isOK(sc);
    }

    public Boolean set(ShardedJedis jedis, String key, Object value) throws Exception {
        return set(jedis, key, value, defaultExpire);
    }

    public Boolean set(ShardedJedis jedis, String key, Object value, Date date) throws Exception {
        int expire = defaultExpire;
        if (date != null) {
            Long l = new Long((date.getTime() - System.currentTimeMillis()) / 1000L);
            expire = l.intValue();
        }
        return set(jedis, key, value, expire);
    }

    public Object get(ShardedJedis jedis, String key) throws Exception {
        Object obj = null;
        byte[] bytes = jedis.get(stringConverter.serialize(key));
        if (bytes != null) {
            obj = serializingTranscoder.deserialize(bytes);
        }
        return obj;
    }

    public Boolean delete(ShardedJedis jedis, String key) {
        Long del = jedis.del(key);
        return (del == null) ? null : (del.intValue() == 0) ? Boolean.FALSE : Boolean.TRUE;
    }

    public Map<String, Object> getMap(ShardedJedis shardedJedis, Collection<String> keys) throws Exception {
        Map<String, Object> result = new HashMap<String, Object>();
        Map<Jedis, List<byte[]>> relation = new HashMap<Jedis, List<byte[]>>();
        for (String key : keys) {
            Jedis jedis = shardedJedis.getShard(key);
            List<byte[]> keyBytes = relation.get(jedis);
            if (keyBytes == null) {
                keyBytes = new ArrayList<byte[]>();
                relation.put(jedis, keyBytes);
            }
            keyBytes.add(stringConverter.serialize(key));
        }

        for (Map.Entry<Jedis, List<byte[]>> entry : relation.entrySet()) {
            Jedis jedis = entry.getKey();
            if (!validate(jedis)) {
                continue;
            }
            List<byte[]> keyBytes = entry.getValue();
            byte[][] keyBytesArr = keyBytes.toArray(new byte[][] {});
            List<byte[]> list = jedis.mget(keyBytesArr);

            if (list != null && list.size() > 0) {
                for (int i = 0; i < keyBytes.size(); i++) {
                    byte[] key = keyBytes.get(i);
                    byte[] value = list.get(i);
                    result.put(stringConverter.deserialize(key),
                            (value != null) ? serializingTranscoder.deserialize(value) : null);
                }
            }
        }
        return result;
    }

    public Boolean setCounter(ShardedJedis jedis, String key, long num) {
        return setCounter(jedis, key, num, defaultExpire);
    }

    public Boolean setCounter(ShardedJedis jedis, String key, long num, int expire) {
        String sc = null;
        if (expire == PERSIST) {
            sc = jedis.set(key, String.valueOf(num));
        } else {
            sc = jedis.setex(key, expire, String.valueOf(num));
        }
        return isOK(sc);
    }

    public Long incr(ShardedJedis jedis, String key) {
        return jedis.incr(key);
    }

    public Long incr(ShardedJedis jedis, String key, long delta) {
        return jedis.incrBy(key, delta);
    }

    public Long decr(ShardedJedis jedis, String key) {
        return jedis.decr(key);
    }

    public Long decr(ShardedJedis jedis, String key, long delta) {
        return jedis.decrBy(key, delta);
    }

    public Long keyDel(ShardedJedis shardedJedis, String... keys) {
        Map<Jedis, List<String>> relation = new HashMap<Jedis, List<String>>();
        long result = 0;
        for (String key : keys) {
            Jedis jedis = shardedJedis.getShard(key);
            List<String> keyList = relation.get(jedis);
            if (keyList == null) {
                keyList = new ArrayList<String>();
                relation.put(jedis, keyList);
            }
            keyList.add(key);
        }
        for (Map.Entry<Jedis, List<String>> entry : relation.entrySet()) {
            Jedis jedis = entry.getKey();
            if (!validate(jedis)) {
                continue;
            }
            List<String> keyList = entry.getValue();
            result += jedis.del(keyList.toArray(new String[] {}));
        }
        return result;
    }

    public Set<String> keyKeys(ShardedJedis jedis, String pattern) {
        throw new UnsupportedOperationException("Does not support keys by pattern");
    }

    public Boolean keyExists(ShardedJedis jedis, String key) {
        return jedis.exists(key);
    }

    public Boolean keyExpire(ShardedJedis jedis, String key, int seconds) {
        Long ret = jedis.expire(key, seconds);
        return isOK(ret);
    }

    public Boolean keyExpireAt(ShardedJedis jedis, String key, long unixTime) {
        Long ret = jedis.expireAt(key, unixTime);
        return isOK(ret);
    }

    public Long keyTtl(ShardedJedis jedis, String key) {
        return jedis.ttl(key);
    }

    public Boolean stringSet(ShardedJedis jedis, String key, String value) {
        String ret = jedis.set(key, value);
        return isOK(ret);
    }

    public Boolean stringSetex(ShardedJedis jedis, String key, String value, int seconds) {
        String ret = jedis.setex(key, seconds, value);
        return isOK(ret);
    }

    public Boolean stringSet(ShardedJedis jedis, String key, String value, EnumExist nxxx, EnumTime expx, long time) {
    	   SetParams setParams = new SetParams();
           setParams.px(time);
           
    	String ret = jedis.set(key, value, setParams);
     
        return isOK(ret);
    }

    public Boolean stringSetnx(ShardedJedis jedis, String key, String value) {
        Long ret = jedis.setnx(key, value);
        return (ret != null) ? ((ret.intValue() == 1) ? Boolean.TRUE : Boolean.FALSE) : null;
    }

    public Boolean stringMset(ShardedJedis shardedJedis, Map<String, String> kvs) {
        Map<Jedis, List<String>> relation = new HashMap<Jedis, List<String>>();
        for (Map.Entry<String, String> entry : kvs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Jedis jedis = shardedJedis.getShard(key);
            List<String> list = relation.get(jedis);
            if (list == null) {
                list = new ArrayList<String>();
                relation.put(jedis, list);
            }
            list.add(key);
            list.add(value);
        }
        for (Map.Entry<Jedis, List<String>> entry : relation.entrySet()) {
            Jedis jedis = entry.getKey();
            if (!validate(jedis)) {
                continue;
            }
            List<String> list = entry.getValue();
            jedis.mset(list.toArray(new String[] {}));
        }
        return Boolean.TRUE;
    }

    public String stringGet(ShardedJedis jedis, String key) {
        return jedis.get(key);
    }

    public Map<String, String> stringMget(ShardedJedis shardedJedis, String... keys) {
        Map<String, String> result = new HashMap<String, String>();
        Map<Jedis, List<String>> relation = new HashMap<Jedis, List<String>>();
        for (String key : keys) {
            Jedis jedis = shardedJedis.getShard(key);
            List<String> keyList = relation.get(jedis);
            if (keyList == null) {
                keyList = new ArrayList<String>();
                relation.put(jedis, keyList);
            }
            keyList.add(key);
        }
        for (Map.Entry<Jedis, List<String>> entry : relation.entrySet()) {
            Jedis jedis = entry.getKey();
            if (!validate(jedis)) {
                continue;
            }
            List<String> keyList = entry.getValue();
            List<String> values = jedis.mget(keyList.toArray(new String[] {}));
            for (int i = 0; i < keyList.size(); i++) {
                result.put(keyList.get(i), values.get(i));
            }
        }
        return result;
    }

    public String stringGetset(ShardedJedis jedis, String key, String value) {
        return jedis.getSet(key, value);
    }

    public Boolean hashHset(ShardedJedis jedis, String key, String field, String value) {
        jedis.hset(key, field, value);
        return Boolean.TRUE;
    }

    public Boolean hashHsetnx(ShardedJedis jedis, String key, String field, String value) {
        Long ret = jedis.hsetnx(key, field, value);
        return (ret != null) ? ((ret.intValue() == 1) ? Boolean.TRUE : Boolean.FALSE) : null;
    }

    public Boolean hashHmset(ShardedJedis jedis, String key, Map<String, String> hash) {
        jedis.hmset(key, hash);
        return Boolean.TRUE;
    }

    public String hashHget(ShardedJedis jedis, String key, String field) {
        return jedis.hget(key, field);
    }

    public List<String> hashHmget(ShardedJedis jedis, String key, String... fields) {
        return jedis.hmget(key, fields);
    }

    public Map<String, String> hashHgetAll(ShardedJedis jedis, String key) {
        return jedis.hgetAll(key);
    }

    public Long hashHdel(ShardedJedis jedis, String key, String... fields) {
        return jedis.hdel(key, fields);
    }

    public Long hashHlen(ShardedJedis jedis, String key) {
        return jedis.hlen(key);
    }

    public Boolean hashHexists(ShardedJedis jedis, String key, String field) {
        return jedis.hexists(key, field);
    }

    public Long hashHincrBy(ShardedJedis jedis, String key, String field, long value) {
        return jedis.hincrBy(key, field, value);
    }

    public Set<String> hashHkeys(ShardedJedis jedis, String key) {
        return jedis.hkeys(key);
    }

    public List<String> hashHvals(ShardedJedis jedis, String key) {
        return jedis.hvals(key);
    }

    public Long listLpush(ShardedJedis jedis, String key, String... strings) {
        return jedis.lpush(key, strings);
    }

    public Long listRpush(ShardedJedis jedis, String key, String... strings) {
        return jedis.rpush(key, strings);
    }

    public String listLpop(ShardedJedis jedis, String key) {
        return jedis.lpop(key);
    }

    public String listRpop(ShardedJedis jedis, String key) {
        return jedis.rpop(key);
    }

    public Long listLlen(ShardedJedis jedis, String key) {
        return jedis.llen(key);
    }

    public String listBlpop(ShardedJedis jedis, String key, int timeout) {
        List<String> arr = jedis.blpop(timeout, key);
        return (arr == null || arr.isEmpty()) ? null : arr.get(1);
    }

    public String listBrpop(ShardedJedis jedis, String key, int timeout) {
        List<String> arr = jedis.brpop(timeout, key);
        return (arr == null || arr.isEmpty()) ? null : arr.get(1);
    }

    public List<String> listLrange(ShardedJedis jedis, String key, long start, long end) {
        return jedis.lrange(key, start, end);
    }

    public Long listLrem(ShardedJedis jedis, String key, String value) {
        return jedis.lrem(key, 0, value);
    }

    public Boolean listLtrim(ShardedJedis jedis, String key, long start, long end) {
        String ret = jedis.ltrim(key, start, end);
        return isOK(ret);
    }

    public Boolean listLset(ShardedJedis jedis, String key, long index, String value) {
        String ret = jedis.lset(key, index, value);
        return isOK(ret);
    }

    public Long setSadd(ShardedJedis jedis, String key, String... members) {
        return jedis.sadd(key, members);
    }

    public Long setSrem(ShardedJedis jedis, String key, String... members) {
        return jedis.srem(key, members);
    }

    public Set<String> setSmembers(ShardedJedis jedis, String key) {
        return jedis.smembers(key);
    }

    public Boolean setSismember(ShardedJedis jedis, String key, String member) {
        return jedis.sismember(key, member);
    }

    public Long setScard(ShardedJedis jedis, String key) {
        return jedis.scard(key);
    }

    public String setSpop(ShardedJedis jedis, String key) {
        return jedis.spop(key);
    }

    public ScanResult<String> setScan(ShardedJedis jedis, String key, String cursor) {
        return jedis.sscan(key, cursor);
    }

    public ScanResult<String> setScan(ShardedJedis jedis, String key, String cursor, ScanParams params) {
        return jedis.sscan(key, cursor, params);
    }

    public Long sortSetZadd(ShardedJedis jedis, String key, String member, double score) {
        return jedis.zadd(key, score, member);
    }

    public Long sortSetZadd(ShardedJedis jedis, String key, Map<Double, String> scoreMembers) {
        return jedis.zadd(key, CollectionUtil.transform(scoreMembers));
    }

    public Long sortSetZadd2(ShardedJedis jedis, String key, Map<String, Double> scoreMembers) {
        return jedis.zadd(key, scoreMembers);
    }

    public Long sortSetZrem(ShardedJedis jedis, String key, String... members) {
        return jedis.zrem(key, members);
    }

    public Long sortSetZcard(ShardedJedis jedis, String key) {
        return jedis.zcard(key);
    }

    public Long sortSetZcount(ShardedJedis jedis, String key, double min, double max) {
        return jedis.zcount(key, min, max);
    }

    public Double sortSetZscore(ShardedJedis jedis, String key, String member) {
        return jedis.zscore(key, member);
    }

    public Double sortSetZincrby(ShardedJedis jedis, String key, String member, double score) {
        return jedis.zincrby(key, score, member);
    }

    public Set<String> sortSetZrange(ShardedJedis jedis, String key, long start, long end) {
        return jedis.zrange(key, start, end);
    }

    public Map<String, Double> sortSetZrangeWithScores(ShardedJedis jedis, String key, long start, long end) {
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

    public Set<String> sortSetZrevrange(ShardedJedis jedis, String key, long start, long end) {
        return jedis.zrevrange(key, start, end);
    }

    public Map<String, Double> sortSetZrevrangeWithScores(ShardedJedis jedis, String key, long start, long end) {
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

    public Long sortSetZrank(ShardedJedis jedis, String key, String member) {
        return jedis.zrank(key, member);
    }

    public Long sortSetZrevrank(ShardedJedis jedis, String key, String member) {
        return jedis.zrevrank(key, member);
    }

    public Set<String> sortSetZrangeByScore(ShardedJedis jedis, String key, double min, double max) {
        return jedis.zrangeByScore(key, min, max);
    }

    public Set<String> sortSetZrangeByScore(ShardedJedis jedis, String key, double min, double max, int offset,
            int count) {
        return jedis.zrangeByScore(key, min, max, offset, count);
    }

    public Set<String> sortSetZrevrangeByScore(ShardedJedis jedis, String key, double max, double min) {
        return jedis.zrevrangeByScore(key, max, min);
    }

    public Set<String> sortSetZrevrangeByScore(ShardedJedis jedis, String key, double max, double min, int offset,
            int count) {
        return jedis.zrevrangeByScore(key, max, min, offset, count);
    }

    public Long sortSetZremrangeByRank(ShardedJedis jedis, String key, long start, long end) {
        return jedis.zremrangeByRank(key, start, end);
    }

    public ScanResult<Tuple> sortSetZscan(ShardedJedis jedis, String key, String cursor) {
        return jedis.zscan(key, cursor);
    }

    public ScanResult<Tuple> sortSetZscan(ShardedJedis jedis, String key, String cursor, ScanParams params) {
        return jedis.zscan(key, cursor, params);
    }

    protected boolean validate(final Jedis jedis) {
        return RedisSentinel.getInstance().valid(jedis);
    }


}
