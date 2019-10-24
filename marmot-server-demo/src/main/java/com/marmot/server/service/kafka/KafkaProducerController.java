package com.marmot.server.service.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marmot.com.marmot.kafka.topic.IMarmotKafkaProducer;

@RestController
@RequestMapping("/kafka")
public class KafkaProducerController {

	private final IMarmotKafkaProducer kafkaProducer;
	@Autowired
	public KafkaProducerController(IMarmotKafkaProducer kafkaProducer) {
		this.kafkaProducer = kafkaProducer;
	}




	@GetMapping("/sendmsg")
	public void sendMsg(String topic,String message){
		kafkaProducer.sendMsg(topic, message);
	}
}
