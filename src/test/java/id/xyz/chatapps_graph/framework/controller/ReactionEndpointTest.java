package id.xyz.chatapps_graph.framework.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.usecase.AttachmentService;
import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.applications.usecase.RateLimitService;
import id.xyz.chatapps_graph.applications.usecase.adapters.MessageServiceImpl;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.entity.MessageReaction;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReactionRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReceiptRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.MessageReactionResult;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReactionEndpointTest {

  @Mock private MessageRepository messageRepository;
  @Mock private MessageReceiptRepository receiptRepository;
  @Mock private ConversationParticipantRepository participantRepository;
  @Mock private ConversationService conversationService;
  @Mock private AttachmentService attachmentService;
  @Mock private AttachmentRepository attachmentRepository;
  @Mock private UserRepository userRepository;
  @Mock private RateLimitService rateLimitService;
  @Mock private MessageReactionRepository reactionRepository;

  @InjectMocks private MessageServiceImpl messageService;

  private static final Long USER_ID = 1L;
  private static final String USER_UUID = "user-123";
  private static final Long MESSAGE_ID = 100L;
  private static final String MESSAGE_UUID = "msg-123";
  private static final Long CONVERSATION_ID = 10L;
  private static final String CONVERSATION_UUID = "conv-123";

  private Message buildMessage() {
    Message m = new Message();
    m.setMessageId(MESSAGE_ID);
    m.setMessageUuid(MESSAGE_UUID);
    m.setConversationId(CONVERSATION_ID);
    m.setSenderId(2L);
    return m;
  }

  private void setupCommonMocks() {
    Message message = buildMessage();
    when(messageRepository.findByMessageUuid(MESSAGE_UUID)).thenReturn(Optional.of(message));
    when(conversationService.isParticipant(CONVERSATION_ID, USER_ID)).thenReturn(true);
    Conversation conv = Conversation.builder()
        .conversationId(CONVERSATION_ID)
        .conversationUuid(CONVERSATION_UUID)
        .build();
    when(conversationService.findConversationById(CONVERSATION_ID)).thenReturn(conv);
    User user = User.builder().userId(USER_ID).userUuid(USER_UUID).build();
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
  }

  @Test
  @DisplayName("addReaction: new reaction — creates with correct emoji")
  void addReaction_NewReaction_CreatesSuccessfully() {
    setupCommonMocks();
    when(reactionRepository.findByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(Optional.empty());

    MessageReactionResult result = messageService.addReaction(USER_ID, MESSAGE_UUID, "👍");

    ArgumentCaptor<MessageReaction> captor = ArgumentCaptor.forClass(MessageReaction.class);
    verify(reactionRepository).save(captor.capture());
    assertEquals("👍", captor.getValue().getEmoji());
    assertEquals(MESSAGE_ID, captor.getValue().getMessageId());
    assertEquals(USER_ID, captor.getValue().getUserId());

    assertEquals(CONVERSATION_UUID, result.conversationUuid());
    assertEquals(USER_UUID, result.userUuid());
    assertEquals("👍", result.emoji());
  }

  @Test
  @DisplayName("addReaction: existing reaction — updates emoji")
  void addReaction_ExistingReaction_UpdatesEmoji() {
    setupCommonMocks();
    MessageReaction existing = MessageReaction.builder()
        .reactionId(1L).messageId(MESSAGE_ID).userId(USER_ID).emoji("👍").build();
    when(reactionRepository.findByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(Optional.of(existing));

    MessageReactionResult result = messageService.addReaction(USER_ID, MESSAGE_UUID, "❤️");

    ArgumentCaptor<MessageReaction> captor = ArgumentCaptor.forClass(MessageReaction.class);
    verify(reactionRepository).save(captor.capture());
    assertEquals("❤️", captor.getValue().getEmoji());
    assertEquals(1L, captor.getValue().getReactionId());

    assertEquals(CONVERSATION_UUID, result.conversationUuid());
    assertEquals(USER_UUID, result.userUuid());
    assertEquals("❤️", result.emoji());
  }

  @Test
  @DisplayName("removeReaction: exists — deletes successfully")
  void removeReaction_Exists_DeletesSuccessfully() {
    setupCommonMocks();
    MessageReaction existing = MessageReaction.builder()
        .reactionId(1L).messageId(MESSAGE_ID).userId(USER_ID).emoji("👍").build();
    when(reactionRepository.findByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(Optional.of(existing));

    MessageReactionResult result = messageService.removeReaction(USER_ID, MESSAGE_UUID);

    verify(reactionRepository).delete(existing);
    assertEquals(CONVERSATION_UUID, result.conversationUuid());
    assertEquals(USER_UUID, result.userUuid());
  }

  @Test
  @DisplayName("removeReaction: not exists — no error, no delete")
  void removeReaction_NotExists_NoError() {
    setupCommonMocks();
    when(reactionRepository.findByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(Optional.empty());

    MessageReactionResult result = messageService.removeReaction(USER_ID, MESSAGE_UUID);

    verify(reactionRepository, never()).delete(any());
    assertEquals(CONVERSATION_UUID, result.conversationUuid());
    assertEquals(USER_UUID, result.userUuid());
  }

  @Test
  @DisplayName("addReaction: rate limited — throws 429")
  void addReaction_RateLimited_Throws429() {
    when(rateLimitService.isReactionRateLimited(USER_ID)).thenReturn(true);

    GeneralException ex = assertThrows(GeneralException.class,
        () -> messageService.addReaction(USER_ID, MESSAGE_UUID, "👍"));

    assertEquals(429, ex.getHttpCode());
    assertEquals("RATE_LIMITED", ex.getKey());
    verify(reactionRepository, never()).save(any());
  }
}
