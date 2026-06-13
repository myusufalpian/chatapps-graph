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
@Table(name = "attachment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attachment_id")
  private Long attachmentId;

  @Column(name = "attachment_uuid", length = 64)
  private String attachmentUuid;

  @Column(name = "uploader_id", nullable = false)
  private Long uploaderId;

  @Column(name = "file_name", length = 255, nullable = false)
  private String fileName;

  @Column(name = "file_path", columnDefinition = "TEXT", nullable = false)
  private String filePath;

  @Column(name = "file_size", nullable = false)
  private Long fileSize;

  @Column(name = "content_type", length = 100, nullable = false)
  private String contentType;

  @Column(name = "attachment_type", length = 20, nullable = false)
  private String attachmentType;

  @Column(name = "created_at", updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void prePersist() {
    if (attachmentUuid == null) attachmentUuid = UUID.randomUUID().toString();
    if (createdAt == null) createdAt = OffsetDateTime.now();
  }
}
