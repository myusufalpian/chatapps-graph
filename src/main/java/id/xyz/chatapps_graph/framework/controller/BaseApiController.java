package id.xyz.chatapps_graph.framework.controller;

import id.xyz.chatapps_graph.framework.dto.BaseMetadata;
import id.xyz.chatapps_graph.framework.dto.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for all REST API controllers.
 * Provides common utilities: client IP extraction and standardized response building.
 *
 * <p>Subclasses should define their own {@code @RequestMapping} and inject dependencies
 * via constructor (e.g., Lombok {@code @RequiredArgsConstructor}).
 */
public abstract class BaseApiController {

  /**
   * Extracts the client IP address from the request.
   *
   * <p><b>Security note:</b> X-Forwarded-For can be spoofed. This method assumes
   * the application runs behind a trusted reverse proxy that sets the header correctly.
   * Only the first hop (closest to client) is trusted.
   *
   * @param request the HTTP servlet request
   * @return the client IP address
   */
  protected String extractClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (StringUtils.hasLength(xForwardedFor)) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  protected <T> ResponseEntity<BaseResponse<T>> success(T data, String message) {
    return ResponseEntity.ok(new BaseResponse<>(data, new BaseMetadata(message)));
  }

  protected ResponseEntity<BaseResponse<Void>> success(String message) {
    return ResponseEntity.ok(new BaseResponse<>(null, new BaseMetadata(message)));
  }

  protected <T> ResponseEntity<BaseResponse<T>> created(T data, String message) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new BaseResponse<>(data, new BaseMetadata(message)));
  }
}
