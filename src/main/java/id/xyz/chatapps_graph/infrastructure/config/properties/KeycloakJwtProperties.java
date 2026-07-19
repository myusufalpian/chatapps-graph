package id.xyz.chatapps_graph.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt.auth.converter")
public record KeycloakJwtProperties(
    String principalAttribute
) {
}
