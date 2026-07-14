package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.framework.dto.MessageReactionResult;
import id.xyz.chatapps_graph.framework.dto.SendMessageResult;
import java.util.List;

public interface MessageService {

  SendMessageResult sendMessage(Long senderId, String recipientUuid, String conversationUuid,
      String messageType, String content, Long attachmentId, String replyToMessageUuid);

  List<Message> listMessages(Long conversationId, Long userId, String cursor, int limit);

  void deleteMessage(String messageUuid, Long userId, String mode);

  ReadReceiptResult markAsRead(String conversationUuid, Long userId);

  DeliveryReceiptResult markAsDelivered(String conversationUuid, Long userId, List<String> messageUuids);

  Message forwardMessage(Long userId, String messageUuid, String targetConversationUuid);

  MessageReactionResult addReaction(Long userId, String messageUuid, String emoji);

  MessageReactionResult removeReaction(Long userId, String messageUuid);

  List<Message> searchMessages(Long userId, String query, String conversationUuid, String cursor, int limit);

  MessageEditResult editMessage(Long userId, String messageUuid, String newContent);

  String resolveUserUuid(Long userId);
}

