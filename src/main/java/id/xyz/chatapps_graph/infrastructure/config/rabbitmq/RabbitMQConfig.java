package id.xyz.chatapps_graph.infrastructure.config.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
public class RabbitMQConfig {

  public static final String CHAT_TASK_EXCHANGE = "chat.task.exchange";
  public static final String CHAT_TASK_EXCHANGE_DLX = "chat.task.exchange.dlx";

  public static final String LINK_PREVIEWS_QUEUE = "chat.task.link-previews";
  public static final String LINK_PREVIEWS_DLQ = "chat.task.link-previews.dlq";
  public static final String LINK_PREVIEWS_ROUTING_KEY = "link-previews";

  public static final String FCM_NOTIFICATIONS_QUEUE = "chat.task.fcm-notifications";
  public static final String FCM_NOTIFICATIONS_DLQ = "chat.task.fcm-notifications.dlq";
  public static final String FCM_NOTIFICATIONS_ROUTING_KEY = "fcm-notifications";

  public static final String VOICE_METADATA_QUEUE = "chat.task.voice-metadata";
  public static final String VOICE_METADATA_RETRY_QUEUE = "chat.task.voice-metadata.retry";
  public static final String VOICE_METADATA_RETRY_5M_QUEUE = "chat.task.voice-metadata.retry-5m";
  public static final String VOICE_METADATA_DLQ = "chat.task.voice-metadata.dlq";
  public static final String VOICE_METADATA_ROUTING_KEY = "voice-metadata";
  public static final String EXPORT_QUEUE = "chat.task.export";
  public static final String EXPORT_DLQ = "chat.task.export.dlq";
  public static final String EXPORT_RETRY_QUEUE = "chat.task.export.retry";
  public static final String EXPORT_RETRY_5M_QUEUE = "chat.task.export.retry-5m";
  public static final String EXPORT_ROUTING_KEY = "export";

  @Bean
  public DirectExchange chatTaskExchange() {
    return new DirectExchange(CHAT_TASK_EXCHANGE);
  }

  @Bean
  public DirectExchange chatTaskExchangeDlx() {
    return new DirectExchange(CHAT_TASK_EXCHANGE_DLX);
  }

  // Queues & DLQs
  @Bean
  public Queue linkPreviewsQueue() {
    return QueueBuilder.durable(LINK_PREVIEWS_QUEUE)
        .withArgument("x-dead-letter-exchange", CHAT_TASK_EXCHANGE_DLX)
        .withArgument("x-dead-letter-routing-key", LINK_PREVIEWS_ROUTING_KEY)
        .build();
  }

  @Bean
  public Queue linkPreviewsDlq() {
    return QueueBuilder.durable(LINK_PREVIEWS_DLQ).build();
  }

  @Bean
  public Queue fcmNotificationsQueue() {
    return QueueBuilder.durable(FCM_NOTIFICATIONS_QUEUE)
        .withArgument("x-dead-letter-exchange", CHAT_TASK_EXCHANGE_DLX)
        .withArgument("x-dead-letter-routing-key", FCM_NOTIFICATIONS_ROUTING_KEY)
        .build();
  }

  @Bean
  public Queue fcmNotificationsDlq() {
    return QueueBuilder.durable(FCM_NOTIFICATIONS_DLQ).build();
  }

  @Bean
  public Queue voiceMetadataQueue() {
    return QueueBuilder.durable(VOICE_METADATA_QUEUE)
        .withArgument("x-dead-letter-exchange", CHAT_TASK_EXCHANGE_DLX)
        .withArgument("x-dead-letter-routing-key", VOICE_METADATA_ROUTING_KEY)
        .build();
  }

  @Bean
  public Queue voiceMetadataDlq() {
    return QueueBuilder.durable(VOICE_METADATA_DLQ).build();
  }

  @Bean
  public Queue voiceMetadataRetryQueue() {
    return QueueBuilder.durable(VOICE_METADATA_RETRY_QUEUE)
        .withArgument("x-message-ttl", 30000)
        .withArgument("x-dead-letter-exchange", CHAT_TASK_EXCHANGE)
        .withArgument("x-dead-letter-routing-key", VOICE_METADATA_ROUTING_KEY)
        .build();
  }

