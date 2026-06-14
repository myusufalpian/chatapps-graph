package id.xyz.chatapps_graph.infrastructure.mapper;

import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.enums.MessageStatus;
import id.xyz.chatapps_graph.framework.dto.AttachmentResponse;
import id.xyz.chatapps_graph.framework.dto.ForwardedInfo;
import id.xyz.chatapps_graph.framework.dto.MessageResponse;
import id.xyz.chatapps_graph.framework.dto.ReactionSummary;
import id.xyz.chatapps_graph.framework.dto.ReplyToResponse;
import java.util.List;

public class MessageMapper {

  private MessageMapper() {}

  public static MessageResponse toResponse(Message message, String senderUuid, String conversationUuid,
      AttachmentResponse attachment, ReplyToResponse replyTo,
      ForwardedInfo forwardedFrom, List<ReactionSummary> reactions) {
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
        .createdAt(message.getCreatedAt())
        .build();
  }

  public static ReplyToResponse toReplyResponse(Message replyMessage, String replySenderUuid) {
    if (replyMessage == null) {
      return null;
    }
    return new ReplyToResponse(replyMessage.getMessageUuid(), replySenderUuid, replyMessage.getContent());
  }
}
