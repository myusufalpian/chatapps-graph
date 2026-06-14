package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.RateLimitService;
import id.xyz.chatapps_graph.infrastructure.config.properties.AuthenticationOtpProperties;
import id.xyz.chatapps_graph.infrastructure.config.properties.ChatRateLimitProperties;
import java.time.Duration;
import java.time.Instant;
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
public class RateLimitServiceImpl implements RateLimitService {

  private static final String IP_RATE_KEY_PREFIX = "rate:ip:";
  private static final String COOLDOWN_KEY_PREFIX = "otp:cooldown:";

  private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT;

  static {
    SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>("""
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local limit = tonumber(ARGV[3])
        redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
        local count = redis.call('ZCARD', key)
        if count >= limit then
          return 1
        end
        redis.call('ZADD', key, now, now .. ':' .. math.random(100000))
        redis.call('EXPIRE', key, math.ceil(window / 1000))
        return 0
        """, Long.class);
  }

  private final StringRedisTemplate redisTemplate;
  private final AuthenticationOtpProperties otpProperties;
  private final ChatRateLimitProperties chatRateLimitProperties;

  @Override
  public boolean isIpRateLimited(String clientIp) {
    String key = IP_RATE_KEY_PREFIX + clientIp;
    long now = Instant.now().toEpochMilli();
    long windowMs = otpProperties.getIpWindowSeconds() * 1000L;

    Long result = redisTemplate.execute(SLIDING_WINDOW_SCRIPT, List.of(key),
        String.valueOf(now), String.valueOf(windowMs), String.valueOf(otpProperties.getMaxIpCount()));
    return Long.valueOf(1L).equals(result);
  }

  @Override
  public boolean isPhoneRateLimited(String phone) {
    String key = otpProperties.getPrefix() + phone + ":req_count";
    String count = redisTemplate.opsForValue().get(key);
    return ParsingUtil.parseIntSafe(count, 0) >= otpProperties.getMaxCount();
  }

  @Override
  public boolean isCooldownActive(String phone, String purpose) {
    String key = COOLDOWN_KEY_PREFIX + purpose + ":" + phone;
    return redisTemplate.hasKey(key);
  }

  @Override
  public void setCooldown(String phone, String purpose) {
    String key = COOLDOWN_KEY_PREFIX + purpose + ":" + phone;
    redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(otpProperties.getCooldownSeconds()));
  }

  @Override
  public boolean isChatRateLimited(Long userId) {
    String key = "rate:chat:" + userId;
    long now = Instant.now().toEpochMilli();
    long windowMs = 60_000L;
    Long result = redisTemplate.execute(SLIDING_WINDOW_SCRIPT, List.of(key),
        String.valueOf(now), String.valueOf(windowMs), String.valueOf(chatRateLimitProperties.getMessagePerMinute()));
    return Long.valueOf(1L).equals(result);
  }

  @Override
  public boolean isReactionRateLimited(Long userId) {
    String key = "rate:reaction:" + userId;
    long now = Instant.now().toEpochMilli();
    long windowMs = 60_000L;
    Long result = redisTemplate.execute(SLIDING_WINDOW_SCRIPT, List.of(key),
        String.valueOf(now), String.valueOf(windowMs), String.valueOf(chatRateLimitProperties.getReactionPerMinute()));
    return Long.valueOf(1L).equals(result);
  }
}
