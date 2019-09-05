package com.marmot.common.proxy;

import java.lang.reflect.Proxy;


public class RpcCallProxy {

	public static <T> T getProxy(Class<T> class1) {
		return (T) Proxy.newProxyInstance(class1.getClassLoader(), new Class[]{class1}, new RpcCallHandler());
	}

}
