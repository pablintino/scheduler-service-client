package com.pablintino.schedulerservice.client;

import com.rabbitmq.client.Delivery;

public interface ISchedulerMessageSink<T> {

  void sink(Delivery delivery);

  Class<T> getDataType();
}
