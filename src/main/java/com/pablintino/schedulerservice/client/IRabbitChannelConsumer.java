package com.pablintino.schedulerservice.client;

import com.rabbitmq.client.Channel;

import java.io.IOException;

@FunctionalInterface
interface IRabbitChannelConsumer {
  void accept(Channel t) throws IOException;
}
