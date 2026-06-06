package id.xyz.chatapps_graph.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.locks.LockSupport;

import id.xyz.chatapps_graph.applications.usecase.RateLimitService;
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
class RateLimitServiceImplIntegrationTest extends RedisIntegrationBase {

  @Autowired private RateLimitService rateLimitService;
  @Autowired private StringRedisTemplate redisTemplate;

  private static final String PHONE = "+628123456789";
  private static final String CLIENT_IP = "192.168.1.100";

  @BeforeEach
  void flush() {
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  // --- isIpRateLimited (sliding window) ---

  @Test
  @DisplayName("isIpRateLimited: returns false for first request")
  void isIpRateLimited_FirstRequest_ReturnsFalse() {
    assertFalse(rateLimitService.isIpRateLimited(CLIENT_IP));
  }

  @Test
  @DisplayName("isIpRateLimited: returns false when under limit (max-ip-count=5)")
  void isIpRateLimited_UnderLimit_ReturnsFalse() {
    for (int i = 0; i < 4; i++) {
      assertFalse(rateLimitService.isIpRateLimited(CLIENT_IP));
    }
  }

  @Test
  @DisplayName("isIpRateLimited: returns true on the Nth call when limit reached")
  void isIpRateLimited_AtLimit_ReturnsTrue() {
    // max-ip-count=5: first 5 calls add to the window and return false, 6th returns true
    for (int i = 0; i < 5; i++) {
      assertFalse(rateLimitService.isIpRateLimited(CLIENT_IP), "Call " + (i + 1) + " should not be limited");
    }
    assertTrue(rateLimitService.isIpRateLimited(CLIENT_IP), "Call 6 should be rate limited");
  }

  @Test
  @DisplayName("isIpRateLimited: different IPs have independent limits")
  void isIpRateLimited_DifferentIps_Independent() {
    for (int i = 0; i < 5; i++) {
      rateLimitService.isIpRateLimited("10.0.0.1");
    }
    assertTrue(rateLimitService.isIpRateLimited("10.0.0.1"));
    assertFalse(rateLimitService.isIpRateLimited("10.0.0.2"));
  }

  // --- isPhoneRateLimited ---

  @Test
  @DisplayName("isPhoneRateLimited: returns false when no requests made")
  void isPhoneRateLimited_NoRequests_ReturnsFalse() {
    assertFalse(rateLimitService.isPhoneRateLimited(PHONE));
  }

  @Test
  @DisplayName("isPhoneRateLimited: returns true when counter reaches max-count (3)")
  void isPhoneRateLimited_AtMaxCount_ReturnsTrue() {
    // Simulate counter being set (normally done by OtpService.generateAndSaveOtp)
    redisTemplate.opsForValue().set("otp:" + PHONE + ":req_count", "3");

    assertTrue(rateLimitService.isPhoneRateLimited(PHONE));
  }

  @Test
  @DisplayName("isPhoneRateLimited: returns false when counter below max")
  void isPhoneRateLimited_BelowMax_ReturnsFalse() {
    redisTemplate.opsForValue().set("otp:" + PHONE + ":req_count", "2");

    assertFalse(rateLimitService.isPhoneRateLimited(PHONE));
  }

  // --- cooldown ---

  @Test
  @DisplayName("setCooldown then isCooldownActive: returns true immediately")
  void cooldown_SetThenCheck_Active() {
    rateLimitService.setCooldown(PHONE, "sign_in");

    assertTrue(rateLimitService.isCooldownActive(PHONE, "sign_in"));
  }

  @Test
  @DisplayName("isCooldownActive: returns false when no cooldown set")
  void cooldown_NotSet_ReturnsFalse() {
    assertFalse(rateLimitService.isCooldownActive(PHONE, "sign_in"));
  }

  @Test
  @DisplayName("cooldown: different purposes are independent")
  void cooldown_DifferentPurposes_Independent() {
    rateLimitService.setCooldown(PHONE, "sign_in");

    assertTrue(rateLimitService.isCooldownActive(PHONE, "sign_in"));
    assertFalse(rateLimitService.isCooldownActive(PHONE, "mfa"));
  }

  @Test
  @DisplayName("cooldown: expires after TTL (cooldown-seconds=2)")
  void cooldown_ExpiresAfterTtl() {
    rateLimitService.setCooldown(PHONE, "sign_in");
    assertTrue(rateLimitService.isCooldownActive(PHONE, "sign_in"));

    // Poll until cooldown expires (TTL=2s, deadline=3s)
    long deadline = System.currentTimeMillis() + 3000;
    while (rateLimitService.isCooldownActive(PHONE, "sign_in")) {
      if (System.currentTimeMillis() > deadline) {
        fail("Cooldown did not expire within 3 seconds");
      }
      LockSupport.parkNanos(100_000_000); // 100ms
    }

    assertFalse(rateLimitService.isCooldownActive(PHONE, "sign_in"));
  }
}
