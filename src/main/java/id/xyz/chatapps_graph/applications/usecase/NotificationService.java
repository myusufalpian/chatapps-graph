package id.xyz.chatapps_graph.applications.usecase;

public interface NotificationService {
  void sendOtp(String phone, String email, String otp);
  void sendOtp(String phone, String otp);
}
