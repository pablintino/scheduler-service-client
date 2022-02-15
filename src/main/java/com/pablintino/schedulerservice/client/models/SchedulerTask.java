package com.pablintino.schedulerservice.client.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.util.Map;

@Getter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SchedulerTask {

  @EqualsAndHashCode.Include private final String taskIdentifier;
  @EqualsAndHashCode.Include private final String taskKey;

  private final ZonedDateTime triggerTime;
  private final String cronExpression;

  @ToString.Exclude private final Map<String, Object> taskData;
}
