package id.xyz.chatapps_graph.applications.usecase;

public interface WebSocketBroadcastService {
  void broadcast(String destination, Object payload);
  void sendToUser(String username, String destination, Object payload);
}
// 
