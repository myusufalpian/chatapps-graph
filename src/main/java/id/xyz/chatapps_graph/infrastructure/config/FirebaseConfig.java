package id.xyz.chatapps_graph.infrastructure.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class FirebaseConfig {

  public FirebaseConfig(@Value("${firebase.credentials-path:}") String credentialsPath,
      ResourceLoader resourceLoader) {
    if (!StringUtils.hasLength(credentialsPath)) {
      log.info("Firebase credentials path not configured, push notifications disabled");
      return;
    }
    try {
      Resource resource = resourceLoader.getResource(credentialsPath);
      if (!resource.exists()) {
        log.warn("Firebase credentials file not found at: {}, push notifications disabled", credentialsPath);
        return;
      }
      try (InputStream is = resource.getInputStream()) {
        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(is))
            .build();
        if (FirebaseApp.getApps().isEmpty()) {
          FirebaseApp.initializeApp(options);
          log.info("Firebase initialized successfully");
        }
      }
    } catch (Exception e) {
      log.warn("Failed to initialize Firebase: {}, push notifications disabled", e.getMessage());
    }
  }
}
