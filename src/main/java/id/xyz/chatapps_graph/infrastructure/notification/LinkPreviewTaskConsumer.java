package id.xyz.chatapps_graph.infrastructure.notification;

import id.xyz.chatapps_graph.applications.usecase.LinkPreviewService;
import id.xyz.chatapps_graph.framework.dto.LinkPreviewTask;
import id.xyz.chatapps_graph.infrastructure.config.rabbitmq.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkPreviewTaskConsumer {

  private final LinkPreviewService linkPreviewService;

  @RabbitListener(
      queues = RabbitMQConfig.LINK_PREVIEWS_QUEUE,
      containerFactory = "rabbitListenerContainerFactory"
  )
  public void consume(LinkPreviewTask task) {
    log.info("Received Link Preview Task for messageId: {} and URL: {}", task.messageId(), task.url());
    try {
      linkPreviewService.processLinkPreviewTask(task.messageId(), task.url(), task.conversationUuid());
    } catch (Exception e) {
      log.error("Failed to process Link Preview Task for messageId: {}", task.messageId(), e);
      throw e;
    }
  }
}
