package com.marmot.common.system;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SystemUtil {


    public static final String CATALINA_BASE = "catalina.base";
    public static final String FILENAME = "server.xml";
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");// 文件名分隔符

    private static SystemRuntimeEnv systemRuntimeEnv;
    private static LogicArea logicAreaEnum;
    private static String logicAreaStr;
    private static String pod;
    private static boolean isDocker;
    private static String ip;
    private static String deployDir;
    private static DeployType deployType;
    private static OsType osType;

    static {
        // -DSystemRuntimeEnvironment=dev、qa、online
        systemRuntimeEnv = SystemRuntimeEnv.get(System.getProperty("SystemRuntimeEnvironment"));
        if (systemRuntimeEnv == null) {
            throw new RuntimeException(
                    "\"-DSystemRuntimeEnvironment\" missing or incorrect, please correct the configuration");
        }

        // -Dpod=针对一个服务节点身份的标示，生成规范：tc.{client_id}.{副本集编号}
        pod = System.getProperty("pod");

        // -Darea=逻辑区，如LPDEV01、LPQA01-LPQA06、sandbox、formal、task
        logicAreaStr = System.getProperty("area");
        if (logicAreaStr == null) {
            throw new RuntimeException("\"-Darea=" + logicAreaStr
                    + "\" missing or incorrect, please correct the following configuration: " + LogicArea.print());
        }
        logicAreaStr = logicAreaStr.trim().toLowerCase();
        logicAreaEnum = LogicArea.get(logicAreaStr);
        if (logicAreaEnum == null) {
            throw new RuntimeException("\"-Darea=" + logicAreaStr
                    + "\" missing or incorrect, please correct the following configuration: " + LogicArea.print());
        }

        // FIXME -Ddocker=true 标识是否运行在docker里
        isDocker = Boolean.parseBoolean(System.getProperty("docker", "false"));

        // 获取本机ip
        ip = findIp();

        // 获取部署目录
        deployDir = getDeployDir();

        // 获取部署类型
        deployType = buildDeployType();

        // 获取运行系统类型
        osType = OsType.get();
    }

    /**
     * 获取系统环境标识
     * <p>
     * 分：dev、qa、online
     * 
     * @return
     */
    public static SystemRuntimeEnv getSystemRuntimeEnvironment() {
        return systemRuntimeEnv;
    }

    /**
     * 是否线上环境
     * 
     * @return
     */
    public static boolean isOnline() {
        return SystemRuntimeEnv.online == systemRuntimeEnv;
    }

    /**
     * 是否线下环境
     * 
     * @return
     */
    public static boolean isOffline() {
        return SystemRuntimeEnv.dev == systemRuntimeEnv || SystemRuntimeEnv.qa == systemRuntimeEnv;
    }

    /**
     * 针对一个服务节点身份的标示，唯一编号，生成规范：tc.{client_id}.{副本集编号}
     * <p>
     * 使用前请沟通使用场景，避免滥用
     * <p>
     * pod名称规范：pod类型.集群id.节点id.3位随机串<br>
     * pod类型：mc(memcached)、rs(redis)、tc(tomcat)<br>
     * 集群id：client_id(应用)、cachegroup_id(缓存)<br>
     * 节点id：副本集编号(应用)、shard-副本集编号(缓存)<br>
     * 3位随机串(缓存)<br>
     * <p>
     * tomcat pod名称规范：集群id是client_id，副本集编号是服务个数自增长字段，编号针对一个项目是全局的，无3位随机串<br>
     * 如：tc.10003.1、tc.10003.2等等<br>
     * 
     * @return
     */
    public static String getPod() {
        return pod;
    }

    /**
     * 获取服务运行的逻辑区枚举，区分使用场景
     * <p>
     * 具体分5块：dev(研发逻辑区)、qa(测试逻辑区)、sandbox(预上线逻辑区)、formal(承接用户流量生产逻辑区)、task(任务逻辑区
     * )<br>
     * 注意：<br>
     * 1. dev、qa逻辑区只在线下环境才有<br>
     * 2. LPDEV01、LPDEV02、... 、LPDEVN都属于 {@link LogicArea.dev}<br>
     * 3. LPQA01、LPQA02、... 、LPQAN都属于 {@link LogicArea.qa}<br>
     * 
     * @return
     */
    public static LogicArea getLogicAreaEnum() {
        return logicAreaEnum;
    }

    /**
     * 获取服务运行的逻辑区字符串，区分使用场景
     * <p>
     * 参考 {@link #getLogicAreaEnum()}<br>
     * 注意：<br>
     * 返回真实逻辑区值，统一小写，例如：<br>
     * lpdev01、lpdevpub、lpqa03、lpqapub、formal等<br>
     * 
     * @return
     */
    public static String getLogicAreaStr() {
        return logicAreaStr;
    }

    /**
     * 是否预上线逻辑区
     * 
     * @return
     */
    public static boolean isSandboxArea() {
        return LogicArea.sandbox == logicAreaEnum;
    }

    /**
     * 是否任务逻辑区
     * 
     * @return
     */
    public static boolean isTaskArea() {
        return LogicArea.task == logicAreaEnum;
    }

    /**
     * 是否正式运行区
     * 
     * @return
     */
    public static boolean isFormalArea() {
        return LogicArea.formal == logicAreaEnum;
    }

    /**
     * 判断工程是否运行在docker里面
     * 
     * @return
     */
    public static boolean isOnDocker() {
        return isDocker;
    }

    /**
     * 获取内网ip地址
     * 
     * @return
     */
    public static String getInNetworkIp() {
        return ip;
    }

    private static String getDeployDir() {
        URL resource = SystemUtil.class.getClassLoader().getResource("");
        if (resource == null) {
            return System.getProperty("user.dir");
        }
        String classPath = SystemUtil.class.getClassLoader().getResource("").getPath();
        int pos = classPath.indexOf(".jar");
        if (pos != -1) {
            classPath = classPath.substring("file:".length(), pos);
            classPath = classPath.substring(0, classPath.lastIndexOf(FILE_SEPARATOR));
        } else {
            classPath = System.getProperty("user.dir");
        }
        return classPath;
    }

    /**
     * 获取项目名称
     * <p>
     * 目录名称判断
     * 
     * @return
     */
    public static String getContextName() {
        return deployDir.substring(deployDir.lastIndexOf(FILE_SEPARATOR) + 1, deployDir.length());
    }

    // /**
    // * 获取tomcat下的项目名
    // *
    // * @return
    // */
    // public static String getContextName() {
    // String webappsPath = System.getProperty(CATALINA_BASE) + fileSeparator()
    // + "webapps";
    // File path = new File(webappsPath);
    // File[] files = path.listFiles();
    // if (files != null) {
    // for (File file : files) {
    // if (file.isDirectory()) {
    // return file.getName();
    // }
    // }
    // }
    // throw new RuntimeException("webapps directory is not contain context
    // project");
    // }

    /**
     * 部署根目录
     * <p>
     * 如：/data/apps/ins-xxx-platform/${subDirectory}/<br>
     * 替换 {@link #getDeployDirectory(String) }
     * 
     * @return
     */
    @Deprecated
    public static String getTomcatRootDirectory(String subDirectory) {
        return getDeployDirectory(subDirectory);
    }

    /**
     * 部署根目录
     * <p>
     * 如：/data/apps/ins-xxx-platform/${subDirectory}/
     * 
     * @return
     */
    public static String getDeployDirectory(String subDirectory) {
        return deployDir + FILE_SEPARATOR + ((subDirectory != null) ? (subDirectory + FILE_SEPARATOR) : "");
    }

    public static String getDeployDirectory() {
        return deployDir;
    }

    /**
     * 部署类型：jar、war
     * 
     * @return
     */
    public static DeployType getDeployType() {
        return deployType;
    }

    /**
     * 运行系统类型：mac、linux、windows
     * 
     * @return
     */
    public static OsType getOsType() {
        return osType;
    }

    /**
     * 是否Linux环境
     * 
     * @return
     */
    public static boolean isLinux() {
        return OsType.LINUX == osType;
    }

    @SuppressWarnings("unchecked")
    private static DeployType buildDeployType() {

        // TODO 判断部署类型：jar或war
        return null;
    }

    public static enum SystemRuntimeEnv {
        @Deprecated
        dev, qa, online;
        public static SystemRuntimeEnv get(String value) {
            SystemRuntimeEnv[] values = SystemRuntimeEnv.values();
            for (SystemRuntimeEnv env : values) {
                if (env.name().equals(value.toLowerCase())) {
                    return env;
                }
            }
            return null;
        }
    }

    public static enum LogicArea {
        @Deprecated
        dev, qa, sandbox, formal, task;
        public static LogicArea get(String value) {
            LogicArea[] values = LogicArea.values();
            value = value.toLowerCase();
            for (LogicArea logicArea : values) {
                if (logicArea.name().equals(value)) {
                    return logicArea;
                }
                if (value.startsWith(("lp" + logicArea.name())) || value.startsWith(logicArea.name())) {
                    return logicArea;
                }
            }
            return null;
        }

        public static String print() {
            StringBuilder log = new StringBuilder();
            LogicArea[] values = LogicArea.values();
            for (LogicArea logicArea : values) {
                if (log.length() != 0) {
                    log.append(",");
                }
                log.append(logicArea.name());
            }
            return log.toString();
        }

    }

    public static enum DeployType {
        JAR, WAR
    }

    public static enum OsType {
        MAC, LINUX, WINDOWS;
        public static OsType get() {
            String value = System.getProperty("os.name").toLowerCase();
            if (value.contains("linux")) {// Linux
                return LINUX;
            } else if (value.contains("mac")) {// Mac OS X
                return MAC;
            } else if (value.contains("windows")) { // Windows 10
                return WINDOWS;
            } else {
                return null;
            }
        }
    }

    private static String findIp() {
        Map<String, String> data = new HashMap<>();
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface netInterface;
            while (allNetInterfaces.hasMoreElements()) {
                netInterface = allNetInterfaces.nextElement();
                if (netInterface.isVirtual() || netInterface.isLoopback() || !netInterface.isUp()
                        || netInterface.getName().startsWith("vir") || netInterface.getName().startsWith("docker")
                        || netInterface.getName().startsWith("contiv")) {
                    continue;
                }
                String realIp = findRealIp(netInterface);
                if (realIp != null) {
                    data.put(netInterface.getName(), realIp);
                }
            }
        } catch (Throwable e) {
        }
        if (data.isEmpty()) {
            return null;
        }
        if (data.size() == 1) {
            return data.values().iterator().next();
        }
        // 多个网卡优先eth0
        String ip = data.remove("eth0");
        if (ip != null) {
            return ip;
        }
        return data.values().iterator().next();
    }

    private static String findRealIp(NetworkInterface networkInterface) {
        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
        List<String> ips = new ArrayList<>();
        InetAddress inetAddress = null;
        while (addresses.hasMoreElements()) {
            inetAddress = addresses.nextElement();
            if (inetAddress == null || !(inetAddress instanceof Inet4Address)) {
                continue;
            }
            if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress()) {
                continue;
            }
            if (inetAddress.isSiteLocalAddress()) {
                ips.add(inetAddress.getHostAddress());
            }
        }
        Enumeration<NetworkInterface> subInterfaces = networkInterface.getSubInterfaces();
        if (subInterfaces != null) {
            NetworkInterface netInterface;
            while (subInterfaces.hasMoreElements()) {
                netInterface = subInterfaces.nextElement();
                // 虚拟接口不会有多个ip了，此处不用递归
                InetAddress subAddress = netInterface.getInetAddresses().nextElement();
                ips.remove(subAddress.getHostAddress());
            }
        }
        // VIP接口配置方式：keepalive的配置方式。在真实ip在第一位，程序读取默认在末尾。
        /**
         * eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast
         * state UP qlen 1000<br>
         * link/ether 1e:00:99:00:00:c1 brd ff:ff:ff:ff:ff:ff<br>
         * inet 10.110.13.142/22 brd 10.110.15.255 scope global eth0<br>
         * valid_lft forever preferred_lft forever<br>
         * inet 10.110.15.216/32 scope global eth0<br>
         * valid_lft forever preferred_lft forever<br>
         */
        return (ips.size() > 0 ? ips.get(ips.size() - 1) : null);
    }

    /**
     * 获取本ip的广播地址
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String getBroadcastIp() {
        String broadcastIp = null;

        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface netInterface;
            while (allNetInterfaces.hasMoreElements()) {
                netInterface = allNetInterfaces.nextElement();
                for (InterfaceAddress ia : netInterface.getInterfaceAddresses()) {
                    String ip = ia.getAddress().getHostAddress();
                    if ("127.0.0.1".equals(ip) || "localhost".equals(ip)) {
                        continue;
                    }
                    InetAddress broadcastInetAddress = ia.getBroadcast();
                    if (broadcastInetAddress == null) {
                        continue;
                    }
                    broadcastIp = broadcastInetAddress.getHostAddress();
                    if (broadcastIp != null) {
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException("Read Network Broadcast Ip fail, please check Network first!");
        }

        return broadcastIp;
    }


}
