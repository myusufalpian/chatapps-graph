package id.xyz.chatapps_graph.framework.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SendMessageRequest(
    String conversationUuid,
    String recipientUuid,
    String messageType,
    String content,
    String replyToMessageUuid,
    String attachmentType
) {}
