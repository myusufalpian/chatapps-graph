package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.NotificationService;
import id.xyz.chatapps_graph.applications.usecase.OtpService;
import id.xyz.chatapps_graph.applications.usecase.RateLimitService;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.KeycloakTokenResponse;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.config.security.KeycloakAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private OtpService otpService;
  @Mock private RateLimitService rateLimitService;
  @Mock private NotificationService notificationService;
  @Mock private KeycloakAuthService keycloakAuthService;

  @InjectMocks private AuthServiceImpl authService;

  @Test
  void refreshToken_success() {
    KeycloakTokenResponse expected = KeycloakTokenResponse.builder()
        .accessToken("access").refreshToken("refresh").expiresIn(300).build();
    when(keycloakAuthService.refreshToken("valid-refresh")).thenReturn(expected);

    KeycloakTokenResponse result = authService.refreshToken("valid-refresh");

    assertEquals(expected, result);
    verify(keycloakAuthService).refreshToken("valid-refresh");
  }

  @Test
  void refreshToken_failure_throwsGeneralException() {
    when(keycloakAuthService.refreshToken("bad-token")).thenThrow(new RestClientException("error"));

    GeneralException ex = assertThrows(GeneralException.class, () -> authService.refreshToken("bad-token"));

    assertEquals("REFRESH_FAILED", ex.getKey());
    verify(keycloakAuthService).refreshToken("bad-token");
  }
}
