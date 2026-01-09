package id.xyz.chatapps_graph.framework.dto;

import lombok.Builder;

@Builder
public record UserSignInRequestDTO (String phone) {}
