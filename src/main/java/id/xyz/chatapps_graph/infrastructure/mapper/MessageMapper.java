package id.xyz.chatapps_graph.infrastructure.mapper;

import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.enums.MessageStatus;
import id.xyz.chatapps_graph.framework.dto.AttachmentResponse;
import id.xyz.chatapps_graph.framework.dto.MessageResponse;
import id.xyz.chatapps_graph.framework.dto.ReplyToResponse;

public class MessageMapper {

  private MessageMapper() {}

  public static MessageResponse toResponse(Message message, String senderUuid, String conversationUuid,
      AttachmentResponse attachment, ReplyToResponse replyTo) {
    String content = message.getMessageStatus() == MessageStatus.DELETED.getValue() ? null : message.getContent();
    return new MessageResponse(
        message.getMessageUuid(),
        conversationUuid,
        senderUuid,
        message.getMessageType(),
        content,
        attachment,
        replyTo,
        message.getMessageStatus(),
        message.getCreatedAt()
    );
  }

  public static ReplyToResponse toReplyResponse(Message replyMessage, String replySenderUuid) {
    if (replyMessage == null) {
      return null;
    }
    return new ReplyToResponse(replyMessage.getMessageUuid(), replySenderUuid, replyMessage.getContent());
  }
}
