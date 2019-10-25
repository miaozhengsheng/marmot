package com.marmot.common.log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.marmot.common.constants.Const;
import com.marmot.common.util.PropUtil;

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
        return register(category, category + System.getProperty("file.separator") + filename + ".log");
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
     * 根目录:${catalina.base}/logs/<br>
     * <p>
     * docker环境:<br>
     * 输出到宿主机器的目录上<br>
     * 根目录：/data/applogs/leida/${ins-user-platform_tc.10003.1_dockerIp}/<br>
     * 动态目录生成规范：$projectName_$pod_$ip<br>
     * 代码：ClientUtil.getProjectName()_SystemUtil.getPod()_SystemUtil.
     * getInNetworkIp()<br>
     * <p>
     * 本机单元测试环境：<br>
     * 根目录:${user.dir}/logs/<br>
     * 
     * @return
     */
    public static final String getLogRootDirectory() {
        String value = System.getProperty("catalina.base");
        if (value == null) {// 单元测试模式
            return System.getProperty("user.dir") + System.getProperty("file.separator") + "logs"
                    + System.getProperty("file.separator");
        } else {// 发布部署模式
            if (Helper.isOnDocker()) {
                return "/data/applogs/leida/" + PropUtil.getInstance().get("project.name") + "_" + Helper.getPod()
                        + "_" + Helper.getInNetworkIp() + System.getProperty("file.separator");
            } else {
                return Helper.getTomcatRootDirectory("logs");
            }
        }
    }

    private static class Helper {

        private static final String FILE_SEPARATOR = "file.separator";
        private static final String CATALINA_BASE = "catalina.base";

        /**
         * 获取内网ip地址
         * 
         * @return
         */
        public static String getInNetworkIp() {
            return readLocalIp();
        }

        /**
         * 获取pod标识
         * <p>
         * 
         * @return
         */
        public static String getPod() {
            return System.getProperty("pod");
        }

        /**
         * 判断工程是否运行在docker里面
         * 
         * @return
         */
        public static boolean isOnDocker() {
            return Boolean.parseBoolean(System.getProperty("docker", "false"));
        }

        /**
         * tomcat根目录
         * <p>
         * 如：/data/apps/ins-xxx-platform/${subDirectory}/
         * 
         * @return
         */
        public static String getTomcatRootDirectory(String subDirectory) {
            return System.getProperty(CATALINA_BASE) + System.getProperty(FILE_SEPARATOR)
                    + ((subDirectory != null) ? (subDirectory + System.getProperty(FILE_SEPARATOR)) : "");
        }

        private static String readLocalIp() {
            String inIp = null;

            try {
                Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
                NetworkInterface netInterface;
                while (allNetInterfaces.hasMoreElements()) {
                    netInterface = allNetInterfaces.nextElement();
                    Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                    InetAddress inetAddress = null;
                    while (addresses.hasMoreElements()) {
                        inetAddress = addresses.nextElement();
                        if (inetAddress != null && inetAddress instanceof Inet4Address) {
                            String ip = inetAddress.getHostAddress();
                            if ("127.0.0.1".equals(ip) || "localhost".equals(ip)) {
                                continue;
                            }
                            inIp = ip;
                            break;
                        }
                    }
                }
            } catch (SocketException e) {
                throw new RuntimeException("Read In-Network Ip fail, please check Network first!");
            }
            if (inIp == null) {
                throw new RuntimeException("Read In-Network Ip fail, please check In-Network first!");
            }
            return inIp;
        }

    }


}
