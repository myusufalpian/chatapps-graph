package id.xyz.chatapps_graph.framework.dto;

import lombok.Builder;

@Builder
public record VerifyOtpRequestDTO(String phone, String code) {}
