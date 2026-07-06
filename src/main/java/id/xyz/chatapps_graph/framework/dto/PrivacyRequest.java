package id.xyz.chatapps_graph.framework.dto;

import jakarta.validation.constraints.NotNull;

public record PrivacyRequest(@NotNull Boolean hideReadReceipt) {}