  @Bean
  public Queue voiceMetadataRetry5mQueue() {
    return QueueBuilder.durable(VOICE_METADATA_RETRY_5M_QUEUE)
        .withArgument("x-message-ttl", 300000)
        .withArgument("x-dead-letter-exchange", CHAT_TASK_EXCHANGE)
        .withArgument("x-dead-letter-routing-key", VOICE_METADATA_ROUTING_KEY)
        .build();
  }

  // Bindings
  @Bean
  public Binding linkPreviewsBinding(Queue linkPreviewsQueue, DirectExchange chatTaskExchange) {
    return BindingBuilder.bind(linkPreviewsQueue).to(chatTaskExchange).with(LINK_PREVIEWS_ROUTING_KEY);
  }

  @Bean
  public Binding linkPreviewsDlqBinding(Queue linkPreviewsDlq, DirectExchange chatTaskExchangeDlx) {
    return BindingBuilder.bind(linkPreviewsDlq).to(chatTaskExchangeDlx).with(LINK_PREVIEWS_ROUTING_KEY);
  }

  @Bean
  public Binding fcmNotificationsBinding(Queue fcmNotificationsQueue, DirectExchange chatTaskExchange) {
    return BindingBuilder.bind(fcmNotificationsQueue).to(chatTaskExchange).with(FCM_NOTIFICATIONS_ROUTING_KEY);
  }

  @Bean
  public Binding fcmNotificationsDlqBinding(Queue fcmNotificationsDlq, DirectExchange chatTaskExchangeDlx) {
    return BindingBuilder.bind(fcmNotificationsDlq).to(chatTaskExchangeDlx).with(FCM_NOTIFICATIONS_ROUTING_KEY);
  }

  @Bean
  public Binding voiceMetadataBinding(Queue voiceMetadataQueue, DirectExchange chatTaskExchange) {
    return BindingBuilder.bind(voiceMetadataQueue).to(chatTaskExchange).with(VOICE_METADATA_ROUTING_KEY);
  }

  @Bean
  public Binding voiceMetadataDlqBinding(Queue voiceMetadataDlq, DirectExchange chatTaskExchangeDlx) {
    return BindingBuilder.bind(voiceMetadataDlq).to(chatTaskExchangeDlx).with(VOICE_METADATA_ROUTING_KEY);
  }

  @Bean
  public Queue exportQueue() {
    return QueueBuilder.durable(EXPORT_QUEUE)
        .withArgument("x-dead-letter-exchange", CHAT_TASK_EXCHANGE_DLX)
        .withArgument("x-dead-letter-routing-key", EXPORT_ROUTING_KEY)
        .build();
  }

  @Bean
  public Queue exportDlq() {
    return QueueBuilder.durable(EXPORT_DLQ).build();
  }

  @Bean
  public Queue exportRetryQueue() {
    return QueueBuilder.durable(EXPORT_RETRY_QUEUE)
        .withArgument("x-message-ttl", 30000)
        .withArgument("x-dead-letter-exchange", CHAT_TASK_EXCHANGE)
        .withArgument("x-dead-letter-routing-key", EXPORT_ROUTING_KEY)
        .build();
  }

  @Bean
  public Queue exportRetry5mQueue() {
    return QueueBuilder.durable(EXPORT_RETRY_5M_QUEUE)
        .withArgument("x-message-ttl", 300000)
        .withArgument("x-dead-letter-exchange", CHAT_TASK_EXCHANGE)
        .withArgument("x-dead-letter-routing-key", EXPORT_ROUTING_KEY)
        .build();
  }

  @Bean
  public Binding exportBinding(Queue exportQueue, DirectExchange chatTaskExchange) {
    return BindingBuilder.bind(exportQueue).to(chatTaskExchange).with(EXPORT_ROUTING_KEY);
  }

  @Bean
  public Binding exportDlqBinding(Queue exportDlq, DirectExchange chatTaskExchangeDlx) {
    return BindingBuilder.bind(exportDlq).to(chatTaskExchangeDlx).with(EXPORT_ROUTING_KEY);
  }

  @Bean
  public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper);
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory,
      Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jackson2JsonMessageConverter);

    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("rabbit-vt-");
    executor.setVirtualThreads(true);
    factory.setTaskExecutor(executor);
    
    return factory;
  }
}
