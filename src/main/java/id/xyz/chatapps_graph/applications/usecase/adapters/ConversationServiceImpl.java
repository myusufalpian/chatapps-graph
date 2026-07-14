package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.ConversationType;
import id.xyz.chatapps_graph.domain.enums.GroupMemberRole;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationRepository;
import id.xyz.chatapps_graph.domain.repository.GroupMemberRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.MultiChatResponse;
import id.xyz.chatapps_graph.framework.dto.ParticipantSummary;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.constant.GeneralConstants.StatusConstants;
import id.xyz.chatapps_graph.infrastructure.mapper.ConversationMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import id.xyz.chatapps_graph.applications.usecase.WebSocketBroadcastService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

  private final ConversationRepository conversationRepository;
  private final ConversationParticipantRepository participantRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final UserRepository userRepository;
  private final SystemMessageService systemMessageService;
  private final WebSocketBroadcastService broadcastService;

  @Override
  @Transactional
  public Conversation findOrCreatePrivateConversation(Long userIdA, Long userIdB) {
    return participantRepository.findPrivateConversationBetween(userIdA, userIdB)
        .flatMap(conversationRepository::findById)
        .orElseGet(() -> createPrivateConversation(userIdA, userIdB));
  }

  @Override
  @Transactional(readOnly = true)
  public Conversation findConversationByUuid(String uuid) {
    return conversationRepository.findByConversationUuid(uuid)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), "CONVERSATION_NOT_FOUND", "Conversation not found"));
  }

  @Override
  @Transactional(readOnly = true)
  public Conversation findConversationById(Long conversationId) {
    return conversationRepository.findById(conversationId)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), "CONVERSATION_NOT_FOUND", "Conversation not found"));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isParticipant(Long conversationId, Long userId) {
    return participantRepository.findByConversationIdAndUserId(conversationId, userId).isPresent();
  }

  private Conversation createPrivateConversation(Long userIdA, Long userIdB) {
    var now = OffsetDateTime.now();

    Conversation conversation = conversationRepository.save(
        Conversation.builder()
            .conversationType(ConversationType.PRIVATE.name())
            .createdAt(now)
            .build()
    );

    participantRepository.save(ConversationParticipant.builder()
        .conversationId(conversation.getConversationId())
        .userId(userIdA)
        .joinedAt(now)
        .build());

    participantRepository.save(ConversationParticipant.builder()
        .conversationId(conversation.getConversationId())
        .userId(userIdB)
        .joinedAt(now)
        .build());

    return conversation;
  }

  @Override
  @Transactional
  public Conversation createMultiChat(Long creatorId, List<Long> participantUserIds) {
    if (participantUserIds.size() < 3) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), "MIN_PARTICIPANTS", "Multi-chat requires at least 3 participants");
    }

    var now = OffsetDateTime.now();

    Conversation conversation = conversationRepository.save(
        Conversation.builder()
            .conversationType(ConversationType.MULTI_CHAT.name())
            .createdAt(now)
            .build()
    );

    for (Long userId : participantUserIds) {
      participantRepository.save(ConversationParticipant.builder()
          .conversationId(conversation.getConversationId())
          .userId(userId)
          .joinedAt(now)
          .build());
    }

    return conversation;
  }

  @Override
  @Transactional
  public MultiChatResponse createMultiChatByUuids(Long creatorId, List<String> participantUuids) {
    List<User> users = participantUuids.stream()
        .map(uuid -> userRepository.findUserByUserUuidAndUserStatus(uuid, StatusConstants.ACTIVE)
            .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), "USER_NOT_FOUND", "User not found: " + uuid)))
        .toList();

    List<Long> allParticipantIds = new java.util.ArrayList<>(users.stream().map(User::getUserId).toList());
    if (!allParticipantIds.contains(creatorId)) {
      allParticipantIds.add(creatorId);
    }

    Conversation conversation = createMultiChat(creatorId, allParticipantIds);

    List<User> allUsers = userRepository.findAllById(allParticipantIds);
    List<ParticipantSummary> participants = allUsers.stream()
        .map(ConversationMapper::toParticipantSummary)
        .toList();

    return MultiChatResponse.builder()
        .conversationUuid(conversation.getConversationUuid())
        .participants(participants)
        .build();
  }

  @Override
  @Transactional
  public Conversation updateDisappearingTtl(String uuid, Long userId, Integer ttlHours) {
    Conversation conversation = conversationRepository.findByConversationUuid(uuid)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), "CONVERSATION_NOT_FOUND", "Conversation not found"));
    
    boolean isParticipant = participantRepository.findByConversationIdAndUserId(conversation.getConversationId(), userId).isPresent();
    if (!isParticipant) {
      throw new GeneralException(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "Not a participant");
    }


    if (ConversationType.MULTI_CHAT.name().equals(conversation.getConversationType())) {
      var member = groupMemberRepository.findByGroupIdAndUserIdAndIsActive(conversation.getGroupId(), userId, 1)
          .orElseThrow(() -> new GeneralException(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "Not a group member"));
      String role = member.getMemberType();
      if (!GroupMemberRole.OWNER.name().equals(role) && !GroupMemberRole.ADMIN.name().equals(role)) {
        throw new GeneralException(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "Only admin/owner can change group disappearing messages TTL");
      }
    }

    if (Objects.equals(conversation.getDisappearingTtl(), ttlHours)) {
      return conversation;
    }

    conversation.setDisappearingTtl(ttlHours);
    Conversation saved = conversationRepository.save(conversation);

    User actor = userRepository.findById(userId)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), "USER_NOT_FOUND", "User not found"));

    String event = ttlHours != null ? "DISAPPEARING_ENABLED" : "DISAPPEARING_DISABLED";
    String ttlString = "";
    if (ttlHours != null) {
      if (ttlHours == 24) ttlString = "24h";
      else if (ttlHours == 168) ttlString = "7d";
      else if (ttlHours == 720) ttlString = "30d";
    }

    // Create system message
    systemMessageService.create(conversation.getConversationId(), userId, event, actor.getUserUuid(), null, ttlString);

    // Broadcast WebSocket notification to topic/chat/{conversationUuid}/settings
    broadcastService.broadcast(
        "/topic/chat/" + conversation.getConversationUuid() + "/settings",
        Map.of("type", "DISAPPEARING_CHANGED", "ttl", ttlHours != null ? ttlString : "off")
    );

    return saved;
  }
}
