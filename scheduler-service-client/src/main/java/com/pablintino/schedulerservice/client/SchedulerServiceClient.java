package com.pablintino.schedulerservice.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pablintino.schedulerservice.client.models.SchedulerTask;
import com.pablintino.schedulerservice.dtos.CallbackDescriptorDto;
import com.pablintino.schedulerservice.dtos.CallbackMethodTypeDto;
import com.pablintino.schedulerservice.dtos.ScheduleRequestDto;
import com.pablintino.schedulerservice.dtos.ScheduleTaskDto;
import com.pablintino.services.commons.exceptions.responses.HttpErrorBody;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class SchedulerServiceClient implements ISchedulerServiceClient {

  private static final String MEDIA_TYPE_APPLICATION_JSON = "application/json";
  private final URL schedulesApiPath;
  private final Map<String, ISchedulerMessageSink<?>> sinksMap = new HashMap<>();
  private final IExtendedRabbitMQListener rabbitMQManager;
  private final HttpClient httpClient;
  private final long clientTimeout;
  private final ObjectMapper objectMapper;
  private final String exchange;

  public SchedulerServiceClient(
      @NonNull IExtendedRabbitMQListener rabbitMQManager,
      @NonNull ObjectMapper objectMapper,
      String baseUrl,
      String exchange,
      long clientTimeout) {

    if (StringUtils.isBlank(baseUrl)) {
      throw new IllegalArgumentException("baseUrl cannot be null or empty");
    }
    if (StringUtils.isBlank(exchange)) {
      throw new IllegalArgumentException("exchange cannot be null or empty");
    }

    this.rabbitMQManager = rabbitMQManager;
    this.objectMapper = objectMapper;
    this.clientTimeout = clientTimeout;
    this.exchange = exchange;

    try {
      this.schedulesApiPath = new URL(new URL(baseUrl), "/api/v1/schedules/");
    } catch (MalformedURLException ex) {
      throw new IllegalArgumentException("The given URL is not valid", ex);
    }

    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.of(clientTimeout, ChronoUnit.MILLIS))
            .build();
  }

  @Override
  public void scheduleTask(String key, String id, ZonedDateTime triggerTime, Object data) {
    Validate.notBlank(key, "key cannot be null");
    Validate.notNull(id, "id cannot be null or empty");
    Validate.notNull(triggerTime, "triggerTime cannot be null");

    log.debug(
        "Scheduling task for key {} with ID {}. Trigger time: {} Data type: {}",
        key,
        id,
        triggerTime,
        data != null ? data.getClass() : "null");

    /* Prepare the request body */
    ScheduleRequestDto request = createCommonScheduleRequest(key, id, triggerTime, data);
    doScheduleRemote(request);
  }

  @Override
  public void scheduleTask(
      String key, String id, ZonedDateTime triggerTime, String cronExpression, Object data) {
    Validate.notBlank(key, "key cannot be null");
    Validate.notNull(id, "id cannot be null or empty");
    Validate.notNull(triggerTime, "triggerTime cannot be null");
    Validate.notBlank(cronExpression, "cronExpression cannot be null");

    log.debug(
        "Scheduling cron task for key {} with ID {}. Trigger time: {} Cron: {} Data type: {}",
        key,
        id,
        triggerTime,
        cronExpression,
        data != null ? data.getClass() : "null");

    /* Prepare the request body */
    ScheduleRequestDto request = createCommonScheduleRequest(key, id, triggerTime, data);
    request.setCronExpression(cronExpression);
    doScheduleRemote(request);
  }

  @Override
  public void registerMessageSink(String key, ISchedulerMessageSink<?> messageSink) {
    Validate.notNull(messageSink, "messageSink cannot be null");
    Validate.notBlank(key, "key cannot be null or empty");

    log.debug("Registering message sink for key {}. Sink: {}", key, messageSink);
    if (sinksMap.containsKey(key)) {
      throw new SchedulerServiceClientException("Key  " + key + " was already registered");
    }

    /* Idempotent calls */
    rabbitMQManager.declareQueue(key);
    rabbitMQManager.bindQueue(key, exchange, key);
    rabbitMQManager.installConsumer(key, messageSink);

    sinksMap.put(key, messageSink);
  }

  @Override
  public void deleteTask(String key, String id) {
    log.debug("Deleting task for key {} and ID {}", key, id);
    performRequest(
        createCommonRequestBuilder(createRelativeURI(key + "/" + id)).DELETE().build(), Void.class);
  }

  @Override
  public SchedulerTask getTask(String key, String id) {
    return performRequest(
            createCommonRequestBuilder(createRelativeURI(key + "/" + id)).GET().build(),
            ScheduleTaskDto.class)
        .map(
            dto ->
                new SchedulerTask(
                    dto.getTaskIdentifier(),
                    dto.getTaskKey(),
                    dto.getTriggerTime(),
                    dto.getCronExpression(),
                    dto.getTaskData()))
        .orElse(null);
  }

  @Override
  public <T> ISchedulerMessageSinkBuilder<T> getMessageSinkBuilder(Class<T> messageSinkType) {
    return new SchedulerMessageSinkBuilder<>(objectMapper, messageSinkType);
  }

  private ScheduleRequestDto createCommonScheduleRequest(
      String key, String id, ZonedDateTime triggerTime, Object data) {

    if (!sinksMap.containsKey(key)) {
      throw new SchedulerServiceClientException("Key  " + key + " has no sink registered");
    }

    if (data != null && !sinksMap.get(key).getDataType().isAssignableFrom(data.getClass())) {
      throw new SchedulerServiceClientException(
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
    try {
      HttpRequest httpRequest =
          createCommonRequestBuilder(createURIfromURL(schedulesApiPath))
              .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
              .build();
      performRequest(httpRequest, Void.class);
    } catch (JsonProcessingException ex) {
      throw new SchedulerServiceClientException("Cannot serialize task request", ex);
    }
  }

  private <T> Optional<T> performRequest(HttpRequest httpRequest, Class<T> rType) {
    try {
      HttpResponse<String> httpResponse =
          httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      if (!Void.class.equals(rType) && httpResponse.statusCode() == 404) {
        return Optional.empty();
      }
      if (httpResponse.statusCode() / 100 != 2) {
        HttpErrorBody errorBody = objectMapper.readValue(httpResponse.body(), HttpErrorBody.class);
        throw new SchedulerServiceClientException(errorBody.getErrorMessage());
      }
      if (rType == null || Void.class.equals(rType)) {
        return Optional.empty();
      }
      if (MEDIA_TYPE_APPLICATION_JSON.equals(
          httpResponse.headers().firstValue("Content-Type").orElse(null))) {
        return Optional.ofNullable(objectMapper.readValue(httpResponse.body(), rType));
      }
      throw new SchedulerServiceClientException("Unexpected scheduler response Media-Type");
    } catch (JsonProcessingException ex) {
      throw new SchedulerServiceClientException(
          "Error parsing request/response scheduler task retrieval request", ex);
    } catch (IOException ex) {
      throw new SchedulerServiceClientException("Error performing " + httpRequest, ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new SchedulerServiceClientException("Send thread interrupted", ex);
    }
  }

  private HttpRequest.Builder createCommonRequestBuilder(URI uri) {
    return HttpRequest.newBuilder(uri)
        .header("Content-Type", MEDIA_TYPE_APPLICATION_JSON)
        .header("Accept", MEDIA_TYPE_APPLICATION_JSON)
        .timeout(Duration.of(clientTimeout, ChronoUnit.MILLIS));
  }

  private URI createURIfromURL(URL url) {
    try {
      return url.toURI();
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException("Cannot convert URL " + url + " to URI", ex);
    }
  }

  private URI createRelativeURI(String relativePath) {
    try {
      return new URL(schedulesApiPath, relativePath).toURI();
    } catch (MalformedURLException | URISyntaxException ex) {
      throw new IllegalArgumentException(
          "Cannot joint relative path " + relativePath + " to " + schedulesApiPath, ex);
    }
  }
}
