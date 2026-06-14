package id.xyz.chatapps_graph.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "chat.rate-limit")
public class ChatRateLimitProperties {

  private int messagePerMinute = 30;
  private int reactionPerMinute = 60;
}
