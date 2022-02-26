package com.pablintino.schedulerservice.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pablintino.schedulerservice.client.IExtendedRabbitMQListener;
import com.pablintino.schedulerservice.client.ISchedulerServiceClient;
import com.pablintino.schedulerservice.client.RabbitMQExtendedListener;
import com.pablintino.schedulerservice.client.SchedulerServiceClient;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerClientConfiguration {

  @Bean
  ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  @Bean
  IExtendedRabbitMQListener rabbitMQManager(
      @Autowired(required = false) ConnectionFactory connectionFactory,
      @Value("${com.pablintino.scheduler.client.rabbit-uri:#{null}}") String rabbitMqUri) {
    if (StringUtils.isBlank(rabbitMqUri) && connectionFactory == null) {
      throw new IllegalStateException("One of uri or connection factory should be provided");
    }

    return connectionFactory == null
        ? new RabbitMQExtendedListener(rabbitMqUri)
        : new RabbitMQExtendedListener(connectionFactory);
  }

  @Bean
  ISchedulerServiceClient schedulerServiceClient(
      IExtendedRabbitMQListener rabbitMQListener,
      ObjectMapper objectMapper,
      @Value("${com.pablintino.scheduler.client.url}") String baseUrl,
      @Value("${com.pablintino.scheduler.client.exchange-name}") String exchange,
      @Value("${com.pablintino.scheduler.client.timeout:10000}") long clientTimeout) {

    return new SchedulerServiceClient(
        rabbitMQListener, objectMapper, baseUrl, exchange, clientTimeout);
  }
}
