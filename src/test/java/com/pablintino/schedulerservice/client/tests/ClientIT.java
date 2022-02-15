package com.pablintino.schedulerservice.client.tests;

import com.pablintino.schedulerservice.client.ISchedulerServiceClient;
import com.pablintino.schedulerservice.client.config.SchedulerClientConfiguration;
import com.pablintino.schedulerservice.client.models.SchedulerTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@SpringBootTest(properties = {"com.pablintino.scheduler.client.exchange-name=svcs.schedules"})
class ClientIT {

    @EnableRabbit
    @Configuration
    @Import(SchedulerClientConfiguration.class)
    static class TestConfiguration {
    }

    @Autowired
    private ISchedulerServiceClient scheduleClient;

    private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    @Test
    @DirtiesContext
    @DisplayName("Test creation")
    void simpleSendOK() throws InterruptedException {
        scheduleClient.registerCallback("svcs.dummy.key1", (id, key, dataMap, metadata) -> {
            try {
                messageQueue.put(id);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("test-key", "dummy-value");
        String taskId = UUID.randomUUID().toString();
        scheduleClient.scheduleTask("svcs.dummy.key1", taskId, ZonedDateTime.now().plus(3, ChronoUnit.SECONDS), testMap);
        Assertions.assertEquals(taskId, messageQueue.poll(10, TimeUnit.SECONDS));
    }

    @Test
    @DirtiesContext
    @DisplayName("Test cron creation")
    void simpleCronSendOK() throws InterruptedException {
        scheduleClient.registerCallback("svcs.dummy.key1", (id, key, dataMap, metadata) -> {
            try {
                messageQueue.put(id);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("test-key", "dummy-value");
        String taskId = UUID.randomUUID().toString();
        scheduleClient.scheduleTask("svcs.dummy.key1", taskId, ZonedDateTime.now().plus(1, ChronoUnit.SECONDS), "*/2 * * * * ?", testMap);
        for (int index = 0; index < 2; index++) {
            Assertions.assertEquals(taskId, messageQueue.poll(3, TimeUnit.SECONDS));
        }
    }

    @Test
    @DirtiesContext
    @DisplayName("Test deletion")
    void simpleSendAndDeleteOK() throws InterruptedException {
        scheduleClient.registerCallback("svcs.dummy.key1", (id, key, dataMap, metadata) -> {
            try {
                messageQueue.put(id);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("test-key", "dummy-value");
        String taskId = UUID.randomUUID().toString();
        scheduleClient.scheduleTask("svcs.dummy.key1", taskId, ZonedDateTime.now().plus(3, ChronoUnit.SECONDS), testMap);
        scheduleClient.deleteTask("svcs.dummy.key1", taskId);
        String message = messageQueue.poll(6, TimeUnit.SECONDS);
        Assertions.assertNull(message);
    }

    @Test
    @DirtiesContext
    @DisplayName("Test retrieval")
    void simpleGetOK() throws InterruptedException {
        scheduleClient.registerCallback("svcs.dummy.key1", (id, key, dataMap, metadata) -> {
            try {
                messageQueue.put(id);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("test-key", "dummy-value");
        String taskId = UUID.randomUUID().toString();
        scheduleClient.scheduleTask("svcs.dummy.key1", taskId, ZonedDateTime.now().plus(5, ChronoUnit.SECONDS), testMap);

        SchedulerTask task = scheduleClient.getTask("svcs.dummy.key1", taskId);
        Assertions.assertNotNull(task);
        String message = messageQueue.poll(10, TimeUnit.SECONDS);
        Assertions.assertNotNull(message);
        Assertions.assertNull(scheduleClient.getTask(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
    }
}
