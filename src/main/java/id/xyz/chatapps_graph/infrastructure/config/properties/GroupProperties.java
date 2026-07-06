package id.xyz.chatapps_graph.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "group")
public class GroupProperties {

  private int maxMembers = 200;
  private AvatarProperties avatar = new AvatarProperties();

  @Getter
  @Setter
  public static class AvatarProperties {
    private int maxDimension = 500;
    private int quality = 85;
  }
}
