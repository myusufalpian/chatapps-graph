package id.xyz.chatapps_graph.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

public abstract class RedisIntegrationBase {

  static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
      .withExposedPorts(6379);

  static {
    REDIS.start();
  }

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
  }

  static boolean isDockerAvailable() {
    try {
      return DockerClientFactory.instance().isDockerAvailable();
    } catch (Exception e) {
      return false;
    }
  }
}
