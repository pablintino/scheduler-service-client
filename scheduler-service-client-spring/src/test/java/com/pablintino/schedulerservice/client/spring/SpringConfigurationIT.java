package com.pablintino.schedulerservice.client.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pablintino.schedulerservice.client.ISchedulerServiceClient;
import com.pablintino.schedulerservice.client.spring.config.SchedulerClientConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {"com.pablintino.scheduler.client.exchange-name=svcs.schedules"})
public class SpringConfigurationIT {

  @Configuration
  @Import(SchedulerClientConfiguration.class)
  static class TestConfiguration {

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }
  }

  @Autowired private ISchedulerServiceClient schedulerServiceClient;

  @Test
  void createSpringContextTest() {
    Assertions.assertNotNull(schedulerServiceClient);
  }
}
