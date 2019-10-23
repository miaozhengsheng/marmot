package com.marmot.com.marmot.kafka;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.marmot.com.marmot.kafka.topic.IKafkaTopic;
import com.marmot.common.component.IMarmotComponent;

@Component
public class KafkaConsumer implements IMarmotComponent, DisposableBean{

	/**
	 * 
	 */

	private final  ExecutorService service=  new ThreadPoolExecutor(4, 10, 3000, TimeUnit.MICROSECONDS, new ArrayBlockingQueue<Runnable>(32));


	@Override
	public void initComponent(ApplicationContext applicationContext) {
	Map<String, IKafkaTopic> beansByType = applicationContext.getBeansOfType(IKafkaTopic.class);
		
		if(beansByType==null||beansByType.isEmpty()){
			return;
		}
		
		Iterator<Entry<String, IKafkaTopic>> iterator = beansByType.entrySet().iterator();
		
		while(iterator.hasNext()){
			
			Entry<String, IKafkaTopic> next = iterator.next();
			
			String topic  =next.getKey();
			
			topic  = topic.substring(topic.indexOf("KAFKA.TOPIC.")+12);
			
			service.submit(new KafKaExecutor(next.getValue(),topic));
		}	
	}
	
	


	private static class KafKaExecutor implements Callable<Void>{
		private IKafkaTopic kafkaConsume;

		private String topic;
		public KafKaExecutor(IKafkaTopic kafkaConsume,String topic) {
			this.kafkaConsume = kafkaConsume;
			this.topic = topic;
			
		}
		@Override
		public Void call() throws Exception {
			System.out.println("需要订阅的TOPIC:"+topic);
			return null;
		}
		
	}


	@Override
	public void destroy() throws Exception {
		service.shutdown();		
	}



	
}
