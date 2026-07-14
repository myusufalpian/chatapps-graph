package id.xyz.chatapps_graph.framework.dto;

import lombok.Builder;

@Builder
public record LinkPreviewTask(
    Long messageId,
    String url,
    String conversationUuid
) {}
