package com.pablintino.schedulerservice.client.config;

import com.pablintino.schedulerservice.client.SchedulerServiceClient;
import com.pablintino.services.commons.amqp.AmqpCommonConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Import(AmqpCommonConfiguration.class)
@ComponentScan(basePackageClasses = SchedulerServiceClient.class)
public class SchedulerClientConfiguration {

  @Bean
  public WebClient.Builder getWebClientBuilder() {
    return WebClient.builder();
  }
}
