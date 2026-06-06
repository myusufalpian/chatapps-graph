package id.xyz.chatapps_graph.framework.dto;

import id.xyz.chatapps_graph.domain.enums.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record OtpRequestDTO(
    @NotBlank String phone,
    @NotNull OtpPurpose purpose
) {}
