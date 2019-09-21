package com.marmot.common.collections;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.marmot.common.other.NamedThreadFactory;

public class ImprovedExpiredHashMap <T, K> {

    private Cache<T, K> cache;
    private ScheduledExecutorService heartbeat;
    private static final AtomicInteger cnt = new AtomicInteger();

    public ImprovedExpiredHashMap(long timeout, TimeUnit unit) {
        cache = CacheBuilder.newBuilder().initialCapacity(100).expireAfterWrite(timeout, unit).build();
        heartbeat = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(
                "Marmot-ImprovedExpiredHashMap-" + cnt.incrementAndGet() + "-" + callThisClassName(), true));
        heartbeat.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    onEvent();
                } catch (Throwable e) {
                }
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    private void onEvent() {
        cache.cleanUp();
    }

    /**
     * 释放资源
     */
    public void destroy() {
        heartbeat.shutdownNow();
        cache.invalidateAll();
    }

    /**
     * 尽最大可能的释放资源
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        destroy();
    }

    public long size() {
        return cache.size();
    }

    public boolean isEmpty() {
        return cache.size() == 0;
    }

    public void clear() {
        cache.invalidateAll();
    }

    public boolean containsKey(Object key) {
        return cache.getIfPresent(key) != null;
    }

    public K get(Object key) {
        return cache.getIfPresent(key);
    }

    public void put(T key, K value) {
        cache.put(key, value);
    }

    public void remove(Object key) {
        cache.invalidate(key);
    }

    public void putAll(Map<? extends T, ? extends K> m) {
        cache.putAll(m);
    }

    public Set<T> keySet() {
        return cache.asMap().keySet();
    }

    public Collection<K> values() {
        return cache.asMap().values();
    }

    public Set<java.util.Map.Entry<T, K>> entrySet() {
        return cache.asMap().entrySet();
    }

    private String callThisClassName() {
        try {
            StackTraceElement[] stes = Thread.currentThread().getStackTrace();
            StackTraceElement ste = stes[3];
            StringBuilder sb = new StringBuilder();
            sb.append(ste.getClassName());
            sb.append(".");
            sb.append(ste.getMethodName());
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

}
