package com.pablintino.schedulerservice.client;

import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@FunctionalInterface
interface IRabbitChannelConsumer {
  void accept(Channel t) throws IOException, TimeoutException;
}
