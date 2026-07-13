package id.xyz.chatapps_graph.framework.dto;

import jakarta.validation.constraints.NotNull;

public record DisappearingTtlRequest(
    @NotNull String ttl
) {}
