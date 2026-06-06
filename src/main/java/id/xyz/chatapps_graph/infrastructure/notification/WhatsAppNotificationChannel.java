package id.xyz.chatapps_graph.infrastructure.notification;

import id.xyz.chatapps_graph.applications.usecase.NotificationChannel;
import id.xyz.chatapps_graph.domain.enums.NotificationChannelType;
import id.xyz.chatapps_graph.infrastructure.notification.whatsapp.FonnteWhatsAppProvider;
import id.xyz.chatapps_graph.infrastructure.notification.whatsapp.WablasWhatsAppProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class WhatsAppNotificationChannel implements NotificationChannel {

  private final FonnteWhatsAppProvider primaryProvider;
  private final WablasWhatsAppProvider fallbackProvider;
  private final CircuitBreakerRegistry circuitBreakerRegistry;

  @Override
  public void send(String phone, String otp) {
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("whatsapp-primary");
    try {
      cb.executeRunnable(() -> primaryProvider.sendMessage(phone, otp));
    } catch (Exception e) {
      log.warn("Primary WhatsApp provider failed, switching to fallback: {}", e.getMessage());
      try {
        fallbackProvider.sendMessage(phone, otp);
      } catch (Exception fallbackEx) {
        log.error("All WhatsApp providers failed. Primary: [{}], Fallback: [{}]",
            e.getMessage(), fallbackEx.getMessage());
      }
    }
  }

  @Override
  public NotificationChannelType getType() {
    return NotificationChannelType.WHATSAPP;
  }
}
