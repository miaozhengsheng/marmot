package com.marmot.framework.util.prameter;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class RequestUtil {
	
	
	public static final String INPUT  = "input";
	
	public static final String DATA="data";
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getInputParameter(HttpServletRequest request){
		return (Map<String, Object>) request.getAttribute(INPUT);
	}
	
	
	public static String getInputJsonData(Map<String, Object> allParamterMap){
		return (String) allParamterMap.get(DATA);
	}

}
