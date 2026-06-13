package id.xyz.chatapps_graph.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "about_user")
@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class AboutUser extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "about_user_id")
  private Long aboutUserId;

  @Column(name = "about_user_uuid", length = 64, columnDefinition = "varchar(64) default gen_random_uuid()")
  private String aboutUserUuid;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "about_desc", columnDefinition = "TEXT")
  private String aboutDesc;
}
