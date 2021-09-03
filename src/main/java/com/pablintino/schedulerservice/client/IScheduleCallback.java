package com.pablintino.schedulerservice.client;

import com.pablintino.schedulerservice.client.models.SchedulerMetadata;

import java.util.Map;

@FunctionalInterface
public interface IScheduleCallback {
    void callback(String id, String key, Map<String, Object> dataMap, SchedulerMetadata metadata);
}
