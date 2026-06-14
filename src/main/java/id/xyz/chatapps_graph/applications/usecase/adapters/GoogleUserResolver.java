package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.UserIdentityResolver;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.entity.UserLinkedAccount;
import id.xyz.chatapps_graph.domain.enums.ProviderType;
import id.xyz.chatapps_graph.domain.repository.UserLinkedAccountRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.infrastructure.constant.GeneralConstants.StatusConstants;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleUserResolver implements UserIdentityResolver {

  private final UserLinkedAccountRepository linkedAccountRepository;
  private final UserRepository userRepository;

  @Override
  public Long resolve(Jwt jwt) {
    String sub = jwt.getSubject();

    return linkedAccountRepository.findByProviderAndProviderSub(provider(), sub)
        .map(UserLinkedAccount::getUserId)
        .orElseGet(() -> resolveNewAccount(jwt, sub));
  }

  @Override
  public String provider() {
    return ProviderType.GOOGLE.name();
  }

  private Long resolveNewAccount(Jwt jwt, String sub) {
    if (!isEmailVerified(jwt)) {
      log.warn("Google account linking rejected: email_verified is not true for sub={}", sub);
      return null;
    }

    String email = jwt.getClaimAsString("email");
    if (!StringUtils.hasLength(email)) {
      return null;
    }

    return userRepository.findByUserMailAndUserStatus(email, StatusConstants.ACTIVE)
        .map(user -> linkExistingUser(user.getUserId(), sub, email))
        .orElseGet(() -> createNewUser(sub, email));
  }

  private boolean isEmailVerified(Jwt jwt) {
    return Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
  }

  private Long linkExistingUser(Long userId, String sub, String email) {
    linkAccount(userId, sub, email);
    return userId;
  }

  private Long createNewUser(String sub, String email) {
    User newUser = User.builder().userMail(email).userStatus(StatusConstants.PENDING_PROFILE).build();
    newUser.setCreatedAt(OffsetDateTime.now());
    newUser = userRepository.save(newUser);

    linkAccount(newUser.getUserId(), sub, email);
    return newUser.getUserId();
  }

  private void linkAccount(Long userId, String sub, String email) {
    linkedAccountRepository.save(UserLinkedAccount.builder()
        .userId(userId)
        .provider(provider())
        .providerSub(sub)
        .providerEmail(email)
        .linkedAt(OffsetDateTime.now())
        .build());
  }
}
