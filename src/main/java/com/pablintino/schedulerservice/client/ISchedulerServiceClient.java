package com.pablintino.schedulerservice.client;

import java.time.ZonedDateTime;
import java.util.Map;

public interface ISchedulerServiceClient {
    void scheduleTask(String key, String id, ZonedDateTime triggerTime, Map<String, Object> data);
    void scheduleTask(String key, String id, ZonedDateTime triggerTime, String cronExpression, Map<String, Object> data);
    void registerCallback(String key, IScheduleCallback callback);
}
