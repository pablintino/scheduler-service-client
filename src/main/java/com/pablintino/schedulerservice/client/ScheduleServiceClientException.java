package com.pablintino.schedulerservice.client;

public class ScheduleServiceClientException extends RuntimeException {
  public ScheduleServiceClientException(String message) {
    super(message);
  }

  public ScheduleServiceClientException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
