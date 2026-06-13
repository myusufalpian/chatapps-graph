package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.domain.entity.Message;
import java.util.List;

public interface MessageService {

  Message sendMessage(Long senderId, String recipientUuid, String conversationUuid,
      String messageType, String content, Long attachmentId, String replyToMessageUuid);

  List<Message> listMessages(Long conversationId, Long userId, String cursor, int limit);

  void deleteMessage(String messageUuid, Long userId, String mode);

  void markAsRead(String conversationUuid, Long userId);
}
