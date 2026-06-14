package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.domain.entity.Conversation;
import java.util.List;

public interface ConversationService {

  Conversation findOrCreatePrivateConversation(Long userIdA, Long userIdB);

  Conversation findConversationByUuid(String uuid);

  boolean isParticipant(Long conversationId, Long userId);

  Conversation createMultiChat(Long creatorId, List<Long> participantUserIds);
}
