package id.xyz.chatapps_graph.framework.dto;

import lombok.Builder;

@Builder
public record ReactionSummary(String emoji, int count, boolean reactedByMe) {}
