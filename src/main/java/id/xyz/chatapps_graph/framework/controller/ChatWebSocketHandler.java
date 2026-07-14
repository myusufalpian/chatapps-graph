package id.xyz.chatapps_graph.framework.controller;

import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.applications.usecase.MessageService;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.enums.MessageType;
import id.xyz.chatapps_graph.domain.enums.ReceiptStatus;
import id.xyz.chatapps_graph.framework.dto.DeliveryReceiptEvent;
import id.xyz.chatapps_graph.framework.dto.DeliveryReceiptRequest;
import id.xyz.chatapps_graph.framework.dto.MessageResponse;
import id.xyz.chatapps_graph.framework.dto.SendMessageResult;
import id.xyz.chatapps_graph.framework.dto.TypingEvent;
import id.xyz.chatapps_graph.framework.dto.TypingRequest;
import id.xyz.chatapps_graph.framework.dto.WebSocketSendMessage;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import id.xyz.chatapps_graph.applications.usecase.WebSocketBroadcastService;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {

  private final MessageService messageService;
  private final ConversationService conversationService;
  private final WebSocketBroadcastService broadcastService;

  @MessageMapping("/chat/send")
  public void handleSendMessage(
      @Payload WebSocketSendMessage payload, SimpMessageHeaderAccessor headerAccessor) {
    Long userId = resolveUserId(headerAccessor);
    if (userId == null) {
      return;
    }

    SendMessageResult result = messageService.sendMessage(userId, payload.recipientUuid(),
        payload.conversationUuid(), MessageType.TEXT.name(), payload.content(),
        null, payload.replyToMessageUuid());

    MessageResponse response = MessageResponse.builder()
        .messageUuid(result.message().getMessageUuid())
        .conversationUuid(result.conversationUuid())
        .senderUuid(result.senderUuid())
        .messageType(result.message().getMessageType())
        .content(result.message().getContent())
        .status(result.message().getMessageStatus())
        .deliveryStatus(ReceiptStatus.SENT.getValue())
        .createdAt(result.message().getCreatedAt())
        .build();

    broadcastService.broadcast("/topic/chat/" + result.conversationUuid(), response);
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

    DeliveryReceiptEvent event = new DeliveryReceiptEvent("MESSAGE_DELIVERED", request.conversationUuid(),
        request.messageUuids(), result.readerUuid(), "DELIVERED");

    result.targetUserPhones().forEach(phone ->
        broadcastService.sendToUser(phone, "/queue/chat/receipts", event));
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

    String userUuid = messageService.resolveUserUuid(userId);

    TypingEvent event = TypingEvent.builder()
        .conversationUuid(request.conversationUuid())
        .userUuid(userUuid)
        .isTyping(true)
        .build();

    broadcastService.broadcast("/topic/chat/" + request.conversationUuid() + "/typing", event);
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
