package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemMessageServiceTest {

  @Mock private MessageRepository messageRepository;
  @Mock private ConversationParticipantRepository participantRepository;

  private SystemMessageService systemMessageService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    systemMessageService = new SystemMessageService(messageRepository, participantRepository, objectMapper);
  }

  @Test
  @DisplayName("create: produces structured JSON content with event and actorUuid")
  void systemMessage_StructuredJsonContent() {
    when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
      Message m = inv.getArgument(0);
      m.setMessageId(1L);
      m.setCreatedAt(OffsetDateTime.now());
      return m;
    });

    systemMessageService.create(10L, 1L, "MEMBER_ADDED", "actor-uuid", "target-uuid");

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(messageRepository).save(captor.capture());
    Message saved = captor.getValue();

    assertEquals("SYSTEM", saved.getMessageType());

    // Parse content as JSON
    String content = saved.getContent();
    assertNotNull(content);
    assertTrue(content.contains("\"event\":\"MEMBER_ADDED\""));
    assertTrue(content.contains("\"actorUuid\":\"actor-uuid\""));
    assertTrue(content.contains("\"targetUuid\":\"target-uuid\""));
  }

  @Test
  @DisplayName("create: no sensitive data in content — only UUIDs, no phone/email")
  void systemMessage_NoSensitiveData() {
    when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
      Message m = inv.getArgument(0);
      m.setMessageId(1L);
      m.setCreatedAt(OffsetDateTime.now());
      return m;
    });

    systemMessageService.create(10L, 1L, "GROUP_CREATED", "actor-uuid-123", null);

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(messageRepository).save(captor.capture());
    String content = captor.getValue().getContent();

    // Should not contain phone-like or email-like patterns
    assertFalse(content.contains("@"));
    assertFalse(content.contains("+62"));
    assertFalse(content.matches(".*\\d{10,}.*"));

    // Should only have event + actorUuid (no targetUuid since null)
    assertTrue(content.contains("\"event\""));
    assertTrue(content.contains("\"actorUuid\""));
    assertFalse(content.contains("targetUuid"));
  }

  @Test
  @DisplayName("create: null targetUuid — omitted from JSON")
  void systemMessage_NullTarget_Omitted() {
    when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
      Message m = inv.getArgument(0);
      m.setMessageId(1L);
      m.setCreatedAt(OffsetDateTime.now());
      return m;
    });

    systemMessageService.create(10L, 1L, "MEMBER_LEFT", "user-uuid", null);

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(messageRepository).save(captor.capture());
    String content = captor.getValue().getContent();

    assertFalse(content.contains("targetUuid"));
  }

  @Test
  @DisplayName("create: updates conversation participant last message")
  void systemMessage_UpdatesParticipantLastMessage() {
    when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
      Message m = inv.getArgument(0);
      m.setMessageId(1L);
      m.setCreatedAt(OffsetDateTime.now());
      return m;
    });

    systemMessageService.create(10L, 1L, "GROUP_RENAMED", "actor-uuid", null);

    verify(participantRepository).incrementUnreadAndUpdateLastMessage(
        eq(10L), eq(1L), any(OffsetDateTime.class), eq("GROUP_RENAMED"), eq("SYSTEM"));
    verify(participantRepository).updateSenderLastMessage(
        eq(10L), eq(1L), any(OffsetDateTime.class), eq("GROUP_RENAMED"), eq("SYSTEM"));
  }
}
