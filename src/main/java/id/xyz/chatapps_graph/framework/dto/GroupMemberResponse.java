package id.xyz.chatapps_graph.framework.dto;

import lombok.Builder;

@Builder
public record GroupMemberResponse(
    String userUuid,
    String fullName,
    String profilePhoto,
    String role
) {}
