package com.marmot.cache.utils;

import java.util.concurrent.TimeUnit;

import com.marmot.cache.factory.RedisCacheClientFactory;

public class ToolUtil {


    /**
     * 超过重试次数抛出运行时异常（getCause()可以获取原始异常）
     * 
     * @param tryCnt
     * @param sleepMillTime
     * @param handle
     */
    public static void retry(int tryCnt, long sleepMillTime, Handle handle) {
        Throwable cause = null;
        while (tryCnt-- > 0) {
            try {
                handle.invoke();
                break;
            } catch (Throwable e) {
                cause = e;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(sleepMillTime);
            } catch (InterruptedException e) {
            }
        }
        if (tryCnt == -1 && cause != null) {
            throw new RuntimeException(cause);
        }
    }

    public static interface Handle {

        public void invoke() throws Throwable;

    }

    public static int justTime(int soTimeout) {
        return Math.max(Math.min(soTimeout, 1000), RedisCacheClientFactory.SO_TIMEOUT);
    }


}
