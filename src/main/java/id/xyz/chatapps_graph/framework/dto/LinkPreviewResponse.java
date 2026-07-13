package id.xyz.chatapps_graph.framework.dto;

import lombok.Builder;

@Builder
public record LinkPreviewResponse(
    String url,
    String title,
    String description,
    String imageUrl,
    String siteName
) {}
