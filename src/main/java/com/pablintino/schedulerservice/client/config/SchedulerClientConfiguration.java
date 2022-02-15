package com.pablintino.schedulerservice.client.config;

import com.pablintino.schedulerservice.client.SchedulerServiceClient;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ComponentScan(basePackageClasses = SchedulerServiceClient.class)
public class SchedulerClientConfiguration {

  @Bean
  public WebClient.Builder getWebClientBuilder() {
    return WebClient.builder();
  }

  @Bean
  public ConnectionFactory connectionFactory(
      @Value("${spring.rabbitmq.host:localhost}") String host,
      @Value("${spring.rabbitmq.port:5672}") int port) {
    return new CachingConnectionFactory(host, port);
  }

  @Bean
  public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
    return new RabbitAdmin(connectionFactory);
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    final var rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jackson2Converter());
    return rabbitTemplate;
  }

  @Bean
  public RabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory =
        new SimpleRabbitListenerContainerFactory();
    simpleRabbitListenerContainerFactory.setConnectionFactory(connectionFactory);
    simpleRabbitListenerContainerFactory.setMessageConverter(jackson2Converter());
    simpleRabbitListenerContainerFactory.setMaxConcurrentConsumers(20);
    return simpleRabbitListenerContainerFactory;
  }

  @Bean
  public Jackson2JsonMessageConverter jackson2Converter() {
    return new Jackson2JsonMessageConverter();
  }
}
