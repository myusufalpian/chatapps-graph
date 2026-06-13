package id.xyz.chatapps_graph.infrastructure.config.security;

import id.xyz.chatapps_graph.applications.usecase.adapters.UserIdentityResolverRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserIdentityFilterTest {

  @Mock private UserIdentityResolverRegistry resolverRegistry;
  @Mock private FilterChain filterChain;

  @InjectMocks private UserIdentityFilter userIdentityFilter;

  private Jwt buildJwt() {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("sub")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300))
        .build();
  }

  @Test
  void authenticated_setsXUserIdAttribute() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/graphql");
    MockHttpServletResponse response = new MockHttpServletResponse();
    Jwt jwt = buildJwt();
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);

    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
    ctx.setAuthentication(auth);
    SecurityContextHolder.setContext(ctx);

    when(resolverRegistry.resolve(jwt)).thenReturn(42L);

    userIdentityFilter.doFilterInternal(request, response, filterChain);

    assertEquals(42L, request.getAttribute("X-User-Id"));
    verify(filterChain).doFilter(request, response);

    SecurityContextHolder.clearContext();
  }

  @Test
  void publicEndpoint_skipped() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/graphiql");
    assertEquals(true, userIdentityFilter.shouldNotFilter(request));

    request.setRequestURI("/ws-chat/info");
    assertEquals(true, userIdentityFilter.shouldNotFilter(request));

    request.setRequestURI("/api/v1/auth/otp/send");
    assertEquals(true, userIdentityFilter.shouldNotFilter(request));

    request.setRequestURI("/api/v1/auth/refresh-token");
    assertEquals(true, userIdentityFilter.shouldNotFilter(request));
  }

  @Test
  void resolveNull_returns401() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/graphql");
    MockHttpServletResponse response = new MockHttpServletResponse();
    Jwt jwt = buildJwt();
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);

    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
    ctx.setAuthentication(auth);
    SecurityContextHolder.setContext(ctx);

    when(resolverRegistry.resolve(jwt)).thenReturn(null);

    userIdentityFilter.doFilterInternal(request, response, filterChain);

    assertEquals(401, response.getStatus());
    assertNull(request.getAttribute("X-User-Id"));

    SecurityContextHolder.clearContext();
  }

  @Test
  void noAuthentication_skipped() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/graphql");
    MockHttpServletResponse response = new MockHttpServletResponse();

    SecurityContextHolder.clearContext();

    userIdentityFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(resolverRegistry);
  }

  @Test
  void userBanned_returns401_viaResolverReturningNull() throws ServletException, IOException {
    // When user is banned, resolverRegistry.resolve() returns null (isBanned check inside)
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/graphql");
    MockHttpServletResponse response = new MockHttpServletResponse();
    Jwt jwt = buildJwt();
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);

    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
    ctx.setAuthentication(auth);
    SecurityContextHolder.setContext(ctx);

    when(resolverRegistry.resolve(jwt)).thenReturn(null);

    userIdentityFilter.doFilterInternal(request, response, filterChain);

    assertEquals(401, response.getStatus());
    assertNull(request.getAttribute("X-User-Id"));

    SecurityContextHolder.clearContext();
  }
}
