package id.xyz.chatapps_graph.infrastructure.config.exception;

import id.xyz.chatapps_graph.framework.dto.ValidationData;
import java.util.List;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {
  private final Integer httpCode;
  private final String key;
  private final List<ValidationData> validation;

  public GeneralException(Integer httpCode, String key, String message, List<ValidationData> validation) {
    super(message);
    this.httpCode = httpCode;
    this.key = key;
    this.validation = validation;
  }

  public GeneralException(Integer httpCode, String key, String message) {
    super(message);
    this.httpCode = httpCode;
    this.key = key;
    this.validation = null;
  }

  public GeneralException(Integer httpCode, String message) {
    super(message);
    this.httpCode = httpCode;
    this.key = null;
    this.validation = null;
  }
}
