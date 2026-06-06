package id.xyz.chatapps_graph.infrastructure.notification;

import id.xyz.chatapps_graph.applications.usecase.NotificationChannel;
import id.xyz.chatapps_graph.domain.enums.NotificationChannelType;
import id.xyz.chatapps_graph.infrastructure.utility.MaskingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailNotificationChannel implements NotificationChannel {

  private final JavaMailSender mailSender;
  private final String fromAddress;

  public EmailNotificationChannel(JavaMailSender mailSender,
                                  @Value("${spring.mail.username:noreply@chatapps.xyz}") String fromAddress) {
    this.mailSender = mailSender;
    this.fromAddress = fromAddress;
  }

  @Override
  public void send(String email, String otp) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(fromAddress);
      message.setTo(email);
      message.setSubject("Your Verification Code");
      message.setText("Your OTP code is: " + otp + ". Valid for 3 minutes. Do not share this code.");
      mailSender.send(message);
      log.info("OTP email sent to [{}]", MaskingUtil.maskEmail(email));
    } catch (Exception e) {
      log.error("Failed to send OTP email to [{}]: {}", MaskingUtil.maskEmail(email), e.getMessage());
    }
  }

  @Override
  public NotificationChannelType getType() {
    return NotificationChannelType.EMAIL;
  }
}
