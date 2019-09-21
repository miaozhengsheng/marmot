package com.marmot.common.other;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class StopWatch {


    private static final Logger logger = Logger.getLogger(StopWatch.class);

    private String name;
    private List<Long> timerList = new ArrayList<Long>();
    private boolean isDebug = false;

    public StopWatch(String name) {
        this(name, false);
    }

    public StopWatch(String name, boolean isDebug) {
        this.name = name;
        this.isDebug = isDebug;
    }

    public String getTimerLog() {
        StringBuilder sb = new StringBuilder(name + " TIME ");
        if (timerList.size() > 1) {
            for (int i = 0; i < timerList.size() - 1; i++) {
                long l0 = timerList.get(i);
                long l1 = timerList.get(i + 1);
                sb.append(l1 - l0);
                sb.append('/');
            }
            sb.append(timerList.get(timerList.size() - 1) - timerList.get(0));
        }
        sb.append(" ms");
        return sb.toString();
    }

    public void stop() {
        timerList.add(System.currentTimeMillis());
    }

    public void log() {
        if (isDebug) {
            logger.debug(this.getTimerLog());
        } else {
            logger.info(this.getTimerLog());
        }
    }

    public void log(long time) {
        StringBuilder sb = new StringBuilder(name + " TIME ");
        if (timerList.size() > 1) {
            for (int i = 0; i < timerList.size() - 1; i++) {
                long l0 = timerList.get(i);
                long l1 = timerList.get(i + 1);
                sb.append(l1 - l0);
                sb.append('/');
            }
            sb.append(time);
            sb.append('/');
            sb.append(timerList.get(timerList.size() - 1) - timerList.get(0));
        }
        sb.append(" ms");
        if (isDebug) {
            logger.debug(sb.toString());
        } else {
            logger.info(sb.toString());
        }
    }

}
