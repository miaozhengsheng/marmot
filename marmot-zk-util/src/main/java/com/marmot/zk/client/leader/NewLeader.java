package com.marmot.zk.client.leader;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.Participant;
import org.apache.curator.framework.recipes.locks.LockInternals;
import org.apache.curator.framework.recipes.locks.LockInternalsSorter;
import org.apache.curator.framework.recipes.locks.StandardLockInternalsDriver;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NotEmptyException;

import com.marmot.zk.client.IZKClient;
import com.marmot.zk.client.exception.ZookeeperException;
import com.marmot.zk.constants.ZKConstants;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.event.NewNodeEventManager;
import com.marmot.zk.listener.NewNodeListener;
import com.marmot.zk.listener.Operation4internal;
import com.marmot.zk.utils.PathUtils;
import com.marmot.zk.utils.SystemUtil;
import com.marmot.zk.enums.EnumChangedEvent;



/**
 * 选举leader 竞争者创建的临时节点丢失分为三种情况 1.客户端主动调用close，此时当前竞选者再无参加选举的权利
 * 2.网络断开,待网络恢复后，该竞选者仍然拥有参加选举的权利 3.通过非正常手段如闪电后台主动删除该节点（绝对禁止这种情况的发生）
 * 
 * 当所有竞争节点网络断开，保持之前的leader不变，网络恢复后选举出新的leader
 * 
 * @author chenhao
 *
 */
public class NewLeader extends LeaderLatch {

    private static final Logger logger = Logger.getLogger(NewLeader.class);
    private static final String LOCK_NAME = "latch-";
    private final CuratorFramework client;
    private final String fullPath;
    private final Object ob = new Object();
    private final NewNodeEventManager nodeEventManager;

    // 排序方式
    private static final LockInternalsSorter sorter = new LockInternalsSorter() {
        @Override
        public String fixForSorting(String str, String lockName) {
            return StandardLockInternalsDriver.standardFixForSorting(str, lockName);
        }
    };

    /**
     * 选举构造器（不要直接用）
     * 
     * @param client
     *            zookeeper连接
     * @param nodeEventManager
     *            监听事件管理对象
     * @param namespace
     *            命名空间NamespaceEnum类型，必须指定
     * @param path
     *            选举路经
     */
    public NewLeader(CuratorFramework client, NewNodeEventManager nodeEventManager, EnumZKNameSpace namespace,
            String path) {
        // 默认IP为当前机器的IP
        this(client, nodeEventManager, namespace, path, StringUtils.defaultString(SystemUtil.getInNetworkIp()),
                CloseMode.NOTIFY_LEADER);
    }

    /**
     * 选举构造器（不要直接用）
     * 
     * @param client
     *            zookeeper连接
     * @param nodeEventManager
     *            监听事件管理对象
     * @param namespace
     *            命名空间NamespaceEnum类型，必须指定
     * @param path
     *            选举路经
     * @param id
     *            参与选举的ID
     * @param closeMode
     *            behaviour of listener on explicit close.
     */
    public NewLeader(CuratorFramework client, NewNodeEventManager nodeEventManager, EnumZKNameSpace namespace,
            String path, String id, CloseMode closeMode) {
        super(client, PathUtils.joinPath(namespace, path), id, closeMode);
        this.fullPath = PathUtils.joinPath(namespace, path);
        this.client = client;
        this.nodeEventManager = nodeEventManager;
        // 强制约束规范
        if (!path.startsWith(ZKConstants.LEADER_PREFIX)) {
            logger.error("【zookeeper】Leader构造参数不规范", new IllegalArgumentException("请使用ZKClient.createLeader()方法"));
        }
    }

