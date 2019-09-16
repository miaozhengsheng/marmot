package com.marmot.zk.utils;

import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;

import com.google.common.collect.Maps;
import com.marmot.zk.client.exception.ZookeeperException;
import com.marmot.zk.constants.ZKConstants;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.listener.Operation4internal;

public class UserAuthUtil {
	

    // 为闪电用户生成随机密码
    private static String randomString = "abcdefghijklmnopqrstuvwxyz0123456789";

    @SuppressWarnings("unchecked")
    public static String getEncryptPwdByName(CuratorFramework client, String userName) throws ZookeeperException {
        if (ZKConstants.ZK_ADMIN_USER.equals(userName)) {
            // 管理员
            return isOnline() ? "f3370ba9f1525b75e7fd93275ae9d4ded385b8a3e46a62f6" : "12345";
        }

        if (ZKConstants.ZK_CLIENT_USER.equals(userName)) {
            // 客户端
            return isOnline() ? "a853efddb4c7e5ac7f8c76568cd45329f3dafb7f46275f64" : "12345";
        }

        // 存放权限的地址
        String path = PathUtils.joinPath(EnumZKNameSpace.PROJECT, ZKConstants.ZK_ACL_CONF_ROOT);

        String configStr = Operation4internal.getValue(client, path);
        Map<String, Object> config = StringUtils.isBlank(configStr) ? Maps.newHashMap() : JsonUtil.json2map(configStr);

        Map<String, String> pwds = null;
        if (config != null && config.size() > 0) {
            // 获取密码
            pwds = (Map<String, String>) config.get(ZKConstants.USER_ACCESS_NODE);
        }

        if (pwds != null) {
            if (pwds.get(userName) != null) {
                // 返回密码
                return pwds.get(userName);
            }
        }

        // 随机生成六位数密码
        Random random = new Random();
        StringBuilder pwd = new StringBuilder("");
        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(36);
            pwd.append(randomString.charAt(index));
        }
        // dev、qa无需加密 online加密
        String encrypt = pwd.toString();
        if (isOnline()) {
            // 获取密钥
            String pk = Security.getProperty(ZKConstants.ZK_SECURITY_PK);
            if (StringUtils.isBlank(pk) || pk.length() < 8) {
                throw new RuntimeException("ZK_SECURITY_PV error!");
            }
            // 加密
            encrypt = DesPlus.getInstance().encrypt(pwd.toString(), pk);
        }

        if (pwds == null) {
            pwds = new HashMap<String, String>();
        }
        pwds.put(userName, encrypt);
        config.put(ZKConstants.USER_ACCESS_NODE, pwds);
        // 存入密码
        Operation4internal.setValue(client, path, StringUtils.stripToEmpty(JsonUtil.toJson(config)));
        return encrypt;
    }

    // 判断环境是否为线上
    public static boolean isOnline() {
        String env = System.getProperty("SystemRuntimeEnvironment");
        if (env == null) {
            throw new IllegalArgumentException("【zookeeper】System Property(SystemRuntimeEnvironment) is null");
        }
        return env.equals("online");
    }


}
