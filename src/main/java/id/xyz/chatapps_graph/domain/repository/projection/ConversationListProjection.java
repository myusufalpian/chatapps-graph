package id.xyz.chatapps_graph.domain.repository.projection;

import java.time.OffsetDateTime;

public interface ConversationListProjection {

  Long getConversationId();

  OffsetDateTime getLastMessageAt();

  String getLastMessagePreview();

  String getLastMessageType();

  Integer getUnreadCount();

  Boolean getIsPinned();

  OffsetDateTime getPinnedAt();

  Boolean getIsMuted();

  String getConversationUuid();

  String getConversationType();
}
