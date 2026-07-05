package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.domain.entity.Message;

public interface PushNotificationService {

  void sendPushForNewMessage(Message message, Long senderId, Long conversationId);
}
