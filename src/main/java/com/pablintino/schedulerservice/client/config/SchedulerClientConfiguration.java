package com.pablintino.schedulerservice.client.config;

import com.pablintino.schedulerservice.client.SchedulerServiceClient;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
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
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory){
        return new RabbitAdmin(connectionFactory);
    }
}
