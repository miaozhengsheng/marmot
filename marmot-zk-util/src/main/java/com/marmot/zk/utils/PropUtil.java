package com.marmot.zk.utils;

import java.net.URL;

public final class PropUtil extends Property {

    private final static String _default = "config.properties";

    private static class Creater {
        public static final PropUtil changePropUtil = new PropUtil();
        public static final PropUtil unchangePropUtil = new PropUtil(false);
    }

    private PropUtil() {
        this(true);
    }

    private PropUtil(boolean isChange) {
        URL url = PropUtil.class.getResource("/");
        if (url != null) {
            setDir(url.getPath());
        }
        setFilename(_default);
        setChange(isChange);
    }

    /**
     * 获取默认的自动加载配置文件工具类实例
     */
    public static PropUtil getInstance() {
        return Creater.changePropUtil;
    }

    /**
     * 获取静态的配置文件工具类实例，不自动加载
     */
    public static PropUtil getUnChangeInstance() {
        return Creater.unchangePropUtil;
    }
}