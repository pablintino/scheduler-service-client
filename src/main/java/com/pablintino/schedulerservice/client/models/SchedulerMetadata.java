package com.pablintino.schedulerservice.client.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class SchedulerMetadata {
  private final Instant triggerTime;
  private final long triggerAttempt;
}
