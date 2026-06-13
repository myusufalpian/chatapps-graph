package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.domain.entity.UserOtp;
import id.xyz.chatapps_graph.domain.enums.OtpPurpose;
import id.xyz.chatapps_graph.domain.repository.UserOtpRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OtpAuditServiceImplTest {

  @Mock private UserOtpRepository userOtpRepository;

  @InjectMocks private OtpAuditServiceImpl otpAuditService;

  private static final Long USER_ID = 1L;
  private static final String OTP = "482915";
  private static final OtpPurpose PURPOSE = OtpPurpose.SIGN_IN;
  private static final OffsetDateTime EXP_AT = OffsetDateTime.now().plusMinutes(1);

  // --- recordOtpGenerated ---

  @Test
  @DisplayName("recordOtpGenerated: saves with SHA-256 hashed OTP, not plaintext")
  void recordOtpGenerated_SavesWithHashedOtp() {
    ArgumentCaptor<UserOtp> captor = ArgumentCaptor.forClass(UserOtp.class);

    otpAuditService.recordOtpGenerated(USER_ID, OTP, PURPOSE, EXP_AT);

    verify(userOtpRepository).save(captor.capture());
    UserOtp saved = captor.getValue();
    assertNotEquals(OTP, saved.getOtpCode());
    assertEquals(expectedSha256(OTP), saved.getOtpCode());
  }

  @Test
  @DisplayName("recordOtpGenerated: sets otp_status = 0 (PENDING)")
  void recordOtpGenerated_SetsStatusPending() {
    ArgumentCaptor<UserOtp> captor = ArgumentCaptor.forClass(UserOtp.class);

    otpAuditService.recordOtpGenerated(USER_ID, OTP, PURPOSE, EXP_AT);

    verify(userOtpRepository).save(captor.capture());
    assertEquals(0, captor.getValue().getOtpStatus());
  }

  @Test
  @DisplayName("recordOtpGenerated: sets userId, purpose, expAt, createdAt correctly")
  void recordOtpGenerated_SetsAllFields() {
    ArgumentCaptor<UserOtp> captor = ArgumentCaptor.forClass(UserOtp.class);

    otpAuditService.recordOtpGenerated(USER_ID, OTP, PURPOSE, EXP_AT);

    verify(userOtpRepository).save(captor.capture());
    UserOtp saved = captor.getValue();
    assertEquals(USER_ID, saved.getUserId());
    assertEquals("SIGN_IN", saved.getPurpose());
    assertEquals(EXP_AT, saved.getExpAt());
    assertNotNull(saved.getCreatedAt());
  }

  @Test
  @DisplayName("recordOtpGenerated: DB error does not throw")
  void recordOtpGenerated_DbError_DoesNotThrow() {
    when(userOtpRepository.save(any())).thenThrow(new RuntimeException("DB down"));

    assertDoesNotThrow(() -> otpAuditService.recordOtpGenerated(USER_ID, OTP, PURPOSE, EXP_AT));
  }

  // --- markVerified ---

  @Test
  @DisplayName("markVerified: updates status to 1 and sets verifiedAt")
  void markVerified_UpdatesStatusAndVerifiedAt() {
    UserOtp pending = buildPendingRecord();
    when(userOtpRepository.findTopByUserIdAndPurposeAndOtpStatusOrderByCreatedAtDesc(USER_ID, "SIGN_IN", 0))
        .thenReturn(Optional.of(pending));

    otpAuditService.markVerified(USER_ID, PURPOSE);

    ArgumentCaptor<UserOtp> captor = ArgumentCaptor.forClass(UserOtp.class);
    verify(userOtpRepository).save(captor.capture());
    assertEquals(1, captor.getValue().getOtpStatus());
    assertNotNull(captor.getValue().getVerifiedAt());
    assertNotNull(captor.getValue().getUpdatedAt());
  }

  @Test
  @DisplayName("markVerified: no pending record — does not throw, does not save")
  void markVerified_NoPendingRecord_NoException() {
    when(userOtpRepository.findTopByUserIdAndPurposeAndOtpStatusOrderByCreatedAtDesc(USER_ID, "SIGN_IN", 0))
        .thenReturn(Optional.empty());

    assertDoesNotThrow(() -> otpAuditService.markVerified(USER_ID, PURPOSE));
    verify(userOtpRepository, never()).save(any());
  }

  @Test
  @DisplayName("markVerified: DB error does not throw")
  void markVerified_DbError_DoesNotThrow() {
    when(userOtpRepository.findTopByUserIdAndPurposeAndOtpStatusOrderByCreatedAtDesc(USER_ID, "SIGN_IN", 0))
        .thenThrow(new RuntimeException("DB down"));

    assertDoesNotThrow(() -> otpAuditService.markVerified(USER_ID, PURPOSE));
  }

  // --- markExpired ---

  @Test
  @DisplayName("markExpired: updates status to 2")
  void markExpired_UpdatesStatus() {
    UserOtp pending = buildPendingRecord();
    when(userOtpRepository.findTopByUserIdAndPurposeAndOtpStatusOrderByCreatedAtDesc(USER_ID, "SIGN_IN", 0))
        .thenReturn(Optional.of(pending));

    otpAuditService.markExpired(USER_ID, PURPOSE);

    ArgumentCaptor<UserOtp> captor = ArgumentCaptor.forClass(UserOtp.class);
    verify(userOtpRepository).save(captor.capture());
    assertEquals(2, captor.getValue().getOtpStatus());
    assertNotNull(captor.getValue().getUpdatedAt());
  }

  @Test
  @DisplayName("markExpired: no pending record — does not throw, does not save")
  void markExpired_NoPendingRecord_NoException() {
    when(userOtpRepository.findTopByUserIdAndPurposeAndOtpStatusOrderByCreatedAtDesc(USER_ID, "SIGN_IN", 0))
        .thenReturn(Optional.empty());

    assertDoesNotThrow(() -> otpAuditService.markExpired(USER_ID, PURPOSE));
    verify(userOtpRepository, never()).save(any());
  }

  // --- helpers ---

  private UserOtp buildPendingRecord() {
    UserOtp record = new UserOtp();
    record.setOtpId(100L);
    record.setUserId(USER_ID);
    record.setOtpStatus(0);
    record.setPurpose("SIGN_IN");
    record.setCreatedAt(OffsetDateTime.now().minusMinutes(1));
    return record;
  }

  private String expectedSha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
