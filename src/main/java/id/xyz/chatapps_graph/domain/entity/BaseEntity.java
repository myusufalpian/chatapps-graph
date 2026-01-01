package id.xyz.chatapps_graph.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {
  @Column(name = "created_at", updatable = false)
  @JsonFormat(pattern = "yyyy-MM-dd HH24:mm:ss", timezone = "Asia/Jakarta")
  private OffsetDateTime createdAt;

  @Column(name = "created_by")
  private String createdBy;

  @Column(name = "updated_at")
  @JsonFormat(pattern = "yyyy-MM-dd HH24:mm:ss", timezone = "Asia/Jakarta")
  private OffsetDateTime updatedAt;

  @Column(name = "updated_by")
  private String updatedBy;
}
