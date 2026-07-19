package id.xyz.chatapps_graph.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.security.oauth2.resourceserver.jwt")
public record OAuth2JwtProperties(
    String issuerUri
) {
}
