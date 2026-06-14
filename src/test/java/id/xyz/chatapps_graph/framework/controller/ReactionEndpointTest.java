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
import id.xyz.chatapps_graph.domain.entity.MessageReaction;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReactionRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReceiptRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
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
  private static final Long MESSAGE_ID = 100L;

  @Test
  @DisplayName("addReaction: new reaction — creates with correct emoji")
  void addReaction_NewReaction_CreatesSuccessfully() {
    when(reactionRepository.findByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(Optional.empty());

    messageService.addReaction(USER_ID, MESSAGE_ID, "👍");

    ArgumentCaptor<MessageReaction> captor = ArgumentCaptor.forClass(MessageReaction.class);
    verify(reactionRepository).save(captor.capture());
    assertEquals("👍", captor.getValue().getEmoji());
    assertEquals(MESSAGE_ID, captor.getValue().getMessageId());
    assertEquals(USER_ID, captor.getValue().getUserId());
  }

  @Test
  @DisplayName("addReaction: existing reaction — updates emoji")
  void addReaction_ExistingReaction_UpdatesEmoji() {
    MessageReaction existing = MessageReaction.builder()
        .reactionId(1L).messageId(MESSAGE_ID).userId(USER_ID).emoji("👍").build();
    when(reactionRepository.findByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(Optional.of(existing));

    messageService.addReaction(USER_ID, MESSAGE_ID, "❤️");

    ArgumentCaptor<MessageReaction> captor = ArgumentCaptor.forClass(MessageReaction.class);
    verify(reactionRepository).save(captor.capture());
    assertEquals("❤️", captor.getValue().getEmoji());
    assertEquals(1L, captor.getValue().getReactionId());
  }

  @Test
  @DisplayName("removeReaction: exists — deletes successfully")
  void removeReaction_Exists_DeletesSuccessfully() {
    MessageReaction existing = MessageReaction.builder()
        .reactionId(1L).messageId(MESSAGE_ID).userId(USER_ID).emoji("👍").build();
    when(reactionRepository.findByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(Optional.of(existing));

    messageService.removeReaction(USER_ID, MESSAGE_ID);

    verify(reactionRepository).delete(existing);
  }

  @Test
  @DisplayName("removeReaction: not exists — no error, no delete")
  void removeReaction_NotExists_NoError() {
    when(reactionRepository.findByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(Optional.empty());

    messageService.removeReaction(USER_ID, MESSAGE_ID);

    verify(reactionRepository, never()).delete(any());
  }

  @Test
  @DisplayName("addReaction: rate limited — throws 429")
  void addReaction_RateLimited_Throws429() {
    when(rateLimitService.isReactionRateLimited(USER_ID)).thenReturn(true);

    GeneralException ex = assertThrows(GeneralException.class,
        () -> messageService.addReaction(USER_ID, MESSAGE_ID, "👍"));

    assertEquals(429, ex.getHttpCode());
    assertEquals("RATE_LIMITED", ex.getKey());
    verify(reactionRepository, never()).save(any());
  }
}
