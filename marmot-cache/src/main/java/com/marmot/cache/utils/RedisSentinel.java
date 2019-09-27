package com.marmot.cache.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.Jedis;

public class RedisSentinel {


    private final ConcurrentMap<Node, RedisHeartbeat> heartbeatSentinel = new ConcurrentHashMap<Node, RedisHeartbeat>();

    private ScheduledExecutorService heartbeat;
    private static final int DEFAULT_CHECK_INTERVAL = 1; // 秒

    private IHeartbeatReporting heartbeatReporting;

    private static RedisSentinel redisSentinel = new RedisSentinel();

    public static RedisSentinel getInstance() {
        return redisSentinel;
    }

    private RedisSentinel() {
        this.heartbeat = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Marmot-Redis-Sentinel");
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }

        });
        this.heartbeat.scheduleWithFixedDelay(newPingRunner(this), DEFAULT_CHECK_INTERVAL, DEFAULT_CHECK_INTERVAL,
                TimeUnit.SECONDS);
    }

    /**
     * 心跳上报接口
     * <p>
     * 注册在 {@link watch}方法之前
     * 
     * @param heartbeatReporting
     */
    public void listen(IHeartbeatReporting heartbeatReporting) {
        this.heartbeatReporting = heartbeatReporting;
    }

    public void watch(String host, int port, String password) {
        Node node = new Node(host, port);
        if (!heartbeatSentinel.containsKey(node)) {
            heartbeatSentinel.put(node, new RedisHeartbeat(host, port, password, heartbeatReporting));
        }
    }

    public void unwatch(String host, int port) {
        Node node = new Node(host, port);
        RedisHeartbeat redisHeartbeat = heartbeatSentinel.remove(node);
        if (redisHeartbeat != null) {
            try {
                redisHeartbeat.disconnect();
            } catch (Exception e) {
            }
        }
    }

    /**
     * 检查链接
     * 
     * @param host
     * @param port
     * @return true:可用 false:不可用
     */
    public boolean valid(String host, int port) {
        return valid(new Node(host, port));
    }

    private boolean valid(Node node) {
        RedisHeartbeat redisHeartbeat = heartbeatSentinel.get(node);
        return (redisHeartbeat != null) ? redisHeartbeat.getStatus() : true;
    }

    /**
     * 检查链接
     * 
     * @param jedis
     * @return true:可用 false:不可用
     */
    public boolean valid(final Jedis jedis) {
        Node node = new Node(jedis.getClient().getHost(), jedis.getClient().getPort());
        boolean status = valid(node);
        if (true == status) {
            // 校验链接的有效性
            RedisHeartbeat redisHeartbeat = heartbeatSentinel.get(node);
            if (redisHeartbeat != null) {
                long lastFailTime = redisHeartbeat.getLastFailTime();
                if (lastFailTime != -1) {
                    if (jedis.getClient().isBroken()) {
                        try {
                            jedis.getClient().disconnect();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
        return status;
    }

    private static Runnable newPingRunner(final RedisSentinel sentinel) {
        return new Runnable() {
            public void run() {
                sentinel.checkStatus();
            }
        };
    }

    private synchronized void checkStatus() {
        for (RedisHeartbeat hb : heartbeatSentinel.values()) {
            hb.ping();
        }
    }

    public void shutdown() {
        if (this.heartbeat != null) {
            this.heartbeat.shutdownNow();
        }
        for (RedisHeartbeat redisHeartbeat : heartbeatSentinel.values()) {
            if (redisHeartbeat != null) {
                try {
                    redisHeartbeat.disconnect();
                } catch (Exception e) {
                }
            }
        }
        heartbeatSentinel.clear();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        shutdown();
    }

    private static class Node {
        String host;
        int port;

        public Node(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((host == null) ? 0 : host.hashCode());
            result = prime * result + port;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Node other = (Node) obj;
            if (host == null) {
                if (other.host != null)
                    return false;
            } else if (!host.equals(other.host))
                return false;
            if (port != other.port)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Node [host=" + host + ", port=" + port + "]";
        }

    }

}
