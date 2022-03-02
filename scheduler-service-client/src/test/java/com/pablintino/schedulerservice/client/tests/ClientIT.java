package com.pablintino.schedulerservice.client.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pablintino.schedulerservice.client.ISchedulerServiceClient;
import com.pablintino.schedulerservice.client.RabbitMQExtendedListener;
import com.pablintino.schedulerservice.client.SchedulerServiceClient;
import com.pablintino.schedulerservice.client.models.SchedulerTask;
import com.pablintino.schedulerservice.client.tests.utils.DummyPayload;
import org.junit.jupiter.api.*;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class ClientIT {

  private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
  private static final Random RANDOM = new Random();
  private ISchedulerServiceClient scheduleClient;
  private RabbitMQExtendedListener listener;

  @BeforeEach
  void beforeEach() {
    Map<String, String> properties =
        System.getProperties().entrySet().stream()
            .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));
    properties.putAll(System.getenv());
    String rabbitUrl =
        properties.getOrDefault(
            "COM_PABLINTINO_SCHEDULER_CLIENT_RABBIT_URI", "amqp://guest:guest@localhost:5672");
    String serviceUrl =
        properties.getOrDefault("COM_PABLINTINO_SCHEDULER_CLIENT_URL", "http://localhost:8080");
    listener = new RabbitMQExtendedListener(rabbitUrl);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    scheduleClient =
        new SchedulerServiceClient(listener, objectMapper, serviceUrl, "svcs.schedules", 10000L);
  }

  @AfterEach
  void afterEach() throws Exception {
    if (listener != null) {
      listener.close();
    }
  }

  @Test
  @DisplayName("Test creation")
  void simpleSendOK() throws InterruptedException {
    /* Register the message sink that will listen for schedule messages */
    String randomKey = generateRandomKey();
    scheduleClient.registerMessageSink(
        randomKey,
        scheduleClient
            .getMessageSinkBuilder(DummyPayload.class)
            .callback(
                (id, key, data, metadata) -> {
                  try {
                    messageQueue.put(id);
                  } catch (InterruptedException e) {
                    Assertions.fail("interrupted exception");
                  }
                })
            .build());

    /* Schedule a simple task that triggers once */
    String taskId = UUID.randomUUID().toString();
    scheduleClient.scheduleTask(
        randomKey,
        taskId,
        ZonedDateTime.now().plus(3, ChronoUnit.SECONDS),
        DummyPayload.buildDummy());

    /* Check that task message arrived */
    Assertions.assertEquals(taskId, messageQueue.poll(10, TimeUnit.SECONDS));
  }

  @Test
  @DisplayName("Test cron creation and deletion")
  void simpleCronSendDeleteOK() throws InterruptedException {
    /* Register the message sink that will listen for schedule messages */
    String randomKey = generateRandomKey();
    scheduleClient.registerMessageSink(
        randomKey,
        scheduleClient
            .getMessageSinkBuilder(DummyPayload.class)
            .callback(
                (id, key, data, metadata) -> {
                  try {
                    messageQueue.put(id);
                  } catch (InterruptedException e) {
                    Assertions.fail("interrupted exception");
                  }
                })
            .build());

    /* Schedule a cron task every two seconds */
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

    /* Delete the schedule. It shouldn't trigger any callback since then */
    scheduleClient.deleteTask(randomKey, taskId);
    String message = messageQueue.poll(6, TimeUnit.SECONDS);
    Assertions.assertNull(message);
  }

  @Test
  @DisplayName("Test deletion")
  void simpleSendAndDeleteOK() throws InterruptedException {
    /* Register the message sink that will listen for schedule messages */
    String randomKey = generateRandomKey();
    scheduleClient.registerMessageSink(
        randomKey,
        scheduleClient
            .getMessageSinkBuilder(DummyPayload.class)
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

    /* Delete the schedule before it triggers */
    scheduleClient.deleteTask(randomKey, taskId);

    /* As it was removed it shouldn't deliver any message */
    String message = messageQueue.poll(6, TimeUnit.SECONDS);
    Assertions.assertNull(message);
  }

  @Test
  @DisplayName("Test retrieval")
  void simpleGetOK() throws InterruptedException {
    /* Register the message sink that will listen for schedule messages */
    String randomKey = generateRandomKey();
    scheduleClient.registerMessageSink(
        randomKey,
        scheduleClient
            .getMessageSinkBuilder(DummyPayload.class)
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

    /* Task should exist as it hasn't executed yet */
    SchedulerTask task = scheduleClient.getTask(randomKey, taskId);
    Assertions.assertNotNull(task);

    /* Wait until its execution */
    String message = messageQueue.poll(10, TimeUnit.SECONDS);
    Assertions.assertNotNull(message);

    /* Shouldn't exist as it has been already executed */
    Assertions.assertNull(
        scheduleClient.getTask(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
  }

  private static String generateRandomKey() {
    return "svcs.dummy.key" + RANDOM.nextLong();
  }
}
