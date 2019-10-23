package com.marmot.cache.redis;

import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.util.Sharded;

public class KeySharded extends Sharded<Jedis, JedisShardInfo> {

    public KeySharded(List<JedisShardInfo> shards) {
        super(shards);
    }
}
