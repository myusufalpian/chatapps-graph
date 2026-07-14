package id.xyz.chatapps_graph.infrastructure.notification;

import static org.mockito.Mockito.verify;

import id.xyz.chatapps_graph.applications.usecase.adapters.PushNotificationServiceImpl;
import id.xyz.chatapps_graph.framework.dto.FCMNotificationTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FCMNotificationTaskConsumerTest {

  @Mock private PushNotificationServiceImpl pushNotificationService;

  @InjectMocks private FCMNotificationTaskConsumer consumer;

  @Test
  @DisplayName("consume: delegates to pushNotificationService.executeSendPush")
  void consume_DelegatesSuccess() {
    FCMNotificationTask task = FCMNotificationTask.builder()
        .messageId(10L)
        .senderId(20L)
        .conversationId(30L)
        .build();

    consumer.consume(task);

    verify(pushNotificationService).executeSendPush(10L, 20L, 30L);
  }
}
// 
