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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleUserResolverTest {

  @Mock private UserLinkedAccountRepository linkedAccountRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private GoogleUserResolver googleUserResolver;

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
    Jwt jwt = buildJwt("google-sub-111", Map.of("email_verified", true, "email", "test@gmail.com"));
    UserLinkedAccount linked = new UserLinkedAccount();
    linked.setUserId(10L);

    when(linkedAccountRepository.findByProviderAndProviderSub("GOOGLE", "google-sub-111"))
        .thenReturn(Optional.of(linked));

    Long result = googleUserResolver.resolve(jwt);

    assertEquals(10L, result);
    verifyNoInteractions(userRepository);
  }

  @Test
  void noLinkedAccount_emailExists_linksAccountAndReturns() {
    Jwt jwt = buildJwt("google-sub-222", Map.of("email_verified", true, "email", "existing@gmail.com"));
    User user = new User();
    user.setUserId(55L);

    when(linkedAccountRepository.findByProviderAndProviderSub("GOOGLE", "google-sub-222"))
        .thenReturn(Optional.empty());
    when(userRepository.findByUserMailAndUserStatus("existing@gmail.com", StatusConstants.ACTIVE))
        .thenReturn(Optional.of(user));

    Long result = googleUserResolver.resolve(jwt);

    assertEquals(55L, result);

    ArgumentCaptor<UserLinkedAccount> captor = ArgumentCaptor.forClass(UserLinkedAccount.class);
    verify(linkedAccountRepository).save(captor.capture());
    UserLinkedAccount saved = captor.getValue();
    assertEquals(55L, saved.getUserId());
    assertEquals("GOOGLE", saved.getProvider());
    assertEquals("google-sub-222", saved.getProviderSub());
    assertEquals("existing@gmail.com", saved.getProviderEmail());
  }

  @Test
  void noLinkedAccount_emailNotFound_autoCreatesUser() {
    Jwt jwt = buildJwt("google-sub-333", Map.of("email_verified", true, "email", "new@gmail.com"));
    User savedUser = new User();
    savedUser.setUserId(77L);

    when(linkedAccountRepository.findByProviderAndProviderSub("GOOGLE", "google-sub-333"))
        .thenReturn(Optional.empty());
    when(userRepository.findByUserMailAndUserStatus("new@gmail.com", StatusConstants.ACTIVE))
        .thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    Long result = googleUserResolver.resolve(jwt);

    assertEquals(77L, result);
    verify(linkedAccountRepository).save(any(UserLinkedAccount.class));
  }

  @Test
  void autoCreate_userStatusIsPendingProfile() {
    Jwt jwt = buildJwt("google-sub-444", Map.of("email_verified", true, "email", "pending@gmail.com"));
    User savedUser = new User();
    savedUser.setUserId(88L);

    when(linkedAccountRepository.findByProviderAndProviderSub("GOOGLE", "google-sub-444"))
        .thenReturn(Optional.empty());
    when(userRepository.findByUserMailAndUserStatus("pending@gmail.com", StatusConstants.ACTIVE))
        .thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    googleUserResolver.resolve(jwt);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    assertEquals(StatusConstants.PENDING_PROFILE, userCaptor.getValue().getUserStatus());
    assertEquals("pending@gmail.com", userCaptor.getValue().getUserMail());
  }

  @Test
  void emailNotVerified_returnsNull() {
    Jwt jwt = buildJwt("google-sub-555", Map.of("email_verified", false, "email", "unverified@gmail.com"));

    when(linkedAccountRepository.findByProviderAndProviderSub("GOOGLE", "google-sub-555"))
        .thenReturn(Optional.empty());

    Long result = googleUserResolver.resolve(jwt);

    assertNull(result);
    verifyNoInteractions(userRepository);
  }

  @Test
  void emailVerifiedMissing_returnsNull() {
    Jwt jwt = buildJwt("google-sub-666", Map.of("email", "missing@gmail.com"));

    when(linkedAccountRepository.findByProviderAndProviderSub("GOOGLE", "google-sub-666"))
        .thenReturn(Optional.empty());

    Long result = googleUserResolver.resolve(jwt);

    assertNull(result);
    verifyNoInteractions(userRepository);
  }

  @Test
  void provider_returnsGoogle() {
    assertEquals("GOOGLE", googleUserResolver.provider());
  }
}
