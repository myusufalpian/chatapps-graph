package id.xyz.chatapps_graph.infrastructure.monitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketMetricsListener {

  private final MetricsFacade metricsFacade;

  @EventListener
  public void handleSessionConnect(SessionConnectEvent event) {
    metricsFacade.recordConnection();
  }

  @EventListener
  public void handleSessionDisconnect(SessionDisconnectEvent event) {
    metricsFacade.recordDisconnect();
  }
}
