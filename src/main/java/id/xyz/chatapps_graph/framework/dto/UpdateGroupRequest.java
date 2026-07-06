package id.xyz.chatapps_graph.framework.dto;

import jakarta.validation.constraints.Size;

public record UpdateGroupRequest(
    @Size(max = 255) String name,
    String description
) {}
