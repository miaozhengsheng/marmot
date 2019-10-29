package com.marmot.framework.compnent;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.marmot.common.component.IMarmotComponent;
import com.marmot.common.nioserver.NioServer;
import com.marmot.common.rpc.scanner.RpcClientFinder;
import com.marmot.common.system.SystemUtil;
import com.marmot.common.util.PropUtil;
import com.marmot.zk.client.exception.ZookeeperException;
import com.marmot.zk.constants.ZKConstants;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.utils.ZookeeperFactory;
@Service
public class MarmotZookeeperCompnent implements IMarmotComponent{
	
	private static final Logger logger = Logger.getLogger(MarmotZookeeperCompnent.class);
	
	@Override
	public void initComponent(ApplicationContext applicationContext) {
		
		logger.debug("spring 上下文启动成功，开始向 ZK注册服务节点..");
		RpcClientFinder.getInstance().load();
		validateRpcService(applicationContext);
		if(!RpcClientFinder.getLocalServiceClass().isEmpty()){
			String port = PropUtil.getInstance().get("rpc-port");
			String ip = SystemUtil.getInNetworkIp();
			try {
				ZookeeperFactory.useDefaultZookeeper().addNode(EnumZKNameSpace.PROJECT, ZKConstants.getProjectRpcNode(PropUtil.getInstance().get("project-name")+"/"+ip+":"
				+port));
			} catch (ZookeeperException e1) {
				e1.printStackTrace();
			}
		
			try {
				
				NioServer.startServer(Integer.valueOf(port));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}
	
	
	private void validateRpcService(ApplicationContext applicationContext) throws RuntimeException{
		if(RpcClientFinder.getLocalServiceClass().isEmpty()){
			return;
		}
		
		List<Class<?>> localServiceClass = RpcClientFinder.getLocalServiceClass();
		
		for(Class<?> clazz:localServiceClass){
			try {
				applicationContext.getBean(clazz);
			} catch (NoSuchBeanDefinitionException e) {
				throw new RuntimeException("系统提供的"+clazz.getName()+" 未找到对应的实现类");
			}
			
			
		}
	}

	@Override
	public void distory(ApplicationContext applicationContext) {
		
		logger.debug("spring 上下文停止前，删除zk上注册的节点");
		
		if(!RpcClientFinder.getLocalServiceClass().isEmpty()){
			NioServer.stopServer();
			String port = PropUtil.getInstance().get("rpc-port");
			String ip = SystemUtil.getInNetworkIp();
			try {
				// 删除rpc节点
				ZookeeperFactory.useDefaultZookeeper().deleteNode(EnumZKNameSpace.PROJECT, ZKConstants.getProjectRpcNode(PropUtil.getInstance().get("project-name")+"/"+ip+":"
						+port));
			} catch (ZookeeperException e) {
				e.printStackTrace();
			}
		}		
	}

	@Override
	public int order() {
		return 0;
	}

}
