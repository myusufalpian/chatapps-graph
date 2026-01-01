package id.xyz.chatapps_graph.infrastructure.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  @Value("${jwt.auth.converter.principal-attribute}")
  private String principalAttribute;

  @Override
  public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
    var defaultAuthorities = new JwtGrantedAuthoritiesConverter().convert(jwt);
    return new JwtAuthenticationToken(jwt, defaultAuthorities, jwt.getClaim(principalAttribute));
  }
}
