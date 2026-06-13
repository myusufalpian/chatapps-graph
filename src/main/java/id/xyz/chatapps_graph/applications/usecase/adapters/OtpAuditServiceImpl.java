package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.OtpAuditService;
import id.xyz.chatapps_graph.domain.entity.UserOtp;
import id.xyz.chatapps_graph.domain.enums.OtpPurpose;
import id.xyz.chatapps_graph.domain.repository.UserOtpRepository;
import id.xyz.chatapps_graph.infrastructure.utility.HashUtil;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpAuditServiceImpl implements OtpAuditService {

  private static final int STATUS_PENDING = 0;
  private static final int STATUS_VERIFIED = 1;
  private static final int STATUS_EXPIRED = 2;

  private final UserOtpRepository userOtpRepository;

  @Override
  public void recordOtpGenerated(Long userId, String otp, OtpPurpose purpose, OffsetDateTime expAt) {
    try {
      UserOtp userOtp = UserOtp.builder()
          .userId(userId)
          .otpCode(HashUtil.sha256(otp))
          .purpose(purpose.name())
          .expAt(expAt)
          .otpStatus(STATUS_PENDING)
          .build();
      userOtp.setCreatedAt(OffsetDateTime.now());
      userOtpRepository.save(userOtp);
    } catch (Exception e) {
      log.error("Failed to record OTP audit for userId [{}]: {}", userId, e.getMessage());
    }
  }

  @Override
  public void markVerified(Long userId, OtpPurpose purpose) {
    try {
      userOtpRepository.findTopByUserIdAndPurposeAndOtpStatusOrderByCreatedAtDesc(
          userId, purpose.name(), STATUS_PENDING)
          .ifPresent(userOtp -> {
            userOtp.setOtpStatus(STATUS_VERIFIED);
            userOtp.setVerifiedAt(OffsetDateTime.now());
            userOtp.setUpdatedAt(OffsetDateTime.now());
            userOtpRepository.save(userOtp);
          });
    } catch (Exception e) {
      log.error("Failed to mark OTP verified for userId [{}]: {}", userId, e.getMessage());
    }
  }

  @Override
  public void markExpired(Long userId, OtpPurpose purpose) {
    try {
      userOtpRepository.findTopByUserIdAndPurposeAndOtpStatusOrderByCreatedAtDesc(
          userId, purpose.name(), STATUS_PENDING)
          .ifPresent(userOtp -> {
            userOtp.setOtpStatus(STATUS_EXPIRED);
            userOtp.setUpdatedAt(OffsetDateTime.now());
            userOtpRepository.save(userOtp);
          });
    } catch (Exception e) {
      log.error("Failed to mark OTP expired for userId [{}]: {}", userId, e.getMessage());
    }
  }
}
