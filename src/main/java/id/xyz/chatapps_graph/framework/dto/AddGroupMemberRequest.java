package id.xyz.chatapps_graph.framework.dto;

import jakarta.validation.constraints.NotBlank;

public record AddGroupMemberRequest(
    @NotBlank String userUuid
) {}
