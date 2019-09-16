package com.marmot.zk.listener;

public interface Listener {
	/**
     * 返回监听节点的全路径
     * 
     * @return 监听节点的全路径
     */
    public String listeningPath();
}
