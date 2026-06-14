package id.xyz.chatapps_graph.infrastructure.utility;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtil {
  @Setter
  private static ObjectMapper objectMapper;

  private JsonUtil() {}

  public static <T> T convert(Object source, Class<T> targetType) {
    try {
      return objectMapper.convertValue(source, targetType);
    } catch (Exception e) {
      log.error("Failed to convert {} to {}: {}", source.getClass().getName(), targetType.getName(), e.getMessage());
      return null;
    }
  }

  public static Map<String, Object> stringToMap(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      log.error("Failed to convert string to map: {}", e.getMessage());
      return Collections.emptyMap();
    }
  }

  public static <T> T stringToModel(String json, Class<T> clazz) {
    try {
      return objectMapper.readValue(json, clazz);
    } catch (Exception e) {
      log.error("Failed to convert string to {}: {}", clazz.getName(), e.getMessage());
      return null;
    }
  }
}