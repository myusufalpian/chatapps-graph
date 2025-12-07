package id.xyz.chatapps_graph.infrastructure.utility;

import static id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants.LoggingConstants.ERROR_LOG;
import static id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants.LoggingConstants.ERROR_TRACE_LOG;
import static id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants.LoggingConstants.ERROR_VALIDATION_LOG;

import id.xyz.chatapps_graph.framework.dto.ValidationData;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExceptionUtil {
  public static GeneralException error(String key, String detail, Integer status, Exception exc) {
    log.error(ERROR_LOG, exc.getMessage());
    log.error(ERROR_TRACE_LOG, exc.fillInStackTrace());
    return new GeneralException(status, key, detail);
  }

  public GeneralException error(String key, String detail, Integer status, List<ValidationData> validation, Exception exc) {
    log.error(ERROR_VALIDATION_LOG, exc.getMessage());
    return new GeneralException(status, key, detail, validation);
  }

}
