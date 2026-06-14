package id.xyz.chatapps_graph.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conversation_participant")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "participant_id")
  private Long participantId;

  @Column(name = "conversation_id", nullable = false)
  private Long conversationId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "joined_at", updatable = false)
  private OffsetDateTime joinedAt;

  @Column(name = "last_message_at")
  private OffsetDateTime lastMessageAt;

  @Column(name = "last_message_preview", columnDefinition = "TEXT")
  private String lastMessagePreview;

  @Column(name = "last_message_type", length = 20)
  private String lastMessageType;

  @Column(name = "unread_count", nullable = false)
  @Builder.Default
  private Integer unreadCount = 0;

  @Column(name = "is_pinned", nullable = false)
  @Builder.Default
  private Boolean isPinned = false;

  @Column(name = "pinned_at")
  private OffsetDateTime pinnedAt;

  @Column(name = "is_archived", nullable = false)
  @Builder.Default
  private Boolean isArchived = false;

  @Column(name = "is_muted", nullable = false)
  @Builder.Default
  private Boolean isMuted = false;
}
