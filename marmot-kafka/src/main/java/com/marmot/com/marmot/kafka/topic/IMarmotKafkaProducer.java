package com.marmot.com.marmot.kafka.topic;

public interface IMarmotKafkaProducer {

	/**
	 * 发送消息
	 * @param topic
	 * @param message
	 * @return
	 */
	public boolean sendMsg(String topic,String message);
}
