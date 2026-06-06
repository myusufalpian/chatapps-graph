package id.xyz.chatapps_graph.infrastructure.notification.whatsapp;

import id.xyz.chatapps_graph.applications.usecase.WhatsAppProvider;
import id.xyz.chatapps_graph.infrastructure.config.properties.FonnteProperties;
import id.xyz.chatapps_graph.infrastructure.utility.MaskingUtil;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class FonnteWhatsAppProvider implements WhatsAppProvider {

  private final RestClient restClient;

  @Autowired
  public FonnteWhatsAppProvider(FonnteProperties properties) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(10));

    this.restClient = RestClient.builder()
        .baseUrl(properties.getApiUrl())
        .defaultHeader("Authorization", properties.getApiToken() != null ? properties.getApiToken() : "")
        .requestFactory(factory)
        .build();
  }

  // Visible for testing
  FonnteWhatsAppProvider(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public void sendMessage(String phone, String otp) {
    Map<String, String> body = Map.of(
        "target", phone,
        "message", "Your OTP code: " + otp + ". Valid for 3 minutes. Do not share this code.",
        "countryCode", "62"
    );

    restClient.post()
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .toBodilessEntity();

    log.info("WhatsApp OTP sent via Fonnte to [{}]", MaskingUtil.maskPhone(phone));
  }

  @Override
  public String getProviderName() {
    return "fonnte";
  }
}
