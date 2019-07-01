package com.marmot.framework;


import javax.servlet.ServletContextEvent;

import org.springframework.web.context.ContextLoaderListener;

import com.marmot.common.rpc.scanner.RpcScanner;
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
	}

	

}
