package com.marmot.common.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;

import com.marmot.common.zk.EnumZKNameSpace;
import com.marmot.common.zk.ZKConstants;
import com.marmot.common.zk.ZKUtil;

public class RpcCallHandler implements InvocationHandler{

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		
		// 获取方法所在的工程的名称
		String projectName = "base-project";

		// 查询zk上该项目注册的服务
		
		List<String> listSubNodes = ZKUtil.getZkClient().listSubNodes(EnumZKNameSpace.PUBLIC,ZKConstants.getProjectRpcNode(projectName));
			
		if(listSubNodes==null||listSubNodes.isEmpty()){
			throw new Exception("无可用的节点");
		}
		
		int nextInt = RandomUtils.nextInt(listSubNodes.size());
		
		String targetAddr = listSubNodes.get(nextInt);
		
		String[] split = targetAddr.split(":");
		
		String ip = split[0];
		int port = Integer.parseInt(split[1]);
		
		System.out.println("注册的服务的IP为："+ip+" 端口为:"+port);
		
		//进行RPC调用
		
		
		NioRemoteCallProcessor callProcessor = new NioRemoteCallProcessor(method.getName(),method.getDeclaringClass().getName(), args);
		
		return callProcessor.callRemote(ip, port);
	}

}
