package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.domain.entity.Message;
import java.util.List;

public interface MessageService {

  Message sendMessage(Long senderId, String recipientUuid, String conversationUuid,
      String messageType, String content, Long attachmentId, String replyToMessageUuid);

  List<Message> listMessages(Long conversationId, Long userId, String cursor, int limit);

  void deleteMessage(String messageUuid, Long userId, String mode);

  boolean markAsRead(String conversationUuid, Long userId);

  Message forwardMessage(Long userId, String messageUuid, String targetConversationUuid);

  void addReaction(Long userId, Long messageId, String emoji);

  void removeReaction(Long userId, Long messageId);

  List<Message> searchMessages(Long userId, String query, String conversationUuid, String cursor, int limit);

  Message editMessage(Long userId, String messageUuid, String newContent);
}
