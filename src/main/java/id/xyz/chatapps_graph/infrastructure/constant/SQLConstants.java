package id.xyz.chatapps_graph.infrastructure.constant;

import lombok.experimental.UtilityClass;

public class SQLConstants {

  @UtilityClass
  public static class UserSQL {
    public static final String GET_USER_DETAIL_BY_USER_ID_AND_USER_STATUS = "SELECT u.user_uuid as userUuid, u.user_phone as userPhone, u.user_email as userEmail, u.user_full_name as userFullName, u.user_status as userStatus, au.about_uuid as aboutUuid, au.about_desc as aboutDesc FROM chatapps.users u JOIN chatapps.about_user au ON u.about_id = au.about_id WHERE u.user_id = :userId and u.user_status = :userStatus";
  }

  @UtilityClass
  public static class MessageSQL {
    public static final String FIND_MESSAGES_AFTER_CURSOR =
        "SELECT m.* FROM message m " +
        "LEFT JOIN message_receipt mr ON mr.message_id = m.message_id AND mr.user_id = :userId " +
        "WHERE m.conversation_id = :conversationId " +
        "AND (mr.is_deleted_for_me IS NULL OR mr.is_deleted_for_me = false) " +
        "AND (m.created_at > :cursorTs OR (m.created_at = :cursorTs AND m.message_id > :cursorId)) " +
        "ORDER BY m.created_at ASC, m.message_id ASC " +
        "LIMIT :limit";

    public static final String FIND_FIRST_MESSAGES =
        "SELECT m.* FROM message m " +
        "LEFT JOIN message_receipt mr ON mr.message_id = m.message_id AND mr.user_id = :userId " +
        "WHERE m.conversation_id = :conversationId " +
        "AND (mr.is_deleted_for_me IS NULL OR mr.is_deleted_for_me = false) " +
        "ORDER BY m.created_at ASC, m.message_id ASC " +
        "LIMIT :limit";
  }

  @UtilityClass
  public static class MessageReceiptSQL {
    public static final String MARK_AS_READ_BY_CONVERSATION =
        "UPDATE message_receipt SET status = :status, read_at = now() " +
        "WHERE user_id = :userId AND status < :status " +
        "AND message_id IN (SELECT m.message_id FROM message m WHERE m.conversation_id = :conversationId)";
  }

  @UtilityClass
  public static class ConversationParticipantSQL {
    public static final String FIND_PRIVATE_CONVERSATION_BETWEEN =
        "SELECT cp.conversation_id FROM conversation_participant cp " +
        "WHERE cp.conversation_id IN (SELECT cp2.conversation_id FROM conversation_participant cp2 WHERE cp2.user_id = :userIdA) " +
        "AND cp.user_id = :userIdB " +
        "AND cp.conversation_id IN (SELECT c.conversation_id FROM conversation c WHERE c.conversation_type = 'PRIVATE')";
  }

  @UtilityClass
  public static class ConversationListSQL {

    private static final String BASE_SELECT =
        "SELECT cp.conversation_id AS conversationId, cp.last_message_at AS lastMessageAt, " +
        "cp.last_message_preview AS lastMessagePreview, cp.last_message_type AS lastMessageType, " +
        "cp.unread_count AS unreadCount, cp.is_pinned AS isPinned, cp.pinned_at AS pinnedAt, " +
        "cp.is_muted AS isMuted, c.conversation_uuid AS conversationUuid, " +
        "c.conversation_type AS conversationType " +
        "FROM conversation_participant cp " +
        "JOIN conversation c ON c.conversation_id = cp.conversation_id ";

    private static final String ORDER_FIRST_PAGE =
        " ORDER BY cp.is_pinned DESC, cp.pinned_at ASC, cp.last_message_at DESC NULLS LAST, cp.conversation_id DESC LIMIT :limit";

    private static final String ORDER_CURSOR_PAGE =
        " ORDER BY cp.last_message_at DESC NULLS LAST, cp.conversation_id DESC LIMIT :limit";

    public static final String LIST_FIRST_PAGE =
        BASE_SELECT +
        "WHERE cp.user_id = :userId AND cp.is_archived = :isArchived" +
        ORDER_FIRST_PAGE;

    public static final String LIST_FIRST_PAGE_UNREAD =
        BASE_SELECT +
        "WHERE cp.user_id = :userId AND cp.is_archived = :isArchived AND cp.unread_count > 0" +
        ORDER_FIRST_PAGE;

    public static final String LIST_FIRST_PAGE_BY_TYPE =
        BASE_SELECT +
        "WHERE cp.user_id = :userId AND cp.is_archived = :isArchived AND c.conversation_type = :convType" +
        ORDER_FIRST_PAGE;

    public static final String LIST_WITH_CURSOR =
        BASE_SELECT +
        "WHERE cp.user_id = :userId AND cp.is_archived = :isArchived AND cp.is_pinned = false " +
        "AND (cp.last_message_at < :cursorTs OR (cp.last_message_at = :cursorTs AND cp.conversation_id < :cursorId))" +
        ORDER_CURSOR_PAGE;

    public static final String LIST_WITH_CURSOR_UNREAD =
        BASE_SELECT +
        "WHERE cp.user_id = :userId AND cp.is_archived = :isArchived AND cp.is_pinned = false " +
        "AND cp.unread_count > 0 " +
        "AND (cp.last_message_at < :cursorTs OR (cp.last_message_at = :cursorTs AND cp.conversation_id < :cursorId))" +
        ORDER_CURSOR_PAGE;

