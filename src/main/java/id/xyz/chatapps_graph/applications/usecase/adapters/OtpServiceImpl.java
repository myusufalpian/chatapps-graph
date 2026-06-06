package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.OtpService;
import id.xyz.chatapps_graph.domain.enums.OtpPurpose;
import id.xyz.chatapps_graph.infrastructure.config.properties.AuthenticationOtpProperties;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import id.xyz.chatapps_graph.infrastructure.utility.ParsingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

  private static final String ATTEMPT_SUFFIX = ":attempts";
  private static final String REQUEST_COUNT_SUFFIX = ":req_count";

  // Atomically increments request count and sets TTL only on first increment
  private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL_SCRIPT;
  static {
    INCREMENT_WITH_TTL_SCRIPT = new DefaultRedisScript<>("""
        local count = redis.call('INCR', KEYS[1])
        if count == 1 then
          redis.call('EXPIRE', KEYS[1], ARGV[1])
        end
        return count
        """, Long.class);
  }

  // Validates OTP atomically: checks attempts, verifies code, handles cleanup/increment
  // Returns: 1 = valid, 0 = invalid (incremented attempts), -1 = max attempts exceeded
  private static final DefaultRedisScript<Long> VALIDATE_OTP_SCRIPT;
  static {
    VALIDATE_OTP_SCRIPT = new DefaultRedisScript<>("""
        local otpKey = KEYS[1]
        local attemptKey = KEYS[2]
        local inputOtp = ARGV[1]
        local maxAttempts = tonumber(ARGV[2])
        local ttl = tonumber(ARGV[3])
        local attempts = tonumber(redis.call('GET', attemptKey) or '0')
        if attempts >= maxAttempts then
          redis.call('DEL', otpKey, attemptKey)
          return -1
        end
        local savedOtp = redis.call('GET', otpKey)
        if savedOtp and savedOtp == inputOtp then
          redis.call('DEL', otpKey, attemptKey)
          return 1
        end
        redis.call('INCR', attemptKey)
        redis.call('EXPIRE', attemptKey, ttl)
        return 0
        """, Long.class);
  }

  private final StringRedisTemplate redisTemplate;
  private final SecureRandom secureRandom = new SecureRandom();
  private final AuthenticationOtpProperties authenticationOtpProperties;

  @Override
  public boolean isRateLimited(String phone) {
    String key = authenticationOtpProperties.getPrefix() + phone + REQUEST_COUNT_SUFFIX;
    String count = redisTemplate.opsForValue().get(key);
    return ParsingUtil.parseIntSafe(count, 0) >= authenticationOtpProperties.getMaxCount();
  }

  @Override
  public String generateAndSaveOtp(String phone, OtpPurpose purpose) {
    long ttlSeconds = authenticationOtpProperties.getDuration() * 60L;
    String purposeKey = buildKey(phone, purpose);
    String reqKey = authenticationOtpProperties.getPrefix() + phone + REQUEST_COUNT_SUFFIX;

    // Atomic increment with TTL set only on first request
    redisTemplate.execute(INCREMENT_WITH_TTL_SCRIPT, List.of(reqKey), String.valueOf(ttlSeconds));

    int bound = (int) Math.pow(10, authenticationOtpProperties.getOtpLength());
    String format = "%0" + authenticationOtpProperties.getOtpLength() + "d";
    String otp = String.format(format, secureRandom.nextInt(bound));

    redisTemplate.opsForValue().set(purposeKey, otp, Duration.ofSeconds(ttlSeconds));
    redisTemplate.delete(purposeKey + ATTEMPT_SUFFIX);
    return otp;
  }

  @Override
  public boolean validateOtp(String phone, String inputOtp, OtpPurpose purpose) {
    String purposeKey = buildKey(phone, purpose);
    String attemptKey = purposeKey + ATTEMPT_SUFFIX;
    long ttlSeconds = authenticationOtpProperties.getDuration() * 60L;

    // Single round-trip: check attempts, verify OTP, cleanup or increment
    Long result = redisTemplate.execute(VALIDATE_OTP_SCRIPT,
        List.of(purposeKey, attemptKey),
        inputOtp,
        String.valueOf(authenticationOtpProperties.getMaxAttempts()),
        String.valueOf(ttlSeconds));

    return result == 1L;
  }

  private String buildKey(String phone, OtpPurpose purpose) {
    return authenticationOtpProperties.getPrefix() + purpose.name().toLowerCase() + ":" + phone;
  }


}
