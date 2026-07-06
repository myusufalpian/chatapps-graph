package id.xyz.chatapps_graph.applications.usecase.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.enums.MessageStatus;
import id.xyz.chatapps_graph.domain.enums.MessageType;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMessageService {

  private final MessageRepository messageRepository;
  private final ConversationParticipantRepository participantRepository;
  private final ObjectMapper objectMapper;

  public Message create(Long conversationId, Long actorId, String event, String actorUuid, String targetUuid) {
    String content = buildContent(event, actorUuid, targetUuid);

    Message message = messageRepository.save(Message.builder()
        .conversationId(conversationId)
        .senderId(actorId)
        .messageType(MessageType.SYSTEM.name())
        .content(content)
        .messageStatus(MessageStatus.ACTIVE.getValue())
        .build());

    participantRepository.incrementUnreadAndUpdateLastMessage(
        conversationId, actorId, message.getCreatedAt(), event, MessageType.SYSTEM.name());
    participantRepository.updateSenderLastMessage(
        conversationId, actorId, message.getCreatedAt(), event, MessageType.SYSTEM.name());

    return message;
  }

  private String buildContent(String event, String actorUuid, String targetUuid) {
    try {
      Map<String, String> payload = new LinkedHashMap<>();
      payload.put("event", event);
      payload.put("actorUuid", actorUuid);
      if (targetUuid != null) {
        payload.put("targetUuid", targetUuid);
      }
      return objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      log.error("Failed to serialize system message content: {}", e.getMessage());
      return "{\"event\":\"" + event + "\"}";
    }
  }
}
