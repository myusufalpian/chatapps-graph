package id.xyz.chatapps_graph.framework.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.xyz.chatapps_graph.applications.usecase.AttachmentService;
import id.xyz.chatapps_graph.applications.usecase.ConversationListService;
import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.applications.usecase.MessageService;
import id.xyz.chatapps_graph.applications.usecase.RateLimitService;
import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.entity.MessageReaction;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.MessageType;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReactionRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.BaseResponse;
import id.xyz.chatapps_graph.framework.dto.ConversationListResponse;
import id.xyz.chatapps_graph.framework.dto.CreateMultiChatRequest;
import id.xyz.chatapps_graph.framework.dto.CursorPageResponse;
import id.xyz.chatapps_graph.framework.dto.EditMessageRequest;
import id.xyz.chatapps_graph.framework.dto.ForwardMessageRequest;
import id.xyz.chatapps_graph.framework.dto.ForwardedInfo;
import id.xyz.chatapps_graph.framework.dto.MarkReadRequest;
import id.xyz.chatapps_graph.framework.dto.MessageResponse;
import id.xyz.chatapps_graph.framework.dto.ParticipantSummary;
import id.xyz.chatapps_graph.framework.dto.MessageSearchResponse;
import id.xyz.chatapps_graph.framework.dto.ReactionRequest;
import id.xyz.chatapps_graph.framework.dto.ReactionSummary;
import id.xyz.chatapps_graph.framework.dto.ReplyToResponse;
import id.xyz.chatapps_graph.framework.dto.SearchResultItem;
import id.xyz.chatapps_graph.framework.dto.SendMessageRequest;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import id.xyz.chatapps_graph.infrastructure.config.properties.MinioProperties;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants;
import id.xyz.chatapps_graph.infrastructure.constant.GeneralConstants.StatusConstants;
import id.xyz.chatapps_graph.infrastructure.mapper.AttachmentMapper;
import id.xyz.chatapps_graph.infrastructure.mapper.ConversationMapper;
import id.xyz.chatapps_graph.infrastructure.mapper.MessageMapper;
import id.xyz.chatapps_graph.infrastructure.service.TranslationService;
import id.xyz.chatapps_graph.infrastructure.utility.CursorUtil;
import id.xyz.chatapps_graph.infrastructure.utility.JsonUtil;
import id.xyz.chatapps_graph.infrastructure.utility.LocaleResolver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

  private static final TypeReference<Map<String, String>> MAP_TYPE_REF =
      new TypeReference<>() {};

  private final MessageService messageService;
  private final AttachmentService attachmentService;
  private final ConversationService conversationService;
  private final ConversationListService conversationListService;
  private final RateLimitService rateLimitService;
  private final ConversationRepository conversationRepository;
  private final MessageReactionRepository reactionRepository;
  private final UserRepository userRepository;
  private final AttachmentRepository attachmentRepository;
  private final MessageRepository messageRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper;
  private final MinioProperties minioProperties;
  private final TranslationService translationService;
  private final LocaleResolver localeResolver;

  @PostMapping("/messages")
  public ResponseEntity<BaseResponse<MessageResponse>> sendMessage(
      @RequestAttribute("X-User-Id") Long userId,
      @RequestPart(value = "file", required = false) MultipartFile file,
      @RequestPart("metadata") String metadataJson) {

    SendMessageRequest request = parseMetadata(metadataJson);

    User sender = userRepository.findById(userId)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.USER_NOT_FOUND, "User not found"));

    Long attachmentId = null;
    if (file != null && !file.isEmpty()) {
      String attType = request.attachmentType() != null ? request.attachmentType() : "FILE";
      Attachment attachment = attachmentService.validateAndUpload(file, attType, userId, sender.getUserUuid());
      attachmentId = attachment.getAttachmentId();
    }

    Message message = messageService.sendMessage(userId, request.recipientUuid(),
        request.conversationUuid(), request.messageType(), request.content(),
        attachmentId, request.replyToMessageUuid());

    Conversation conv = resolveConversation(request.conversationUuid(), message.getConversationId());
    MessageResponse response = MessageMapper.toResponse(message, sender.getUserUuid(), conv.getConversationUuid(), null, null, null, null, null);

    messagingTemplate.convertAndSend("/topic/chat/" + conv.getConversationUuid(), response);
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
    if (!conversationService.isParticipant(conversation.getConversationId(), userId)) {
      throw new GeneralException(HttpStatus.FORBIDDEN.value(), ErrorConstants.FORBIDDEN, "Not a participant");
    }

    String locale = localeResolver.resolve(httpRequest);

    int fetchLimit = Math.min(limit, 50);
    List<Message> messages = messageService.listMessages(conversation.getConversationId(), userId, cursor, fetchLimit + 1);

    boolean hasMore = messages.size() > fetchLimit;
    List<Message> resultMessages = hasMore ? messages.subList(0, fetchLimit) : messages;

    String nextCursor = null;
    if (hasMore) {
      Message last = resultMessages.get(resultMessages.size() - 1);
      nextCursor = CursorUtil.encode(last.getCreatedAt(), last.getMessageId());
    }

    List<MessageResponse> responses = buildMessageResponses(resultMessages, uuid, userId, locale);
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

    Message edited = messageService.editMessage(userId, uuid, request.content());

    Conversation conv = conversationRepository.findById(edited.getConversationId())
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.CONVERSATION_NOT_FOUND, "Conversation not found"));

    User sender = userRepository.findById(userId).orElse(null);
    MessageResponse response = MessageMapper.toResponse(edited,
        sender != null ? sender.getUserUuid() : null, conv.getConversationUuid(),
        null, null, null, null, null);

    return success(response, "Message edited");
  }

  @PutMapping("/messages/read")
  public ResponseEntity<BaseResponse<Void>> markAsRead(
      @RequestAttribute("X-User-Id") Long userId,
      @RequestBody MarkReadRequest request) {

    Conversation conversation = conversationService.findConversationByUuid(request.conversationUuid());
    if (!conversationService.isParticipant(conversation.getConversationId(), userId)) {
      throw new GeneralException(HttpStatus.FORBIDDEN.value(), ErrorConstants.FORBIDDEN, "Not a participant");
    }

    boolean broadcastRead = messageService.markAsRead(request.conversationUuid(), userId);

    if (broadcastRead) {
      messagingTemplate.convertAndSend("/topic/chat/" + request.conversationUuid() + "/receipts",
          Map.of("userId", userId, "status", "READ", "conversationUuid", request.conversationUuid()));
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
    List<Message> messages = messageService.searchMessages(userId, query, conversationUuid, cursor, fetchLimit);

    boolean hasMore = messages.size() > fetchLimit;
    List<Message> resultMessages = hasMore ? messages.subList(0, fetchLimit) : messages;

    // Batch fetch users and conversations
    Map<Long, User> userMap = userRepository.findAllById(
        resultMessages.stream().map(Message::getSenderId).distinct().toList()
    ).stream().collect(Collectors.toMap(User::getUserId, Function.identity()));

    Map<Long, Conversation> convMap = conversationRepository.findAllById(
        resultMessages.stream().map(Message::getConversationId).distinct().toList()
    ).stream().collect(Collectors.toMap(Conversation::getConversationId, Function.identity()));

    List<SearchResultItem> results = resultMessages.stream()
        .map(m -> SearchResultItem.builder()
            .messageUuid(m.getMessageUuid())
            .conversationUuid(convMap.containsKey(m.getConversationId()) ? convMap.get(m.getConversationId()).getConversationUuid() : null)
            .senderUuid(userMap.containsKey(m.getSenderId()) ? userMap.get(m.getSenderId()).getUserUuid() : null)
            .content(m.getContent())
            .messageType(m.getMessageType())
            .createdAt(m.getCreatedAt())
            .build())
        .toList();

    String nextCursor = null;
    if (hasMore) {
      Message last = resultMessages.getLast();
      nextCursor = CursorUtil.encode(last.getCreatedAt(), last.getMessageId());
    }

    return success(MessageSearchResponse.builder()
        .results(results).nextCursor(nextCursor).hasMore(hasMore).build(), "Search results");
  }

  @PostMapping("/conversations/multi-chat")
  public ResponseEntity<BaseResponse<Map<String, Object>>> createMultiChat(
      @RequestAttribute("X-User-Id") Long userId,
      @RequestBody CreateMultiChatRequest request) {

    List<String> participantUuids = request.participantUuids();
    if (CollectionUtils.isEmpty(participantUuids)) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), ErrorConstants.INVALID_REQUEST,
          "participantUuids is required");
    }

    // Resolve user IDs from UUIDs
    List<User> users = participantUuids.stream()
        .map(uuid -> userRepository.findUserByUserUuidAndUserStatus(uuid, StatusConstants.ACTIVE)
            .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.USER_NOT_FOUND, "User not found: " + uuid)))
        .toList();

    List<Long> allParticipantIds = new ArrayList<>(users.stream().map(User::getUserId).toList());
    if (!allParticipantIds.contains(userId)) {
      allParticipantIds.add(userId);
    }

    Conversation conversation = conversationService.createMultiChat(userId, allParticipantIds);

    // Build participant summary
    List<User> allUsers = userRepository.findAllById(allParticipantIds);
    List<ParticipantSummary> participants = allUsers.stream()
        .map(ConversationMapper::toParticipantSummary)
        .toList();

    return created(Map.of(
        "conversationUuid", conversation.getConversationUuid(),
        "participants", participants
    ), "Multi-chat created");
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

  @PutMapping("/messages/{uuid}/reactions")
  public ResponseEntity<BaseResponse<Void>> addReaction(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("uuid") String uuid,
      @RequestBody ReactionRequest request) {

    Message message = messageRepository.findByMessageUuid(uuid)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.MESSAGE_NOT_FOUND, "Message not found"));

    if (!conversationService.isParticipant(message.getConversationId(), userId)) {
      throw new GeneralException(HttpStatus.FORBIDDEN.value(), ErrorConstants.FORBIDDEN, "Not a participant");
    }

    messageService.addReaction(userId, message.getMessageId(), request.emoji());

    Conversation conv = conversationRepository.findById(message.getConversationId()).orElse(null);
    if (conv != null) {
      messagingTemplate.convertAndSend("/topic/chat/" + conv.getConversationUuid() + "/reactions",
          Map.of("messageUuid", uuid, "emoji", request.emoji(), "userUuid", resolveUserUuid(userId), "action", "ADD"));
    }

    return success("Reaction added");
  }

  @DeleteMapping("/messages/{uuid}/reactions")
  public ResponseEntity<BaseResponse<Void>> removeReaction(
      @RequestAttribute("X-User-Id") Long userId,
      @PathVariable("uuid") String uuid) {

    Message message = messageRepository.findByMessageUuid(uuid)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.MESSAGE_NOT_FOUND, "Message not found"));

    if (!conversationService.isParticipant(message.getConversationId(), userId)) {
      throw new GeneralException(HttpStatus.FORBIDDEN.value(), ErrorConstants.FORBIDDEN, "Not a participant");
    }

    messageService.removeReaction(userId, message.getMessageId());

    Conversation conv = conversationRepository.findById(message.getConversationId()).orElse(null);
    if (conv != null) {
      messagingTemplate.convertAndSend("/topic/chat/" + conv.getConversationUuid() + "/reactions",
          Map.of("messageUuid", uuid, "userUuid", resolveUserUuid(userId), "action", "REMOVE"));
    }

    return success("Reaction removed");
  }

  @PostMapping("/messages/forward")
  public ResponseEntity<BaseResponse<MessageResponse>> forwardMessage(
      @RequestAttribute("X-User-Id") Long userId,
      @RequestBody ForwardMessageRequest request) {

    Message forwarded = messageService.forwardMessage(userId, request.messageUuid(), request.targetConversationUuid());

    Conversation conv = conversationRepository.findById(forwarded.getConversationId())
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.CONVERSATION_NOT_FOUND, "Conversation not found"));

    Message original = messageRepository.findById(forwarded.getForwardedFromId()).orElse(null);
    User originalSender = original != null ? userRepository.findById(original.getSenderId()).orElse(null) : null;

    ForwardedInfo forwardedInfo = ForwardedInfo.builder()
        .originalMessageUuid(original != null ? original.getMessageUuid() : null)
        .originalSenderUuid(originalSender != null ? originalSender.getUserUuid() : null)
        .build();

    User sender = userRepository.findById(userId).orElse(null);
    MessageResponse response = MessageMapper.toResponse(forwarded,
        sender != null ? sender.getUserUuid() : null, conv.getConversationUuid(),
        null, null, forwardedInfo, null, null);

    messagingTemplate.convertAndSend("/topic/chat/" + conv.getConversationUuid(), response);
    return created(response, "Message forwarded");
  }

  private SendMessageRequest parseMetadata(String metadataJson) {
    try {
      return JsonUtil.stringToModel(metadataJson, SendMessageRequest.class);
    } catch (Exception e) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), ErrorConstants.INVALID_METADATA, "Invalid metadata JSON");
    }
  }

  private Conversation resolveConversation(String conversationUuid, Long conversationId) {
    if (conversationUuid != null) {
      return conversationService.findConversationByUuid(conversationUuid);
    }
    return conversationRepository.findById(conversationId)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.CONVERSATION_NOT_FOUND, "Conversation not found"));
  }

  private List<MessageResponse> buildMessageResponses(List<Message> messages, String conversationUuid, Long requestUserId, String locale) {
    if (messages.isEmpty()) {
      return List.of();
    }

    Map<Long, User> userMap = userRepository.findAllById(
        messages.stream().map(Message::getSenderId).distinct().toList()
    ).stream().collect(Collectors.toMap(User::getUserId, Function.identity()));

    User requester = userRepository.findById(requestUserId).orElse(null);
    boolean hideReadReceipt = requester != null && Boolean.TRUE.equals(requester.getHideReadReceipt());

    Map<Long, Attachment> attachmentMap = loadAttachments(messages);
    Map<Long, Message> replyMap = loadReplies(messages);

    // Batch fetch reactions
    List<Long> messageIds = messages.stream().map(Message::getMessageId).toList();
    List<MessageReaction> allReactions = reactionRepository.findAllByMessageIdIn(messageIds);
    Map<Long, List<MessageReaction>> reactionsByMessage = allReactions.stream()
        .collect(Collectors.groupingBy(MessageReaction::getMessageId));

    // Batch fetch forwarded originals
    Map<Long, Message> forwardMap = loadForwardedOriginals(messages);

    // Build UUID-keyed user map for O(1) system message placeholder resolution
    Map<String, User> userByUuidMap = userMap.values().stream()
        .collect(Collectors.toMap(User::getUserUuid, Function.identity(), (a, b) -> a));

    return messages.stream()
        .map(m -> mapMessage(m, conversationUuid, userMap, userByUuidMap, attachmentMap, replyMap, reactionsByMessage, forwardMap, requestUserId, locale, hideReadReceipt))
        .toList();
  }

  private MessageResponse mapMessage(Message m, String conversationUuid,
      Map<Long, User> userMap, Map<String, User> userByUuidMap,
      Map<Long, Attachment> attachmentMap, Map<Long, Message> replyMap,
      Map<Long, List<MessageReaction>> reactionsByMessage, Map<Long, Message> forwardMap,
      Long requestUserId, String locale, boolean hideReadReceipt) {

    User sender = userMap.get(m.getSenderId());
    String senderUuid = sender != null ? sender.getUserUuid() : null;

    var attResp = m.getAttachmentId() != null
        ? AttachmentMapper.toResponse(attachmentMap.get(m.getAttachmentId()),
            minioProperties.getEndpoint() + "/" + minioProperties.getBucket())
        : null;

    var replyResp = m.getReplyToMessageId() != null
        ? buildReplyResponse(replyMap.get(m.getReplyToMessageId()), userMap)
        : null;

    // Build reactions summary
    List<ReactionSummary> reactions = buildReactionSummary(reactionsByMessage.get(m.getMessageId()), requestUserId);

    // Build forwarded info
    ForwardedInfo forwardedInfo = null;
    if (m.getForwardedFromId() != null) {
      Message original = forwardMap.get(m.getForwardedFromId());
      if (original != null) {
        User origSender = userMap.getOrDefault(original.getSenderId(),
            userRepository.findById(original.getSenderId()).orElse(null));
        forwardedInfo = ForwardedInfo.builder()
            .originalMessageUuid(original.getMessageUuid())
            .originalSenderUuid(origSender != null ? origSender.getUserUuid() : null)
            .build();
      }
    }

    // Build displayText for SYSTEM messages
    String displayText = null;
    if (MessageType.SYSTEM.name().equals(m.getMessageType()) && m.getContent() != null) {
      displayText = resolveSystemMessageDisplayText(m.getContent(), userByUuidMap, locale);
    }

    return MessageMapper.toResponse(m, senderUuid, conversationUuid, attResp, replyResp, forwardedInfo, reactions, displayText);
  }

  private String resolveSystemMessageDisplayText(String content, Map<String, User> userByUuidMap, String locale) {
    try {
      Map<String, String> payload = objectMapper.readValue(content, MAP_TYPE_REF);
      String event = payload.get("event");
      if (event == null) return content;

      String actorUuid = payload.get("actorUuid");
      String targetUuid = payload.get("targetUuid");

      Map<String, String> params = new HashMap<>(2);

      if (actorUuid != null) {
        User actor = userByUuidMap.get(actorUuid);
        params.put("actor", actor != null ? actor.getUserFullName() : actorUuid);
      }

      if (targetUuid != null) {
        User target = userByUuidMap.get(targetUuid);
        params.put("target", target != null ? target.getUserFullName() : targetUuid);
      }

      return translationService.translateSystemMessage(event, locale, params);
    } catch (Exception e) {
      return content;
    }
  }

  private ReplyToResponse buildReplyResponse(Message replyMsg, Map<Long, User> userMap) {
    if (replyMsg == null) return null;
    User replySender = userMap.get(replyMsg.getSenderId());
    return MessageMapper.toReplyResponse(replyMsg, replySender != null ? replySender.getUserUuid() : null);
  }

  private Map<Long, Attachment> loadAttachments(List<Message> messages) {
    List<Long> ids = messages.stream().map(Message::getAttachmentId).filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) return Map.of();
    return attachmentRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(Attachment::getAttachmentId, Function.identity()));
  }

  private Map<Long, Message> loadReplies(List<Message> messages) {
    List<Long> ids = messages.stream().map(Message::getReplyToMessageId).filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) return Map.of();
    return messageRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(Message::getMessageId, Function.identity()));
  }

  private Map<Long, Message> loadForwardedOriginals(List<Message> messages) {
    List<Long> ids = messages.stream().map(Message::getForwardedFromId).filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) return Map.of();
    return messageRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(Message::getMessageId, Function.identity()));
  }

  private List<ReactionSummary> buildReactionSummary(List<MessageReaction> reactions, Long requestUserId) {
    if (reactions == null || reactions.isEmpty()) return null;
    return reactions.stream()
        .collect(Collectors.groupingBy(MessageReaction::getEmoji))
        .entrySet().stream()
        .map(e -> ReactionSummary.builder()
            .emoji(e.getKey())
            .count(e.getValue().size())
            .reactedByMe(e.getValue().stream().anyMatch(r -> r.getUserId().equals(requestUserId)))
            .build())
        .toList();
  }

  private String resolveUserUuid(Long userId) {
    return userRepository.findById(userId).map(User::getUserUuid).orElse(null);
  }
}
