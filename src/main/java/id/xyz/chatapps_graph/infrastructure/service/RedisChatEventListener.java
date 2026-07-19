package id.xyz.chatapps_graph.infrastructure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.xyz.chatapps_graph.framework.dto.ChatEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatEventListener implements MessageListener {

  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
    byte[] body = message.getBody();
    if (body.length == 0) {
      return;
    }

    try {
      ChatEventPayload event = objectMapper.readValue(body, ChatEventPayload.class);
      
      Object parsedPayload = event.payload();
      
      if (event.destination().startsWith("/user/")) {
        String dest = event.destination().substring(6); // remove "/user/"
        int firstSlash = dest.indexOf('/');
        if (firstSlash != -1) {
          String username = dest.substring(0, firstSlash);
          String realDest = dest.substring(firstSlash);
          messagingTemplate.convertAndSendToUser(username, realDest, parsedPayload);
        }
      } else {
        messagingTemplate.convertAndSend(event.destination(), parsedPayload);
      }
    } catch (Exception e) {
      log.error("Failed to process Redis pub/sub chat event", e);
    }
  }
}
