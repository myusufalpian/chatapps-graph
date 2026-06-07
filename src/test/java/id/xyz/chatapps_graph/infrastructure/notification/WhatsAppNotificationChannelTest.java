package id.xyz.chatapps_graph.infrastructure.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import id.xyz.chatapps_graph.domain.enums.NotificationChannelType;
import id.xyz.chatapps_graph.infrastructure.notification.whatsapp.FonnteWhatsAppProvider;
import id.xyz.chatapps_graph.infrastructure.notification.whatsapp.WablasWhatsAppProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WhatsAppNotificationChannelTest {

  @Mock private FonnteWhatsAppProvider primaryProvider;
  @Mock private WablasWhatsAppProvider fallbackProvider;

  private WhatsAppNotificationChannel channel;

  private static final String PHONE = "+628123456789";
  private static final String OTP = "123456";

  @BeforeEach
  void setUp() {
    CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .minimumNumberOfCalls(2)
        .slidingWindowSize(4)
        .build());
    channel = new WhatsAppNotificationChannel(primaryProvider, fallbackProvider, circuitBreakerRegistry);
  }

  @Test
  @DisplayName("send: primary success → fallback NOT called")
  void send_PrimarySuccess_FallbackNotCalled() {
    doNothing().when(primaryProvider).sendMessage(PHONE, OTP);

    channel.send(PHONE, OTP);

    verify(primaryProvider).sendMessage(PHONE, OTP);
    verify(fallbackProvider, never()).sendMessage(PHONE, OTP);
  }

  @Test
  @DisplayName("send: primary fails → fallback called and succeeds")
  void send_PrimaryFails_FallbackCalled() {
    doThrow(new RuntimeException("Fonnte down")).when(primaryProvider).sendMessage(PHONE, OTP);
    doNothing().when(fallbackProvider).sendMessage(PHONE, OTP);

    channel.send(PHONE, OTP);

    verify(primaryProvider).sendMessage(PHONE, OTP);
    verify(fallbackProvider).sendMessage(PHONE, OTP);
  }

  @Test
  @DisplayName("send: primary fails + fallback fails → does not propagate")
  void send_BothFail_DoesNotPropagate() {
    doThrow(new RuntimeException("Fonnte down")).when(primaryProvider).sendMessage(PHONE, OTP);
    doThrow(new RuntimeException("Wablas down")).when(fallbackProvider).sendMessage(PHONE, OTP);

    assertDoesNotThrow(() -> channel.send(PHONE, OTP));
  }

  @Test
  @DisplayName("send: circuit open → primary skipped, fallback called directly")
  void send_CircuitOpen_FallbackCalledDirectly() {
    // Trip the circuit breaker by causing failures
    doThrow(new RuntimeException("fail")).when(primaryProvider).sendMessage(PHONE, OTP);
    doNothing().when(fallbackProvider).sendMessage(PHONE, OTP);

    // Call enough times to open circuit (minimumNumberOfCalls=2, threshold=50%)
    channel.send(PHONE, OTP);
    channel.send(PHONE, OTP);

    // Reset mock to track next interaction
    reset(primaryProvider, fallbackProvider);
    doNothing().when(fallbackProvider).sendMessage(PHONE, OTP);

    // Circuit should be open now — primary should NOT be called
    channel.send(PHONE, OTP);

    verify(primaryProvider, never()).sendMessage(PHONE, OTP);
    verify(fallbackProvider).sendMessage(PHONE, OTP);
  }

  @Test
  @DisplayName("getType: returns WHATSAPP")
  void getType_ReturnsWhatsApp() {
    assertEquals(NotificationChannelType.WHATSAPP, channel.getType());
  }
}
