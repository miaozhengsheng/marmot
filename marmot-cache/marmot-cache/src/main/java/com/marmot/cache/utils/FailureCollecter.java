package com.marmot.cache.utils;

import org.apache.log4j.Logger;

import com.marmot.common.conf.ClientUtil;
import com.marmot.common.system.SystemUtil;
import com.marmot.common.util.Log4jUtil;

public class FailureCollecter {


    private static final Logger logger = Log4jUtil.register2("redisfail", "failure");

    public static void redisFailure(String actionUrl, String method, String key, String address, long createTime) {
        StringBuilder stat = new StringBuilder();
        /**
         * 应用ip，应用clientId，缓存节点ip:port，时间戳，耗时，请求url，调用方法，请求key
         */
        stat.append(SystemUtil.getInNetworkIp());
        stat.append(",");
        stat.append(ClientUtil.getClientId());
        stat.append(",");
        stat.append(address);
        stat.append(",");
        stat.append(createTime);
        stat.append(",");
        stat.append(System.currentTimeMillis() - createTime);
        stat.append(",");
        stat.append(actionUrl);
        stat.append(",");
        stat.append(method);
        stat.append(",");
        stat.append(key);
        // 日志输出
        logger.info(stat.toString());
    }


}
