package com.pablintino.schedulerservice.client;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@Slf4j
public class RabbitMQExtendedListener implements IExtendedRabbitMQListener, AutoCloseable {

  private static final int FAILURE_REATTEMPTS = 3;
  private final Map<String, ISchedulerMessageSink<?>> consumers = new HashMap<>();
  private final ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(10);
  private final ConnectionFactory connectionFactory;
  private Connection connection;
  private Channel channel;

  public RabbitMQExtendedListener(String uri) {
    if (StringUtils.isBlank(uri)) {
      throw new IllegalArgumentException("uri cannot be null or empty");
    }
    connectionFactory = new ConnectionFactory();
    connectionFactory.setSharedExecutor(threadPoolExecutor);
    try {
      connectionFactory.setUri(uri);
    } catch (URISyntaxException | NoSuchAlgorithmException | KeyManagementException ex) {
      throw new RabbitMQListenerException("URI cannot be parsed", ex);
    }
  }

  public RabbitMQExtendedListener(ConnectionFactory connectionFactory) {
    if (connectionFactory == null) {
      throw new IllegalArgumentException("connectionFactory cannot be null");
    }
    this.connectionFactory = connectionFactory;
  }

  @Override
  public void installConsumer(String queue, ISchedulerMessageSink<?> messageSink) {
    synchronized (consumers) {
      if (consumers.containsKey(queue)) {
        throw new RabbitMQListenerException("Queue has already a registered consumer");
      }
      try {
        callOnChannel(c -> addChannelConsumer(c, queue, messageSink));
        /* Add message sink to map always after inserting. If done before and a channel recreation occurs it could be
        tried to install twice */
        consumers.put(queue, messageSink);
      } catch (IOException | TimeoutException ex) {
        throw new RabbitMQListenerException("Exception installing consumer for queue " + queue, ex);
      }
    }
  }

  @Override
  public void declareQueue(String name) {
    try {
      callOnChannel(c -> c.queueDeclare(name, true, false, true, null));
    } catch (IOException | TimeoutException ex) {
      throw new RabbitMQListenerException("Exception declaring consumer for queue " + name, ex);
    }
  }

  @Override
  public void bindQueue(String queueName, String exchange, String key) {
    try {
      callOnChannel(c -> c.queueBind(queueName, exchange, key));
    } catch (IOException | TimeoutException ex) {
      throw new RabbitMQListenerException(
          "Exception binding queue " + queueName + " to " + exchange, ex);
    }
  }

  @Override
  public void close() throws Exception {
    if (connection != null) {
      connection.close();
    }
  }

  private synchronized Channel getChannel() throws IOException, TimeoutException {
    if (connection == null || !connection.isOpen()) {
      connection = connectionFactory.newConnection(getAppIdentifier());
    }
    if (channel == null || !channel.isOpen()) {
      Channel chan = connection.createChannel(); // NOSONAR
      /* Close the old channel */
      closeChannel(channel);
      synchronized (consumers) {
        for (Map.Entry<String, ISchedulerMessageSink<?>> entry : consumers.entrySet()) {
          addChannelConsumer(chan, entry.getKey(), entry.getValue());
        }

        channel = chan;
      }
    }

    return channel;
  }

  private static void addChannelConsumer(
      Channel channel, String queue, ISchedulerMessageSink<?> messageSink) throws IOException {
    channel.basicConsume(
        queue, true, (String consumerTag, Delivery message) -> messageSink.sink(message), s -> {});
  }

  private void callOnChannel(IRabbitChannelConsumer function) throws IOException, TimeoutException {
    IOException exception;
    int remains = FAILURE_REATTEMPTS;
    do {
      try {
        function.accept(getChannel());
        return;
      } catch (IOException ex) {
        exception = ex;
        remains--;
      }
    } while (remains > 0);
    throw exception;
  }

  private static void closeChannel(Channel channel) {
    if (channel != null) {
      try {
        channel.close();
      } catch (IOException | TimeoutException ex) {
        log.error("Error while closing AMQP channel " + channel);
      }
    }
  }

  private static String getAppIdentifier() throws UnknownHostException {
    return "scheduler-client-"
        + InetAddress.getLocalHost().getHostName()
        + "-"
        + ProcessHandle.current().pid();
  }
}
