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
@Table(name = "\"group\"")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "group_id")
  private Long groupId;

  @Column(name = "group_uuid", length = 64, columnDefinition = "varchar(64) default gen_random_uuid()")
  private String groupUuid;

  @Column(name = "group_name", length = 255)
  private String groupName;

  @Column(name = "group_desc", columnDefinition = "TEXT")
  private String groupDesc;

  @Column(name = "is_active", columnDefinition = "integer default 0")
  private Integer isActive;
}
