package id.xyz.chatapps_graph.infrastructure.utility;

import org.springframework.util.StringUtils;

public final class ParsingUtil {

  private ParsingUtil() {}

  public static int parseIntSafe(String value, int defaultValue) {
    if (!StringUtils.hasLength(value)) return defaultValue;
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
