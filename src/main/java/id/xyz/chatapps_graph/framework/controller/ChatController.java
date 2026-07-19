package id.xyz.chatapps_graph.framework.controller;

import id.xyz.chatapps_graph.applications.usecase.AttachmentService;
import id.xyz.chatapps_graph.applications.usecase.ConversationListService;
import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.applications.usecase.MessageEditResult;
import id.xyz.chatapps_graph.applications.usecase.MessageService;
import id.xyz.chatapps_graph.applications.usecase.ReadReceiptResult;
import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.framework.dto.DisappearingTtlRequest;
import id.xyz.chatapps_graph.framework.dto.DisappearingTtlResponse;

import id.xyz.chatapps_graph.domain.enums.ReceiptStatus;
import id.xyz.chatapps_graph.framework.dto.BaseResponse;
import id.xyz.chatapps_graph.framework.dto.ConversationListResponse;
import id.xyz.chatapps_graph.framework.dto.CreateMultiChatRequest;
import id.xyz.chatapps_graph.framework.dto.CursorPageResponse;
import id.xyz.chatapps_graph.framework.dto.EditMessageRequest;
import id.xyz.chatapps_graph.framework.dto.ForwardMessageRequest;
import id.xyz.chatapps_graph.framework.dto.MarkReadRequest;
import id.xyz.chatapps_graph.framework.dto.MessageEditedEvent;
import id.xyz.chatapps_graph.framework.dto.MessageReactionResult;
import id.xyz.chatapps_graph.framework.dto.MessageResponse;
import id.xyz.chatapps_graph.framework.dto.MessageSearchResponse;
import id.xyz.chatapps_graph.framework.dto.MultiChatResponse;
import id.xyz.chatapps_graph.framework.dto.ReactionRequest;
import id.xyz.chatapps_graph.framework.dto.ReadReceiptEvent;
import id.xyz.chatapps_graph.framework.dto.SearchResultItem;
import id.xyz.chatapps_graph.framework.dto.SendMessageRequest;
import id.xyz.chatapps_graph.framework.dto.SendMessageResult;
import id.xyz.chatapps_graph.framework.mapper.MessageResponseMapper;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants;
import id.xyz.chatapps_graph.infrastructure.mapper.MessageMapper;
import id.xyz.chatapps_graph.infrastructure.utility.CursorUtil;
import id.xyz.chatapps_graph.infrastructure.utility.LocaleResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import id.xyz.chatapps_graph.applications.usecase.WebSocketBroadcastService;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController extends BaseApiController {

  private final MessageService messageService;
  private final AttachmentService attachmentService;
  private final ConversationService conversationService;
  private final ConversationListService conversationListService;
  private final WebSocketBroadcastService broadcastService;
  private final LocaleResolver localeResolver;
  private final MessageResponseMapper messageResponseMapper;

  @PostMapping("/messages")
  public ResponseEntity<BaseResponse<MessageResponse>> sendMessage(
      @RequestAttribute("X-User-Id") Long userId,
      @RequestPart(value = "file", required = false) MultipartFile file,
      @RequestPart("metadata") String metadataJson) {

    SendMessageRequest request;
    try {
      request = id.xyz.chatapps_graph.infrastructure.utility.JsonUtil.stringToModel(metadataJson, SendMessageRequest.class);
    } catch (Exception e) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), ErrorConstants.INVALID_METADATA, "Invalid metadata JSON");
    }

    Long attachmentId = null;
    if (file != null && !file.isEmpty()) {
      String attType = request.attachmentType() != null ? request.attachmentType() : "FILE";
      Attachment attachment = attachmentService.validateAndUpload(file, attType, userId);
      attachmentId = attachment.getAttachmentId();
    }

    SendMessageResult result = messageService.sendMessage(userId, request.recipientUuid(),
        request.conversationUuid(), request.messageType(), request.content(),
        attachmentId, request.replyToMessageUuid());

    MessageResponse response = MessageMapper.toResponse(result.message(), result.senderUuid(), result.conversationUuid(),
        null, null, null, null, null, ReceiptStatus.SENT.getValue());

    broadcastService.broadcast("/topic/chat/" + result.conversationUuid(), response);
    return created(response, "Message sent");
  }

  @GetMapping("/conversations/{uuid}/messages")
  public ResponseEntity<BaseResponse<CursorPageResponse<MessageResponse>>> listMessages(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("uuid") String uuid,
      @RequestParam(value = "cursor", required = false) String cursor,
      @RequestParam(value = "limit", defaultValue = "20") int limit,
      HttpServletRequest httpRequest) {

    Conversation conversation = conversationService.findConversationByUuid(uuid);
    conversationService.validateParticipant(conversation.getConversationId(), userId);

    String locale = localeResolver.resolve(httpRequest);

    int fetchLimit = Math.min(limit, 50);
    List<Message> messages = messageService.listMessages(conversation.getConversationId(), userId, cursor, fetchLimit + 1);

    boolean hasMore = messages.size() > fetchLimit;
    List<Message> resultMessages = hasMore ? messages.subList(0, fetchLimit) : messages;

    String nextCursor = null;
    if (hasMore) {
      Message last = resultMessages.getLast();
      nextCursor = CursorUtil.encode(last.getCreatedAt(), last.getMessageId());
    }

    List<MessageResponse> responses = messageResponseMapper.toResponseList(resultMessages, uuid, userId, locale);
    return success(new CursorPageResponse<>(responses, nextCursor, hasMore), "Messages retrieved");
  }

  @DeleteMapping("/messages/{uuid}")
  public ResponseEntity<BaseResponse<Void>> deleteMessage(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("uuid") String uuid,
      @RequestParam("mode") String mode) {

    messageService.deleteMessage(uuid, userId, mode);
    return success("Message deleted");
  }

  @PutMapping("/messages/{uuid}")
  public ResponseEntity<BaseResponse<MessageResponse>> editMessage(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("uuid") String uuid,
      @Valid @RequestBody EditMessageRequest request) {

    MessageEditResult editResult = messageService.editMessage(userId, uuid, request.content());
    Message edited = editResult.message();

    MessageResponse response = MessageMapper.toResponse(edited,
        editResult.senderUuid(), editResult.conversationUuid(),
        null, null, null, null, null, ReceiptStatus.SENT.getValue());

    if (editResult.changed()) {
      broadcastService.broadcast("/topic/chat/" + editResult.conversationUuid(),
          new MessageEditedEvent("MESSAGE_EDITED", edited.getMessageUuid(), editResult.conversationUuid(),
              edited.getContent(), edited.getEditedAt()));
    }

    return success(response, "Message edited");
  }

  @PutMapping("/messages/read")
  public ResponseEntity<BaseResponse<Void>> markAsRead(
      @RequestAttribute("X-User-Id") Long userId,
      @RequestBody MarkReadRequest request) {

    Conversation conversation = conversationService.findConversationByUuid(request.conversationUuid());
    conversationService.validateParticipant(conversation.getConversationId(), userId);

    ReadReceiptResult result = messageService.markAsRead(request.conversationUuid(), userId);
    if (result.receiptsUpdated()) {
      ReadReceiptEvent event = new ReadReceiptEvent("MESSAGE_READ", request.conversationUuid(),
          result.readerUuid(), "READ");

      result.targetUserPhones().forEach(phone ->
          broadcastService.sendToUser(phone, "/queue/chat/receipts", event));
    }

    return success("Marked as read");
  }

  @GetMapping("/conversations")
  public ResponseEntity<BaseResponse<ConversationListResponse>> listConversations(
      @RequestAttribute("X-User-Id") Long userId,
      @RequestParam(value = "filter", defaultValue = "ALL") String filter,
      @RequestParam(value = "cursor", required = false) String cursor,
      @RequestParam(value = "limit", defaultValue = "20") int limit) {

    ConversationListResponse response = conversationListService.listConversations(userId, filter, cursor, limit);
    return success(response, "Conversations retrieved");
  }

  @GetMapping("/messages/search")
  public ResponseEntity<BaseResponse<MessageSearchResponse>> searchMessages(
      @RequestAttribute("X-User-Id") Long userId,
      @RequestParam("q") String query,
      @RequestParam(value = "limit", defaultValue = "20") int limit,
      @RequestParam(value = "cursor", required = false) String cursor,
      @RequestParam(value = "conversationUuid", required = false) String conversationUuid) {

    if (!StringUtils.hasLength(query)) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), ErrorConstants.INVALID_QUERY, "Search query is required");
    }

    int fetchLimit = Math.min(limit, 50);
    List<Message> messages = messageService.searchMessages(userId, query, conversationUuid, cursor, fetchLimit + 1);

    boolean hasMore = messages.size() > fetchLimit;
    List<Message> resultMessages = hasMore ? messages.subList(0, fetchLimit) : messages;

    List<SearchResultItem> results = messageResponseMapper.toSearchResultItemList(resultMessages);

    String nextCursor = null;
    if (hasMore) {
      Message last = resultMessages.getLast();
      nextCursor = CursorUtil.encode(last.getCreatedAt(), last.getMessageId());
    }

    return success(MessageSearchResponse.builder()
        .results(results).nextCursor(nextCursor).hasMore(hasMore).build(), "Search results");
  }

  @PostMapping("/conversations/multi-chat")
  public ResponseEntity<BaseResponse<MultiChatResponse>> createMultiChat(
      @RequestAttribute("X-User-Id") Long userId,
      @RequestBody CreateMultiChatRequest request) {

    List<String> participantUuids = request.participantUuids();
    if (CollectionUtils.isEmpty(participantUuids)) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), ErrorConstants.INVALID_REQUEST,
          "participantUuids is required");
    }

    MultiChatResponse response = conversationService.createMultiChatByUuids(userId, participantUuids);
    return created(response, "Multi-chat created");
  }

  @PutMapping("/conversations/{uuid}/pin")
  public ResponseEntity<BaseResponse<Void>> pinConversation(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("uuid") String uuid) {
    conversationListService.pinConversation(userId, uuid);
    return success("Conversation pinned");
  }

  @DeleteMapping("/conversations/{uuid}/pin")
  public ResponseEntity<BaseResponse<Void>> unpinConversation(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("uuid") String uuid) {
    conversationListService.unpinConversation(userId, uuid);
    return success("Conversation unpinned");
  }

  @PutMapping("/conversations/{uuid}/archive")
  public ResponseEntity<BaseResponse<Void>> archiveConversation(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("uuid") String uuid) {
    conversationListService.archiveConversation(userId, uuid);
    return success("Conversation archived");
  }

  @DeleteMapping("/conversations/{uuid}/archive")
  public ResponseEntity<BaseResponse<Void>> unarchiveConversation(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("uuid") String uuid) {
    conversationListService.unarchiveConversation(userId, uuid);
    return success("Conversation unarchived");
  }

  @PutMapping("/conversations/{uuid}/mute")
  public ResponseEntity<BaseResponse<Void>> muteConversation(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("uuid") String uuid) {
    conversationListService.muteConversation(userId, uuid);
    return success("Conversation muted");
  }

  @DeleteMapping("/conversations/{uuid}/mute")
  public ResponseEntity<BaseResponse<Void>> unmuteConversation(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("uuid") String uuid) {
    conversationListService.unmuteConversation(userId, uuid);
    return success("Conversation unmuted");
  }

  @PutMapping("/conversations/{uuid}/disappearing")
  public ResponseEntity<BaseResponse<DisappearingTtlResponse>> updateDisappearingTtl(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("uuid") String uuid,
      @Valid @RequestBody DisappearingTtlRequest request) {

    Integer ttlHours = request.toHours();

    Conversation conversation = conversationService.updateDisappearingTtl(uuid, userId, ttlHours);
    String currentTtl = conversation.getDisappearingTtl() == null ? "off" : request.ttl();

    return success(new DisappearingTtlResponse(conversation.getConversationUuid(), currentTtl), "Disappearing messages settings updated");
  }

  @PutMapping("/messages/{uuid}/reactions")
  public ResponseEntity<BaseResponse<Void>> addReaction(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("uuid") String uuid,
      @RequestBody ReactionRequest request) {

    MessageReactionResult result = messageService.addReaction(userId, uuid, request.emoji());

    broadcastService.broadcast("/topic/chat/" + result.conversationUuid() + "/reactions",
        Map.of("messageUuid", uuid, "emoji", request.emoji(), "userUuid", result.userUuid() != null ? result.userUuid() : "", "action", "ADD"));

    return success("Reaction added");
  }

  @DeleteMapping("/messages/{uuid}/reactions")
  public ResponseEntity<BaseResponse<Void>> removeReaction(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("uuid") String uuid) {

    MessageReactionResult result = messageService.removeReaction(userId, uuid);

    broadcastService.broadcast("/topic/chat/" + result.conversationUuid() + "/reactions",
        Map.of("messageUuid", uuid, "userUuid", result.userUuid() != null ? result.userUuid() : "", "action", "REMOVE"));

    return success("Reaction removed");
  }

  @PostMapping("/messages/forward")
  public ResponseEntity<BaseResponse<MessageResponse>> forwardMessage(
      @RequestAttribute("X-User-Id") Long userId,
      @RequestBody ForwardMessageRequest request) {

    Message forwarded = messageService.forwardMessage(userId, request.messageUuid(), request.targetConversationUuid());
    Conversation conv = conversationService.findConversationById(forwarded.getConversationId());

    MessageResponse response = messageResponseMapper.toResponseList(
        List.of(forwarded), conv.getConversationUuid(), userId, "id").getFirst();

    broadcastService.broadcast("/topic/chat/" + conv.getConversationUuid(), response);
    return created(response, "Message forwarded");
  }
}
