package id.xyz.chatapps_graph.infrastructure.config.properties;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorsPropertiesTest {

  @Test
  void defaultValue_ReturnsLocalhost3000() {
    CorsProperties properties = new CorsProperties();

    assertTrue(properties.getAllowedOrigins().contains("http://localhost:3000"));
  }

  @Test
  void setAllowedOrigins_ReturnsConfiguredValues() {
    CorsProperties properties = new CorsProperties();
    List<String> custom = List.of("https://example.com", "https://app.example.com");

    properties.setAllowedOrigins(custom);

    assertEquals(custom, properties.getAllowedOrigins());
  }

  @Test
  void allowedOrigins_NotEmpty() {
    CorsProperties properties = new CorsProperties();

    assertFalse(properties.getAllowedOrigins().isEmpty());
  }
}
