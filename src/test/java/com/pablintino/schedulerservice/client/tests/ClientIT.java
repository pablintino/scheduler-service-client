package com.pablintino.schedulerservice.client.tests;

import com.pablintino.schedulerservice.client.ISchedulerServiceClient;
import com.pablintino.schedulerservice.client.config.SchedulerClientConfiguration;
import com.pablintino.schedulerservice.client.models.SchedulerTask;
import com.pablintino.schedulerservice.client.tests.utils.DummyPayload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@SpringBootTest(properties = {"com.pablintino.scheduler.client.exchange-name=svcs.schedules"})
class ClientIT {

  @Configuration
  @Import(SchedulerClientConfiguration.class)
  static class TestConfiguration {}

  private static final Random RANDOM = new Random();

  @Autowired private ISchedulerServiceClient scheduleClient;

  private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

  @Test
  @DirtiesContext
  @DisplayName("Test creation")
  void simpleSendOK() throws InterruptedException {
    String randomKey = generateRandomKey();
    scheduleClient.registerMessageSink(
        randomKey,
        scheduleClient
            .getMessageSinkBuilder()
            .ofType(DummyPayload.class)
            .callback(
                (id, key, data, metadata) -> {
                  try {
                    messageQueue.put(id);
                  } catch (InterruptedException e) {
                    Assertions.fail("interrupted exception");
                  }
                })
            .build());

    String taskId = UUID.randomUUID().toString();
    scheduleClient.scheduleTask(
        randomKey,
        taskId,
        ZonedDateTime.now().plus(3, ChronoUnit.SECONDS),
        DummyPayload.buildDummy());
    Assertions.assertEquals(taskId, messageQueue.poll(10, TimeUnit.SECONDS));
  }

  @Test
  @DirtiesContext
  @DisplayName("Test cron creation")
  void simpleCronSendOK() throws InterruptedException {
    String randomKey = generateRandomKey();
    scheduleClient.registerMessageSink(
        randomKey,
        scheduleClient
            .getMessageSinkBuilder()
            .ofType(DummyPayload.class)
            .callback(
                (id, key, data, metadata) -> {
                  try {
                    messageQueue.put(id);
                  } catch (InterruptedException e) {
                    Assertions.fail("interrupted exception");
                  }
                })
            .build());

    String taskId = UUID.randomUUID().toString();
    scheduleClient.scheduleTask(
        randomKey,
        taskId,
        ZonedDateTime.now().plus(1, ChronoUnit.SECONDS),
        "*/2 * * * * ?",
        DummyPayload.buildDummy());
    for (int index = 0; index < 2; index++) {
      Assertions.assertEquals(taskId, messageQueue.poll(3, TimeUnit.SECONDS));
    }
  }

  @Test
  @DirtiesContext
  @DisplayName("Test deletion")
  void simpleSendAndDeleteOK() throws InterruptedException {
    String randomKey = generateRandomKey();
    scheduleClient.registerMessageSink(
        randomKey,
        scheduleClient
            .getMessageSinkBuilder()
            .ofType(DummyPayload.class)
            .callback(
                (id, key, data, metadata) -> {
                  try {
                    messageQueue.put(id);
                  } catch (InterruptedException e) {
                    Assertions.fail("interrupted exception");
                  }
                })
            .build());

    String taskId = UUID.randomUUID().toString();
    scheduleClient.scheduleTask(
        randomKey,
        taskId,
        ZonedDateTime.now().plus(3, ChronoUnit.SECONDS),
        DummyPayload.buildDummy());
    scheduleClient.deleteTask(randomKey, taskId);
    String message = messageQueue.poll(6, TimeUnit.SECONDS);
    Assertions.assertNull(message);
  }

  @Test
  @DirtiesContext
  @DisplayName("Test retrieval")
  void simpleGetOK() throws InterruptedException {
    String randomKey = generateRandomKey();
    scheduleClient.registerMessageSink(
        randomKey,
        scheduleClient
            .getMessageSinkBuilder()
            .ofType(DummyPayload.class)
            .callback(
                (id, key, data, metadata) -> {
                  try {
                    messageQueue.put(id);
                  } catch (InterruptedException e) {
                    Assertions.fail("interrupted exception");
                  }
                })
            .build());

    String taskId = UUID.randomUUID().toString();
    scheduleClient.scheduleTask(
        randomKey,
        taskId,
        ZonedDateTime.now().plus(5, ChronoUnit.SECONDS),
        DummyPayload.buildDummy());

    SchedulerTask task = scheduleClient.getTask(randomKey, taskId);
    Assertions.assertNotNull(task);
    String message = messageQueue.poll(10, TimeUnit.SECONDS);
    Assertions.assertNotNull(message);
    Assertions.assertNull(
        scheduleClient.getTask(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
  }

  private static String generateRandomKey() {
    return "svcs.dummy.key" + RANDOM.nextLong();
  }
}
