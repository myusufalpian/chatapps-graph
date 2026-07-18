package id.xyz.chatapps_graph.infrastructure.config.exception.handler;

import id.xyz.chatapps_graph.framework.dto.BaseErrorData;
import id.xyz.chatapps_graph.framework.dto.ErrorData;
import id.xyz.chatapps_graph.framework.dto.ErrorResponse;
import id.xyz.chatapps_graph.framework.dto.ValidationData;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants;
import id.xyz.chatapps_graph.infrastructure.service.TranslationService;
import id.xyz.chatapps_graph.infrastructure.utility.LocaleResolver;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
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

import id.xyz.chatapps_graph.infrastructure.monitoring.MetricsFacade;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GlobalExceptionHandler {

  private final TranslationService translationService;
  private final LocaleResolver localeResolver;
  private final MetricsFacade metricsFacade;

  public GlobalExceptionHandler(TranslationService translationService, LocaleResolver localeResolver, MetricsFacade metricsFacade) {
    this.translationService = translationService;
    this.localeResolver = localeResolver;
    this.metricsFacade = metricsFacade;
  }

  @ExceptionHandler(GeneralException.class)
  public ResponseEntity<ErrorResponse> handleGeneralException(GeneralException exc, HttpServletRequest request) {
    metricsFacade.incrementErrors(exc.getClass().getSimpleName());

    String locale = localeResolver.resolve(request);
    String translatedMessage = exc.getKey() != null
        ? translationService.translateError(exc.getKey(), locale)
        : exc.getMessage();

    ErrorData error = new BaseErrorData(
        exc.getKey(),
        translatedMessage,
        exc.getHttpCode(),
        exc.getValidation()
    );
    return ResponseEntity.status(exc.getHttpCode()).body(new ErrorResponse(error));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneralException(Exception exc, HttpServletRequest request) {
    metricsFacade.incrementErrors(exc.getClass().getSimpleName());
    log.error("Unhandled exception: ", exc);
    String locale = localeResolver.resolve(request);
    String translatedMessage = translationService.translateError(ErrorConstants.INTERNAL_SERVER_ERROR, locale);
    ErrorData error = new BaseErrorData(
        ErrorConstants.INTERNAL_SERVER_ERROR,
        translatedMessage,
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        null
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(error));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException exc, HttpServletRequest request) {
    metricsFacade.incrementErrors(exc.getClass().getSimpleName());
    ErrorData error = new BaseErrorData(
        ErrorConstants.BAD_REQUEST,
        translationService.translateError(ErrorConstants.BAD_REQUEST, localeResolver.resolve(request)),
        HttpStatus.BAD_REQUEST.value(),
        null
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(error));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException exc, HttpServletRequest request) {
    metricsFacade.incrementErrors(exc.getClass().getSimpleName());
    List<ValidationData> validationDataList = new ArrayList<>();
    for (FieldError fieldError : exc.getBindingResult().getFieldErrors()) {
      validationDataList.add(new ValidationData(fieldError.getField(), fieldError.getDefaultMessage()));
    }
    return buildValidationErrorResponse(validationDataList, request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException exc, HttpServletRequest request) {
    metricsFacade.incrementErrors(exc.getClass().getSimpleName());
    List<ValidationData> validationDataList = new ArrayList<>();
    for (ConstraintViolation<?> violation : exc.getConstraintViolations()) {
      validationDataList.add(new ValidationData(
          violation.getPropertyPath().toString(),
          violation.getMessage()
      ));
    }
    return buildValidationErrorResponse(validationDataList, request);
  }

  private ResponseEntity<ErrorResponse> buildValidationErrorResponse(
      List<ValidationData> validationDataList, HttpServletRequest request) {
    ErrorData error = new BaseErrorData(
        ErrorConstants.BAD_REQUEST,
        translationService.translateError(ErrorConstants.BAD_REQUEST, localeResolver.resolve(request)),
        HttpStatus.BAD_REQUEST.value(),
        validationDataList
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(error));
  }
}
