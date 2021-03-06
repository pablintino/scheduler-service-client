package com.pablintino.schedulerservice.client;

import com.pablintino.schedulerservice.client.models.SchedulerMetadata;

@FunctionalInterface
public interface IScheduleCallback<T> {
  void callback(String id, String key, T data, SchedulerMetadata metadata);
}
