package com.marmot.server.service.kafka;

import org.springframework.stereotype.Service;

import com.marmot.com.marmot.kafka.topic.IKafkaTopic;

@Service("KAFKA.TOPIC.mutipl-partitions-topic")
public class KafKaDemo implements IKafkaTopic{

	@Override
	public void consumeMessage(String message) {
		System.out.println("当前消费的消息为:"+message);
	}

}
