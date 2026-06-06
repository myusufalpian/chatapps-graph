package id.xyz.chatapps_graph.infrastructure.config.exception.handler;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import id.xyz.chatapps_graph.framework.dto.BaseErrorData;
import id.xyz.chatapps_graph.framework.dto.ErrorData;
import id.xyz.chatapps_graph.framework.dto.ValidationData;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants.ErrorKeyConstants;
import id.xyz.chatapps_graph.infrastructure.utility.JsonUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Component
@Slf4j
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, @NotNull DataFetchingEnvironment env) {

    switch (ex) {
      case GeneralException generalEx -> {
        String key = generalEx.getKey();
        String detail = generalEx.getMessage();
        Integer status = generalEx.getHttpCode();
        List<ValidationData> validation = generalEx.getValidation();

        ErrorData errorData = new BaseErrorData(key, detail, status, validation);

        return GraphqlErrorBuilder.newError()
            .errorType(mapStatusToErrorType(status))
            .message(detail)
            .path(env.getExecutionStepInfo().getPath())
            .location(env.getField().getSourceLocation())
            .extensions(toSafeExtensions(errorData))
            .build();
      }
      case ConstraintViolationException valEx -> {
        List<ValidationData> validationList = new ArrayList<>();

        for (ConstraintViolation<?> violation : valEx.getConstraintViolations()) {
          String fieldName = getLeafNode(violation.getPropertyPath().toString());
          String message = violation.getMessage();
          validationList.add(new ValidationData(fieldName, message));
        }

        BaseErrorData errorData = new BaseErrorData(
            ErrorConstants.VALIDATION_ERROR,
            ErrorKeyConstants.FIELD_VALIDATION_ERROR,
            HttpStatus.BAD_REQUEST.value(),
            validationList
        );

        return GraphqlErrorBuilder.newError()
            .errorType(ErrorType.BAD_REQUEST)
            .message("Input validation failed")
            .path(env.getExecutionStepInfo().getPath())
            .location(env.getField().getSourceLocation())
            .extensions(toSafeExtensions(errorData))
            .build();
      }
      case MethodArgumentNotValidException validEx -> {
        List<ValidationData> validationList = new ArrayList<>();

        for (FieldError fieldError : validEx.getBindingResult().getFieldErrors()) {
          validationList.add(new ValidationData(fieldError.getField(), fieldError.getDefaultMessage()));
        }

        BaseErrorData errorData = new BaseErrorData(
            ErrorConstants.BAD_REQUEST,
            ErrorKeyConstants.FIELD_VALIDATION_ERROR,
            HttpStatus.BAD_REQUEST.value(),
            validationList
        );

        return GraphqlErrorBuilder.newError()
            .errorType(ErrorType.BAD_REQUEST)
            .message("Field Validation Error")
            .path(env.getExecutionStepInfo().getPath())
            .location(env.getField().getSourceLocation())
            .extensions(toSafeExtensions(errorData))
            .build();
      }
      case MethodArgumentTypeMismatchException mismatchEx -> {
        String message = String.format(ErrorKeyConstants.METHOD_ARGUMENT_TYPE_MISMATCH,
            mismatchEx.getName(),
            mismatchEx.getRequiredType() != null ? mismatchEx.getRequiredType().getSimpleName() : "unknown",
            mismatchEx.getValue());

        BaseErrorData errorData = new BaseErrorData(
            ErrorConstants.BAD_REQUEST,
            message,
            HttpStatus.BAD_REQUEST.value(),
            null
        );

        return GraphqlErrorBuilder.newError()
            .errorType(ErrorType.BAD_REQUEST)
            .message(message)
            .path(env.getExecutionStepInfo().getPath())
            .location(env.getField().getSourceLocation())
            .extensions(toSafeExtensions(errorData))
            .build();
      }
      default -> {
        log.error("Unhandled GraphQL exception: ", ex);

        BaseErrorData errorData = new BaseErrorData(
            ErrorConstants.INTERNAL_SERVER_ERROR,
            ErrorKeyConstants.INTERNAL_SERVER_ERROR,
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            null
        );

        return GraphqlErrorBuilder.newError()
            .errorType(ErrorType.INTERNAL_ERROR)
            .message(ErrorKeyConstants.INTERNAL_SERVER_ERROR)
            .path(env.getExecutionStepInfo().getPath())
            .location(env.getField().getSourceLocation())
            .extensions(toSafeExtensions(errorData))
            .build();
      }
    }
  }

  private ErrorType mapStatusToErrorType(int status) {
    return switch (status) {
      case 400 -> ErrorType.BAD_REQUEST;
      case 401 -> ErrorType.UNAUTHORIZED;
      case 403 -> ErrorType.FORBIDDEN;
      case 404 -> ErrorType.NOT_FOUND;
      default -> ErrorType.INTERNAL_ERROR;
    };
  }

  private Map<String, Object> toSafeExtensions(ErrorData errorData) {
    try {
      return JsonUtil.convertObjectToMap(errorData);
    } catch (Exception e) {
      log.warn("Failed to serialize error extensions: {}", e.getMessage());
      return Map.of("status", errorData.status(), "key", String.valueOf(errorData.key()));
    }
  }

  private String getLeafNode(String propertyPath) {
    if (!StringUtils.hasText(propertyPath)) {
      return propertyPath;
    }
    int lastDotIndex = propertyPath.lastIndexOf('.');
    if (lastDotIndex != -1) {
      return propertyPath.substring(lastDotIndex + 1);
    }
    return propertyPath;
  }
}
