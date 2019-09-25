package com.marmot.cache.utils;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import com.marmot.common.system.SystemUtil;
import com.marmot.common.util.IoUtil;
import com.marmot.common.util.IoUtil.FileLoading;

public class ClusterUtil {


    /**
     * 全局集群配置文件路径
     */
    public static final String CLUSTER_CONF_PATH = "cluster.conf.path";

    /**
     * 全局集群配置文件名
     */
    public static final String CLUSTER_FILENAME = "ins-cluster-debug.conf";

    public static enum Agreement {
        http, memcached, redis, kafka, ssdb, tcp
    }

    /**
     * 读取指定项目所使用的服务协议集群信息 <br>
     * 数据格式：ip:port
     * 
     * @param projectName 项目名
     * @param agreement 协议名称(如http、memcached、redis、tcp等)
     * @param ignoreTag 是否忽略标签的影响
     * @return
     */
    public static Set<String> getClustersFromFile(String projectName, Agreement agreement) {
        File file = checkAndGet();
        if (file == null) {
            return null;
        }
        if (projectName == null || projectName.trim().length() == 0) {
            // try once
            projectName = SystemUtil.getContextName();
        }
        return getServers(file, projectName, agreement.name());
    }

    /**
     * 检查并且获取文件
     */
    public static File checkAndGet() {
        String dir = System.getProperty(CLUSTER_CONF_PATH);
        if (dir == null) {
            return null;
        }
        File file = new File(dir);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        String name = file.getName();
        if (!CLUSTER_FILENAME.equals(name)) {
            return null;
        }
        return file;
    }

    /**
     * 是否可以本机调试
     * <p>
     * 可以在研发或qa环境调试
     * 
     * @param projectName
     * @return
     */
    public static boolean mayDebug(String projectName) {
        if (!SystemUtil.isOnline()) {
            Set<String> serversFromFile = ClusterUtil.getClustersFromFile(projectName, Agreement.http);
            if (serversFromFile != null && serversFromFile.size() > 0) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> getServers(File file, final String projectName, final String agreement) {
        final Set<String> servers = new LinkedHashSet<String>();
        IoUtil.load(file, "UTF-8", new FileLoading() {
            boolean begin = false;
            boolean end = false;

            @Override
            public boolean row(String line, int n) {
                if (ignore(line)) {
                    return true;
                }
                if (begin) {
                    end = isEndRead(line);
                    if (!end) {
                        String server = still(line, agreement);
                        if (server != null) {
                            servers.add(server);
                        }
                    } else {
                        return false;
                    }
                } else {
                    begin = isBeginRead(line, projectName);
                }
                return true;
            }

        });
        return servers;
    }

    private static boolean isBeginRead(String line, String projectName) {
        String str = line.trim();
        return str.startsWith("router") && (str.indexOf(projectName) != -1);
    }

    private static boolean isEndRead(String line) {
        return "}".equals(line.trim());
    }

    private static String still(String line, String agreement) {
        String str = line.trim();
        if (!str.startsWith("#")) {
            if (str.startsWith(agreement)) {
                String server = str.substring(agreement.length(), str.length() - 1);
                return server.trim();
            }
        }
        return null;
    }

    private static boolean ignore(String line) {
        return line.trim().length() == 0 || line.trim().startsWith("#");
    }


}
