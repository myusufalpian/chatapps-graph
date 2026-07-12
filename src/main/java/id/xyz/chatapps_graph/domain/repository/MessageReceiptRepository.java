package id.xyz.chatapps_graph.domain.repository;

import static id.xyz.chatapps_graph.infrastructure.constant.SQLConstants.MessageReceiptSQL;

import id.xyz.chatapps_graph.domain.entity.MessageReceipt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, Long> {

  Optional<MessageReceipt> findByMessageIdAndUserId(Long messageId, Long userId);

  List<MessageReceipt> findAllByMessageId(Long messageId);

  List<MessageReceipt> findAllByMessageIdIn(List<Long> messageIds);

  @Query(value = "SELECT DISTINCT m.sender_id FROM message m "
      + "JOIN message_receipt mr ON mr.message_id = m.message_id "
      + "WHERE m.conversation_id = :conversationId AND mr.user_id = :userId "
      + "AND mr.status < :readStatus", nativeQuery = true)
  List<Long> findUnreadMessageSenderIds(Long conversationId, Long userId, int readStatus);

  @Query(value = "SELECT DISTINCT m.sender_id FROM message m "
      + "JOIN message_receipt mr ON mr.message_id = m.message_id "
      + "WHERE m.conversation_id = :conversationId AND mr.user_id = :userId "
      + "AND mr.status = :sentStatus AND m.message_uuid IN (:messageUuids)", nativeQuery = true)
  List<Long> findUndeliveredMessageSenderIds(Long conversationId, Long userId, List<String> messageUuids,
      int sentStatus);

  @Modifying
  @Query(value = MessageReceiptSQL.MARK_AS_READ_BY_CONVERSATION, nativeQuery = true)
  int markAsReadByConversation(Long conversationId, Long userId, int status);

  @Modifying
  @Query(value = "UPDATE message_receipt mr SET status = :deliveredStatus "
      + "FROM message m WHERE mr.message_id = m.message_id "
      + "AND m.conversation_id = :conversationId AND mr.user_id = :userId "
      + "AND mr.status = :sentStatus AND m.message_uuid IN (:messageUuids)", nativeQuery = true)
  int markAsDeliveredByConversation(Long conversationId, Long userId, List<String> messageUuids,
      int sentStatus, int deliveredStatus);
}