    public static final String LIST_WITH_CURSOR_BY_TYPE =
        BASE_SELECT +
        "WHERE cp.user_id = :userId AND cp.is_archived = :isArchived AND cp.is_pinned = false " +
        "AND c.conversation_type = :convType " +
        "AND (cp.last_message_at < :cursorTs OR (cp.last_message_at = :cursorTs AND cp.conversation_id < :cursorId))" +
        ORDER_CURSOR_PAGE;

    public static final String COUNT_PINNED =
        "SELECT COUNT(*) FROM conversation_participant WHERE user_id = :userId AND is_pinned = true";

    public static final String INCREMENT_UNREAD =
        "UPDATE conversation_participant SET unread_count = unread_count + 1, " +
        "last_message_at = :lastMessageAt, last_message_preview = :preview, last_message_type = :messageType " +
        "WHERE conversation_id = :conversationId AND user_id != :senderId";

    public static final String AUTO_UNARCHIVE =
        "UPDATE conversation_participant SET is_archived = false " +
        "WHERE conversation_id = :conversationId AND user_id != :senderId AND is_archived = true";

    public static final String UPDATE_SENDER_LAST_MESSAGE =
        "UPDATE conversation_participant SET last_message_at = :lastMessageAt, " +
        "last_message_preview = :preview, last_message_type = :messageType " +
        "WHERE conversation_id = :conversationId AND user_id = :senderId";

    public static final String RESET_UNREAD =
        "UPDATE conversation_participant SET unread_count = 0 " +
        "WHERE conversation_id = :conversationId AND user_id = :userId";

    public static final String UPDATE_LAST_MESSAGE_PREVIEW_FOR_ALL =
        "UPDATE conversation_participant SET last_message_preview = :preview " +
        "WHERE conversation_id = :conversationId";

    public static final String COUNT_PINNED_FOR_UPDATE =
        "SELECT COUNT(*) FROM conversation_participant WHERE user_id = :userId AND is_pinned = true FOR UPDATE";

    public static final String FIND_LATEST_ACTIVE_MESSAGE_ID =
        "SELECT m.message_id FROM message m " +
        "WHERE m.conversation_id = :conversationId AND m.message_status = 0 " +
        "ORDER BY m.created_at DESC, m.message_id DESC LIMIT 1";
  }

  @UtilityClass
  public static class MessageSearchSQL {
    public static final String SEARCH_FIRST_PAGE =
        "SELECT m.* FROM message m " +
        "JOIN conversation_participant cp ON cp.conversation_id = m.conversation_id AND cp.user_id = :userId " +
        "LEFT JOIN message_receipt mr ON mr.message_id = m.message_id AND mr.user_id = :userId " +
        "WHERE m.search_vector @@ plainto_tsquery('simple', :query) " +
        "AND m.message_status = 0 " +
        "AND (mr.is_deleted_for_me IS NULL OR mr.is_deleted_for_me = false) " +
        "AND m.created_at > now() - interval '1 year' " +
        "ORDER BY m.created_at DESC, m.message_id DESC LIMIT :limit";

    public static final String SEARCH_WITH_CURSOR =
        "SELECT m.* FROM message m " +
        "JOIN conversation_participant cp ON cp.conversation_id = m.conversation_id AND cp.user_id = :userId " +
        "LEFT JOIN message_receipt mr ON mr.message_id = m.message_id AND mr.user_id = :userId " +
        "WHERE m.search_vector @@ plainto_tsquery('simple', :query) " +
        "AND m.message_status = 0 " +
        "AND (mr.is_deleted_for_me IS NULL OR mr.is_deleted_for_me = false) " +
        "AND m.created_at > now() - interval '1 year' " +
        "AND (m.created_at < :cursorTs OR (m.created_at = :cursorTs AND m.message_id < :cursorId)) " +
        "ORDER BY m.created_at DESC, m.message_id DESC LIMIT :limit";

    public static final String SEARCH_IN_CONVERSATION =
        "SELECT m.* FROM message m " +
        "JOIN conversation_participant cp ON cp.conversation_id = m.conversation_id AND cp.user_id = :userId " +
        "LEFT JOIN message_receipt mr ON mr.message_id = m.message_id AND mr.user_id = :userId " +
        "WHERE m.search_vector @@ plainto_tsquery('simple', :query) " +
        "AND m.conversation_id = :conversationId " +
        "AND m.message_status = 0 " +
        "AND (mr.is_deleted_for_me IS NULL OR mr.is_deleted_for_me = false) " +
        "ORDER BY m.created_at DESC, m.message_id DESC LIMIT :limit";

    public static final String SEARCH_IN_CONVERSATION_WITH_CURSOR =
        "SELECT m.* FROM message m " +
        "JOIN conversation_participant cp ON cp.conversation_id = m.conversation_id AND cp.user_id = :userId " +
        "LEFT JOIN message_receipt mr ON mr.message_id = m.message_id AND mr.user_id = :userId " +
        "WHERE m.search_vector @@ plainto_tsquery('simple', :query) " +
        "AND m.conversation_id = :conversationId " +
        "AND m.message_status = 0 " +
        "AND (mr.is_deleted_for_me IS NULL OR mr.is_deleted_for_me = false) " +
        "AND (m.created_at < :cursorTs OR (m.created_at = :cursorTs AND m.message_id < :cursorId)) " +
        "ORDER BY m.created_at DESC, m.message_id DESC LIMIT :limit";
  }

  @UtilityClass
  public static class UserPresenceSQL {
    public static final String UPDATE_LAST_SEEN =
        "UPDATE users SET last_seen_at = now() WHERE user_id = :userId";
  }
}
