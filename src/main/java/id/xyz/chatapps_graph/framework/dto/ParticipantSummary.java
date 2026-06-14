package id.xyz.chatapps_graph.framework.dto;

import lombok.Builder;

@Builder
public record ParticipantSummary(
    String userUuid,
    String fullName,
    String profilePhoto
) {}
