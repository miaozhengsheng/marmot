package com.marmot.zk.listener;

import com.marmot.zk.client.IZKClient;
import com.marmot.zk.enums.EnumChangedEvent;

public interface NewNodeChildListener extends Listener{
	  /**
     * 子节点变化（增加/删除/内容更新）时触发
     * 
     * @param zookeeperClient zookeeperClient实例
     * @param childName 改变的子节点名
     * @param type 事件类型(CHILD_ADDED, CHILD_UPDATED, CHILD_REMOVED)
     */
    public void childChanged(IZKClient zookeeperClient, String childName, EnumChangedEvent type);

}
