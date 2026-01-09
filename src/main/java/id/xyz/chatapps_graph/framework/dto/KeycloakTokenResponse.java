package id.xyz.chatapps_graph.framework.dto;

import lombok.Builder;

@Builder
public record KeycloakTokenResponse(
    String accessToken,
    String refreshToken,
    Integer expiresIn,
    Integer refreshExpiresIn,
    String tokenType
) {

}
