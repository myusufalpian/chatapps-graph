package id.xyz.chatapps_graph.framework.dto;

import lombok.Builder;

@Builder
public record ForwardedInfo(String originalMessageUuid, String originalSenderUuid) {}
