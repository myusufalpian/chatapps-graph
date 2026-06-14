package id.xyz.chatapps_graph.applications.usecase;

public interface RateLimitService {
  boolean isIpRateLimited(String clientIp);

  boolean isPhoneRateLimited(String phone);

  boolean isCooldownActive(String phone, String purpose);

  void setCooldown(String phone, String purpose);

  boolean isChatRateLimited(Long userId);

  boolean isReactionRateLimited(Long userId);
}
