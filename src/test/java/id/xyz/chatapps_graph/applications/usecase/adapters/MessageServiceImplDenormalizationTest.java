package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.usecase.AttachmentService;
import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.enums.MessageStatus;
import id.xyz.chatapps_graph.domain.enums.ReceiptStatus;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReceiptRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplDenormalizationTest {

  @Mock private MessageRepository messageRepository;
  @Mock private MessageReceiptRepository receiptRepository;
  @Mock private ConversationParticipantRepository participantRepository;
  @Mock private ConversationService conversationService;
  @Mock private AttachmentService attachmentService;
  @Mock private AttachmentRepository attachmentRepository;
  @Mock private UserRepository userRepository;

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

  private Message buildSavedMessage() {
    Message m = new Message();
    m.setMessageId(100L);
    m.setMessageUuid(MESSAGE_UUID);
    m.setConversationId(CONVERSATION_ID);
    m.setSenderId(SENDER_ID);
    m.setMessageType("TEXT");
    m.setContent("Hello world");
    m.setMessageStatus(MessageStatus.ACTIVE.getValue());
    m.setCreatedAt(OffsetDateTime.now());
    return m;
  }

  // --- sendMessage denormalization ---

  @Test
  @DisplayName("sendMessage: updates lastMessageAt and preview for other participants")
  void sendMessage_UpdatesLastMessageAtAndPreview() {
    Conversation conv = buildConversation();
    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conv);
    when(conversationService.isParticipant(CONVERSATION_ID, SENDER_ID)).thenReturn(true);

    Message saved = buildSavedMessage();
    when(messageRepository.save(any(Message.class))).thenReturn(saved);

    ConversationParticipant pSender = ConversationParticipant.builder().userId(SENDER_ID).conversationId(CONVERSATION_ID).build();
    ConversationParticipant pRecipient = ConversationParticipant.builder().userId(RECIPIENT_ID).conversationId(CONVERSATION_ID).build();
    when(participantRepository.findAllByConversationId(CONVERSATION_ID)).thenReturn(List.of(pSender, pRecipient));

    messageService.sendMessage(SENDER_ID, null, CONVERSATION_UUID, "TEXT", "Hello world", null, null);

    verify(participantRepository).incrementUnreadAndUpdateLastMessage(
        eq(CONVERSATION_ID), eq(SENDER_ID), eq(saved.getCreatedAt()), eq("Hello world"), eq("TEXT"));
    verify(participantRepository).updateSenderLastMessage(
        eq(CONVERSATION_ID), eq(SENDER_ID), eq(saved.getCreatedAt()), eq("Hello world"), eq("TEXT"));
  }

  @Test
  @DisplayName("sendMessage: increments unread count for other participants (not sender)")
  void sendMessage_IncrementsUnreadForOtherParticipants() {
    Conversation conv = buildConversation();
    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conv);
    when(conversationService.isParticipant(CONVERSATION_ID, SENDER_ID)).thenReturn(true);

    Message saved = buildSavedMessage();
    when(messageRepository.save(any(Message.class))).thenReturn(saved);

    ConversationParticipant pSender = ConversationParticipant.builder().userId(SENDER_ID).conversationId(CONVERSATION_ID).build();
    ConversationParticipant pRecipient = ConversationParticipant.builder().userId(RECIPIENT_ID).conversationId(CONVERSATION_ID).build();
    when(participantRepository.findAllByConversationId(CONVERSATION_ID)).thenReturn(List.of(pSender, pRecipient));

    messageService.sendMessage(SENDER_ID, null, CONVERSATION_UUID, "TEXT", "Hello world", null, null);

    // incrementUnreadAndUpdateLastMessage excludes sender by SQL, so verify call with senderId param
    verify(participantRepository).incrementUnreadAndUpdateLastMessage(
        eq(CONVERSATION_ID), eq(SENDER_ID), any(OffsetDateTime.class), eq("Hello world"), eq("TEXT"));
  }

  @Test
  @DisplayName("sendMessage: auto-unarchives conversation for non-sender participants")
  void sendMessage_ArchivedConversation_AutoUnarchives() {
    Conversation conv = buildConversation();
    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conv);
    when(conversationService.isParticipant(CONVERSATION_ID, SENDER_ID)).thenReturn(true);

    Message saved = buildSavedMessage();
    when(messageRepository.save(any(Message.class))).thenReturn(saved);

    ConversationParticipant pSender = ConversationParticipant.builder().userId(SENDER_ID).conversationId(CONVERSATION_ID).build();
    ConversationParticipant pRecipient = ConversationParticipant.builder().userId(RECIPIENT_ID).conversationId(CONVERSATION_ID).build();
    when(participantRepository.findAllByConversationId(CONVERSATION_ID)).thenReturn(List.of(pSender, pRecipient));

    messageService.sendMessage(SENDER_ID, null, CONVERSATION_UUID, "TEXT", "Hello world", null, null);

    verify(participantRepository).autoUnarchive(CONVERSATION_ID, SENDER_ID);
  }

  // --- markAsRead denormalization ---

  @Test
  @DisplayName("markAsRead: resets unread count to zero")
  void markAsRead_ResetsUnreadToZero() {
    Conversation conv = buildConversation();
    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conv);
    when(receiptRepository.markAsReadByConversation(CONVERSATION_ID, SENDER_ID, ReceiptStatus.READ.getValue()))
        .thenReturn(3);

    messageService.markAsRead(CONVERSATION_UUID, SENDER_ID);

    verify(participantRepository).resetUnreadCount(CONVERSATION_ID, SENDER_ID);
  }

  // --- deleteForAll denormalization ---

  @Test
  @DisplayName("deleteForAll: last message with no remaining active — updates preview to 'Pesan dihapus'")
  void deleteForAll_LastMessage_UpdatesPreviewToDeleted() {
    Message message = buildSavedMessage();
    when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(message));
    when(messageRepository.saveAndFlush(message)).thenReturn(message);
    // No remaining active messages after deletion
    when(messageRepository.findLatestActiveMessageId(CONVERSATION_ID)).thenReturn(Optional.empty());

    messageService.deleteMessage(MESSAGE_UUID, SENDER_ID, "FOR_ALL");

    verify(participantRepository).updateLastMessagePreviewForAll(CONVERSATION_ID, "Pesan dihapus");
  }
}
