package id.xyz.chatapps_graph.framework.dto;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record PresenceResponse(String status, OffsetDateTime lastSeenAt) {}
