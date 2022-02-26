package com.pablintino.schedulerservice.client.models;

import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class SchedulerMetadata {
  private final Instant triggerTime;
  private final long triggerAttempt;
}
