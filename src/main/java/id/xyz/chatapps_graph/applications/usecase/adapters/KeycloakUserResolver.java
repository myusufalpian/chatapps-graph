package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.UserIdentityResolver;
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
public class KeycloakUserResolver implements UserIdentityResolver {

  private final UserLinkedAccountRepository linkedAccountRepository;
  private final UserRepository userRepository;

  @Override
  public Long resolve(Jwt jwt) {
    String sub = jwt.getSubject();

    return linkedAccountRepository.findByProviderAndProviderSub(provider(), sub)
        .map(UserLinkedAccount::getUserId)
        .orElseGet(() -> fallbackByPhone(jwt, sub));
  }

  @Override
  public String provider() {
    return ProviderType.KEYCLOAK.name();
  }

  private Long fallbackByPhone(Jwt jwt, String sub) {
    String phone = jwt.getClaimAsString("preferred_username");
    if (!StringUtils.hasLength(phone)) {
      return null;
    }

    return userRepository.findUserByUserPhoneAndUserStatus(phone, StatusConstants.ACTIVE)
        .map(user -> {
          linkAccount(user.getUserId(), sub);
          return user.getUserId();
        })
        .orElse(null);
  }

  private void linkAccount(Long userId, String sub) {
    linkedAccountRepository.save(UserLinkedAccount.builder()
        .userId(userId)
        .provider(provider())
        .providerSub(sub)
        .linkedAt(OffsetDateTime.now())
        .build());
  }
}
