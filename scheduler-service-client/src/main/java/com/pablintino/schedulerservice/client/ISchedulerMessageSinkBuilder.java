package com.pablintino.schedulerservice.client;

public interface ISchedulerMessageSinkBuilder<T> {
  ISchedulerMessageSinkBuilder<T> callback(IScheduleCallback<T> callback);

  ISchedulerMessageSink<T> build();
}
