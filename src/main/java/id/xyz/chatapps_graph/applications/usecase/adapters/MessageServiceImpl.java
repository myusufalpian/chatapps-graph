package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.AttachmentService;
import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.applications.usecase.DeliveryReceiptResult;
import id.xyz.chatapps_graph.framework.dto.MessageReactionResult;
import id.xyz.chatapps_graph.applications.usecase.MessageEditResult;
import id.xyz.chatapps_graph.applications.usecase.MessageService;
import id.xyz.chatapps_graph.framework.dto.SendMessageResult;
import id.xyz.chatapps_graph.applications.usecase.PushNotificationService;
import id.xyz.chatapps_graph.applications.usecase.RateLimitService;
import id.xyz.chatapps_graph.applications.usecase.ReadReceiptResult;
import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.entity.MessageEditHistory;
import id.xyz.chatapps_graph.domain.entity.MessageReaction;
import id.xyz.chatapps_graph.domain.entity.MessageReceipt;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.DeleteMode;
import id.xyz.chatapps_graph.domain.enums.MessageStatus;
import id.xyz.chatapps_graph.domain.enums.ReceiptStatus;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.MessageEditHistoryRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReactionRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReceiptRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.config.properties.ChatEditProperties;
import id.xyz.chatapps_graph.infrastructure.constant.ErrorConstants;
import id.xyz.chatapps_graph.infrastructure.constant.GeneralConstants.StatusConstants;
import id.xyz.chatapps_graph.infrastructure.utility.CursorUtil;
import id.xyz.chatapps_graph.infrastructure.utility.CursorUtil.CursorPosition;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import id.xyz.chatapps_graph.framework.dto.LinkPreviewTask;
import id.xyz.chatapps_graph.infrastructure.config.rabbitmq.RabbitMQConfig;
import id.xyz.chatapps_graph.infrastructure.monitoring.MetricsFacade;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

  private final MessageRepository messageRepository;
  private final MessageReceiptRepository receiptRepository;
  private final ConversationParticipantRepository participantRepository;
  private final ConversationService conversationService;
  private final AttachmentService attachmentService;
  private final AttachmentRepository attachmentRepository;
  private final UserRepository userRepository;
  private final RateLimitService rateLimitService;
  private final MessageReactionRepository reactionRepository;
  private final PushNotificationService pushNotificationService;
  private final MessageEditHistoryRepository editHistoryRepository;
  private final ChatEditProperties chatEditProperties;
  private final MetricsFacade metricsFacade;
  private final RabbitTemplate rabbitTemplate;

  @Override
  @Transactional
  public SendMessageResult sendMessage(Long senderId, String recipientUuid, String conversationUuid,
      String messageType, String content, Long attachmentId, String replyToMessageUuid) {

    if (rateLimitService.isChatRateLimited(senderId)) {
      throw new GeneralException(429, ErrorConstants.RATE_LIMITED, "Too many messages, try again later");
    }

    Conversation conversation = resolveConversation(senderId, recipientUuid, conversationUuid);
    validateParticipant(conversation.getConversationId(), senderId);
    Long replyToId = resolveReplyTo(replyToMessageUuid);
    String filePath = resolveAttachmentFilePath(attachmentId);

    try {
      Message message = messageRepository.save(Message.builder()
          .conversationId(conversation.getConversationId())
          .senderId(senderId)
          .messageType(messageType)
          .content(content)
          .attachmentId(attachmentId)
          .replyToMessageId(replyToId)
          .messageStatus(MessageStatus.ACTIVE.getValue())
          .build());

      createReceipts(message.getMessageId(), conversation.getConversationId(), senderId);
      metricsFacade.incrementMessagesSent(conversation.getConversationType());


      // Update denormalized fields
      String preview = id.xyz.chatapps_graph.infrastructure.mapper.MessageMapper.buildPreview(content, messageType, 100);
      participantRepository.incrementUnreadAndUpdateLastMessage(
          conversation.getConversationId(), senderId, message.getCreatedAt(), preview, messageType);
      participantRepository.updateSenderLastMessage(
          conversation.getConversationId(), senderId, message.getCreatedAt(), preview, messageType);
      participantRepository.autoUnarchive(conversation.getConversationId(), senderId);

      registerPushAfterCommit(message, senderId, conversation.getConversationId());

      List<String> urls = extractUrls(content);
      if (!urls.isEmpty()) {
        String firstUrl = urls.getFirst();
        String convUuid = conversation.getConversationUuid();
        Long msgId = message.getMessageId();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
          TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              triggerAsyncLinkPreview(firstUrl, msgId, convUuid);
            }
          });
        } else {
          triggerAsyncLinkPreview(firstUrl, msgId, convUuid);
        }
      }

      User sender = userRepository.findById(senderId)
          .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.USER_NOT_FOUND, "User not found"));

      return SendMessageResult.builder()
          .message(message)
          .senderUuid(sender.getUserUuid())
          .conversationUuid(conversation.getConversationUuid())
          .build();
    } catch (Exception e) {
      if (filePath != null) {
        attachmentService.deleteFile(filePath);
      }
      throw e;
    }
  }


  @Override
  @Transactional(readOnly = true)
  public List<Message> listMessages(Long conversationId, Long userId, String cursor, int limit) {
    CursorPosition position = CursorUtil.parse(cursor);
    if (position != null) {
      return messageRepository.findMessagesAfterCursor(conversationId, userId, position.timestamp(), position.id(), limit);
    }
    return messageRepository.findFirstMessages(conversationId, userId, limit);
  }

  @Override
  @Transactional
  public void deleteMessage(String messageUuid, Long userId, String mode) {
    Message message = messageRepository.findByMessageUuid(messageUuid)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.MESSAGE_NOT_FOUND, "Message not found"));

    if (DeleteMode.FOR_ALL.name().equals(mode)) {
      if (!message.getSenderId().equals(userId)) {
        throw new GeneralException(HttpStatus.FORBIDDEN.value(), ErrorConstants.FORBIDDEN, "Only sender can delete for all");
      }
      message.setMessageStatus(MessageStatus.DELETED.getValue());
      messageRepository.saveAndFlush(message);
      // Update preview only if this was the last message in conversation
      Optional<Long> latestId = messageRepository.findLatestActiveMessageId(message.getConversationId());
      if (latestId.isEmpty() || latestId.get() < message.getMessageId()) {
        if (latestId.isPresent()) {
          messageRepository.findById(latestId.get()).ifPresent(latest -> {
            String preview = id.xyz.chatapps_graph.infrastructure.mapper.MessageMapper.buildPreview(latest.getContent(), latest.getMessageType(), 100);
            participantRepository.updateLastMessagePreviewForAll(message.getConversationId(), preview);
          });
        } else {
          participantRepository.updateLastMessagePreviewForAll(message.getConversationId(), id.xyz.chatapps_graph.infrastructure.constant.GeneralConstants.ResponseConstants.MESSAGE_DELETED);
        }
      }
    } else {
      MessageReceipt receipt = receiptRepository.findByMessageIdAndUserId(message.getMessageId(), userId)
          .orElseGet(() -> MessageReceipt.builder()
              .messageId(message.getMessageId())
              .userId(userId)
              .status(ReceiptStatus.SENT.getValue())
              .isDeletedForMe(false)
              .build());
      receipt.setIsDeletedForMe(true);
      receiptRepository.save(receipt);
    }
  }

  @Override
  @Transactional
  public ReadReceiptResult markAsRead(String conversationUuid, Long userId) {
    Conversation conversation = conversationService.findConversationByUuid(conversationUuid);

    User reader = userRepository.findById(userId).orElse(null);
    boolean shouldUpdateReceipts = reader != null && !Boolean.TRUE.equals(reader.getHideReadReceipt());
    List<Long> senderIds = List.of();

    if (shouldUpdateReceipts) {
      senderIds = receiptRepository.findUnreadMessageSenderIds(
          conversation.getConversationId(), userId, ReceiptStatus.READ.getValue());
      int updatedCount = receiptRepository.markAsReadByConversation(
          conversation.getConversationId(), userId, ReceiptStatus.READ.getValue());
      if (updatedCount == 0) {
        senderIds = List.of();
      }
    }

    // Always reset unread count (UX: badge disappears regardless of privacy setting)
    participantRepository.resetUnreadCount(conversation.getConversationId(), userId);

    List<String> targetUserPhones = List.of();
    if (shouldUpdateReceipts && !senderIds.isEmpty()) {
      targetUserPhones = userRepository.findAllById(senderIds).stream()
          .filter(sender -> !Boolean.TRUE.equals(sender.getHideReadReceipt()))
          .map(User::getUserPhone)
          .filter(StringUtils::hasLength)
          .toList();
    }

    return shouldUpdateReceipts && !targetUserPhones.isEmpty()
        ? new ReadReceiptResult(true, targetUserPhones, reader != null ? reader.getUserUuid() : null)
        : ReadReceiptResult.hidden();
  }

  @Override
  @Transactional
  public DeliveryReceiptResult markAsDelivered(String conversationUuid, Long userId, List<String> messageUuids) {
    if (!StringUtils.hasLength(conversationUuid) || messageUuids == null || messageUuids.isEmpty()) {
      return DeliveryReceiptResult.hidden();
    }

    Conversation conversation = conversationService.findConversationByUuid(conversationUuid);
    if (!conversationService.isParticipant(conversation.getConversationId(), userId)) {
      throw new GeneralException(HttpStatus.FORBIDDEN.value(), ErrorConstants.FORBIDDEN, "Not a participant");
    }

    List<Long> senderIds = receiptRepository.findUndeliveredMessageSenderIds(
        conversation.getConversationId(), userId, messageUuids, ReceiptStatus.SENT.getValue());
    int updatedCount = receiptRepository.markAsDeliveredByConversation(
        conversation.getConversationId(), userId, messageUuids, ReceiptStatus.SENT.getValue(),
        ReceiptStatus.DELIVERED.getValue());

    if (updatedCount == 0) {
      senderIds = List.of();
    }

    List<String> targetUserPhones = List.of();
    if (!senderIds.isEmpty()) {
      targetUserPhones = userRepository.findAllById(senderIds).stream()
          .filter(sender -> org.springframework.util.StringUtils.hasLength(sender.getUserPhone()))
          .map(User::getUserPhone)
          .toList();
    }

    User reader = userRepository.findById(userId).orElse(null);

    return !targetUserPhones.isEmpty()
        ? new DeliveryReceiptResult(true, targetUserPhones, reader != null ? reader.getUserUuid() : null)
        : DeliveryReceiptResult.hidden();
  }

  private Conversation resolveConversation(Long senderId, String recipientUuid, String conversationUuid) {
    if (StringUtils.hasLength(conversationUuid)) {
      return conversationService.findConversationByUuid(conversationUuid);
    }
    User recipient = userRepository.findUserByUserUuidAndUserStatus(recipientUuid, StatusConstants.ACTIVE)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.USER_NOT_FOUND, "Recipient not found"));
    return conversationService.findOrCreatePrivateConversation(senderId, recipient.getUserId());
  }

  private void validateParticipant(Long conversationId, Long userId) {
    if (!conversationService.isParticipant(conversationId, userId)) {
      throw new GeneralException(HttpStatus.FORBIDDEN.value(), ErrorConstants.FORBIDDEN, "Not a participant");
    }
  }

  private Long resolveReplyTo(String replyToMessageUuid) {
    if (!StringUtils.hasLength(replyToMessageUuid)) {
      return null;
    }
    return messageRepository.findByMessageUuid(replyToMessageUuid)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.MESSAGE_NOT_FOUND, "Reply message not found"))
        .getMessageId();
  }

  private String resolveAttachmentFilePath(Long attachmentId) {
    if (attachmentId == null) {
      return null;
    }
    return attachmentRepository.findById(attachmentId)
        .map(Attachment::getFilePath)
        .orElse(null);
  }

  private void createReceipts(Long messageId, Long conversationId, Long senderId) {
    List<MessageReceipt> receipts = participantRepository.findAllByConversationId(conversationId).stream()
        .filter(p -> !p.getUserId().equals(senderId))
        .map(p -> MessageReceipt.builder()
            .messageId(messageId)
            .userId(p.getUserId())
            .status(ReceiptStatus.SENT.getValue())
            .isDeletedForMe(false)
            .build())
        .toList();
    
    if (!receipts.isEmpty()) {
      receiptRepository.saveAll(receipts);
    }
  }

  @Override
  @Transactional
  public Message forwardMessage(Long userId, String messageUuid, String targetConversationUuid) {
    if (rateLimitService.isChatRateLimited(userId)) {
      throw new GeneralException(429, ErrorConstants.RATE_LIMITED, "Too many messages, try again later");
    }

    Message original = messageRepository.findByMessageUuid(messageUuid)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.MESSAGE_NOT_FOUND, "Message not found"));

    // Validate user is participant in source conversation
    validateParticipant(original.getConversationId(), userId);

    Conversation target = conversationService.findConversationByUuid(targetConversationUuid);
    validateParticipant(target.getConversationId(), userId);

    Message forwarded = messageRepository.save(Message.builder()
        .conversationId(target.getConversationId())
        .senderId(userId)
        .messageType(original.getMessageType())
        .content(original.getContent())
        .attachmentId(original.getAttachmentId())
        .forwardedFromId(original.getMessageId())
        .messageStatus(MessageStatus.ACTIVE.getValue())
        .build());

    createReceipts(forwarded.getMessageId(), target.getConversationId(), userId);

    String preview = id.xyz.chatapps_graph.infrastructure.mapper.MessageMapper.buildPreview(original.getContent(), original.getMessageType(), 100);
    participantRepository.incrementUnreadAndUpdateLastMessage(
        target.getConversationId(), userId, forwarded.getCreatedAt(), preview, original.getMessageType());
    participantRepository.updateSenderLastMessage(
        target.getConversationId(), userId, forwarded.getCreatedAt(), preview, original.getMessageType());
    participantRepository.autoUnarchive(target.getConversationId(), userId);

    registerPushAfterCommit(forwarded, userId, target.getConversationId());

    return forwarded;
  }

  @Override
  @Transactional
  public MessageReactionResult addReaction(Long userId, String messageUuid, String emoji) {
    if (rateLimitService.isReactionRateLimited(userId)) {
      throw new GeneralException(429, ErrorConstants.RATE_LIMITED, "Too many reactions, try again later");
    }
    Message message = messageRepository.findByMessageUuid(messageUuid)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.MESSAGE_NOT_FOUND, "Message not found"));
    validateParticipant(message.getConversationId(), userId);

    Long messageId = message.getMessageId();
    MessageReaction reaction = reactionRepository.findByMessageIdAndUserId(messageId, userId)
        .orElse(MessageReaction.builder().messageId(messageId).userId(userId).build());
    reaction.setEmoji(emoji);
    reactionRepository.save(reaction);

    Conversation conv = conversationService.findConversationById(message.getConversationId());
    User user = userRepository.findById(userId).orElse(null);
    String userUuid = user != null ? user.getUserUuid() : null;

    return MessageReactionResult.builder()
        .conversationUuid(conv.getConversationUuid())
        .userUuid(userUuid)
        .emoji(emoji)
        .build();
  }

  @Override
  @Transactional
  public MessageReactionResult removeReaction(Long userId, String messageUuid) {
    Message message = messageRepository.findByMessageUuid(messageUuid)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.MESSAGE_NOT_FOUND, "Message not found"));
    validateParticipant(message.getConversationId(), userId);

    reactionRepository.findByMessageIdAndUserId(message.getMessageId(), userId)
        .ifPresent(reactionRepository::delete);

    Conversation conv = conversationService.findConversationById(message.getConversationId());
    User user = userRepository.findById(userId).orElse(null);
    String userUuid = user != null ? user.getUserUuid() : null;

    return MessageReactionResult.builder()
        .conversationUuid(conv.getConversationUuid())
        .userUuid(userUuid)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Message> searchMessages(Long userId, String query, String conversationUuid, String cursor, int limit) {
    if (!StringUtils.hasLength(query)) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), ErrorConstants.INVALID_QUERY, "Search query is required");
    }
    int fetchLimit = Math.min(limit, 50) + 1;
    CursorPosition cursorPosition = CursorUtil.parse(cursor);

    if (StringUtils.hasLength(conversationUuid)) {
      Conversation conv = conversationService.findConversationByUuid(conversationUuid);
      if (cursorPosition != null) {
        return messageRepository.searchMessagesInConversationWithCursor(userId, query, conv.getConversationId(), cursorPosition.timestamp(), cursorPosition.id(), fetchLimit);
      }
      return messageRepository.searchMessagesInConversation(userId, query, conv.getConversationId(), fetchLimit);
    }

    if (cursorPosition != null) {
      return messageRepository.searchMessagesWithCursor(userId, query, cursorPosition.timestamp(), cursorPosition.id(), fetchLimit);
    }
    return messageRepository.searchMessages(userId, query, fetchLimit);
  }

  private void registerPushAfterCommit(Message message, Long senderId, Long conversationId) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          pushNotificationService.sendPushForNewMessage(message, senderId, conversationId);
        }
      });
    } else {
      pushNotificationService.sendPushForNewMessage(message, senderId, conversationId);
    }
  }


  @Override
  @Transactional
  public MessageEditResult editMessage(Long userId, String messageUuid, String newContent) {
    Message message = messageRepository.findByMessageUuid(messageUuid)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.MESSAGE_NOT_FOUND, "Message not found"));

    if (!message.getSenderId().equals(userId)) {
      throw new GeneralException(HttpStatus.FORBIDDEN.value(), ErrorConstants.NOT_SENDER, "Only sender can edit message");
    }

    if (message.getMessageStatus().equals(MessageStatus.DELETED.getValue())) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), ErrorConstants.MESSAGE_DELETED, "Cannot edit deleted message");
    }

    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime editDeadline = message.getCreatedAt().plusMinutes(chatEditProperties.getMaxWindowMinutes());
    if (now.isAfter(editDeadline)) {
      throw new GeneralException(HttpStatus.BAD_REQUEST.value(), ErrorConstants.EDIT_WINDOW_EXPIRED, "Edit window has expired");
    }

    Conversation conv = conversationService.findConversationById(message.getConversationId());
    User sender = userRepository.findById(userId)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), ErrorConstants.USER_NOT_FOUND, "User not found"));

    if (Objects.equals(message.getContent(), newContent)) {
      return new MessageEditResult(message, false, conv.getConversationUuid(), sender.getUserUuid());
    }

    editHistoryRepository.save(MessageEditHistory.builder()
        .messageId(message.getMessageId())
        .originalContent(message.getContent())
        .editedAt(now)
        .build());

    message.setContent(newContent);
    message.setEditedAt(now);
    return new MessageEditResult(messageRepository.save(message), true, conv.getConversationUuid(), sender.getUserUuid());
  }

  private static final java.util.regex.Pattern URL_PATTERN = java.util.regex.Pattern.compile(
      "https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
  );

  private java.util.List<String> extractUrls(String text) {
    if (text == null || text.trim().isEmpty()) {
      return java.util.List.of();
    }
    java.util.List<String> urls = new java.util.ArrayList<>();
    java.util.regex.Matcher matcher = URL_PATTERN.matcher(text);
    while (matcher.find()) {
      urls.add(matcher.group());
    }
    return urls;
  }

  private void triggerAsyncLinkPreview(String url, Long messageId, String conversationUuid) {
    LinkPreviewTask task = LinkPreviewTask.builder()
        .messageId(messageId)
        .url(url)
        .conversationUuid(conversationUuid)
        .build();

    rabbitTemplate.convertAndSend(
        RabbitMQConfig.CHAT_TASK_EXCHANGE,
        RabbitMQConfig.LINK_PREVIEWS_ROUTING_KEY,
        task
    );
  }

  @Override
  @Transactional(readOnly = true)
  public String resolveUserUuid(Long userId) {
    return userRepository.findById(userId).map(User::getUserUuid).orElse(null);
  }
}

