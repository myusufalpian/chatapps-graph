package id.xyz.chatapps_graph.infrastructure.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.xyz.chatapps_graph.framework.dto.ChatEventPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class RedisChatEventListenerTest {

  @Mock private SimpMessagingTemplate messagingTemplate;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private RedisChatEventListener listener;

  @Test
  @DisplayName("onMessage: standard broadcast destination forwards to SimpMessagingTemplate")
  void onMessage_BroadcastDestination_Success() throws Exception {
    Message message = mock(Message.class);
    byte[] body = "event-json".getBytes();
    when(message.getBody()).thenReturn(body);

    ChatEventPayload event = ChatEventPayload.builder()
        .destination("/topic/chat/123")
        .payload("payload")
        .build();

    when(objectMapper.readValue(body, ChatEventPayload.class)).thenReturn(event);

    listener.onMessage(message, new byte[0]);

    verify(messagingTemplate).convertAndSend("/topic/chat/123", "payload");
  }

  @Test
  @DisplayName("onMessage: user-specific destination forwards via convertAndSendToUser")
  void onMessage_UserDestination_Success() throws Exception {
    Message message = mock(Message.class);
    byte[] body = "event-json".getBytes();
    when(message.getBody()).thenReturn(body);

    ChatEventPayload event = ChatEventPayload.builder()
        .destination("/user/john_doe/queue/replies")
        .payload("payload")
        .build();

    when(objectMapper.readValue(body, ChatEventPayload.class)).thenReturn(event);

    listener.onMessage(message, new byte[0]);

    verify(messagingTemplate).convertAndSendToUser("john_doe", "/queue/replies", "payload");
  }
}