    /**
     * 判断当前节点是否是leader
     *
     * @return true/false
     */
    @Override
    public boolean hasLeadership() {
        // 连接已断开，返回false
        if (!Operation4internal.isConnect(client)) {
            return false;
        }

        // 初始后没有开始选举，执行选举
        if (getState() == State.LATENT) {
            try {
                start();
            } catch (Exception e) {
                logger.error("【zookeeper】LeaderLatch.start() failure! ", e);
                // 选举失败肯定不是leader
                return false;
            }
        }

        // 当前节点已经退出选举，直接返回false
        if (getState() == State.CLOSED) {
            logger.warn(
                    "【zookeeper】Leader is closed. id=" + getId() + ", path=" + fullPath + ", hashcode=" + hashCode());
            return false;
        }

        boolean isRealLeader = false;
        // 验证当前节点是否为最小节点
        try {
            List<String> participants = Operation4internal.getSubNodes(client, fullPath);
            if (participants == null || participants.size() == 0) {
                return false;
            }
            // 实时获取选举节点下的最小节点的id
            Participant participant = getLeader();
            if (getId().equals(participant.getId())) {
                isRealLeader = true;
            }
        } catch (Exception e) {
            logger.error("【zookeeper】hasLeadership() 实时获取所有参选节点失败.", e);
        }
        return isRealLeader;
    }

    @Override
    public Participant getLeader() throws ZookeeperException {

        // 重载curator的getLeader方法，避免出现获取leader出现NoNodeException的场景
        try {
            // 获取当前竞争节点的有序集合，排在第一位的为主节点
            Collection<String> participantNodes = LockInternals.getParticipantNodes(client, fullPath, LOCK_NAME,
                    sorter);
            if (participantNodes.size() > 0) {
                Iterator<String> paths = participantNodes.iterator();
                while (paths.hasNext()) {
                    try {
                        String path = paths.next();
                        byte[] bytes = client.getData().forPath(path);
                        String thisId = new String(bytes, "UTF-8");
                        return new Participant(thisId, true);
                    } catch (NoNodeException e) {
                        // 主节点被并发删除，顺序让第二位成为主节点
                        // 出现的场景可能为项目上下线时选举leader
                    }
                }
            }
            // 无leader
            return new Participant("", false);
        } catch (Exception e) {
            // 其余异常场景抛出异常
            throw new ZookeeperException("获取当前主节点失败", e);
        }
    }

    /**
     * 启动leader节点
     * 
     * @throws ZookeeperException
     *             异常
     */
    @Override
    public void start() throws ZookeeperException {

        // 链接已经断开抛出异常
        if (!Operation4internal.isConnect(client)) {
            throw new ZookeeperException("【zookeeper】zookeeper client is not connect,start leader error");
        }

        try {
            synchronized (ob) {
                switch (getState()) {
                case CLOSED:
                    // 已经退出选举，不再接收选举请求
                    throw new ZookeeperException(
                            "【zookeeper】Leader is stopped, cannot be started more than once, hashcode=" + hashCode());
                case LATENT:
                    // 开始选举
                    super.start();
                    break;
                case STARTED:
                    return;
                default:
                    break;
                }
            }

            // 阻塞至leader选举成功
            waitLeader();
        } catch (Exception e) {
            throw new ZookeeperException("【zookeeper】start leader error,", e);
        }
    }

