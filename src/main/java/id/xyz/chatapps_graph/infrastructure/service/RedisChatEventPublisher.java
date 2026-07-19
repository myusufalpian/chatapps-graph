package id.xyz.chatapps_graph.infrastructure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.xyz.chatapps_graph.framework.dto.ChatEventPayload;
import id.xyz.chatapps_graph.infrastructure.config.redis.RedisPubSubConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisChatEventPublisher {

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  public void publish(String destination, Object payload) {
    try {
      ChatEventPayload event = ChatEventPayload.builder()
          .destination(destination)
          .payload(payload)
          .build();
      String eventJson = objectMapper.writeValueAsString(event);
      stringRedisTemplate.convertAndSend(RedisPubSubConfig.CHAT_EVENTS_TOPIC, eventJson);
    } catch (Exception e) {
      log.error("Failed to publish Redis pub/sub chat event to {}", destination, e);
    }
  }
}
