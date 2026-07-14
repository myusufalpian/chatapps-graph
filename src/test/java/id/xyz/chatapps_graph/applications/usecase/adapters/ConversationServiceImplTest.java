package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import id.xyz.chatapps_graph.domain.entity.GroupMember;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.GroupMemberRole;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationRepository;
import id.xyz.chatapps_graph.domain.repository.GroupMemberRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import id.xyz.chatapps_graph.applications.usecase.WebSocketBroadcastService;
import org.springframework.http.HttpStatus;

import id.xyz.chatapps_graph.framework.dto.MultiChatResponse;
import id.xyz.chatapps_graph.infrastructure.constant.GeneralConstants.StatusConstants;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

  @Mock private ConversationRepository conversationRepository;
  @Mock private ConversationParticipantRepository participantRepository;
  @Mock private GroupMemberRepository groupMemberRepository;
  @Mock private UserRepository userRepository;
  @Mock private SystemMessageService systemMessageService;
  @Mock private WebSocketBroadcastService broadcastService;

  @InjectMocks private ConversationServiceImpl conversationService;


  private static final Long USER_A = 1L;
  private static final Long USER_B = 2L;
  private static final Long CONVERSATION_ID = 10L;
  private static final String CONVERSATION_UUID = "conv-uuid-123";

  @Test
  @DisplayName("findOrCreate: no existing conversation — creates new with two participants")
  void findOrCreate_NoExisting_CreatesNew() {
    when(participantRepository.findPrivateConversationBetween(USER_A, USER_B))
        .thenReturn(Optional.empty());

    Conversation expectedToSave = Conversation.builder()
        .conversationType("PRIVATE")
        .build();

    Conversation saved = new Conversation();
    saved.setConversationId(CONVERSATION_ID);
    saved.setConversationType("PRIVATE");

    when(conversationRepository.save(refEq(expectedToSave, "createdAt"))).thenReturn(saved);

    Conversation result = conversationService.findOrCreatePrivateConversation(USER_A, USER_B);

    assertEquals(CONVERSATION_ID, result.getConversationId());
    assertEquals("PRIVATE", result.getConversationType());

    ConversationParticipant pA = ConversationParticipant.builder()
        .conversationId(CONVERSATION_ID)
        .userId(USER_A)
        .build();
    ConversationParticipant pB = ConversationParticipant.builder()
        .conversationId(CONVERSATION_ID)
        .userId(USER_B)
        .build();

    verify(participantRepository).save(refEq(pA, "joinedAt"));
    verify(participantRepository).save(refEq(pB, "joinedAt"));
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
    verify(conversationRepository, never()).save(refEq(new Conversation(), "createdAt"));
  }

  @Test
  @DisplayName("isParticipant: user not a member — returns false")
  void isParticipant_NotMember_ReturnsFalse() {
    when(participantRepository.findByConversationIdAndUserId(CONVERSATION_ID, USER_A))
        .thenReturn(Optional.empty());

    boolean result = conversationService.isParticipant(CONVERSATION_ID, USER_A);

    assertFalse(result);
  }

  @Test
  @DisplayName("updateDisappearingTtl: private chat success")
  void updateDisappearingTtl_PrivateChat_Success() {
    Conversation conv = Conversation.builder()
        .conversationId(CONVERSATION_ID)
        .conversationUuid(CONVERSATION_UUID)
        .conversationType("PRIVATE")
        .disappearingTtl(null)
        .build();

    when(conversationRepository.findByConversationUuid(CONVERSATION_UUID)).thenReturn(Optional.of(conv));
    when(participantRepository.findByConversationIdAndUserId(CONVERSATION_ID, USER_A))
        .thenReturn(Optional.of(new ConversationParticipant()));

    Conversation updatedConv = Conversation.builder()
        .conversationId(CONVERSATION_ID)
        .conversationUuid(CONVERSATION_UUID)
        .conversationType("PRIVATE")
        .disappearingTtl(24)
        .build();
    when(conversationRepository.save(conv)).thenReturn(updatedConv);

    User actor = new User();
    actor.setUserId(USER_A);
    actor.setUserUuid("actor-uuid");
    when(userRepository.findById(USER_A)).thenReturn(Optional.of(actor));

    Message sysMsg = new Message();
    when(systemMessageService.create(CONVERSATION_ID, USER_A, "DISAPPEARING_ENABLED", "actor-uuid", null, "24h"))
        .thenReturn(sysMsg);

    Conversation result = conversationService.updateDisappearingTtl(CONVERSATION_UUID, USER_A, 24);

    assertEquals(24, result.getDisappearingTtl());
    verify(broadcastService).broadcast(
        eq("/topic/chat/" + CONVERSATION_UUID + "/settings"),
        eq(Map.of("type", "DISAPPEARING_CHANGED", "ttl", "24h"))
    );
  }

  @Test
  @DisplayName("updateDisappearingTtl: group chat but user not admin/owner — throws Forbidden")
  void updateDisappearingTtl_GroupChat_NotAdmin_ThrowsForbidden() {
    Conversation conv = Conversation.builder()
        .conversationId(CONVERSATION_ID)
        .conversationUuid(CONVERSATION_UUID)
        .conversationType("MULTI_CHAT")
        .groupId(50L)
        .disappearingTtl(null)
        .build();

    when(conversationRepository.findByConversationUuid(CONVERSATION_UUID)).thenReturn(Optional.of(conv));
    when(participantRepository.findByConversationIdAndUserId(CONVERSATION_ID, USER_A))
        .thenReturn(Optional.of(new ConversationParticipant()));

    GroupMember member = new GroupMember();
    member.setMemberType(GroupMemberRole.MEMBER.name());
    when(groupMemberRepository.findByGroupIdAndUserIdAndIsActive(50L, USER_A, 1))
        .thenReturn(Optional.of(member));

    GeneralException ex = assertThrows(GeneralException.class, () ->
        conversationService.updateDisappearingTtl(CONVERSATION_UUID, USER_A, 24)
    );

    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getHttpCode());
    assertEquals("Only admin/owner can change group disappearing messages TTL", ex.getMessage());
  }

  @Test
  @DisplayName("updateDisappearingTtl: not participant — throws Forbidden")
  void updateDisappearingTtl_NotParticipant_ThrowsForbidden() {
    Conversation conv = Conversation.builder()
        .conversationId(CONVERSATION_ID)
        .conversationUuid(CONVERSATION_UUID)
        .conversationType("PRIVATE")
        .disappearingTtl(null)
        .build();

    when(conversationRepository.findByConversationUuid(CONVERSATION_UUID)).thenReturn(Optional.of(conv));
    when(participantRepository.findByConversationIdAndUserId(CONVERSATION_ID, USER_A))
        .thenReturn(Optional.empty());

    GeneralException ex = assertThrows(GeneralException.class, () ->
        conversationService.updateDisappearingTtl(CONVERSATION_UUID, USER_A, 24)
    );

    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getHttpCode());
    assertEquals("Not a participant", ex.getMessage());
  }

  @Test
  @DisplayName("findConversationById: existing conversation — returns it")
  void findConversationById_Existing_ReturnsIt() {
    Conversation conv = new Conversation();
    conv.setConversationId(CONVERSATION_ID);
    when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conv));

    Conversation result = conversationService.findConversationById(CONVERSATION_ID);

    assertNotNull(result);
    assertEquals(CONVERSATION_ID, result.getConversationId());
  }

  @Test
  @DisplayName("findConversationById: conversation not found — throws NotFound")
  void findConversationById_NotFound_ThrowsNotFound() {
    when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.empty());

    GeneralException ex = assertThrows(GeneralException.class, () ->
        conversationService.findConversationById(CONVERSATION_ID)
    );

    assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpCode());
    assertEquals("CONVERSATION_NOT_FOUND", ex.getKey());
  }

  @Test
  @DisplayName("createMultiChatByUuids: succeeds and returns MultiChatResponse")
  void createMultiChatByUuids_Succeeds() {
    User uB = new User();
    uB.setUserId(USER_B);
    uB.setUserUuid("uuid-B");
    uB.setUserFullName("User B");

    User uC = new User();
    uC.setUserId(3L);
    uC.setUserUuid("uuid-C");
    uC.setUserFullName("User C");

    User uA = new User();
    uA.setUserId(USER_A);
    uA.setUserUuid("uuid-A");
    uA.setUserFullName("User A");

    when(userRepository.findUserByUserUuidAndUserStatus("uuid-B", StatusConstants.ACTIVE)).thenReturn(Optional.of(uB));
    when(userRepository.findUserByUserUuidAndUserStatus("uuid-C", StatusConstants.ACTIVE)).thenReturn(Optional.of(uC));

    Conversation conversation = new Conversation();
    conversation.setConversationId(10L);
    conversation.setConversationUuid("conv-uuid");
    conversation.setConversationType("MULTI_CHAT");
    when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

    when(userRepository.findAllById(any())).thenReturn(List.of(uA, uB, uC));

    MultiChatResponse response = conversationService.createMultiChatByUuids(USER_A, List.of("uuid-B", "uuid-C"));

    assertNotNull(response);
    assertEquals("conv-uuid", response.conversationUuid());
  }

  @Test
  @DisplayName("createMultiChatByUuids: user not found — throws NotFound")
  void createMultiChatByUuids_UserNotFound_ThrowsNotFound() {
    when(userRepository.findUserByUserUuidAndUserStatus("uuid-B", StatusConstants.ACTIVE)).thenReturn(Optional.empty());

    GeneralException ex = assertThrows(GeneralException.class, () ->
        conversationService.createMultiChatByUuids(USER_A, List.of("uuid-B", "uuid-C"))
    );

    assertEquals(HttpStatus.NOT_FOUND.value(), ex.getHttpCode());
    assertEquals("USER_NOT_FOUND", ex.getKey());
  }
}

