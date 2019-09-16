package com.marmot.zk.client;

import org.apache.log4j.Logger;

import com.marmot.zk.utils.ZookeeperFactory;


public class ZKClientWrapper {


    private static final Logger logger = Logger.getLogger(ZKClientWrapper.class);

    // 该方法可以允许不初始化zk链接，直接关闭
    public static void destroy() {

        try {
            // 关闭新连接
            IZKClient zookeeperClient = ZookeeperFactory.useDefaultZookeeper();
            if (zookeeperClient != null) {
                zookeeperClient.destroy();
            }
        } catch (Exception e) {
            logger.error("关闭zk链接异常", e);
        }
    }

}
