package com.pablintino.schedulerservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;

class SchedulerMessageSinkBuilder<T> implements ISchedulerMessageSinkBuilder<T> {
  private final ObjectMapper objectMapper;
  private final Class<T> dataType;
  private IScheduleCallback<T> callback;

  public SchedulerMessageSinkBuilder(ObjectMapper objectMapper, Class<T> dataType) {
    this.objectMapper = objectMapper;
    this.dataType = dataType;
  }

  public ISchedulerMessageSinkBuilder<T> callback(IScheduleCallback<T> callback) {
    this.callback = callback;
    return this;
  }

  public ISchedulerMessageSink<T> build() {
    return new SchedulerMessageSink<>(objectMapper, callback, dataType);
  }
}
