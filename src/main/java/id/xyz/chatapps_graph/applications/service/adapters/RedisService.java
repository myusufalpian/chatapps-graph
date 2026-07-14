package id.xyz.chatapps_graph.applications.service.adapters;

import id.xyz.chatapps_graph.applications.service.CachePort;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants.ErrorKeyConstants;
import id.xyz.chatapps_graph.infrastructure.constant.GeneralConstants.LoggingConstants;
import id.xyz.chatapps_graph.infrastructure.utility.ExceptionUtil;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RedisService implements CachePort {

  private final RedisTemplate<String, Object> redisTemplate;

  @Autowired
  public RedisService(@org.springframework.beans.factory.annotation.Qualifier("objectRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void set(String key, Object value, Duration ttl) {
    try {
      redisTemplate.opsForValue().set(key, value, ttl);
      log.debug(LoggingConstants.REDIS_SET_WITH_TTL, key, ttl.getSeconds());
    } catch (Exception e) {
      log.error(ErrorConstants.LoggingConstants.FAILED_SET_KEY_REDIS, key, e);
      throw ExceptionUtil.error(ErrorConstants.INTERNAL_SERVER_ERROR,
          String.format(ErrorKeyConstants.ADD_DATA_TO_REDIS_FAILED, e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
    }
  }

  @Override
  public void set(String key, Object value) {
    try {
      redisTemplate.opsForValue().set(key, value);
    } catch (Exception e) {
      log.error(ErrorConstants.LoggingConstants.FAILED_SET_KEY_REDIS, key, e);
      throw ExceptionUtil.error(ErrorConstants.INTERNAL_SERVER_ERROR,
          String.format(ErrorKeyConstants.ADD_DATA_TO_REDIS_FAILED, e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
    }
  }

  @Override
  public <T> Optional<T> get(String key, Class<T> clazz) {
    try {
      Object value = redisTemplate.opsForValue().get(key);
      if (value == null) {
        return Optional.empty();
      }
      return Optional.of(clazz.cast(value));
    } catch (Exception e) {
      log.error(ErrorConstants.LoggingConstants.FAILED_GET_KEY_FROM_REDIS, key, e);
      return Optional.empty();
    }
  }

  @Override
  public void delete(String key) {
    try {
      redisTemplate.delete(key);
    } catch (Exception e) {
      log.error(ErrorConstants.LoggingConstants.FAILED_DELETE_KEY_FROM_REDIS, key, e);
    }
  }

  @Override
  public boolean exists(String key) {
    try {
      return redisTemplate.hasKey(key);
    } catch (Exception e) {
      log.error(ErrorConstants.LoggingConstants.FAILED_CHECK_KEY_FROM_REDIS, key, e);
      return false;
    }
  }

  @Override
  public void addToSet(String key, Object... values) {
    try {
      if (values != null && values.length > 0) {
        redisTemplate.opsForSet().add(key, values);
        log.debug(LoggingConstants.ADDED_ITEMS_TO_SET, values.length, key);
      }
    } catch (Exception e) {
      log.error(ErrorConstants.LoggingConstants.FAILED_ADD_TO_SET_KEY_REDIS, key, e);
      throw ExceptionUtil.error(ErrorConstants.INTERNAL_SERVER_ERROR, ErrorKeyConstants.ADD_SET_REDIS_FAILED,
          HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
    }
  }

  @Override
  public <T> Set<T> getSetMembers(String key, Class<T> clazz) {
    try {
      Set<Object> members = redisTemplate.opsForSet().members(key);
      if (members == null || members.isEmpty()) {
        return Collections.emptySet();
      }
      return members.stream()
          .map(clazz::cast)
          .collect(Collectors.toSet());
    } catch (Exception e) {
      log.error(ErrorConstants.LoggingConstants.FAILED_GET_SET_MEMBER_FROM_REDIS, key, e);
      return Collections.emptySet();
    }
  }
}
