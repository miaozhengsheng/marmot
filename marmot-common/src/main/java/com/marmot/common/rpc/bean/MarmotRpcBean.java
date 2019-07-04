package com.marmot.common.rpc.bean;

import java.lang.reflect.Method;


public class MarmotRpcBean {
	
	
	private Object target;
	
	private Method method;
	
	private String[] parameterNames;
	
	private Class<?>[] parameterTypes;

	public MarmotRpcBean(Object target, Method method,Class<?>[] paramterTypes,String[] parameterNames) {
		this.target = target;
		this.method = method;
		this.parameterTypes = paramterTypes;
		this.parameterNames = parameterNames;
	}

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public String[] getParameterNames() {
		return parameterNames;
	}

	public void setParameterNames(String[] parameterNames) {
		this.parameterNames = parameterNames;
	}

	public Class<?>[] getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(Class<?>[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}


	
	

	
}
