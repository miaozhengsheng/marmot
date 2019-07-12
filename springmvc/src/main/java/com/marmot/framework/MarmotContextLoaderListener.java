package com.marmot.framework;


import javax.servlet.ServletContextEvent;

import org.springframework.web.context.ContextLoaderListener;

import com.marmot.common.rpc.scanner.RpcScanner;
import com.marmot.common.zk.EnumZKNameSpace;
import com.marmot.common.zk.ZKConstants;
import com.marmot.common.zk.ZKUtil;
import com.marmot.common.zk.client.IZKClient;
import com.marmot.framework.util.RPCUtil;

public class MarmotContextLoaderListener extends ContextLoaderListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		super.contextInitialized(event);
		// 扫描所有的rpc服务
		RpcScanner.scanPackage("com.marmot.**.service.*");
		// 初始化远程调用的对应关系
		try {
			RPCUtil.initRpcMapper();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 如果项目提供了RPC服务 需要将项目注册到ZK上
		IZKClient client = ZKUtil.getZkClient();
		client.createNode(EnumZKNameSpace.PUBLIC, ZKConstants.getProjectRpcNode("test"));
		
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		super.contextDestroyed(event);
	
	}

	
	

}
