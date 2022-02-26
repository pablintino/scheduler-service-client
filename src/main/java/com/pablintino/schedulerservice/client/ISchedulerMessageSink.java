package com.pablintino.schedulerservice.client;

import com.rabbitmq.client.Delivery;

public interface ISchedulerMessageSink<T extends Object> {

  void sink(Delivery delivery);

  Class<T> getDataType();
}
