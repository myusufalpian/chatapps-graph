package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.domain.enums.OtpPurpose;
import java.time.OffsetDateTime;

public interface OtpAuditService {
  void recordOtpGenerated(Long userId, String otp, OtpPurpose purpose, OffsetDateTime expAt);
  void markVerified(Long userId, OtpPurpose purpose);
  void markExpired(Long userId, OtpPurpose purpose);
}
