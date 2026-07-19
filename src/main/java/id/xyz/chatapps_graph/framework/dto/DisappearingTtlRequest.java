package id.xyz.chatapps_graph.framework.dto;

import jakarta.validation.constraints.NotNull;

import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants;
import org.springframework.http.HttpStatus;

public record DisappearingTtlRequest(
    @NotNull String ttl
) {
  public Integer toHours() {
    return switch (ttl) {
      case "24h" -> 24;
      case "7d" -> 168;
      case "30d" -> 720;
      case "off" -> null;
      default -> throw new GeneralException(HttpStatus.BAD_REQUEST.value(), ErrorConstants.BAD_REQUEST, "Invalid TTL value");
    };
  }
}
