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
@Table(name = "user_otp")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOtp extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "otp_id")
  private Long otpId;

  @Column(name = "otp_uuid", length = 64, columnDefinition = "varchar(64) default gen_random_uuid()")
  private String otpUuid;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "otp_code", length = 64)
  private String otpCode;

  @Column(name = "purpose", length = 20)
  private String purpose;

  @Column(name = "exp_at")
  private OffsetDateTime expAt;

  @Column(name = "verified_at")
  private OffsetDateTime verifiedAt;

  @Column(name = "otp_status", columnDefinition = "integer default 0")
  private Integer otpStatus;
}
