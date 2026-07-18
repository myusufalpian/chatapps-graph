package id.xyz.chatapps_graph.domain.entity;

import id.xyz.chatapps_graph.domain.enums.ExportJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "export_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportJob {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "export_job_id")
  private Long exportJobId;

  @Column(name = "export_uuid", nullable = false, unique = true)
  private UUID exportUuid;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ExportJobStatus status;

  @Column(name = "format", nullable = false, length = 10)
  private String format;

  @Column(name = "snapshot_at", nullable = false)
  private OffsetDateTime snapshotAt;

  @Column(name = "storage_path", length = 1024)
  private String storagePath;

  @Column(name = "file_size")
  private Long fileSize;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  @Column(name = "retry_count", nullable = false)
  private Integer retryCount;

  @Column(name = "last_error_code", length = 100)
  private String lastErrorCode;

  @Column(name = "last_error_message", columnDefinition = "TEXT")
  private String lastErrorMessage;

  @Column(name = "processing_started_at")
  private OffsetDateTime processingStartedAt;

  @Column(name = "lease_expires_at")
  private OffsetDateTime leaseExpiresAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "started_at")
  private OffsetDateTime startedAt;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;

  @PrePersist
  void prePersist() {
    if (exportUuid == null) exportUuid = UUID.randomUUID();
    if (status == null) status = ExportJobStatus.PENDING;
    if (format == null) format = "JSON";
    if (retryCount == null) retryCount = 0;
    if (createdAt == null) createdAt = OffsetDateTime.now();
  }
}
