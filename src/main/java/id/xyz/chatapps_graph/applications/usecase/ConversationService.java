package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.framework.dto.MultiChatResponse;
import java.util.List;

public interface ConversationService {

  Conversation findOrCreatePrivateConversation(Long userIdA, Long userIdB);

  Conversation findConversationByUuid(String uuid);

  Conversation findConversationById(Long conversationId);

  boolean isParticipant(Long conversationId, Long userId);

  void validateParticipant(Long conversationId, Long userId);

  Conversation createMultiChat(Long creatorId, List<Long> participantUserIds);

  MultiChatResponse createMultiChatByUuids(Long creatorId, List<String> participantUuids);

  Conversation updateDisappearingTtl(String uuid, Long userId, Integer ttlHours);
}
