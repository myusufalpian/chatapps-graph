package id.xyz.chatapps_graph.infrastructure.utility;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashUtilTest {

  @Test
  @DisplayName("sha256: same input always returns same hash")
  void sha256_ReturnsConsistentHash() {
    String hash1 = HashUtil.sha256("hello");
    String hash2 = HashUtil.sha256("hello");

    assertEquals(hash1, hash2);
  }

  @Test
  @DisplayName("sha256: different inputs produce different hashes")
  void sha256_DifferentInput_DifferentOutput() {
    String hash1 = HashUtil.sha256("hello");
    String hash2 = HashUtil.sha256("world");

    assertNotEquals(hash1, hash2);
  }

  @Test
  @DisplayName("sha256: output is 64 character hex string")
  void sha256_Length64Hex() {
    String hash = HashUtil.sha256("test");

    assertEquals(64, hash.length());
    assertTrue(hash.matches("[0-9a-f]{64}"));
  }
}
