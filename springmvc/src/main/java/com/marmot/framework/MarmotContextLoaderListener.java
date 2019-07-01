package com.marmot.framework;


import javax.servlet.ServletContextEvent;

import org.springframework.web.context.ContextLoaderListener;

import com.marmot.common.rpc.scanner.RpcScanner;
import com.marmot.framework.util.RPCUtil;

public class MarmotContextLoaderListener extends ContextLoaderListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		super.contextInitialized(event);
		// ɨ�����е�rpc����
		RpcScanner.scanPackage("com.marmot.**.service.*");
		// ��ʼ��Զ�̵��õĶ�Ӧ��ϵ
		try {
			RPCUtil.initRpcMapper();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	

}
