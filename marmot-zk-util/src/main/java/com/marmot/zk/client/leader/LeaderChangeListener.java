package com.marmot.zk.client.leader;

public interface LeaderChangeListener {

    /**
     * leader变更事件监听
     * 
     * @param isSelfLeader
     *            若leader发生改变，通知客户端自己是否是新的leader
     */
    public void leaderChanged(boolean isSelfLeader);
}
