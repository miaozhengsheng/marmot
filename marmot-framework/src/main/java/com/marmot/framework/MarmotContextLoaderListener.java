package com.marmot.framework;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContextEvent;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.context.ContextLoaderListener;

import com.marmot.common.nioserver.NioServer;
import com.marmot.common.rpc.scanner.RpcClientFinder;
import com.marmot.common.system.SystemUtil;
import com.marmot.common.util.PropUtil;
import com.marmot.common.zk.EnumZKNameSpace;
import com.marmot.common.zk.ZKConstants;
import com.marmot.common.zk.ZKUtil;

public class MarmotContextLoaderListener extends ContextLoaderListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		super.contextInitialized(event);
		// 初始化 本地服务和远程服务信息
		RpcClientFinder.getInstance().load();
		// 校验提供的rpc是否都已经实现 如果存在未实现的rpc接口 怎不能发布
		validateRpcService();
		// 如果提供了远程服务 需要将服务的ip和端口注册到zk上
		if(!RpcClientFinder.getLocalServiceClass().isEmpty()){
		    // 将当前的IP注册到ZK服务器上
			String port = PropUtil.getInstance().get("rpc-port");
			String ip = SystemUtil.getLocalIp();
			ZKUtil.getZkClient().setTempNodeData(EnumZKNameSpace.PROJECT, ZKConstants.getProjectRpcNode(PropUtil.getInstance().get("project-name")+"/"+ip+":"
			+port));
		
			// 在端口上启用NIO服务
			try {
				
				NioServer.startServer(Integer.valueOf(port));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
				throw new RuntimeException("提供的rpc服务"+clazz.getName()+" 未找打对应的实现，请确认");
			}
			
			
		}
	}
	

}
