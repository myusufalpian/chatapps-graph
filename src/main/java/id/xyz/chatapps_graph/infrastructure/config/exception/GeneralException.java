package id.xyz.chatapps_graph.infrastructure.config.exception;

import id.xyz.chatapps_graph.framework.dto.ValidationData;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class GeneralException extends RuntimeException {
  private final Integer httpCode;
  private final String key;
  private final String message;
  private final List<ValidationData> validation;

  public GeneralException(Integer httpCode, String key, String message, List<ValidationData> validation) {
    this.httpCode = httpCode;
    this.key = key;
    this.message = message;
    this.validation = validation;
  }

  public GeneralException(Integer httpCode, String key, String message) {
    this.httpCode = httpCode;
    this.key = key;
    this.message = message;
    this.validation = null;
  }

  public GeneralException(Integer httpCode, String message) {
    this.httpCode = httpCode;
    this.key = null;
    this.message = message;
    this.validation = null;
  }
}
