package id.xyz.chatapps_graph.framework.controller;

import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.applications.usecase.MessageService;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.MessageType;
import id.xyz.chatapps_graph.domain.enums.ReceiptStatus;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.DeliveryReceiptEvent;
import id.xyz.chatapps_graph.framework.dto.DeliveryReceiptRequest;
import id.xyz.chatapps_graph.framework.dto.MessageResponse;
import id.xyz.chatapps_graph.framework.dto.TypingEvent;
import id.xyz.chatapps_graph.framework.dto.TypingRequest;
import id.xyz.chatapps_graph.framework.dto.WebSocketSendMessage;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {

  private final MessageService messageService;
  private final ConversationService conversationService;
  private final UserRepository userRepository;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/chat/send")
  public void handleSendMessage(
      @Payload WebSocketSendMessage payload, SimpMessageHeaderAccessor headerAccessor) {
    Long userId = resolveUserId(headerAccessor);
    if (userId == null) {
      return;
    }

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

    MessageResponse response = MessageResponse.builder()
        .messageUuid(message.getMessageUuid())
        .conversationUuid(conv.getConversationUuid())
        .senderUuid(senderUuid)
        .messageType(message.getMessageType())
        .content(message.getContent())
        .status(message.getMessageStatus())
        .deliveryStatus(ReceiptStatus.SENT.getValue())
        .createdAt(message.getCreatedAt())
        .build();

    messagingTemplate.convertAndSend("/topic/chat/" + conv.getConversationUuid(), response);
  }

  @MessageMapping("/chat/delivered")
  public void handleDelivered(
      @Payload DeliveryReceiptRequest request, SimpMessageHeaderAccessor headerAccessor) {
    Long userId = resolveUserId(headerAccessor);
    if (userId == null) {
      return;
    }

    Conversation conv = conversationService.findConversationByUuid(request.conversationUuid());
    if (!conversationService.isParticipant(conv.getConversationId(), userId)) {
      return;
    }

    var result = messageService.markAsDelivered(request.conversationUuid(), userId, request.messageUuids());
    if (!result.receiptsUpdated()) {
      return;
    }

    User reader = userRepository.findById(userId).orElse(null);
    DeliveryReceiptEvent event = new DeliveryReceiptEvent("MESSAGE_DELIVERED", request.conversationUuid(),
        request.messageUuids(), reader != null ? reader.getUserUuid() : null, "DELIVERED");

    userRepository.findAllById(result.senderIds()).stream()
        .filter(sender -> StringUtils.hasLength(sender.getUserPhone()))
        .forEach(sender -> messagingTemplate.convertAndSendToUser(sender.getUserPhone(),
            "/queue/chat/receipts", event));
  }

  @MessageMapping("/chat/typing")
  public void handleTyping(
      @Payload TypingRequest request, SimpMessageHeaderAccessor headerAccessor) {
    Long userId = resolveUserId(headerAccessor);
    if (userId == null) {
      return;
    }

    Conversation conv = conversationService.findConversationByUuid(request.conversationUuid());
    if (!conversationService.isParticipant(conv.getConversationId(), userId)) {
      return;
    }

    User sender = userRepository.findById(userId).orElse(null);
    String userUuid = sender != null ? sender.getUserUuid() : null;

    TypingEvent event = TypingEvent.builder()
        .conversationUuid(request.conversationUuid())
        .userUuid(userUuid)
        .isTyping(true)
        .build();

    messagingTemplate.convertAndSend("/topic/chat/" + request.conversationUuid() + "/typing", event);
  }

  /**
   * Safely resolves the authenticated user ID from the WebSocket session attributes.
   * Returns {@code null} if the session attributes map is null or the user ID is not set,
   * preventing a {@link NullPointerException} from an uninitialized session.
   */
  private Long resolveUserId(SimpMessageHeaderAccessor headerAccessor) {
    Map<String, Object> attrs = headerAccessor.getSessionAttributes();
    if (attrs == null) {
      return null;
    }
    return (Long) attrs.get("X-User-Id");
  }
}
