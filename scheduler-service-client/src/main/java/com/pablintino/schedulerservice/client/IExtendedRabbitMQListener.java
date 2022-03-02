package com.pablintino.schedulerservice.client;

public interface IExtendedRabbitMQListener {
  void declareQueue(String name);

  void bindQueue(String queueName, String exchange, String key);

  void installConsumer(String queue, ISchedulerMessageSink<?> listener);
}
