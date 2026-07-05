package id.xyz.chatapps_graph.infrastructure.utility;

import id.xyz.chatapps_graph.infrastructure.config.properties.MediaProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VideoThumbnailService {

  private final int timeoutSeconds;
  private final Boolean ffmpegAvailable;

  public VideoThumbnailService(MediaProperties mediaProperties) {
    this.timeoutSeconds = mediaProperties.getVideo() != null
        ? mediaProperties.getVideo().getThumbnailTimeoutSeconds() : 10;
    this.ffmpegAvailable = checkFfmpeg();
  }

  public byte[] extractFirstFrame(Path videoFile, int dimension) {
    if (!ffmpegAvailable) {
      return null;
    }

    Path outputFile = null;
    try {
      outputFile = Files.createTempFile("thumb_", ".jpg");
      ProcessBuilder pb = new ProcessBuilder(
          "ffmpeg", "-i", videoFile.toString(),
          "-vframes", "1",
          "-vf", "scale=" + dimension + ":-1",
          "-y", outputFile.toString()
      );
      pb.redirectErrorStream(true);
      pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
      Process process = pb.start();

      boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        log.warn("ffmpeg process timed out for video thumbnail");
        return null;
      }

      if (process.exitValue() != 0) {
        log.warn("ffmpeg exited with code {} for video thumbnail", process.exitValue());
        return null;
      }

      return Files.readAllBytes(outputFile);
    } catch (IOException | InterruptedException e) {
      log.warn("Failed to extract video thumbnail: {}", e.getMessage());
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return null;
    } finally {
      if (outputFile != null) {
        try {
          Files.deleteIfExists(outputFile);
        } catch (IOException e) {
          log.warn("Failed to delete temp thumbnail file: {}", e.getMessage());
        }
      }
    }
  }

  private Boolean checkFfmpeg() {
    try {
      Process process = new ProcessBuilder("ffmpeg", "-version")
          .redirectErrorStream(true)
          .redirectOutput(ProcessBuilder.Redirect.DISCARD)
          .start();
      boolean finished = process.waitFor(5, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        log.warn("ffmpeg not available, video thumbnails disabled");
        return false;
      }
      boolean available = process.exitValue() == 0;
      if (!available) {
        log.warn("ffmpeg not available, video thumbnails disabled");
      }
      return available;
    } catch (IOException | InterruptedException e) {
      log.warn("ffmpeg not available, video thumbnails disabled");
      return false;
    }
  }
}
