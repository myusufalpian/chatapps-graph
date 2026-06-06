package id.xyz.chatapps_graph.infrastructure.config.websocket;

import id.xyz.chatapps_graph.infrastructure.config.security.KeycloakJwtConverter;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

  private final JwtDecoder jwtDecoder;
  private final KeycloakJwtConverter keycloakJwtConverter;

  @Override
  public Message<?> preSend(@Nullable Message<?> message, @Nullable MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
      String authHeader = accessor.getFirstNativeHeader("Authorization");

      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new AuthenticationCredentialsNotFoundException("Missing or invalid Authorization header");
      }

      String token = authHeader.substring(7);
      try {
        Jwt jwt = jwtDecoder.decode(token);
        JwtAuthenticationToken authentication = (JwtAuthenticationToken) keycloakJwtConverter.convert(jwt);
        accessor.setUser(authentication);
      } catch (JwtException e) {
        log.warn("WebSocket CONNECT rejected: invalid JWT - {}", e.getMessage());
        throw new AuthenticationCredentialsNotFoundException("Invalid or expired token");
      }
    }

    return message;
  }
}
