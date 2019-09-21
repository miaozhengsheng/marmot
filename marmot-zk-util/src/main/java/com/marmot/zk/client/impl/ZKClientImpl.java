package com.marmot.zk.client.impl;

import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch.CloseMode;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.marmot.zk.bean.AclDto;
import com.marmot.zk.client.IZKClient;
import com.marmot.zk.client.exception.ZookeeperException;
import com.marmot.zk.client.leader.NewLeader;
import com.marmot.zk.client.lock.DistributedReadWriteLock;
import com.marmot.zk.client.lock.DistributedReentrantLock;
import com.marmot.zk.constants.ZKConstants;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.event.NewNodeEventManager;
import com.marmot.zk.listener.Listener;
import com.marmot.zk.listener.Operation4internal;
import com.marmot.zk.utils.DesPlus;
import com.marmot.zk.utils.JsonUtil;
import com.marmot.zk.utils.PathUtils;
import com.marmot.zk.utils.SystemUtil;
import com.marmot.zk.utils.UserAuthUtil;
import com.marmot.zk.utils.ZookeeperFactory;

public class ZKClientImpl implements IZKClient {
	


    // 日志
    private static final Logger logger = Logger.getLogger(ZKClientImpl.class);
    // 客户端链接
    private CuratorFramework client;

    // 监听事件由无缓存的类管控
    private NewNodeEventManager nodeEventManager;

    // 链接监听事件
    private final List<ConnectionStateListener> CONNECTION_LISTENERS = Collections
            .synchronizedList(new ArrayList<ConnectionStateListener>());

    public ZKClientImpl(CuratorFramework client, boolean isAuth) {
        this.client = client;
        nodeEventManager = new NewNodeEventManager(client, this, isAuth);
    }

    // 获取监听事件
    public NewNodeEventManager getNodeEventManager() {
        return nodeEventManager;
    }

    // 获取链接监听事件
    public List<ConnectionStateListener> getConnectionListener() {
        return Lists.newArrayList(CONNECTION_LISTENERS);
    }

    @Override
    public List<String> getSubNodes(EnumZKNameSpace namespace, String path) throws ZookeeperException {

        // 获取全路径
        String fullPath = PathUtils.joinPath(namespace, path);
        // 从zk读取数据
        return Operation4internal.getSubNodes(client, fullPath);
    }

    @Override
    public Map<String, Object> getMap(EnumZKNameSpace namespace, String path) throws ZookeeperException {
        // 获取全路径
        String fullPath = PathUtils.joinPath(namespace, path);
        // 从zk读取数据
        String value = Operation4internal.getValue(client, fullPath);
        if (value == null) {
            // 仅在节点不存在的情况下，值才为null
            return null;
        }
        Map<String, Object> results = StringUtils.isBlank(value) ? Maps.newHashMap() : JsonUtil.json2map(value);
        if (results == null) {
            throw new ZookeeperException("node value is not json data,path=" + path + ",value=" + value);
        }
        return results;
    }

    @Override
    public String getString(EnumZKNameSpace namespace, String path) throws ZookeeperException {
        // 获取全路径
        String fullPath = PathUtils.joinPath(namespace, path);
        // 从zk读取数据
        return Operation4internal.getValue(client, fullPath);
    }

    @Override
    public boolean exist(EnumZKNameSpace namespace, String path) throws ZookeeperException {
        // 获取全路径
        String fullPath = PathUtils.joinPath(namespace, path);
        return Operation4internal.exist(client, fullPath);
    }

    @Override
    public boolean addTempNode(EnumZKNameSpace namespace, String path) throws ZookeeperException {

        // 获取全路径
        String fullPath = PathUtils.joinPath(namespace, path);
        return Operation4internal.addNode(client, fullPath, CreateMode.EPHEMERAL);
    }

    
    @Override
    public boolean addNode(EnumZKNameSpace namespace, String path) throws ZookeeperException {
        // 获取全路径
        String fullPath = PathUtils.joinPath(namespace, path);
        return Operation4internal.addNode(client, fullPath, CreateMode.PERSISTENT);
    }

    @Override
    public boolean deleteNode(EnumZKNameSpace namespace, String path) throws ZookeeperException {
        // 获取全路径
        String fullPath = PathUtils.joinPath(namespace, path);
        return Operation4internal.deleteNode(client, fullPath);
    }

