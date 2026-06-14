package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.usecase.AttachmentService;
import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.applications.usecase.RateLimitService;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.enums.MessageStatus;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReceiptRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageServiceSprint1Test {

  @Mock private MessageRepository messageRepository;
  @Mock private MessageReceiptRepository receiptRepository;
  @Mock private ConversationParticipantRepository participantRepository;
  @Mock private ConversationService conversationService;
  @Mock private AttachmentService attachmentService;
  @Mock private AttachmentRepository attachmentRepository;
  @Mock private UserRepository userRepository;
  @Mock private RateLimitService rateLimitService;

  @InjectMocks private MessageServiceImpl messageService;

  private static final Long USER_ID = 1L;
  private static final Long OTHER_USER_ID = 2L;
  private static final Long CONVERSATION_ID = 10L;
  private static final Long TARGET_CONV_ID = 20L;
  private static final String CONVERSATION_UUID = "conv-uuid";
  private static final String TARGET_CONV_UUID = "target-conv-uuid";
  private static final String MESSAGE_UUID = "msg-uuid";

  private Conversation buildConversation(Long id, String uuid) {
    Conversation c = new Conversation();
    c.setConversationId(id);
    c.setConversationUuid(uuid);
    c.setConversationType("PRIVATE");
    return c;
  }

  private Message buildMessage(Long messageId, Long conversationId, Long senderId) {
    Message m = new Message();
    m.setMessageId(messageId);
    m.setMessageUuid(MESSAGE_UUID);
    m.setConversationId(conversationId);
    m.setSenderId(senderId);
    m.setMessageType("TEXT");
    m.setContent("Hello world");
    m.setMessageStatus(MessageStatus.ACTIVE.getValue());
    m.setCreatedAt(OffsetDateTime.now());
    return m;
  }

  @Nested
  @DisplayName("Rate Limiting")
  class RateLimitTests {

    @Test
    @DisplayName("sendMessage: rate limited — throws 429")
    void sendMessage_RateLimited_Throws429() {
      when(rateLimitService.isChatRateLimited(USER_ID)).thenReturn(true);

      GeneralException ex = assertThrows(GeneralException.class,
          () -> messageService.sendMessage(USER_ID, null, CONVERSATION_UUID, "TEXT", "Hi", null, null));

      assertEquals(429, ex.getHttpCode());
      assertEquals("RATE_LIMITED", ex.getKey());
      verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("forwardMessage: rate limited — throws 429")
    void forwardMessage_RateLimited_Throws429() {
      when(rateLimitService.isChatRateLimited(USER_ID)).thenReturn(true);

      GeneralException ex = assertThrows(GeneralException.class,
          () -> messageService.forwardMessage(USER_ID, MESSAGE_UUID, TARGET_CONV_UUID));

      assertEquals(429, ex.getHttpCode());
      assertEquals("RATE_LIMITED", ex.getKey());
      verify(messageRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Message Forwarding")
  class ForwardTests {

    @Test
    @DisplayName("forwardMessage: valid participant in both — creates new message with forwardedFromId")
    void forwardMessage_ValidParticipant_CreatesNewMessage() {
      Message original = buildMessage(100L, CONVERSATION_ID, OTHER_USER_ID);
      when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(original));
      when(conversationService.isParticipant(CONVERSATION_ID, USER_ID)).thenReturn(true);

      Conversation target = buildConversation(TARGET_CONV_ID, TARGET_CONV_UUID);
      when(conversationService.findConversationByUuid(TARGET_CONV_UUID)).thenReturn(target);
      when(conversationService.isParticipant(TARGET_CONV_ID, USER_ID)).thenReturn(true);

      Message forwarded = buildMessage(200L, TARGET_CONV_ID, USER_ID);
      forwarded.setForwardedFromId(100L);
      when(messageRepository.save(any(Message.class))).thenReturn(forwarded);
      when(participantRepository.findAllByConversationId(TARGET_CONV_ID)).thenReturn(List.of());

      Message result = messageService.forwardMessage(USER_ID, MESSAGE_UUID, TARGET_CONV_UUID);

      assertNotNull(result);
      assertEquals(200L, result.getMessageId());
      assertEquals(100L, result.getForwardedFromId());

      ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
      verify(messageRepository).save(captor.capture());
      Message saved = captor.getValue();
      assertEquals(TARGET_CONV_ID, saved.getConversationId());
      assertEquals(USER_ID, saved.getSenderId());
      assertEquals("TEXT", saved.getMessageType());
      assertEquals("Hello world", saved.getContent());
      assertEquals(100L, saved.getForwardedFromId());
    }

    @Test
    @DisplayName("forwardMessage: not participant in source — throws 403")
    void forwardMessage_NotParticipantInSource_Throws403() {
      Message original = buildMessage(100L, CONVERSATION_ID, OTHER_USER_ID);
      when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(original));
      when(conversationService.isParticipant(CONVERSATION_ID, USER_ID)).thenReturn(false);

      GeneralException ex = assertThrows(GeneralException.class,
          () -> messageService.forwardMessage(USER_ID, MESSAGE_UUID, TARGET_CONV_UUID));

      assertEquals(403, ex.getHttpCode());
      verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("forwardMessage: not participant in target — throws 403")
    void forwardMessage_NotParticipantInTarget_Throws403() {
      Message original = buildMessage(100L, CONVERSATION_ID, OTHER_USER_ID);
      when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(original));
      when(conversationService.isParticipant(CONVERSATION_ID, USER_ID)).thenReturn(true);

      Conversation target = buildConversation(TARGET_CONV_ID, TARGET_CONV_UUID);
      when(conversationService.findConversationByUuid(TARGET_CONV_UUID)).thenReturn(target);
      when(conversationService.isParticipant(TARGET_CONV_ID, USER_ID)).thenReturn(false);

      GeneralException ex = assertThrows(GeneralException.class,
          () -> messageService.forwardMessage(USER_ID, MESSAGE_UUID, TARGET_CONV_UUID));

      assertEquals(403, ex.getHttpCode());
      verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("forwardMessage: copies content, type, and attachmentId from original")
    void forwardMessage_CopiesContentAndType() {
      Message original = buildMessage(100L, CONVERSATION_ID, OTHER_USER_ID);
      original.setMessageType("IMAGE");
      original.setContent("photo.jpg");
      original.setAttachmentId(5L);
      when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(original));
      when(conversationService.isParticipant(CONVERSATION_ID, USER_ID)).thenReturn(true);

      Conversation target = buildConversation(TARGET_CONV_ID, TARGET_CONV_UUID);
      when(conversationService.findConversationByUuid(TARGET_CONV_UUID)).thenReturn(target);
      when(conversationService.isParticipant(TARGET_CONV_ID, USER_ID)).thenReturn(true);

      Message forwarded = buildMessage(200L, TARGET_CONV_ID, USER_ID);
      when(messageRepository.save(any(Message.class))).thenReturn(forwarded);
      when(participantRepository.findAllByConversationId(TARGET_CONV_ID)).thenReturn(List.of());

      messageService.forwardMessage(USER_ID, MESSAGE_UUID, TARGET_CONV_UUID);

      ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
      verify(messageRepository).save(captor.capture());
      Message saved = captor.getValue();
      assertEquals("IMAGE", saved.getMessageType());
      assertEquals("photo.jpg", saved.getContent());
      assertEquals(5L, saved.getAttachmentId());
    }

    @Test
    @DisplayName("forwardMessage: updates denormalized fields on target conversation")
    void forwardMessage_UpdatesDenormalizedFields() {
      Message original = buildMessage(100L, CONVERSATION_ID, OTHER_USER_ID);
      when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(original));
      when(conversationService.isParticipant(CONVERSATION_ID, USER_ID)).thenReturn(true);

      Conversation target = buildConversation(TARGET_CONV_ID, TARGET_CONV_UUID);
      when(conversationService.findConversationByUuid(TARGET_CONV_UUID)).thenReturn(target);
      when(conversationService.isParticipant(TARGET_CONV_ID, USER_ID)).thenReturn(true);

      Message forwarded = buildMessage(200L, TARGET_CONV_ID, USER_ID);
      when(messageRepository.save(any(Message.class))).thenReturn(forwarded);
      when(participantRepository.findAllByConversationId(TARGET_CONV_ID)).thenReturn(List.of());

      messageService.forwardMessage(USER_ID, MESSAGE_UUID, TARGET_CONV_UUID);

      verify(participantRepository).incrementUnreadAndUpdateLastMessage(
          TARGET_CONV_ID, USER_ID, forwarded.getCreatedAt(), "Hello world", "TEXT");
      verify(participantRepository).updateSenderLastMessage(
          TARGET_CONV_ID, USER_ID, forwarded.getCreatedAt(), "Hello world", "TEXT");
      verify(participantRepository).autoUnarchive(TARGET_CONV_ID, USER_ID);
    }
  }
}
