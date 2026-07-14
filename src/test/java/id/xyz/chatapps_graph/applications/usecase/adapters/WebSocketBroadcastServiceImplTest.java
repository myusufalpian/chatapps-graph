package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.mockito.Mockito.verify;

import id.xyz.chatapps_graph.infrastructure.service.RedisChatEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebSocketBroadcastServiceImplTest {

  @Mock private RedisChatEventPublisher publisher;

  @InjectMocks private WebSocketBroadcastServiceImpl broadcastService;

  @Test
  @DisplayName("broadcast: calls publisher with correct arguments")
  void broadcast_Success() {
    String destination = "/topic/test";
    String payload = "hello";

    broadcastService.broadcast(destination, payload);

    verify(publisher).publish(destination, payload);
  }

  @Test
  @DisplayName("sendToUser: formats destination and calls publisher")
  void sendToUser_Success() {
    String username = "user123";
    String destination = "/queue/reply";
    String payload = "world";

    broadcastService.sendToUser(username, destination, payload);

    verify(publisher).publish("/user/user123/queue/reply", payload);
  }
}
