package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.service.CachePort;
import id.xyz.chatapps_graph.applications.usecase.UserIdentityResolver;
import id.xyz.chatapps_graph.domain.entity.UserLinkedAccount;
import id.xyz.chatapps_graph.domain.repository.UserLinkedAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserIdentityResolverRegistryTest {

  @Mock private CachePort cachePort;
  @Mock private UserLinkedAccountRepository linkedAccountRepository;
  @Mock private UserIdentityResolver keycloakResolver;
  @Mock private UserIdentityResolver googleResolver;

  private UserIdentityResolverRegistry registry;

  private static final String KEYCLOAK_ISSUER = "http://localhost:8080/realms/chatapps";
  private static final String GOOGLE_ISSUER = "https://accounts.google.com";

  @BeforeEach
  void setUp() {
    when(keycloakResolver.provider()).thenReturn("KEYCLOAK");
    when(googleResolver.provider()).thenReturn("GOOGLE");

    id.xyz.chatapps_graph.infrastructure.config.properties.AppIdentityProperties props = new id.xyz.chatapps_graph.infrastructure.config.properties.AppIdentityProperties(3600L);
    id.xyz.chatapps_graph.infrastructure.config.properties.OAuth2JwtProperties oauthProps = new id.xyz.chatapps_graph.infrastructure.config.properties.OAuth2JwtProperties(KEYCLOAK_ISSUER);

    registry = new UserIdentityResolverRegistry(
        List.of(keycloakResolver, googleResolver),
        cachePort,
        linkedAccountRepository,
        props,
        oauthProps
    );
  }

  private Jwt buildJwt(String issuer, String sub) {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject(sub)
        .issuer(issuer)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300))
        .build();
  }

  @Test
  void identifiesKeycloakIssuer_resolvesViaKeycloakResolver() {
    Jwt jwt = buildJwt(KEYCLOAK_ISSUER, "kc-sub");
    when(cachePort.get("user:resolve:KEYCLOAK:kc-sub", String.class)).thenReturn(Optional.empty());
    when(keycloakResolver.resolve(jwt)).thenReturn(1L);
    when(cachePort.exists("user:banned:1")).thenReturn(false);

    Long result = registry.resolve(jwt);

    assertEquals(1L, result);
    verify(keycloakResolver).resolve(jwt);
  }

  @Test
  void identifiesGoogleIssuer_resolvesViaGoogleResolver() {
    Jwt jwt = buildJwt(GOOGLE_ISSUER, "google-sub");
    when(cachePort.get("user:resolve:GOOGLE:google-sub", String.class)).thenReturn(Optional.empty());
    when(googleResolver.resolve(jwt)).thenReturn(2L);
    when(cachePort.exists("user:banned:2")).thenReturn(false);

    Long result = registry.resolve(jwt);

    assertEquals(2L, result);
    verify(googleResolver).resolve(jwt);
  }

  @Test
  void unknownIssuer_returnsNull() {
    Jwt jwt = buildJwt("https://unknown.example.com", "unknown-sub");

    Long result = registry.resolve(jwt);

    assertNull(result);
  }

  @Test
  void cacheHit_doesNotQueryDb() {
    Jwt jwt = buildJwt(KEYCLOAK_ISSUER, "cached-sub");
    when(cachePort.get("user:resolve:KEYCLOAK:cached-sub", String.class)).thenReturn(Optional.of("42"));
    when(cachePort.exists("user:banned:42")).thenReturn(false);

    Long result = registry.resolve(jwt);

    assertEquals(42L, result);
    verify(keycloakResolver, never()).resolve(jwt);
  }

  @Test
  void cacheMiss_queriesDbAndCachesResult() {
    Jwt jwt = buildJwt(KEYCLOAK_ISSUER, "miss-sub");
    when(cachePort.get("user:resolve:KEYCLOAK:miss-sub", String.class)).thenReturn(Optional.empty());
    when(keycloakResolver.resolve(jwt)).thenReturn(77L);
    when(cachePort.exists("user:banned:77")).thenReturn(false);

    Long result = registry.resolve(jwt);

    assertEquals(77L, result);
    verify(cachePort).set("user:resolve:KEYCLOAK:miss-sub", "77", Duration.ofSeconds(3600));
  }

  @Test
  void invalidateUserCache_deletesAllProviderKeys() {
    UserLinkedAccount link1 = new UserLinkedAccount();
    link1.setProvider("KEYCLOAK");
    link1.setProviderSub("kc-sub-1");
    UserLinkedAccount link2 = new UserLinkedAccount();
    link2.setProvider("GOOGLE");
    link2.setProviderSub("g-sub-1");

    when(linkedAccountRepository.findAllByUserId(5L)).thenReturn(List.of(link1, link2));

    registry.invalidateUserCache(5L);

    verify(cachePort).delete("user:resolve:KEYCLOAK:kc-sub-1");
    verify(cachePort).delete("user:resolve:GOOGLE:g-sub-1");
  }

  @Test
  void userBanned_returnsNull_fromCache() {
    Jwt jwt = buildJwt(KEYCLOAK_ISSUER, "banned-sub");
    when(cachePort.get("user:resolve:KEYCLOAK:banned-sub", String.class)).thenReturn(Optional.of("99"));
    when(cachePort.exists("user:banned:99")).thenReturn(true);

    Long result = registry.resolve(jwt);

    assertNull(result);
  }

  @Test
  void userBanned_returnsNull_fromResolver() {
    Jwt jwt = buildJwt(KEYCLOAK_ISSUER, "banned-sub2");
    when(cachePort.get("user:resolve:KEYCLOAK:banned-sub2", String.class)).thenReturn(Optional.empty());
    when(keycloakResolver.resolve(jwt)).thenReturn(100L);
    when(cachePort.exists("user:banned:100")).thenReturn(true);

    Long result = registry.resolve(jwt);

    assertNull(result);
    verify(cachePort, never()).set("user:resolve:KEYCLOAK:banned-sub2", "100", Duration.ofSeconds(3600));
  }

  @Test
  void resolverReturnsNull_returnsNull() {
    Jwt jwt = buildJwt(KEYCLOAK_ISSUER, "null-sub");
    when(cachePort.get("user:resolve:KEYCLOAK:null-sub", String.class)).thenReturn(Optional.empty());
    when(keycloakResolver.resolve(jwt)).thenReturn(null);

    Long result = registry.resolve(jwt);

    assertNull(result);
  }
}
