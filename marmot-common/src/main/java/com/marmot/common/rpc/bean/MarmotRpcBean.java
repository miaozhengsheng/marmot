package com.marmot.common.rpc.bean;

import java.lang.reflect.Method;


public class MarmotRpcBean {
	
	
	private Object target;
	
	private Method method;
	
	private Object[] parameters;
	
	private Object[] parameterTypes;

	public MarmotRpcBean(Object target, Method method) {
		super();
		this.target = target;
		this.method = method;
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

	public Object[] getParameters() {
		return parameters;
	}

	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}

	public Object[] getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(Object[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}
	

	
}
