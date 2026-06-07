package id.xyz.chatapps_graph.applications.usecase.adapters;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import id.xyz.chatapps_graph.applications.usecase.NotificationChannel;
import id.xyz.chatapps_graph.domain.enums.NotificationChannelType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

  @Mock private NotificationChannel whatsAppChannel;
  @Mock private NotificationChannel emailChannel;

  private static final String PHONE = "+628123456789";
  private static final String EMAIL = "user@test.com";
  private static final String OTP = "123456";

  private NotificationServiceImpl createService() {
    when(whatsAppChannel.getType()).thenReturn(NotificationChannelType.WHATSAPP);
    when(emailChannel.getType()).thenReturn(NotificationChannelType.EMAIL);
    return new NotificationServiceImpl(List.of(whatsAppChannel, emailChannel));
  }

  private NotificationServiceImpl createServiceWhatsAppOnly() {
    when(whatsAppChannel.getType()).thenReturn(NotificationChannelType.WHATSAPP);
    return new NotificationServiceImpl(List.of(whatsAppChannel));
  }

  @Test
  @DisplayName("sendOtp(phone, email, otp): dispatches to WhatsApp with phone")
  void sendOtp_DispatchesToWhatsApp() {
    NotificationServiceImpl service = createService();

    service.sendOtp(PHONE, EMAIL, OTP);

    verify(whatsAppChannel).send(PHONE, OTP);
  }

  @Test
  @DisplayName("sendOtp(phone, email, otp): dispatches to Email with email")
  void sendOtp_DispatchesToEmail() {
    NotificationServiceImpl service = createService();

    service.sendOtp(PHONE, EMAIL, OTP);

    verify(emailChannel).send(EMAIL, OTP);
  }

  @Test
  @DisplayName("sendOtp(phone, null, otp): skips Email channel")
  void sendOtp_NullEmail_SkipsEmail() {
    NotificationServiceImpl service = createService();

    service.sendOtp(PHONE, null, OTP);

    verify(whatsAppChannel).send(PHONE, OTP);
    verifyNoMoreInteractions(emailChannel);
  }

  @Test
  @DisplayName("sendOtp(phone, email, otp): WhatsApp failure does NOT affect Email dispatch")
  void sendOtp_WhatsAppFails_EmailStillDispatched() {
    NotificationServiceImpl service = createService();
    doThrow(new RuntimeException("WA down")).when(whatsAppChannel).send(PHONE, OTP);

    service.sendOtp(PHONE, EMAIL, OTP);

    verify(emailChannel).send(EMAIL, OTP);
  }

  @Test
  @DisplayName("sendOtp(phone, email, otp): Email failure does NOT affect WhatsApp dispatch")
  void sendOtp_EmailFails_WhatsAppStillDispatched() {
    NotificationServiceImpl service = createService();
    doThrow(new RuntimeException("SMTP down")).when(emailChannel).send(EMAIL, OTP);

    service.sendOtp(PHONE, EMAIL, OTP);

    verify(whatsAppChannel).send(PHONE, OTP);
  }

  @Test
  @DisplayName("sendOtp(phone, otp): backward compat — only WhatsApp dispatched")
  void sendOtp_BackwardCompat_OnlyWhatsApp() {
    NotificationServiceImpl service = createServiceWhatsAppOnly();

    service.sendOtp(PHONE, OTP);

    verify(whatsAppChannel).send(PHONE, OTP);
  }
}
