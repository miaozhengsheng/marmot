package com.marmot.framework.util;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.marmot.common.rpc.annotation.MarmotInterface;
import com.marmot.common.rpc.annotation.MarmotMethod;
import com.marmot.common.rpc.bean.MarmotRpcBean;
import com.marmot.common.rpc.scanner.RpcScanner;

public class RPCUtil {
	
	public static final ConcurrentHashMap<String, MarmotRpcBean> RPC_MAPPER = new ConcurrentHashMap<String, MarmotRpcBean>();
	
	
	public static void initRpcMapper() throws Exception{


		if (CollectionUtils.isEmpty(RpcScanner.allRpcInterfaces)) {
			return;
		}

		for (Class<?> clazz : RpcScanner.allRpcInterfaces) {
			// 如果不是接口 不进行处理
			if (!clazz.isInterface()) {
				continue;
			}

			// 没有被注解 修饰 不进行处理
			if (!clazz.isAnnotationPresent(MarmotInterface.class)) {
				continue;
			}

			Method[] declaredMethods = clazz.getDeclaredMethods();
			if (declaredMethods == null || declaredMethods.length == 0) {
				continue;
			}

			String basePath = clazz.getSimpleName();
			MarmotInterface annoationRoot = clazz
					.getAnnotation(MarmotInterface.class);

			if (!StringUtils.isEmpty(annoationRoot.url())) {
				basePath = annoationRoot.url();
			}
			
			String beanName = genBeanName(clazz.getSimpleName());
			

			for (Method method : declaredMethods) {

				if (!method.isAnnotationPresent(MarmotMethod.class)) {
					continue;
				}

				MarmotMethod annotationMethod = method
						.getAnnotation(MarmotMethod.class);

				String subPath = method.getName();
				if (!StringUtils.isEmpty(annotationMethod.url())) {
					subPath = annotationMethod.url();
				}
				
				Object target = SpringContextUtil.getBean(beanName);
				
				MarmotRpcBean rpcBean = new MarmotRpcBean(target, method);
				
				RPC_MAPPER.put("/"+basePath+"/"+subPath+"/", rpcBean);

			}

		}
	}
	
	private static String genBeanName(String clazzName){
		byte[] bytes = clazzName.getBytes();
		
		bytes[1] = (byte) (bytes[1]+32);
		
		return new String(bytes, 1, bytes.length-1)+"Impl";
	}

}
