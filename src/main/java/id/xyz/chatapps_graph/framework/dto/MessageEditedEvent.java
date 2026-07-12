package id.xyz.chatapps_graph.framework.dto;

import java.time.OffsetDateTime;

public record MessageEditedEvent(
    String eventType,
    String messageUuid,
    String conversationUuid,
    String content,
    OffsetDateTime editedAt
) {}
