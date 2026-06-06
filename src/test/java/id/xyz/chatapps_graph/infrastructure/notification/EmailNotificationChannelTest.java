package id.xyz.chatapps_graph.infrastructure.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import id.xyz.chatapps_graph.domain.enums.NotificationChannelType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailNotificationChannelTest {

  @Mock private JavaMailSender mailSender;

  private static final String EMAIL = "user@test.com";
  private static final String OTP = "123456";
  private static final String FROM = "noreply@chatapps.xyz";

  private EmailNotificationChannel createChannel() {
    return new EmailNotificationChannel(mailSender, FROM);
  }

  @Test
  @DisplayName("send: success → verifies mailSender.send() called with correct message")
  void send_Success_MailSent() {
    EmailNotificationChannel channel = createChannel();

    channel.send(EMAIL, OTP);

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());

    SimpleMailMessage msg = captor.getValue();
    assertEquals(FROM, msg.getFrom());
    Assertions.assertNotNull(msg.getTo());
    assertEquals(EMAIL, msg.getTo()[0]);
    assertEquals("Your Verification Code", msg.getSubject());
    assertEquals("Your OTP code is: 123456. Valid for 3 minutes. Do not share this code.", msg.getText());
  }

  @Test
  @DisplayName("send: mail exception → does not throw")
  void send_MailException_DoesNotThrow() {
    doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));
    EmailNotificationChannel channel = createChannel();

    assertDoesNotThrow(() -> channel.send(EMAIL, OTP));
  }

  @Test
  @DisplayName("getType: returns EMAIL")
  void getType_ReturnsEmail() {
    assertEquals(NotificationChannelType.EMAIL, createChannel().getType());
  }
}
