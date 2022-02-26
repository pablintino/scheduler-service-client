package com.pablintino.schedulerservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pablintino.schedulerservice.client.models.SchedulerTask;
import com.pablintino.schedulerservice.dtos.CallbackDescriptorDto;
import com.pablintino.schedulerservice.dtos.CallbackMethodTypeDto;
import com.pablintino.schedulerservice.dtos.ScheduleRequestDto;
import com.pablintino.schedulerservice.dtos.ScheduleTaskDto;
import com.pablintino.services.commons.responses.HttpErrorBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SchedulerServiceClient implements ISchedulerServiceClient {

  private static final String SCHEDULES_API_PATH = "/api/v1/schedules";

  private final Map<String, ISchedulerMessageSink<?>> sinksMap = new HashMap<>();
  private final IExtendedRabbitMQListener rabbitMQManager;
  private final WebClient schedulerClient;
  private final long clientTimeout;
  private final ObjectMapper objectMapper;
  private final String exchange;

  public SchedulerServiceClient(
      WebClient.Builder webClientBuilder,
      IExtendedRabbitMQListener rabbitMQManager,
      ObjectMapper objectMapper,
      String baseUrl,
      String exchange,
      long clientTimeout) {
    this.rabbitMQManager = rabbitMQManager;
    this.clientTimeout = clientTimeout;
    this.objectMapper = objectMapper;
    this.exchange = exchange;
    this.schedulerClient =
        webClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
  }

  @Override
  public void scheduleTask(String key, String id, ZonedDateTime triggerTime, Object data) {
    Assert.hasLength(key, "key cannot be null");
    Assert.notNull(id, "id cannot be null or empty");
    Assert.notNull(triggerTime, "triggerTime cannot be null");

    /* Prepare the request body */
    ScheduleRequestDto request = createCommonScheduleRequest(key, id, triggerTime, data);
    doScheduleRemote(request);
  }

  @Override
  public void scheduleTask(
      String key, String id, ZonedDateTime triggerTime, String cronExpression, Object data) {
    Assert.hasLength(key, "key cannot be null");
    Assert.notNull(id, "id cannot be null or empty");
    Assert.notNull(triggerTime, "triggerTime cannot be null");
    Assert.hasLength(cronExpression, "cronExpression cannot be null");

    /* Prepare the request body */
    ScheduleRequestDto request = createCommonScheduleRequest(key, id, triggerTime, data);
    request.setCronExpression(cronExpression);
    doScheduleRemote(request);
  }

  @Override
  public void registerMessageSink(String key, ISchedulerMessageSink<?> messageSink) {
    Assert.notNull(messageSink, "messageSink cannot be null");
    Assert.hasLength(key, "key cannot be null or empty");

    if (sinksMap.containsKey(key)) {
      throw new ScheduleServiceClientException("Key  " + key + " was already registered");
    }

    /* Idempotent calls */
    rabbitMQManager.declareQueue(key);
    rabbitMQManager.bindQueue(key, exchange, key);
    rabbitMQManager.installConsumer(key, messageSink);

    sinksMap.put(key, messageSink);
  }

  @Override
  public void deleteTask(String key, String id) {
    schedulerClient
        .delete()
        .uri(SCHEDULES_API_PATH + "/{key}/{id}", key, id)
        .retrieve()
        .onStatus(
            HttpStatus::isError,
            response ->
                response
                    .bodyToMono(HttpErrorBody.class)
                    .flatMap(
                        err ->
                            Mono.error(new ScheduleServiceClientException(err.getErrorMessage()))))
        .bodyToMono(Void.class)
        .timeout(Duration.of(clientTimeout, ChronoUnit.MILLIS))
        .block();
  }

  @Override
  public SchedulerTask getTask(String key, String id) {
    try {
      ScheduleTaskDto scheduleTaskDto =
          schedulerClient
              .get()
              .uri(SCHEDULES_API_PATH + "/{key}/{id}", key, id)
              .retrieve()
              .onStatus(
                  HttpStatus::isError,
                  response ->
                      response
                          .bodyToMono(HttpErrorBody.class)
                          .flatMap(
                              err ->
                                  Mono.error(
                                      new ScheduleServiceClientException(
                                          err.getErrorMessage(), err.getStatus()))))
              .bodyToMono(ScheduleTaskDto.class)
              .timeout(Duration.of(clientTimeout, ChronoUnit.MILLIS))
              .block();
      return new SchedulerTask(
          scheduleTaskDto.getTaskIdentifier(),
          scheduleTaskDto.getTaskKey(),
          scheduleTaskDto.getTriggerTime(),
          scheduleTaskDto.getCronExpression(),
          scheduleTaskDto.getTaskData());
    } catch (ScheduleServiceClientException ex) {
      if (ex.getHttpCode() != null && ex.getHttpCode().equals(HttpStatus.NOT_FOUND.value())) {
        return null;
      }
      throw ex;
    }
  }

  @Override
  public SchedulerMessageSink.Builder getMessageSinkBuilder() {
    return new SchedulerMessageSink.Builder(objectMapper);
  }

  private ScheduleRequestDto createCommonScheduleRequest(
      String key, String id, ZonedDateTime triggerTime, Object data) {

    if (!sinksMap.containsKey(key)) {
      throw new ScheduleServiceClientException("Key  " + key + " has no sink registered");
    }

    if (data != null && !sinksMap.get(key).getDataType().equals(data.getClass())) {
      throw new ScheduleServiceClientException(
          "Data type "
              + data.getClass().getName()
              + " doesn't match the registered sink data type");
    }

    /* Prepare the request body */
    ScheduleRequestDto request = new ScheduleRequestDto();
    request.setTaskData(data);
    request.setTaskKey(key);
    request.setTaskIdentifier(id);
    request.setTriggerTime(triggerTime);
    CallbackDescriptorDto callbackDescriptor = new CallbackDescriptorDto();
    callbackDescriptor.setType(CallbackMethodTypeDto.AMQP);
    request.setCallbackDescriptor(callbackDescriptor);
    return request;
  }

  private void doScheduleRemote(ScheduleRequestDto request) {
    schedulerClient
        .post()
        .uri(SCHEDULES_API_PATH)
        .body(Mono.just(request), ScheduleRequestDto.class)
        .retrieve()
        .onStatus(
            HttpStatus::isError,
            response ->
                response
                    .bodyToMono(HttpErrorBody.class)
                    .flatMap(
                        err ->
                            Mono.error(new ScheduleServiceClientException(err.getErrorMessage()))))
        .bodyToMono(Void.class)
        .timeout(Duration.of(clientTimeout, ChronoUnit.MILLIS))
        .block();
  }
}
