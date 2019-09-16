package com.marmot.zk.listener;

import com.marmot.zk.client.IZKClient;
import com.marmot.zk.enums.EnumChangedEvent;

public interface NewNodeListener extends Listener{
	/**
     * 节点删除/更新时触发
     * 
     * @param zookeeperClient zookeeperClient实例
     * @param type 事件类型(ADDED, UPDATED, REMOVED)
     */
    public void nodeChanged(IZKClient zookeeperClient, EnumChangedEvent type);
}
