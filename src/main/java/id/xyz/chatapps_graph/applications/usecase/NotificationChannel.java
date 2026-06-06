package id.xyz.chatapps_graph.applications.usecase;

import id.xyz.chatapps_graph.domain.enums.NotificationChannelType;

public interface NotificationChannel {
  void send(String destination, String otp);
  NotificationChannelType getType();
}
