package id.xyz.chatapps_graph.applications.usecase.adapters.fakes;

import id.xyz.chatapps_graph.applications.usecase.NotificationService;
import java.util.ArrayList;
import java.util.List;

public class FakeNotificationService implements NotificationService {

  public record SentOtp(String phone, String email, String otp) {}

  private final List<SentOtp> sent = new ArrayList<>();

  @Override
  public void sendOtp(String phone, String email, String otp) {
    sent.add(new SentOtp(phone, email, otp));
  }

  @Override
  public void sendOtp(String phone, String otp) {
    sent.add(new SentOtp(phone, null, otp));
  }

  public List<SentOtp> getSent() {
    return sent;
  }

  public void clear() {
    sent.clear();
  }
}
