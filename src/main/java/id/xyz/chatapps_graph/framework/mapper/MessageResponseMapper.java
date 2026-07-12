package id.xyz.chatapps_graph.framework.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.entity.MessageReaction;
import id.xyz.chatapps_graph.domain.entity.MessageReceipt;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.MessageType;
import id.xyz.chatapps_graph.domain.enums.ReceiptStatus;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.ForwardedInfo;
import id.xyz.chatapps_graph.framework.dto.MessageResponse;
import id.xyz.chatapps_graph.framework.dto.ReactionSummary;
import id.xyz.chatapps_graph.framework.dto.ReplyToResponse;
import id.xyz.chatapps_graph.infrastructure.config.properties.MinioProperties;
import id.xyz.chatapps_graph.infrastructure.mapper.AttachmentMapper;
import id.xyz.chatapps_graph.infrastructure.mapper.MessageMapper;
import id.xyz.chatapps_graph.infrastructure.service.TranslationService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper component responsible for converting a {@link Message} domain entity into a
 * {@link MessageResponse} DTO, including attachment, reply, reaction, forwarded-info, and
 * delivery-status resolution.
 */
@Component
@RequiredArgsConstructor
public class MessageResponseMapper {

  private static final TypeReference<Map<String, String>> MAP_TYPE_REF = new TypeReference<>() {};

  private final MinioProperties minioProperties;
  private final TranslationService translationService;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  /**
   * Maps a single {@link Message} to a {@link MessageResponse}.
   *
   * @param m                  the message to map
   * @param conversationUuid   UUID of the conversation the message belongs to
   * @param userMap            map of userId → User for participants in the message list
   * @param userByUuidMap      map of userUuid → User for system-message placeholder resolution
   * @param attachmentMap      map of attachmentId → Attachment
   * @param replyMap           map of messageId → Message for reply-to targets
   * @param reactionsByMessage map of messageId → list of reactions
   * @param forwardMap         map of messageId → original forwarded Message
   * @param receiptsByMessage  map of messageId → list of receipts
   * @param requestUserId      ID of the requesting user (for delivery status calculation)
   * @param locale             locale string for system-message translation
   * @param hideReadReceipt    whether to hide read-receipt status for the requester
   * @return the assembled {@link MessageResponse}
   */
  public MessageResponse toResponse(
      Message m,
      String conversationUuid,
      Map<Long, User> userMap,
      Map<String, User> userByUuidMap,
      Map<Long, Attachment> attachmentMap,
      Map<Long, Message> replyMap,
      Map<Long, List<MessageReaction>> reactionsByMessage,
      Map<Long, Message> forwardMap,
      Map<Long, List<MessageReceipt>> receiptsByMessage,
      Long requestUserId,
      String locale,
      boolean hideReadReceipt) {

    User sender = userMap.get(m.getSenderId());
    String senderUuid = sender != null ? sender.getUserUuid() : null;

    var attResp = m.getAttachmentId() != null
        ? AttachmentMapper.toResponse(
            attachmentMap.get(m.getAttachmentId()),
            minioProperties.getEndpoint() + "/" + minioProperties.getBucket())
        : null;

    var replyResp = m.getReplyToMessageId() != null
        ? buildReplyResponse(replyMap.get(m.getReplyToMessageId()), userMap)
        : null;

    List<ReactionSummary> reactions =
        buildReactionSummary(reactionsByMessage.get(m.getMessageId()), requestUserId);

    ForwardedInfo forwardedInfo = buildForwardedInfo(m, forwardMap, userMap);

    String displayText = null;
    if (MessageType.SYSTEM.name().equals(m.getMessageType()) && m.getContent() != null) {
      displayText = resolveSystemMessageDisplayText(m.getContent(), userByUuidMap, locale);
    }

    Integer deliveryStatus = resolveDeliveryStatus(m, requestUserId, receiptsByMessage);

    return MessageMapper.toResponse(
        m, senderUuid, conversationUuid, attResp, replyResp,
        forwardedInfo, reactions, displayText, deliveryStatus);
  }

  private ReplyToResponse buildReplyResponse(Message replyMsg, Map<Long, User> userMap) {
    if (replyMsg == null) {
      return null;
    }
    User replySender = userMap.get(replyMsg.getSenderId());
    return MessageMapper.toReplyResponse(replyMsg, replySender != null ? replySender.getUserUuid() : null);
  }

  private ForwardedInfo buildForwardedInfo(
      Message m, Map<Long, Message> forwardMap, Map<Long, User> userMap) {
    if (m.getForwardedFromId() == null) {
      return null;
    }
    Message original = forwardMap.get(m.getForwardedFromId());
    if (original == null) {
      return null;
    }
    User origSender = userMap.getOrDefault(
        original.getSenderId(),
        userRepository.findById(original.getSenderId()).orElse(null));
    return ForwardedInfo.builder()
        .originalMessageUuid(original.getMessageUuid())
        .originalSenderUuid(origSender != null ? origSender.getUserUuid() : null)
        .build();
  }

  private List<ReactionSummary> buildReactionSummary(
      List<MessageReaction> reactions, Long requestUserId) {
    if (reactions == null || reactions.isEmpty()) {
      return null;
    }
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

  private Integer resolveDeliveryStatus(
      Message message, Long requestUserId,
      Map<Long, List<MessageReceipt>> receiptsByMessage) {
    if (!message.getSenderId().equals(requestUserId)) {
      return null;
    }
    List<MessageReceipt> receipts = receiptsByMessage.get(message.getMessageId());
    if (receipts == null || receipts.isEmpty()) {
      return ReceiptStatus.SENT.getValue();
    }
    return receipts.stream()
        .map(MessageReceipt::getStatus)
        .filter(Objects::nonNull)
        .min(Integer::compareTo)
        .orElse(ReceiptStatus.SENT.getValue());
  }

  private String resolveSystemMessageDisplayText(
      String content, Map<String, User> userByUuidMap, String locale) {
    try {
      Map<String, String> payload = objectMapper.readValue(content, MAP_TYPE_REF);
      String event = payload.get("event");
      if (event == null) {
        return content;
      }

      String actorUuid = payload.get("actorUuid");
      String targetUuid = payload.get("targetUuid");

      Map<String, String> params = HashMap.newHashMap(2);

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
}
