package id.xyz.chatapps_graph.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "notification.whatsapp.fonnte")
public class FonnteProperties {
  private String apiUrl = "https://api.fonnte.com/send";
  private String apiToken;
}
