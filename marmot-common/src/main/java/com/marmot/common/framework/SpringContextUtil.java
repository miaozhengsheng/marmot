package com.marmot.common.framework;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Service
public class SpringContextUtil implements ApplicationContextAware{

	
	private static  ApplicationContext applicationContext = null;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		if(SpringContextUtil.applicationContext==null){
			SpringContextUtil.applicationContext = applicationContext;
		}else {
            if (applicationContext instanceof WebApplicationContext) {
                SpringContextUtil.applicationContext = applicationContext;
            }
        }
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getBean(String beanName){
		return (T)applicationContext.getBean(beanName);
	}
	
	
	public static <T> T getBean(Class<T> clazz){
		return (T)applicationContext.getBean(clazz);
	}
}