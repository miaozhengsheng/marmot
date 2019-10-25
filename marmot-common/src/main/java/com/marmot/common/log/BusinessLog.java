package com.marmot.common.log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.marmot.common.util.TextEscapeUtil;

public class BusinessLog {


    /**
     * 默认输出logger
     */
    private final static Logger logger = Logger.getLogger(BusinessLog.class);

    /**
     * 自定义输出目录的logger
     */
    private final ConcurrentMap<String, Logger> categoryLoggers = new ConcurrentHashMap<String, Logger>();
    private final byte[] lock = new byte[] {};

    private final static BusinessLog monitorLogger = new BusinessLog();

    private BusinessLog() {
    }

    public static BusinessLog getInstance() {
        return monitorLogger;
    }

    /**
     * 默认输出目录：${catalina.base}/logs/debug/debug.log
     * 
     * @param message
     */
    public void log(String message) {
        log(logger, message, null, true);
    }

    /**
     * 输出目录：${catalina.base}/logs/${category}/debug.log
     * 
     * @param category
     * @param message
     */
    public void log(String category, String message) {
        log(getLogger(category), message, null, false);
    }

    public void log(String message, Throwable t) {
        log(logger, message, t, true);
    }

    public void log(String category, String message, Throwable t) {
        log(getLogger(category), message, t, false);
    }

    public void logWithStack(String category, String message) {
        log(getLogger(category), message, null, true);
    }

    public void logWithStack(String category, String message, Throwable t) {
        log(getLogger(category), message, t, true);
    }

    private void log(final Logger logger, String message, Throwable t, boolean track) {
        logger.info(((message != null && message.trim().length() != 0) ? filter(message) : message)
                + ((t == null) ? ((track) ? printStackTraceMgr(false) : "") : "\n"
                        + WrapperThrowableRenderer.renderDynamic(t)));
    }

    private Logger getLogger(String category) {
        Logger log = categoryLoggers.get(category);
        if (log == null) {
            synchronized (lock) {
                log = categoryLoggers.get(category);
                if (log == null) {
                    categoryLoggers.put(category, log = Log4jUtil.register(category));
                }
            }
        }
        return log;
    }

    /**
     * 打印输出日志代码行
     * 
     * @param isAll
     * @return
     */
    private String printStackTraceMgr(boolean isAll) {
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        StackTraceElement ste;
        StringBuilder sb = new StringBuilder();
        for (int i = 4; i < stes.length; i++) {
            ste = stes[i];
            sb.append("\r\n");
            sb.append("at ");
            sb.append(ste.getClassName());
            sb.append(".");
            sb.append(ste.getMethodName());
            sb.append("(");
            sb.append(ste.getFileName());
            sb.append(":");
            sb.append(ste.getLineNumber());
            sb.append(")");
            if (!isAll) {
                break;
            }
        }
        return sb.toString();
    }

    public static String filter(String message) {
        return TextEscapeUtil.allEscape(message);
    }


}
