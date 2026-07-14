package id.xyz.chatapps_graph.framework.dto;

import lombok.Builder;

@Builder
public record FCMNotificationTask(
    Long messageId,
    Long senderId,
    Long conversationId
) {}
