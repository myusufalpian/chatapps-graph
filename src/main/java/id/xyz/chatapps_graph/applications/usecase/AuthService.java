package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.domain.enums.OtpPurpose;
import id.xyz.chatapps_graph.framework.dto.KeycloakTokenResponse;

public interface AuthService {

  void requestOtp(String phone, OtpPurpose purpose, String clientIp);
  void requestMfaOtp(String phone, String clientIp);
  KeycloakTokenResponse verifyOtpAndLogin(String phone, String code);
  KeycloakTokenResponse refreshToken(String refreshToken);
}
