package id.xyz.chatapps_graph.infrastructure.utility;

public final class MaskingUtil {

  private MaskingUtil() {}

  public static String maskPhone(String phone) {
    if (phone == null || phone.isEmpty() || phone.length() < 4) return "***";
    int len = phone.length();
    int maskLen = Math.max(1, len - 5);
    return phone.substring(0, 3) + "*".repeat(maskLen) + phone.substring(len - 2);
  }

  public static String maskToken(String token) {
    if (token == null || token.isEmpty() || token.length() < 8) return "***";
    return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
  }

  public static String maskEmail(String email) {
    if (email == null || email.isEmpty()) return "***";
    int atIndex = email.indexOf('@');
    if (atIndex <= 1) return "***" + email.substring(atIndex < 0 ? 0 : atIndex);
    return email.charAt(0) + "*".repeat(atIndex - 1) + email.substring(atIndex);
  }
}
