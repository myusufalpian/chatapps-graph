package id.xyz.chatapps_graph.infrastructure.notification.whatsapp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import id.xyz.chatapps_graph.infrastructure.config.properties.WablasProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;

class WablasWhatsAppProviderTest {

  private MockWebServer mockWebServer;
  private WablasWhatsAppProvider provider;

  private static final String PHONE = "+628123456789";
  private static final String OTP = "123456";

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    WablasProperties properties = new WablasProperties();
    properties.setApiUrl(mockWebServer.url("/send-message").toString());
    properties.setApiToken("test-token");
    provider = new WablasWhatsAppProvider(properties);
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  @DisplayName("sendMessage: success → completes without exception")
  void sendMessage_Success() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

    assertDoesNotThrow(() -> provider.sendMessage(PHONE, OTP));
    assertEquals(1, mockWebServer.getRequestCount());
  }

  @Test
  @DisplayName("sendMessage: API failure → throws exception")
  void sendMessage_Failure_Throws() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Server Error"));

    assertThrows(HttpServerErrorException.class, () -> provider.sendMessage(PHONE, OTP));
  }

  @Test
  @DisplayName("getProviderName: returns wablas")
  void getProviderName_ReturnsWablas() {
    assertEquals("wablas", provider.getProviderName());
  }
}
