package com.marmot.common.util;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.marmot.common.constants.Const;
import com.marmot.common.system.SystemUtil;

public class Log4jUtil {


    /**
     * 动态设置package日志输出级别
     * 
     * @param name
     * @param value
     * @return
     */
    public static final Level setLevel(String name, String value) {
        Level level = Level.toLevel(value);
        LogManager.getLogger(name).setLevel(level);
        return level;
    }

    /**
     * 设置输出台日志输出级别
     * 
     * @param levelInt
     */
    public static final void setConsoleLevel(int levelInt) {
        AppenderSkeleton appenderSkeleton = (AppenderSkeleton) LogManager.getRootLogger().getAppender("CONSOLE");
        appenderSkeleton.setThreshold(Level.toLevel(levelInt));
    }

    /**
     * 增加日志输出文件
     * <p>
     * ${category}/debug.log<br>
     * 
     * @param className
     */
    public static final Logger register(String category) {
        return register2(category, "debug");
    }

    /**
     * 增加日志输出文件
     * <p>
     * ${category}/${filename}.log<br>
     * 
     * @param category
     * @param filename
     * @return
     */
    public static final Logger register2(String category, String filename) {
        return register(category, category + SystemUtil.FILE_SEPARATOR + filename + ".log");
    }

    public static final Logger register(String category, String filePath) {
        // 构建appender
        DailyRollingFileAppender appender = new DailyRollingFileAppender();
        appender.setFile(getLogRootDirectory() + filePath);
        appender.setAppend(true);
        appender.setThreshold(Level.INFO);
        appender.setDatePattern("'.'yyyy-MM-dd-HH");
        appender.setEncoding("UTF-8");
        PatternLayout layout = new PatternLayout();
        layout.setConversionPattern("%d{yyyy-MM-dd HH:mm:ss:SSS} %p [%t] %c | %m%n");
        appender.setLayout(layout);
        appender.setName(category);
        // 激活
        appender.activateOptions();

        // 设置logger
        Logger logger = LogManager.getLogger(Const.NAMESPACE_LOG4J_CATEGORY + category);
        logger.removeAllAppenders();
        logger.setLevel(Level.INFO);
        // 不继承父类的appender(即root)，也可以提高性能
        logger.setAdditivity(false);
        logger.addAppender(appender);

        return logger;
    }

    /**
     * 根据环境确定日志输出根目录
     * <p>
     * 非docker环境:<br>
     * 根目录:${项目部署根目录}/logs/<br>
     * <p>
     * docker环境:<br>
     * 输出到宿主机器的目录上<br>
     * 根目录：/data/applogs/leida/${ins-user-platform_tc.10003.1_dockerIp}/<br>
     * 动态目录生成规范：$projectName_$pod_$ip<br>
     * 代码：ClientUtil.getProjectName()_SystemUtil.getPod()_SystemUtil.
     * getInNetworkIp()<br>
     * 
     * @return
     */
    public static final String getLogRootDirectory() {
        if (SystemUtil.isOnDocker()) {
            return "/data/applogs/leida/" + PropUtil.getInstance().get("project.name") + "_" + SystemUtil.getPod() + "_"
                    + SystemUtil.getInNetworkIp() + SystemUtil.FILE_SEPARATOR;
        } else {
            return SystemUtil.getDeployDirectory("logs");
        }
    }


}
