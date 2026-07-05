package id.xyz.chatapps_graph.framework.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceRegistrationRequest(
    @NotBlank @Size(max = 500) String deviceToken,
    @NotBlank String platform
) {}
