package id.xyz.chatapps_graph.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.identity")
public record AppIdentityProperties(
    long cacheTtlSeconds
) {
}