    @Override
    public void setTempNode4String(EnumZKNameSpace namespace, String path, String value) throws ZookeeperException {

        if (value == null) {
            throw new RuntimeException("value can not be null,path=" + path);
        }
        if (!exist(namespace, path)) {
            // 节点不存在
            addTempNode(namespace, path);
        }
        // 获取全路径
        String fullPath = PathUtils.joinPath(namespace, path);
        Operation4internal.setValue(client, fullPath, value);
    }

    @Override
    public void setTempNode4Map(EnumZKNameSpace namespace, String path, Map<String, Object> value)
            throws ZookeeperException {

        if (value == null) {
            throw new RuntimeException("value can not be null,path=" + path);
        }

        String jsonData = (value != null) ? StringUtils.stripToEmpty(JsonUtil.toJson(value)) : "";
        setTempNode4String(namespace, path, jsonData);
    }

    @Override
    public void setNode4Map(EnumZKNameSpace namespace, String path, Map<String, Object> value) throws ZookeeperException {

        if (value == null) {
            throw new RuntimeException("value can not be null,path=" + path);
        }
        String jsonData = (value != null) ? StringUtils.stripToEmpty(JsonUtil.toJson(value)) : "";
        setNode4String(namespace, path, jsonData);
    }

    @Override
    public void setNode4String(EnumZKNameSpace namespace, String path, String value) throws ZookeeperException {

        if (value == null) {
            throw new RuntimeException("value can not be null,path=" + path);
        }
        if (!exist(namespace, path)) {
            // 节点不存在
            addNode(namespace, path);
        }
        // 获取全路径
        String fullPath = PathUtils.joinPath(namespace, path);
        Operation4internal.setValue(client, fullPath, value);
    }

    @Override
    public void addListener(Listener listener, boolean single) throws ZookeeperException {
        nodeEventManager.initListener(listener, single);
    }

    @Override
    public void addListener(Listener listener) throws ZookeeperException {
        addListener(listener, false);
    }

    @Override
    public void addConnectionStateListener(ConnectionStateListener listener) throws ZookeeperException {
        if (listener != null)
            CONNECTION_LISTENERS.add(listener);
    }

    @Override
    public void removeListener(Listener listener) throws ZookeeperException {
        if (listener == null) {
            return;
        }
        nodeEventManager.removeListener(listener);
    }

