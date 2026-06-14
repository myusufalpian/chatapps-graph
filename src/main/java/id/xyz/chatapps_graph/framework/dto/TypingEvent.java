package id.xyz.chatapps_graph.framework.dto;

import lombok.Builder;

@Builder
public record TypingEvent(String conversationUuid, String userUuid, boolean isTyping) {}
