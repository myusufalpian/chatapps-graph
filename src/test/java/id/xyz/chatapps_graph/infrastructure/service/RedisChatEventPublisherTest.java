package id.xyz.chatapps_graph.infrastructure.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.xyz.chatapps_graph.framework.dto.ChatEventPayload;
import id.xyz.chatapps_graph.infrastructure.config.redis.RedisPubSubConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisChatEventPublisherTest {

  @Mock private StringRedisTemplate stringRedisTemplate;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private RedisChatEventPublisher publisher;

  @Test
  @DisplayName("publish: serializes event payload and publishes to redis")
  void publish_Success() throws Exception {
    String destination = "/topic/test";
    String payload = "test-payload";
    String payloadJson = "\"test-payload\"";
    
    ChatEventPayload eventPayload = ChatEventPayload.builder()
        .destination(destination)
        .payload(payload)
        .build();
    String eventJson = "{\"destination\":\"/topic/test\",\"payload\":\"test-payload\"}";

    when(objectMapper.writeValueAsString(eventPayload)).thenReturn(eventJson);

    publisher.publish(destination, payload);

    verify(stringRedisTemplate).convertAndSend(RedisPubSubConfig.CHAT_EVENTS_TOPIC, eventJson);
  }
}
