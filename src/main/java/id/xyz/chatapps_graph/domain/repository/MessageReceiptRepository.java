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

  @Modifying
  @Query(value = MessageReceiptSQL.MARK_AS_READ_BY_CONVERSATION, nativeQuery = true)
  int markAsReadByConversation(Long conversationId, Long userId, int status);
}
