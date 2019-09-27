package com.marmot.cache.utils;

public interface IHeartbeatReporting {
	  /* 上报redis节点心跳状态变化数据
	     * 
	     * @param ip redis节点地址
	     * @param port redis节点端口
	     * @param status 检测连通状态，false:断开 true:恢复
	     * @param time 发生的时间戳
	     */
	    public void report(String ip, int port, boolean status, long time);

}
