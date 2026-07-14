package id.xyz.chatapps_graph.framework.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants;
import id.xyz.chatapps_graph.infrastructure.utility.JsonUtil;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SendMessageRequest(
    String conversationUuid,
    String recipientUuid,
    String messageType,
    String content,
    String replyToMessageUuid,
    String attachmentType
) {

  public static SendMessageRequest fromJson(String json) {
    try {
      return JsonUtil.stringToModel(json, SendMessageRequest.class);
    } catch (Exception e) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), ErrorConstants.INVALID_METADATA, "Invalid metadata JSON");
    }
  }
}
