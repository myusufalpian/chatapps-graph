package id.xyz.chatapps_graph.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "message")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "message_id")
  private Long messageId;

  @Column(name = "message_uuid", length = 64)
  private String messageUuid;

  @Column(name = "conversation_id", nullable = false)
  private Long conversationId;

  @Column(name = "sender_id", nullable = false)
  private Long senderId;

  @Column(name = "message_type", length = 20, nullable = false)
  private String messageType;

  @Column(name = "content", columnDefinition = "TEXT")
  private String content;

  @Column(name = "attachment_id")
  private Long attachmentId;

  @Column(name = "reply_to_message_id")
  private Long replyToMessageId;

  @Column(name = "forwarded_from_id")
  private Long forwardedFromId;

  @Column(name = "message_status", nullable = false)
  private Integer messageStatus;

  @Column(name = "created_at", updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void prePersist() {
    if (messageUuid == null) messageUuid = UUID.randomUUID().toString();
    if (createdAt == null) createdAt = OffsetDateTime.now();
  }
}
