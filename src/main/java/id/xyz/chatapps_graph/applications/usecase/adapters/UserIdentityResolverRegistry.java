package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.service.CachePort;
import id.xyz.chatapps_graph.applications.usecase.UserIdentityResolver;
import id.xyz.chatapps_graph.domain.entity.UserLinkedAccount;
import id.xyz.chatapps_graph.domain.enums.ProviderType;
import id.xyz.chatapps_graph.domain.repository.UserLinkedAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class UserIdentityResolverRegistry {

  private static final String CACHE_PREFIX = "user:resolve:";
  private static final String BANNED_PREFIX = "user:banned:";

  private final Map<String, UserIdentityResolver> resolverMap = new HashMap<>();
  private final CachePort cachePort;
  private final UserLinkedAccountRepository linkedAccountRepository;
  private final Duration cacheTtl;

  public UserIdentityResolverRegistry(
      List<UserIdentityResolver> resolvers,
      CachePort cachePort,
      UserLinkedAccountRepository linkedAccountRepository,
      @Value("${app.identity.cache-ttl-seconds:3600}") long cacheTtlSeconds,
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String keycloakIssuer) {

    this.cachePort = cachePort;
    this.linkedAccountRepository = linkedAccountRepository;
    this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);

    for (UserIdentityResolver resolver : resolvers) {
      if (resolver.provider().equals(ProviderType.KEYCLOAK.name())) {
        resolverMap.put(keycloakIssuer, resolver);
      } else if (resolver.provider().equals(ProviderType.GOOGLE.name())) {
        resolverMap.put("https://accounts.google.com", resolver);
      }
    }
  }

  public Long resolve(Jwt jwt) {
    try {
      String issuer = jwt.getIssuer().toString();
      String sub = jwt.getSubject();

      UserIdentityResolver resolver = resolverMap.get(issuer);
      if (resolver == null) {
        log.warn("No resolver found for issuer: {}", issuer);
        return null;
      }

      String provider = resolver.provider();
      String cacheKey = CACHE_PREFIX + provider + ":" + sub;

      // Check cache first
      Optional<String> cached = cachePort.get(cacheKey, String.class);
      if (cached.isPresent()) {
        Long userId = Long.valueOf(cached.get());
        if (isBanned(userId)) {
          return null;
        }
        return userId;
      }

      // Resolve from DB
      Long userId = resolver.resolve(jwt);
      if (userId == null) {
        return null;
      }

      // Check banned
      if (isBanned(userId)) {
        return null;
      }

      // Cache result
      cachePort.set(cacheKey, userId.toString(), cacheTtl);
      return userId;

    } catch (Exception e) {
      log.error("Error resolving user identity: {}", e.getMessage(), e);
      return null;
    }
  }

  public boolean isBanned(Long userId) {
    return cachePort.exists(BANNED_PREFIX + userId);
  }

  public void invalidateUserCache(Long userId) {
    try {
      List<UserLinkedAccount> accounts = linkedAccountRepository.findAllByUserId(userId);
      for (UserLinkedAccount account : accounts) {
        String cacheKey = CACHE_PREFIX + account.getProvider() + ":" + account.getProviderSub();
        cachePort.delete(cacheKey);
      }
    } catch (Exception e) {
      log.error("Error invalidating cache for userId={}: {}", userId, e.getMessage(), e);
    }
  }
}
