package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import id.xyz.chatapps_graph.domain.enums.ConversationType;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

  private final ConversationRepository conversationRepository;
  private final ConversationParticipantRepository participantRepository;

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
}
