package id.xyz.chatapps_graph.applications.usecase;

import org.springframework.security.oauth2.jwt.Jwt;

public interface UserIdentityResolver {

  Long resolve(Jwt jwt);

  String provider();
}
