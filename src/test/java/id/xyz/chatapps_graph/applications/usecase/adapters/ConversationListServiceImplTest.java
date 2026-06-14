package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversationListServiceImplTest {

  @Mock private ConversationParticipantRepository participantRepository;
  @Mock private UserRepository userRepository;
  @Mock private ConversationService conversationService;

  @InjectMocks private ConversationListServiceImpl conversationListService;

  private static final Long USER_ID = 1L;
  private static final Long CONVERSATION_ID = 10L;
  private static final String CONVERSATION_UUID = "conv-uuid-123";

  private Conversation buildConversation() {
    Conversation c = new Conversation();
    c.setConversationId(CONVERSATION_ID);
    c.setConversationUuid(CONVERSATION_UUID);
    c.setConversationType("PRIVATE");
    return c;
  }

  private ConversationParticipant buildParticipant(boolean pinned, boolean archived, boolean muted) {
    return ConversationParticipant.builder()
        .participantId(1L)
        .conversationId(CONVERSATION_ID)
        .userId(USER_ID)
        .isPinned(pinned)
        .pinnedAt(pinned ? OffsetDateTime.now() : null)
        .isArchived(archived)
        .isMuted(muted)
        .unreadCount(0)
        .build();
  }

  // --- pin ---

  @Test
  @DisplayName("pin: success — sets isPinned=true and pinnedAt")
  void pin_Success_SetsFields() {
    Conversation conv = buildConversation();
    ConversationParticipant cp = buildParticipant(false, false, false);

    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conv);
    when(participantRepository.findByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
        .thenReturn(Optional.of(cp));
    when(participantRepository.countPinnedByUserIdForUpdate(USER_ID)).thenReturn(2);

    conversationListService.pinConversation(USER_ID, CONVERSATION_UUID);

    ArgumentCaptor<ConversationParticipant> captor = ArgumentCaptor.forClass(ConversationParticipant.class);
    verify(participantRepository).save(captor.capture());
    ConversationParticipant saved = captor.getValue();
    assertTrue(saved.getIsPinned());
    assertNotNull(saved.getPinnedAt());
  }

  @Test
  @DisplayName("pin: max 5 reached — throws 400")
  void pin_MaxReached_Throws400() {
    Conversation conv = buildConversation();
    ConversationParticipant cp = buildParticipant(false, false, false);

    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conv);
    when(participantRepository.findByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
        .thenReturn(Optional.of(cp));
    when(participantRepository.countPinnedByUserIdForUpdate(USER_ID)).thenReturn(5);

    GeneralException ex = assertThrows(GeneralException.class,
        () -> conversationListService.pinConversation(USER_ID, CONVERSATION_UUID));

    assertEquals(400, ex.getHttpCode());
    assertEquals("MAX_PINNED", ex.getKey());
  }

  // --- unpin ---

  @Test
  @DisplayName("unpin: success — clears isPinned and pinnedAt")
  void unpin_Success_ClearsFields() {
    Conversation conv = buildConversation();
    ConversationParticipant cp = buildParticipant(true, false, false);

    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conv);
    when(participantRepository.findByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
        .thenReturn(Optional.of(cp));

    conversationListService.unpinConversation(USER_ID, CONVERSATION_UUID);

    ArgumentCaptor<ConversationParticipant> captor = ArgumentCaptor.forClass(ConversationParticipant.class);
    verify(participantRepository).save(captor.capture());
    ConversationParticipant saved = captor.getValue();
    assertFalse(saved.getIsPinned());
    assertNull(saved.getPinnedAt());
  }

  // --- archive ---

  @Test
  @DisplayName("archive: success — sets isArchived=true")
  void archive_Success() {
    Conversation conv = buildConversation();
    ConversationParticipant cp = buildParticipant(false, false, false);

    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conv);
    when(participantRepository.findByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
        .thenReturn(Optional.of(cp));

    conversationListService.archiveConversation(USER_ID, CONVERSATION_UUID);

    ArgumentCaptor<ConversationParticipant> captor = ArgumentCaptor.forClass(ConversationParticipant.class);
    verify(participantRepository).save(captor.capture());
    assertTrue(captor.getValue().getIsArchived());
  }

  // --- mute ---

  @Test
  @DisplayName("mute: success — sets isMuted=true")
  void mute_Success() {
    Conversation conv = buildConversation();
    ConversationParticipant cp = buildParticipant(false, false, false);

    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conv);
    when(participantRepository.findByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
        .thenReturn(Optional.of(cp));

    conversationListService.muteConversation(USER_ID, CONVERSATION_UUID);

    ArgumentCaptor<ConversationParticipant> captor = ArgumentCaptor.forClass(ConversationParticipant.class);
    verify(participantRepository).save(captor.capture());
    assertTrue(captor.getValue().getIsMuted());
  }

  // --- resolveParticipant error ---

  @Test
  @DisplayName("pin: user not participant — throws 403")
  void pin_NotParticipant_Throws403() {
    Conversation conv = buildConversation();
    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conv);
    when(participantRepository.findByConversationIdAndUserId(CONVERSATION_ID, USER_ID))
        .thenReturn(Optional.empty());

    GeneralException ex = assertThrows(GeneralException.class,
        () -> conversationListService.pinConversation(USER_ID, CONVERSATION_UUID));

    assertEquals(403, ex.getHttpCode());
    assertEquals("FORBIDDEN", ex.getKey());
  }
}
