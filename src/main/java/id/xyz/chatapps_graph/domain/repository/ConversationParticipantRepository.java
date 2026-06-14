package id.xyz.chatapps_graph.domain.repository;

import static id.xyz.chatapps_graph.infrastructure.constant.SQLConstants.ConversationListSQL;
import static id.xyz.chatapps_graph.infrastructure.constant.SQLConstants.ConversationParticipantSQL;

import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import id.xyz.chatapps_graph.domain.repository.projection.ConversationListProjection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

  Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);

  List<ConversationParticipant> findAllByUserId(Long userId);

  List<ConversationParticipant> findAllByConversationId(Long conversationId);

  List<ConversationParticipant> findAllByConversationIdIn(List<Long> conversationIds);

  @Query(value = ConversationParticipantSQL.FIND_PRIVATE_CONVERSATION_BETWEEN, nativeQuery = true)
  Optional<Long> findPrivateConversationBetween(Long userIdA, Long userIdB);

  // --- List conversations with projection ---

  @Query(value = ConversationListSQL.LIST_FIRST_PAGE, nativeQuery = true)
  List<ConversationListProjection> findConversationsFirstPage(Long userId, boolean isArchived, int limit);

  @Query(value = ConversationListSQL.LIST_FIRST_PAGE_UNREAD, nativeQuery = true)
  List<ConversationListProjection> findConversationsFirstPageUnread(Long userId, boolean isArchived, int limit);

  @Query(value = ConversationListSQL.LIST_FIRST_PAGE_BY_TYPE, nativeQuery = true)
  List<ConversationListProjection> findConversationsFirstPageByType(Long userId, boolean isArchived, String convType, int limit);

  @Query(value = ConversationListSQL.LIST_WITH_CURSOR, nativeQuery = true)
  List<ConversationListProjection> findConversationsWithCursor(Long userId, boolean isArchived,
      OffsetDateTime cursorTs, Long cursorId, int limit);

  @Query(value = ConversationListSQL.LIST_WITH_CURSOR_UNREAD, nativeQuery = true)
  List<ConversationListProjection> findConversationsWithCursorUnread(Long userId, boolean isArchived,
      OffsetDateTime cursorTs, Long cursorId, int limit);

  @Query(value = ConversationListSQL.LIST_WITH_CURSOR_BY_TYPE, nativeQuery = true)
  List<ConversationListProjection> findConversationsWithCursorByType(Long userId, boolean isArchived,
      String convType, OffsetDateTime cursorTs, Long cursorId, int limit);

  // --- Pin count ---

  @Query(value = ConversationListSQL.COUNT_PINNED, nativeQuery = true)
  int countPinnedByUserId(Long userId);

  @Query(value = ConversationListSQL.COUNT_PINNED_FOR_UPDATE, nativeQuery = true)
  int countPinnedByUserIdForUpdate(Long userId);

  // --- Denormalized field updates ---

  @Modifying
  @Query(value = ConversationListSQL.INCREMENT_UNREAD, nativeQuery = true)
  void incrementUnreadAndUpdateLastMessage(Long conversationId, Long senderId,
      OffsetDateTime lastMessageAt, String preview, String messageType);

  @Modifying
  @Query(value = ConversationListSQL.AUTO_UNARCHIVE, nativeQuery = true)
  void autoUnarchive(Long conversationId, Long senderId);

  @Modifying
  @Query(value = ConversationListSQL.UPDATE_SENDER_LAST_MESSAGE, nativeQuery = true)
  void updateSenderLastMessage(Long conversationId, Long senderId,
      OffsetDateTime lastMessageAt, String preview, String messageType);

  @Modifying
  @Query(value = ConversationListSQL.RESET_UNREAD, nativeQuery = true)
  void resetUnreadCount(Long conversationId, Long userId);

  @Modifying
  @Query(value = ConversationListSQL.UPDATE_LAST_MESSAGE_PREVIEW_FOR_ALL, nativeQuery = true)
  void updateLastMessagePreviewForAll(Long conversationId, String preview);
}
