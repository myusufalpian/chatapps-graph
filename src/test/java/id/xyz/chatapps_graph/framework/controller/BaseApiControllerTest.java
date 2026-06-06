package id.xyz.chatapps_graph.framework.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.framework.dto.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class BaseApiControllerTest {

  @Mock private HttpServletRequest httpRequest;

  private final TestController controller = new TestController();

  // Concrete subclass for testing
  static class TestController extends BaseApiController {}

  // --- extractClientIp ---

  @Test
  @DisplayName("extractClientIp: should return first IP from X-Forwarded-For header")
  void extractClientIp_ReturnsFirstIp_WhenXForwardedForHasSingleIp() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50");

    assertEquals("203.0.113.50", controller.extractClientIp(httpRequest));
  }

  @Test
  @DisplayName("extractClientIp: should return first IP trimmed from multiple X-Forwarded-For values")
  void extractClientIp_ReturnsFirstIpTrimmed_WhenMultipleIps() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50, 70.41.3.18, 150.172.238.178");

    assertEquals("203.0.113.50", controller.extractClientIp(httpRequest));
  }

  @Test
  @DisplayName("extractClientIp: should return remoteAddr when X-Forwarded-For is null")
  void extractClientIp_ReturnsRemoteAddr_WhenHeaderNull() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
    when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

    assertEquals("127.0.0.1", controller.extractClientIp(httpRequest));
  }

  @Test
  @DisplayName("extractClientIp: should return remoteAddr when X-Forwarded-For is blank")
  void extractClientIp_ReturnsRemoteAddr_WhenHeaderBlank() {
    when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("   ");
    when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");

    assertEquals("192.168.1.1", controller.extractClientIp(httpRequest));
  }

  // --- success(data, message) ---

  @Test
  @DisplayName("success(data, message): should return 200 with data and message")
  void success_WithData_Returns200WithBody() {
    ResponseEntity<BaseResponse<String>> response = controller.success("payload", "OK");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("payload", response.getBody().data());
    assertEquals("OK", response.getBody().metadata().message());
  }

  // --- success(message) ---

  @Test
  @DisplayName("success(message): should return 200 with null data and message")
  void success_WithoutData_Returns200WithNullData() {
    ResponseEntity<BaseResponse<Void>> response = controller.success("Done");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(response.getBody().data());
    assertEquals("Done", response.getBody().metadata().message());
  }

  // --- created(data, message) ---

  @Test
  @DisplayName("created(data, message): should return 201 with data and message")
  void created_Returns201WithBody() {
    ResponseEntity<BaseResponse<Integer>> response = controller.created(42, "Created");

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(42, response.getBody().data());
    assertEquals("Created", response.getBody().metadata().message());
  }
}
