package com.marmot.common.other;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    final AtomicInteger threadNumber = new AtomicInteger(1);
    private String namePrefix;
    private boolean single;

    public NamedThreadFactory(String name) {
        this(name, false);
    }

    public NamedThreadFactory(String name, boolean single) {
        this.namePrefix = name;
        this.single = single;
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, (single) ? namePrefix : namePrefix + "-" + threadNumber.getAndIncrement());
        t.setDaemon(true);
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }

}
