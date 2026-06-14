package id.xyz.chatapps_graph.framework.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record ConversationItemResponse(
    String conversationUuid,
    String conversationType,
    OffsetDateTime lastMessageAt,
    String lastMessagePreview,
    String lastMessageType,
    Integer unreadCount,
    Boolean isPinned,
    Boolean isMuted,
    List<ParticipantSummary> participants
) {}
