package id.xyz.chatapps_graph.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "media")
public class MediaProperties {

  private long maxFileSize = 20971520L;
  private ImageProperties image = new ImageProperties();
  private VideoProperties video = new VideoProperties();

  @Getter
  @Setter
  public static class ImageProperties {
    private int maxDimension = 1920;
    private int quality = 85;
    private int thumbnailDimension = 200;
    private int thumbnailQuality = 60;
  }

  @Getter
  @Setter
  public static class VideoProperties {
    private int thumbnailTimeoutSeconds = 10;
  }
}
