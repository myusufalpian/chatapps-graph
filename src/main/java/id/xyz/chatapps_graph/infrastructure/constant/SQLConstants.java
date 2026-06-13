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
}
