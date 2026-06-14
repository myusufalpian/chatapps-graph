package id.xyz.chatapps_graph.infrastructure.mapper;

import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.enums.MessageStatus;
import id.xyz.chatapps_graph.framework.dto.AttachmentResponse;
import id.xyz.chatapps_graph.framework.dto.MessageResponse;
import id.xyz.chatapps_graph.framework.dto.ReplyToResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MessageMapperTest {

  private static final OffsetDateTime CREATED = OffsetDateTime.of(2026, 6, 13, 10, 0, 0, 0, ZoneOffset.UTC);

  @Test
  @DisplayName("toResponse: maps all fields correctly")
  void toResponse_MapsAllFields() {
    Message message = Message.builder()
        .messageUuid("msg-uuid")
        .messageType("TEXT")
        .content("Hello")
        .messageStatus(MessageStatus.ACTIVE.getValue())
        .createdAt(CREATED)
        .build();

    MessageResponse result = MessageMapper.toResponse(message, "sender-uuid", "conv-uuid", null, null, null, null);

    assertEquals("msg-uuid", result.messageUuid());
    assertEquals("conv-uuid", result.conversationUuid());
    assertEquals("sender-uuid", result.senderUuid());
    assertEquals("TEXT", result.messageType());
    assertEquals("Hello", result.content());
    assertEquals(0, result.status());
    assertEquals(CREATED, result.createdAt());
    assertNull(result.attachment());
    assertNull(result.replyTo());
  }

  @Test
  @DisplayName("toResponse: deleted message has null content")
  void toResponse_DeletedMessage_ContentNull() {
    Message message = Message.builder()
        .messageUuid("msg-uuid")
        .messageType("TEXT")
        .content("Secret")
        .messageStatus(MessageStatus.DELETED.getValue())
        .createdAt(CREATED)
        .build();

    MessageResponse result = MessageMapper.toResponse(message, "sender", "conv", null, null, null, null);

    assertNull(result.content());
    assertEquals(1, result.status());
  }

  @Test
  @DisplayName("toResponse: with attachment maps attachment")
  void toResponse_WithAttachment_MapsAttachment() {
    Message message = Message.builder()
        .messageUuid("msg-uuid")
        .messageType("IMAGE")
        .messageStatus(MessageStatus.ACTIVE.getValue())
        .createdAt(CREATED)
        .build();
    AttachmentResponse att = new AttachmentResponse("att-uuid", "photo.jpg", "chat/u/1_photo.jpg", 1024L, "image/jpeg", "IMAGE");

    MessageResponse result = MessageMapper.toResponse(message, "sender", "conv", att, null, null, null);

    assertNotNull(result.attachment());
    assertEquals("att-uuid", result.attachment().attachmentUuid());
    assertEquals("photo.jpg", result.attachment().fileName());
  }

  @Test
  @DisplayName("toResponse: with reply maps reply preview")
  void toResponse_WithReply_MapsReplyPreview() {
    Message message = Message.builder()
        .messageUuid("msg-uuid")
        .messageType("TEXT")
        .content("Reply text")
        .messageStatus(MessageStatus.ACTIVE.getValue())
        .createdAt(CREATED)
        .build();
    ReplyToResponse reply = new ReplyToResponse("reply-uuid", "original-sender", "Original message");

    MessageResponse result = MessageMapper.toResponse(message, "sender", "conv", null, reply, null, null);

    assertNotNull(result.replyTo());
    assertEquals("reply-uuid", result.replyTo().messageUuid());
    assertEquals("original-sender", result.replyTo().senderUuid());
    assertEquals("Original message", result.replyTo().content());
  }

  @Test
  @DisplayName("toReplyResponse: null message returns null")
  void toReplyResponse_NullMessage_ReturnsNull() {
    assertNull(MessageMapper.toReplyResponse(null, "sender"));
  }

  @Test
  @DisplayName("toReplyResponse: maps message to reply preview")
  void toReplyResponse_MapsCorrectly() {
    Message replyMsg = Message.builder()
        .messageUuid("reply-uuid")
        .content("Original")
        .build();

    ReplyToResponse result = MessageMapper.toReplyResponse(replyMsg, "sender-uuid");

    assertEquals("reply-uuid", result.messageUuid());
    assertEquals("sender-uuid", result.senderUuid());
    assertEquals("Original", result.content());
  }
}
