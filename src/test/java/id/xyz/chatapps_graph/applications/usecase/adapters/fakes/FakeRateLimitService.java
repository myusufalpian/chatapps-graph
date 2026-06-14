package id.xyz.chatapps_graph.applications.usecase.adapters.fakes;

import id.xyz.chatapps_graph.applications.usecase.RateLimitService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FakeRateLimitService implements RateLimitService {

  private final Map<String, Integer> ipCounts = new HashMap<>();
  private final Set<String> cooldowns = new HashSet<>();
  private final Map<String, Integer> phoneCounts = new HashMap<>();
  private final int maxIpCount;
  private final int maxPhoneCount;

  public FakeRateLimitService(int maxIpCount, int maxPhoneCount) {
    this.maxIpCount = maxIpCount;
    this.maxPhoneCount = maxPhoneCount;
  }

  @Override
  public boolean isIpRateLimited(String clientIp) {
    int count = ipCounts.merge(clientIp, 1, Integer::sum);
    return count > maxIpCount;
  }

  @Override
  public boolean isPhoneRateLimited(String phone) {
    return phoneCounts.getOrDefault(phone, 0) >= maxPhoneCount;
  }

  @Override
  public boolean isCooldownActive(String phone, String purpose) {
    return cooldowns.contains(purpose + ":" + phone);
  }

  @Override
  public void setCooldown(String phone, String purpose) {
    cooldowns.add(purpose + ":" + phone);
  }

  @Override
  public boolean isChatRateLimited(Long userId) {
    return false;
  }

  @Override
  public boolean isReactionRateLimited(Long userId) {
    return false;
  }

  public boolean hasCooldown(String phone, String purpose) {
    return cooldowns.contains(purpose + ":" + phone);
  }

  public void setPhoneCount(String phone, int count) {
    phoneCounts.put(phone, count);
  }

  public void clear() {
    ipCounts.clear();
    cooldowns.clear();
    phoneCounts.clear();
  }
}
