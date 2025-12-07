package id.xyz.chatapps_graph.infrastructure.config.storage;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
  @Bean
  public MinioClient minioClient(
      @Value("${application.minio.endpoint}") String endpoint,
      @Value("${application.minio.access-key}") String accessKey,
      @Value("${application.minio.secret-key}") String secretKey) {

    return MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build();
  }
}
