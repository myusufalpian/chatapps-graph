package id.xyz.chatapps_graph.infrastructure.config.security;

import id.xyz.chatapps_graph.framework.dto.KeycloakTokenResponse;
import id.xyz.chatapps_graph.infrastructure.config.properties.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class KeycloakAuthService {

  private final RestClient restClient = RestClient.create();

  private final KeycloakProperties keycloakProperties;

  public KeycloakTokenResponse exchangePasswordForToken(String phoneNumber) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "password");
    formData.add("client_id", keycloakProperties.getClientId());
    formData.add("username", phoneNumber);

    if (StringUtils.hasLength(keycloakProperties.getClientSecret())) {
      formData.add("client_secret", keycloakProperties.getClientSecret());
    }

    return restClient.post()
        .uri(keycloakProperties.getTokenUri())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(formData)
        .retrieve()
        .body(KeycloakTokenResponse.class);
  }

  public KeycloakTokenResponse refreshToken(String refreshToken) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "refresh_token");
    formData.add("client_id", keycloakProperties.getClientId());
    formData.add("refresh_token", refreshToken);

    if (StringUtils.hasLength(keycloakProperties.getClientSecret())) {
      formData.add("client_secret", keycloakProperties.getClientSecret());
    }

    return restClient.post()
        .uri(keycloakProperties.getTokenUri())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(formData)
        .retrieve()
        .body(KeycloakTokenResponse.class);
  }

}