    @Override
    public NewLeader createLeader(String selectNode, String id) throws ZookeeperException {
        if (selectNode == null || selectNode.length() == 0) {
            return null;
        }
        if (id == null || id.length() == 0) {
            StringBuilder idBuilder = new StringBuilder(SystemUtil.getInNetworkIp());
            String pod = System.getProperty("pod");
            if (pod != null) {
                idBuilder.append('_').append(pod);
            }
            id = idBuilder.toString();
        }
        String path = ZKConstants.LEADER_PREFIX + selectNode;
        try {
            Operation4internal.addNode(client, PathUtils.joinPath(EnumZKNameSpace.PROJECT, path), CreateMode.PERSISTENT);
        } catch (Exception e) {
        }

        try {
            List<String> allSelector = getSubNodes(EnumZKNameSpace.PROJECT, path);
            if (allSelector != null) {
                for (String node : allSelector) {
                    // 如果相同ID的选举节点存在，先清理
                    if (id.equals(getString(EnumZKNameSpace.PROJECT, path + "/" + node))) {
                        try {
                            Operation4internal.deleteNode(client,
                                    PathUtils.joinPath(EnumZKNameSpace.PROJECT, path + "/" + node));
                        } catch (Exception e) {
                            // 忽略
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        return new NewLeader(client, nodeEventManager, EnumZKNameSpace.PROJECT, path, id, CloseMode.NOTIFY_LEADER);
    }

    @Override
    public NewLeader createLeader(String selectNode) throws ZookeeperException {
        StringBuilder idBuilder = new StringBuilder(SystemUtil.getInNetworkIp());
        String pod = System.getProperty("pod");
        if (pod != null) {
            idBuilder.append('_').append(pod);
        }
        return createLeader(selectNode, idBuilder.toString());
    }

    @Override
    public DistributedReentrantLock createDistributedReentrantLock(String lockNode) throws ZookeeperException {
        if (lockNode == null || lockNode.length() == 0) {
            return null;
        }
        return new DistributedReentrantLock(client, EnumZKNameSpace.PROJECT, ZKConstants.LOCK_PREFIX + lockNode);
    }

    @Override
    public DistributedReadWriteLock createDistributedReadWriteLock(String lockNode) throws ZookeeperException {

        if (lockNode == null || lockNode.length() == 0) {
            return null;
        }
        return new DistributedReadWriteLock(client, EnumZKNameSpace.PROJECT, ZKConstants.LOCK_PREFIX + lockNode);
    }

    @Override
    public Object getSelfConfigDataByKey(String key) throws ZookeeperException {
        Map<String, Object> config = getMap(EnumZKNameSpace.PROJECT, PathUtils.getCurrentClientId() + "/config");
        return config != null ? config.get(key) : null;
    }

    @Override
    public void destroy() {
        Operation4internal.destroy(nodeEventManager, client);
    }

    public List<AclDto> getAcls(EnumZKNameSpace namespace, String path) throws ZookeeperException {

        // 获取节点全路径
        String fullPath = PathUtils.joinPath(namespace, path);
        List<ACL> acls = Operation4internal.getAcl(client, fullPath);
        List<AclDto> aclDtos = Lists.newArrayList();

        if (acls == null) {
            return aclDtos;
        }

        for (ACL acl : acls) {
            if (!ZKConstants.ZK_SCHEMA.equals(acl.getId().getScheme())) {
                // 不是digest鉴权模式，即为world鉴权模式-->该节点没有设置acl
                continue;
            }
            AclDto aclDto = new AclDto(acl.getId().getId().split(ZKConstants.ZK_INTERVAL)[0], acl.getPerms());
            aclDtos.add(aclDto);
        }
        return aclDtos;
    }

    public boolean updateAcls(EnumZKNameSpace namespace, String path, List<AclDto> aclDtos) throws ZookeeperException {
        // 获取节点全路径
        String fullPath = PathUtils.joinPath(namespace, path);
        List<ACL> acls = Lists.newArrayList();
        if (aclDtos == null || aclDtos.size() == 0) {
            // 无需Acl控制-->切换为world模式
            // setAcl fullpath world:anyone:crwda
            // 31即拥有所有权限
            acls.add(new ACL(31, new Id("world", "anyone")));
            Operation4internal.setAcl(client, fullPath, acls);
            return true;
        }
        for (AclDto aclDto : aclDtos) {

            if (StringUtils.isBlank(aclDto.getUserName()) || aclDto.getPermissions() == null
                    || aclDto.getPermissions().size() == 0) {
                // 参数错误 设置失败
                logger.error("parms error,parms=" + aclDto + ",path=" + fullPath);
                return false;
            }
            Integer perms = aclDto.getNumber4Perm();
            try {
                acls.add(new ACL(perms, new Id(ZKConstants.ZK_SCHEMA, DigestAuthenticationProvider.generateDigest(
                        aclDto.getUserName() + ZKConstants.ZK_INTERVAL + getPwdByName(aclDto.getUserName())))));
            } catch (Exception e) {
                throw new ZookeeperException("set acl error,path=" + fullPath, e);
            }
        }

        // setAcl fullpath digest:user:pwd:crdwa
        Operation4internal.setAcl(client, fullPath, acls);
        return true;
    }

    public Stat getStat(EnumZKNameSpace namespace, String path) throws ZookeeperException {
        String fullPath = PathUtils.joinPath(namespace, path);
        return Operation4internal.getStat(client, fullPath);
    }

    public long getSessionId() throws ZookeeperException {
        return Operation4internal.getSessionId(client);
    }

    public String getEncryptPwd(String userName) throws ZookeeperException {
        return UserAuthUtil.getEncryptPwdByName(client, userName);
    }

    public void setNode4Compress(EnumZKNameSpace namespace, String path, CreateMode mode, String value)
            throws ZookeeperException {
        boolean isExist = exist(namespace, path);
        if (!isExist) {
            Operation4internal.addNode(client, PathUtils.joinPath(namespace, path), mode);
        }
        Operation4internal.setValue4Compress(client, PathUtils.joinPath(namespace, path), value);
    }

    private String getPwdByName(String userName) throws ZookeeperException {
        // dev、qa无需解密 online解密
        String encryptPwd = getEncryptPwd(userName);
        if (UserAuthUtil.isOnline()) {
            String pk = Security.getProperty(ZKConstants.ZK_SECURITY_PK);
            if (StringUtils.isBlank(pk) || pk.length() < 8) {
                throw new RuntimeException("ZK_SECURITY_PV error!");
            }
            return DesPlus.getInstance().decrypt(encryptPwd, pk);
        }
        return encryptPwd;
    }

}
