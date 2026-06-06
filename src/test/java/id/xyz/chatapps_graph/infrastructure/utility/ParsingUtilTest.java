package id.xyz.chatapps_graph.infrastructure.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParsingUtilTest {

  @Test void null_returnsDefault() { assertEquals(0, ParsingUtil.parseIntSafe(null, 0)); }
  @Test void empty_returnsDefault() { assertEquals(5, ParsingUtil.parseIntSafe("", 5)); }
  @Test void blank_returnsDefault() { assertEquals(7, ParsingUtil.parseIntSafe("  ", 7)); }
  @Test void valid_returnsParsed() { assertEquals(42, ParsingUtil.parseIntSafe("42", 0)); }
  @Test void negative_returnsParsed() { assertEquals(-5, ParsingUtil.parseIntSafe("-5", 0)); }
  @Test void nonNumeric_returnsDefault() { assertEquals(0, ParsingUtil.parseIntSafe("abc", 0)); }
  @Test void overflow_returnsDefault() { assertEquals(0, ParsingUtil.parseIntSafe("99999999999", 0)); }
}
