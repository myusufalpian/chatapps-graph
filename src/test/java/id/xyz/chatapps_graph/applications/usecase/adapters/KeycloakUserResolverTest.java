package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.entity.UserLinkedAccount;
import id.xyz.chatapps_graph.domain.repository.UserLinkedAccountRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.infrastructure.constant.GeneralConstants.StatusConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakUserResolverTest {

  @Mock private UserLinkedAccountRepository linkedAccountRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private KeycloakUserResolver keycloakUserResolver;

  private Jwt buildJwt(String sub, Map<String, Object> claims) {
    Jwt.Builder builder = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject(sub)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300));
    claims.forEach(builder::claim);
    return builder.build();
  }

  @Test
  void linkedAccountExists_returnsUserId() {
    Jwt jwt = buildJwt("kc-sub-123", Map.of());
    UserLinkedAccount linked = new UserLinkedAccount();
    linked.setUserId(42L);

    when(linkedAccountRepository.findByProviderAndProviderSub("KEYCLOAK", "kc-sub-123"))
        .thenReturn(Optional.of(linked));

    Long result = keycloakUserResolver.resolve(jwt);

    assertEquals(42L, result);
    verifyNoInteractions(userRepository);
  }

  @Test
  void noLinkedAccount_fallbackByPhone_linksAndReturns() {
    Jwt jwt = buildJwt("kc-sub-456", Map.of("preferred_username", "+6281234567890"));
    User user = new User();
    user.setUserId(99L);

    when(linkedAccountRepository.findByProviderAndProviderSub("KEYCLOAK", "kc-sub-456"))
        .thenReturn(Optional.empty());
    when(userRepository.findUserByUserPhoneAndUserStatus("+6281234567890", StatusConstants.ACTIVE))
        .thenReturn(Optional.of(user));

    Long result = keycloakUserResolver.resolve(jwt);

    assertEquals(99L, result);

    ArgumentCaptor<UserLinkedAccount> captor = ArgumentCaptor.forClass(UserLinkedAccount.class);
    verify(linkedAccountRepository).save(captor.capture());
    UserLinkedAccount saved = captor.getValue();
    assertEquals(99L, saved.getUserId());
    assertEquals("KEYCLOAK", saved.getProvider());
    assertEquals("kc-sub-456", saved.getProviderSub());
  }

  @Test
  void noLinkedAccount_noPhone_returnsNull() {
    Jwt jwt = buildJwt("kc-sub-789", Map.of());

    when(linkedAccountRepository.findByProviderAndProviderSub("KEYCLOAK", "kc-sub-789"))
        .thenReturn(Optional.empty());

    Long result = keycloakUserResolver.resolve(jwt);

    assertNull(result);
    verifyNoMoreInteractions(userRepository);
  }

  @Test
  void noLinkedAccount_phoneNotFound_returnsNull() {
    Jwt jwt = buildJwt("kc-sub-000", Map.of("preferred_username", "+620000"));

    when(linkedAccountRepository.findByProviderAndProviderSub("KEYCLOAK", "kc-sub-000"))
        .thenReturn(Optional.empty());
    when(userRepository.findUserByUserPhoneAndUserStatus("+620000", StatusConstants.ACTIVE))
        .thenReturn(Optional.empty());

    Long result = keycloakUserResolver.resolve(jwt);

    assertNull(result);
    verifyNoMoreInteractions(linkedAccountRepository);
  }

  @Test
  void provider_returnsKeycloak() {
    assertEquals("KEYCLOAK", keycloakUserResolver.provider());
  }
}
