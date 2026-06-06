package id.xyz.chatapps_graph.applications.usecase.adapters;

import id.xyz.chatapps_graph.applications.usecase.NotificationChannel;
import id.xyz.chatapps_graph.applications.usecase.NotificationService;
import id.xyz.chatapps_graph.domain.enums.NotificationChannelType;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final List<NotificationChannel> channels;

  @Async
  @Override
  public void sendOtp(String phone, String email, String otp) {
    channels.stream()
        .map(channel -> CompletableFuture.runAsync(() -> dispatch(channel, phone, email, otp)))
        .forEach(CompletableFuture::join);
  }

  @Async
  @Override
  public void sendOtp(String phone, String otp) {
    channels.stream()
        .filter(ch -> ch.getType() == NotificationChannelType.WHATSAPP)
        .findFirst()
        .ifPresent(ch -> {
          try {
            ch.send(phone, otp);
          } catch (Exception e) {
            log.error("Failed to send OTP via WhatsApp: {}", e.getMessage());
          }
        });
  }

  private void dispatch(NotificationChannel channel, String phone, String email, String otp) {
    try {
      if (channel.getType() == NotificationChannelType.WHATSAPP) {
        channel.send(phone, otp);
      } else if (channel.getType() == NotificationChannelType.EMAIL && email != null) {
        channel.send(email, otp);
      }
    } catch (Exception e) {
      log.error("Failed to send OTP via {}: {}", channel.getType(), e.getMessage());
    }
  }
}
