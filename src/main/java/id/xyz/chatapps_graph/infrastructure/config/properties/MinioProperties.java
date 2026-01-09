package id.xyz.chatapps_graph.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "application.minio")
public class MinioProperties {
  private String endpoint;

  private String accessKey;

  private String secretKey;

  private String bucket;
}
