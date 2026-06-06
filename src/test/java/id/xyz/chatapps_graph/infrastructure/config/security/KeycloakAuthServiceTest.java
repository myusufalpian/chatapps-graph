package id.xyz.chatapps_graph.infrastructure.config.security;

import id.xyz.chatapps_graph.framework.dto.KeycloakTokenResponse;
import id.xyz.chatapps_graph.infrastructure.config.properties.KeycloakProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakAuthServiceTest {

  private MockWebServer mockWebServer;
  private KeycloakAuthService keycloakAuthService;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    KeycloakProperties props = new KeycloakProperties();
    props.setTokenUri(mockWebServer.url("/token").toString());
    props.setClientId("test-client");
    props.setClientSecret("test-secret");
    keycloakAuthService = new KeycloakAuthService(props);
  }

  @AfterEach
  void tearDown() throws Exception { mockWebServer.shutdown(); }

  @Test
  void refreshToken_success() throws Exception {
    mockWebServer.enqueue(new MockResponse()
        .setBody("""
            {"access_token":"new-access","refresh_token":"new-refresh","expires_in":300,"refresh_expires_in":1800,"token_type":"Bearer"}""")
        .addHeader("Content-Type", "application/json"));

    KeycloakTokenResponse response = keycloakAuthService.refreshToken("old-refresh-token");

    assertNotNull(response);
    assertEquals("new-access", response.accessToken());
    assertEquals("new-refresh", response.refreshToken());
    assertEquals(300, response.expiresIn());
    assertEquals(1800, response.refreshExpiresIn());
    assertEquals("Bearer", response.tokenType());
    RecordedRequest request = mockWebServer.takeRequest();
    String body = request.getBody().readUtf8();
    assertTrue(body.contains("grant_type=refresh_token"));
    assertTrue(body.contains("refresh_token=old-refresh-token"));
    assertTrue(body.contains("client_id=test-client"));
    assertTrue(body.contains("client_secret=test-secret"));
  }
}
