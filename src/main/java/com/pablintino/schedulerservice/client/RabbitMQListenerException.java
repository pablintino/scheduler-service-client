package com.pablintino.schedulerservice.client;

public class RabbitMQListenerException extends RuntimeException {
  public RabbitMQListenerException(String message) {
    super(message);
  }

  public RabbitMQListenerException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
