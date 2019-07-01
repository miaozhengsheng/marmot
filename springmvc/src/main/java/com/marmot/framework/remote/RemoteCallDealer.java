package com.marmot.framework.remote;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.marmot.common.rpc.bean.MarmotRpcBean;

public class RemoteCallDealer {
	
	public Object deal(MarmotRpcBean rpcBean) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		
		Object target = rpcBean.getTarget();
		
		Method method = rpcBean.getMethod();
		
		Object[] parameters = rpcBean.getParameters();
		
		Object invoke = method.invoke(target, parameters);
		
		
		return invoke;
	}

}
