package id.xyz.chatapps_graph.framework.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.applications.usecase.DeliveryReceiptResult;
import id.xyz.chatapps_graph.applications.usecase.MessageService;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.DeliveryReceiptRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerTest {

  @Mock private MessageService messageService;
  @Mock private ConversationService conversationService;
  @Mock private UserRepository userRepository;
  @Mock private SimpMessagingTemplate messagingTemplate;

  @InjectMocks private ChatWebSocketHandler handler;

  @Test
  void handleDelivered_broadcastsReceiptToSenderQueue() {
    Conversation conversation = new Conversation();
    conversation.setConversationId(10L);
    conversation.setConversationUuid("conv-uuid");
    when(conversationService.findConversationByUuid("conv-uuid")).thenReturn(conversation);
    when(conversationService.isParticipant(10L, 2L)).thenReturn(true);
    when(messageService.markAsDelivered("conv-uuid", 2L, List.of("msg-1")))
        .thenReturn(new DeliveryReceiptResult(true, List.of(7L)));

    User reader = User.builder().userId(2L).userUuid("reader-uuid").build();
    User sender = User.builder().userId(7L).userPhone("15550001").build();
    when(userRepository.findById(2L)).thenReturn(Optional.of(reader));
    when(userRepository.findAllById(List.of(7L))).thenReturn(List.of(sender));

    SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
    headers.setSessionAttributes(Map.of("X-User-Id", 2L));

    handler.handleDelivered(new DeliveryReceiptRequest("conv-uuid", List.of("msg-1")), headers);

    verify(messagingTemplate).convertAndSendToUser(eq("15550001"), eq("/queue/chat/receipts"),
        any());
  }
}
