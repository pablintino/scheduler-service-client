package com.pablintino.schedulerservice.client;

import com.pablintino.schedulerservice.client.models.SchedulerTask;

import java.time.ZonedDateTime;

public interface ISchedulerServiceClient {
  void scheduleTask(String key, String id, ZonedDateTime triggerTime, Object data);

  void scheduleTask(
      String key, String id, ZonedDateTime triggerTime, String cronExpression, Object data);

  void registerMessageSink(String key, ISchedulerMessageSink<?> messageSink);

  void deleteTask(String key, String id);

  SchedulerTask getTask(String key, String id);

  SchedulerMessageSink.Builder getMessageSinkBuilder();
}
