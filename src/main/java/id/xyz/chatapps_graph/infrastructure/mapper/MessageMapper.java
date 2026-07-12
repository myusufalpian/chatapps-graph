package id.xyz.chatapps_graph.infrastructure.mapper;

import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.enums.MessageStatus;
import id.xyz.chatapps_graph.framework.dto.AttachmentResponse;
import id.xyz.chatapps_graph.framework.dto.ForwardedInfo;
import id.xyz.chatapps_graph.framework.dto.MessageResponse;
import id.xyz.chatapps_graph.framework.dto.ReactionSummary;
import id.xyz.chatapps_graph.framework.dto.ReplyToResponse;
import java.util.List;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class MessageMapper {

  public static MessageResponse toResponse(Message message, String senderUuid, String conversationUuid,
      AttachmentResponse attachment, ReplyToResponse replyTo,
      ForwardedInfo forwardedFrom, List<ReactionSummary> reactions, String displayText) {
    return toResponse(message, senderUuid, conversationUuid, attachment, replyTo, forwardedFrom, reactions, displayText, null);
  }

  public static MessageResponse toResponse(Message message, String senderUuid, String conversationUuid,
      AttachmentResponse attachment, ReplyToResponse replyTo,
      ForwardedInfo forwardedFrom, List<ReactionSummary> reactions, String displayText, Integer deliveryStatus) {
    String content = message.getMessageStatus() == MessageStatus.DELETED.getValue() ? null : message.getContent();
    return MessageResponse.builder()
        .messageUuid(message.getMessageUuid())
        .conversationUuid(conversationUuid)
        .senderUuid(senderUuid)
        .messageType(message.getMessageType())
        .content(content)
        .attachment(attachment)
        .replyTo(replyTo)
        .forwardedFrom(forwardedFrom)
        .reactions(reactions)
        .status(message.getMessageStatus())
        .deliveryStatus(deliveryStatus)
        .createdAt(message.getCreatedAt())
        .editedAt(message.getEditedAt())
        .displayText(displayText)
        .build();
  }

  public static ReplyToResponse toReplyResponse(Message replyMessage, String replySenderUuid) {
    if (replyMessage == null) {
      return null;
    }
    return new ReplyToResponse(replyMessage.getMessageUuid(), replySenderUuid, replyMessage.getContent());
  }
}
