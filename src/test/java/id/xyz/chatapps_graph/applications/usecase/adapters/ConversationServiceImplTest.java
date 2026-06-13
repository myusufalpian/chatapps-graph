package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

  @Mock private ConversationRepository conversationRepository;
  @Mock private ConversationParticipantRepository participantRepository;

  @InjectMocks private ConversationServiceImpl conversationService;

  private static final Long USER_A = 1L;
  private static final Long USER_B = 2L;
  private static final Long CONVERSATION_ID = 10L;

  @Test
  @DisplayName("findOrCreate: no existing conversation — creates new with two participants")
  void findOrCreate_NoExisting_CreatesNew() {
    when(participantRepository.findPrivateConversationBetween(USER_A, USER_B))
        .thenReturn(Optional.empty());

    Conversation saved = new Conversation();
    saved.setConversationId(CONVERSATION_ID);
    saved.setConversationType("PRIVATE");
    when(conversationRepository.save(any(Conversation.class))).thenReturn(saved);

    Conversation result = conversationService.findOrCreatePrivateConversation(USER_A, USER_B);

    assertEquals(CONVERSATION_ID, result.getConversationId());
    assertEquals("PRIVATE", result.getConversationType());

    ArgumentCaptor<ConversationParticipant> captor = ArgumentCaptor.forClass(ConversationParticipant.class);
    verify(participantRepository, times(2)).save(captor.capture());

    ConversationParticipant pA = captor.getAllValues().get(0);
    ConversationParticipant pB = captor.getAllValues().get(1);
    assertEquals(USER_A, pA.getUserId());
    assertEquals(USER_B, pB.getUserId());
    assertEquals(CONVERSATION_ID, pA.getConversationId());
    assertEquals(CONVERSATION_ID, pB.getConversationId());
    assertNotNull(pA.getJoinedAt());
    assertNotNull(pB.getJoinedAt());
  }

  @Test
  @DisplayName("findOrCreate: existing conversation — returns it without creating")
  void findOrCreate_Existing_ReturnsExisting() {
    when(participantRepository.findPrivateConversationBetween(USER_A, USER_B))
        .thenReturn(Optional.of(CONVERSATION_ID));

    Conversation existing = new Conversation();
    existing.setConversationId(CONVERSATION_ID);
    existing.setConversationType("PRIVATE");
    when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(existing));

    Conversation result = conversationService.findOrCreatePrivateConversation(USER_A, USER_B);

    assertEquals(CONVERSATION_ID, result.getConversationId());
    verify(conversationRepository, never()).save(any());
    verify(participantRepository, never()).save(any(ConversationParticipant.class));
  }

  @Test
  @DisplayName("isParticipant: user not a member — returns false")
  void isParticipant_NotMember_ReturnsFalse() {
    when(participantRepository.findByConversationIdAndUserId(CONVERSATION_ID, USER_A))
        .thenReturn(Optional.empty());

    boolean result = conversationService.isParticipant(CONVERSATION_ID, USER_A);

    assertFalse(result);
  }
}
