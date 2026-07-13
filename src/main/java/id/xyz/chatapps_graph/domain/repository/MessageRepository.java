package id.xyz.chatapps_graph.domain.repository;

import static id.xyz.chatapps_graph.infrastructure.constant.SQLConstants.ConversationListSQL;
import static id.xyz.chatapps_graph.infrastructure.constant.SQLConstants.MessageSQL;
import static id.xyz.chatapps_graph.infrastructure.constant.SQLConstants.MessageSearchSQL;

import id.xyz.chatapps_graph.domain.entity.Message;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  @Query(value = MessageSearchSQL.SEARCH_FIRST_PAGE, nativeQuery = true)
  List<Message> searchMessages(Long userId, String query, int limit);

  @Query(value = MessageSearchSQL.SEARCH_WITH_CURSOR, nativeQuery = true)
  List<Message> searchMessagesWithCursor(Long userId, String query, OffsetDateTime cursorTs, Long cursorId, int limit);

  @Query(value = MessageSearchSQL.SEARCH_IN_CONVERSATION, nativeQuery = true)
  List<Message> searchMessagesInConversation(Long userId, String query, Long conversationId, int limit);

  @Query(value = MessageSearchSQL.SEARCH_IN_CONVERSATION_WITH_CURSOR, nativeQuery = true)
  List<Message> searchMessagesInConversationWithCursor(Long userId, String query, Long conversationId, OffsetDateTime cursorTs, Long cursorId, int limit);

  @Query("SELECT m.messageId FROM Message m WHERE m.conversationId = :conversationId AND m.messageStatus != 1 AND m.createdAt < :threshold")
  List<Long> findExpiredMessageIds(
      @Param("conversationId") Long conversationId, 
      @Param("threshold") OffsetDateTime threshold, 
      Pageable pageable
  );

  @Modifying
  @Query("UPDATE Message m SET m.messageStatus = 1, m.deletedAt = :now WHERE m.messageId IN :ids")
  void softDeleteMessages(@Param("ids") List<Long> ids, @Param("now") OffsetDateTime now);
}

