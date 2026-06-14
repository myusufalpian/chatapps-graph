package id.xyz.chatapps_graph.framework.dto;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record SearchResultItem(
    String messageUuid,
    String conversationUuid,
    String senderUuid,
    String content,
    String messageType,
    OffsetDateTime createdAt
) {}
