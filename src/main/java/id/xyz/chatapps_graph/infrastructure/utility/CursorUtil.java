package id.xyz.chatapps_graph.infrastructure.utility;

import java.time.OffsetDateTime;
import org.springframework.util.StringUtils;

public class CursorUtil {

  public record CursorPosition(OffsetDateTime timestamp, Long id) {}

  private CursorUtil() {}

  public static CursorPosition parse(String cursor) {
    if (!StringUtils.hasLength(cursor) || cursor.isBlank()) {
      return null;
    }
    int lastUnderscore = cursor.lastIndexOf('_');
    if (lastUnderscore < 0) {
      return null;
    }
    OffsetDateTime ts = OffsetDateTime.parse(cursor.substring(0, lastUnderscore));
    Long id = Long.parseLong(cursor.substring(lastUnderscore + 1));
    return new CursorPosition(ts, id);
  }

  public static String encode(OffsetDateTime timestamp, Long id) {
    return timestamp.toString() + "_" + id;
  }
}
