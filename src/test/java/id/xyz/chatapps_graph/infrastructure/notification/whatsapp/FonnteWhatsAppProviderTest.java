package id.xyz.chatapps_graph.infrastructure.notification.whatsapp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import id.xyz.chatapps_graph.infrastructure.config.properties.FonnteProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;

class FonnteWhatsAppProviderTest {

  private MockWebServer mockWebServer;
  private FonnteWhatsAppProvider provider;

  private static final String PHONE = "+628123456789";
  private static final String OTP = "123456";

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    FonnteProperties properties = new FonnteProperties();
    properties.setApiUrl(mockWebServer.url("/send").toString());
    properties.setApiToken("test-token");
    provider = new FonnteWhatsAppProvider(properties);
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
  @DisplayName("sendMessage: API returns 4xx → throws exception")
  void sendMessage_4xx_Throws() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("Bad Request"));

    assertThrows(HttpClientErrorException.class, () -> provider.sendMessage(PHONE, OTP));
  }

  @Test
  @DisplayName("sendMessage: API returns 5xx → throws exception")
  void sendMessage_5xx_Throws() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Server Error"));

    assertThrows(HttpServerErrorException.class, () -> provider.sendMessage(PHONE, OTP));
  }

  @Test
  @DisplayName("getProviderName: returns fonnte")
  void getProviderName_ReturnsFonnte() {
    assertEquals("fonnte", provider.getProviderName());
  }
}
