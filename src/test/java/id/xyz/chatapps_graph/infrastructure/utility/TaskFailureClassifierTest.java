package id.xyz.chatapps_graph.infrastructure.utility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;

class TaskFailureClassifierTest {
  @Test
  void transientTimeoutIsRetryable() {
    assertTrue(TaskFailureClassifier.isRetryable(new SocketTimeoutException("timeout")));
  }

  @Test
  void clientErrorIsNotRetryable() {
    assertFalse(TaskFailureClassifier.isRetryable(
        new GeneralException(400, "INVALID_AUDIO", "Unsupported audio format")));
  }

  @Test
  void unknownPermanentFailureIsNotRetryable() {
    assertFalse(TaskFailureClassifier.isRetryable(new IllegalArgumentException("invalid payload")));
  }
}
