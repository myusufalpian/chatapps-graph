package id.xyz.chatapps_graph.infrastructure.notification;

import static org.mockito.Mockito.verify;

import id.xyz.chatapps_graph.applications.usecase.LinkPreviewService;
import id.xyz.chatapps_graph.framework.dto.LinkPreviewTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkPreviewTaskConsumerTest {

  @Mock private LinkPreviewService linkPreviewService;

  @InjectMocks private LinkPreviewTaskConsumer consumer;

  @Test
  @DisplayName("consume: delegates to linkPreviewService.processLinkPreviewTask")
  void consume_DelegatesSuccess() {
    LinkPreviewTask task = LinkPreviewTask.builder()
        .messageId(42L)
        .url("https://example.com")
        .conversationUuid("conv-123")
        .build();

    consumer.consume(task);

    verify(linkPreviewService).processLinkPreviewTask(42L, "https://example.com", "conv-123");
  }
}
