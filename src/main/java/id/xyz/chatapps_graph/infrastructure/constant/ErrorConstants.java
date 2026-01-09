package id.xyz.chatapps_graph.infrastructure.constant;

import lombok.experimental.UtilityClass;

public class ErrorConstants {
  public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
  public static final String BAD_REQUEST = "BAD_REQUEST";
  public static final String UNAUTHORIZED = "UNAUTHORIZED";
  public static final String FORBIDDEN = "FORBIDDEN";
  public static final String NOT_FOUND = "NOT_FOUND";
  public static final String VALIDATION_ERROR = "REQUEST_VALIDATION_ERROR";

  @UtilityClass
  public static class ErrorKeyConstants {
    public static final String STORAGE_ERROR = "Storage error: %s";
    public static final String USER_NOT_FOUND = "User not found";
    public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    public static final String METHOD_ARGUMENT_TYPE_MISMATCH = "Type mismatch in '%s' type '%s' but value '%s'.";
    public static final String FIELD_VALIDATION_ERROR = "Field validation error";
    public static final String FAILED_CONVERT_MODEL_TO_MAP = "Failed to convert %s to map";
    public static final String ADD_SET_REDIS_FAILED = "Failed to add to set %s";
    public static final String ADD_DATA_TO_REDIS_FAILED = "Failed to add data to Redis %s";
    public static final String SIGNIN_FAILED = "Sign In Failed";

  }
  @UtilityClass
  public static class ErrorMessageConstants {
    public static final String INVALID_CREDENTIALS = "Invalid Credentials";
    public static final String INVALID_USER = "User is not active or does not exist";
    
  }

  @UtilityClass
  public static class LoggingConstants {
    public static final String ERROR_LOG = "Error: {}";
    public static final String UNDEFINED_EXCEPTION_HANDLER = "Undefined Exception Handler: {}";
    public static final String ERROR_TRACE_LOG = "Error trace: {}";
    public static final String LOGIN_ERROR = "Login failed for phone: [{}]";
    public static final String ERROR_VALIDATION_LOG = "Error validation: {}";
    public static final String FAILED_SET_KEY_REDIS = "Failed to set key {} in Redis";
    public static final String FAILED_GET_KEY_FROM_REDIS = "Failed to get key {} from Redis";
    public static final String FAILED_DELETE_KEY_FROM_REDIS = "Failed to delete key {} from Redis";
    public static final String FAILED_CHECK_KEY_FROM_REDIS = "Failed to check existence of key {}";
    public static final String FAILED_ADD_TO_SET_KEY_REDIS = "Failed to add to set {}";
    public static final String FAILED_GET_SET_MEMBER_FROM_REDIS = "Failed to get set members for {}";
  }
}
