package id.xyz.chatapps_graph.framework.dto;

import lombok.Builder;

@Builder
public record ValidationData(String field, String message) {
}
