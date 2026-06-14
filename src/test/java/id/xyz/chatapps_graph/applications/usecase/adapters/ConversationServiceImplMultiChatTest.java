package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplMultiChatTest {

  @Mock private ConversationRepository conversationRepository;
  @Mock private ConversationParticipantRepository participantRepository;

  @InjectMocks private ConversationServiceImpl conversationService;

  private static final Long CREATOR_ID = 1L;

  @Test
  @DisplayName("createMultiChat: min 3 participants — succeeds and creates conversation + participants")
  void createMultiChat_MinThreeParticipants_Succeeds() {
    List<Long> participantIds = List.of(CREATOR_ID, 2L, 3L);

    Conversation saved = new Conversation();
    saved.setConversationId(10L);
    saved.setConversationType("MULTI_CHAT");
    when(conversationRepository.save(any(Conversation.class))).thenReturn(saved);

    Conversation result = conversationService.createMultiChat(CREATOR_ID, participantIds);

    assertNotNull(result);
    assertEquals("MULTI_CHAT", result.getConversationType());
    verify(conversationRepository).save(any(Conversation.class));
    verify(participantRepository, times(3)).save(any(ConversationParticipant.class));
  }

  @Test
  @DisplayName("createMultiChat: less than 3 participants — throws 400")
  void createMultiChat_LessThanThree_Throws400() {
    List<Long> participantIds = List.of(CREATOR_ID, 2L);

    GeneralException ex = assertThrows(GeneralException.class,
        () -> conversationService.createMultiChat(CREATOR_ID, participantIds));

    assertEquals(400, ex.getHttpCode());
    assertEquals("MIN_PARTICIPANTS", ex.getKey());
    verify(conversationRepository, never()).save(any());
    verify(participantRepository, never()).save(any(ConversationParticipant.class));
  }
}
