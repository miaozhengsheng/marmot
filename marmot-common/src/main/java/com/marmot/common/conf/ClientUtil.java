package com.marmot.common.conf;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import com.marmot.common.other.StaggerTime;
import com.marmot.common.util.IoUtil;
import com.marmot.common.util.IoUtil.FileLoading;
import com.marmot.common.util.PropUtil;
import com.marmot.zk.client.IZKClient;
import com.marmot.zk.client.exception.ZookeeperException;
import com.marmot.zk.enums.EnumChangedEvent;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.listener.NewNodeListener;
import com.marmot.zk.utils.ZookeeperFactory;

public class ClientUtil {


    private static final Logger logger = Logger.getLogger(ClientUtil.class);

    /**
     * 项目名
     */
    private static final String PROJECT_NAME;
    /**
     * 项目clientId
     */
    private static final String CLIENT_ID;

    /**
     * zookeeper节点名称
     */
    private static final String NODE_NAME = "common/clientIdInfo";

    /**
     * client_id => project_name 缓存
     */
    private static final AtomicReference<Map<String, String>> C2P = new AtomicReference<Map<String, String>>(
            new HashMap<String, String>());
    /**
     * project_name => client_id 缓存
     */
    private static final AtomicReference<Map<String, String>> P2C = new AtomicReference<Map<String, String>>(
            new HashMap<String, String>());

    static {
        PROJECT_NAME = PropUtil.getInstance().get("project.name");
        CLIENT_ID = PropUtil.getInstance().get("project.client_id");

        final Map<String, String> c2p = new HashMap<String, String>();
        final Map<String, String> p2c = new HashMap<String, String>();
        boolean change = false;
        try {
            change = loadRemote(ZookeeperFactory.useDefaultZookeeper(), c2p, p2c);
        } catch (Exception e) {
            logger.warn("ClientUtil load " + NODE_NAME + " from zookeeper fail", e);
        }
        if (!change) {
            logger.warn("ClientUtil load " + NODE_NAME + " from zookeeper no data, instead of native load");
            loadNative(c2p, p2c);
        }
        referenceRefresh(c2p, p2c);
        try {
			createListener();
		} catch (ZookeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * 从zookeeper加载clientInfo信息
     * 
     * @param zkclient
     * @param c2p
     * @param p2c
     * @return
     * @throws Exception
     */
    private static boolean loadRemote(IZKClient zkclient, final Map<String, String> c2p, final Map<String, String> p2c)
            throws Exception {
        Map<String, Object> data = readZkData(zkclient);
        if (data == null || data.isEmpty()) {
            return false;
        }
        logger.warn("ClientUtil load " + NODE_NAME + " from zookeeper " + data.size() + " lines");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            // client_id : project_name
            c2p.put(entry.getKey(), (String) entry.getValue());
            p2c.put((String) entry.getValue(), entry.getKey());
        }
        return true;
    }

    /**
     * 获取节点信息
     * 
     * @param zkclient
     * @return
     * @throws ZookeeperException 
     */
    private static Map<String, Object> readZkData(final IZKClient zkclient) throws ZookeeperException {
        return zkclient.getMap(EnumZKNameSpace.PUBLIC, NODE_NAME);
    }

    /**
     * 监听节点变化
     * @throws ZookeeperException 
     */
    private static void createListener() throws ZookeeperException {
        ZookeeperFactory.useDefaultZookeeper().addListener(new NewNodeListener() {

            @Override
            public String listeningPath() {
                return EnumZKNameSpace.PUBLIC.getNamespace() + "/" + NODE_NAME;
            }

			@Override
			public void nodeChanged(IZKClient zookeeperClient,
					EnumChangedEvent type) {
				 final Map<String, String> c2p = new HashMap<String, String>();
                final Map<String, String> p2c = new HashMap<String, String>();
                boolean change = false;
                // 错开时间
                StaggerTime.waited();
                try {
                    change = loadRemote(ZookeeperFactory.useDefaultZookeeper(), c2p, p2c);
                } catch (Exception e) {
                    logger.warn("ClientUtil load " + NODE_NAME + " from zookeeper fail", e);
                }
                if (change) {
                    referenceRefresh(c2p, p2c);
                }
				
			}

        });
    }

    private static void referenceRefresh(final Map<String, String> c2p, final Map<String, String> p2c) {
        C2P.set(c2p);
        P2C.set(p2c);
    }

    /**
     * 加载本地字典
     * 
     * @param c2p
     * @param p2c
     */
    private static void loadNative(final Map<String, String> c2p, final Map<String, String> p2c) {
        InputStream is = ClientUtil.class.getClassLoader().getResourceAsStream("client.dic");
        IoUtil.load(is, "UTF-8", new FileLoading() {

            @Override
            public boolean row(String line, int n) {
                if (line.startsWith("#")) {
                    return true;
                }
                String[] arr = line.split("\\;");
                if (arr.length != 2) {
                    return true;
                }
                String projectName = arr[0].trim();
                String clientId = arr[1].trim();
                c2p.put(clientId, projectName);
                p2c.put(projectName, clientId);
                return true;
            }
        });
    }

    /**
     * 根据项目名取客户端id
     * 
     * @param projectName
     * @return
     */
    public static final String clientId(String projectName) {
        if (projectName == null || projectName.length() == 0) {
            return "unknow";
        }
        String clientId = P2C.get().get(projectName);
        return (clientId != null) ? clientId : "unknow";
    }

    /**
     * 根据客户端id取项目名
     * 
     * @param clientId
     * @return
     */
    public static final String projectName(String clientId) {
        if (clientId == null || clientId.length() == 0) {
            return "unknow";
        }
        String projectName = C2P.get().get(clientId);
        return (projectName != null) ? projectName : "unknow";
    }

    /**
     * 获取本项目名
     * 
     * @return
     */
    public final static String getProjectName() {
        return PROJECT_NAME;
    }

    /**
     * 获取本项目clientId
     * 
     * @return
     */
    public final static String getClientId() {
        return CLIENT_ID;
    }

    /**
     * 返回所有客户端id和项目名对应关系列表<br>
     * client_id => project_name
     * 
     * @return
     */
    public static final Map<String, String> getAll() {
        return Collections.unmodifiableMap(C2P.get());
    }


}
