package id.xyz.chatapps_graph.infrastructure.notification;

import id.xyz.chatapps_graph.applications.usecase.adapters.PushNotificationServiceImpl;
import id.xyz.chatapps_graph.framework.dto.FCMNotificationTask;
import id.xyz.chatapps_graph.infrastructure.config.rabbitmq.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FCMNotificationTaskConsumer {

  private final PushNotificationServiceImpl pushNotificationService;

  @RabbitListener(
      queues = RabbitMQConfig.FCM_NOTIFICATIONS_QUEUE,
      containerFactory = "rabbitListenerContainerFactory"
  )
  public void consume(FCMNotificationTask task) {
    log.info("Received FCM Notification Task for messageId: {}", task.messageId());
    try {
      pushNotificationService.executeSendPush(task.messageId(), task.senderId(), task.conversationId());
    } catch (Exception e) {
      log.error("Failed to process FCM Notification Task for messageId: {}", task.messageId(), e);
      throw e;
    }
  }
}
