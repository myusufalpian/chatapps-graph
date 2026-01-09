package id.xyz.chatapps_graph.infrastructure.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants.ErrorKeyConstants;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtil {
  private static ObjectMapper objectMapper;

  public static Map<String, Object> convertObjectToMap(Object object) {
    try {
      return objectMapper.convertValue(object, Map.class);
    } catch (Exception e) {
      log.error(String.format(ErrorKeyConstants.FAILED_CONVERT_MODEL_TO_MAP, object.getClass().getName()), e);
      return Collections.emptyMap();
    }
  }
}
