package id.xyz.chatapps_graph.framework.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KeycloakTokenResponse(
    String accessToken,
    String refreshToken,
    Integer expiresIn,
    Integer refreshExpiresIn,
    String tokenType
) {

}
