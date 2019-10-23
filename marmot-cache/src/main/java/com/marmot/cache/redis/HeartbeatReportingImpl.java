package com.marmot.cache.redis;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.marmot.cache.utils.IHeartbeatReporting;
import com.marmot.common.conf.ClientUtil;
import com.marmot.common.httpclient.HttpSendClient;
import com.marmot.common.httpclient.HttpSendClientFactory;
import com.marmot.common.system.SystemUtil;
import com.marmot.common.system.SystemUtil.LogicArea;
import com.marmot.common.util.JsonUtil;
import com.marmot.common.util.Log4jUtil;
import com.marmot.zk.client.impl.ZKClientImpl;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.utils.ZookeeperFactory;

public class HeartbeatReportingImpl  implements IHeartbeatReporting {

    private static final Logger catalina = Logger.getLogger(HeartbeatReportingImpl.class);

    private final Logger logger;

    private HttpSendClient httpSendClient;

    private static final ZKClientImpl ZKCLIENT = (ZKClientImpl) ZookeeperFactory.useDefaultZookeeper();

    public HeartbeatReportingImpl() {
        this.logger = Log4jUtil.register2("redis", "heartbeat");
        this.httpSendClient = HttpSendClientFactory.getInstance();
    }

    @Override
    public void report(String ip, int port, boolean status, long time) {
        StringBuilder stat = new StringBuilder();
        /**
         * 应用ip，应用clientId，redis节点ip:port，不通/恢复，时间戳
         */
        stat.append(SystemUtil.getInNetworkIp());
        stat.append(",");
        stat.append(ClientUtil.getClientId());
        stat.append(",");
        stat.append(ip);
        stat.append(":");
        stat.append(port);
        stat.append(",");
        stat.append(status);
        stat.append(",");
        stat.append(time);
        // 日志输出
        logger.info(stat.toString());

        // 上报缓存中心
        String address = getAddress();
        if (address == null) {
            catalina.warn("redis心跳异常上报缓存中心失败, 从服务中心获取缓存中心地址为空");
            return;
        }
        String url = "http://" + address + "/heartbeat/report.json";
        Map<String, Object> params = new HashMap<>();
        params.put("clientId", ClientUtil.getClientId());
        params.put("ip", SystemUtil.getInNetworkIp());
        params.put("redis", ip + ":" + port);
        params.put("status", status);
        params.put("time", time);
        try {
            httpSendClient.postAjax(url, params);
        } catch (IOException e) {
            catalina.error("redis心跳异常上报缓存中心失败, 地址：" + url + ", 内容：" + params, e);
        }
    }

    public synchronized void close() {
        httpSendClient.shutdown();
    }

    private String getAddress() {
        try {
            List<String> serviceList = ZKCLIENT.getSubNodes(EnumZKNameSpace.PUBLIC,
                    "/rpc/registry/ins-firefly-internal");
            if (serviceList != null && serviceList.size() > 0) {
                for (String service : serviceList) {
                    String data = ZKCLIENT.getString(
                    		EnumZKNameSpace.PUBLIC ,"/rpc/registry/ins-firefly-internal/" + service);
                    if (data != null) {
                        Map<String, Object> map = JsonUtil.json2map(data);
                        if (map != null) {
                            if (LogicArea.sandbox != LogicArea.get((String) map.get("area"))) {
                                return service;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            catalina.error("get " + EnumZKNameSpace.PUBLIC.getNamespace() + "/rpc/registry/ins-firefly-internal fail", e);
        }
        return null;
    }

}
