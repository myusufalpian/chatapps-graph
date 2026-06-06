package id.xyz.chatapps_graph.applications.usecase;

public interface WhatsAppProvider {
  void sendMessage(String phone, String otp);
  String getProviderName();
}
