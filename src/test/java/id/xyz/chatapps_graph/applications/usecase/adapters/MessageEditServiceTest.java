package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.usecase.AttachmentService;
import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.applications.usecase.PushNotificationService;
import id.xyz.chatapps_graph.applications.usecase.RateLimitService;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.entity.MessageEditHistory;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.MessageStatus;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.MessageEditHistoryRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReactionRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReceiptRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.config.properties.ChatEditProperties;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageEditServiceTest {

  @Mock private MessageRepository messageRepository;
  @Mock private MessageReceiptRepository receiptRepository;
  @Mock private ConversationParticipantRepository participantRepository;
  @Mock private ConversationService conversationService;
  @Mock private AttachmentService attachmentService;
  @Mock private AttachmentRepository attachmentRepository;
  @Mock private UserRepository userRepository;
  @Mock private RateLimitService rateLimitService;
  @Mock private MessageReactionRepository reactionRepository;
  @Mock private PushNotificationService pushNotificationService;
  @Mock private MessageEditHistoryRepository editHistoryRepository;
  @Mock private ChatEditProperties chatEditProperties;

  @InjectMocks private MessageServiceImpl messageService;

  private static final Long USER_ID = 1L;
  private static final Long OTHER_USER_ID = 2L;
  private static final String MESSAGE_UUID = "msg-uuid-edit-test";

  private Message buildActiveMessage(Long senderId, OffsetDateTime createdAt) {
    Message m = new Message();
    m.setMessageId(100L);
    m.setMessageUuid(MESSAGE_UUID);
    m.setConversationId(10L);
    m.setSenderId(senderId);
    m.setMessageType("TEXT");
    m.setContent("Original content");
    m.setMessageStatus(MessageStatus.ACTIVE.getValue());
    m.setCreatedAt(createdAt);
    return m;
  }

  @Nested
  @DisplayName("editMessage — Happy Path")
  class EditMessageHappyPath {

    @Test
    @DisplayName("valid edit within window: updates content and sets editedAt")
    void editMessage_ValidWithinWindow_UpdatesContent() {
      Message message = buildActiveMessage(USER_ID, OffsetDateTime.now().minusMinutes(5));
      when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(message));
      when(chatEditProperties.getMaxWindowMinutes()).thenReturn(30);
      when(editHistoryRepository.save(any(MessageEditHistory.class)))
          .thenAnswer(inv -> inv.getArgument(0));
      when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

      Message result = messageService.editMessage(USER_ID, MESSAGE_UUID, "Edited content").message();

      assertEquals("Edited content", result.getContent());
      assertNotNull(result.getEditedAt());
    }

    @Test
    @DisplayName("edit saves original content to history before overwrite")
    void editMessage_SavesOriginalToHistory() {
      Message message = buildActiveMessage(USER_ID, OffsetDateTime.now().minusMinutes(10));
      when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(message));
      when(chatEditProperties.getMaxWindowMinutes()).thenReturn(30);
      when(editHistoryRepository.save(any(MessageEditHistory.class)))
          .thenAnswer(inv -> inv.getArgument(0));
      when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

      messageService.editMessage(USER_ID, MESSAGE_UUID, "New text");

      ArgumentCaptor<MessageEditHistory> captor = ArgumentCaptor.forClass(MessageEditHistory.class);
      verify(editHistoryRepository).save(captor.capture());
      MessageEditHistory history = captor.getValue();
      assertEquals(100L, history.getMessageId());
      assertEquals("Original content", history.getOriginalContent());
      assertNotNull(history.getEditedAt());
    }

    @Test
    @DisplayName("edit sets editedAt with consistent timestamp")
    void editMessage_SetsEditedAt() {
      Message message = buildActiveMessage(USER_ID, OffsetDateTime.now().minusMinutes(1));
      when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(message));
      when(chatEditProperties.getMaxWindowMinutes()).thenReturn(30);
      when(editHistoryRepository.save(any(MessageEditHistory.class)))
          .thenAnswer(inv -> inv.getArgument(0));
      when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

      Message result = messageService.editMessage(USER_ID, MESSAGE_UUID, "Edited").message();

      assertNotNull(result.getEditedAt());
      // editedAt should be same timestamp as history's editedAt
      ArgumentCaptor<MessageEditHistory> histCaptor = ArgumentCaptor.forClass(MessageEditHistory.class);
      verify(editHistoryRepository).save(histCaptor.capture());
      assertEquals(result.getEditedAt(), histCaptor.getValue().getEditedAt());
    }

    @Test
    @DisplayName("multiple edits: all history entries are preserved")
    void editMessage_MultipleEdits_AllHistoryPreserved() {
      Message message = buildActiveMessage(USER_ID, OffsetDateTime.now().minusMinutes(2));
      when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(message));
      when(chatEditProperties.getMaxWindowMinutes()).thenReturn(30);
      when(editHistoryRepository.save(any(MessageEditHistory.class)))
          .thenAnswer(inv -> inv.getArgument(0));
      when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

      // First edit
      messageService.editMessage(USER_ID, MESSAGE_UUID, "Edit 1");

      // Simulate second edit (content now = "Edit 1")
      message.setContent("Edit 1");
      messageService.editMessage(USER_ID, MESSAGE_UUID, "Edit 2");

      // History saved twice
      ArgumentCaptor<MessageEditHistory> captor = ArgumentCaptor.forClass(MessageEditHistory.class);
      verify(editHistoryRepository, org.mockito.Mockito.times(2)).save(captor.capture());
      assertEquals("Original content", captor.getAllValues().get(0).getOriginalContent());
      assertEquals("Edit 1", captor.getAllValues().get(1).getOriginalContent());
    }

    @Test
    @DisplayName("unchanged content: returns no-op without history or persistence write")
    void editMessage_UnchangedContent_ReturnsNoOp() {
      Message message = buildActiveMessage(USER_ID, OffsetDateTime.now().minusMinutes(2));
      when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(message));
      when(chatEditProperties.getMaxWindowMinutes()).thenReturn(30);

      var result = messageService.editMessage(USER_ID, MESSAGE_UUID, "Original content");

      assertFalse(result.changed());
      verify(editHistoryRepository, never()).save(any());
      verify(messageRepository, never()).save(any(Message.class));
    }
  }

  @Nested
  @DisplayName("editMessage — Error Paths")
  class EditMessageErrorPaths {

    @Test
    @DisplayName("message not found: throws 404")
    void editMessage_NotFound_Throws404() {
      when(messageRepository.findByMessageUuid("nonexistent")).thenReturn(Optional.empty());

      GeneralException ex = assertThrows(GeneralException.class,
          () -> messageService.editMessage(USER_ID, "nonexistent", "New content"));

      assertEquals(404, ex.getHttpCode());
      assertEquals("MESSAGE_NOT_FOUND", ex.getKey());
    }

    @Test
    @DisplayName("not sender: throws 403 NOT_SENDER")
    void editMessage_NotSender_Throws403() {
      Message message = buildActiveMessage(OTHER_USER_ID, OffsetDateTime.now().minusMinutes(5));
      when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(message));

      GeneralException ex = assertThrows(GeneralException.class,
          () -> messageService.editMessage(USER_ID, MESSAGE_UUID, "Hack attempt"));

      assertEquals(403, ex.getHttpCode());
      assertEquals("NOT_SENDER", ex.getKey());
      verify(editHistoryRepository, never()).save(any());
      verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleted message: throws 400 MESSAGE_DELETED")
    void editMessage_DeletedMessage_Throws400() {
      Message message = buildActiveMessage(USER_ID, OffsetDateTime.now().minusMinutes(5));
      message.setMessageStatus(MessageStatus.DELETED.getValue());
      when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(message));

      GeneralException ex = assertThrows(GeneralException.class,
          () -> messageService.editMessage(USER_ID, MESSAGE_UUID, "Edit deleted"));

      assertEquals(400, ex.getHttpCode());
      assertEquals("MESSAGE_DELETED", ex.getKey());
      verify(editHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("window expired (31 min): throws 400 EDIT_WINDOW_EXPIRED")
    void editMessage_WindowExpired_Throws400() {
      Message message = buildActiveMessage(USER_ID, OffsetDateTime.now().minusMinutes(31));
      when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(message));
      when(chatEditProperties.getMaxWindowMinutes()).thenReturn(30);

      GeneralException ex = assertThrows(GeneralException.class,
          () -> messageService.editMessage(USER_ID, MESSAGE_UUID, "Too late"));

      assertEquals(400, ex.getHttpCode());
      assertEquals("EDIT_WINDOW_EXPIRED", ex.getKey());
      verify(editHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("edit at exact 30 min boundary: still allowed (createdAt + 30 > now)")
    void editMessage_ExactBoundary_StillAllowed() {
      // Created exactly 29 minutes 59 seconds ago — should still pass
      Message message = buildActiveMessage(USER_ID, OffsetDateTime.now().minusMinutes(29).minusSeconds(59));
      when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(message));
      when(chatEditProperties.getMaxWindowMinutes()).thenReturn(30);
      when(editHistoryRepository.save(any(MessageEditHistory.class)))
          .thenAnswer(inv -> inv.getArgument(0));
      when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

      Message result = messageService.editMessage(USER_ID, MESSAGE_UUID, "Just in time").message();

      assertEquals("Just in time", result.getContent());
    }
  }

  @Nested
  @DisplayName("markAsRead — Read Receipt Privacy")
  class MarkAsReadPrivacy {

    @BeforeEach
    void setup() {
      when(conversationService.findConversationByUuid("conv-uuid"))
          .thenReturn(buildConversation());
    }

    private id.xyz.chatapps_graph.domain.entity.Conversation buildConversation() {
      var c = new id.xyz.chatapps_graph.domain.entity.Conversation();
      c.setConversationId(10L);
      c.setConversationUuid("conv-uuid");
      return c;
    }

    @Test
    @DisplayName("hideReadReceipt=false: updates receipt to READ and returns true")
    void markAsRead_HideFalse_NormalBehavior() {
      User reader = User.builder().userId(USER_ID).hideReadReceipt(false).build();
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(reader));
      when(receiptRepository.findUnreadMessageSenderIds(10L, USER_ID, 2)).thenReturn(java.util.List.of(7L));
      when(receiptRepository.markAsReadByConversation(10L, USER_ID, 2)).thenReturn(1);

      var result = messageService.markAsRead("conv-uuid", USER_ID);

      assertTrue(result.receiptsUpdated());
      assertEquals(java.util.List.of(7L), result.senderIds());
      verify(receiptRepository).markAsReadByConversation(eq(10L), eq(USER_ID), eq(2));
      verify(participantRepository).resetUnreadCount(10L, USER_ID);
    }

    @Test
    @DisplayName("hideReadReceipt=true: does NOT update receipt but resets unread")
    void markAsRead_HideTrue_ReceiptNotUpdatedToRead() {
      User reader = User.builder().userId(USER_ID).hideReadReceipt(true).build();
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(reader));

      boolean result = messageService.markAsRead("conv-uuid", USER_ID).receiptsUpdated();

      assertFalse(result);
      verify(receiptRepository, never()).markAsReadByConversation(
          eq(10L), eq(USER_ID), eq(2));
      verify(participantRepository).resetUnreadCount(10L, USER_ID);
    }

    @Test
    @DisplayName("hideReadReceipt=true: unread count still resets (UX badge disappears)")
    void markAsRead_HideTrue_UnreadCountStillResets() {
      User reader = User.builder().userId(USER_ID).hideReadReceipt(true).build();
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(reader));

      messageService.markAsRead("conv-uuid", USER_ID);

      verify(participantRepository).resetUnreadCount(10L, USER_ID);
    }

    @Test
    @DisplayName("user not found (null): does NOT broadcast and does NOT update receipt")
    void markAsRead_UserNull_DefaultsBroadcast() {
      when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

      boolean result = messageService.markAsRead("conv-uuid", USER_ID).receiptsUpdated();

      assertFalse(result);
      verify(receiptRepository, never()).markAsReadByConversation(
          eq(10L), eq(USER_ID), eq(2));
      verify(participantRepository).resetUnreadCount(10L, USER_ID);
    }
  }
}
