package id.xyz.chatapps_graph.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "application.keycloak")
public class KeycloakProperties {
  private String tokenUri;
  private String clientId;
  private String clientSecret;
}
