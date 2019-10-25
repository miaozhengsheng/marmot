package com.marmot.com.marmot.kafka;

import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import com.marmot.com.marmot.kafka.topic.IMarmotKafkaProducer;

@Service
public class MarmotKafkaProducer implements IMarmotKafkaProducer,InitializingBean,DisposableBean{
	
	private   Producer<String, String> producer = null;

	@Override
	public boolean sendMsg(String topic, String message) {
		Future<RecordMetadata> send = producer.send(new ProducerRecord<String, String>(topic, message));
		producer.flush();
		return false;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
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
	public void destroy() throws Exception {
		producer.close();
		
	}
}
