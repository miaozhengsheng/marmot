package com.marmot.cache.redis.pubsub;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import com.marmot.cache.redis.IBlockHandle;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class SubscribeThread extends Thread {

    private static final Logger logger = Logger.getLogger(SubscribeThread.class);

    private final AtomicReference<JedisPool> jedisPoolReference;
    private final IBlockHandle<Jedis> blockHandle;

    private AbstractSubscriber subscriber;
    private boolean pattern;
    private String[] channels;

    public SubscribeThread(AtomicReference<JedisPool> jedisPoolReference, IBlockHandle<Jedis> blockHandle,
            AbstractSubscriber subscriber, boolean pattern, String... channels) {
        this.jedisPoolReference = jedisPoolReference;
        this.blockHandle = blockHandle;
        this.subscriber = subscriber;
        this.pattern = pattern;
        this.channels = channels;
        setName("Marmot-Redis-subscriber-" + Arrays.toString(channels));
        setDaemon(true);
    }

    @Override
    public void run() {
        while (true && !Thread.currentThread().isInterrupted()) {
            Jedis jedis = jedisPoolReference.get().getResource();
            blockHandle.used(jedis);
            try {
                // 阻塞模式
                if (pattern) {
                    jedis.psubscribe(subscriber, channels);
                } else {
                    jedis.subscribe(subscriber, channels);
                }
            } catch (Exception e) {
                logger.warn("redis subscribe " + Arrays.toString(channels) + " fail, comint into a new subscription", e);
            } finally {
                if (jedis != null) {
                    blockHandle.release(jedis);
                    jedis.close();
                }
            }
            // 结束订阅
            if (!subscriber.isSubscribed()) {
                break;
            }
            // 网络问题重新建立连接
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
            }
        }
    }
}
