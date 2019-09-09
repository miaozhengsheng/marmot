package com.marmot.common.rpc.bean;

import java.io.Serializable;
import java.util.Arrays;


public class MarmotRpcBean implements Serializable{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	private String methodName;
	
	private String clazzName;
	
	private Object[] paramterValues; 
	
	private Class<?>[] parameterTypes;

	public MarmotRpcBean(String methodName,String clazzName,Object[] paramterValues,Class<?>[] parameterTypes) {
		this.methodName = methodName;
		this.paramterValues = paramterValues;
		this.clazzName = clazzName;
		this.parameterTypes = parameterTypes;
	}


	
	public Class<?>[] getParameterTypes() {
		return parameterTypes;
	}



	public void setParameterTypes(Class<?>[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}



	public String getMethodName() {
		return methodName;
	}




	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}



	public String getClazzName() {
		return clazzName;
	}



	public void setClazzName(String clazzName) {
		this.clazzName = clazzName;
	}



	public Object[] getParamterValues() {
		return paramterValues;
	}

	public void setParamterValues(Object[] paramterValues) {
		this.paramterValues = paramterValues;
	}

	@Override
	public String toString() {
		return "MarmotRpcBean [methodName=" + methodName + ", paramterValues="
				+ Arrays.toString(paramterValues) + "]";
	}
	

	
}
