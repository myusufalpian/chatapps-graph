package id.xyz.chatapps_graph.infrastructure.config.exception.handler;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import id.xyz.chatapps_graph.framework.dto.BaseErrorData;
import id.xyz.chatapps_graph.framework.dto.ErrorData;
import id.xyz.chatapps_graph.framework.dto.ValidationData;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants.ResponseConstants;
import id.xyz.chatapps_graph.infrastructure.utility.JsonUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {

    if (ex instanceof GeneralException generalEx) {

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
          .extensions(JsonUtil.convertObjectToMap(errorData))
          .build();
    }

    if (ex instanceof ConstraintViolationException valEx) {
      List<ValidationData> validationList = new ArrayList<>();

      for (ConstraintViolation<?> violation : valEx.getConstraintViolations()) {
        String fieldName = getLeafNode(violation.getPropertyPath().toString());
        String message = violation.getMessage();
        validationList.add(new ValidationData(fieldName, message));
      }

      BaseErrorData errorData = new BaseErrorData(
          ErrorConstants.VALIDATION_ERROR,
          ResponseConstants.FIELD_VALIDATION_ERROR,
          HttpStatus.BAD_REQUEST.value(),
          validationList
      );

      return GraphqlErrorBuilder.newError()
          .errorType(ErrorType.BAD_REQUEST)
          .message("Input validation failed")
          .path(env.getExecutionStepInfo().getPath())
          .location(env.getField().getSourceLocation())
          .extensions(JsonUtil.convertObjectToMap(errorData))
          .build();
    }

    if (ex instanceof MethodArgumentNotValidException validEx) {
      List<ValidationData> validationList = new ArrayList<>();

      for (FieldError fieldError : validEx.getBindingResult().getFieldErrors()) {
        ValidationData validationData = ValidationData.builder().field(fieldError.getField())
            .message(fieldError.getDefaultMessage()).build();
        validationList.add(validationData);
      }

      BaseErrorData errorData = new BaseErrorData(
          ErrorConstants.BAD_REQUEST,
          ResponseConstants.FIELD_VALIDATION_ERROR,
          HttpStatus.BAD_REQUEST.value(),
          validationList
      );

      return GraphqlErrorBuilder.newError()
          .errorType(ErrorType.BAD_REQUEST)
          .message("Field Validation Error")
          .path(env.getExecutionStepInfo().getPath())
          .location(env.getField().getSourceLocation())
          .extensions(JsonUtil.convertObjectToMap(errorData))
          .build();
    }

    if (ex instanceof MethodArgumentTypeMismatchException mismatchEx) {
      String message = String.format(ResponseConstants.METHOD_ARGUMENT_TYPE_MISMATCH,
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
          .extensions(JsonUtil.convertObjectToMap(errorData))
          .build();
    }

    return null;
  }

  private ErrorType mapStatusToErrorType(int status) {
    return switch (status) {
      case 400 -> ErrorType.BAD_REQUEST;
      case 401, 403 -> ErrorType.FORBIDDEN;
      case 404 -> ErrorType.NOT_FOUND;
      default -> ErrorType.INTERNAL_ERROR;
    };
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
