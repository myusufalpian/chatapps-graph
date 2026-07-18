package id.xyz.chatapps_graph.infrastructure.utility;

import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.sql.SQLTransientException;
import java.util.concurrent.TimeoutException;
import org.springframework.dao.TransientDataAccessException;

public final class TaskFailureClassifier {
  private TaskFailureClassifier() {}

  public static boolean isRetryable(Throwable failure) {
    Throwable current = failure;
    while (current != null) {
      if (current instanceof GeneralException exception) return exception.getHttpCode() >= 500;
      if (current instanceof ConnectException || current instanceof SocketTimeoutException
          || current instanceof SQLTransientException
          || current instanceof TransientDataAccessException
          || current instanceof TimeoutException
          || current instanceof IOException) return true;
      current = current.getCause();
    }
    return false;
  }
}
