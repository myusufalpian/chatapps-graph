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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Users")
@Getter
@Setter
public class User extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "user_uuid", length = 64, columnDefinition = "varchar(64) default gen_random_uuid()")
  private String userUuid;

  @Column(name = "user_phone", length = 20)
  private String userPhone;

  @Column(name = "user_mail", length = 100)
  private String userMail;

  @Column(name = "user_full_name", length = 255)
  private String userFullName;

  @Column(name = "profile_photo", columnDefinition = "TEXT")
  private String profilePhoto;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "about_id")
  private MstAbout about;

  @Column(name = "is_backup_active", columnDefinition = "integer default 0")
  private Integer isBackupActive;

  @Column(name = "user_status", columnDefinition = "integer default 0")
  private Integer userStatus;
}
