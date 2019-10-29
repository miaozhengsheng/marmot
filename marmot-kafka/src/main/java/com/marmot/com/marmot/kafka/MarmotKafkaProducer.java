package com.marmot.com.marmot.kafka;

import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.marmot.com.marmot.kafka.topic.IMarmotKafkaProducer;
import com.marmot.common.component.IMarmotComponent;

@Service
public class MarmotKafkaProducer implements IMarmotKafkaProducer,IMarmotComponent{
	
	private   Producer<String, String> producer = null;
	
	  private static final Logger logger = Logger.getLogger(MarmotKafkaProducer.class);

	@Override
	public boolean sendMsg(String topic, String message) {
		Future<RecordMetadata> send = producer.send(new ProducerRecord<String, String>(topic, message));
		producer.flush();
		return false;
	}
	


	@Override
	public void initComponent(ApplicationContext applicationContext) {
		
		logger.debug("spring 上下文启动成功，开始初始化kafka生产的节点");
		
		Properties properties = new Properties();
        properties.put("bootstrap.servers", "192.168.117.130:9092,192.168.117.131:9092,192.168.117.132:9092");
        properties.put("acks", "all");
        properties.put("retries", 0);
        properties.put("batch.size", 16384);
        properties.put("linger.ms", 1);
        properties.put("buffer.memory", 33554432);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
      
        producer = new KafkaProducer<String, String>(properties);		
	}

	@Override
	public void distory(ApplicationContext applicationContext) {
		logger.debug("spring 上下文停止，开始停止 kafka生产客户端..");
		
		producer.close();
	}

	@Override
	public int order() {
		return 3;
	}
}