    private void waitLeader() throws ZookeeperException {
        // 尝试100次 防止死循环
        Integer tryTime = 0;
        boolean isTimeOut = true;
        while (tryTime < 100) {
            // 阻塞至选举完成

            try {
                // 休眠100ms，避免频繁操作
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (Exception e) {
                // 忽略
            }

            if (!Operation4internal.isConnect(client)) {
                // 判断zookeeper状态 链接断开退出选举
                logger.warn("【zookeeper】zookeeper client is not connect,start leader error");
                isTimeOut = false;
                break;
            }

            if (getState() != State.STARTED) {
                throw new ZookeeperException(
                        "【zookeeper】Leader is stopped, cannot be started more than once, hashcode=" + hashCode());
            }

            try {
                boolean isLeader = getLeader().isLeader();
                if (isLeader) {
                    // 已经拥有选举者，退出
                    isTimeOut = false;
                    break;
                }
            } catch (Exception e) {
                // 忽略
            }

            tryTime++;
        }

        if (isTimeOut) {
            // 选举超时
            throw new ZookeeperException("【zookeeper】start leader timeout!");
        }
    }

    /**
     * 启动leader选举，带监控器
     * 
     * @param leaderChangeListener
     *            主节点监控器
     * @throws ZookeeperException
     *             异常
     */
    public boolean start(LeaderChangeListener leaderChangeListener) throws ZookeeperException {
        // 选举
        start();
        // 判断自身是否是leader
        boolean isLeader = hasLeadership();
        // 通知自身是否是leader
        leaderChangeListener.leaderChanged(isLeader);
        // 注册
        NewNodeListener listener = getListener(leaderChangeListener);
        nodeEventManager.initListener(listener, false);
        return isLeader;
    }

    private NewNodeListener getListener(LeaderChangeListener leaderChangeListener) {
        // 监听当选为leader的临时节点。节点发生变化仅通知前任leader
        NewNodeListener listener = new NewNodeListener() {

            private String path = getLeaderNodePath();

            public void setPath(String path) {
                this.path = path;
            }

            @Override
            public String listeningPath() {
                return path;
            }

            @Override
            public void nodeChanged(IZKClient zookeeperClient, EnumChangedEvent type) {
                if (type != EnumChangedEvent.REMOVED) {
                    logger.warn("【zookeeper】leader唯一标识被非法改变! path=" + path);
                    return;
                }
                try {
                    // 原leader临时节点被删除
                    // 注销监听器
                    nodeEventManager.removeListener(this);
                    if (getState() == State.STARTED) {
                        // 重新注册
                        waitLeader();
                        String newPath = getLeaderNodePath();
                        if (newPath != null) {
                            // 校验路径
                            PathUtils.checkNodePath(newPath);
                            // 更换监听路径
                            this.setPath(getLeaderNodePath());
                            nodeEventManager.initListener(this, false);
                        }
                    }
                } catch (Exception e) {
                    logger.error("【zookeeper】重新注册leader监听失败", e);
                }
                // 通知leader已经更改
                leaderChangeListener.leaderChanged(hasLeadership());
            }
        };

        return listener;
    }

    private String getLeaderNodePath() {

        // 重载curator的getLeader方法，避免出现获取leader出现NoNodeException的场景
        try {
            // 获取当前竞争节点的有序集合，排在第一位的为主节点
            Collection<String> participantNodes = LockInternals.getParticipantNodes(client, fullPath, LOCK_NAME,
                    sorter);
            if (participantNodes.size() > 0) {
                Iterator<String> paths = participantNodes.iterator();
                while (paths.hasNext()) {
                    String path = paths.next();
                    boolean isExist = Operation4internal.exist(client, path);
                    if (!isExist) {
                        // 主节点已经不存在
                        continue;
                    }
                    return path;
                }
            }
        } catch (Exception e) {
            // 其余异常场景抛出异常
            logger.warn("【zookeeper】获取主节点路径失败", e);
        }
        // 无leader
        return null;
    }

    @Override
    public void close() throws IOException {
        synchronized (ob) {
            if (getState() == State.STARTED) {
                logger.warn(
                        "【zookeeper】close leader. id=" + getId() + ", path=" + fullPath + ", hashcode=" + hashCode());
                super.close(CloseMode.NOTIFY_LEADER);
                deleteParticipantNode();
            }
        }
    }

    @Override
    public void close(CloseMode closeMode) throws IOException {
        synchronized (ob) {
            if (getState() == State.STARTED) {
                logger.warn(
                        "【zookeeper】close leader. id=" + getId() + ", path=" + fullPath + ", hashcode=" + hashCode());
                super.close(closeMode);
                deleteParticipantNode();
            }
        }
    }

    /**
     * Leader关闭后，马上删除参加选举节点的id与当天退出选举id相同的临时节点
     */
    private void deleteParticipantNode() {
        List<String> participants = null;
        try {
            participants = Operation4internal.getSubNodes(client, fullPath);
        } catch (Exception e) {
            // 忽略
        }
        if (participants != null) {
            for (String node : participants) {
                String currentPath = fullPath + "/" + node;
                try {
                    // 如果相同ID的选举节点存在，删除
                    String data = Operation4internal.getValue(client, currentPath);
                    if (data != null && getId().equals(data)) {
                        Operation4internal.deleteNode(client, currentPath);
                        break;
                    }
                } catch (Exception e) {
                    // 忽略
                }
            }
        }
        // 删除节点
        try {
            client.delete().guaranteed().forPath(fullPath);
        } catch (NotEmptyException e) {
            // 若节点下仍有子节点，忽略
        } catch (Exception e) {
            logger.warn("【zookeeper】清除leader信息失败", e);
        }
    }
}
