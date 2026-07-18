package id.xyz.chatapps_graph.infrastructure.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  @Value("${jwt.auth.converter.principal-attribute}")
  private String principalAttribute;

  @Override
  public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
    Collection<org.springframework.security.core.GrantedAuthority> authorities =
        new ArrayList<>(new JwtGrantedAuthoritiesConverter().convert(jwt));
    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
    if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
      roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
    }
    return new JwtAuthenticationToken(jwt, authorities, jwt.getClaim(principalAttribute));
  }
}
