package id.xyz.chatapps_graph.infrastructure.utility;

import org.springframework.util.StringUtils;

public final class MaskingUtil {

  private MaskingUtil() {}

  public static String maskPhone(String phone) {
    if (!StringUtils.hasLength(phone) || phone.length() < 4) return "***";
    int len = phone.length();
    int maskLen = Math.max(1, len - 5);
    return phone.substring(0, 3) + "*".repeat(maskLen) + phone.substring(len - 2);
  }

  public static String maskToken(String token) {
    if (!StringUtils.hasLength(token) || token.length() < 8) return "***";
    return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
  }

  public static String maskEmail(String email) {
    if (!StringUtils.hasLength(email)) return "***";
    int atIndex = email.indexOf('@');
    if (atIndex <= 1) return "***" + email.substring(Math.max(atIndex, 0));
    return email.charAt(0) + "*".repeat(atIndex - 1) + email.substring(atIndex);
  }
}
