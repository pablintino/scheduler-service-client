package com.pablintino.schedulerservice.client;

public class SchedulerServiceClientException extends RuntimeException {
  public SchedulerServiceClientException(String message) {
    super(message);
  }

  public SchedulerServiceClientException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
