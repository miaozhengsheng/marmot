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
		// ɨ�����е�rpc����
		RpcScanner.scanPackage("com.marmot.**.service.*");
		// ��ʼ��Զ�̵��õĶ�Ӧ��ϵ
		try {
			RPCUtil.initRpcMapper();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// �����Ŀ�ṩ��RPC���� ��Ҫ����Ŀע�ᵽZK��
		IZKClient client = ZKUtil.getZkClient();
		client.createNode(EnumZKNameSpace.PUBLIC, ZKConstants.getProjectRpcNode("test"));
		
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		super.contextDestroyed(event);
	
	}

	
	

}
