package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.framework.dto.ConversationListResponse;

public interface ConversationListService {

  ConversationListResponse listConversations(Long userId, String filter, String cursor, int limit);

  void pinConversation(Long userId, String conversationUuid);

  void unpinConversation(Long userId, String conversationUuid);

  void archiveConversation(Long userId, String conversationUuid);

  void unarchiveConversation(Long userId, String conversationUuid);

  void muteConversation(Long userId, String conversationUuid);

  void unmuteConversation(Long userId, String conversationUuid);
}
