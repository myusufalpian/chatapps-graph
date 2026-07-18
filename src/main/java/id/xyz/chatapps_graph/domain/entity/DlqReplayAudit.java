package id.xyz.chatapps_graph.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dlq_replay_audits")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqReplayAudit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "audit_id")
  private Long auditId;

  @Column(name = "operator_id", nullable = false)
  private Long operatorId;

  @Column(name = "task_id", nullable = false, length = 255)
  private String taskId;

  @Column(name = "reason", nullable = false, length = 255)
  private String reason;

  @Column(name = "old_status", nullable = false, length = 20)
  private String oldStatus;

  @Column(name = "new_status", nullable = false, length = 20)
  private String newStatus;

  @Column(name = "correlation_id", length = 255)
  private String correlationId;

  @Column(name = "replayed_at", nullable = false)
  private OffsetDateTime replayedAt;

  @PrePersist
  void prePersist() {
    if (replayedAt == null) replayedAt = OffsetDateTime.now();
  }
}
