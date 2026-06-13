package id.xyz.chatapps_graph.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mst_about")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MstAbout extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "about_id")
  private Long aboutId;

  @Column(name = "about_uuid", length = 64, columnDefinition = "varchar(64) default gen_random_uuid()")
  private String aboutUuid;

  @Column(name = "about_desc", columnDefinition = "TEXT")
  private String aboutDesc;

  @Column(name = "is_active", columnDefinition = "integer default 0")
  private Integer isActive;
}
