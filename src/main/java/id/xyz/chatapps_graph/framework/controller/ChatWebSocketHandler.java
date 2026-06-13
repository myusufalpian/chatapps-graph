package id.xyz.chatapps_graph.framework.controller;

import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.applications.usecase.MessageService;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.MessageType;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.MessageResponse;
import id.xyz.chatapps_graph.framework.dto.WebSocketSendMessage;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {

  private final MessageService messageService;
  private final ConversationService conversationService;
  private final UserRepository userRepository;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/chat/send")
  public void handleSendMessage(@Payload WebSocketSendMessage payload, SimpMessageHeaderAccessor headerAccessor) {
    Long userId = (Long) headerAccessor.getSessionAttributes().get("X-User-Id");

    Message message = messageService.sendMessage(userId, payload.recipientUuid(),
        payload.conversationUuid(), MessageType.TEXT.name(), payload.content(),
        null, payload.replyToMessageUuid());

    User sender = userRepository.findById(userId).orElse(null);
    String senderUuid = sender != null ? sender.getUserUuid() : null;

    Conversation conv;
    if (payload.conversationUuid() != null) {
      conv = conversationService.findConversationByUuid(payload.conversationUuid());
    } else {
      User recipient = userRepository.findUserByUserUuidAndUserStatus(payload.recipientUuid(), 0).orElse(null);
      conv = conversationService.findOrCreatePrivateConversation(userId, recipient != null ? recipient.getUserId() : 0L);
    }

    MessageResponse response = new MessageResponse(message.getMessageUuid(), conv.getConversationUuid(),
        senderUuid, message.getMessageType(), message.getContent(), null, null,
        message.getMessageStatus(), message.getCreatedAt());

    messagingTemplate.convertAndSend("/topic/chat/" + conv.getConversationUuid(), response);

    messagingTemplate.convertAndSend("/topic/chat/" + conv.getConversationUuid() + "/receipts",
        Map.of("messageUuid", message.getMessageUuid(), "status", "DELIVERED"));
  }
}
