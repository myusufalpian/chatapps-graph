package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.WebSocketBroadcastService;
import id.xyz.chatapps_graph.infrastructure.service.RedisChatEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketBroadcastServiceImpl implements WebSocketBroadcastService {

  private final RedisChatEventPublisher publisher;

  @Override
  public void broadcast(String destination, Object payload) {
    publisher.publish(destination, payload);
  }

  @Override
  public void sendToUser(String username, String destination, Object payload) {
    publisher.publish("/user/" + username + destination, payload);
  }
}
