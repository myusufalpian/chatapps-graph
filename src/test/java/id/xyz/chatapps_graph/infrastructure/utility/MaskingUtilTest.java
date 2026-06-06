package id.xyz.chatapps_graph.infrastructure.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaskingUtilTest {

  @Test void maskPhone_null() { assertEquals("***", MaskingUtil.maskPhone(null)); }
  @Test void maskPhone_empty() { assertEquals("***", MaskingUtil.maskPhone("")); }
  @Test void maskPhone_short() { assertEquals("***", MaskingUtil.maskPhone("ab")); }
  @Test void maskPhone_exactly4() {
    String result = MaskingUtil.maskPhone("1234");
    assertTrue(result.startsWith("123"));
    assertTrue(result.endsWith("34"));
    assertTrue(result.contains("*"));
  }
  @Test void maskPhone_normal() {
    String result = MaskingUtil.maskPhone("+628123456789");
    assertTrue(result.startsWith("+62"));
    assertTrue(result.endsWith("89"));
    assertTrue(result.contains("*"));
    assertEquals(13, result.length());
  }

  @Test void maskToken_null() { assertEquals("***", MaskingUtil.maskToken(null)); }
  @Test void maskToken_empty() { assertEquals("***", MaskingUtil.maskToken("")); }
  @Test void maskToken_short() { assertEquals("***", MaskingUtil.maskToken("abc")); }
  @Test void maskToken_normal() {
    String result = MaskingUtil.maskToken("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9");
    assertTrue(result.startsWith("eyJh"));
    assertTrue(result.endsWith("VCJ9"));
    assertTrue(result.contains("****"));
  }
}
