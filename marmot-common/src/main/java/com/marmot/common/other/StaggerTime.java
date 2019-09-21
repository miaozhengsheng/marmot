package com.marmot.common.other;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class StaggerTime {


    private static final int RANGE = 10;

    /**
     * 随机等待<br>
     * 时间范围:0-10秒
     */
    public static final void waited() {
        waited(RANGE);
    }

    /**
     * 随机等待<br>
     * 时间范围:0-指定秒
     * 
     * @param second 单位秒
     */
    public static final void waited(int second) {
        sleep(ThreadLocalRandom.current().nextInt(second));
    }

    /**
     * 等待一段时间
     * 
     * @param second 单位秒
     */
    private static final void sleep(int second) {
        try {
            TimeUnit.SECONDS.sleep(second);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }



}
