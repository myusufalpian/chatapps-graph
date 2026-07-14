package id.xyz.chatapps_graph.framework.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.applications.usecase.DeliveryReceiptResult;
import id.xyz.chatapps_graph.applications.usecase.MessageService;
import id.xyz.chatapps_graph.applications.usecase.WebSocketBroadcastService;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.framework.dto.DeliveryReceiptRequest;
import id.xyz.chatapps_graph.framework.dto.SendMessageResult;
import id.xyz.chatapps_graph.framework.dto.TypingRequest;
import id.xyz.chatapps_graph.framework.dto.WebSocketSendMessage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerTest {

  @Mock private MessageService messageService;
  @Mock private ConversationService conversationService;
  @Mock private WebSocketBroadcastService broadcastService;

  @InjectMocks private ChatWebSocketHandler handler;

  @Test
  @DisplayName("handleSendMessage broadcasts response message")
  void handleSendMessage_succeeds() {
    WebSocketSendMessage payload = new WebSocketSendMessage("conv-uuid", "recipient-uuid", "Hello", null);
    
    Message msg = new Message();
    msg.setMessageUuid("msg-uuid");
    msg.setContent("Hello");
    msg.setMessageType("TEXT");
    msg.setMessageStatus(0);
    msg.setCreatedAt(java.time.OffsetDateTime.now());

    SendMessageResult result = new SendMessageResult(msg, "sender-uuid", "conv-uuid");

    when(messageService.sendMessage(2L, "recipient-uuid", "conv-uuid", "TEXT", "Hello", null, null))
        .thenReturn(result);

    SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
    headers.setSessionAttributes(Map.of("X-User-Id", 2L));

    handler.handleSendMessage(payload, headers);

    verify(broadcastService).broadcast(eq("/topic/chat/conv-uuid"), any());
  }

  @Test
  @DisplayName("handleSendMessage with null user ID returns early")
  void handleSendMessage_nullUserId_returnsEarly() {
    WebSocketSendMessage payload = new WebSocketSendMessage("conv-uuid", "recipient-uuid", "Hello", null);
    
    SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
    // No session attributes

    handler.handleSendMessage(payload, headers);

    verify(messageService, never()).sendMessage(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("handleDelivered broadcasts receipts to target users")
  void handleDelivered_broadcastsReceiptToSenderQueue() {
    Conversation conversation = new Conversation();
    conversation.setConversationId(10L);
    conversation.setConversationUuid("conv-uuid");
    when(conversationService.findConversationByUuid("conv-uuid")).thenReturn(conversation);
    when(conversationService.isParticipant(10L, 2L)).thenReturn(true);
    
    DeliveryReceiptResult result = new DeliveryReceiptResult(true, List.of("15550001"), "reader-uuid");
    when(messageService.markAsDelivered("conv-uuid", 2L, List.of("msg-1")))
        .thenReturn(result);

    SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
    headers.setSessionAttributes(Map.of("X-User-Id", 2L));

    handler.handleDelivered(new DeliveryReceiptRequest("conv-uuid", List.of("msg-1")), headers);

    verify(broadcastService).sendToUser(eq("15550001"), eq("/queue/chat/receipts"), any());
  }

  @Test
  @DisplayName("handleDelivered with non-participant user returns early")
  void handleDelivered_nonParticipant_returnsEarly() {
    Conversation conversation = new Conversation();
    conversation.setConversationId(10L);
    conversation.setConversationUuid("conv-uuid");
    when(conversationService.findConversationByUuid("conv-uuid")).thenReturn(conversation);
    when(conversationService.isParticipant(10L, 2L)).thenReturn(false);

    SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
    headers.setSessionAttributes(Map.of("X-User-Id", 2L));

    handler.handleDelivered(new DeliveryReceiptRequest("conv-uuid", List.of("msg-1")), headers);

    verify(messageService, never()).markAsDelivered(any(), any(), any());
  }

  @Test
  @DisplayName("handleTyping broadcasts typing event")
  void handleTyping_succeeds() {
    Conversation conversation = new Conversation();
    conversation.setConversationId(10L);
    conversation.setConversationUuid("conv-uuid");
    
    when(conversationService.findConversationByUuid("conv-uuid")).thenReturn(conversation);
    when(conversationService.isParticipant(10L, 2L)).thenReturn(true);
    when(messageService.resolveUserUuid(2L)).thenReturn("user-uuid");

    SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
    headers.setSessionAttributes(Map.of("X-User-Id", 2L));

    handler.handleTyping(new TypingRequest("conv-uuid"), headers);

    verify(broadcastService).broadcast(eq("/topic/chat/conv-uuid/typing"), any());
  }
}
