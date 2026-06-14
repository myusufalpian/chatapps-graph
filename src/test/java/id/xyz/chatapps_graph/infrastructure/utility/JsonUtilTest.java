package id.xyz.chatapps_graph.infrastructure.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUtilTest {

  @BeforeAll
  static void setup() {
    JsonUtil.setObjectMapper(new ObjectMapper());
  }

  // --- Test model for convert/stringToModel ---
  record TestDto(String name, int age) {}

  @Nested
  @DisplayName("convert")
  class ConvertTests {

    @Test
    @DisplayName("positive: object to Map")
    void convert_ObjectToMap_ReturnsMap() {
      TestDto dto = new TestDto("Alice", 30);

      @SuppressWarnings("unchecked")
      Map<String, Object> result = JsonUtil.convert(dto, Map.class);

      assertNotNull(result);
      assertEquals("Alice", result.get("name"));
      assertEquals(30, result.get("age"));
    }

    @Test
    @DisplayName("positive: Map to model")
    void convert_MapToModel_ReturnsModel() {
      Map<String, Object> map = Map.of("name", "Bob", "age", 25);

      TestDto result = JsonUtil.convert(map, TestDto.class);

      assertNotNull(result);
      assertEquals("Bob", result.name());
      assertEquals(25, result.age());
    }

    @Test
    @DisplayName("negative: incompatible types — returns null")
    void convert_IncompatibleTypes_ReturnsNull() {
      String invalidSource = "not a valid object for conversion to TestDto";

      TestDto result = JsonUtil.convert(invalidSource, TestDto.class);

      assertNull(result);
    }
  }

  @Nested
  @DisplayName("stringToMap")
  class StringToMapTests {

    @Test
    @DisplayName("positive: valid JSON string to map")
    void stringToMap_ValidJson_ReturnsMap() {
      String json = "{\"key\":\"value\",\"count\":42}";

      Map<String, Object> result = JsonUtil.stringToMap(json);

      assertNotNull(result);
      assertEquals("value", result.get("key"));
      assertEquals(42, result.get("count"));
    }

    @Test
    @DisplayName("negative: invalid JSON — returns empty map")
    void stringToMap_InvalidJson_ReturnsEmptyMap() {
      String invalidJson = "not valid json{{{";

      Map<String, Object> result = JsonUtil.stringToMap(invalidJson);

      assertNotNull(result);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("negative: null input — returns empty map")
    void stringToMap_Null_ReturnsEmptyMap() {
      Map<String, Object> result = JsonUtil.stringToMap(null);

      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("stringToModel")
  class StringToModelTests {

    @Test
    @DisplayName("positive: valid JSON to model")
    void stringToModel_ValidJson_ReturnsModel() {
      String json = "{\"name\":\"Charlie\",\"age\":35}";

      TestDto result = JsonUtil.stringToModel(json, TestDto.class);

      assertNotNull(result);
      assertEquals("Charlie", result.name());
      assertEquals(35, result.age());
    }

    @Test
    @DisplayName("negative: invalid JSON — returns null")
    void stringToModel_InvalidJson_ReturnsNull() {
      String invalidJson = "broken json";

      TestDto result = JsonUtil.stringToModel(invalidJson, TestDto.class);

      assertNull(result);
    }

    @Test
    @DisplayName("negative: null input — returns null")
    void stringToModel_Null_ReturnsNull() {
      TestDto result = JsonUtil.stringToModel(null, TestDto.class);

      assertNull(result);
    }

    @Test
    @DisplayName("boundary: JSON missing fields — deserializes with primitive defaults")
    void stringToModel_MissingFields_DeserializesPartially() {
      String json = "{\"name\":\"Partial\"}";

      TestDto result = JsonUtil.stringToModel(json, TestDto.class);

      assertNotNull(result);
      assertEquals("Partial", result.name());
      assertEquals(0, result.age());
    }
  }
}
