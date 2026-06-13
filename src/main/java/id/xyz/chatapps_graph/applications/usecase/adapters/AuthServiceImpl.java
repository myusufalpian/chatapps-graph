package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.AuthService;
import id.xyz.chatapps_graph.applications.usecase.NotificationService;
import id.xyz.chatapps_graph.applications.usecase.OtpAuditService;
import id.xyz.chatapps_graph.applications.usecase.OtpService;
import id.xyz.chatapps_graph.applications.usecase.RateLimitService;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.OtpPurpose;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.KeycloakTokenResponse;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.config.properties.AuthenticationOtpProperties;
import id.xyz.chatapps_graph.infrastructure.config.security.KeycloakAuthService;
import id.xyz.chatapps_graph.infrastructure.constant.GeneralConstants.StatusConstants;
import id.xyz.chatapps_graph.infrastructure.utility.MaskingUtil;
import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{7,14}$");

  private final UserRepository userRepository;
  private final OtpService otpService;
  private final OtpAuditService otpAuditService;
  private final RateLimitService rateLimitService;
  private final NotificationService notificationService;
  private final KeycloakAuthService keycloakAuthService;
  private final AuthenticationOtpProperties otpProperties;

  @Override
  public void requestOtp(String phone, OtpPurpose purpose, String clientIp) {
    validatePhone(phone);

    if (rateLimitService.isCooldownActive(phone, purpose.name().toLowerCase())) {
      throw new GeneralException(HttpStatus.TOO_MANY_REQUESTS.value(), "OTP_COOLDOWN",
          "Please wait before requesting another OTP");
    }

    if (rateLimitService.isPhoneRateLimited(phone)) {
      throw new GeneralException(HttpStatus.TOO_MANY_REQUESTS.value(), "OTP_RATE_LIMITED",
          "Too many OTP requests. Please try again later");
    }

    if (rateLimitService.isIpRateLimited(clientIp)) {
      throw new GeneralException(HttpStatus.TOO_MANY_REQUESTS.value(), "IP_RATE_LIMITED",
          "Too many requests from this IP. Please try again later");
    }

    var userOpt = userRepository.findUserByUserPhoneAndUserStatus(phone, StatusConstants.ACTIVE);
    if (userOpt.isEmpty()) {
      log.warn("OTP requested for unregistered phone [{}]", MaskingUtil.maskPhone(phone));
      simulateDelay();
      return;
    }

    User user = userOpt.get();
    String otp = otpService.generateAndSaveOtp(phone, purpose);
    OffsetDateTime expAt = OffsetDateTime.now().plusMinutes(otpProperties.getDuration());
    otpAuditService.recordOtpGenerated(user.getUserId(), otp, purpose, expAt);
    rateLimitService.setCooldown(phone, purpose.name().toLowerCase());
    notificationService.sendOtp(phone, user.getUserMail(), otp);
    log.info("OTP sent to phone [{}] for purpose [{}]", MaskingUtil.maskPhone(phone), purpose);
  }

  @Override
  public void requestMfaOtp(String phone, String clientIp) {
    requestOtp(phone, OtpPurpose.MFA, clientIp);
  }

  @Override
  public KeycloakTokenResponse verifyOtpAndLogin(String phone, String code) {
    validatePhone(phone);

    var userOpt = userRepository.findUserByUserPhoneAndUserStatus(phone, StatusConstants.ACTIVE);
    if (userOpt.isEmpty()) {
      throw new GeneralException(HttpStatus.UNAUTHORIZED.value(), "INVALID_OTP",
          "The OTP code is incorrect or expired");
    }

    User user = userOpt.get();
    boolean isValid = otpService.validateOtp(phone, code, OtpPurpose.SIGN_IN);

    if (!isValid) {
      throw new GeneralException(HttpStatus.UNAUTHORIZED.value(), "INVALID_OTP",
          "The OTP code is incorrect or expired");
    }

    otpAuditService.markVerified(user.getUserId(), OtpPurpose.SIGN_IN);

    try {
      return keycloakAuthService.exchangePasswordForToken(phone);
    } catch (RestClientException exc) {
      log.error("Failed to exchange token in Keycloak for user [{}]", MaskingUtil.maskPhone(phone), exc);
      throw new GeneralException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "LOGIN_FAILED",
          "Could not generate session");
    }
  }

  @Override
  public KeycloakTokenResponse refreshToken(String refreshToken) {
    try {
      return keycloakAuthService.refreshToken(refreshToken);
    } catch (RestClientException exc) {
      log.error("Failed to refresh token in Keycloak: [{}]", MaskingUtil.maskToken(refreshToken), exc);
      throw new GeneralException(HttpStatus.UNAUTHORIZED.value(), "REFRESH_FAILED",
          "Could not refresh session");
    }
  }

  private void validatePhone(String phone) {
    if (!StringUtils.hasLength(phone) || !PHONE_PATTERN.matcher(phone).matches()) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), "INVALID_PHONE",
          "Invalid phone number format");
    }
  }

  private void simulateDelay() {
    try {
      Thread.sleep(ThreadLocalRandom.current().nextLong(50, 200));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
