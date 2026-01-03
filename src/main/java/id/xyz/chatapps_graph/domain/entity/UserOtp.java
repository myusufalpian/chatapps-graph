package id.xyz.chatapps_graph.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "user_otp")
@Getter @Setter
public class UserOtp extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "otp_id")
  private Long otpId;

  @Column(name = "otp_uuid", length = 64, columnDefinition = "varchar(64) default gen_random_uuid()")
  private String otpUuid;

  @Column(name = "otp_code", length = 10)
  private String otpCode;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Jakarta")
  @Column(name = "exp_at")
  private java.util.Date expAt;

  @Column(name = "otp_status", columnDefinition = "integer default 0")
  private Integer otpStatus;
}
