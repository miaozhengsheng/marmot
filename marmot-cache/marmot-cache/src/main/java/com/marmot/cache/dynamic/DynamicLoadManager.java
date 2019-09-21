package com.marmot.cache.dynamic;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.marmot.cache.constants.CacheConst;
import com.marmot.cache.utils.ZKUtil;
import com.marmot.zk.client.IZKClient;
import com.marmot.zk.client.exception.ZookeeperException;
import com.marmot.zk.client.impl.ZKClientImpl;
import com.marmot.zk.enums.EnumChangedEvent;
import com.marmot.zk.listener.NewNodeListener;
import com.marmot.zk.utils.ZookeeperFactory;

public class DynamicLoadManager {


    private static final Logger logger = Logger.getLogger(DynamicLoadManager.class);

    public static final ZKClientImpl ZKCLIENT = (ZKClientImpl) ZookeeperFactory.useDefaultZookeeper();

    protected volatile Set<String> lastCacheConfigSet = new LinkedHashSet<String>();

    private final byte[] lock = new byte[0];

    private String projectName;
    private String groupName;

    private IDynamicLoadHandle dynamicLoadHandle;
    private NewNodeListener currentGroupIdListener;

    public DynamicLoadManager() {
    }

    public void set(String projectName, String groupName) {
        this.projectName = projectName;
        this.groupName = groupName;
    }

    public void setDynamicLoadHandle(IDynamicLoadHandle dynamicLoadHandle) {
        this.dynamicLoadHandle = dynamicLoadHandle;
    }

    /**
     * 初始化监听器
     * @throws ZookeeperException 
     * 
     */
    public void createCacheConfigListener() throws ZookeeperException {
        // 监听 projectInfo
        createCacheProjectListener();
        // 监听 groupId
        createCacheGroupListener();
    }

    private void createCacheProjectListener() throws ZookeeperException {
    	
        ZKCLIENT.addListener(new NewNodeListener() {

            @Override
            public String listeningPath() {
            	try {
            		 return ZKUtil.getProjectInfoPath(projectName, groupName);
				} catch (Exception e) {
					e.printStackTrace();
				}
            	return null;
            }
            @Override
            public void nodeChanged(IZKClient zkclient, EnumChangedEvent type) {
                if (EnumChangedEvent.UPDATED == type) {
                	try {
                		handle();
                        // 删除旧的group监听，增加新监听
                        ZKCLIENT.removeListener(currentGroupIdListener);
                        createCacheGroupListener();
					} catch (Exception e) {
					}
                    
                }
            }

        });
    }

    private void createCacheGroupListener() throws ZookeeperException {
        currentGroupIdListener = new NewNodeListener() {

            @Override
            public String listeningPath() {
                try {
					return ZKUtil.getGroupIdPath(projectName, groupName);
				} catch (ZookeeperException e) {
					e.printStackTrace();
				}
                
                return null;
            }

            @Override
            public void nodeChanged(IZKClient zkclient, EnumChangedEvent type) {
                if (EnumChangedEvent.UPDATED == type) {
                    try {
						handle();
					} catch (ZookeeperException e) {
						e.printStackTrace();
					}
                }
            }

        };
        ZKCLIENT.addListener(currentGroupIdListener);
    }

    private void handle() throws ZookeeperException {
        Set<String> currentCacheConfigSet = null;
            currentCacheConfigSet = ZKUtil.getRedisClusters(projectName, groupName);

        if (currentCacheConfigSet == null || currentCacheConfigSet.isEmpty()) {
            return;
        }
        synchronized (lock) {
            // 是否需要重新加载资源
            if (isNeedReload(lastCacheConfigSet, currentCacheConfigSet)) {
                try {
                    dynamicLoadHandle.invoke(currentCacheConfigSet);
                    lastCacheConfigSet = currentCacheConfigSet;
                } catch (Throwable e) {
                    logger.warn(CacheConst.LOG_PREFIX + " 重新加载cache资源失败, oldConfig:"
                            + lastCacheConfigSet + " | newConfig:" + currentCacheConfigSet, e);
                }
            }
        }
    }

    /**
     * 是否需要重新加载底层资源
     * 
     * @param lastCacheConfigSet
     * @param currentCacheConfigSet
     * @return
     */
    private boolean isNeedReload(Set<String> lastCacheConfigSet, Set<String> currentCacheConfigSet) {
        // 扩容/缩容
        if (lastCacheConfigSet.size() != currentCacheConfigSet.size()) {
            return true;
        }

        Iterator<String> lastIt = lastCacheConfigSet.iterator();
        Iterator<String> currentIt = currentCacheConfigSet.iterator();
        while (lastIt.hasNext() && currentIt.hasNext()) {
            if (!lastIt.next().equals(currentIt.next())) {
                return true;
            }
        }
        return false;
    }

    public enum CacheType {
        memcached, redis
    }


}
