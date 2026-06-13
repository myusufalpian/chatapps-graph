package id.xyz.chatapps_graph.infrastructure.config.security;

import id.xyz.chatapps_graph.applications.usecase.adapters.UserIdentityResolverRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserIdentityFilter extends OncePerRequestFilter {

  private static final Set<String> PUBLIC_PREFIXES = Set.of(
      "/graphiql", "/ws-chat", "/api/v1/auth/otp", "/api/v1/auth/refresh-token"
  );

  private final UserIdentityResolverRegistry resolverRegistry;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      filterChain.doFilter(request, response);
      return;
    }

    Jwt jwt = jwtAuth.getToken();
    Long userId = resolverRegistry.resolve(jwt);

    if (userId == null) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    request.setAttribute("X-User-Id", userId);
    filterChain.doFilter(request, response);
  }
}
