package id.xyz.chatapps_graph.infrastructure.constant;

import lombok.experimental.UtilityClass;

public class GeneralConstants {
  @UtilityClass
  public static class ResponseConstants {
    public static final String SUCCESS_GET_DATA = "Get data success";
    public static final String SUCCESS_CREATE_DATA = "Create data success";
  }

  @UtilityClass
  public static class RedisConstants {
    public static final String REDIS_KEY_PREFIX = "chatapps-%s";
  }

  @UtilityClass
  public static class WebsocketConstants {
    public static final String WEBSOCKET_TOPIC = "/topic";
    public static final String WEBSOCKET_APP = "/app";
  }

  @UtilityClass
  public static class LoggingConstants {
    public static final String GET_CHAT_DATA_USER = "Get chat data user %s to user %s";
    public static final String EDIT_CHAT_DATA_USER = "Edit chat data user %s to user %s with chatId: %s";
    public static final String DELETE_CHAT_DATA_USER = "Delete chat data user %s to user %s with chatId: %s";
    public static final String REDIS_SET_WITH_TTL = "Redis SET: {} (TTL: {}s)";
    public static final String ADDED_ITEMS_TO_SET = "Added {} items to set {}";
  }

}
