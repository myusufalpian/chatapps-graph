package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.AttachmentService;
import id.xyz.chatapps_graph.applications.usecase.ConversationService;
import id.xyz.chatapps_graph.applications.usecase.MessageService;
import id.xyz.chatapps_graph.applications.usecase.RateLimitService;
import id.xyz.chatapps_graph.domain.entity.Attachment;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.entity.MessageReaction;
import id.xyz.chatapps_graph.domain.entity.MessageReceipt;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.enums.DeleteMode;
import id.xyz.chatapps_graph.domain.enums.MessageStatus;
import id.xyz.chatapps_graph.domain.enums.ReceiptStatus;
import id.xyz.chatapps_graph.domain.repository.AttachmentRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReactionRepository;
import id.xyz.chatapps_graph.domain.repository.MessageReceiptRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.infrastructure.config.exception.GeneralException;
import id.xyz.chatapps_graph.infrastructure.constant.GeneralConstants.StatusConstants;
import id.xyz.chatapps_graph.infrastructure.utility.CursorUtil;
import id.xyz.chatapps_graph.infrastructure.utility.CursorUtil.CursorPosition;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  @Override
  @Transactional
  public Message sendMessage(Long senderId, String recipientUuid, String conversationUuid,
      String messageType, String content, Long attachmentId, String replyToMessageUuid) {

    if (rateLimitService.isChatRateLimited(senderId)) {
      throw new GeneralException(429, "RATE_LIMITED", "Too many messages, try again later");
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

      // Update denormalized fields
      String preview = truncatePreview(content, messageType);
      participantRepository.incrementUnreadAndUpdateLastMessage(
          conversation.getConversationId(), senderId, message.getCreatedAt(), preview, messageType);
      participantRepository.updateSenderLastMessage(
          conversation.getConversationId(), senderId, message.getCreatedAt(), preview, messageType);
      participantRepository.autoUnarchive(conversation.getConversationId(), senderId);

      return message;
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
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), "MESSAGE_NOT_FOUND", "Message not found"));

    if (DeleteMode.FOR_ALL.name().equals(mode)) {
      if (!message.getSenderId().equals(userId)) {
        throw new GeneralException(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "Only sender can delete for all");
      }
      message.setMessageStatus(MessageStatus.DELETED.getValue());
      messageRepository.saveAndFlush(message);
      // Update preview only if this was the last message in conversation
      Optional<Long> latestId = messageRepository.findLatestActiveMessageId(message.getConversationId());
      if (latestId.isEmpty() || latestId.get() < message.getMessageId()) {
        if (latestId.isPresent()) {
          messageRepository.findById(latestId.get()).ifPresent(latest -> {
            String preview = truncatePreview(latest.getContent(), latest.getMessageType());
            participantRepository.updateLastMessagePreviewForAll(message.getConversationId(), preview);
          });
        } else {
          participantRepository.updateLastMessagePreviewForAll(message.getConversationId(), "Pesan dihapus");
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
  public void markAsRead(String conversationUuid, Long userId) {
    Conversation conversation = conversationService.findConversationByUuid(conversationUuid);
    receiptRepository.markAsReadByConversation(conversation.getConversationId(), userId, ReceiptStatus.READ.getValue());
    participantRepository.resetUnreadCount(conversation.getConversationId(), userId);
  }

  private Conversation resolveConversation(Long senderId, String recipientUuid, String conversationUuid) {
    if (conversationUuid != null && !conversationUuid.isBlank()) {
      return conversationService.findConversationByUuid(conversationUuid);
    }
    User recipient = userRepository.findUserByUserUuidAndUserStatus(recipientUuid, StatusConstants.ACTIVE)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), "USER_NOT_FOUND", "Recipient not found"));
    return conversationService.findOrCreatePrivateConversation(senderId, recipient.getUserId());
  }

  private void validateParticipant(Long conversationId, Long userId) {
    if (!conversationService.isParticipant(conversationId, userId)) {
      throw new GeneralException(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "Not a participant");
    }
  }

  private Long resolveReplyTo(String replyToMessageUuid) {
    if (replyToMessageUuid == null || replyToMessageUuid.isBlank()) {
      return null;
    }
    return messageRepository.findByMessageUuid(replyToMessageUuid)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), "MESSAGE_NOT_FOUND", "Reply message not found"))
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
    participantRepository.findAllByConversationId(conversationId).stream()
        .filter(p -> !p.getUserId().equals(senderId))
        .forEach(p -> receiptRepository.save(MessageReceipt.builder()
            .messageId(messageId)
            .userId(p.getUserId())
            .status(ReceiptStatus.SENT.getValue())
            .isDeletedForMe(false)
            .build()));
  }

  @Override
  @Transactional
  public Message forwardMessage(Long userId, String messageUuid, String targetConversationUuid) {
    if (rateLimitService.isChatRateLimited(userId)) {
      throw new GeneralException(429, "RATE_LIMITED", "Too many messages, try again later");
    }

    Message original = messageRepository.findByMessageUuid(messageUuid)
        .orElseThrow(() -> new GeneralException(HttpStatus.NOT_FOUND.value(), "MESSAGE_NOT_FOUND", "Message not found"));

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

    String preview = truncatePreview(original.getContent(), original.getMessageType());
    participantRepository.incrementUnreadAndUpdateLastMessage(
        target.getConversationId(), userId, forwarded.getCreatedAt(), preview, original.getMessageType());
    participantRepository.updateSenderLastMessage(
        target.getConversationId(), userId, forwarded.getCreatedAt(), preview, original.getMessageType());
    participantRepository.autoUnarchive(target.getConversationId(), userId);

    return forwarded;
  }

  @Override
  @Transactional
  public void addReaction(Long userId, Long messageId, String emoji) {
    if (rateLimitService.isReactionRateLimited(userId)) {
      throw new GeneralException(429, "RATE_LIMITED", "Too many reactions, try again later");
    }
    MessageReaction reaction = reactionRepository.findByMessageIdAndUserId(messageId, userId)
        .orElse(MessageReaction.builder().messageId(messageId).userId(userId).build());
    reaction.setEmoji(emoji);
    reactionRepository.save(reaction);
  }

  @Override
  @Transactional
  public void removeReaction(Long userId, Long messageId) {
    reactionRepository.findByMessageIdAndUserId(messageId, userId)
        .ifPresent(reactionRepository::delete);
  }

  private String truncatePreview(String content, String messageType) {
    if (content == null || content.isBlank()) {
      return messageType;
    }
    return content.length() <= 100 ? content : content.substring(0, 100);
  }
}
