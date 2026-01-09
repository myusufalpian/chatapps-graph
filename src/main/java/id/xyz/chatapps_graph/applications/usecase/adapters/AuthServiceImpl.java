package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.AuthService;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.KeycloakTokenResponse;
import id.xyz.chatapps_graph.framework.dto.UserSignInRequestDTO;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.config.security.KeycloakAuthService;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants.ErrorMessageConstants;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants.LoggingConstants;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants.ErrorKeyConstants;
import id.xyz.chatapps_graph.infrastructure.constant.GeneralConstants.StatusConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final KeycloakAuthService keycloakAuthService;

  public String getTokenAuth(UserSignInRequestDTO signInRequest) {
    userRepository.findUserByUserPhoneAndUserStatus(signInRequest.phone(), StatusConstants.ACTIVE)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorKeyConstants.USER_NOT_FOUND, ErrorMessageConstants.INVALID_USER));

    try {
      KeycloakTokenResponse tokenResponse = keycloakAuthService.exchangePasswordForToken(signInRequest.phone());
      return tokenResponse.accessToken();

    } catch (Exception exc) {
      log.error(LoggingConstants.LOGIN_ERROR, signInRequest.phone(), exc);
      throw new GeneralException(HttpStatus.UNAUTHORIZED.value(), ErrorKeyConstants.SIGNIN_FAILED, ErrorMessageConstants.INVALID_CREDENTIALS);
    }
  }

}
