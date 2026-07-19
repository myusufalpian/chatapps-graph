package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.domain.entity.User;

import id.xyz.chatapps_graph.applications.usecase.AttachmentService;
import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.applications.usecase.RateLimitService;
import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.entity.MessageReceipt;
import id.xyz.chatapps_graph.domain.enums.MessageStatus;
import id.xyz.chatapps_graph.domain.enums.ReceiptStatus;
import id.xyz.chatapps_graph.applications.usecase.LinkPreviewService;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.domain.repository.MessageEditHistoryRepository;
import id.xyz.chatapps_graph.infrastructure.config.properties.ChatEditProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReceiptRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

  @Mock private MessageRepository messageRepository;
  @Mock private MessageReceiptRepository receiptRepository;
  @Mock private ConversationParticipantRepository participantRepository;
  @Mock private ConversationService conversationService;
  @Mock private AttachmentService attachmentService;
  @Mock private AttachmentRepository attachmentRepository;
  @Mock private UserRepository userRepository;
  @Mock private RateLimitService rateLimitService;
  @Mock private id.xyz.chatapps_graph.domain.repository.MessageReactionRepository reactionRepository;
  @Mock private id.xyz.chatapps_graph.applications.usecase.PushNotificationService pushNotificationService;
  @Mock private MessageEditHistoryRepository editHistoryRepository;
  @Mock private ChatEditProperties chatEditProperties;
  @Mock private LinkPreviewService linkPreviewService;
  @Mock private RabbitTemplate rabbitTemplate;
  @Mock private PlatformTransactionManager transactionManager;
  @Mock private id.xyz.chatapps_graph.infrastructure.monitoring.MetricsFacade metricsFacade;

  @InjectMocks private MessageServiceImpl messageService;


  private static final Long SENDER_ID = 1L;
  private static final Long RECIPIENT_ID = 2L;
  private static final Long CONVERSATION_ID = 10L;
  private static final String CONVERSATION_UUID = "conv-uuid-123";
  private static final String MESSAGE_UUID = "msg-uuid-456";

  private Conversation buildConversation() {
    Conversation c = new Conversation();
    c.setConversationId(CONVERSATION_ID);
    c.setConversationUuid(CONVERSATION_UUID);
    c.setConversationType("PRIVATE");
    return c;
  }

  private Message buildMessage(Long senderId) {
    Message m = new Message();
    m.setMessageId(100L);
    m.setMessageUuid(MESSAGE_UUID);
    m.setConversationId(CONVERSATION_ID);
    m.setSenderId(senderId);
    m.setMessageType("TEXT");
    m.setContent("Hello");
    m.setMessageStatus(MessageStatus.ACTIVE.getValue());
    m.setCreatedAt(OffsetDateTime.now());
    return m;
  }

  // --- sendMessage ---

  @Test
  @DisplayName("sendTextMessage: saves message and creates receipts for other participants")
  void sendTextMessage_SavesAndCreatesReceipts() {
    Conversation conversation = buildConversation();
    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conversation);
    when(conversationService.isParticipant(CONVERSATION_ID, SENDER_ID)).thenReturn(true);

    Message savedMsg = buildMessage(SENDER_ID);
    when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

    ConversationParticipant pSender = new ConversationParticipant();
    pSender.setUserId(SENDER_ID);
    pSender.setConversationId(CONVERSATION_ID);
    ConversationParticipant pRecipient = new ConversationParticipant();
    pRecipient.setUserId(RECIPIENT_ID);
    pRecipient.setConversationId(CONVERSATION_ID);
    when(participantRepository.findAllByConversationId(CONVERSATION_ID))
        .thenReturn(List.of(pSender, pRecipient));

    User sender = User.builder().userId(SENDER_ID).userUuid("sender-uuid").build();
    when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));

    id.xyz.chatapps_graph.framework.dto.SendMessageResult result = messageService.sendMessage(SENDER_ID, null, CONVERSATION_UUID, "TEXT", "Hello", null, null);

    assertNotNull(result);
    assertEquals(100L, result.message().getMessageId());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<MessageReceipt>> captor = ArgumentCaptor.forClass(List.class);
    verify(receiptRepository).saveAll(captor.capture());
    MessageReceipt receipt = captor.getValue().getFirst();
    assertEquals(RECIPIENT_ID, receipt.getUserId());
    assertEquals(100L, receipt.getMessageId());
    assertEquals(ReceiptStatus.SENT.getValue(), receipt.getStatus());
    assertEquals(false, receipt.getIsDeletedForMe());
  }

  @Test
  @DisplayName("sendWithAttachment: DB fails — compensating tx deletes Minio file")
  void sendWithAttachment_DbFails_MinioFileDeleted() {
    Conversation conversation = buildConversation();
    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conversation);
    when(conversationService.isParticipant(CONVERSATION_ID, SENDER_ID)).thenReturn(true);

    Attachment att = new Attachment();
    att.setAttachmentId(5L);
    att.setFilePath("chat/user-uuid/12345_photo.jpg");
    when(attachmentRepository.findById(5L)).thenReturn(Optional.of(att));

    when(messageRepository.save(any(Message.class))).thenThrow(new RuntimeException("DB error"));

    assertThrows(RuntimeException.class,
        () -> messageService.sendMessage(SENDER_ID, null, CONVERSATION_UUID, "IMAGE", null, 5L, null));

    verify(attachmentService).deleteFile("chat/user-uuid/12345_photo.jpg");
  }

  @Test
  @DisplayName("nonParticipant: cannot send — throws 403")
  void nonParticipant_CannotSend_Throws403() {
    Conversation conversation = buildConversation();
    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conversation);
    when(conversationService.isParticipant(CONVERSATION_ID, SENDER_ID)).thenReturn(false);

    GeneralException ex = assertThrows(GeneralException.class,
        () -> messageService.sendMessage(SENDER_ID, null, CONVERSATION_UUID, "TEXT", "Hi", null, null));

    assertEquals(403, ex.getHttpCode());
    assertEquals("FORBIDDEN", ex.getKey());
    verify(messageRepository, never()).save(any());
  }

  // --- listMessages ---

  @Test
  @DisplayName("listMessages: cursor pagination — calls findMessagesAfterCursor")
  void listMessages_CursorPagination_CorrectOrder() {
    OffsetDateTime ts = OffsetDateTime.parse("2026-01-01T10:00:00+07:00");
    String cursor = ts.toString() + "_50";

    Message m1 = buildMessage(SENDER_ID);
    when(messageRepository.findMessagesAfterCursor(eq(CONVERSATION_ID), eq(SENDER_ID), eq(ts), eq(50L), eq(20)))
        .thenReturn(List.of(m1));

    List<Message> result = messageService.listMessages(CONVERSATION_ID, SENDER_ID, cursor, 20);

    assertEquals(1, result.size());
    verify(messageRepository).findMessagesAfterCursor(CONVERSATION_ID, SENDER_ID, ts, 50L, 20);
    verify(messageRepository, never()).findFirstMessages(any(), any(), anyInt());
  }

  @Test
  @DisplayName("listMessages: no cursor — calls findFirstMessages (excludes deleted for me)")
  void listMessages_ExcludesDeletedForMe() {
    Message m1 = buildMessage(SENDER_ID);
    when(messageRepository.findFirstMessages(CONVERSATION_ID, SENDER_ID, 20))
        .thenReturn(List.of(m1));

    List<Message> result = messageService.listMessages(CONVERSATION_ID, SENDER_ID, null, 20);

    assertEquals(1, result.size());
    verify(messageRepository).findFirstMessages(CONVERSATION_ID, SENDER_ID, 20);
  }

  // --- deleteMessage ---

  @Test
  @DisplayName("deleteForAll: sender can delete — sets status to DELETED")
  void deleteForAll_OnlySender_CanDelete() {
    Message message = buildMessage(SENDER_ID);
    when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(message));

    messageService.deleteMessage(MESSAGE_UUID, SENDER_ID, "FOR_ALL");

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(messageRepository).saveAndFlush(captor.capture());
    assertEquals(MessageStatus.DELETED.getValue(), captor.getValue().getMessageStatus());
  }

  @Test
  @DisplayName("deleteForAll: non-sender — throws 403")
  void deleteForAll_NonSender_Throws403() {
    Message message = buildMessage(SENDER_ID);
    when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(message));

    GeneralException ex = assertThrows(GeneralException.class,
        () -> messageService.deleteMessage(MESSAGE_UUID, 999L, "FOR_ALL"));

    assertEquals(403, ex.getHttpCode());
    assertEquals("FORBIDDEN", ex.getKey());
    verify(messageRepository, never()).save(any());
  }

  @Test
  @DisplayName("deleteForMe: sets receipt isDeletedForMe to true")
  void deleteForMe_SetsReceiptFlag() {
    Message message = buildMessage(SENDER_ID);
    when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(message));

    MessageReceipt existing = new MessageReceipt();
    existing.setReceiptId(1L);
    existing.setMessageId(100L);
    existing.setUserId(RECIPIENT_ID);
    existing.setStatus(ReceiptStatus.SENT.getValue());
    existing.setIsDeletedForMe(false);
    when(receiptRepository.findByMessageIdAndUserId(100L, RECIPIENT_ID)).thenReturn(Optional.of(existing));

    messageService.deleteMessage(MESSAGE_UUID, RECIPIENT_ID, "FOR_ME");

    ArgumentCaptor<MessageReceipt> captor = ArgumentCaptor.forClass(MessageReceipt.class);
    verify(receiptRepository).save(captor.capture());
    assertTrue(captor.getValue().getIsDeletedForMe());
  }

  // --- markAsRead ---

  @Test
  @DisplayName("markAsRead: updates receipts via repository")
  void markAsRead_UpdatesReceipts() {
    Conversation conversation = buildConversation();
    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conversation);
    when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(
        id.xyz.chatapps_graph.domain.entity.User.builder().userId(SENDER_ID).hideReadReceipt(false).build()));
    when(receiptRepository.markAsReadByConversation(CONVERSATION_ID, SENDER_ID, ReceiptStatus.READ.getValue()))
        .thenReturn(3);

    messageService.markAsRead(CONVERSATION_UUID, SENDER_ID);

    verify(receiptRepository).markAsReadByConversation(CONVERSATION_ID, SENDER_ID, ReceiptStatus.READ.getValue());
  }

  @Test
  @DisplayName("markAsDelivered: updates receipt rows and returns target user phones")
  void markAsDelivered_UpdatesReceipts() {
    Conversation conversation = buildConversation();
    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conversation);
    when(conversationService.isParticipant(CONVERSATION_ID, RECIPIENT_ID)).thenReturn(true);
    when(receiptRepository.findUndeliveredMessageSenderIds(CONVERSATION_ID, RECIPIENT_ID,
        List.of("msg-1", "msg-2"), ReceiptStatus.SENT.getValue())).thenReturn(List.of(7L));
    when(receiptRepository.markAsDeliveredByConversation(CONVERSATION_ID, RECIPIENT_ID,
        List.of("msg-1", "msg-2"), ReceiptStatus.SENT.getValue(), ReceiptStatus.DELIVERED.getValue()))
        .thenReturn(2);

    User sender = User.builder().userId(7L).userPhone("sender-phone").build();
    when(userRepository.findAllById(List.of(7L))).thenReturn(List.of(sender));
    User reader = User.builder().userId(RECIPIENT_ID).userUuid("reader-uuid").build();
    when(userRepository.findById(RECIPIENT_ID)).thenReturn(Optional.of(reader));

    var result = messageService.markAsDelivered(CONVERSATION_UUID, RECIPIENT_ID, List.of("msg-1", "msg-2"));

    assertTrue(result.receiptsUpdated());
    assertEquals(List.of("sender-phone"), result.targetUserPhones());
    assertEquals("reader-uuid", result.readerUuid());
    verify(receiptRepository).markAsDeliveredByConversation(CONVERSATION_ID, RECIPIENT_ID,
        List.of("msg-1", "msg-2"), ReceiptStatus.SENT.getValue(), ReceiptStatus.DELIVERED.getValue());
  }

  @Test
  @DisplayName("markAsDelivered: empty payload returns hidden no-op")
  void markAsDelivered_EmptyPayload_ReturnsHidden() {
    var result = messageService.markAsDelivered(CONVERSATION_UUID, RECIPIENT_ID, List.of());

    assertFalse(result.receiptsUpdated());
    assertEquals(List.of(), result.targetUserPhones());
    assertNull(result.readerUuid());
  }
}
