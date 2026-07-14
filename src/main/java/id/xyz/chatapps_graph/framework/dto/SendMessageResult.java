package id.xyz.chatapps_graph.framework.dto;

import id.xyz.chatapps_graph.domain.entity.Message;
import lombok.Builder;

@Builder
public record SendMessageResult(
    Message message,
    String senderUuid,
    String conversationUuid
) {}
