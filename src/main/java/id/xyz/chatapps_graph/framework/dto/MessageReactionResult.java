package id.xyz.chatapps_graph.framework.dto;

import lombok.Builder;

@Builder
public record MessageReactionResult(
    String conversationUuid,
    String userUuid,
    String emoji
) {}
