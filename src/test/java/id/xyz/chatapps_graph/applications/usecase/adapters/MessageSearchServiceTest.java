package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.usecase.AttachmentService;
import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.applications.usecase.RateLimitService;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.enums.MessageStatus;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReactionRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReceiptRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageSearchServiceTest {

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
  private static final Long CONVERSATION_ID = 10L;
  private static final String CONVERSATION_UUID = "conv-uuid";

  private Message buildMessage(Long id) {
    Message m = new Message();
    m.setMessageId(id);
    m.setMessageUuid("msg-" + id);
    m.setConversationId(CONVERSATION_ID);
    m.setSenderId(2L);
    m.setMessageType("TEXT");
    m.setContent("Hello search");
    m.setMessageStatus(MessageStatus.ACTIVE.getValue());
    m.setCreatedAt(OffsetDateTime.now());
    return m;
  }

  @Test
  @DisplayName("searchMessages: empty query — throws 400")
  void searchMessages_EmptyQuery_Throws400() {
    GeneralException ex = assertThrows(GeneralException.class,
        () -> messageService.searchMessages(USER_ID, "", null, null, 20));
    assertEquals(400, ex.getHttpCode());
    assertEquals("INVALID_QUERY", ex.getKey());
  }

  @Test
  @DisplayName("searchMessages: valid query without cursor — returns results from repository")
  void searchMessages_ValidQuery_ReturnsResults() {
    List<Message> expected = List.of(buildMessage(1L), buildMessage(2L));
    when(messageRepository.searchMessages(USER_ID, "hello", 21)).thenReturn(expected);

    List<Message> result = messageService.searchMessages(USER_ID, "hello", null, null, 20);

    assertEquals(2, result.size());
    verify(messageRepository).searchMessages(USER_ID, "hello", 21);
  }

  @Test
  @DisplayName("searchMessages: no results — returns empty list")
  void searchMessages_NoResults_ReturnsEmptyList() {
    when(messageRepository.searchMessages(USER_ID, "nonexistent", 21)).thenReturn(List.of());

    List<Message> result = messageService.searchMessages(USER_ID, "nonexistent", null, null, 20);

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("searchMessages: with cursor — calls cursor variant")
  void searchMessages_CursorPagination_CorrectPage() {
    OffsetDateTime ts = OffsetDateTime.parse("2026-06-14T10:00:00+07:00");
    String cursor = ts.toString() + "_50";
    List<Message> expected = List.of(buildMessage(3L));
    when(messageRepository.searchMessagesWithCursor(USER_ID, "hello", ts, 50L, 21)).thenReturn(expected);

    List<Message> result = messageService.searchMessages(USER_ID, "hello", null, cursor, 20);

    assertEquals(1, result.size());
    verify(messageRepository).searchMessagesWithCursor(USER_ID, "hello", ts, 50L, 21);
  }

  @Test
  @DisplayName("searchMessages: limit exceeds 50 — capped at 51 (50+1 for hasMore)")
  void searchMessages_LimitExceeds50_CappedAt50() {
    when(messageRepository.searchMessages(USER_ID, "test", 51)).thenReturn(List.of());

    messageService.searchMessages(USER_ID, "test", null, null, 100);

    verify(messageRepository).searchMessages(USER_ID, "test", 51);
  }

  @Test
  @DisplayName("searchMessages: with conversationUuid — scoped to that conversation")
  void searchMessages_FilterByConversation_ScopedCorrectly() {
    Conversation conv = new Conversation();
    conv.setConversationId(CONVERSATION_ID);
    conv.setConversationUuid(CONVERSATION_UUID);
    when(conversationService.findConversationByUuid(CONVERSATION_UUID)).thenReturn(conv);
    when(messageRepository.searchMessagesInConversation(USER_ID, "hello", CONVERSATION_ID, 21))
        .thenReturn(List.of(buildMessage(1L)));

    List<Message> result = messageService.searchMessages(USER_ID, "hello", CONVERSATION_UUID, null, 20);

    assertEquals(1, result.size());
    verify(messageRepository).searchMessagesInConversation(USER_ID, "hello", CONVERSATION_ID, 21);
  }
}
