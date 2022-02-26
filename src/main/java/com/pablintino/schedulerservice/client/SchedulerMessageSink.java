package com.pablintino.schedulerservice.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pablintino.schedulerservice.callback.CallbackMessage;
import com.pablintino.schedulerservice.client.models.SchedulerMetadata;
import com.rabbitmq.client.Delivery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
class SchedulerMessageSink<T extends Object> implements ISchedulerMessageSink<T> {

  private static final String MEDIA_TYPE_APPLICATION_JSON = "application/json";
  private final ObjectMapper objectMapper;
  private final Class<T> dataType;
  private final IScheduleCallback<T> callback;

  private SchedulerMessageSink(
      ObjectMapper objectMapper, IScheduleCallback<T> callback, Class<T> dataType) {
    this.objectMapper = objectMapper;
    this.dataType = dataType;
    this.callback = callback;
  }

  public static class Builder {
    private final ObjectMapper objectMapper;

    Builder(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
    }

    public <T> TypedBuilder<T> ofType(Class<T> dataType) {
      return new TypedBuilder<>(objectMapper, dataType);
    }
  }

  public static class TypedBuilder<T> {
    private final ObjectMapper objectMapper;
    private final Class<T> dataType;
    private IScheduleCallback<T> callback;

    public TypedBuilder(ObjectMapper objectMapper, Class<T> dataType) {
      this.objectMapper = objectMapper;
      this.dataType = dataType;
    }

    public TypedBuilder<T> callback(IScheduleCallback<T> callback) {
      this.callback = callback;
      return this;
    }

    public ISchedulerMessageSink<T> build() {
      return new SchedulerMessageSink<>(objectMapper, callback, dataType);
    }
  }

  @Override
  public void sink(Delivery delivery) {
    String encoding = delivery.getProperties().getContentEncoding();
    try {
      Charset charset =
          StringUtils.isNotBlank(encoding) && Charset.isSupported(encoding)
              ? Charset.forName(encoding)
              : StandardCharsets.UTF_8;
      if (MEDIA_TYPE_APPLICATION_JSON.equals(delivery.getProperties().getContentType())) {
        String jsonMessage = new String(delivery.getBody(), charset);

        CallbackMessage callbackMessage =
            objectMapper.readValue(jsonMessage, CallbackMessage.class);
        Object data = callbackMessage.getData();
        T payload =
            data != null
                ? objectMapper.readValue(objectMapper.writeValueAsString(data), dataType)
                : null;

        SchedulerMetadata metadata =
            new SchedulerMetadata(
                callbackMessage.getTriggerTime(), callbackMessage.getNotificationAttempt());
        callback.callback(callbackMessage.getId(), callbackMessage.getKey(), payload, metadata);
      } else {
        log.error("Received a message delivery that cannot be deserialized");
      }
    } catch (JsonProcessingException ex) {
      log.error("Exception serializing/deserializing incoming scheduler message", ex);
    } catch (Exception ex) {
      log.error("Exception processing incoming scheduler message", ex);
    }
  }

  @Override
  public Class<T> getDataType() {
    return dataType;
  }
}
