package id.xyz.chatapps_graph.framework.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record MessageResponse(
    String messageUuid,
    String conversationUuid,
    String senderUuid,
    String messageType,
    String content,
    AttachmentResponse attachment,
    ReplyToResponse replyTo,
    Integer status,
    OffsetDateTime createdAt
) {}
