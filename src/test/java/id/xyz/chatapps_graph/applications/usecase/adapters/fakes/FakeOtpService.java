package id.xyz.chatapps_graph.applications.usecase.adapters.fakes;

import id.xyz.chatapps_graph.applications.usecase.OtpService;
import id.xyz.chatapps_graph.domain.enums.OtpPurpose;
import java.util.HashMap;
import java.util.Map;

public class FakeOtpService implements OtpService {

  private final Map<String, String> otpStore = new HashMap<>();
  private final Map<String, Integer> requestCounts = new HashMap<>();
  private final int maxCount;

  public FakeOtpService(int maxCount) {
    this.maxCount = maxCount;
  }

  @Override
  public boolean isRateLimited(String phone) {
    return requestCounts.getOrDefault(phone, 0) >= maxCount;
  }

  @Override
  public String generateAndSaveOtp(String phone, OtpPurpose purpose) {
    requestCounts.merge(phone, 1, Integer::sum);
    String otp = "123456";
    otpStore.put(purpose.name() + ":" + phone, otp);
    return otp;
  }

  @Override
  public boolean validateOtp(String phone, String inputOtp, OtpPurpose purpose) {
    String key = purpose.name() + ":" + phone;
    String stored = otpStore.get(key);
    if (stored != null && stored.equals(inputOtp)) {
      otpStore.remove(key);
      return true;
    }
    return false;
  }

  public String getStoredOtp(String phone, OtpPurpose purpose) {
    return otpStore.get(purpose.name() + ":" + phone);
  }

  public int getRequestCount(String phone) {
    return requestCounts.getOrDefault(phone, 0);
  }

  public void clear() {
    otpStore.clear();
    requestCounts.clear();
  }
}
