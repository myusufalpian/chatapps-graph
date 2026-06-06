package id.xyz.chatapps_graph.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "notification.whatsapp.wablas")
public class WablasProperties {
  private String apiUrl = "https://api.wablas.com/api/send-message";
  private String apiToken;
}
