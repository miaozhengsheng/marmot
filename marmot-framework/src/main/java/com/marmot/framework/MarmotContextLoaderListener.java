package com.marmot.framework;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContextEvent;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.context.ContextLoaderListener;

import com.marmot.common.nioserver.NioServer;
import com.marmot.common.rpc.scanner.RpcClientFinder;
import com.marmot.common.util.PropUtil;
import com.marmot.common.zk.EnumZKNameSpace;
import com.marmot.common.zk.ZKConstants;
import com.marmot.common.zk.ZKUtil;
import com.marmot.common.zk.client.IZKClient;

public class MarmotContextLoaderListener extends ContextLoaderListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		super.contextInitialized(event);
		// ��ʼ�� ���ط����Զ�̷�����Ϣ
		RpcClientFinder.getInstance().load();
		// У���ṩ��rpc�Ƿ��Ѿ�ʵ�� �������δʵ�ֵ�rpc�ӿ� �����ܷ���
		validateRpcService();
		// �����Ŀ�ṩ��RPC���� ��Ҫ����Ŀע�ᵽZK��
		IZKClient client = ZKUtil.getZkClient();
		client.createNode(EnumZKNameSpace.PUBLIC,
				ZKConstants.getProjectRpcNode(PropUtil.getInstance().get("project-name")));
		// �ڶ˿�������NIO����
		try {
			NioServer.startServer(7777);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		super.contextDestroyed(event);

		NioServer.stopServer();
	}
	
	private void validateRpcService() throws RuntimeException{
		if(RpcClientFinder.getLocalServiceClass().isEmpty()){
			return;
		}
		
		List<Class<?>> localServiceClass = RpcClientFinder.getLocalServiceClass();
		
		for(Class<?> clazz:localServiceClass){
			try {
				getCurrentWebApplicationContext().getBean(clazz);
			} catch (NoSuchBeanDefinitionException e) {
				throw new RuntimeException("�ṩ��rpc����"+clazz.getName()+" δ�Ҵ��Ӧ��ʵ�֣���ȷ��");
			}
			
			
		}
	}
	

}
