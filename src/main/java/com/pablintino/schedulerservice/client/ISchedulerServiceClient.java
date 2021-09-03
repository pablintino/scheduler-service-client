package com.pablintino.schedulerservice.client;

import com.pablintino.schedulerservice.client.models.SchedulerTask;

import java.time.ZonedDateTime;
import java.util.Map;

public interface ISchedulerServiceClient {
    void scheduleTask(String key, String id, ZonedDateTime triggerTime, Map<String, Object> data);
    void scheduleTask(String key, String id, ZonedDateTime triggerTime, String cronExpression, Map<String, Object> data);
    void registerCallback(String key, IScheduleCallback callback);
    void deleteTask(String key, String id);
    SchedulerTask getTask(String key, String id);
}
