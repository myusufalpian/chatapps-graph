package id.xyz.chatapps_graph.applications.usecase.adapters;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import id.xyz.chatapps_graph.applications.usecase.PushNotificationService;
import id.xyz.chatapps_graph.domain.entity.Conversation;
import id.xyz.chatapps_graph.domain.entity.ConversationParticipant;
import id.xyz.chatapps_graph.domain.entity.Message;
import id.xyz.chatapps_graph.domain.entity.User;
import id.xyz.chatapps_graph.domain.entity.UserDevice;
import id.xyz.chatapps_graph.domain.repository.ConversationParticipantRepository;
import id.xyz.chatapps_graph.domain.repository.ConversationRepository;
import id.xyz.chatapps_graph.domain.repository.MessageRepository;
import id.xyz.chatapps_graph.domain.repository.UserDeviceRepository;
import id.xyz.chatapps_graph.domain.repository.UserRepository;
import id.xyz.chatapps_graph.framework.dto.FCMNotificationTask;
import id.xyz.chatapps_graph.infrastructure.config.rabbitmq.RabbitMQConfig;
import id.xyz.chatapps_graph.infrastructure.mapper.MessageMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {

  private static final int MAX_PREVIEW_LENGTH = 100;

  private final ConversationParticipantRepository participantRepository;
  private final UserDeviceRepository userDeviceRepository;
  private final UserRepository userRepository;
  private final ConversationRepository conversationRepository;
  private final MessageRepository messageRepository;
  private final RabbitTemplate rabbitTemplate;

  @Override
  public void sendPushForNewMessage(Message message, Long senderId, Long conversationId) {
    FCMNotificationTask task = FCMNotificationTask.builder()
        .messageId(message.getMessageId())
        .senderId(senderId)
        .conversationId(conversationId)
        .build();
    
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.CHAT_TASK_EXCHANGE,
        RabbitMQConfig.FCM_NOTIFICATIONS_ROUTING_KEY,
        task
    );
  }

  public void executeSendPush(Long messageId, Long senderId, Long conversationId) {
    if (FirebaseApp.getApps().isEmpty()) {
      return;
    }

    try {
      Message message = messageRepository.findById(messageId).orElse(null);
      if (message == null) {
        log.warn("FCM Notification failed: Message not found for ID {}", messageId);
        return;
      }

      List<ConversationParticipant> participants = participantRepository.findAllByConversationId(conversationId);

      List<Long> targetUserIds = participants.stream()
          .filter(p -> !p.getUserId().equals(senderId))
          .filter(p -> !Boolean.TRUE.equals(p.getIsMuted()))
          .map(ConversationParticipant::getUserId)
          .toList();

      if (targetUserIds.isEmpty()) {
        return;
      }

      List<UserDevice> devices = userDeviceRepository.findByUserIdInAndDeletedAtIsNull(targetUserIds);
      if (devices.isEmpty()) {
        return;
      }

      Map<String, UserDevice> tokenToDevice = devices.stream()
          .collect(Collectors.toMap(UserDevice::getDeviceToken, Function.identity()));
      List<String> tokens = devices.stream().map(UserDevice::getDeviceToken).toList();

      User sender = userRepository.findById(senderId).orElse(null);
      String senderName = sender != null ? sender.getUserFullName() : "Unknown";
      String preview = buildPreview(message.getContent(), message.getMessageType());

      Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
      String conversationUuid = conversation != null ? conversation.getConversationUuid() : "";

      MulticastMessage fcmMessage = MulticastMessage.builder()
          .setNotification(Notification.builder()
              .setTitle(senderName)
              .setBody(preview)
              .build())
          .putAllData(Map.of(
              "conversationUuid", conversationUuid,
              "messageUuid", message.getMessageUuid() != null ? message.getMessageUuid() : ""
          ))
          .addAllTokens(tokens)
          .build();

      BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(fcmMessage);
      handleUnregisteredTokens(response, tokens, tokenToDevice);

    } catch (Exception e) {
      log.error("Failed to send push notification", e);
    }
  }

  private void handleUnregisteredTokens(BatchResponse response, List<String> tokens, Map<String, UserDevice> tokenToDevice) {
    List<SendResponse> responses = response.getResponses();
    List<UserDevice> toSoftDelete = new java.util.ArrayList<>();
    for (int i = 0; i < responses.size(); i++) {
      SendResponse sendResponse = responses.get(i);
      if (!sendResponse.isSuccessful() && sendResponse.getException() != null
          && sendResponse.getException().getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
        String token = tokens.get(i);
        UserDevice device = tokenToDevice.get(token);
        if (device != null) {
          device.setDeletedAt(OffsetDateTime.now());
          toSoftDelete.add(device);
          log.info("Soft-deleted unregistered device token for userId: {}", device.getUserId());
        }
      }
    }
    if (!toSoftDelete.isEmpty()) {
      userDeviceRepository.saveAll(toSoftDelete);
    }
  }

  private String buildPreview(String content, String messageType) {
    return MessageMapper.buildPreview(content, messageType, MAX_PREVIEW_LENGTH);
  }
}

