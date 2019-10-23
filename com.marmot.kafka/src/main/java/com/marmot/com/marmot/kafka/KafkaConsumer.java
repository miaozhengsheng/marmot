package com.marmot.com.marmot.kafka;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.marmot.com.marmot.kafka.topic.IKafkaTopic;

@Component
public class KafkaConsumer implements ApplicationContextAware{


	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		
		System.out.println("初始化完成。开始处理kafka请求。。。。");
		Map<String, IKafkaTopic> beansByType = applicationContext.getBeansOfType(IKafkaTopic.class);
		
		if(beansByType==null||beansByType.isEmpty()){
			return;
		}
		
		Iterator<Entry<String, IKafkaTopic>> iterator = beansByType.entrySet().iterator();
		
		while(iterator.hasNext()){
			
			Entry<String, IKafkaTopic> next = iterator.next();
			
			System.out.println(next.getKey());
			
		}		
	}
}
