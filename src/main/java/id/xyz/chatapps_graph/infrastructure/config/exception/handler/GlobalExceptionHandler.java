package id.xyz.chatapps_graph.infrastructure.config.exception.handler;

import id.xyz.chatapps_graph.framework.dto.BaseErrorData;
import id.xyz.chatapps_graph.framework.dto.ErrorData;
import id.xyz.chatapps_graph.framework.dto.ErrorResponse;
import id.xyz.chatapps_graph.framework.dto.ValidationData;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants.ResponseConstants;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler({GeneralException.class})
  public ResponseEntity<ErrorResponse> handleGeneralException(GeneralException exc) {
    ErrorData error = new BaseErrorData(
        exc.getKey(),
        exc.getMessage(),
        exc.getHttpCode(),
        null
    );
    return ResponseEntity.status(exc.getHttpCode()).body(new ErrorResponse(error));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneralException(Exception exc) {
    ErrorData error = new BaseErrorData(
        ErrorConstants.INTERNAL_SERVER_ERROR,
        exc.getMessage(),
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        null
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(new ErrorResponse(error));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException exc) {
    ErrorData error = new BaseErrorData(
        ErrorConstants.BAD_REQUEST,
        String.format(ResponseConstants.METHOD_ARGUMENT_TYPE_MISMATCH, exc.getName(), exc.getValue()),
        HttpStatus.BAD_REQUEST.value(),
        null
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(new ErrorResponse(error));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exc) {
    List<ValidationData> validationDataList = new ArrayList<>();
    for (FieldError fieldError : exc.getBindingResult().getFieldErrors()) {
      validationDataList.add(new ValidationData(fieldError.getField(), fieldError.getDefaultMessage()));
    }
    return buildValidationErrorResponse(validationDataList);
  }

  private ResponseEntity<ErrorResponse> buildValidationErrorResponse(List<ValidationData> validationDataList) {
    ErrorData error = new BaseErrorData(
        ErrorConstants.BAD_REQUEST,
        ResponseConstants.FIELD_VALIDATION_ERROR,
        HttpStatus.BAD_REQUEST.value(),
        validationDataList
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(new ErrorResponse(error));
  }
}
