package id.xyz.chatapps_graph.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "auth.otp")
public class AuthenticationOtpProperties {
  private Integer duration;
  private String prefix;
  private Integer maxCount;
  private Integer maxAttempts;
  private Integer cooldownSeconds = 60;
  private Integer otpLength = 6;
  private Integer maxIpCount = 10;
  private Integer ipWindowSeconds = 3600;
}
