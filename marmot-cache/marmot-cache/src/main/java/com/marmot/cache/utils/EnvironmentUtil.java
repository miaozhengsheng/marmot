package com.marmot.cache.utils;

import java.util.Set;

import com.marmot.cache.utils.ClusterUtil.Agreement;
import com.marmot.common.system.SystemUtil;
import com.marmot.common.system.SystemUtil.LogicArea;
import com.marmot.common.system.SystemUtil.SystemRuntimeEnv;

public class EnvironmentUtil {


    /**
     * memcached是否调试模式
     * 
     * @param projectName
     * @return
     */
    public static boolean mayDebugMemcached(String projectName) {
        return mayDebug(projectName, Agreement.memcached);
    }

    /**
     * redis是否调试模式
     * 
     * @param projectName
     * @return
     */
    public static boolean mayDebugRedis(String projectName) {
        return mayDebug(projectName, Agreement.redis);
    }

    /**
     * 是否预上线环境
     * 
     * @return
     */
    public static boolean isSandboxArea() {
        return LogicArea.sandbox == SystemUtil.getLogicAreaEnum();
    }

    /**
     * 是否可以本机调试
     * <p>
     * 可以在研发或qa环境调试
     * 
     * @param projectName
     * @param agreement
     * @return
     */
    private static boolean mayDebug(String projectName, Agreement agreement) {
        SystemRuntimeEnv env = SystemUtil.getSystemRuntimeEnvironment();
        if (SystemRuntimeEnv.online != env) {
            Set<String> serversFromFile = ClusterUtil.getClustersFromFile(projectName, agreement);
            if (serversFromFile != null && serversFromFile.size() > 0) {
                return true;
            }
        }
        return false;
    }


}
