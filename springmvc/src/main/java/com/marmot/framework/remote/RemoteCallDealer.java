package com.marmot.framework.remote;

import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;

import com.marmot.common.rpc.bean.MarmotRpcBean;
import com.marmot.common.util.JsonUtil;
import com.marmot.framework.util.prameter.RequestUtil;

public class RemoteCallDealer {
	
	public static Object deal(MarmotRpcBean rpcBean,HttpServletRequest request) throws Exception{
		
		Object target = rpcBean.getTarget();
		
		Method method = rpcBean.getMethod();
		
		
		String jsonStr = request.getParameter(RequestUtil.INPUT);
		
		if(StringUtils.isEmpty(jsonStr)){
			throw new Exception("²ÎÊý´íÎó");
		}
		
		Map<String, Object> json2map = JsonUtil.json2map(jsonStr);
		
		String dataStr = (String)json2map.get(RequestUtil.DATA);
		
		Map<String, String> paramterMap  =  JsonUtil.getRootJson(dataStr);
		
		
		Object[] paramters = null;
		
		if(rpcBean.getParameterTypes()!=null&&rpcBean.getParameterTypes().length>0){
			
			paramters = new Object[rpcBean.getParameterTypes().length];
			
			String[] parameterNames = rpcBean.getParameterNames();
			
			for(int i=0;i<parameterNames.length;i++){
				
				String paramterName = parameterNames[i];
				
				String  valueJson = paramterMap.get(paramterName);
				
				if(StringUtils.isEmpty(valueJson)){
					parameterNames[i] = null;
				}else{
					paramters[i] = JsonUtil.json2objectDepth(valueJson, rpcBean.getParameterTypes()[i],new Class<?>[]{});
				}
				
			}
		}
		
		return method.invoke(target, paramters);
	}

	
}
