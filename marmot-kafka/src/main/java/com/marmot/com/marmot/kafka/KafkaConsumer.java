package com.marmot.com.marmot.kafka;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.serializer.StringDecoder;
import kafka.utils.VerifiableProperties;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.marmot.com.marmot.kafka.topic.IKafkaTopic;
import com.marmot.common.component.IMarmotComponent;
import com.marmot.common.conf.ClientUtil;

@Component
public class KafkaConsumer implements IMarmotComponent {

	private ExecutorService service = null;

	private static  ConsumerConnector connector ;
	
	private static final Logger logger = Logger.getLogger(KafkaConsumer.class);

	@Override
	public void initComponent(ApplicationContext applicationContext) {

		logger.debug("spring 上下文启动成功，开始初始化kafka消费节点");
		
		Map<String, IKafkaTopic> beansByType = applicationContext
				.getBeansOfType(IKafkaTopic.class);

		if (beansByType == null || beansByType.isEmpty()) {
			return;
		}
		connector = initConsumer();
		Iterator<Entry<String, IKafkaTopic>> iterator = beansByType.entrySet()
				.iterator();

		service = new ThreadPoolExecutor(beansByType.size(), 10, 3000,
				TimeUnit.MICROSECONDS, new ArrayBlockingQueue<Runnable>(32));

		while (iterator.hasNext()) {

			Entry<String, IKafkaTopic> next = iterator.next();

			String topic = next.getKey();

			topic = topic.substring(topic.indexOf("KAFKA.TOPIC.") + 12);

			service.submit(new KafKaExecutor(next.getValue(), topic));
		}
	}

	private ConsumerConnector initConsumer() {
		// 初始化 链接
		Properties props = new Properties();
		// zookeeper 配置
		props.put("zookeeper.connect",
				"192.168.117.130:2181,192.168.117.131:2181,192.168.117.132:2181");

		props.put("group.id", ClientUtil.getProjectName());

		// zk连接超时
		props.put("zookeeper.session.timeout.ms", "4000");
		props.put("zookeeper.sync.time.ms", "200");
		props.put("auto.commit.interval.ms", "1000");
		props.put("auto.offset.reset", "smallest");
		// 序列化类
		props.put("serializer.class", "kafka.serializer.StringEncoder");

		ConsumerConfig config = new ConsumerConfig(props);

		ConsumerConnector consumer = kafka.consumer.Consumer
				.createJavaConsumerConnector(config);

		return consumer;
	}

	private static class KafKaExecutor implements Callable<Void> {

		private IKafkaTopic kafkaConsume;

		private String topic;

		public KafKaExecutor(IKafkaTopic kafkaConsume, String topic) {
			this.kafkaConsume = kafkaConsume;
			this.topic = topic;

		}

		@Override
		public Void call() throws Exception {

			Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
			topicCountMap.put(topic, new Integer(1));

			StringDecoder keyDecoder = new StringDecoder(
					new VerifiableProperties());
			StringDecoder valueDecoder = new StringDecoder(
					new VerifiableProperties());

			Map<String, List<KafkaStream<String, String>>> consumerMap = connector
					.createMessageStreams(topicCountMap, keyDecoder,
							valueDecoder);
			KafkaStream<String, String> stream = consumerMap.get(topic).get(0);
			ConsumerIterator<String, String> it = stream.iterator();
			while (it.hasNext()) {
				// 消费消息
				kafkaConsume.consumeMessage(it.next().message());
			}

			return null;
		}

	}


	@Override
	public void distory(ApplicationContext applicationContext) {
		
		logger.debug("服务停止，开始清理kafka对应的资源");
		if(connector!=null){
			connector.commitOffsets();
		}
		if(service!=null){
			service.shutdown();
		}
	}

	@Override
	public int order() {
		return 2;
	}

}
