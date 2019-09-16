package com.marmot.common.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.util.CollectionUtils;

import com.marmot.common.rpc.scanner.RpcClientFinder;
import com.marmot.zk.constants.ZKConstants;
import com.marmot.zk.enums.EnumZKNameSpace;
import com.marmot.zk.utils.ZookeeperFactory;

public class RpcCallHandler implements InvocationHandler{

	
	private static final Map<String, List<String>> SERVER_NODE_LIST = new ConcurrentHashMap<String, List<String>>();
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		
		Class<?> clazz = method.getDeclaringClass();
		
		String remoteClient = RpcClientFinder.getRemoteClient(clazz);
		
		if(StringUtils.isBlank(remoteClient)){
			throw new Exception("调用的服务不存在");
		}

		// 查询zk上该项目注册的服务
		
		List<String> listSubNodes = SERVER_NODE_LIST.get(remoteClient);
		if(CollectionUtils.isEmpty(listSubNodes)){		
			if(ZookeeperFactory.useDefaultZookeeper().exist(EnumZKNameSpace.PROJECT,ZKConstants.getProjectRpcNode(remoteClient))){
				listSubNodes =	ZookeeperFactory.useDefaultZookeeper().getSubNodes(EnumZKNameSpace.PROJECT,ZKConstants.getProjectRpcNode(remoteClient));
			}
			if(!CollectionUtils.isEmpty(listSubNodes)){
				SERVER_NODE_LIST.put(remoteClient, listSubNodes);
			}
		}
			
		if(listSubNodes==null||listSubNodes.isEmpty()){
			throw new Exception("无可用的节点");
		}
		
		int nextInt = RandomUtils.nextInt(listSubNodes.size());
		
		String targetAddr = listSubNodes.get(nextInt);
		
		String[] split = targetAddr.split(":");
		
		String ip = split[0];
		int port = Integer.parseInt(split[1]);
		
		NioRemoteCallProcessor callProcessor = new NioRemoteCallProcessor(method.getName(),method.getDeclaringClass().getName(), args);
		
		return callProcessor.callRemote(ip, port);
	}

}
