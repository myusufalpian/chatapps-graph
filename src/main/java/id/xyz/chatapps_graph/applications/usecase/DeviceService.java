package id.xyz.chatapps_graph.applications.usecase;

public interface DeviceService {

  void registerDevice(Long userId, String deviceToken, String platform);

  void unregisterDevice(Long userId, String deviceToken);
}
