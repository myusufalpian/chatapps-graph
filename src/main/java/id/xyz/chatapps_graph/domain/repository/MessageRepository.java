package id.xyz.chatapps_graph.domain.repository;

import static id.xyz.chatapps_graph.infrastructure.constant.SQLConstants.ConversationListSQL;
import static id.xyz.chatapps_graph.infrastructure.constant.SQLConstants.MessageSQL;

import id.xyz.chatapps_graph.domain.entity.Message;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

  Optional<Message> findByMessageUuid(String messageUuid);

  @Query(value = MessageSQL.FIND_MESSAGES_AFTER_CURSOR, nativeQuery = true)
  List<Message> findMessagesAfterCursor(Long conversationId, Long userId,
      OffsetDateTime cursorTs, Long cursorId, int limit);

  @Query(value = MessageSQL.FIND_FIRST_MESSAGES, nativeQuery = true)
  List<Message> findFirstMessages(Long conversationId, Long userId, int limit);

  @Query(value = ConversationListSQL.FIND_LATEST_ACTIVE_MESSAGE_ID, nativeQuery = true)
  Optional<Long> findLatestActiveMessageId(Long conversationId);
}
