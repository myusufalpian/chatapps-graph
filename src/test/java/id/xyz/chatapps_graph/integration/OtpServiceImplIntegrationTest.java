package id.xyz.chatapps_graph.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import id.xyz.chatapps_graph.applications.usecase.OtpService;
import id.xyz.chatapps_graph.domain.enums.OtpPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration")
@EnabledIf("isDockerAvailable")
class OtpServiceImplIntegrationTest extends RedisIntegrationBase {

  @Autowired private OtpService otpService;
  @Autowired private StringRedisTemplate redisTemplate;

  private static final String PHONE = "+628123456789";

  @BeforeEach
  void flush() {
    assertNotNull(redisTemplate.getConnectionFactory());
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  // --- generateAndSaveOtp ---

  @Test
  @DisplayName("generateAndSaveOtp: stores OTP in Redis with correct key")
  void generateAndSaveOtp_StoresInRedis() {
    String otp = otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);

    String stored = redisTemplate.opsForValue().get("otp:sign_in:" + PHONE);
    assertEquals(otp, stored);
  }

  @Test
  @DisplayName("generateAndSaveOtp: OTP has correct length and is numeric")
  void generateAndSaveOtp_CorrectFormat() {
    String otp = otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);

    assertEquals(6, otp.length());
    assertTrue(otp.matches("\\d{6}"));
  }

  @Test
  @DisplayName("generateAndSaveOtp: key has TTL set (duration=1min → 60s)")
  void generateAndSaveOtp_HasTtl() {
    otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);

    Long ttl = redisTemplate.getExpire("otp:sign_in:" + PHONE);
    assertNotNull(ttl);
    assertTrue(ttl > 0 && ttl <= 60, "TTL should be between 1 and 60 seconds, was: " + ttl);
  }

  @Test
  @DisplayName("generateAndSaveOtp: uses different keys per purpose")
  void generateAndSaveOtp_DifferentKeysPerPurpose() {
    String signInOtp = otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);
    String mfaOtp = otpService.generateAndSaveOtp(PHONE, OtpPurpose.MFA);

    assertEquals(signInOtp, redisTemplate.opsForValue().get("otp:sign_in:" + PHONE));
    assertEquals(mfaOtp, redisTemplate.opsForValue().get("otp:mfa:" + PHONE));
  }

  @Test
  @DisplayName("generateAndSaveOtp: increments request counter")
  void generateAndSaveOtp_IncrementsCounter() {
    otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);
    otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);

    String count = redisTemplate.opsForValue().get("otp:" + PHONE + ":req_count");
    assertEquals("2", count);
  }

  @Test
  @DisplayName("generateAndSaveOtp: clears attempt counter on new OTP")
  void generateAndSaveOtp_ClearsAttempts() {
    // Simulate a failed validation to create attempt key
    otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);
    otpService.validateOtp(PHONE, "wrong!", OtpPurpose.SIGN_IN);
    assertNotNull(redisTemplate.opsForValue().get("otp:sign_in:" + PHONE + ":attempts"));

    // Generate new OTP should clear attempts
    otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);
    assertNull(redisTemplate.opsForValue().get("otp:sign_in:" + PHONE + ":attempts"));
  }

  // --- validateOtp ---

  @Test
  @DisplayName("validateOtp: returns true and deletes key for correct OTP")
  void validateOtp_CorrectOtp_DeletesKey() {
    String otp = otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);

    boolean result = otpService.validateOtp(PHONE, otp, OtpPurpose.SIGN_IN);

    assertTrue(result);
    assertNull(redisTemplate.opsForValue().get("otp:sign_in:" + PHONE));
  }

  @Test
  @DisplayName("validateOtp: returns false for wrong OTP and increments attempts")
  void validateOtp_WrongOtp_IncrementsAttempts() {
    otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);

    boolean result = otpService.validateOtp(PHONE, "wrong!", OtpPurpose.SIGN_IN);

    assertFalse(result);
    String attempts = redisTemplate.opsForValue().get("otp:sign_in:" + PHONE + ":attempts");
    assertEquals("1", attempts);
  }

  @Test
  @DisplayName("validateOtp: after max attempts (3), deletes OTP and attempt keys")
  void validateOtp_MaxAttempts_DeletesAll() {
    String otp = otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);

    // Exhaust attempts (max-attempts=3 in integration profile)
    otpService.validateOtp(PHONE, "wrong1", OtpPurpose.SIGN_IN);
    otpService.validateOtp(PHONE, "wrong2", OtpPurpose.SIGN_IN);
    boolean thirdAttempt = otpService.validateOtp(PHONE, "wrong3", OtpPurpose.SIGN_IN);

    assertFalse(thirdAttempt);
    // Both keys should be deleted
    assertNull(redisTemplate.opsForValue().get("otp:sign_in:" + PHONE));
    assertNull(redisTemplate.opsForValue().get("otp:sign_in:" + PHONE + ":attempts"));

    // Even correct OTP should fail now
    assertFalse(otpService.validateOtp(PHONE, otp, OtpPurpose.SIGN_IN));
  }

  @Test
  @DisplayName("validateOtp: returns false when no OTP was generated")
  void validateOtp_NoOtpGenerated_ReturnsFalse() {
    assertFalse(otpService.validateOtp(PHONE, "123456", OtpPurpose.SIGN_IN));
  }

  // --- isRateLimited ---

  @Test
  @DisplayName("isRateLimited: returns false when under limit")
  void isRateLimited_UnderLimit_ReturnsFalse() {
    otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);
    otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);

    assertFalse(otpService.isRateLimited(PHONE));
  }

  @Test
  @DisplayName("isRateLimited: returns true when max-count (3) reached")
  void isRateLimited_AtLimit_ReturnsTrue() {
    // max-count=3 in integration profile
    otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);
    otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);
    otpService.generateAndSaveOtp(PHONE, OtpPurpose.SIGN_IN);

    assertTrue(otpService.isRateLimited(PHONE));
  }
}
