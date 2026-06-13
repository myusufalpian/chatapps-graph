package id.xyz.chatapps_graph.infrastructure.utility;

import id.xyz.chatapps_graph.infrastructure.utility.CursorUtil.CursorPosition;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CursorUtilTest {

  private static final OffsetDateTime TS = OffsetDateTime.of(2026, 6, 13, 10, 0, 0, 0, ZoneOffset.UTC);

  @Test
  @DisplayName("parse: valid cursor returns CursorPosition with correct timestamp and id")
  void parse_ValidCursor_ReturnsCursorPosition() {
    String cursor = TS.toString() + "_42";

    CursorPosition result = CursorUtil.parse(cursor);

    assertNotNull(result);
    assertEquals(TS, result.timestamp());
    assertEquals(42L, result.id());
  }

  @Test
  @DisplayName("parse: null returns null")
  void parse_Null_ReturnsNull() {
    assertNull(CursorUtil.parse(null));
  }

  @Test
  @DisplayName("parse: blank string returns null")
  void parse_Blank_ReturnsNull() {
    assertNull(CursorUtil.parse("   "));
  }

  @Test
  @DisplayName("parse: no underscore returns null")
  void parse_InvalidFormat_ReturnsNull() {
    assertNull(CursorUtil.parse("nounderscore"));
  }

  @Test
  @DisplayName("encode: returns timestamp_id format")
  void encode_ReturnsCorrectFormat() {
    String result = CursorUtil.encode(TS, 99L);

    assertEquals(TS.toString() + "_99", result);
  }
}
