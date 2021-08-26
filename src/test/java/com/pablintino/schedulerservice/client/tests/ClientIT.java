package com.pablintino.schedulerservice.client.tests;

import com.pablintino.schedulerservice.client.ISchedulerServiceClient;
import com.pablintino.schedulerservice.client.config.SchedulerClientConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@TestPropertySource(locations="classpath:test-properties.properties")
class ClientIT {

	@EnableRabbit
	@Configuration
	@Import(SchedulerClientConfiguration.class)
	static class TestConfiguration{

		@Bean
		public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
			SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
			factory.setConnectionFactory(connectionFactory);
			factory.setMaxConcurrentConsumers(20);
			return factory;
		}

		@Bean
		ConnectionFactory connectionFactory(@Value("${spring.rabbitmq.host:localhost}") String host, @Value("${spring.rabbitmq.port:5672}") int port) {
			return new CachingConnectionFactory(host, port);
		}
	}

	@Autowired
	private ISchedulerServiceClient scheduleClient;

	private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

	@Test
	@DisplayName("Test")
	void simpleSendOK() throws InterruptedException {
		scheduleClient.registerCallback("svcs.dummy.key1", (id, key, dataMap) -> {
			try {
				messageQueue.put(id);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		Map<String, Object> testMap = new HashMap<>();
		testMap.put("test-key", "dummy-value");
		scheduleClient.scheduleTask("svcs.dummy.key1", "test-task", ZonedDateTime.now().plus(3, ChronoUnit.SECONDS), testMap);
		String message = messageQueue.poll(10, TimeUnit.SECONDS);
		Assertions.assertNotNull(message);
	}


}
