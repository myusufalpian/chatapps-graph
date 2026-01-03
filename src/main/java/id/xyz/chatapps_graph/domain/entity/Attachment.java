package id.xyz.chatapps_graph.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "attachment")
@Getter
@Setter
public class Attachment extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attachment_id")
  private Long attachmentId;

  @Column(name = "attachment_uuid", length = 64, columnDefinition = "varchar(64) default gen_random_uuid()")
  private String attachmentUuid;

  @Column(name = "attachment_type", length = 50)
  private String attachmentType;

  @Column(name = "attachment_source", columnDefinition = "TEXT")
  private String attachmentSource;

  @Column(name = "is_active", columnDefinition = "integer default 0")
  private Integer isActive;
}
