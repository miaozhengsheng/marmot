package com.marmot.zk.constants;

import com.marmot.zk.utils.PathUtils;

public class ZKConstants {
	
	public static final String ROOT = "/config";
	
	public static final String PUBLIC = "/public";
	
	public static final String RPC = "/rpc";
	
	public static final String RPC_REIGISTRY = "/registry";
	
	// zookeeper集群地址
    public static final String ZK_URL_DEV = "192.168.117.130:2181,192.168.117.131:2181,192.168.117.132:2181";
    public static final String ZK_URL_QA = "192.168.117.130:2181,192.168.117.131:2181,192.168.117.132:2181";
    public static final String ZK_URL_ONLINE = "192.168.117.130:2181,192.168.117.131:2181,192.168.117.132:2181";

    // zookeeper节点最大层级设置key，其值为数值
    public static final String KEY_MAX_LEVEL = "max_level";
    // zookeeper节点名最大字符数设置key，其值为数值
    public static final String KEY_MAX_LENGTH = "max_length";

    // TODO zk Acl 鉴权模式
    public static final String ZK_SCHEMA = "digest";

    // TODO zk Acl 用户名密码间隔标志
    public static final String ZK_INTERVAL = ":";

    // TODO zk 配置路径
    public static final String ZK_CONF_ROOT = "/20078/config";
    // TODO ACL配置路径
    public static final String ZK_ACL_CONF_ROOT = "/20078/config/aclconfig";

    // TODO zk 伪逻辑读权限列表名
    public static final String KEY_CLIENT_RO = "client_ro";

    // TODO zk 伪逻辑写权限列表名
    public static final String KEY_CLIENT_WRITE = "client_w";

    // TODO zk集群根节点
    public static final String ZK_ROOT_NODE = "/";

    public static final String RESERVED_PATH_NAME = "zookeeper";

    // TODO zookeeper admin账号
    public static final String ZK_ADMIN_USER = "admin";

    // TODO zookeeper client账号
    public static final String ZK_CLIENT_USER = "client";

    // TODO java.security文件中私钥的键值
    public static final String ZK_SECURITY_PK = "zk.security.pk";

    public static final int DEFAULT_SESSION_TIMEOUT_MS = 30 * 1000;// 30秒会话超时

    public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 20 * 1000;// 20秒链接超时

    public static final String USER_ACCESS_NODE = "access";// 密文存储节点名
	
	
	public static String getProjectRpcNode(String projectName){
		return RPC+RPC_REIGISTRY+"/"+projectName;
	}
	// 锁规范路径前缀
    public static final String LOCK_PREFIX = "/10000/lock/" + PathUtils.getCurrentClientId() + "/";
    // 选举规范路径前缀
    public static final String LEADER_PREFIX = "/10000/leader/" + PathUtils.getCurrentClientId() + "/";
   
	

}
