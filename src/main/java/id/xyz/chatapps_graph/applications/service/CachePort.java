package id.xyz.chatapps_graph.applications.service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public interface CachePort {

  void set(String key, Object value, Duration ttl);

  void set(String key, Object value);

  <T> Optional<T> get(String key, Class<T> clazz);

  void delete(String key);

  boolean exists(String key);

  void addToSet(String key, Object... values);

  <T> Set<T> getSetMembers(String key, Class<T> clazz);
}
